# zookeeperServer之PrepRequestProcessor源码解析

## **一、前言**

　　前面学习了请求处理链的RequestProcessor父类，接着学习PrepRequestProcessor，其通常是请求处理链的第一个处理器。

## **二、PrepRequestProcessor源码分析**

### 2.1 类的继承关系　　

```java
public class PrepRequestProcessor extends Thread implements RequestProcessor {}
```

　　说明：可以看到PrepRequestProcessor继承了Thread类并实现了RequestProcessor接口，表示其可以作为线程使用。

### 2.2 类的属性

```java
public class PrepRequestProcessor extends Thread implements RequestProcessor {
    // 日志记录器
    private static final Logger LOG = LoggerFactory.getLogger(PrepRequestProcessor.class);

    // 是否跳过ACL,需查看系统配置
    static boolean skipACL;
    static {
        skipACL = System.getProperty("zookeeper.skipACL", "no").equals("yes");
        if (skipACL) {
            LOG.info("zookeeper.skipACL==\"yes\", ACL checks will be skipped");
        }
    }

    /**
     * this is only for testing purposes.
     * should never be useed otherwise
     */
    // 仅用作测试使用
    private static  boolean failCreate = false;

    // 已提交请求队列
    LinkedBlockingQueue<Request> submittedRequests = new LinkedBlockingQueue<Request>();

    // 下个处理器
    RequestProcessor nextProcessor;

    // Zookeeper服务器
    ZooKeeperServer zks;
}
```

　　说明：类的核心属性有submittedRequests和nextProcessor，前者表示已经提交的请求，而后者表示提交的下个处理器。

### 2.3 类的构造函数　　

```java
    public PrepRequestProcessor(ZooKeeperServer zks,
            RequestProcessor nextProcessor) {
        // 调用父类Thread构造函数
        super("ProcessThread(sid:" + zks.getServerId()
                + " cport:" + zks.getClientPort() + "):");
        // 类属性赋值
        this.nextProcessor = nextProcessor;
        this.zks = zks;
    }
```

　　说明：该构造函数首先会调用父类Thread的构造函数，然后利用构造函数参数给nextProcessor和zks赋值。

### 2.4 核心函数分析

　　1. run函数　

```java
    public void run() {
        try {
            while (true) { // 无限循环
                // 从队列中取出一个请求
                Request request = submittedRequests.take();
                // 
                long traceMask = ZooTrace.CLIENT_REQUEST_TRACE_MASK;
                if (request.type == OpCode.ping) { // 请求类型为PING
                    traceMask = ZooTrace.CLIENT_PING_TRACE_MASK;
                }
                if (LOG.isTraceEnabled()) { // 是否可追踪
                    ZooTrace.logRequest(LOG, traceMask, 'P', request, "");
                }
                if (Request.requestOfDeath == request) { // 在关闭处理器之后，会添加requestOfDeath，表示关闭后不再处理请求
                    break;
                }
                // 调用pRequest函数
                pRequest(request);
            }
        } catch (InterruptedException e) { // 中断异常
            LOG.error("Unexpected interruption", e);
        } catch (RequestProcessorException e) { // 请求处理异常
            if (e.getCause() instanceof XidRolloverException) {
                LOG.info(e.getCause().getMessage());
            }
            LOG.error("Unexpected exception", e);
        } catch (Exception e) { // 其他异常
            LOG.error("Unexpected exception", e);
        }
        LOG.info("PrepRequestProcessor exited loop!");
    }
```

　　说明：run函数是对Thread类run函数的重写，其核心逻辑相对简单，即不断从队列中取出request进行处理，其会调用pRequest函数，其源码如下　　

