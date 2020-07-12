### spark数据持久化

spark可用将数据持久化的内存或者磁盘上，以便于后面的rdd进行迭代运算的时候，避免多次计算同一个rdd。

````scala
# rdd的两次执行
val result = input.map(line => line * line)
println(result.count())
println(result.collect().mkString("."))
````

下表表示spark在持久化的几种级别

|        级别         | 使用的空间 | CPU时间 | 是否在内存中 | 是否在磁盘上 |                   备注                   |
| :-----------------: | :--------: | :-----: | :----------: | :----------: | :--------------------------------------: |
|     MEMERY_ONLY     |     高     |   低    |      是      |      否      |                                          |
|   MEMERY_ONLY_SER   |     低     |   高    |      是      |      否      |                                          |
|   MEMERY_AND_DISK   |     高     |  中等   |     部分     |     部分     | 如果数据在内存钟放不下，则溢出写道磁盘上 |
| MEMERY_AND_DISK_SER |     低     |   高    |     部分     |     部分     | 如果数据在内存钟放不下，则溢出写道磁盘上 |
|      DISK_ONLE      |     低     |   高    |      否      |      是      |                                          |

````scala
# rdd的两次执行
val result = input.map(line => line * line)
result.persist(StorageLevel.DISK_ONLE)
println(result.count())
println(result.collect().mkString("."))
````

如果要缓存的数据太多，内存中放不下，Spark会自动利用最近最少使用(LRU)的缓存策略把最老的分区从内存中移除。对于仅把数据存放在内存中的缓存级别，下一次要用到已经被移除的分区时，这些分区就需要重新计算。但是对于使用内存与磁盘的缓存级别的分区来说，被移除的分区都会写入磁盘。



### Spark的数据读取

Spark可用从任何支持Java数据库连接(JDBC)的关系型数据库中读取数据，包括Mysql、Postgre。在Spark中提供了JdbcRdd，

````scala
	def createConnection() = {
    Class.forName("com.mysql.jdbc.Driver").newInstance();
    DriverManager.getConnction("jdbc:mysql://localhost:3306/test")
    }
    val extractValues(r:ResultSet) = {
        (r.getInt(1),r.getString(2))
    }
    //查询必须包含两个?用于对结果进行分区的参数占位符
    val data = new JdbcRDD(sc,createConncetion,"SELECT * FROM test WHERE ? <= id AND id <= ?",lowerBound=1,upperBound=3,numPartitions=2,mapRow=extractValues)

````

该api有一个最大的好处就是，用户能够自己制定JDBC连接。JdbcRdd接收这样几个参数

````java
JdbcRDD(SparkContext sc, scala.Function0<java.sql.Connection> getConnection, String sql, long lowerBound, long upperBound, int numPartitions, scala.Function1<java.sql.ResultSet,T> mapRow, scala.reflect.ClassTag<T> evidence$1) 
````



* 首先，要提供一个用于对数据库创建连接的函数。这个函数让每个节点在连接必要的配置后创建自己读取数据的连接
* 接下来，要提供一个可用读取一定范围内数据的查询，以及查询参数中lowerBound和upperBound的值。这些参数可用让Spark在不同机器上查询不同范围的数据，这样就不会因尝试在一个节点上读取所有数据而遭遇性能瓶颈。
* 这个函数的最后一个参数是一个可用将输出结果从java.sql.ResultSet转为操作数据有用的格式的函数。在上例中我们会得到一个(Int,String)对