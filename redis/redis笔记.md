### 什么是NoSql

Not only Sql (不仅仅是SQL)，泛指非关系型数据库，随着web2.0，互联网的诞生，传统的关系型数据库很难对付web 2.0时代！尤其是超大规模的高并发的社区！暴露出来很多难以克服的问题。

很多的数据类型，比如个人信息、社交网络、地理位置，这些数据类型的存储不需要一个固定的格式！不需要多余的操作就可以横向操作！

### NOSQL的特点

1. 方便扩展（数据之间没有关系，很好扩展）
2. 大数据量，高性能。(redis  一秒读8W次，写11W次)
3. 数据类型的多样性！（不需要事先设计数据库，随去随用！如果是数据量十分大的表，很多人就无法设计了）
4. 传统的RDBM和NOSQL

   ````shell
   #传统的RDBMS
   ````
- 结构化阻止
- SQL
- 数据和关系都存在单独的表中
- 数据操作/数据定义语言
- 严格的一致性
- 基础的事务
- ......

#NOSQL
- 不仅仅是数据
- 没有固定的查询语言
- 键值对存储，列存储，文旦存储，图形数据库（社交关系）
- CAP定理和BASE(异地多活！)
- 高性能，高可用，可扩展
  
   ````
   
   ````

### 3V与3高

大数据时代的3V，主要的问题：

1. 海量Volume
2. 多样Variety
3. 实时Velocity

大数据的3高，主要对程序的要求：

1. 高并发
2. 扩展性
3.  高性能

### NOSQL的四大分类

#### KV键值对

* 新浪：redis
* 美团：redis + Tair
* 阿里、百度：Redis + Memecache

**文档型数据库(bson)**

* MongoDB:是一个基于分布式文件存储的数据库，c++编写，主要处理大量的文档。

### 列存储数据库

* HBase
* 分布式文件系统

### 图关系型数据库

* Neo4J,InfoGrid;



### Redis入门

> Redis(Remote Dirctory Server)

#### Redis安装

