# zookeeperServer之FinalRequestProcessor源码解析

# **一、前言**

　　前面分析了SyncReqeustProcessor，接着分析请求处理链中最后的一个处理器FinalRequestProcessor。

# **二、FinalRequestProcessor源码分析**

## 2.1 类的继承关系　　

```java
public class FinalRequestProcessor implements RequestProcessor {}
```

　　说明：FinalRequestProcessor只实现了RequestProcessor接口，其需要实现processRequest方法和shutdown方法。

## 2.2 类的属性　

```java
public class FinalRequestProcessor implements RequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FinalRequestProcessor.class);

    // ZooKeeper服务器
    ZooKeeperServer zks;
}
```

　　说明：其核心属性为zks，表示Zookeeper服务器，可以通过zks访问到Zookeeper内存数据库。

## 2.3 类的构造函数

```java
    public FinalRequestProcessor(ZooKeeperServer zks) {
        this.zks = zks;
    }
```

## 2.4 核心函数分析

　　1. processRequest　

````java
public void processRequest(Request request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request:: " + request);
        }
        // request.addRQRec(">final");
        long traceMask = ZooTrace.CLIENT_REQUEST_TRACE_MASK;
        if (request.type == OpCode.ping) { // 请求类型为PING
            traceMask = ZooTrace.SERVER_PING_TRACE_MASK;
        }
        if (LOG.isTraceEnabled()) {
            ZooTrace.logRequest(LOG, traceMask, 'E', request, "");
        }
        ProcessTxnResult rc = null;
        synchronized (zks.outstandingChanges) { // 同步块
            while (!zks.outstandingChanges.isEmpty()
                    && zks.outstandingChanges.get(0).zxid <= request.zxid) { // outstandingChanges不为空且首个元素的zxid小于请求的zxid
                // 移除首个元素
                ChangeRecord cr = zks.outstandingChanges.remove(0);
                if (cr.zxid < request.zxid) { // 若Record的zxid小于请求的zxid
                    LOG.warn("Zxid outstanding "
                            + cr.zxid
                            + " is less than current " + request.zxid);
                }
                if (zks.outstandingChangesForPath.get(cr.path) == cr) { // 根据路径得到Record并判断是否为cr
                    // 移除cr的路径对应的记录
                    zks.outstandingChangesForPath.remove(cr.path);
                }
            }
            if (request.hdr != null) { // 请求头不为空
                // 获取请求头
               TxnHeader hdr = request.hdr;
               // 获取请求事务
               Record txn = request.txn;
                // 处理事务
               rc = zks.processTxn(hdr, txn);
            }
            // do not add non quorum packets to the queue.
            if (Request.isQuorum(request.type)) { // 只将quorum包（事务性请求）添加进队列
                zks.getZKDatabase().addCommittedProposal(request);
            }
        }

        if (request.hdr != null && request.hdr.getType() == OpCode.closeSession) { // 请求头不为空并且请求类型为关闭会话
            ServerCnxnFactory scxn = zks.getServerCnxnFactory();
            // this might be possible since
            // we might just be playing diffs from the leader
            if (scxn != null && request.cnxn == null) { // 
                // calling this if we have the cnxn results in the client's
                // close session response being lost - we've already closed
                // the session/socket here before we can send the closeSession
                // in the switch block below
                // 关闭会话
                scxn.closeSession(request.sessionId);
                return;
            }
        }

        if (request.cnxn == null) { // 请求的cnxn为空，直接返回 
            return;
        }
        ServerCnxn cnxn = request.cnxn;

        String lastOp = "NA";
        zks.decInProcess();
        Code err = Code.OK;
        Record rsp = null;
        boolean closeSession = false;
        try {
            if (request.hdr != null && request.hdr.getType() == OpCode.error) {
                throw KeeperException.create(KeeperException.Code.get((
                        (ErrorTxn) request.txn).getErr()));
            }

            KeeperException ke = request.getException();
            if (ke != null && request.type != OpCode.multi) {
                throw ke;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("{}",request);
            }
            switch (request.type) {
            case OpCode.ping: { // PING请求
                // 更新延迟
                zks.serverStats().updateLatency(request.createTime);

                lastOp = "PING";
                // 更新响应的状态
                cnxn.updateStatsForResponse(request.cxid, request.zxid, lastOp,
                        request.createTime, System.currentTimeMillis());
                // 设置响应
                cnxn.sendResponse(new ReplyHeader(-2,
                        zks.getZKDatabase().getDataTreeLastProcessedZxid(), 0), null, "response");
                return;
            }
            case OpCode.createSession: { // 创建会话请求
                // 更新延迟
                zks.serverStats().updateLatency(request.createTime);
                
                lastOp = "SESS";
                // 更新响应的状态
                cnxn.updateStatsForResponse(request.cxid, request.zxid, lastOp,
                        request.createTime, System.currentTimeMillis());
                // 结束会话初始化
                zks.finishSessionInit(request.cnxn, true);
                return;
            }
            case OpCode.multi: { // 多重操作
                
                lastOp = "MULT";
                rsp = new MultiResponse() ;

                for (ProcessTxnResult subTxnResult : rc.multiResult) { // 遍历多重操作结果

                    OpResult subResult ;

                    switch (subTxnResult.type) { // 确定每个操作类型
                        case OpCode.check: // 检查
                            subResult = new CheckResult();
                            break;
                        case OpCode.create: // 创建
                            subResult = new CreateResult(subTxnResult.path);
                            break;
                        case OpCode.delete: // 删除
                            subResult = new DeleteResult();
                            break;
                        case OpCode.setData: // 设置数据
                            subResult = new SetDataResult(subTxnResult.stat);
                            break;
                        case OpCode.error: // 错误
                            subResult = new ErrorResult(subTxnResult.err) ;
                            break;
                        default: 
                            throw new IOException("Invalid type of op");
                    }
                    // 添加至响应结果集中
                    ((MultiResponse)rsp).add(subResult);
                }

                break;
            }
            case OpCode.create: { // 创建
                lastOp = "CREA";
                // 创建响应
                rsp = new CreateResponse(rc.path);
                err = Code.get(rc.err);
                break;
            }
            case OpCode.delete: { // 删除
                lastOp = "DELE";
                err = Code.get(rc.err);
                break;
            }
            case OpCode.setData: { // 设置数据
                lastOp = "SETD";
                rsp = new SetDataResponse(rc.stat);
                err = Code.get(rc.err);
                break;
            }
            case OpCode.setACL: { // 设置ACL
                lastOp = "SETA";
                rsp = new SetACLResponse(rc.stat);
                err = Code.get(rc.err);
                break;
            }
            case OpCode.closeSession: { // 关闭会话
                lastOp = "CLOS";
                closeSession = true;
                err = Code.get(rc.err);
                break;
            }
            case OpCode.sync: { // 同步
                lastOp = "SYNC";
                SyncRequest syncRequest = new SyncRequest();
                ByteBufferInputStream.byteBuffer2Record(request.request,
                        syncRequest);
                rsp = new SyncResponse(syncRequest.getPath());
                break;
            }
            case OpCode.check: { // 检查
                lastOp = "CHEC";
                rsp = new SetDataResponse(rc.stat);
                err = Code.get(rc.err);
                break;
            }
            case OpCode.exists: { // 存在性判断
                lastOp = "EXIS";
                // TODO we need to figure out the security requirement for this!
                ExistsRequest existsRequest = new ExistsRequest();
                // 将byteBuffer转化为Record
                ByteBufferInputStream.byteBuffer2Record(request.request,
                        existsRequest);
                String path = existsRequest.getPath();
                if (path.indexOf('\0') != -1) {
                    throw new KeeperException.BadArgumentsException();
                }
                Stat stat = zks.getZKDatabase().statNode(path, existsRequest
                        .getWatch() ? cnxn : null);
                rsp = new ExistsResponse(stat);
                break;
            }
            case OpCode.getData: { // 获取数据
                lastOp = "GETD";
                GetDataRequest getDataRequest = new GetDataRequest();
                ByteBufferInputStream.byteBuffer2Record(request.request,
                        getDataRequest);
                DataNode n = zks.getZKDatabase().getNode(getDataRequest.getPath());
                if (n == null) {
                    throw new KeeperException.NoNodeException();
                }
                Long aclL;
                synchronized(n) {
                    aclL = n.acl;
                }
                PrepRequestProcessor.checkACL(zks, zks.getZKDatabase().convertLong(aclL),
                        ZooDefs.Perms.READ,
                        request.authInfo);
                Stat stat = new Stat();
                byte b[] = zks.getZKDatabase().getData(getDataRequest.getPath(), stat,
                        getDataRequest.getWatch() ? cnxn : null);
                rsp = new GetDataResponse(b, stat);
                break;
            }
            case OpCode.setWatches: { // 设置watch
                lastOp = "SETW";
                SetWatches setWatches = new SetWatches();
                // XXX We really should NOT need this!!!!
                request.request.rewind();
                ByteBufferInputStream.byteBuffer2Record(request.request, setWatches);
                long relativeZxid = setWatches.getRelativeZxid();
                zks.getZKDatabase().setWatches(relativeZxid, 
                        setWatches.getDataWatches(), 
                        setWatches.getExistWatches(),
                        setWatches.getChildWatches(), cnxn);
                break;
            }
            case OpCode.getACL: { // 获取ACL
                lastOp = "GETA";
                GetACLRequest getACLRequest = new GetACLRequest();
                ByteBufferInputStream.byteBuffer2Record(request.request,
                        getACLRequest);
                Stat stat = new Stat();
                List<ACL> acl = 
                    zks.getZKDatabase().getACL(getACLRequest.getPath(), stat);
                rsp = new GetACLResponse(acl, stat);
                break;
            }
            case OpCode.getChildren: { // 获取子节点
                lastOp = "GETC";
                GetChildrenRequest getChildrenRequest = new GetChildrenRequest();
                ByteBufferInputStream.byteBuffer2Record(request.request,
                        getChildrenRequest);
                DataNode n = zks.getZKDatabase().getNode(getChildrenRequest.getPath());
                if (n == null) {
                    throw new KeeperException.NoNodeException();
                }
                Long aclG;
                synchronized(n) {
                    aclG = n.acl;
                    
                }
                PrepRequestProcessor.checkACL(zks, zks.getZKDatabase().convertLong(aclG), 
                        ZooDefs.Perms.READ,
                        request.authInfo);
                List<String> children = zks.getZKDatabase().getChildren(
                        getChildrenRequest.getPath(), null, getChildrenRequest
                                .getWatch() ? cnxn : null);
                rsp = new GetChildrenResponse(children);
                break;
            }
            case OpCode.getChildren2: {
                lastOp = "GETC";
                GetChildren2Request getChildren2Request = new GetChildren2Request();
                ByteBufferInputStream.byteBuffer2Record(request.request,
                        getChildren2Request);
                Stat stat = new Stat();
                DataNode n = zks.getZKDatabase().getNode(getChildren2Request.getPath());
                if (n == null) {
                    throw new KeeperException.NoNodeException();
                }
                Long aclG;
                synchronized(n) {
                    aclG = n.acl;
                }
                PrepRequestProcessor.checkACL(zks, zks.getZKDatabase().convertLong(aclG), 
                        ZooDefs.Perms.READ,
                        request.authInfo);
                List<String> children = zks.getZKDatabase().getChildren(
                        getChildren2Request.getPath(), stat, getChildren2Request
                                .getWatch() ? cnxn : null);
                rsp = new GetChildren2Response(children, stat);
                break;
            }
            }
        } catch (SessionMovedException e) {
            // session moved is a connection level error, we need to tear
            // down the connection otw ZOOKEEPER-710 might happen
            // ie client on slow follower starts to renew session, fails
            // before this completes, then tries the fast follower (leader)
            // and is successful, however the initial renew is then 
            // successfully fwd/processed by the leader and as a result
            // the client and leader disagree on where the client is most
            // recently attached (and therefore invalid SESSION MOVED generated)
            cnxn.sendCloseSession();
            return;
        } catch (KeeperException e) {
            err = e.code();
        } catch (Exception e) {
            // log at error level as we are returning a marshalling
            // error to the user
            LOG.error("Failed to process " + request, e);
            StringBuilder sb = new StringBuilder();
            ByteBuffer bb = request.request;
            bb.rewind();
            while (bb.hasRemaining()) {
                sb.append(Integer.toHexString(bb.get() & 0xff));
            }
            LOG.error("Dumping request buffer: 0x" + sb.toString());
            err = Code.MARSHALLINGERROR;
        }

        long lastZxid = zks.getZKDatabase().getDataTreeLastProcessedZxid();
        ReplyHeader hdr =
            new ReplyHeader(request.cxid, lastZxid, err.intValue());

        zks.serverStats().updateLatency(request.createTime);
        cnxn.updateStatsForResponse(request.cxid, lastZxid, lastOp,
                    request.createTime, System.currentTimeMillis());

        try {
            cnxn.sendResponse(hdr, rsp, "response");
            if (closeSession) {
                cnxn.sendCloseSession();
            }
        } catch (IOException e) {
            LOG.error("FIXMSG",e);
        }
    }
