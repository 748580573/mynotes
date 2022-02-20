# 核心篇：Redisson 是如何用 Redis 来实现一把分布式锁的？

从上一篇MySQL如何实现分布式锁文章开始我们就已经正式步入了“coding”环节，将之前的理论和原理进行落地，MySQL实现分布式锁的原理我们也已经分析过了，他的弊大于利，所以企业级一般是不采取MySQL来做分布式锁的，那企业中都用什么呢？大多数会选择Redis和Zookeeper，所以接下来我将会花几篇的时间来分析Redis实现分布式锁的每一个细节以及核心思想以及Redisson这个框架是如何落地实现的。

本篇文章为Redis实现分布式锁的第一篇，而Redis实现分布式锁的核心思想以及关键点我们之前文章已经分析过，所以这篇我会带着之前的原理以及思想来深度剖析下Redisson是如何落地实现的。

在开始之前我们先看下加锁解锁的Redisson分布式锁的Demo：

# 一、Redisson#Demo

```
// 1. Create config object
Config config = new Config();
config.useClusterServers()
       // use "rediss://" for SSL connection
      .addNodeAddress("redis://127.0.0.1:7181")
      .addNodeAddress("redis://127.0.0.1:7182")
      .addNodeAddress("redis://127.0.0.1:7183");

// 2. Create Redisson instance
// Sync and Async API
RedissonClient redisson = Redisson.create(config);
RLock lock = redisson.getLock("myLock");

// 3.加锁
lock.lock();
try {
  ...
} finally {
    // 4.解锁
    lock.unlock();  
}
```

使用很简单，分为三步骤：

- 配置Redis，我们这里配置了一个集群，集群内有三台Redis。
- 加锁（`lock.lock()`）
- 解锁（`lock.unlock()`）

一句话概括这个简单的流程：**获取配置、加锁、解锁。**

所以可以发现Redissson实在太友好了，加锁解锁好像并没有我们之前分析的那么复杂，就这三步骤，但是，lock和unlock对于我们是个“黑盒”，我们不知道他内部怎么实现的，我们只是用了他的api，这是远远不够的，我们不充当api工程师，接下来我们就逐个分析这三步，一层层揭开这些“黑盒”的真面目。先来看看第一步：**配置Redis**。

# 二、底层剖析

第一步配置Redis，我不打算用大量篇幅来讲解，不仅没营养还影响阅读体验，因为他的核心思想太简单了，你想想，如何你要使用Redis你怎么办？你第一步肯定要和RedisServer建立链接，链接建立成功后才能进行api操作，所以第一步配置Redis其实核心就是和RedisServer进行网络通信建立链接。链接建立完成实例化一个RedissonClient对象供业务系统方便的使用它的api，所以核心在于各种api的底层原理，他们是个“黑盒”，就比如：加锁，我们只需要调用一个lock(),那这个lock()干了什么？他的实现原理是不是我们之前分析Redis实现分布式锁时候那些关键点？所以我们需要揭开这个“黑盒”，先从lock()开始，看看他底层到底是如何加锁的？

## 1. lock()

先来回顾下Redis作为分布式锁的关键点，有四个：**原子性、过期时间、锁续期**和**正确释放锁**。

我们之前介绍了至少具备两大特性：**可重入**以及**互斥性**。

接下来我们在剖析源码，看看是否符合这四个关键点呢？可重入和互斥性是怎么做的呢？

```
// 省略了复杂的调用链，只拿出核心的代码来剖析。直奔主题
<T> RFuture<T> tryLockInnerAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId, RedisStrictCommand<T> command) {
    return evalWriteAsync(getRawName(), LongCodec.INSTANCE, command,
                          "if (redis.call('exists', KEYS[1]) == 0) then " +
                          "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                          "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                          "return nil; " +
                          "end; " +
                          "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                          "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                          "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                          "return nil; " +
                          "end; " +
                          "return redis.call('pttl', KEYS[1]);",
                          Collections.singletonList(getRawName()), unit.toMillis(leaseTime), getLockName(threadId));
}
```

