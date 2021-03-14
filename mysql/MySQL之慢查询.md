# MySQL 慢查询日志如何查看及配置

## 查看配置

MySQL 慢查询日志是排查问题 SQL 语句，以及检查当前 MySQL 性能的一个重要功能。

- 查看是否开启慢查询功能：

```shell
mysql> show variables like 'slow_query%';
+---------------------+------------------------------------+
| Variable_name       | Value                              |
+---------------------+------------------------------------+
| slow_query_log      | OFF                                |
| slow_query_log_file | /var/lib/mysql/instance-1-slow.log |
+---------------------+------------------------------------+
2 rows in set (0.01 sec)
mysql> show variables like 'long_query_time';
+-----------------+-----------+
| Variable_name   | Value     |
+-----------------+-----------+
| long_query_time | 10.000000 |
+-----------------+-----------+
1 row in set (0.00 sec)
```

说明：

slow_query_log 慢查询开启状态
slow_query_log_file 慢查询日志存放的位置（这个目录需要MySQL的运行帐号的可写权限，一般设置为MySQL的数据存放目录）
long_query_time 查询超过多少秒才记录

## 配置

临时配置

默认没有开启慢查询日志记录，通过命令临时开启：

```shell
mysql> set global slow_query_log='ON';
Query OK, 0 rows affected (0.00 sec)
mysql> set global slow_query_log_file='/var/lib/mysql/instance-1-slow.log';
Query OK, 0 rows affected (0.00 sec)
mysql> set global long_query_time=2;
Query OK, 0 rows affected (0.00 sec)
```

永久配置

修改配置文件达到永久配置状态：

```shell
/etc/mysql/conf.d/mysql.cnf
[mysqld]
slow_query_log = ON
slow_query_log_file = /var/lib/mysql/instance-1-slow.log
long_query_time = 2
```

配置好后，重新启动 MySQL 即可。

测试
通过运行下面的命令，达到问题 SQL 语句的执行：

```shell
mysql> select sleep(2);
+----------+
| sleep(2) |
+----------+
|        0 |
+----------+
1 row in set (2.00 sec)
```

然后查看慢查询日志内容：

```shell
$ cat /var/lib/mysql/instance-1-slow.log
/usr/sbin/mysqld, Version: 8.0.13 (MySQL Community Server - GPL). started with:
Tcp port: 3306  Unix socket: /var/run/mysqld/mysqld.sock
Time                 Id Command    Argument
/usr/sbin/mysqld, Version: 8.0.13 (MySQL Community Server - GPL). started with:
Tcp port: 3306  Unix socket: /var/run/mysqld/mysqld.sock
Time                 Id Command    Argument
# Time: 2018-12-18T05:55:15.941477Z
# User@Host: root[root] @ localhost []  Id:    53
# Query_time: 2.000479  Lock_time: 0.000000 Rows_sent: 1  Rows_examined: 0
SET timestamp=1545112515;
select sleep(2);
```