````

说明：对于processRequest函数，进行分段分析　　

```java
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request:: " + request);
        }
        // request.addRQRec(">final");
        long traceMask = ZooTrace.CLIENT_REQUEST_TRACE_MASK;
        if (request.type == OpCode.ping) { // 请求类型为PING
            traceMask = ZooTrace.SERVER_PING_TRACE_MASK;
        }
        if (LOG.isTraceEnabled()) {
            ZooTrace.logRequest(LOG, traceMask, 'E', request, "");
        }
```

说明：可以看到其主要作用是判断是否为PING请求，同时会根据LOG的设置确定是否进行日志记录，接着下面代码

```java
synchronized (zks.outstandingChanges) { // 同步块
            while (!zks.outstandingChanges.isEmpty()
                    && zks.outstandingChanges.get(0).zxid <= request.zxid) { // outstandingChanges不为空且首个元素的zxid小于等于请求的zxid
                // 移除首个元素
                ChangeRecord cr = zks.outstandingChanges.remove(0);
                if (cr.zxid < request.zxid) { // 若Record的zxid小于请求的zxid
                    LOG.warn("Zxid outstanding "
                            + cr.zxid
                            + " is less than current " + request.zxid);
                }
                if (zks.outstandingChangesForPath.get(cr.path) == cr) { // 根据路径得到Record并判断是否为cr
                    // 移除cr的路径对应的记录
                    zks.outstandingChangesForPath.remove(cr.path);
                }
            }
            if (request.hdr != null) { // 请求头不为空
                // 获取请求头
               TxnHeader hdr = request.hdr;
               // 获取请求事务
               Record txn = request.txn;
                // 处理事务
               rc = zks.processTxn(hdr, txn);
            }
            // do not add non quorum packets to the queue.
            if (Request.isQuorum(request.type)) { // 只将quorum包（事务性请求）添加进队列
                zks.getZKDatabase().addCommittedProposal(request);
            }
        }
```

