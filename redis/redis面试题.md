## Redies支持的数据类型

string类型

格式：set key value

string类型是二进制安全的。意思是redis的string可以包含任何数据。比如jpg图片或者序列化的对象。

string类型是redis最基本的数据类型，一个键最大能存储512M。



Hash(哈希)

格式：hmset name  key1 value1  key2 value2

redis hash是一个键值(key=>value)对整合

redis hash是一个string类型的field和value的映射表，hash特别适合用于存储对象。





List(列表)

Redis列表是简单的字符串列表，按照插入顺序排序。你可以添加一个元素到列表的头部（左边）或者尾部（右边）

格式：lpush name value

在key对应的list的头部添加字符串元素

格式：rpush name value

在key对应的list的尾部添加字符串元素

格式：lrem name index

key对应的list中删除count个和value相同的元素

格式：llen name

返回key对应的list的长度



Set（集合）

格式：sadd name value

Redis的set是string类型的无序集合。

集合是通过哈希表实现的，所以添加，删除，查找的复杂度都是O(1)。



zset(sortes set：有序集合)

格式：zadd name value

Redis zset和set一样也是string类型元素的集合，且不允许重复的成员。

不同的是每个元素都会关联一个double类型的分数。redis正式通过分数来为集合中的成员进行从小到大的排序

zset的成员是唯一的，但分数（score)确实可以重复的。



什么是redis持久化？redis有哪几种持久化方式？优缺点是什么？

持久化就是把内存的数据写到磁盘中去，防止服务宕机了，内存数据丢失。

redis提供了两种持久化方式：RDB(默认)和AOF

RDB:

rdb是redis DataBase缩写

功能核心函数rdbSave(生成RDB文件)和rdbLoad(从文件加载内存)两个函数

![](./img/2.png)

AOF：

Aof是Append-only file缩写

![](./img/1.png)

每当执行服务器（定时）任务或者函数时fulshAppendOnlyFile函数都会被调用，这个函数执行以下两个工作

aop写入保存

1、write:根据条件，将aof_buf中的缓存写入到AOF文件

2、save:根据条件，调用fsync或fdatasync函数，将AOF文件保存到磁盘中。

**存储结构**

内容是redis通讯协议(RESP)格式的命令文本存储

**比较**

1、aof文件比rdb更新频率高，优先使用AOP还原数据

2、aop比rdb更安全也更大

3、rdb性能比aof好

4、如果两个都配了，优先加载AOF

**刚刚上面你有提到redis通讯协议(RESP )，能解释下什么是RESP？有什么特点？（可以看到很多面试其实都是连环炮，面试官其实在等着你回答到这个点，如果你答上了对你的评价就又加了一分）**

RESP是redis客户端和服务端之间使用的一种通讯协议：

RESP的特点：实现简单、快速解析、可读性好

For Simple Strings the first byte of the reply is "+" 回复

For Errors the first byte of the reply is "-" 错误

For Integers the first byte of the reply is ":" 整数

For Bulk Strings the first byte of the reply is "$" 字符串

For Arrays the first byte of the reply is "*" 数组

