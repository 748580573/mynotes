# Redis限流

限流算法在分布式领域是一个经常被提到的话题，当系统的处理能力有限时，如何阻止计划外的请求继续对系统施压，这是一个需要重视的问题。

除了控制限流，限流还有一个应用目的时用于控制用户行为，避免垃圾请求。在网络社区种，用户的发帖、回复、点赞等行为都要严格受控，一般要严格限定某行为在规定时间内允许的次数，超过了次数那就是非法行为。对非法行为，业务必须规定适当的惩处策略。

## 简单限流

首先我们来看一个常见 的简单的限流策略。系统要限定用户的某个行为在指定的时间里只能允许发生 N 次，如何使用 Redis 的数据结构来实现这个限流的功能？

我们先定义一个接口，然后来讨论实现的思路

```java
/**
 *
 * @param userId  用户标志
 * @param actionKey  操作key
 * @param period    指定时间段
 * @param maxCount  指定时间段内用户行为发生的最大次数
 * @return
 */
public boolean isActionAllowed(String userId,String actionKey,int period,int maxCount);
```

可以简单的理解到，我们的需求是限制用户在`指定时间段内`的`行为`，因此我们需要一个数据结构能够表达时间窗口，然后还需要有对时间窗口内的数据进行统计的功能。此时答案已经呼啸而出了，我们可以采用redis的zset的数据结构来实现这个功能。

````shell
zadd key [NX|XX] [CH] [INCR] score member [score member ...]
````

将key作为用户行为的标志，score记录时间戳，member不做要求，只需要保持唯一就行了。在对用户进行限流判定的时候，只需要取指定时间内的数据进行统计，时间外的数据删除掉就行了，这样就用zset形成了一个时间窗口，达到了简单限流的目的。用代码表示如下:

```java
public class RedisTest {

    private Jedis jedis;

    public RedisTest(Jedis jedis){
        this.jedis = jedis;
    }

    /**
     *
     * @param userId  用户标志
     * @param actionKey  操作key
     * @param period    指定时间段
     * @param maxCount  指定时间段内用户行为发生的最大次数
     * @return
     */
    public boolean isActionAllowed(String userId,String actionKey,int period,int maxCount){
        String key = String.format("hist:%s:%s", userId, actionKey);
        long nowTs = System.currentTimeMillis();
        Pipeline pipe = jedis.pipelined();
        pipe.multi();
        pipe.zadd(key, nowTs, "" + nowTs);
        pipe.zremrangeByScore(key, 0, nowTs - period * 1000);
        Response<Long> count = pipe.zcard(key);
        pipe.expire(key, period + 1);
        pipe.exec();
        pipe.close();
        return count.get() <= maxCount;
    }

    public static void main(String[] args) {

        Jedis jedis = new Jedis("localhost",6379);
        jedis.set("name","hw");
        RedisTest test = new RedisTest(jedis);
        int count = 0;
        long start = System.currentTimeMillis();
        for (int i = 0;i < 100;i++){
            if (test.isActionAllowed("1","reply",5,10)){
                count++;
            }
        }
        System.out.println("用时：" + (System.currentTimeMillis() - start) / 1000);
        System.out.println(count);
        jedis.close();
    }
}
```

因为这几个连续的 Redis 操作都是针对同一个 key 的，使用 pipeline 可以显著提升 Redis 存取效率。但这种方案也有缺点

* 因为它要记录时间窗口内所有的行为记录，如果这个量很大，比如限定 60s 内操作不得超过 100w 次这样的参数，它是不适合做这样的限流的，因为会消耗大量的存储空间。

* 如果用Pipline，会存在数据同步的命令，使用pipeline发生的命令集并不是原子性的，读者可以是使用lua脚本来修改上面的代码。
* Reids集群不支持pipline

## 漏斗限流

漏斗限流是最常用的限流方法之一，顾名思义，这个算法的灵感源于漏斗（funnel）的结构

![](./img/limitflow/1)

漏斗应该是大家日常熟知的工具了，如果不堵住口子，它会一直从下面流出东西，如果漏斗被灌满了，这是就需要等一段时间，等漏斗腾出空间后再加东西进去。所以，漏斗的剩余空间就代表着当前行为可以持续进行的数量，漏嘴的流水速率代表着系统允许该行为的最大频率。下面我们使用代码来描述单机漏斗算法。