````java
protected void pRequest(Request request) throws RequestProcessorException {
        // LOG.info("Prep>>> cxid = " + request.cxid + " type = " +
        // request.type + " id = 0x" + Long.toHexString(request.sessionId));
        // 将请求的hdr和txn设置为null
        request.hdr = null;
        request.txn = null;
        
        try {
            switch (request.type) { // 确定请求类型
                case OpCode.create: // 创建节点请求
                // 新生创建节点请求
                CreateRequest createRequest = new CreateRequest();
                // 处理请求
                pRequest2Txn(request.type, zks.getNextZxid(), request, createRequest, true);
                break;
            case OpCode.delete: // 删除节点请求
                // 新生删除节点请求
                DeleteRequest deleteRequest = new DeleteRequest();               
                // 处理请求
                pRequest2Txn(request.type, zks.getNextZxid(), request, deleteRequest, true);
                break;
            case OpCode.setData: // 设置数据请求
                // 新生设置数据请求
                SetDataRequest setDataRequest = new SetDataRequest();                
                // 处理请求
                pRequest2Txn(request.type, zks.getNextZxid(), request, setDataRequest, true);
                break;
            case OpCode.setACL: // 设置ACL请求
                // 新生设置ACL请求
                SetACLRequest setAclRequest = new SetACLRequest();                
                // 处理请求
                pRequest2Txn(request.type, zks.getNextZxid(), request, setAclRequest, true);
                break;
            case OpCode.check: // 检查版本请求
                // 新生检查版本请求
                CheckVersionRequest checkRequest = new CheckVersionRequest();   
                // 处理请求
                pRequest2Txn(request.type, zks.getNextZxid(), request, checkRequest, true);
                break;
            case OpCode.multi: // 多重请求
                // 新生多重请求
                MultiTransactionRecord multiRequest = new MultiTransactionRecord();
                try {
                    // 将ByteBuffer转化为Record
                    ByteBufferInputStream.byteBuffer2Record(request.request, multiRequest);
                } catch(IOException e) {
                   // 出现异常则重新生成Txn头
                   request.hdr =  new TxnHeader(request.sessionId, request.cxid, zks.getNextZxid(),
                            zks.getTime(), OpCode.multi);
                   throw e;
                }
                List<Txn> txns = new ArrayList<Txn>();
                //Each op in a multi-op must have the same zxid!
                long zxid = zks.getNextZxid();
                KeeperException ke = null;

                //Store off current pending change records in case we need to rollback
                // 存储当前挂起的更改记录，以防我们需要回滚
                HashMap<String, ChangeRecord> pendingChanges = getPendingChanges(multiRequest);

                int index = 0;
                for(Op op: multiRequest) { // 遍历请求
                    Record subrequest = op.toRequestRecord() ;

                    /* If we've already failed one of the ops, don't bother
                     * trying the rest as we know it's going to fail and it
                     * would be confusing in the logfiles.
                     */
                    if (ke != null) { // 发生了异常
                        request.hdr.setType(OpCode.error);
                        request.txn = new ErrorTxn(Code.RUNTIMEINCONSISTENCY.intValue());
                    } 
                    
                    /* Prep the request and convert to a Txn */
                    else { // 未发生异常
                        try {
                            // 将Request转化为Txn
                            pRequest2Txn(op.getType(), zxid, request, subrequest, false);
                        } catch (KeeperException e) { // 转化发生异常
                            if (ke == null) {
                                ke = e;
                            }
                            // 设置请求头的类型
                            request.hdr.setType(OpCode.error);
                            // 设置请求的Txn
                            request.txn = new ErrorTxn(e.code().intValue());
                            LOG.info("Got user-level KeeperException when processing "
                                    + request.toString() + " aborting remaining multi ops."
                                    + " Error Path:" + e.getPath()
                                    + " Error:" + e.getMessage());
                            // 设置异常
                            request.setException(e);
 
                            /* Rollback change records from failed multi-op */
                            // 从多重操作中回滚更改记录
                            rollbackPendingChanges(zxid, pendingChanges);
                        }
                    }

                    //FIXME: I don't want to have to serialize it here and then
                    //       immediately deserialize in next processor. But I'm 
                    //       not sure how else to get the txn stored into our list.
                    // 序列化
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    BinaryOutputArchive boa = BinaryOutputArchive.getArchive(baos);
                    request.txn.serialize(boa, "request") ;
                    ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray());

                    txns.add(new Txn(request.hdr.getType(), bb.array()));
                    index++;
                }
                
                // 给请求头赋值
                request.hdr = new TxnHeader(request.sessionId, request.cxid, zxid, zks.getTime(), request.type);
                // 设置请求的Txn
                request.txn = new MultiTxn(txns);
                
                break;

            //create/close session don't require request record
            case OpCode.createSession: // 创建会话请求
            case OpCode.closeSession: // 关闭会话请求
                pRequest2Txn(request.type, zks.getNextZxid(), request, null, true);
                break;
 
            //All the rest don't need to create a Txn - just verify session
            // 所有以下请求只需验证会话即可
            case OpCode.sync: 
            case OpCode.exists:
            case OpCode.getData:
            case OpCode.getACL:
            case OpCode.getChildren:
            case OpCode.getChildren2:
            case OpCode.ping:
            case OpCode.setWatches: 
                zks.sessionTracker.checkSession(request.sessionId,
                        request.getOwner());
                break;
            }
        } catch (KeeperException e) { // 发生KeeperException异常
            if (request.hdr != null) {
                request.hdr.setType(OpCode.error);
                request.txn = new ErrorTxn(e.code().intValue());
            }
            LOG.info("Got user-level KeeperException when processing "
                    + request.toString()
                    + " Error Path:" + e.getPath()
                    + " Error:" + e.getMessage());
            request.setException(e);
        } catch (Exception e) { // 其他异常
            // log at error level as we are returning a marshalling
            // error to the user
            LOG.error("Failed to process " + request, e);

            StringBuilder sb = new StringBuilder();
            ByteBuffer bb = request.request;
            if(bb != null){
                bb.rewind();
                while (bb.hasRemaining()) {
                    sb.append(Integer.toHexString(bb.get() & 0xff));
                }
            } else {
                sb.append("request buffer is null");
            }

            LOG.error("Dumping request buffer: 0x" + sb.toString());
            if (request.hdr != null) {
                request.hdr.setType(OpCode.error);
                request.txn = new ErrorTxn(Code.MARSHALLINGERROR.intValue());
            }
        }
        // 给请求的zxid赋值
        request.zxid = zks.getZxid();
        // 传递给下个处理器进行处理
        nextProcessor.processRequest(request);
    }
