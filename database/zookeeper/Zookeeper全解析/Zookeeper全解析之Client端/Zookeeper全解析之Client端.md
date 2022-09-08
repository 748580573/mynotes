# Zookeeper全解析之Client端

Zookeeper的Client直接与用户打交道，是我们使用Zookeeper的interface。了解ZK Client的结构和工作原理有利于我们合理的使用ZK，并能在使用中更早的发现问题。本文将在研究源码的技术上讲述ZK Client的工作原理及内部工作机制。

在看完ZK Client的大致架构以后我希望能有一种简单的方式描述ZK Client的基本结构，想来想去我觉得还是图片比较能反映情况，于是我画了这张大致的结构图：

我想既然我画了这张图，就让我们从这张图开始讲起吧。

![](./imgs/1.jpg)

**模块：**

我们可以认为ZK的Client由三个主要模块组成：Zookeeper, WatcherManager, ClientCnxn

Zookeeper是ZK Client端的真正接口，用户可以操作的最主要的类，当用户创建一个Zookeeper实例以后，几乎所有的操作都被这个实例包办了，用户不用关心怎么连接到Server，Watcher什么时候被触发等等令人伤神的问题。

WatcherManager，顾名思义，它是用来管理Watcher的，Watcher是ZK的一大特色功能，允许多个Client对一个或多个ZNode进行监控，当ZNode有变化时能够通知到监控这个ZNode的各个Client。我们把一个ZK Client简单看成一个Zookeeper实例，那么这个实例内部的WatcherManager就管理了ZK Client绑定的所有Watcher。

ClientCnxn是管理所有网络IO的模块，所有和ZK Server交互的信息和数据都经过这个模块，包括给ZK Server发送Request，从ZK Server接受Response，以及从ZK Server接受Watcher Event。ClientCnxn完全管理了网络，从外部看来网络操作是透明的。

**线程：**

每当我们创建一个Zookeeper实例的时候，会有两个线程被创建：SendThread和EventThread。所以当我们使用ZK Client端的时候应该尽量只创建一个Zookeeper实例并反复使用。大量的创建销毁Zookeeper实例不仅会反复的创建和销毁线程，而且会在Server端创建大量的Session。

SendThread是真正处理网络IO的线程，所有通过网络发送和接受的数据包都在这个线程中处理。这个线程的主体是一个while循环：

````java
while (zooKeeper.state.isAlive()) {
        try {
            if (sockKey == null) {
            // don’t re-establish connection if we are closing
                if (closing) {
                    break;
                }
                startConnect();
                lastSend = now;
                lastHeard = now;
            }
            … ….
            selector.select(to);
            Set<SelectionKey> selected;
            synchronized (this) {
                selected = selector.selectedKeys();
            }
            // Everything below and until we get back to the select is
            // non blocking, so time is effectively a constant. That is
            // Why we just have to do this once, here
            now = System.currentTimeMillis();
            for (SelectionKey k : selected) {
                … …
                if (doIO()) {
                    lastHeard = now;
                }
                … …
            }
        }
        catch() {
            … …
        }
    }
````

这里用了java的nio功能，当selector侦测到事件发生的时候就会触发一次循环，主要的操作会在doIO()里面完成：

````java
boolean doIO() throws InterruptedException, IOException {
        boolean packetReceived = false;
        SocketChannel sock = (SocketChannel) sockKey.channel();
        if (sock == null) {
            throw new IOException(“Socket is null!”);
        }
        if (sockKey.isReadable()) {
            … …
        }
         
        if (sockKey.isWritable()) {
        … …
        }

        if (outgoingQueue.isEmpty()) {
            disableWrite();
        } else {
            enableWrite();
        }
        return packetReceived;
    }
````

这个过程大概是这样的：

1. 如果有数据可读，则读取数据包，如果数据包是先前发出去的Request的Response，那么这个数据包一定在Pending Queue里面。将它从Pending Queue里面移走，并将此信息添加到Waiting Event Queue 里面，如果数据包是一个Watcher Event，将此信息添加到Waiting Event Queue里面。

2. 如果OutgoingQueue里面有数据需要发送，则发送数据包并把数据包从Outgoing Queue移至Pending Queue，意思是数据我已经发出去了，但还要等待Server端的回复，所以这个请求现在是Pending 的状态。

另外一个线程EventThread是用来处理Event的。前面提到SendThread从Server收到数据的时候会把一些信息添加到Event Thread里面，比如Finish Event和Watcher Event。EventThread就是专门用来处理这些Event的，收到Finish Event的时候会把相对应的Package置成Finish状态，这样等待结果的Client函数就能得以返回。收到Watcher Event的时候会联系WatcherManager找到相对应的Watcher，从WatcherManager里面移除这个Watcher(因为每个Watcher只会被通知一次) 并回调Watcher的process函数。所以所有Watcher的process函数是运行在EventThread里面的。

**保持连接：**

到目前为止应该已经大概介绍了ZK Client端的大致结构和处理流程。还剩下一个问题就是当网络出问题时ZK Client是如何处理的。其实这个过程并不复杂，大概是执行以下步骤：

1. 网络发生故障，网络操作抛出的异常被捕获。

2. 确认网络操作失败，清除当前与Server相关的网络资源，包括Socket等等。

3. 在Server列表中逐个尝试链接Server。

这个过程从外界看来是透明的，外界并不会觉察到ZK Client已经悄悄地更换了一个连接的Server。

好了，对于ZK Client的介绍大概就这么多了，希望这样的介绍对于大家学习和使用Zookeeper有一些帮助。对于文章中没有介绍或者没有说清楚的地方需要进一步查看源码来解决。