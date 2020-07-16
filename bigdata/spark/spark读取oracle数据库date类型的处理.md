在用spark 2.1读取oracle的date类型的时候，读取的数据结果会丢失掉时分秒精度（难道timestamp是摆设），为了处理这个问题，可以在spark中添加一下代码

````java
 public static void oracleInit() {
     JdbcDialect dialect = new JdbcDialect() {

​        //判断是否为oracle库
​        @Override
​        public boolean canHandle(String url) {
​          return url.startsWith("jdbc:oracle");
​        }

​        //用于读取Oracle数据库时数据类型的转换
​        @Override
​        public Option<DataType> getCatalystType(int sqlType, String typeName, int size, MetadataBuilder md){
​          if (sqlType == Types.DATE && typeName.equals("DATE") && size == 0)
​             return Option.apply(DataTypes.TimestampType);
​          return Option.empty();
​        }

​        //用于写Oracle数据库时数据类型的转换
​        @Override
​        public Option<JdbcType> getJDBCType(DataType dt) {
​          if (DataTypes.*StringType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("VARCHAR2(255)", Types.*VARCHAR*));
​          } else if (DataTypes.*BooleanType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("NUMBER(1)", Types.*NUMERIC*));
​          } else if (DataTypes.*IntegerType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("NUMBER(10)", Types.*NUMERIC*));
​          } else if (DataTypes.*LongType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("NUMBER(19)", Types.*NUMERIC*));
​          } else if (DataTypes.*DoubleType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("NUMBER(19,4)", Types.*NUMERIC*));
​          } else if (DataTypes.*FloatType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("NUMBER(19,4)", Types.*NUMERIC*));
​          } else if (DataTypes.*ShortType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("NUMBER(5)", Types.*NUMERIC*));
​          } else if (DataTypes.*ByteType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("NUMBER(3)", Types.*NUMERIC*));
​          } else if (DataTypes.*BinaryType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("BLOB", Types.*BLOB*));
​          } else if (DataTypes.*TimestampType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("DATE", Types.*DATE*));
​          } else if (DataTypes.*DateType*.sameType(dt)) {
​             return Option.*apply*(
​                  new JdbcType("DATE", Types.*DATE*));
​          } else if (DataTypes.*createDecimalType*()
​                .sameType(dt)) { //unlimited
/*             return DecimalType.Fixed(precision, scale)
​                  =>Some(JdbcType("NUMBER(" + precision + "," + scale + ")",
​                  java.sql.Types.NUMERIC))*/
​             return Option.*apply*(
​                  new JdbcType("NUMBER(38,4)", Types.*NUMERIC*));
​          }
​          return Option.*empty*();
​        }
​     };
​     //注册此方言
​      JdbcDialects.*registerDialect*(dialect);
//     JdbcDialects.unregisterDialect(dialect);
  }


SparkConf conf = new SparkConf();
conf.setAppName("Test");
//设置启动方式为本地方式
conf.setMaster("local[2]");
JavaSparkContext sc = new JavaSparkContext(conf);

SQLContext sqlContext = new SQLContext(sc);
oracleInit();
System.out.println("================"+sqlContext.getSQLDialect());

Map<String, String> options = new HashMap<String, String>();
options.put("user", "whst");
options.put("password", "123456");
options.put("url", *url*);
options.put("dbtable", "WHST_EXAMPLE");
//一次取多少行数据
options.put("fetchSize", "20");
options.put("driver", *driver*);
DataFrame jdbcDF = sqlContext.read().format("jdbc").options(options).load();
jdbcDF.show(false);
````



* **beforeFetch**：判断该dialect能否处理该jdbc url
* **getCatalystType**：为给定的jdbc元数据信息获取自定义的数据类型
  * sqlType ：sql的类型(java.sql.Types)
  * typeName :sql的名字("BIGINT UNSIGNED")
  * size：类型的大小
  * md ：与此类型相关的结果元数据

> 详细文档，参考：https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/jdbc/JdbcDialect.html