````

　　说明：pRequest会确定请求类型，并根据请求类型不同生成不同的请求对象，然后调用pRequest2Txn函数，其源码如下　　

````java
protected void pRequest2Txn(int type, long zxid, Request request, Record record, boolean deserialize)
        throws KeeperException, IOException, RequestProcessorException
    {
        // 新生事务头
        request.hdr = new TxnHeader(request.sessionId, request.cxid, zxid,
                                    zks.getTime(), type);

        switch (type) { // 确定类型
            case OpCode.create: // 创建节点操作
                // 检查会话，检查会话持有者是否为该owner
                zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                // 向下转化
                CreateRequest createRequest = (CreateRequest)record;   
                if(deserialize) // 反序列化，将ByteBuffer转化为Record
                    ByteBufferInputStream.byteBuffer2Record(request.request, createRequest);
                // 获取节点路径
                String path = createRequest.getPath();
                // 索引最后一个'/'
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash == -1 || path.indexOf('\0') != -1 || failCreate) { // 判断最后一个'/'是否合法
                    LOG.info("Invalid path " + path + " with session 0x" +
                            Long.toHexString(request.sessionId));
                    throw new KeeperException.BadArgumentsException(path);
                }
                // 移除重复的ACL项
                List<ACL> listACL = removeDuplicates(createRequest.getAcl());
                if (!fixupACL(request.authInfo, listACL)) { // 确保ACL列表不为空
                    throw new KeeperException.InvalidACLException(path);
                }
                // 提取节点的父节点路径
                String parentPath = path.substring(0, lastSlash);
                // 获取父节点的Record
                ChangeRecord parentRecord = getRecordForPath(parentPath);
                // 检查ACL列表
                checkACL(zks, parentRecord.acl, ZooDefs.Perms.CREATE,
                        request.authInfo);
                // 获取父节点的Record的子节点版本号
                int parentCVersion = parentRecord.stat.getCversion();
                // 获取创建模式
                CreateMode createMode =
                    CreateMode.fromFlag(createRequest.getFlags());
                if (createMode.isSequential()) { // 顺序模式
                    // 在路径后添加一串数字
                    path = path + String.format(Locale.ENGLISH, "%010d", parentCVersion);
                }
                try {
                    // 验证路径
                    PathUtils.validatePath(path);
                } catch(IllegalArgumentException ie) {
                    LOG.info("Invalid path " + path + " with session 0x" +
                            Long.toHexString(request.sessionId));
                    throw new KeeperException.BadArgumentsException(path);
                }
                try {
                    if (getRecordForPath(path) != null) {
                        throw new KeeperException.NodeExistsException(path);
                    }
                } catch (KeeperException.NoNodeException e) {
                    // ignore this one
                }
                // 父节点是否为临时节点
                boolean ephemeralParent = parentRecord.stat.getEphemeralOwner() != 0;
                if (ephemeralParent) { // 父节点为临时节点
                    throw new KeeperException.NoChildrenForEphemeralsException(path);
                }
                // 新的子节点版本号
                int newCversion = parentRecord.stat.getCversion()+1;
                // 新生事务
                request.txn = new CreateTxn(path, createRequest.getData(),
                        listACL,
                        createMode.isEphemeral(), newCversion);
                // 
                StatPersisted s = new StatPersisted();
                if (createMode.isEphemeral()) { // 创建节点为临时节点
                    s.setEphemeralOwner(request.sessionId);
                }
                // 拷贝
                parentRecord = parentRecord.duplicate(request.hdr.getZxid());
                // 子节点数量加1
                parentRecord.childCount++;
                // 设置新的子节点版本号
                parentRecord.stat.setCversion(newCversion);
                // 将parentRecord添加至outstandingChanges和outstandingChangesForPath中
                addChangeRecord(parentRecord);
                // 将新生成的ChangeRecord(包含了StatPersisted信息)添加至outstandingChanges和outstandingChangesForPath中
                addChangeRecord(new ChangeRecord(request.hdr.getZxid(), path, s,
                        0, listACL));
                break;
            case OpCode.delete: // 删除节点请求
                // 检查会话，检查会话持有者是否为该owner
                zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                // 向下转化为DeleteRequest
                DeleteRequest deleteRequest = (DeleteRequest)record;
                if(deserialize) // 反序列化，将ByteBuffer转化为Record
                    ByteBufferInputStream.byteBuffer2Record(request.request, deleteRequest);
                // 获取节点路径
                path = deleteRequest.getPath();
                // 索引最后一个'/'
                lastSlash = path.lastIndexOf('/');
                if (lastSlash == -1 || path.indexOf('\0') != -1
                        || zks.getZKDatabase().isSpecialPath(path)) {
                    throw new KeeperException.BadArgumentsException(path);
                }
                // 提取节点的父节点路径
                parentPath = path.substring(0, lastSlash);
                // 获取父节点的Record
                parentRecord = getRecordForPath(parentPath);
                // 获取节点的Record
                ChangeRecord nodeRecord = getRecordForPath(path);
                // 检查ACL列表
                checkACL(zks, parentRecord.acl, ZooDefs.Perms.DELETE,
                        request.authInfo);
                // 获取版本
                int version = deleteRequest.getVersion();
                if (version != -1 && nodeRecord.stat.getVersion() != version) {
                    throw new KeeperException.BadVersionException(path);
                }
                if (nodeRecord.childCount > 0) { // 该结点有子节点，抛出异常
                    throw new KeeperException.NotEmptyException(path);
                }
                // 新生删除事务
                request.txn = new DeleteTxn(path);
                // 拷贝父节点Record
                parentRecord = parentRecord.duplicate(request.hdr.getZxid());
                // 父节点的孩子节点数目减1
                parentRecord.childCount--;
                // // 将parentRecord添加至outstandingChanges和outstandingChangesForPath中
                addChangeRecord(parentRecord);
                // 将新生成的ChangeRecord(包含了StatPersisted信息)添加至outstandingChanges和outstandingChangesForPath中
                addChangeRecord(new ChangeRecord(request.hdr.getZxid(), path,
                        null, -1, null));
                break;
            case OpCode.setData: // 设置数据请求
                // 检查会话，检查会话持有者是否为该owner
                zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                // 向下转化
                SetDataRequest setDataRequest = (SetDataRequest)record;
                if(deserialize) // 反序列化，将ByteBuffer转化为Record
                    ByteBufferInputStream.byteBuffer2Record(request.request, setDataRequest);
                // 获取节点路径
                path = setDataRequest.getPath();
                // 获取节点的Record
                nodeRecord = getRecordForPath(path);
                // 检查ACL列表
                checkACL(zks, nodeRecord.acl, ZooDefs.Perms.WRITE,
                        request.authInfo);
                // 获取请求的版本号
                version = setDataRequest.getVersion();
                // 节点当前版本号
                int currentVersion = nodeRecord.stat.getVersion();
                if (version != -1 && version != currentVersion) {
                    throw new KeeperException.BadVersionException(path);
                }
                // 新生版本号
                version = currentVersion + 1;
                // 新生设置数据事务
                request.txn = new SetDataTxn(path, setDataRequest.getData(), version);
                // 拷贝
                nodeRecord = nodeRecord.duplicate(request.hdr.getZxid());
                // 设置版本号
                nodeRecord.stat.setVersion(version);
                // 将nodeRecord添加至outstandingChanges和outstandingChangesForPath中
                addChangeRecord(nodeRecord);
                break;
            case OpCode.setACL: // 设置ACL请求
                // 检查会话，检查会话持有者是否为该owner
                zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                // 向下转化
                SetACLRequest setAclRequest = (SetACLRequest)record;
                if(deserialize) // 反序列化，将ByteBuffer转化为Record
                    ByteBufferInputStream.byteBuffer2Record(request.request, setAclRequest);
                // 获取节点路径
                path = setAclRequest.getPath();
                // 移除重复的ACL项
                listACL = removeDuplicates(setAclRequest.getAcl());
                if (!fixupACL(request.authInfo, listACL)) { // 确保ACL列表不为空
                    throw new KeeperException.InvalidACLException(path);
                }
                // 获取节点的Record
                nodeRecord = getRecordForPath(path);
                // 检查ACL列表
                checkACL(zks, nodeRecord.acl, ZooDefs.Perms.ADMIN,
                        request.authInfo);
                // 获取版本号
                version = setAclRequest.getVersion();
                // 当前版本号
                currentVersion = nodeRecord.stat.getAversion();
                if (version != -1 && version != currentVersion) { // 验证版本号
                    throw new KeeperException.BadVersionException(path);
                }
                // 新生版本号
                version = currentVersion + 1;
                // 设置请求事务
                request.txn = new SetACLTxn(path, listACL, version);
                // 拷贝
                nodeRecord = nodeRecord.duplicate(request.hdr.getZxid());
                // 设置ACL版本号
                nodeRecord.stat.setAversion(version);
                // 将nodeRecord添加至outstandingChanges和outstandingChangesForPath中
                addChangeRecord(nodeRecord);
                break;
            case OpCode.createSession: // 创建会话请求
                // 将request缓冲区rewind
                request.request.rewind();
                // 获取缓冲区大小
                int to = request.request.getInt();
                // 创建会话事务
                request.txn = new CreateSessionTxn(to);
                // 再次将request缓冲区rewind
                request.request.rewind();
                // 添加session
                zks.sessionTracker.addSession(request.sessionId, to);
                // 设置会话的owner
                zks.setOwner(request.sessionId, request.getOwner());
                break;
            case OpCode.closeSession: // 关闭会话请求
                // We don't want to do this check since the session expiration thread
                // queues up this operation without being the session owner.
                // this request is the last of the session so it should be ok
                //zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                // 获取会话所有的临时节点
                HashSet<String> es = zks.getZKDatabase()
                        .getEphemerals(request.sessionId);
                synchronized (zks.outstandingChanges) {
                    for (ChangeRecord c : zks.outstandingChanges) { // 遍历outstandingChanges队列的所有ChangeRecord
                        if (c.stat == null) { // 若其stat为null
                            // Doing a delete
                            // 则从es中移除其路径
                            es.remove(c.path);
                        } else if (c.stat.getEphemeralOwner() == request.sessionId) { // 若临时节点属于该会话
                            // 则将其路径添加至es中
                            es.add(c.path);
                        }
                    }
                    for (String path2Delete : es) { // 遍历es
                        // 新生ChangeRecord，并将其添加至outstandingChanges和outstandingChangesForPath中
                        addChangeRecord(new ChangeRecord(request.hdr.getZxid(),
                                path2Delete, null, 0, null));
                    }
                    
                    // 关闭会话
                    zks.sessionTracker.setSessionClosing(request.sessionId);
                }

                LOG.info("Processed session termination for sessionid: 0x"
                        + Long.toHexString(request.sessionId));
                break;
            case OpCode.check: // 检查请求
                // 检查会话，检查会话持有者是否为该owner
                zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
                // 向下转化
                CheckVersionRequest checkVersionRequest = (CheckVersionRequest)record;
                if(deserialize) // 反序列化，将ByteBuffer转化为Record
                    ByteBufferInputStream.byteBuffer2Record(request.request, checkVersionRequest);
                // 获取节点路径
                path = checkVersionRequest.getPath();
                // 获取节点的Record
                nodeRecord = getRecordForPath(path);
                // 检查ACL列表
                checkACL(zks, nodeRecord.acl, ZooDefs.Perms.READ,
                        request.authInfo);
                // 获取版本号
                version = checkVersionRequest.getVersion();
                // 当前版本号
                currentVersion = nodeRecord.stat.getVersion();
                if (version != -1 && version != currentVersion) { // 验证版本号
                    throw new KeeperException.BadVersionException(path);
                }
                // 新生版本号
                version = currentVersion + 1;
                // 新生请求的事务
                request.txn = new CheckVersionTxn(path, version);
                break;
        }
    }
