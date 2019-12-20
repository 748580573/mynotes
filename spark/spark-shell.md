# spark-shell sql

## 切换数据库
spark.catalog.setCurrentDatabase("miner")
## 执行sql
spark.sql("select * from 表名")

## 设置sql的分区数
spark.conf.set("spark.sql.shuffle.partitions","200")

## 开启两表内连接
config(“spark.sql.crossJoin.enabled”, “true”)