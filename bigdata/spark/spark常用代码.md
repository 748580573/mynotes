# 常用的代码

## 添加列

```java
boolean isRepeateFields = false;
Set<String> fieldSet = new HashSet<>();
StructField[] fields = dataset.schema().fields();
List<String> dropNames = new ArrayList<>();
for (int i = 0;i < fields.length;i++){
   if (fieldSet.contains(fields[i].name())){
      String newName = fields[i].name()+new Random().nextInt(1000);
      fields[i] = new StructField(newName, fields[i].dataType(), fields[i].nullable(), fields[i].metadata());
      dataset = dataset.sparkSession().createDataFrame(dataset.rdd(),new StructType(fields));
      dropNames.add(newName);
      isRepeateFields = true;
   }else {
      fieldSet.add(fields[i].name());
   }
}
```

## 删除列

````java
dataset = dataset.drop(dropName);
````

## JDBC读取数据

````java
properties.put("user", username);
properties.put("password", password);
properties.put("fetchsize","5000");
dataset = spark.read()
		.option("driver", driver)
		.option("url",url)
		.option("dbtable",tableName)
		.option("batchsize", "5000")
		.jdbc(url, tableName, predicates, properties);
````

## JDBC写输入数据

````java
 var prop = new Properties()
    prop.setProperty("user","root")
    prop.setProperty("password","wuheng");
    dataset
      .write
      .option("driver","com.mysql.cj.jdbc.Driver")
      .mode(SaveMode.Overwrite)
      .jdbc(url,"test2",prop)
````

## 以空表头创建dataset

```java
   public static void main(String[] args) {
	SparkSession sparkSession = SparkSession.builder().appName("Test").master("local")
           .config("spark.sql.inMemoryColumnarStorage.compressed", "true").getOrCreate();
	*//给定一串表头*
	String colstr = "编号,姓名,性别,年龄";
	*//以,分割*
	String[] cols = colstr.split(",");
	List<StructField> fields = new ArrayList<>();
	for (String fieldName : cols) {
		*//创建StructField，因不知其类型，默认转为字符型*
		StructField field = DataTypes.createStructField(fieldName, DataTypes.StringType, true);
		fields.add(field);
	}
	*//创建StructType*
	StructType schema = DataTypes.createStructType(fields);
	List<Row> rows = new ArrayList<>();
	*//创建只包含schema的Dataset*
	Dataset<Row> data = sparkSession.createDataFrame(rows, schema);
	data.show();
}
```

## 通过sql获取json里的数据

get_json_object(string json_string, string path)

说明：
第一个参数填写json对象变量，第二个参数使用$表示json变量标识，然后用 . 或 [] 读取对象或数组；如果输入的json字符串无效，那么返回NULL。

````shell
with data_table as (select  "{
  \"timestamp\": \"2021-03-23T06:45:11.460Z\",
  \"metadata\": {
    \"beat\": \"filebeat\",
    \"type\": \"doc\",
    \"version\": \"6.6.1\",
    \"topic\": \"gateway_track_log\"
  },
  \"service_port\": \"1111\",
  \"service_name\": \"gateway\",
  \"service_ip\": [{\"ip_a\":\"100.100.89.09\"},{\"ip_b\":\"100.100.89.10\"}],
  \"center_name\": \"open\"
}" as col) 
select get_json_object(col,'$.timestamp') as `timestamp`,
       get_json_object(col,'$.metadata.type') as metadata_type,
       get_json_object(col,'$.service_ip[0].ip_a') as service_ip_a 
  from data_table;
````

## 日期作差datediff

datediff(endDate, startDate) - Returns the number of days from `startDate` to `endDate`.

Examples:`> SELECT datediff('2009-07-31', '2009-07-30');`

