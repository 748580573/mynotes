# zookeeperServer之SyncRequestProcessor源码解析

# **一、前言**

在分析了PrepRequestProcessor处理器后，接着来分析SyncRequestProcessor，该处理器将请求存入磁盘，其将请求批量的存入磁盘以提高效率，请求在写入磁盘之前是不会被转发到下个处理器的。

# **二、SyncRequestProcessor源码分析**

## 2.1 类的继承关系　　　

```java
public class SyncRequestProcessor extends Thread implements RequestProcessor {}
```

说明：与PrepRequestProcessor一样，SyncRequestProcessor也继承了Thread类并实现了RequestProcessor接口，表示其可以作为线程使用。

## 2.2 类的属性　　

```java
public class SyncRequestProcessor extends Thread implements RequestProcessor {
    // 日志
    private static final Logger LOG = LoggerFactory.getLogger(SyncRequestProcessor.class);
    
    // Zookeeper服务器
    private final ZooKeeperServer zks;
    
    // 请求队列
    private final LinkedBlockingQueue<Request> queuedRequests =
        new LinkedBlockingQueue<Request>();
        
    // 下个处理器
    private final RequestProcessor nextProcessor;
    
    // 快照处理线程
    private Thread snapInProcess = null;
    
    // 是否在运行中
    volatile private boolean running;

    /**
     * Transactions that have been written and are waiting to be flushed to
     * disk. Basically this is the list of SyncItems whose callbacks will be
     * invoked after flush returns successfully.
     */
    // 等待被刷新到磁盘的请求队列
    private final LinkedList<Request> toFlush = new LinkedList<Request>();
    
    // 随机数生成器
    private final Random r = new Random(System.nanoTime());
    /**
     * The number of log entries to log before starting a snapshot
     */
    // 快照个数
    private static int snapCount = ZooKeeperServer.getSnapCount();
    
    /**
     * The number of log entries before rolling the log, number
     * is chosen randomly
     */
    // 日志滚动之前记录的日志号，编号是随机选择的
    private static int randRoll;

    // 结束请求标识
    private final Request requestOfDeath = Request.requestOfDeath;
}
```

说明：其中，SyncRequestProcessor维护了ZooKeeperServer实例，其用于获取ZooKeeper的数据库和其他信息；维护了一个处理请求的队列，其用于存放请求；维护了一个处理快照的线程，用于处理快照；维护了一个running标识，标识SyncRequestProcessor是否在运行；同时还维护了一个等待被刷新到磁盘的请求队列。

## 2.3 类的构造函数

```
    public SyncRequestProcessor(ZooKeeperServer zks,
            RequestProcessor nextProcessor)
    {
        // 调用父类构造函数
        super("SyncThread:" + zks.getServerId());
        // 给字段赋值
        this.zks = zks;
        this.nextProcessor = nextProcessor;
        running = true;
    }
```

　　说明：构造函数首先会调用Thread类的构造函数，然后根据构造函数参数给类的属性赋值，其中会确定下个处理器，并会设置该处理器正在运行的标识。

## 2.4 类的核心函数分析

**run函数**

```java
    public void run() {
        try {
            // 写日志数量初始化为0
            int logCount = 0;

            // we do this in an attempt to ensure that not all of the servers
            // in the ensemble take a snapshot at the same time
            // 确保所有的服务器在同一时间不是使用的同一个快照
            setRandRoll(r.nextInt(snapCount/2));
            while (true) { // 
                // 初始请求为null
                Request si = null;
                if (toFlush.isEmpty()) { // 没有需要刷新到磁盘的请求
                    // 从请求队列中取出一个请求，若队列为空会阻塞
                    si = queuedRequests.take();
                } else { // 队列不为空，即有需要刷新到磁盘的请求
                    // 从请求队列中取出一个请求，若队列为空，则返回空，不会阻塞
                    si = queuedRequests.poll();
                    if (si == null) { // 取出的请求为空
                        // 刷新到磁盘
                        flush(toFlush);
                        // 跳过后面的处理
                        continue;
                    }
                }
                if (si == requestOfDeath) { // 在关闭处理器之后，会添加requestOfDeath，表示关闭后不再处理请求
                    break;
                }
                if (si != null) { // 请求不为空
                    // track the number of records written to the log
                    if (zks.getZKDatabase().append(si)) { // 将请求添加至日志文件，只有事务性请求才会返回true
                        // 写入一条日志，logCount加1
                        logCount++;
                        if (logCount > (snapCount / 2 + randRoll)) { // 满足roll the log的条件
                            randRoll = r.nextInt(snapCount/2);
                            // roll the log
                            zks.getZKDatabase().rollLog();
                            // take a snapshot
                            if (snapInProcess != null && snapInProcess.isAlive()) { // 正在进行快照
                                LOG.warn("Too busy to snap, skipping");
                            } else { // 未被处理
                                snapInProcess = new Thread("Snapshot Thread") { // 创建线程来处理快照
                                        public void run() {
                                            try {
                                                // 进行快照
                                                zks.takeSnapshot();
                                            } catch(Exception e) {
                                                LOG.warn("Unexpected exception", e);
                                            }
                                        }
                                    };
                                // 开始处理
                                snapInProcess.start();
                            }
                            // 重置为0
                            logCount = 0;
                        }
                    } else if (toFlush.isEmpty()) { // 等待被刷新到磁盘的请求队列为空
                        // optimization for read heavy workloads
                        // iff this is a read, and there are no pending
                        // flushes (writes), then just pass this to the next
                        // processor
                        // 查看此时toFlush是否为空，如果为空，说明近段时间读多写少，直接响应
                        if (nextProcessor != null) { // 下个处理器不为空
                            // 下个处理器开始处理请求
                            nextProcessor.processRequest(si);
                            if (nextProcessor instanceof Flushable) { // 处理器是Flushable的
                                // 刷新到磁盘
                                ((Flushable)nextProcessor).flush();
                            }
                        }
                        // 跳过后续处理
                        continue;
                    }
                    // 将请求添加至被刷新至磁盘队列
                    toFlush.add(si);
                    if (toFlush.size() > 1000) { // 队列大小大于1000，直接刷新到磁盘
                        flush(toFlush);
                    }
                }
            }
        } catch (Throwable t) { // 出现异常
            LOG.error("Severe unrecoverable error, exiting", t);
            // 设置运行标识为false，表示该处理器不再运行
            running = false;
            // 退出程序
            System.exit(11);
        }
        LOG.info("SyncRequestProcessor exited!");
    }
```

