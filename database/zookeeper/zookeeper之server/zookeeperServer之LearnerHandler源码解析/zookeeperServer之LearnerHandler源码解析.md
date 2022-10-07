# zookeeperServer之LearnerHandler源码解析

## 一、前言

为了保证整个集群内部的实时通信，同时为了确保可以控制所有的Follower/Observer服务器，Leader服务器会与每个Follower/Observer服务器建立一个TCP长连接。同时也会为每个Follower/Observer服务器创建一个名为LearnerHandler的实体。LearnerHandler是Learner服务器的管理者，主要负责Follower/Observer服务器和Leader服务器之间的一系列网络通信，包括数据同步、请求转发和Proposal提议的投票等。Leader服务器中保存了所有Follower/Observer对应的LearnerHandler。

* LearnerHandler主要是处理Leader和Learner之间的交互.
* Leader和每个Learner连接会维持一个长连接，并有一个单独的LearnerHandler线程和一个Learner进行交互
* 可以认为，Learner和LearnerHandler是一一对应的关系.

## 二、LearnerHandler源码解析

### SyncLimitCheck

SyncLimitCheck ： 作用就是控制leader等待当前learner给proposal回复ACK的时间

在Leader发出proposal时更新对应时间，zxid记录

在Leader收到对应ACK时，清除对应zxid的记录

检查时，判断当前时间和最早已经发出proposal但是没有收到ack的时间对比，看是否超时

````java
    private class SyncLimitCheck 
        private boolean started = false;
        private long currentZxid = 0;//最久一次更新了但是没有收到ack的proposal的zxid
        private long currentTime = 0;//最久一次更新了但是没有收到ack的proposal的时间
        private long nextZxid = 0;//最新一次更新了但是没有收到ack的proposal的zxid
        private long nextTime = 0;//最新一次更新了但是没有收到ack的proposal的时间

        public synchronized void start() {//启动同步超时检测
            started = true;
        }

        public synchronized void updateProposal(long zxid, long time) {//发送proposal时，更新提议的统计时间
            if (!started) {
                return;
            }
            if (currentTime == 0) {//如果还没初始化就初始化
                currentTime = time;
                currentZxid = zxid;
            } else {//如果已经初始化，就记录下一次的时间
                nextTime = time;
                nextZxid = zxid;
            }
        }

        public synchronized void updateAck(long zxid) {//收到Learner关于zxid的ack了，更新ack的统计时间
             if (currentZxid == zxid) {//如果是刚刚发送的ack
                 currentTime = nextTime;//传递到下一个记录
                 currentZxid = nextZxid;
                 nextTime = 0;
                 nextZxid = 0;
             } else if (nextZxid == zxid) {//如果旧的ack还没收到 但是收到了 新的ack
                 LOG.warn("ACK for " + zxid + " received before ACK for " + currentZxid + "!!!!");
                 nextTime = 0;
                 nextZxid = 0;
             }
        }

        public synchronized boolean check(long time) {//如果没有等待超时，返回true
            if (currentTime == 0) {
                return true;
            } else {
                long msDelay = (time - currentTime) / 1000000;//当前时间与最久一次没收到ack的proposal的时间差
                return (msDelay < (leader.self.tickTime * leader.self.syncLimit));
            }
        }
    }
````

除了IO，sock以外，其余部分源码如下

````java
    final Leader leader;//对应Leader角色

    /** Deadline for receiving the next ack. If we are bootstrapping then
     * it's based on the initLimit, if we are done bootstrapping it's based
     * on the syncLimit. Once the deadline is past this learner should
     * be considered no longer "sync'd" with the leader. */
    volatile long tickOfNextAckDeadline;//下一个接收ack的deadline，启动时(数据同步)是一个标准，完成启动后(正常交互)，是另一个标准
    
    /**
     * ZooKeeper server identifier of this learner
     */
    protected long sid = 0;//当前这个learner的sid

    protected int version = 0x1;//当前这个learner的version

    final LinkedBlockingQueue<QuorumPacket> queuedPackets =
        new LinkedBlockingQueue<QuorumPacket>();//待发送packet的队列

    private SyncLimitCheck syncLimitCheck = new SyncLimitCheck();//proposal，ack检测

    final QuorumPacket proposalOfDeath = new QuorumPacket();//代表一个关闭shutdown的packet来关闭发送packet的线程

    private LearnerType  learnerType = LearnerType.PARTICIPANT;//默认的learner类型（也叫Follower）,也可以设置为OBSERVER
````

