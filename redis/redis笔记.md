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

