### greenplum导入csv数据

**方法一**

```
COPY products FROM '/path/to/input.csv' WITH csv;
```

**方法二**

````sql
//添加字段头导出
psql -d  databasename   -h  localhost     -p 5432  -c "\copy (select * from  tablename  limit 10000 ) to /tmp/my_data2.csv  with csv header  delimiter '|' " 
 
 //不添加字段头导出
 psql -d  databasename   -h  localhost     -p 5432  -c "\copy (select * from  tablename  limit 10000 ) to /tmp/my_data2.csv  with csv delimiter '|' "
````



### 获取表的元信息

表的元信息都在informatoin.schema这张表中，