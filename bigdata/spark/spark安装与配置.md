#### spark-env.sh

将`spark-env.sh.template`重命名为`spark-env.sh`

添加如下内容

````
export JAVA_HOME=
export HADOOP_HOME=
export HADOOP_CONF_DIR
SPARK_MASTER_IP=
SPARK_LOCAL_DIRS=
````



#### slaves

将`slaves.template`重命名为`slaves`

```bash
Slave01
Slave02
```

#### 配置环境变量

vim /etc/profile

添加

````
export SPARK_HOME=
export PATH=$SPARK_HOME/bin:$PATH
````

