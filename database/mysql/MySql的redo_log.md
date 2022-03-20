# MySQL的redo log

### 概念

每个月都有那么几天特别希望工资如果能提前发那改多好啊！恰巧今天发工资了，我的账户余额只剩50块钱了，工资的财务姐姐汇来3000块钱。

````sql
update account set money = money + 3000 where id = 100
````

我们知道在MySQL中，更新数据后，数据是放在buffer pool中，不会立马就将数据写到磁盘上，也就是说这个时候磁盘上还是老数据。要是这个时候机房突然断电了，内存中的数据没了，咋办？钱钱没领到，今天过了不久只有挨饿了么。这是肯定不语序发生的，为了做到这一点`redo log`诞生了，它能够保证即使服务器宕机，服务器重启后数据仍是修改完成后的数据。

有了redo log后，数据的更新流程就变成这样了：

1. 修改Buffer Pool 对应页中的信息。（假设页已经存在在Buffer Pool中）
2. 新增redo log 记上一笔：把id=24 的账户的余额改为5200。 （当然实际情况可能还要复杂，比如要记录索引等等其他修改，顺带一条redo log 日志的类型在Mysql 5.7.21中有53种之多）
3. 返回更新结果

这样，即使修改完成后，Buffer Pool 中的数据来不及刷入磁盘，数据库重启之时，还是可以从redo log 中读取修改的内容，`重新再做一遍同样的操作（redo)`。保证了Buffer Pool 中的数据丢失还能找回来。

### 为什么redo log 的信息在崩溃的时候不会丢失？

redo log 是存放在磁盘的。

- innodb_log_group_home_dir 指定redo日志文件存放的目录。
- innodb_log_file_size 指定每个redo日志文件的大小。
- innodb_log_files_in_group 指定redo日志的个数

redo log 可以存放的空间为：`innodb_log_file_size × innodb_log_files_in_group`

那么问题来了，我们知道Buffer Pool 是通过减少磁盘操作，从而提升性能的。现在为了保证持久性，写入redo log 还是要进行磁盘操作，这样不是白费力了吗？
redo log 要进行磁盘操作这个是不可避免的，但是与Buffer Pool `磁盘随机写入`比， redo log 的磁盘操作有几个优势： 

1. 引入redo log buffer，按组写入，不是一条条写。

2. 磁盘为顺序写入。

当然也可以记住结论就是 redo log 日志的磁盘操作比起 Buffer Pool 的磁盘操作 性能要好很多。

### 怎么保证redo log buffer 中的信息在崩溃的时候不会丢失呢？

照搬Buffer Pool 的原理,redo log 也不是直接写入磁盘的，redo log 会先写入 redo log buffer 中，然后再写入磁盘。 那么问题又又又来了，怎么保证redo log buffer 中的信息在崩溃的时候不会丢失呢？
redo log buffer 的刷盘时机：

- redo log buffer 空间不足的时候。
- 事务提交的时候
- 后台线程在不停的刷
- 服务正常关闭的时候
- `checkpoint`的时候

(可以比较下Buffer Pool 的刷盘时机，看看两者有什么不同有什么相同）
从刷盘时机来看，redo log buffer的刷盘时机更为频繁，尤其是`事务提交的时候`这一条保证了提交的事务redo log 就已经存储到了磁盘中。
`innodb_flush_log_at_trx_commit 取值范围 0，1，2`
这个变量是用来控制事务提交的时候是否要刷新redo log buffer 到磁盘中。
0：标识事务提交不会向磁盘同步redo log。
1：默认值，每次提交事务向磁盘同步redo log。
2：每次提交事务将redo log 写到操作系统的缓冲区中，但没有真正写入磁盘。
这三种方式都各有利弊：
0: 虽说`按组批量顺序写入`磁盘提高了不少磁盘操作效率，但是终归还是有点性能损耗的，把变量设置成0，那么事务提交的时候就完全避免了磁盘操作， 交由后台线程去刷新磁盘。 这样在大量频繁修改的业务场景的能够提升性能，但是同样要面对服务器挂了有数据丢失的风险。
1: 性能不如0，但是保证了提交的事务数据不丢失。
2: 性能介于0，1之间。能保证如果操作系统没挂，数据库挂了，事务的持久性还能保证，但是如果操作系统也挂了数据就会丢失。
（根据业务场景和服务器配置挑选合适的`innodb_flush_log_at_trx_commit`也能提升性能）

### 总结

- redo log 使用来保证InnoDB 的持久性的。
- redo log 是保存在磁盘文件中的，但是通过`按组，顺序写入`的方式提升磁盘IO效率
- redo log buffer 向磁盘刷入的时机更为频繁。可以通过`innodb_flush_log_at_trx_commit`控制事务提交是否刷盘。
- redo log 由于空间有限，所以通过一次次的checkpoint 来标记可以覆盖的空间信息。