　　说明：该函数是整个处理器的核心，其逻辑大致如下

　　(1) 设置randRoll大小，确保所有的服务器在同一时间不是使用的同一个快照。

　　(2) 判断toFlush队列是否为空，若是，则表示没有需要刷新到磁盘的请求，进入(3)，若否，进入(4)。

　　(3) 从queuedRequests中取出一个请求，进入(6)。

　　(4) 从queuedRequests中取出一个请求，判断该请求是否为null，若是，则进入(5)，若否，则进入(6)。

　　(5) 调用flush函数，将toFlush中的请求刷新到磁盘，跳过之后的处理部分。

　　(6) 判断请求是否是结束请求（在调用shutdown之后，会在队列中添加一个requestOfDeath）。若是，则退出，否则，进入(7)。

　　(7) 判断请求是否为null，若否，则进入(8)，否则进入(2)。

　　(8) 若写入日志成功，返回true（表示为事务性请求），进入(9)，否则进入(18)。

　　(9) logCount加1，并判断是否大于了阈值，若是，则进入(10)，否则进入(18)。

　　(10) 调用rollLog函数翻转日志文件。

　　(11) 判断snapInProcess是否为空并且是否存活，若是，则输出日志，否则，进入(12)。

　　(12) 创建snapInProcess线程并启动。

　　(13) 重置logCount为0。

　　(14) 判断toFlush队列是否为空，若是，进入(15)。

　　(15) 判断nextProcessor是否为空，若否，则使用nextProcessor处理请求，否则进入(16)。

　　(16) 判断nextProcessor是否是Flushable的，若是，则调用flush函数刷新请求至磁盘，否则进入(17)

　　(17) 跳过之后的处理步骤。

　　(18) 将请求添加至toFlush队列。

　　(19) 若toFlush队列大小大于1000，则刷新至磁盘，进入(2)。

其中会调用flush函数，其源码如下　

```java
    // 刷新到磁盘
    private void flush(LinkedList<Request> toFlush)
        throws IOException, RequestProcessorException
    {
        if (toFlush.isEmpty()) // 队列为空，返回
            return;

        // 提交至ZK数据库
        zks.getZKDatabase().commit();
        while (!toFlush.isEmpty()) { // 队列不为空
            // 从队列移除请求
            Request i = toFlush.remove();
            if (nextProcessor != null) { // 下个处理器不为空
                // 下个处理器开始处理请求
                nextProcessor.processRequest(i);
            }
        }
        if (nextProcessor != null && nextProcessor instanceof Flushable) { // 下个处理器不为空并且是Flushable的
            // 刷新到磁盘
            ((Flushable)nextProcessor).flush();
        }
    }
```

说明：该函数主要用于将toFlush队列中的请求刷新到磁盘中。

**shutdown函数**　

```java
    public void shutdow1n() {
        LOG.info("Shutting down");
        // 添加结束请求请求至队列
        queuedRequests.add(requestOfDeath);
        try {
            if(running){ // 还在运行
                // 等待该线程终止
                this.join();
            }
            if (!toFlush.isEmpty()) { // 队列不为空
                // 刷新到磁盘
                flush(toFlush);
            }
        } catch(InterruptedException e) {
            LOG.warn("Interrupted while wating for " + this + " to finish");
        } catch (IOException e) {
            LOG.warn("Got IO exception during shutdown");
        } catch (RequestProcessorException e) {
            LOG.warn("Got request processor exception during shutdown");
        }
        if (nextProcessor != null) {
            nextProcessor.shutdown();
        }
    }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

　　说明：该函数用于关闭SyncRequestProcessor处理器，其首先会在queuedRequests队列中添加一个结束请求，然后再判断SyncRequestProcessor是否还在运行，若是，则会等待其结束；之后判断toFlush队列是否为空，若不为空，则刷新到磁盘中。

## **三、总结**

　　本篇讲解了SyncRequestProcessor的工作原理，其主要作用包含将事务性请求刷新到磁盘，并且对请求进行快照处理。也谢谢各位园友的观看~****

关于SyncRequestProcessor的下一个处理器FinalRequestProcessor的源码解析，[点击此处](../zookeeperServer之FinalRequestProcessor源码解析/zookeeperServer之FinalRequestProcessor源码解析.md)查看。