# zookeeperServer之创建session概览

目录

- session建立的主要过程
- 客户端发起连接
- 服务端创建session

## session建立的主要过程

用一张图来说明session建立过程中client和server的交互

![](./imgs/1.png)

主要流程

- 服务端启动，客户端启动
- 客户端发起socket连接
- 服务端accept socket连接，socket连接建立
- 客户端发送ConnectRequest给server
- server收到后初始化ServerCnxn，代表一个和客户端的连接，即session，server发送ConnectResponse给client
- client处理ConnectResponse，session建立完成

## 客户发起连接

### 和server建立socket连接

客户端要发起连接要先启动，不论是使用curator client还是zkClient，都是初始化`org.apache.zookeeper.ZooKeeper#ZooKeeper`。

Zookeeper初始化的主要工作是初始化自己的一些关键组件

- Watcher，外部构造好传入
- 初始化StaticHostProvider，决定客户端选择连接哪一个server
- ClientCnxn，客户端网络通信的组件，主要启动逻辑就是启动这个类

ClientCnxn包含两个线程

- SendThread，负责client端消息的发送和接收
- EventThread，负责处理event

ClientCnxn初始化的过程就是初始化启动这两个线程，客户端发起连接的主要逻辑在SendThread线程中

```java
// org.apache.zookeeper.ClientCnxn.SendThread#run
@Override
public void run() {
    clientCnxnSocket.introduce(this,sessionId);
    clientCnxnSocket.updateNow();
    clientCnxnSocket.updateLastSendAndHeard();
    int to;
    long lastPingRwServer = System.currentTimeMillis();
    final int MAX_SEND_PING_INTERVAL = 10000; //10 seconds
    while (state.isAlive()) {
        try {
            // client是否连接到server，如果没有连接到则连接server
            if (!clientCnxnSocket.isConnected()) {
                if(!isFirstConnect){
                    try {
                        Thread.sleep(r.nextInt(1000));
                    } catch (InterruptedException e) {
                        LOG.warn("Unexpected exception", e);
                    }
                }
                // don't re-establish connection if we are closing
                if (closing || !state.isAlive()) {
                    break;
                }
                // 这个里面去连接server
                startConnect();
                clientCnxnSocket.updateLastSendAndHeard();
            }

            // 省略中间代码...
            clientCnxnSocket.doTransport(to, pendingQueue, outgoingQueue, ClientCnxn.this);
            // 省略中间代码...
}
```

SendThread#run是一个while循环，只要client没有被关闭会一直循环，每次循环判断当前client是否连接到server，如果没有则发起连接，发起连接调用了startConnect

```java
private void startConnect() throws IOException {
    state = States.CONNECTING;

    InetSocketAddress addr;
    if (rwServerAddress != null) {
        addr = rwServerAddress;
        rwServerAddress = null;
    } else {
        // 通过hostProvider来获取一个server地址
        addr = hostProvider.next(1000);
    }
	// 省略中间代码...

    // 建立client与server的连接
    clientCnxnSocket.connect(addr);
}
```

到这里client发起了socket连接，server监听的端口收到client的连接请求后和client建立连接。

### 通过一个request来建立session连接

socket连接建立后，client会向server发送一个ConnectRequest来建立session连接。两种情况会发送ConnectRequest

- 在上面的connect方法中会判断是否socket已经建立成功，如果建立成功就会发送ConnectRequest
- 如果socket没有立即建立成功（socket连接建立是异步的），则发送这个packet要延后到doTransport中

发送ConnectRequest是在下面的方法中

```java
// org.apache.zookeeper.ClientCnxn.SendThread#primeConnection
void primeConnection() throws IOException {
    LOG.info("Socket connection established to "
             + clientCnxnSocket.getRemoteSocketAddress()
             + ", initiating session");
    isFirstConnect = false;
    long sessId = (seenRwServerBefore) ? sessionId : 0;
    ConnectRequest conReq = new ConnectRequest(0, lastZxid,
                                               sessionTimeout, sessId, sessionPasswd);
    	// 省略中间代码...
    	// 将conReq封装为packet放入outgoingQueue等待发送
        outgoingQueue.addFirst(new Packet(null, null, conReq,
                                          null, null, readOnly));
    }
    clientCnxnSocket.enableReadWriteOnly();
    if (LOG.isDebugEnabled()) {
        LOG.debug("Session establishment request sent on "
                  + clientCnxnSocket.getRemoteSocketAddress());
    }
}
```

请求中带的参数

- lastZxid：上一个事务的id
- sessionTimeout：client端配置的sessionTimeout
- sessId：sessionId，如果之前建立过连接取的是上一次连接的sessionId
- sessionPasswd：session的密码

## 服务端创建session

