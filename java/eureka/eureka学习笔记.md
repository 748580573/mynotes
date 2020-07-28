### eureka配置说明

```yml
eureka:
  client:
    register-with-eureka: true    #是否注册到注册中心
    fetch-registry: true          #是否去注册中心拉去检索服务
    service-url:                  #注册中心的ip
      defaultZone: http://eureka7001:7001/eureka,http://eureka7002:7002/eureka
  instance:
    instance-id: payment8002  #在页面你显示的名字
    prefer-ip-address: true   #在页面上显示ip
    lease-renewal-interval-in-seconds: 1      #每一秒发送一次心跳
    lease-expiration-duration-in-seconds: 2   #表示eureka server至上一次收到client的心跳之后，等待下一次心跳的超时时间，在这个时间内若没收到下一次心跳，则将移除该instance
   server:  #注册中的配置
    enable-self-preservation: false   #关闭自我保护机制
    eviction-interval-timer-in-ms: 2000  #eureka server清理无效节点的时间间隔，默认60000毫秒，即60秒
```

###  eureka安装

#### 父pom加入

```xml
<dependencyManagement>
    <dependencies>
        <!--spring boot 2.2.2-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.2.2.RELEASE</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!--spring cloud Hoxton.SR1-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>Hoxton.SR1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
</dependencyManagement>
```



#### eureka server

子pom加入

```xml
<dependencies>
    <!--eureka-server-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
    <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
</dependencies>
```

```yml
server:
  port: 7001
spring:
  application:
    name: eureka7001
eureka:
  instance:
    hostname: eureka7001
  client:
    #表示不向自己注册自己
    register-with-eureka: false
    #false表示自己就是注册中心，不需要去检索服务
    fetch-registry: false
    service-url:
    #http://eureka7002:7002/eureka,http://eureka7003:7003/eureka
      defaultZone: http://eureka7002:7002/eureka/
      
```



#### eureka clinet



```xml
<dependencies>
    <!--eureka-server-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
</dependencies>
```

```yml
eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      #如果有三台注册中心，则改配置应该为defaultZone:      	#http://eureka7003:7003/eureka,http://eureka7001:7001/eureka
      defaultZone: http://eureka7001:7001/eureka
```

#### 启动负载均衡

```java
# 在bean上添加@LoadBalanced注释，赋予该bean访问eureka注册中心的能力，这样就能使用负载均衡了
@Bean
@LoadBalanced
public RestTemplate getRestTemplate(){
    return new RestTemplate();
}
```

#### 服务发现