1. [下载](https://redis.io/)Redis的安装包，将安装包上传至/home/softwore目录

2. 安装gcc
   1. 安装gcc，yum install gcc-c++
   2. 进入redis目录，输入make命令

   ````shell
   [root@master ~]# cd /home/softwore/redis-6.0.8
   [root@master redis-6.0.8]# ls
   00-RELEASENOTES  CONTRIBUTING  deps     Makefile   README.md   runtest          runtest-moduleapi  sentinel.conf  tests   utils
   BUGS             COPYING       INSTALL  MANIFESTO  redis.conf  runtest-cluster  runtest-sentinel   src            TLS.md
   [root@master redis-6.0.8]# 
   [root@master redis-6.0.8]# 
   [root@master redis-6.0.8]# make & make install
   ````
   3. make完后会在/usr/local/bin目录下生成redis的相关文件

   ````shell
   [root@master bin]# cd /usr/local/bin/
   [root@master bin]# ls
   mysql_miner  node  npm  pm2  redis-benchmark  redis-check-aof  redis-check-rdb  redis-cli  redis-sentinel  redis-server
   ````

   4. 配置redis的配置问阿金

   ````shell
   mkdir -p  /usr/local/bin/redis_config
   cp -r /home/softwore/redis-6.0.8/redis.conf /usr/local/bin/redis_config
   ````

   5. 启动redis服务

   ````shell
   cd /usr/local/bin
   [root@master redis_config]# ./redis-server  redis_config/redis.conf 
   bash: ./redis-server: No such file or directory
   [root@master redis_config]# cd ..
   [root@master bin]# ./redis-server  redis_config/redis.conf 
   20812:C 22 Oct 2020 11:02:48.254 # oO0OoO0OoO0Oo Redis is starting oO0OoO0OoO0Oo
   20812:C 22 Oct 2020 11:02:48.254 # Redis version=6.0.8, bits=64, commit=00000000, modified=0, pid=20812, just started
   20812:C 22 Oct 2020 11:02:48.254 # Configuration loaded
   20812:M 22 Oct 2020 11:02:48.255 * Increased maximum number of open files to 10032 (it was originally set to 1024).
                   _._                                                  
              _.-``__ ''-._                                             
         _.-``    `.  `_.  ''-._           Redis 6.0.8 (00000000/0) 64 bit
     .-`` .-```.  ```\/    _.,_ ''-._                                   
    (    '      ,       .-`  | `,    )     Running in standalone mode
    |`-._`-...-` __...-.``-._|'` _.-'|     Port: 6378
    |    `-._   `._    /     _.-'    |     PID: 20812
     `-._    `-._  `-./  _.-'    _.-'                                   
    |`-._`-._    `-.__.-'    _.-'_.-'|                                  
    |    `-._`-._        _.-'_.-'    |           http://redis.io        
     `-._    `-._`-.__.-'_.-'    _.-'                                   
    |`-._`-._    `-.__.-'    _.-'_.-'|                                  
    |    `-._`-._        _.-'_.-'    |                                  
     `-._    `-._`-.__.-'_.-'    _.-'                                   
         `-._    `-.__.-'    _.-'                                       
             `-._        _.-'                                           
                 `-.__.-'                                               
   
   20812:M 22 Oct 2020 11:02:48.257 # WARNING: The TCP backlog setting of 511 cannot be enforced because /proc/sys/net/core/somaxconn is set to the lower value of 128.
   20812:M 22 Oct 2020 11:02:48.257 # Server initialized
   20812:M 22 Oct 2020 11:02:48.257 # WARNING overcommit_memory is set to 0! Background save may fail under low memory condition. To fix this issue add 'vm.overcommit_memory = 1' to /etc/sysctl.conf and then reboot or run the command 'sysctl vm.overcommit_memory=1' for this to take effect.
   20812:M 22 Oct 2020 11:02:48.257 # WARNING you have Transparent Huge Pages (THP) support enabled in your kernel. This will create latency and memory usage issues with Redis. To fix this issue run the command 'echo madvise > /sys/kernel/mm/transparent_hugepage/enabled' as root, and add it to your /etc/rc.local in order to retain the setting after a reboot. Redis must be restarted after THP is disabled (set to 'madvise' or 'never').
   20812:M 22 Oct 2020 11:02:48.258 * Ready to accept connections
   
   ````

   

### 安装遇到的问题

#### Redis6安装遇到的问题

这里安装redis的时候会可能会出现下面的问题

````shell
server.c:5170:39: error: ‘struct redisServer’ has no member named ‘maxmemory’
if (server.maxmemory > 0 && server.maxmemory < 1024*1024) {
                                  ^
server.c:5171:176: error: ‘struct redisServer’ has no member named ‘maxmemory’
    serverLog(LL_WARNING,"WARNING: You specified a maxmemory value that is less than 1MB (current value is %llu bytes). Are you sure this is what you really want?", server.maxmemory);
                                                                                                                                                                           ^
server.c:5174:31: error: ‘struct redisServer’ has no member named ‘server_cpulist’
redisSetCpuAffinity(server.server_cpulist);
````

解决办法：
出现这种错误是由于gcc版本太低，升级gcc

````shell
# 查看gcc版本是否在5.3以上，centos7.6默认安装4.8.5
gcc -v
# 升级gcc到5.3及以上,如下：
升级到gcc 9.3：
yum -y install centos-release-scl
yum -y install devtoolset-9-gcc devtoolset-9-gcc-c++ devtoolset-9-binutils
scl enable devtoolset-9 bash
需要注意的是scl命令启用只是临时的，退出shell或重启就会恢复原系统gcc版本。
如果要长期使用gcc 9.3的话：
 
echo "source /opt/rh/devtoolset-9/enable" >>/etc/profile
这样退出shell重新打开就是新版的gcc了
以下其他版本同理，修改devtoolset版本号即可
````





### 性能测试

redis-benchmark是一个压力测试工具！

redis 性能测试工具可选参数如下所示：

| 序号 | 选项      | 描述                                       | 默认值    |
| :--- | :-------- | :----------------------------------------- | :-------- |
| 1    | **-h**    | 指定服务器主机名                           | 127.0.0.1 |
| 2    | **-p**    | 指定服务器端口                             | 6379      |
| 3    | **-s**    | 指定服务器 socket                          |           |
| 4    | **-c**    | 指定并发连接数                             | 50        |
| 5    | **-n**    | 指定请求数                                 | 10000     |
| 6    | **-d**    | 以字节的形式指定 SET/GET 值的数据大小      | 2         |
| 7    | **-k**    | 1=keep alive 0=reconnect                   | 1         |
| 8    | **-r**    | SET/GET/INCR 使用随机 key, SADD 使用随机值 |           |
| 9    | **-P**    | 通过管道传输 <numreq> 请求                 | 1         |
| 10   | **-q**    | 强制退出 redis。仅显示 query/sec 值        |           |
| 11   | **--csv** | 以 CSV 格式输出                            |           |
| 12   | **-l**    | 生成循环，永久执行测试                     |           |
| 13   | **-t**    | 仅运行以逗号分隔的测试命令列表。           |           |
| 14   | **-I**    | Idle 模式。仅打开 N 个 idle 连接并等待。   |           |

````shell
#测试：100个并发，10w请求
redis-benchmark  -h localhost  -p 6379 -c 100 -n 1000000 
````



### Redis尝试

#### 切换数据库

````shell
select 3
````

#### 查看数据库大小

````shell
DBSIZE
````

#### 查看数据库所有的key

````shell
keys *
````

#### 清空当前库

````
flushdb
````

#### 清空全部数据库

````shell
flushall
````

#### 判断key是否存在

````shell
EXISTS KEYS
````

#### 移动Key到别的数据库

````shell
move  key  database
move  name  1
````

#### 给key设置过期时间

````shell
EXPIRE key  second
#设置key 10秒后过期
EXPIRE name 10
````

#### 查看key的type类型

````shell
127.0.0.1:6379> type name
string
````



#### Redis是单线程的

Redis是很快的，Redis是基于内存操作的，CPU不是Redis性能瓶颈，Redis的瓶颈是根据机器的内存和网络带宽，既然可以使用单线程来实现，那就使用单线程了！不过Redis 6之后就是多线程了。

#### Redis为什么单线程还这么快？

以前一直有个误区，以为：高性能服务器 一定是 多线程来实现的

原因很简单因为误区二导致的：多线程 一定比 单线程 效率高。其实不然。

在说这个事前希望大家都能对 CPU 、 内存 、 硬盘的速度都有了解了，这样可能理解得更深刻一点，不了解的朋友点：CPU到底比内存跟硬盘快多少

redis 核心就是 如果我的数据全都在内存里，我单线程的去操作 就是效率最高的，为什么呢，因为多线程的本质就是 CPU 模拟出来多个线程的情况，这种模拟出来的情况就有一个代价，就是上下文的切换，对于一个内存的系统来说，它没有上下文的切换就是效率最高的。redis 用 单个CPU 绑定一块内存的数据，然后针对这块内存的数据进行多次读写的时候，都是在一个CPU上完成的，所以它是单线程处理这个事。在内存的情况下，这个方案就是最佳方案 —— 阿里 沈询

因为一次CPU上下文的切换大概在 1500ns 左右。

从内存中读取 1MB 的连续数据，耗时大约为 250us，假设1MB的数据由多个线程读取了1000次，那么就有1000次时间上下文的切换，

那么就有1500ns * 1000 = 1500us ，我单线程的读完1MB数据才250us ,你光时间上下文的切换就用了1500us了，我还不算你每次读一点数据 的时间，

那什么时候用多线程的方案呢？

答案是：下层的存储等慢速的情况。比如磁盘

内存是一个 IOPS 非常高的系统，因为我想申请一块内存就申请一块内存，销毁一块内存我就销毁一块内存，内存的申请和销毁是很容易的。而且内存是可以动态的申请大小的。

磁盘的特性是：IPOS很低很低，但吞吐量很高。这就意味着，大量的读写操作都必须攒到一起，再提交到磁盘的时候，性能最高。为什么呢？

如果我有一个事务组的操作（就是几个已经分开了的事务请求，比如写读写读写，这么五个操作在一起），在内存中，因为IOPS非常高，我可以一个一个的完成，但是如果在磁盘中也有这种请求方式的话，

我第一个写操作是这样完成的：我先在硬盘中寻址，大概花费10ms，然后我读一个数据可能花费1ms然后我再运算（忽略不计），再写回硬盘又是10ms ，总共21ms

第二个操作去读花了10ms, 第三个又是写花费了21ms ,然后我再读10ms, 写21ms ，五个请求总共花费83ms，这还是最理想的情况下，这如果在内存中，大概1ms不到。

所以对于磁盘来说，它吞吐量这么大，那最好的方案肯定是我将N个请求一起放在一个buff里，然后一起去提交。

方法就是用异步：将请求和处理的线程不绑定，请求的线程将请求放在一个buff里，然后等buff快满了，处理的线程再去处理这个buff。然后由这个buff 统一的去写入磁盘，或者读磁盘，这样效率就是最高。java里的 IO不就是这么干的么~

对于慢速设备，这种处理方式就是最佳的，慢速设备有磁盘，网络 ，SSD 等等，

多线程 ，异步的方式处理这些问题非常常见，大名鼎鼎的netty 就是这么干的。

终于把 redis 为什么是单线程说清楚了，把什么时候用单线程跟多线程也说清楚了，其实也是些很简单的东西，只是基础不好的时候，就真的尴尬。。。。

补一发大师语录：来说说，为何单核cpu绑定一块内存效率最高

“我们不能任由操作系统负载均衡，因为我们自己更了解自己的程序，所以我们可以手动地为其分配CPU核，而不会过多地占用CPU”，默认情况下单线程在进行系统调用的时候会随机使用CPU内核，为了优化Redis，我们可以使用工具为单线程绑定固定的CPU内核，减少不必要的性能损耗！

redis作为单进程模型的程序，为了充分利用多核CPU，常常在一台server上会启动多个实例。而为了减少切换的开销，有必要为每个实例指定其所运行的CPU。

Linux 上 taskset 可以将某个进程绑定到一个特定的CPU。你比操作系统更了解自己的程序，为了避免调度器愚蠢的调度你的程序，或是为了在多线程程序中避免缓存失效造成的开销。

顺便再提一句：redis 的瓶颈在网络上 。。。。



### 五大数据类型

Redis是一种开放源代码（BSD许可）的内存中数据结构存储，用作数据库，缓存和消息代理。它支持数据结构，例如字符串，哈希，列表，集合，带范围查询的排序集合，位图，超日志，带有半径查询和流的地理空间索引。Redis具有内置的复制，Lua脚本，LRU驱逐，事务和不同级别的磁盘持久性，并通过Redis Sentinel和Redis Cluster自动分区提供高可用性。

#### String（字符串）

##### 普通字符串操作

````shell

127.0.0.1:6379> set name wuheng
OK
127.0.0.1:6379> get name
"wuheng"
127.0.0.1:6379> exists name
(integer) 1
127.0.0.1:6379> append name " nihao"
(integer) 12
127.0.0.1:6379> get name
"wuheng nihao"
````

##### 数字的自增

````shell
127.0.0.1:6379> set number 10
OK
127.0.0.1:6379> type number
string
127.0.0.1:6379> incrby number 8
(integer) 18
127.0.0.1:6379> get number
"18"
127.0.0.1:6379> type number
string
127.0.0.1:6379> incr number
(integer) 19
127.0.0.1:6379> get number
"19"
````

##### 字符串截取

````shell
#截取0到3
127.0.0.1:6379> getrange name 0 3
"wuhe"
#截取所有的串
127.0.0.1:6379> getrange name 0 -1
"wuheng nihao"
````

##### 字符串的替换

````shell
# 替换指定位置开始的字符串
127.0.0.1:6379> setrange name 2 runqiu
(integer) 12
127.0.0.1:6379> get name
"wurunqiuihao"
````

##### 创建key并设置过期时间(存在就覆盖)

````shell
127.0.0.1:6379> setex age 10 100
OK
127.0.0.1:6379> get age
"100"
127.0.0.1:6379> get age
(nil)
````

##### 创建key(不存在则创建，存在则创建失败)

> 该操作可用于分布式锁中

````shell
127.0.0.1:6379> setnx height 10
(integer) 1
127.0.0.1:6379> setnx height 10
(integer) 0
````

##### 多个值设置操作

````shell
127.0.0.1:6379> mset k1 v1 k2 v2 k3 v3 k4 v4
OK
127.0.0.1:6379> get k1
"v1"
127.0.0.1:6379> get k2
"v2"
127.0.0.1:6379> get k3
"v3"
````

##### 获取多个key值操作

````shell
127.0.0.1:6379> mget k1 k2 k3
1) "v1"
2) "v2"
3) "v3"
````

##### 组合命令getset

````shell
127.0.0.1:6379> getset aolige  100
(nil)
127.0.0.1:6379> get aolige
"100"
````



#### List(双向链表)

##### Lpush 添加数据

````shell
127.0.0.1:6379> lpush  list one
(integer) 1
(1.67s)
127.0.0.1:6379> lpush list two
(integer) 2
127.0.0.1:6379> lpush list three
(integer) 3
(1.66s)
````

##### Llen查询链表长度

````shell
127.0.0.1:6379> llen list
(integer) 4
````



##### Lrange范围查询

````shell
#从下面可以看出 list 的lpush操作是将数据按照入栈的顺序添加的
127.0.0.1:6379> lrange list 0 -1
1) "three"
2) "two"
3) "one"
127.0.0.1:6379> lrange list 0 1
1) "three"
2) "two"
````

##### Rpush从右端添加值

再list的另一端插入数据，这里的另一端是相较于Lpush命令的

````shell
127.0.0.1:6379> rpush list right
(integer) 4
127.0.0.1:6379> lrange list  0 -1
1) "three"
2) "two"
3) "one"
4) "right"
````

##### Lpop 从list的首端移除值

````shell
127.0.0.1:6379> lpop list
"three"
127.0.0.1:6379>
````

##### Rpop从list的未端移除值

````shell
127.0.0.1:6379> rpop list
"right"
````

##### Lindex获取list指定下标的数据

````shell
127.0.0.1:6379> lrange list 0 -1
1) "two"
2) "one"
127.0.0.1:6379> lindex list 1
"one"
````

##### Lrem移除值

````shell
127.0.0.1:6379> lpush list one
(integer) 3
127.0.0.1:6379> lpush list one
(integer) 4
127.0.0.1:6379> lpush list one
(integer) 5
127.0.0.1:6379> lrange list 0 -1
1) "one"
2) "one"
3) "one"
4) "two"
5) "one"
````

##### Ltrim对原列表进行修剪(截断)

````shell
127.0.0.1:6379> lpush list k1
(integer) 1
127.0.0.1:6379> lpush list k2
(integer) 2
127.0.0.1:6379> lpush list k3
(integer) 3
127.0.0.1:6379> lpush list k4
(integer) 4
127.0.0.1:6379> lrange list 0 -1
1) "k4"
2) "k3"
3) "k2"
4) "k1
#截取操作
127.0.0.1:6379> ltrim list 0 2
OK
127.0.0.1:6379> lrange list 0 -1
1) "k4"
2) "k3"
3) "k2"
````

##### 组合命令RpopLpush

将list的最右端数据插入newlist的最左端

````
127.0.0.1:6379> lrange list 0 -1
1) "k4"
2) "k3"
3) "k2"
127.0.0.1:6379> lrange newlist 0 -1
(empty array)
127.0.0.1:6379> RpopLpush list newlist
"k2"
127.0.0.1:6379> lrange newlist 0 -1
1) "k2"
````

##### Lset 对list指定下标设置值

````she
127.0.0.1:6379> lrange list 0 -1
1) "k4"
2) "k3"
127.0.0.1:6379> lset list 0  hw
OK
127.0.0.1:6379> lrange list 0 -1
1) "hw"
2) "k3"
````

##### Linsert 在指定值的前后插入数据

````sh
#在指定值前插入数据
127.0.0.1:6379> lrange list 0 -1
1) "hw"
2) "k3"
127.0.0.1:6379> Linsert list before k3 k2
(integer) 3
127.0.0.1:6379> lrange list 0 -1
1) "hw"
2) "k2"
3) "k3"
#在指定值后加入数据
127.0.0.1:6379> lrange list 0 -1
1) "hw"
2) "k2"
3) "k3"
127.0.0.1:6379> Linsert list after k3 k1
(integer) 4
127.0.0.1:6379> lrange list 0 -1
1) "hw"
2) "k2"
3) "k3"
4) "k1"
````



#### Set(无序不重复集合)

##### Sadd 添加元素

````shell
127.0.0.1:6379> Sadd myset hello
(integer) 1
127.0.0.1:6379> Sadd myset world
(integer) 1
````

##### Smembers获取某个key的所有数据

````shell
127.0.0.1:6379> smembers test
1) "1"
2) "2"
3) "3"
4) "4
````

##### Scard查询set的元素个数

````shell
127.0.0.1:6379> Scard myset
(integer) 2
````

##### Srem从set中移除元素

````shell
127.0.0.1:6379> Srem myset hello
(integer) 1
````

##### Srandmember随机获取set中的元素

````shell
127.0.0.1:6379> Sadd myset hello
(integer) 1
127.0.0.1:6379> Sadd myset meximexi
(integer) 1
127.0.0.1:6379> Sadd myset aoligei
(integer) 1
127.0.0.1:6379> Srandmember myset
"aoligei"
127.0.0.1:6379> Srandmember myset
"world"
127.0.0.1:6379> Srandmember myset
"world"
127.0.0.1:6379> Srandmember myset
"meximexi"
````

##### Spop随机移除元素

````shell
127.0.0.1:6379> spop myset
"meximexi"
127.0.0.1:6379> spop myset
"world"
````

##### Smove移动元素到另一个set中

````shell
127.0.0.1:6379> sadd myset2 lalala
127.0.0.1:6379> sadd myset2 hello
127.0.0.1:6379> smove myset myset2 aoligei
(integer) 0
127.0.0.1:6379> Srandmember myset2 4
1) "lalala"
2) "hello"
3) "aoligei"
````

##### Sdiff求多个集合的差集

````shell
127.0.0.1:6379> Sdiff myset myset2
(empty array)
127.0.0.1:6379> Sdiff myset2 myset
1) "lalala"
2) "hello"
3) "aoligei"
````

##### Sinter求多个集合交集

````shell
127.0.0.1:6379> Srandmember myset 4
1) "hello"
127.0.0.1:6379> Srandmember myset2 4
1) "lalala"
2) "hello"
3) "aoligei"
127.0.0.1:6379> Sinter myset myset2
1) "hello"
````

##### Sunion求多个集合并集

````shell
127.0.0.1:6379> Srandmember myset 4
1) "hello"
127.0.0.1:6379> Srandmember myset2 4
1) "lalala"
2) "hello"
3) "aoligei"
127.0.0.1:6379> Sunion myset myset2
1) "lalala"
2) "aoligei"
3) "hello"
````

#### Hash(hash，key-value)

##### Hset添加元素

````
# hset hashkey field  value
127.0.0.1:6379> hset myhash name hw
(integer) 1
````

##### Hget获取元素

````shell
# hget hashkey field
127.0.0.1:6379> hget myhash key1
"hw"
````

##### Hmset 添加多个元素

````shell
# hmset hashkey [field value]+
127.0.0.1:6379> hmset myhash  key2 hw2  key3  hw3
OK
````

##### Hmget获取多个元素

````shell
#hmset hashkey [field]+
127.0.0.1:6379> hmget myhash key1 key2 key3
1) "hw"
2) "hw3"
3) "hw3"
````

##### Hgetall获取全部的数据

````shell
#hgetall hashkey
127.0.0.1:6379> hgetall myhash
1) "key1"      #field
2) "hw"        #value
3) "key2"
4) "hw3"
5) "key3"
6) "hw3"
````

##### Hdel删除指定的field

````shell
127.0.0.1:6379> hdel myhash key1
(integer) 1
127.0.0.1:6379> hgetall myhash
1) "key2"
2) "hw3"
3) "key3"
4) "hw3
````

##### Hlen获取key的长度

````shell
127.0.0.1:6379> hlen myhash
(integer) 2
````

##### Hexist判断field是否存在

````shell
#判断hash里的field是否存在
127.0.0.1:6379> Hgetall myhash
1) "key2"
2) "hw3"
3) "key3"
4) "hw3"
127.0.0.1:6379> Hexists myhash key2
(integer) 1
127.0.0.1:6379> Hexists myhash key1
(integer) 0
````

##### Hkeys获取某个key的所有field

````shell
127.0.0.1:6379> Hkeys myhash
1) "key2"
2) "key3"
````

##### Hvals获取某个key的所有的value

````shell
127.0.0.1:6379> Hvals myhash
1) "hw3"
2) "hw3"
````

##### Hincrby给某个field做加法

````shell
#Hincrby key field addNuber
127.0.0.1:6379> Hgetall myhash
1) "number"
2) "1"
3) "number2"
4) "2"
5) "number3"
6) "3"
127.0.0.1:6379> Hincrby myhash number  3
(integer) 4
127.0.0.1:6379> Hgetall myhash
1) "number"
2) "4"
3) "number2"
4) "2"
5) "number3"
6) "3"
````

##### Hsetnx如果不存在field则可以设置

````shell
#Hsetnx key field value
#如果存在则不可以设置
127.0.0.1:6379> Hsetnx myhash number 10
(integer) 0
127.0.0.1:6379> Hgetall myhash
1) "number"
2) "4"
3) "number2"
4) "2"
5) "number3"
6) "3"
#如果不存在则可以设置
127.0.0.1:6379> Hsetnx myhash number10 10
(integer) 1
127.0.0.1:6379> Hgetall myhash
1) "number"
2) "4"
3) "number2"
4) "2"
5) "number3"
6) "3"
7) "number10"
8) "10"
````

#### Zset（有序集合)

##### Zadd 添加值

````shell
#zadd key score value
127.0.0.1:6379> zadd  salary  3000 zhangsan
(integer) 1
127.0.0.1:6379> zadd  salary  2000 lisi
(integer) 1
127.0.0.1:6379> zadd  salary  1000 wangwu
(integer) 1
127.0.0.1:6379> zrange salary 0 -1
1) "wangwu"
2) "lisi"
3) "zhangsan"
````

##### zrange获取某个key的所有信息

````shell
127.0.0.1:6379> zrange salary 0 -1
1) "wangwu"
2) "lisi"
3) "zhangsan"
````

##### Zrangebyscore升序排序

````shell
#zrangebyscore key min max [WITHSCORES] [LIMIT offset count]
#这里-inf  +inf表示的是范围，即从负无穷到正无穷按升序排序
127.0.0.1:6379> Zrangebyscore salary -inf +inf 
1) "wangwu"
2) "lisi"
3) "zhangsan"
#对score中2000到3000的值进行升序排序
127.0.0.1:6379> Zrangebyscore salary 2000 3000
1) "lisi"
2) "zhangsan"
#将value对应的score也一起打印出来
127.0.0.1:6379> Zrangebyscore salary -inf +inf withscores
1) "wangwu"
2) "1000"
3) "lisi"
4) "2000"
5) "zhangsan"
6) "3000"
````

##### Zrevrangebyscore降序排序

````shell
#Zrevrangebyscore salary max min [WITHSCORES] [LIMIT offset count]
127.0.0.1:6379> ZREVRANGEBYSCORE salary +inf -inf
1) "zhangsan"
2) "lisi"
3) "wangwu"
````

##### Zrmv删除元素

````shell
#  Zrem key member [member ...]
127.0.0.1:6379> ZREM salary lisi
(integer) 1
127.0.0.1:6379> ZREVRANGEBYSCORE salary +inf -inf
1) "zhangsan"
2) "wangwu"
````

##### Zcard统计Zset里的元素个数

````shell
 #zcard key
 127.0.0.1:6379> zcard salary
(integer) 2
````

#####  Zcount统计指定区间的元素数量

````shell
#Zcount key min max
127.0.0.1:6379> zrange salary 0 -1 withscores
1) "wangwu"
2) "1000"
3) "lisi"
4) "2000"
5) "zhangsan"
6) "3000"
127.0.0.1:6379> zcount salary 2000 3000
(integer) 2
````



#### Geospatial地理位置

##### Geoadd添加地理位置

经度（longitude）必须放在纬度（latitude）之前，对于可被索引的坐标位置是有一定限制条件的：非常靠近极点的位置是不能被索引的，

- 有效的经度是-180度到180度
- 有效的纬度是-85.05112878度到85.05112878度

````shell
# Geoadd key longitude latitude member [longitude latitude member ...]
127.0.0.1:6379> Geoadd cities 116.404269 39.91582 "beijing"  121.478799 31.23545 "shanghai"
(integer) 2
````

##### Zrange获取某个key的所有的位置信息

````shell
127.0.0.1:6379> Zrange cities 0 -1 withscores
1) "shanghai"
2) "4054803475356098"
3) "beijing"
4) "4069885555377153"
````

##### Geodist返回一个key中指定两个位置的距离

````shell
#Geodist key member1 member2 [m|km|ft|mi]
127.0.0.1:6379> Geodist cities beijing shanghai km
"1068.5682"
````

##### Geohash返回一个或多个位置的经纬度信息

````shell
Geohash key member [member ...]
127.0.0.1:6379> Geohash cities beijing
1) "wx4g0f7n800"
````

#####  Geopos返回一个或多个位置的经纬度信息

````shell
#Geopos key member [member ...]
127.0.0.1:6379> Geopos cities beijing
1) 1) "116.40426903963088989"
   2) "39.91581928642635546"
````

##### Georadius以给定位置为中心，半径不超过给定半径的附近所有位置

````shell
#Georadius key longitude latitude radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count] [ASC|DESC] [STORE key] [STORED]
127.0.0.1:6379> GEORADIUS cities 120 30 500 km
1) "shanghai"
````

##### Georadiusbymember指定已添加的某个位置作为中心,半径不超过给定半径的附近所有位置

````shell
GEORADIUSBYMEMBER key member radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count] [ASC|DESC] [STORE key] [STOREDIST
127.0.0.1:6379> Georadiusbymember cities shanghai 3000 km
1) "shanghai"
2) "beijing"
````

#### Hyperloglog

##### 简介

Redis HyperLogLog 是用来做基数统计的算法，HyperLogLog 的优点是，在输入元素的数量或者体积非常非常大时，计算基数所需的空间总是固定 的、并且是很小的。

在 Redis 里面，每个 HyperLogLog 键只需要花费 12 KB 内存，就可以计算接近 2^64 个不同元素的基 数。这和计算基数时，元素越多耗费内存就越多的集合形成鲜明对比。

但是，因为 HyperLogLog 只会根据输入元素来计算基数，而不会储存输入元素本身，所以 HyperLogLog 不能像集合那样，返回输入的各个元素。

##### 什么是基数

比如数据集 {1, 3, 5, 7, 5, 7, 8}， 那么这个数据集的基数集为 {1, 3, 5 ,7, 8}, 基数(不重复元素)为5。 基数估计就是在误差可接受的范围内，快速计算基数。

##### 基数的应用实例

下面通过一个实例说明基数在电商数据分析中的应用。

假设一个淘宝网店在其店铺首页放置了10个宝贝链接，分别从Item01到Item10为这十个链接编号。店主希望可以在一天中随时查看从今天零点开始到目前这十个宝贝链接分别被多少个独立访客点击过。所谓独立访客（Unique Visitor，简称UV）是指有多少个自然人，例如，即使我今天点了五次Item01，我对Item01的UV贡献也是1，而不是5。

用术语说这实际是一个实时数据流统计分析问题。

要实现这个统计需求。需要做到如下三点：

1、对独立访客做标识

2、在访客点击链接时记录下链接编号及访客标记

3、对每一个要统计的链接维护一个[数据结构](http://lib.csdn.net/base/31)和一个当前UV值，当某个链接发生一次点击时，能迅速定位此用户在今天是否已经点过此链接，如果没有则此链接的UV增加1

下面分别介绍三个步骤的实现方案

**对独立访客做标识**

客观来说，目前还没有能在互联网上准确对一个自然人进行标识的方法，通常采用的是近似方案。例如通过登录用户+cookie跟踪的方式：当某个用户已经登录，则采用会员ID标识；对于未登录用户，则采用跟踪cookie的方式进行标识。为了简单起见，我们假设完全采用跟踪cookie的方式对独立访客进行标识。

**实时UV计算**

#### BigMap（位图）

##### setbit添加

````shell
#setbit key offset value
127.0.0.1:6379> setbit sign 1 0
(integer) 
````

##### Getbit获取

````shell
127.0.0.1:6379> getbit sign 1
(integer) 1
````

##### Bitcount(统计)

````shell
# bitcount key [start end]
127.0.0.1:6379> setbit sign 0 1
(integer) 0
127.0.0.1:6379> setbit sign 1 0
(integer) 1
127.0.0.1:6379> setbit sign 2 0
(integer) 0
127.0.0.1:6379> setbit sign 3 0
(integer) 0
127.0.0.1:6379> bitcount sign
(integer) 1
````

#### Redis基本的事务操作

redis保证命令的原子性，但是redis事务不保证的原子性

redis没有隔离级别的概念

##### Redis的三个阶段

* 开启事务(multi)

* 命令入队

* 执行事务(exec)

````shell
127.0.0.1:6379> MULTI
OK
127.0.0.1:6379> set k1 v1
QUEUED
127.0.0.1:6379> set k2 v2
QUEUED
127.0.0.1:6379> get k2
QUEUED
127.0.0.1:6379> set k3 v3 
QUEUED
127.0.0.1:6379> exec
1) OK
2) OK
3) "v2"
4) OK
````

##### (discard)放弃事务

````shell
127.0.0.1:6379> multi
OK
127.0.0.1:6379> set k1 v1
QUEUED
127.0.0.1:6379> set k2 v2
QUEUED
127.0.0.1:6379> set k3 v3
QUEUED
127.0.0.1:6379> set k4 v4
QUEUED
127.0.0.1:6379> discard
OK
127.0.0.1:6379> get k4
(nil)
````

##### 注意事项

在redis事务执行过程中，如果事务中的其中一个命令有语法错误，在事务执行时，其他的命令也会照常执行。



#### 悲观锁

> 认为，什么时候都会出问题，无论做什么都需要加锁



#### （watch)乐观锁

一般watch命令与事务是一起使用的，WATCH命令可以监控一个或多个键，一旦其中有一个键被修改（或删除），之后的事务就不会执行。监控一直持续到EXEC命令（事务中的命令是在EXEC之后才执行的，所以在MULTI命令后可以修改WATCH监控的键值）



#### Jedis

##### 导入pom

````xml
        <!-- https://mvnrepository.com/artifact/redis.clients/jedis -->
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>3.3.0</version>
        </dependency>
````

然后reids的java客户端的api跟上文所描述的api一一对应。

#### SpringBoot整合Redis

##### 导入pom

````xml
<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
````

##### 说明

在SpringBoot2.x之后，jeids被替换为了lettuce。

jedis：采用直连，多个线程操作的话，是不安全的，如果想要避免不安全性，使用jedis pool连接池！

lettuce：采用netty，示例可以再多个线程中进行共享，不存在线程不安全的情况！可以减少线程数量。

##### 使用

````java
//set普通的set key value操作
        redisTemplate.opsForValue();
        //list操作
        redisTemplate.opsForList();
        //Set集合操作
        redisTemplate.opsForSet();
        //Hash操作
        redisTemplate.opsForHash();
        //ZSet操作
        redisTemplate.opsForZSet();
        //Geospatial操作
        redisTemplate.opsForGeo();
        //Hyperloglog操作
        redisTemplate.opsForHyperLogLog();
        //bigmap操作
        redisTemplate.opsForValue().setBit()
````

##### 自定义RedisTemplate

```java
@Configuration
public class RedisConfig {

    @Bean
    @SuppressWarnings("all")
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory){
        //我们为了方便，一般直接使用<String,Object>
        RedisTemplate<String,Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        //JSON序列化配置
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.DEFAULT);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        //String的序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        //key采用String的徐丽华暗示
        template.setKeySerializer(stringRedisSerializer);
        //hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        //value采用jackson方式序列化
        template.setValueSerializer(jackson2JsonRedisSerializer);
        //hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
```

#### Redis.conf

##### 单位

redis中的单位有1k,1kb,1m,1mb,1g,1gb

````xml
# Note on units: when memory size is needed, it is possible to specify
# it in the usual form of 1k 5GB 4M and so forth:
#
# 1k => 1000 bytes
# 1kb => 1024 bytes
# 1m => 1000000 bytes
# 1mb => 1024*1024 bytes
# 1g => 1000000000 bytes
# 1gb => 1024*1024*1024 bytes
#
# units are case insensitive so 1GB 1Gb 1gB are all the same.
````

#####  inclue 包含其他文件

可以将其他配置文件组合一起，共同形成redis的配置文件

````shell
################################## INCLUDES ###################################

# Include one or more other config files here.  This is useful if you
# have a standard template that goes to all Redis servers but also need
# to customize a few per-server settings.  Include files can include
# other files, so use this wisely.
#
# Notice option "include" won't be rewritten by command "CONFIG REWRITE"
# from admin or Redis Sentinel. Since Redis always uses the last processed
# line as value of a configuration directive, you'd better put includes
# at the beginning of this file to avoid overwriting config change at runtime.
#
# If instead you are interested in using includes to override configuration
# options, it is better to use include as the last line.
#
# include /path/to/local.conf
# include /path/to/other.conf

````

##### bind 绑定ip

````shell
默认情况下，如果未指定“ bind”配置指令，则Redis会侦听服务器上所有可用网络接口的连接。可以使用“ bind”配置指令侦听一个或多个选定接口，然后侦听一个 或更多IP地址。
Examples:

bind 192.168.1.100 10.0.0.1
bind 127.0.0.1 ::1
````

##### port设置端口

````
redis服务的端口设置
````

##### daemonize守护进程方式运行（后台运行）

````she
By default Redis does not run as a daemon. Use 'yes' if you need it.
Note that Redis will write a pid file in /var/run/redis.pid when daemonized.
````

##### pidfile进程文件

````shell
如果指定了pid文件，则Redis会在启动时将其写入指定位置并在退出时将其删除。

当服务器在非守护进程中运行时，如果没有pid文件，则不会创建在配置中指定。 守护服务器时，pid文件即使未指定，也使用，默认为“ /var/run/redis.pid”。

redis会尽力创建pid文件：如果Redis无法创建它不会发生任何不良情况，服务器将启动并正常运行。
````

##### loglevel设置日志等级

````
# debug (a lot of information, useful for development/testing)
# verbose (many rarely useful info, but not a mess like the debug level)
# notice (moderately verbose, what you want in production probably) 生产环境使用
# warning (only very important / critical messages are logged)
loglevel notice
````

##### logfile日志文件名

````
# Specify the log file name. Also the empty string can be used to force
# Redis to log on the standard output. Note that if you use standard
# output for logging but daemonize, logs will be sent to /dev/null
logfile ""
````

##### databases默认数据库数量

````
# Set the number of databases. The default database is DB 0, you can select
# a different one on a per-connection basis using SELECT <dbid> where
# dbid is a number between 0 and 'databases'-1
````

##### 是否显示log

````
always-show-logo yes
````

##### save快照

持久化，在规定的时间内，执行了多少次操作，则将数据进行持久化(.rdb,.aof)。

````shell
save 900 1         #如果900秒内至少有1个key改变，则进行持久化
save 300 10        #如果300秒内至少有10个key改变，则进行持久化
save 60 10000      #如果60秒内至少有1w个key改变，则进行持久化

````

##### stop-writes-on-bgsave-error 持久化出错后是否继续工作

````
stop-writes-on-bgsave-error yes
````

##### rdbcompression开启rdb文件压缩

````shell
rdbcompression yes
````

##### rdbchecksum校验rdb文件

保存rdb文件的时候，进行错误校验 

````shell
rdbchecksum yes
````

##### dbfilename 设置rdb文件名称

````shell
dbfilename dump.rdb
````

##### requirepass设置redis访问密码

````shell
requirepass  123456
#获取在redis客户端中设置
config set requirepass "123456"
````

##### maxclients设置最大客户端的数量

````shell
maxclients 10000
````

##### maxmemory 设置redis可使用的最大值

redis默认不会对内存使用进行限制

````shell
maxmemory 1048576
maxmemory 1048576B
maxmemory 1000KB
maxmemory 100MB
maxmemory 1GB
maxmemory 1000K
maxmemory 100M
maxmemory 1G
````

##### maxmemory-policy内存到达上限的策略

````shell
1、volatile-lru：只对设置了过期时间的key进行LRU（默认值） 
2、allkeys-lru ： 删除lru算法的key   
3、volatile-random：随机删除即将过期key   
4、allkeys-random：随机删除   
5、volatile-ttl ： 删除即将过期的   
6、noeviction ： 永不过期，返回错误
````



##### appendonly开启aof模式

````shell
appendonly  no
````

##### appendfilename持久化文件的名字

````shell
appendfilename "appendonly.aof"
````

##### appendfsync aof同步

````shell
appendfsync everysec  #每秒sync
appendfsync always    #每次修改都sync
appendfsync no        #不执行sync，操作系统自己同步数据，速度最快
````



#### RDB(Redis Database)

Redis是内存数据库，如果不将内存中的数据库状态保存到磁盘，那么服务器进程一旦推出，服务器中的数据也将消失。所有redis提供了持久化的操作。

![](./img/8.jpg)

Redis会单独fork一个子进程来进行持久化，会先将数据写入一个临时文件中，待持久化过程都结束了，在用这个临时文件替换上次持久化好的文件。整个过程中，主进程是不会进行任何IO操作的。这确保了极高的性能。如果需要进行大规模数据的恢复，且对于上一次数据的完整性不是特别敏感，那RDB方式要比AOF方式更高效。RDB的缺点是最后一次持久化后的数据可能丢失。我们默认的就是RDB，一般情况下不需要修改这个配置。

##### 如何恢复RDB文件

将RDB文件放在redis的启动目录下

````shell
# 将RDB文件放在dir目录下面
127.0.0.1:6379> config get dir
1) "dir"
2) "/usr/local/bin"
````

也可以直接去redis.conf里查看dir的配置

````shell
# The working directory.
#
# The DB will be written inside this directory, with the filename specified
# above using the 'dbfilename' configuration directive.
#
# The Append Only File will also be created inside this directory.
#
# Note that you must specify a directory here, not a file name.
dir ./
````

##### RDB的优缺点

**优点**

1. 适合大数据的数据恢复！
2. 对数据的完整性要求不高!

**缺点**

1. 需要一定的时间进行进程操作,如果redis宕机了，最后一次修改的数据就会没了！
2. fork进程的时候会占用的一定的内容空间。



#### AOF (Append Only File)

 以日志的形式来记录每个写操作，将Redis执行过的所有写指令记录下来(读操作不记录)，只许追加文件但不可以改写文件，redis启动之初会读取该文件重新构建数据，换言之，redis重启的话就根据日志文件的内容将写指令从前到后执行一次以完成数据的恢复工作。

<img src="./img/9.png" style="zoom:67%;" />

##### 开启aof

将appendonly的值设置为yes，则开启了aof,开启了aof后，在redis正常运行期间就不会出发rdb了，但是redis被shutdown后会出发rdb操作，但对aof方式没有任何影响。

````shell
# By default Redis asynchronously dumps the dataset on disk. This mode is
# good enough in many applications, but an issue with the Redis process or
# a power outage may result into a few minutes of writes lost (depending on
# the configured save points).
#
# The Append Only File is an alternative persistence mode that provides
# much better durability. For instance using the default data fsync policy
# (see later in the config file) Redis can lose just one second of writes in a
# dramatic event like a server power outage, or a single write if something
# wrong with the Redis process itself happens, but the operating system is
# still running correctly.
#
# AOF and RDB persistence can be enabled at the same time without problems.
# If the AOF is enabled on startup Redis will load the AOF, that is the file
# with the better durability guarantees.
#
# Please check http://redis.io/topics/persistence for more information.
appendonly yes
````

##### AOF文件检测

如果aof文件有错，或者被破坏了，这个时候redis是启动不起来的，因此我们需要修复这个文件。

redis给我们提供了一个工具`redis-check-aof --fix`,

````shell
[root@master bin]# redis-check-aof --fix appendonly.aof 
0x              87: Expected \r\n, got: 6473
AOF analyzed: size=153, ok_up_to=110, diff=43
This will shrink the AOF from 153 bytes, with 43 bytes, to 110 bytes
Continue? [y/N]: y
Successfully truncated AOF
[root@master bin]# 
````

如果文件正常了，这个时候就可以redis-server就能够启动起来了。

##### AOF重写机制(rewrite)

**重写机制是什么：**

​      AOF采用文件追加方式，文件会越来越大为避免出现此种情况，新增了重写机制, 当AOF文件的大小超过所设定的阈值时，Redis就会启动AOF文件的内容压缩，只保留可以恢复数据的最小指令集.可以使用命令bgrewriteaof

​    **重写原理**

​      AOF文件持续增长而过大时，会**fork**出一条新进程来将文件重写(也是先写临时文件最后再rename)，遍历新进程的内存中数据，每条记录有一条的Set语句。重写aof文件的操作，并没有读取旧的aof文件，而是将整个内存中的数据库内容用命令的方式重写了一个新的aof文件，这点和快照有点类似

​    **触发机制**

​      Redis会记录上次重写时的AOF大小，默认配置是当AOF文件大小是上次rewrite后大小的一倍且文件大于64M时触发。请见配置文件默认是，auto-aof-rewrite-percentage 100的意思是超过100%，也就是一倍；auto-aof-rewrite-min-size 64mb是查过64mb。

​      这里插一句，假如你到一家新公司，老板把公司吹的天花乱坠，什么技术有多牛，业务量有多大。如果他们使用aof来做redis持久化，这时候，你只要偷偷看一眼他们redis的这个配置项auto-aof-rewrite-min-size，如果是64mb，那么你就应该心领神会了——这个公司要么业务量根本没这么大，要么这个公司的人并不怎么牛。真正大型系统，3gb都是起步，64mb根本是在搞笑。这个配置时观察一个公司水平的一个很好的维度。

##### AOF的优缺点

**优点**

1. 每一次修改都同步，文件完整性会更好！

2. 每秒同步一次，可能会丢失一秒的数据

**缺点**

1. aop文件远远大于rdb，因此数据恢复的速度远小于rdb
2. AOF的运行效率也比rdb慢，因此redis的默认配置是rdb而不是aop

#### 发布订阅

##### 概念讲解

Redis发布订阅（pub/sub)是一种消息通信模式：发布者(pub)发送消息，订阅者（sub)接受消息。

<img src="./img/10.jpg" style="zoom:80%;" />



Redis 发布订阅 (pub/sub) 是一种消息通信模式：发送者 (pub) 发送消息，订阅者 (sub) 接收消息。

Redis 客户端可以订阅任意数量的频道。

下图展示了频道 channel1 ， 以及订阅这个频道的三个客户端 —— client2 、 client5 和 client1 之间的关系：

![img](./img/11.png)

当有新消息通过 PUBLISH 命令发送给频道 channel1 时， 这个消息就会被发送给订阅它的三个客户端：

![img](./img/12.png)

##### redis发布订阅的命令

下表列出了 redis 发布订阅常用命令：

| 序号 | 命令及描述                                                   |
| :--- | :----------------------------------------------------------- |
| 1    | [PSUBSCRIBE pattern [pattern ...\]](https://www.runoob.com/redis/pub-sub-psubscribe.html) 订阅一个或多个符合给定模式的频道。 |
| 2    | [PUBSUB subcommand [argument [argument ...\]]](https://www.runoob.com/redis/pub-sub-pubsub.html) 查看订阅与发布系统状态。 |
| 3    | [PUBLISH channel message](https://www.runoob.com/redis/pub-sub-publish.html) 将信息发送到指定的频道。 |
| 4    | [PUNSUBSCRIBE [pattern [pattern ...\]]](https://www.runoob.com/redis/pub-sub-punsubscribe.html) 退订所有给定模式的频道。 |
| 5    | [SUBSCRIBE channel [channel ...\]](https://www.runoob.com/redis/pub-sub-subscribe.html) 订阅给定的一个或多个频道的信息。 |
| 6    | [UNSUBSCRIBE [channel [channel ...\]]](https://www.runoob.com/redis/pub-sub-unsubscribe.html) 指退订给定的频道。 |

启动一个redis客户端，订阅

````shell
[root@master bin]# redis-cli 
127.0.0.1:6379> subscribe yxlm
Reading messages... (press Ctrl-C to quit)
1) "subscribe"
2) "yxlm"
3) (integer) 1
1) "message"
2) "yxlm"
3) "lalala,demaxiya"
````

启动另一个redis客户端，发布消息

````shell
[root@master ~]# redis-cli 
127.0.0.1:6379> PUBLISH yxlm lalala,demaxiya
(integer) 1
127.0.0.1:6379> 
````

##### 使用场景

1. 消息系统
2. 实时聊天（聊天室）
3. 关注系统