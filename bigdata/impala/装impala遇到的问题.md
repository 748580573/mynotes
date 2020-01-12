#### /lib/lsb/init-functions: No such file or directory

yum install redhat-lsb

#### Impala无法启动catalog,报com.mysql.jdbc.Driver was not found.....CLASSPATH....

全文可参考：

http://lxw1234.com/archives/2017/06/862.htm

 

PS：此步需要在所有节点以root用户执行。

将$HIVE_HOME/lib/mysql-connector-java-5.1.40-bin.jar 复制到 /usr/lib/impala/lib下，

并且修改/usr/bin/catalogd中修改CLASSPATH，添加MySQL驱动包：

export CLASSPATH=”${IMPALA_HOME}/lib/mysql-connector-java-5.1.40-bin.jar:……”