````

说明：pRequest2Txn会根据不同的请求类型进行不同的验证，如对创建节点而言，其会进行会话验证，ACL列表验证，节点路径验证及判断创建节点的类型（顺序节点、临时节点等）而进行不同操作，同时还会使父节点的子节点数目加1，之后会再调用addChangeRecord函数将ChangeRecord添加至ZooKeeperServer的outstandingChanges和outstandingChangesForPath中。

在pRequest函数中，如果请求类型是多重操作，那么会调用getPendingChanges函数，其会获取挂起的更改，其源码如下

```java
    HashMap<String, ChangeRecord> getPendingChanges(MultiTransactionRecord multiRequest) {
        HashMap<String, ChangeRecord> pendingChangeRecords = new HashMap<String, ChangeRecord>();
        
        for(Op op: multiRequest) { // 
            String path = op.getPath();

            try {
                // 获取path对应的ChangeRecord
                ChangeRecord cr = getRecordForPath(path);
                if (cr != null) { 
                    pendingChangeRecords.put(path, cr);
                }
                /*
                 * ZOOKEEPER-1624 - We need to store for parent's ChangeRecord
                 * of the parent node of a request. So that if this is a
                 * sequential node creation request, rollbackPendingChanges()
                 * can restore previous parent's ChangeRecord correctly.
                 *
                 * Otherwise, sequential node name generation will be incorrect
                 * for a subsequent request.
                 */
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash == -1 || path.indexOf('\0') != -1) {
                    continue;
                }
                // 提取节点的父节点路径
                String parentPath = path.substring(0, lastSlash);
                // 获取父节点的Record
                ChangeRecord parentCr = getRecordForPath(parentPath);
                if (parentCr != null) {
                    pendingChangeRecords.put(parentPath, parentCr);
                }
            } catch (KeeperException.NoNodeException e) {
                // ignore this one
            }
        }
        
        return pendingChangeRecords;
    }
```