说明：同步块处理，当outstandingChanges不为空且其首元素的zxid小于等于请求的zxid时，就会一直从outstandingChanges中取出首元素，并且对outstandingChangesForPath做相应的操作，若请求头不为空，则处理请求。若为事务性请求，则提交到ZooKeeper内存数据库中。对于processTxn函数而言，其最终会调用DataTree的processTxn，即内存数据库结构的DataTree的处理事务函数，而判断是否为事务性请求则是通过调用isQuorum函数，会改变服务器状态的（事务性）请求就是Quorum。之后调用addCommittedProposal函数将请求添加至ZKDatabase的committedLog结构中，方便follower快速同步。

　　接下来会根据请求的类型进行相应的操作，如对于PING请求而言，其处理如下　　

```java
            case OpCode.ping: { // PING请求
                // 更新延迟
                zks.serverStats().updateLatency(request.createTime);

                lastOp = "PING";
                // 更新响应的状态
                cnxn.updateStatsForResponse(request.cxid, request.zxid, lastOp,
                        request.createTime, System.currentTimeMillis());
                // 设置响应
                cnxn.sendResponse(new ReplyHeader(-2,
                        zks.getZKDatabase().getDataTreeLastProcessedZxid(), 0), null, "response");
                return;
            }
```

说明：其首先会根据请求的创建时间来更新Zookeeper服务器的延迟，updateLatency函数中会记录最大延迟、最小延迟、总的延迟和延迟次数。然后更新响应中的状态，如请求创建到响应该请求总共花费的时间、最后的操作类型等。然后设置响应后返回。而对于创建会话请求而言，其处理如下　　

