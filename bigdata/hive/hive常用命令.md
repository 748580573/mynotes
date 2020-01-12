## hive初始化元数据

schematool -dbType mysql -initSchema

## hive启动metastored

hive --service metastore &