### 和client建立socket连接

在server启动的过程中除了会启动用于选举的网络组件还会启动用于处理client请求的网络组件

```java
org.apache.zookeeper.server.NIOServerCnxnFactory
```

![](./imgs/2.png)

主要启动了三个线程：

- AcceptThread：用于接收client的连接请求，建立连接后交给SelectorThread线程处理
- SelectorThread：用于处理读写请求
- ConnectionExpirerThread：检查session连接是否过期

client发起socket连接的时候，server监听了该端口，接收到client的连接请求，然后把建立练级的SocketChannel放入队列里面，交给SelectorThread处理

```java
// org.apache.zookeeper.server.NIOServerCnxnFactory.SelectorThread#addAcceptedConnection
public boolean addAcceptedConnection(SocketChannel accepted) {
    if (stopped || !acceptedQueue.offer(accepted)) {
        return false;
    }
    wakeupSelector();
    return true;
}
```

### 建立session连接

SelectorThread是一个不断循环的线程，每次循环都会处理刚刚建立的socket连接

```java
// org.apache.zookeeper.server.NIOServerCnxnFactory.SelectorThread#run
while (!stopped) {
    try {
        select();
        //  处理对立中的socket
        processAcceptedConnections();
        processInterestOpsUpdateRequests();
    } catch (RuntimeException e) {
        LOG.warn("Ignoring unexpected runtime exception", e);
    } catch (Exception e) {
        LOG.warn("Ignoring unexpected exception", e);
    }
}

// org.apache.zookeeper.server.NIOServerCnxnFactory.SelectorThread#processAcceptedConnections
private void processAcceptedConnections() {
    SocketChannel accepted;
    while (!stopped && (accepted = acceptedQueue.poll()) != null) {
        SelectionKey key = null;
        try {
            // 向该socket注册读事件
            key = accepted.register(selector, SelectionKey.OP_READ);
            // 创建一个NIOServerCnxn维护session
            NIOServerCnxn cnxn = createConnection(accepted, key, this);
            key.attach(cnxn);
            addCnxn(cnxn);
        // 省略中间代码...
    }
}
```

说了这么久，我们说的session究竟是什么还没有解释，session中文翻译是会话，在这里就是zk的server和client维护的一个具有一些特别属性的网络连接，网络连接这里就是socket连接，一些特别的属性包括

- sessionId：唯一标示一个会话
- sessionTimeout：这个连接的超时时间，超过这个时间server就会把连接断开

所以session建立的两步就是

- 建立socket连接
- client发起建立session请求，server建立一个实例来维护这个连接

server收到ConnectRequest之后，按照正常处理io的方式处理这个request，server端的主要操作是

- 反序列化为ConnectRequest
- 根据request中的sessionId来判断是新的session连接还是session重连
  - 如果是新连接
    - 生成sessionId
    - 创建新的SessionImpl并放入org.apache.zookeeper.server.SessionTrackerImpl#sessionExpiryQueue
    - 封装该请求为新的request在processorChain中传递，最后交给FinalRequestProcessor处理
  - 如果是重连
    - 关闭sessionId对应的原来的session
      - 关闭原来的socket连接
      - sessionImp会在sessionExpiryQueue中由于过期被清理
    - 重新打开一个session
      - 将原来的sessionId设置到当前的NIOServerCnxn实例中，作为新的连接的sessionId
      - 校验密码是否正确密码错误的时候直接返回给客户端，不可用的session
      - 密码正确的话，新建SessionImpl
      - 返回给客户端sessionId

总体流程是

![](./imgs/3.png)

其中有一个session生成算法我们来看下

```java
public static long initializeNextSession(long id) {
    // sessionId是long类型，共8个字节，64位
    long nextSid;
    // 取时间戳的的低40位作为初始化sessionId的第16-55位，这里使用的是无符号右移，不会出现负数
    nextSid = (Time.currentElapsedTime() << 24) >>> 8;
    // 使用serverId（配置文件中指定的myid）作为高8位
    nextSid =  nextSid | (id <<56);
    // nextSid为long的最小值，这中情况不可能出现，这里只是作为一个case列在这里
    if (nextSid == EphemeralType.CONTAINER_EPHEMERAL_OWNER) {
        ++nextSid;  // this is an unlikely edge case, but check it just in case
    }
    return nextSid;
}
```

初始化sessionId的组成

myid(1字节)+截取的时间戳低40位(5个字节)+2个字节(初始化都是0)

每个server再基于这个id不断自增，这样的算法就保证了每个server的sessionId是全局唯一的。

## 总结

session在zk框架中是一个重要概念，很多功能都依赖于session，比如临时节点，session关闭后就自动删除了。本文主要介绍了session的建立过程中client和server各自的处理方式。