　　说明：可以看到在函数中，会遍历多重操作，针对每个操作，通过其路径获取对应的Record，然后添加至pendingChangeRecords，然后对其父节点进行相应操作，之后返回，其中会调用getRecordForPath函数，其源码如下　

```java
    ChangeRecord getRecordForPath(String path) throws KeeperException.NoNodeException {
        ChangeRecord lastChange = null;
        synchronized (zks.outstandingChanges) { // 同步块
            // 先从outstandingChangesForPath队列中获取
            lastChange = zks.outstandingChangesForPath.get(path);
            /*
            for (int i = 0; i < zks.outstandingChanges.size(); i++) {
                ChangeRecord c = zks.outstandingChanges.get(i);
                if (c.path.equals(path)) {
                    lastChange = c;
                }
            }
            */
            if (lastChange == null) { // 若在outstandingChangesForPath中未获取到，则从数据库中获取
                DataNode n = zks.getZKDatabase().getNode(path);
                if (n != null) { // 节点存在
                    Long acl;
                    Set<String> children;
                    synchronized(n) {
                        acl = n.acl;
                        children = n.getChildren();
                    }
                    // 新生ChangeRecord
                    lastChange = new ChangeRecord(-1, path, n.stat,
                        children != null ? children.size() : 0,
                            zks.getZKDatabase().convertLong(acl));
                }
            }
        }
        if (lastChange == null || lastChange.stat == null) { // 抛出异常
            throw new KeeperException.NoNodeException(path);
        }
        return lastChange;
    }
```

　　说明：其表示根据节点路径获取节点的Record，其首先会从outstandingChangesForPath中获取路径对应的Record，若未获取成功，则从Zookeeper数据库中获取，若还未存在，则抛出异常。

　　2. processResult函数　

```java
    public void processRequest(Request request) {
        // request.addRQRec(">prep="+zks.outstandingChanges.size());
        // 将请求添加至队列中
        submittedRequests.add(request);
    }
```

　　说明：该函数是对父接口函数的实现，其主要作用是将请求添加至submittedRequests队列进行后续处理（run函数中）。

## **三、总结**

　　针对PrepRequestProcessor的源码就分析到这里，其完成的业务逻辑也相对简单，其通常是处理链的第一个处理器

关于PrepRequestProcessor的下一个处理器SyncRequestProcessor的源码解析，[点击此处](../zookeeperServer之SyncRequestProcessor源码解析/zookeeperServer之SyncRequestProcessor源码解析.md)查看。