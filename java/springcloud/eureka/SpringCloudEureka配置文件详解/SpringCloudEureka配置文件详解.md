# SpringCloudEureka配置文件详解

本篇内容用来说明Eureka 常用配置的含义。

以下配置都是以 `eureka.server` 开头：

| 参数                                        | 描述                         | 备注       |
| ------------------------------------------- | ---------------------------- | ---------- |
| eureka.server.eviction-interval-timer-in-ms | server清理无效节点的时间间隔 | 默认60秒   |
| eureka.server.enable-self-preservation      | 是否开启自我保护，默认true   | true false |
| eureka.server.renewal-percent-threshold     | 开启自我保护的系数           | 默认：0.85 |

client参数配置：

| 参数                                                   | 描述                           | 备注          |
| ------------------------------------------------------ | ------------------------------ | ------------- |
| eureka.client.enabled                                  | 是否开启client，默认true       | true false    |
| eureka.client.register-with-eureka                     | 是否注册                       | 默认true      |
| eureka.client.fetch-registry                           | 是否检索服务                   | true false    |
| eureka.client.serviceUrl.defaultZone                   | 默认服务注册中心地址           | 多个用","隔开 |
| eureka.client.eureka-server-connect-timeout-seconds    | 连接server服务器超时时间       | 默认5秒       |
| eureka.client.eureka-connection-idle-timeout-seconds   | 连接server的连接空闲时长       | 默认30秒      |
| eureka.client.eureka-server-read-timeout-seconds       | 连接server读取数据超时时间     | 默认8秒       |
| eureka.client.eureka-server-total-connections          | 连接server的最大连接数         | 默认200       |
| eureka.client.eureka-server-total-connections-per-host | 对单个server的最大连接数       | 默认50        |
| eureka.client.eureka-service-url-poll-interval-seconds | 获取集群中最新的server节点数据 | 默认0         |
| eureka.client.heartbeat-executor-thread-pool-size      | client维持与server的心跳线程数 | 默认2         |
| eureka.client.service-url                              | 列出所有可用注册中心的地址     |               |

eureka instance 相关配置：

| 参数                                                 | 描述                                 | 备注                                                         |
| ---------------------------------------------------- | ------------------------------------ | ------------------------------------------------------------ |
| eureka.instance.lease-renewal-interval-in-seconds    | 服务续约任务调用间隔时间，默认30秒   | client每隔30秒向server上报自己状态，避免被server剔除         |
| eureka.instance.lease-expiration-duration-in-seconds | 服务时效时间，默认90秒               | 当server 90秒内没有收到client的注册信息时，会将该节点剔除    |
| eureka.client.registry-fetch-interval-seconds        | client本地缓存清单更新间隔，默认30秒 | client每隔30秒，向server请求可用服务清单。对于API网关类应用，可以适当降低时间间隔 |
| eureka.instance.prefer-ip-address                    | 注册服务时是否使用IP注册，默认false  | true false                                                   |
| eureka.instance.ip-address                           | server端的ip地址                     |                                                              |
| eureka.instance.hostname                             | server端的hostname                   | 默认localhost                                                |
| eureka.instance.instance-id                          | 注册到server的实例                   |                                                              |