### hive复制表结果及表数据

在使用Hive的过程中，复制表结构和数据是很常用的操作，本文介绍两种复制表结构和数据的方法。

1、复制非分区表表结构和数据

Hive集群中原本有一张bigdata17_old表，通过下面的SQL语句可以将bigdata17_old的表结构和数据复制到bigdata17_new表：
`CREATE TABLE bigdata17_new AS SELECT * FROM bigdata17_old;`

如果是分区表，则必须使用like关键字复制表结构，包括分区，然后用insert语句将老表的数据插入新表中。

2、复制分区表表结构和数据
复制表SQL：
`CREATE TABLE bigdata17_new like bigdata17_old;`

复制数据sql：
`insert overwrite table bigdata17_new partition(dt) select * from bigdata17_old;`

如果遇到bigdata17_old表数据量巨大，有T以上的级别时，上述方法的效率则比较低。下面介绍一种快速复制表结构和表数据的方法。

从旧表中复制表结构，这个和上面介绍方法是一样的：
`CREATE TABLE bigdata17_new like bigdata17_old;`

然后使用hadoop fs - cp命令将bigdata17_old旧表的数据拷贝到bigdata17_new新表：

```
hadoop fs -cp /user/warehouse/bigdata17.db/bigdata17_old/* /user/warehouse/bigdata17.db/bigdata17_new/
```

然后执行`MSCK REPAIR TABLE new_table;`命令让两张表的分区元数据保持一致。

详细使用过程如下：
bigdata17_old表有两个字段，id和dt，其中dt是分区字段，一共有4条记录，两个分区：

```
hive> desc bigdata17_old;
OK
id                      int                                         
dt                      string                                      
                 
# Partition Information          
# col_name              data_type               comment             
                 
dt                      string                                      
Time taken: 0.147 seconds, Fetched: 7 row(s)

hive> select * from bigdata17_old;
OK
15      2018-10-13
18      2018-10-13
12      2018-10-14
13      2018-10-14
Time taken: 0.118 seconds, Fetched: 4 row(s)

hive> show partitions bigdata17_old;
OK
dt=2018-10-13
dt=2018-10-14
Time taken: 0.113 seconds, Fetched: 2 row(s)
```

创建表结构和bigdata17_old表一模一样的表bigdata17_new:

```
create table bigdata17_new like bigdata17_old;
```

查看表bigdata17_new的表结构：

```
hive> show partitions bigdata17_new;
OK
Time taken: 0.153 seconds
hive> desc bigdata17_new;
OK
id                      int                                         
dt                      string                                      
                 
# Partition Information          
# col_name              data_type               comment             
                 
dt                      string                                      
Time taken: 0.151 seconds, Fetched: 7 row(s)
```

由于表bigdata17_new还没有数据，因此该表中没有分区信息。

将bigdata17_old目录下的数据文件拷贝到bigata17_new目录下：

```
[root@hadoop-master hive_test]# hadoop fs -cp /user/hive/warehouse/bigdata17.db/bigdata17_old/* /user/hive/warehouse/bigdata17.db/bigdata17_new/;
18/10/13 19:02:54 WARN util.NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable
[root@hadoop-master hive_test]# hadoop fs -ls /user/hive/warehouse/bigdata17.db/bigdata17_new/
18/10/13 19:03:26 WARN util.NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable
Found 2 items
drwxr-xr-x   - root supergroup          0 2018-10-13 19:02 /user/hive/warehouse/bigdata17.db/bigdata17_new/dt=2018-10-13
drwxr-xr-x   - root supergroup          0 2018-10-13 19:02 /user/hive/warehouse/bigdata17.db/bigdata17_new/dt=2018-10-14
```

查看表bigdata17_new的分区信息：

```
hive> show partitions bigdata17_new;
OK
Time taken: 0.125 seconds
```

虽然数据拷贝过来了，但是表bigdata17_new的分区信息还没更新到metastore中，因此需要使用MSCK命令修复bigdata17_new的分区信息，执行该命令后就会把bigdata17_new的分区信息更新到hive metastore中：

```
hive> MSCK REPAIR TABLE bigdata17_new;
OK
Partitions not in metastore:    bigdata17_new:dt=2018-10-13     bigdata17_new:dt=2018-10-14
Repair: Added partition to metastore bigdata17_new:dt=2018-10-13
Repair: Added partition to metastore bigdata17_new:dt=2018-10-14
Time taken: 0.21 seconds, Fetched: 3 row(s)
```

查看表bigdata17_new的表结构和查询表数据：

```
hive> show partitions bigdata17_new;
OK
dt=2018-10-13
dt=2018-10-14
Time taken: 0.137 seconds, Fetched: 2 row(s)
hive> select * from bigdata17_new;
OK
15      2018-10-13
18      2018-10-13
12      2018-10-14
13      2018-10-14
Time taken: 0.099 seconds, Fetched: 4 row(s)
```

表bigdata17_new已经创建完毕，它的表结构、分区信息和表bigdata17_old一样，数据也一模一样。

如果是跨Hive集群复制表和数据，又要怎么做呢？

其实和上述步骤差不多，只是因为跨Hive集群，新表和旧表之间不能使用hadoop cp命令拷贝数据。假设有两个集群,分区为Hive1和Hive2,两个Hive集群都有表bigdata17_order,表结构完全一样。怎么将集群Hive1中的bigdata17_order表的数据拷贝到集群Hive2中的bigdata17_order表中呢？下面介绍实现步骤：
1、将表Hive1集群bigdata17_order目录下的数据下载到本地：

```
hadoop fs -get /user/warehouse/bigdata17.db/bigdata17_order/* /home/hadoop/hivetest/bigdata17_order/
```

2、通过hadoop fs -put命令将本地数据上传到集群Hive2中的bigdata17_order目录中：

```
hadoop fs -put /home/hadoop/hivetest/bigdata17_order/* /user/warehouse/bigdata17.db/bigdata17_order/
```

3、在集群Hive2中执行MSCK命令修复表bigdata17_order的分区信息：

```
MSCK REPAIR TABLE bigdata17_order;
```

Hive MSCK命令的用法请参考：[一起学Hive——使用MSCK命令修复Hive分区](http://www.bigdata17.com/2018/10/10/hivemsck.html/)