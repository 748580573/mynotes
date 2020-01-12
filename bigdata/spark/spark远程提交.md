1.使用spark脚本提交到yarn，首先需要将spark所在的主机和hadoop集群之间hosts相互配置（也就是把spark主机的ip和主机名配置到hadoop所有节点的/etc/hosts里面，再把集群所有节点的ip和主机名配置到spark所在主机的/etc/hosts里面）。  
2.然后需要把hadoop目录etc/hadoop下面的*-site.xml复制到${SPARK_HOME}的conf下面.  
3.确保hadoop集群配置了 HADOOP_CONF_DIR or YARN_CONF_DIR