乍一看，嚯，这是一段完全看不懂的lua脚本，而我们在之前篇幅讲解**原子性**的时候说过，Redis可以用lua脚本来保证多个命令的原子性，那这里好像是符合原子性这一特性的，但是这段lua的原理是怎样的？看起来很吓人，但是别慌，我接下来逐个拆解，把它变成通俗易懂的“人话”。

我给他拆解成三段，先来看**第一段：**

```
"if (redis.call('exists', KEYS[1]) == 0) then " +
"redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
"redis.call('pexpire', KEYS[1], ARGV[1]); " +
"return nil; " +
"end; " +
```

首先我们就看到了KEYS[1]这个东西，这个是什么呢？这个就是我们最上面Demo中的myLock，对应到Demo中的代码如下：

```
RLock lock = redisson.getLock("myLock");
```

所以这段lua的流程如下：

- 判断myLock这把锁是否存在，如果不存在则

  1. 利用命令`hincrby myLock ARGV[2] 1`来进行设置锁，注意这里**采取了hash结构**，key是myLock，map对象是ARGV[2]:1。

  1. 设置过期时间为ARGV[1]

  1. 返回null代表成功

这是不是有点意思了？首先他采取了Redis的hash数据结构，变成我们看得懂的格式就是下面这样子的：

```
myLock: {
    ARGV[2]:1
}
```

看到这里，我知道你有三个疑问：

1. ARGV[2]是什么？

1. ARGV[1]又是什么？
2. ARGV[2]后面的1又是什么呢？

下面一个个解答：

1. ARGV[2]是一个由`uuid:threadId`组成的一个唯一的数值。为什么不直接用threadId？之前文章也说过，这里温习下，是因为现在都是集群部署，threadId在不同机器上是可以重复的，所以会出问题。
2. ARGV[1]猜也能猜到了，跟在了`pexpire`命令后面，那肯定是过期时间了，是的，在构造器里初始化的，默认是30s过期时间。
3. ARGV[2]后面的1又是什么呢？难倒是1代表成功，0代表失败吗？你要这么想就错了，我直接判断key是否存在不就知道有没有锁了吗？为啥要判断这个，那么这个1是干嘛的呢？还记得我们之前分析的锁重入概念吗？这里的1就代表锁次数，下次重入就会变成2。

所以就形成了更通俗易懂的代码如下：

```
lua原子操作 {
    hincrby, myLock, uuid:threadId, 1
    pexpire, myLock, 30s 
}
```

总结**第一段**的完整流程图：

![](./imgs/1.png) 上面分析的是第一段，也就是不包含myLock这把锁的情况下会走上面的流程，那如果myLock这把锁存在是什么情况呢？如果myLock存在，那么会走**第二段：**

```
"if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
"redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
"redis.call('pexpire', KEYS[1], ARGV[1]); " +
"return nil; " +
"end; " +
```

有了第一段的基础，这段代码就不那么恐怖了吧，起码我们知道`KEYS[1]、ARGV[1]、ARGV[2]`这三个参数是什么含义了。直接进入正题，看第二段的流程：

- 如果存在锁myLock:[uuid:threadId]，则

  1. 利用命令`hincrby myLock threadId 1`来给锁的value值自增1，也就是变成了key是myLock，map对象是[uuid:threadId]:2。

  1. 设置过期时间为30s，相当于续期

  1. 最后返回key的过期时间。

  1. 返回null代表成功

那我们分析下，什么时候会执行第二段lua脚本？只有相同线程抢占两次锁才会进入第二段，一个方法里lock两次的场景，也就是**锁重入**的时候，重入的逻辑也很简单，就是hincrby给锁次数+1，然后重新续期，比如：线程1加锁了且重入了一次，那就是如下效果：

