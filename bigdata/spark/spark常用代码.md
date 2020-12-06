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