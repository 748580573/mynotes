#### 配置从节点vim slaves

````
slave1
slave2
````



#### vim core-site.xml

````xml
<configuration>
        <property>
                <name>fs.defaultFS</name>
                <value>hdfs://master:9000</value>
        </property>
        <property>
                <name>hadoop.tmp.dir</name>
                <!-- 配置临时目录-->
                <value>file:/usr/local/hadoop-2.8.4/tmp</value>
        </property>
</configuration>
````

#### vim hdfs-site.xml

````xml
<configuration>
        <property>
                <name>dfs.namenode.secondary.http-address</name>
                <value>master:9001</value>
        </property>
        <property>
                <!-- 配置name目录-->
                <name>dfs.namenode.name.dir</name>
                <value>file:/usr/local/hadoop-2.8.4/dfs/name</value>
        </property>
        <property>
                 <!-- 配置数据目录-->
                <name>dfs.datanode.data.dir</name>
                <value>file:/usr/local/hadoop-2.8.4/dfs/data</value>
        </property>
        <property>
                <name>dfs.repliction</name>
                <value>3</value>
        </property>
</configuration>
````

#### vim mapred-site.xml

````xml
<configuration>
        <property>
                <name>mapreduce.framework.name</name>
                <value>yarn</value>
        </property>
</configuration>
````

#### vim yarn-site.xml

````
<configuration>
        <property>
                <name>yarn.nodemanager.aux-services</name>
                <value>mapreduce_shuffle</value>
        </property>
        <property>
                <name>yarn.nodemanager.aux-services.mapreduce.shuffle.class</name>
                <value>org.apache.hadoop.mapred.ShuffleHandler</value>
        </property>
        <property>
                <name>yarn.resourcemanager.address</name>
                <value>master:8032</value>
        </property>
        <property>
                <name>yarn.resourcemanager.scheduler.address</name>
                <value>master:8030</value>
        </property>
        <property>
                <name>yarn.resourcemanager.resource-tracker.address</name>
                <value>master:8035</value>
        </property>
        <property>
                <name>yarn.resourcemanager.admin.address</name>
                <value>master:8033</value>
        </property>
        <property>
                <name>yarn.resourcemanager.webapp.address</name>
                <value>master:8088</value>
        </property>
</configuration>
````