```
lua原子操作 {
    hincrby, myLock, uuid:线程1, 2
    pexpire, myLock, 30s 
}
```

总结**前两段**的完整流程图：



![Redisson-可重入锁.png](./imgs/2.png) 

至此我们分析完了如果没锁的情况是如何加锁的以及有锁且持有锁的线程就是当前请求线程然后进行锁重入的情况，那如果有锁，但持有锁的线程不是当前请求线程呢？这时候怎么处理？这就是**第三段：**

```
"return redis.call('pttl', KEYS[1]);"
```

- 如果有锁myLock，但是myLock不是当前机器加的或者不是当前线程加的（也就是myLock对应的mapKey不是uuid:threadId），那么就返回剩余时间。返回剩余时间有啥用？在这段lua代码的外层用到了，外层判断返回过期时间了就死循环重试，相当于抢锁失败，重试。核心代码如下：

```
// 对照源代码进行了修改，省去了那些无关重要的代码
private void lock(long leaseTime, TimeUnit unit, boolean interruptibly) throws InterruptedException {
    long threadId = Thread.currentThread().getId();
    // tryAcquire就是上面分析的lua完整脚本
    Long ttl = tryAcquire(-1, leaseTime, unit, threadId);
    // 返回null就代表上锁成功。别问我为啥设置成null是成功，我也不知道。。。
    if (ttl == null) {
        return;
    }
    // 如果没成功，也就是锁的剩余时间不是null的话，那么就执行下面的逻辑（省略了非此次探讨的核心代码）
    // 其实就是说 如果有锁（锁剩余时间不是null），那就死循环等待重新抢锁。
    try {
        while (true) {
            // 重新抢锁
            ttl = tryAcquire(-1, leaseTime, unit, threadId);
            // 抢锁成功就break退出循环
            if (ttl == null) {
                break;
            }
            // 省略一些代码
        }
    } finally {}
}
```

流程很清晰易懂了，一段话总结下：**执行lua，然后判断返回值是不是null，如果返回值是null那就代表上锁成功，直接return，如果返回值不是null（也就是对应我们上面lua第三段，返回的是锁剩余过期时间），那就代表互斥了，加锁失败，需要重试。**

![Redisson-互斥性.png](./imgs/3.png)

 **总结：**

一共包含三个知识点：**采取的hash数据结构、支持锁重入、很好的解决了互斥性**。核心流程如下：

- 请求进来判断当前执行的方法有没有上锁，如果没有上锁则进行上锁，上锁成功后返回nil。
- 如果当前执行的方法已经有锁了，那就看持有锁的线程是不是自己，是自己就代表重入，直接给锁状态hincrby + 1，然后返回nil代表成功。
- 如果当前执行的方法已经有锁但持有锁的线程不是当前请求的线程，那则代表竞争抢锁了，需要互斥，就pttl命令返回当前锁的剩余时间，然后外层进行等待重试。

加锁的底层实现原理我们已经剖析的很透彻了，也可以发现Redisson的lock()符合我们分布式锁的关键特征：**原子性以及过期时间**。也带有**可重入以及互斥性**的核心功能。锁加完了，执行完业务后是不是要释放？那释放锁的unlock()这个“黑盒”的底层是如何实现的呢？

## 2. unlock()

unlock()释放锁相对lock()加锁就简单太多了，但是肯定不是你想的那样直接`del key`，因为有锁重入的情况，所以需要判断是否有锁重入，如果有锁重入那就先把锁重入次数减1，当没有锁重入的时候直接删除锁即可，核心源码如下：

```
protected RFuture<Boolean> unlockInnerAsync(long threadId) {
    return evalWriteAsync(getRawName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                          "if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then " +
                          "return nil;" +
                          "end; " +
                          "local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); " +
                          "if (counter > 0) then " +
                          "redis.call('pexpire', KEYS[1], ARGV[2]); " +
                          "return 0; " +
                          "else " +
                          "redis.call('del', KEYS[1]); " +
                          "redis.call('publish', KEYS[2], ARGV[1]); " +
                          "return 1; " +
                          "end; " +
                          "return nil;",
                          Arrays.asList(getRawName(), getChannelName()), LockPubSub.UNLOCK_MESSAGE, internalLockLeaseTime, getLockName(threadId));
}
```