```java
import java.util.HashMap;
import java.util.Map;

public class FunnelRatelimiter {

    private class Funnel{
        int capacity;            //漏洞的总容量
        float leakingRate;       //漏斗的流出速率
        float leftQuota;           //漏斗的剩余容量
        long leakingTs;          //漏斗上一次泄露的时间

        public Funnel(int capacity,float leakingRate){
            this.capacity = capacity;
            this.leakingRate = leakingRate;
            this.leftQuota = capacity;
            this.leakingTs = System.currentTimeMillis();
        }

        /**
         * 漏洞泄露
         */
        public void leak(){
            long durationTime = (System.currentTimeMillis() - this.leakingTs) / 1000;
            long reduction = (long) (durationTime * this.leakingRate);
            //漏斗里的东西流完了
            if (reduction > this.leftQuota || reduction < 0){
                this.leftQuota = capacity;
            }else {
                this.leftQuota += reduction;
            }
            this.leftQuota = Math.min(this.leftQuota, capacity);
        }

        /**
         * 漏斗添加
         * @param quota  添加的额度
         * @return
         */
        boolean watering(int quota){
            leak();
            if (this.leftQuota >= quota){
                this.leftQuota -= quota;
                return true;
            }
            return false;
        }
    }

    private Map<String,Funnel> funnels = new HashMap<>();

    public boolean isActionAllowed(String userId,String actionKey,int capacity,float leakingRate){
        String key = String.format("%s:%s", userId, actionKey);
        Funnel funnel = funnels.get(key);
        if (funnel == null){
            funnel = new Funnel(capacity,leakingRate);
            funnels.put(key,funnel);
        }
        return funnel.watering(1);
    }

}
```

上面的代码实现了简单的漏斗限流，问题来了，分布式的漏斗算法该如何实现？能不能使用 Redis 的基础数据结构来搞定？

我们观察 Funnel 对象的几个字段，我们发现可以将 Funnel 对象的内容按字段存储到一个 hash 结构中，灌水的时候将 hash 结构的字段取出来进行逻辑运算后，再将新值回填到 hash 结构中就完成了一次行为频度的检测。

但是有个问题，我们无法保证整个过程的原子性。从 hash 结构中取值，然后在内存里运算，再回填到 hash 结构，这三个过程无法原子化，意味着需要进行适当的加锁控制。而一旦加锁，就意味着会有加锁失败，加锁失败就需要选择重试或者放弃。

如果重试的话，就会导致性能下降。如果放弃的话，就会影响用户体验。同时，代码的复杂度也跟着升高很多。这真是个艰难的选择，我们该如何解决这个问题呢？Redis-Cell 救星来了！

## Redis-Cell

Redis 4.0 提供了一个限流 Redis 模块，它叫 redis-cell。该模块也使用了漏斗算法，并提供了原子的限流指令。有了这个模块，限流问题就非常简单了。

该模块只有1条指令`cl.throttle`，它的参数和返回值都略显复杂，接下来让我们来看看这个指令具体该如何使用。

````shell
> cl.throttle zhangsan:reply 15 30 60 1
                      ▲     ▲  ▲  ▲  ▲
                      |     |  |  |  └───── need 1 quota (可选参数，默认值也是1，表示向漏斗添加的数量)
                      |     |  └──┴─────── 30 operations / 60 seconds 这是漏水速率
                      |     └───────────── 15 capacity 这是漏斗容量
                      └─────────────────── key laoqian
````

上面这个指令的意思是允许「用户张三的回复行为」的频率为每 60s 最多 30 次(漏水速率)，漏斗的初始容量为 15，也就是说一开始可以连续回复 15 个帖子，然后才开始受漏水速率的影响。我们看到这个指令中漏水速率变成了 2 个参数，替代了之前的单个浮点数。用两个参数相除的结果来表达漏水速率相对单个浮点数要更加直观一些。

````shell
> cl.throttle laoqian:reply 15 30 60
1) (integer) 0   # 0 表示允许，1表示拒绝
2) (integer) 15  # 漏斗容量capacity
3) (integer) 14  # 漏斗剩余空间left_quota
4) (integer) -1  # 如果拒绝了，需要多长时间后再试(漏斗有空间了，单位秒)
5) (integer) 2   # 多长时间后，漏斗完全空出来(left_quota==capacity，单位秒)
````

在执行限流指令时，如果被拒绝了，就需要丢弃或重试。cl.throttle 指令考虑的非常周到，连重试时间都帮你算好了，直接取返回结果数组的第四个值进行 sleep 即可，如果不想阻塞线程，也可以异步定时任务来重试