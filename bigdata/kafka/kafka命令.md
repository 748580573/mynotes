#### 启动kafka

````shell
./kafka-server-start.sh  config/server.properties

./kafka-server-start.sh   -deamon  config/server.properties
````

#### 关闭Kafka

````shell
./kafka-server-stop.sh  config/server.properties
````

#### 创建kafka topic命令

````shell
./kafka-topics.sh   --zookeeper  {zkhost}:{zkport}  --create  --topic  {topicName}  --partitions  2   --replication-factor  2 
````

#### 查看kafka topic列表命令

````shell
./kafka-topics.sh   --zookeeper  {zkhost}:{zkport}  --list  
````

#### 查看kafka 某个topic详情

````shell
./kafka-topics.sh  --zookeeper  {zkhost}:{zkport}   --describe  --topic first
````

#### 删除kafka某个topic

````shell
./kafka-topics.sh  --zookeeper  {zkhost}:{zkport}  --delete  --topic {topickName}
````

#### 修改分区数

````shell
./kafka-topics.sh  --zookeeper   {zkhost}:{zkport}   --alter  --topic  {topicName}   --partitions 6
````

#### 运行kafka生产者案例

````shell
bin/kafka-console-producer.sh --topic wuheng --broker-list master:9092
````

#### 运行Kafka消费者案例

````shell
# 0.9以前版本的命令
bin/kafka-console-consumer.sh  --topic  {topicName} --zookeeper   {zkhost}:{zkport}  [--from-beginning]
# 0.9以后版本的命令
bin/kafka-console-consumer.sh  --topic  {topicName} --bootstrap-server   {kafkahost}:{kafkaport} [--from-beginning]
````