有了上面加锁的铺垫，在看这段解锁的lua不懵了吧？大同小异，所以我就不逐段拆解了，直接说下核心流程：

- 如果当前线程不持有锁则return nil，因为根本无锁（可能锁过期了，`lock(time,unit)`带时间的没有watchDog机制），无需释放，所以直接返回。

- 如果当前线程持有锁，则

  1. 给锁的value-1：`hincrby, myLock, uuid:threadId, -1`，为啥不直接删了？因为有锁重入的情况呀！

  1. 减完1后值还大于0，那代表是有重入的，还没释放完，那不能del掉，重新给他续期。`pexpire, myLock, 30s(默认30s)`

  1. 如果减完1不大于0了，那就代表释放完了，没重入的情况了，所以直接del掉就好了，然后通知订阅者（redis的发布订阅模式pub/sub）

  1. 返回1，代表解锁成功。

同加锁一样，我也把它整理一张图：

![Redisson-解锁.png](./imgs/4.png)

 细心的小伙伴早就发现加锁的流程图中有根据锁key找到slot的流程，这段流程是什么意思呢？我们Demo是一个三台Redis节点组成的集群，那我们lock()的时候是给哪台机器加锁呢？

## 3. 给哪台机器加锁？

答案是：只给一台加锁，Redis集群分成16384个slot，我们这里是3台，所以除以3，也就是3台平均分配这些slot，然后根据这个锁key算出hash值看看落在那个slot槽上，也就找到了那台机器，然后只给那一台机器加锁。源码如下：

```
private NodeSource getNodeSource(String key) {
    // 计算key所属的slot
    int slot = connectionManager.calcSlot(key);
    // 然后根据slot选择机器
    return new NodeSource(slot);
}
```

# 三、总结

此篇为Redis实现分布式锁的核心、基石。是必须要掌握的东西，尤其是对如何加锁、如何锁重入的、采取什么数据结构来完成的、如何互斥的以及如何解锁的，这每一个点都是核心，都是Redis实现分布式锁必备的东西。完整流程就是加锁的最后一个图以及释放锁的图，由于图太大了，占篇幅，这里不重复贴了。大家一定要仔细反复看这篇文章，掌握后会发现很简单，没有那么难。下面对这篇文章做个收尾，核心就是加锁三段lua+释放锁流程：

**加锁lua：**

```
"if (redis.call('exists', KEYS[1]) == 0) then " +
"redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
"redis.call('pexpire', KEYS[1], ARGV[1]); " +
"return nil; " +
"end; " +
```

- 如果当前请求的方法没有其他线程上锁，那直接加锁且续期。锁数据结构采取hash类型。

**重入lua**

```
"if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
"redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
"redis.call('pexpire', KEYS[1], ARGV[1]); " +
"return nil; " +
"end; " +
```

- 如果当前方法有锁，且持有锁的线程是当前请求线程，那就代表锁重入，直接给锁状态+1且重新续期即可。

**互斥lua**

```
"return redis.call('pttl', KEYS[1]);"
```

- 如果当前方法有锁，且持有锁的线程**不**是当前请求线程，那就返回过期时间，外层进行重试加锁。

**释放锁**

- 如果锁过期了，那直接返回，无需解锁。
- 如果锁没过期，则判断是否有锁重入，如果有锁重入那就锁状态-1，且renew过期时间，续期。
- 如果锁重入也都释放完了，那直接del掉，然后pub/sub模型通知订阅者。
- 返回成功。



> 转载自：https://juejin.cn/book/7018393458620497934/section/7018860092099395621