1. 尽量不用 select *，而是 select 具体的字段名

2. 返回要加limit 分页

3. 选择合适的文件类型，推荐Parquet

4. 选择数值类型的字段进行分区，比如年月日

5. "COMPUTE STATS收集表的统计信息，可以对join语句进行自动优化，分析出最简单的方式是通过执行

6. COMPUTE STATS来收集涉及到的所有表的统计信息，并让 Impala 基于每一个表的大小、每一个列不同值的个数、等等信息自动的优化查询注：如果COMPUTE STATS占用了大多数CPU周期，则会对同时运行的其他查询产生负面影响"

7. 如果用**COMPUTE STATS**关键字自动优化的效果不好，考虑使用STRAIGHT_JOIN关键字，保持sql原有的执行顺序COMPUTE INCREMENTAL STATS [db_name.]table_name [PARTITION (partition_spec)]  统计增量

8. "再impala 2.1之，COMPUTE INCREMENTAL STATS命令，将大表分区存储后，可用该命令增量的对各分区进行统计，并且当分区变动时只对变化部分进行扫描，并更新相应的统计信息；
    select STRAIGHT_JOIN id, name
    from test_parquet t inner join other_parquet o
    on t.id = o.id

"

9. "show table stats  tablename;
   +-------+--------+------+--------------+--------+-------------------+
   | #Rows | #Files | Size | Bytes Cached | Format | Incremental stats |
   +-------+--------+------+--------------+--------+-------------------+
   | -1 | 0 | 0B | NOT CACHED | TEXT | false |
   +-------+--------+------+--------------+--------+-------------------+

Rows如果为负一，则表示该表还未被统计过，可使用COMPUTE STATS tablename对表的统计信息进行创建和更新。"

10. 使用alter table  tablename add partition(year=2014);将表进行分区"
11. 缓存某一张表

* 首先发出hdfs cacheadmin命令以设置一个或多个缓存池，这些缓存池与impalad守护程序由同一用户拥有（通常是impala）。例如：hdfs cacheadmin -addPool four_gig_pool -owner impala -limit 4000000000（存储池的大小）

* 然后执行alter table census set cached in 'pool_name'将表缓存到hdfs的缓存中去

* alter table census set uncached; 取消表缓存

* set cached in 'pool_name' with replication = 4;设置某张表被缓存的份数
  12. "这边找到一个为啥我最近弄的数据测试出来的效果很慢的原因之一
      就是impala 使用insert into table  values(),(),(),() 就会生成一个文件
      我代码时一个insert into生成1000条数据  所以1000 W数据就会生成1W个文件   后面我尝试分区的时候 把数据弄到了12个文件种  发现速度提升了几倍"
  13. 差集的语句建议用join的方式实现。select count(0) from rk001b3 left join rk001b4 on rk001b3.id = rk001b4.id where rk001b4.id is null ;
  14. 在执行完sql后，输入SUMMARY可以查看sql每个阶段的耗时