```java
            case OpCode.createSession: { // 创建会话请求
                // 更新延迟
                zks.serverStats().updateLatency(request.createTime);
                
                lastOp = "SESS";
                // 更新响应的状态
                cnxn.updateStatsForResponse(request.cxid, request.zxid, lastOp,
                        request.createTime, System.currentTimeMillis());
                // 结束会话初始化
                zks.finishSessionInit(request.cnxn, true);
                return;
            }
```

说明：其首先还是会根据请求的创建时间来更新Zookeeper服务器的延迟，然后设置最后的操作类型，然后更新响应的状态，之后调用finishSessionInit函数表示结束会话的初始化。其他请求与此类似，之后会根据其他请求再次更新服务器的延迟，设置响应的状态等，最后使用sendResponse函数将响应发送给请求方，其处理流程如下　

```java
        // 获取最后处理的zxid
        long lastZxid = zks.getZKDatabase().getDataTreeLastProcessedZxid();
        // 响应头
        ReplyHeader hdr =
            new ReplyHeader(request.cxid, lastZxid, err.intValue());
        // 更新服务器延迟
        zks.serverStats().updateLatency(request.createTime);
        // 更新状态
        cnxn.updateStatsForResponse(request.cxid, lastZxid, lastOp,
                    request.createTime, System.currentTimeMillis());

        try {
            // 返回响应
            cnxn.sendResponse(hdr, rsp, "response");
            if (closeSession) {
                // 关闭会话
                cnxn.sendCloseSession();
            }
        } catch (IOException e) {
            LOG.error("FIXMSG",e);
        }
```

# **三、总结**

本篇博文分析了请求处理链的FinalRequestProcessor，其通常是请求处理链的最后一个处理器，而对于请求处理链部分的分析也就到这里，还有其他的处理器再使用时再进行分析

如果FinalRequestProcessor处理的事务性的请求 的话，就会将请求转给LearnerHandler处理，其源码解析[点击这里](../zookeeperServer之ProposalRequestProcessor源码解析/zookeeperServer之ProposalRequestProcessor源码解析.md)