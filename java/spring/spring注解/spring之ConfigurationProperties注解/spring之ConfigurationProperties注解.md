# Spring之@ConfigurationProperties注解

## 前言

最近在思考使用java config的方式进行配置，java config是指基于java配置的spring。传统的Spring一般都是基本xml配置的，后来spring3.0新增了许多java config的注解，特别是spring boot，基本都是清一色的java config。

### Spring配置方式

第一阶段：xml配置
    在spring 1.x时代，使用spring开发满眼都是xml配置的bean，随着项目的扩大，
我们需要把xml配置文件分放到不同的配置文件中，那时候需要频繁地在开发的类和配置文件间切换。

### 第二阶段：注解配置

在spring 2.x时代，随着JDK1.5带来的注解支持，spring提供了声明bean的注解，
大大减少了配置量。这时spring圈子存在一种争论：注解配置和xml配置究竟哪个更好？我们最终的选择是应用
的基本配置用xml，业务配置用户注解。

### 第三阶段：Java配置(java config)

从spring 3.x到现在，spring提供了Java配置的能力，使用Java配置更好的理解
配置的bean。spring 4.x和spring boot都推荐使用Java配置。

Spring IOC有一个非常核心的概念——Bean。由Spring容器来负责对Bean的实例化，装配和管理。XML是用来描述Bean最为流行的配置方式。但随着Spring的日益发展，越来越多的人对Spring提出了批评。“Spring项目大量的烂用XML”就是最为严励的一个批评。由于Spring会把几乎所有的业务类都以Bean的形式配置在XML文件中，造成了大量的XML文件。使用XML来配置Bean失去了编译时的类型安全检查。大量的XML配置使得整个项目变得更加复杂。

随着JAVA EE 5.0的发布，其中引入了一个非常重要的特性——Annotations(注释)。注释是源代码的标签，这些标签可以在源代码层进行处理或通过编译器把它熔入到class文件中。在JAVA EE5以后的版本中，注释成为了一个主要的配置选项。Spring使用注释来描述Bean的配置与采用XML相比，因类注释是在一个类源代码中，可以获得类型安全检查的好处。可以良好的支持重构。

JavaConfig就是使用注释来描述Bean配置的组件。JavaConfig 是Spring的一个子项目, 比起Spring，它还是一个非常年青的项目。目前的版本是1.0 M2。使用XML来配置Bean所能实现的功能，通过JavaConfig同样可以很好的实现。

**下面具体讲一讲@ConfigurationProperties使用方法**

## @ConfigurationProperties

Spring源码中大量使用了ConfigurationProperties注解，比如`server.port`就是由该注解获取到的，通过与其他注解配合使用，能够实现Bean的按需配置。

该注解有一个prefix属性，通过指定的前缀，绑定配置文件中的配置，该注解可以放在类上，也可以放在方法上

![img](https://qboshi.oss-cn-hangzhou.aliyuncs.com/pic/c903b621-ff2b-41b4-b9c8-af7ce6718ab6.png)

可以从注解说明中看到，当将该注解作用于方法上时，如果想要有效的绑定配置，那么该方法需要有@Bean注解且所属Class需要有@Configuration注解。

**简单一句话概括就是：Sring的有效运行是通过上下文（Bean容器）中Bean的配合完成的，Bean可以简单理解成对象，有些对象需要指定字段内容，那么这些内容我们可以通过配置文件进行绑定，然后将此Bean归还给容器**

## 作用于方法

比较常见的就是配置读写分离的场景。

### 配置文件内容

```
#数据源
spring.datasource.druid.write.url=jdbc:mysql://localhost:3306/jpa
spring.datasource.druid.write.username=root
spring.datasource.druid.write.password=1
spring.datasource.druid.write.driver-class-name=com.mysql.jdbc.Driver

spring.datasource.druid.read.url=jdbc:mysql://localhost:3306/jpa
spring.datasource.druid.read.username=root
spring.datasource.druid.read.password=1
spring.datasource.druid.read.driver-class-name=com.mysql.jdbc.Driver
```

### java代码

```
public class DruidDataSourceConfig {
    /**
     * DataSource 配置
     * @return
     */
    @ConfigurationProperties(prefix = "spring.datasource.druid.read")
    @Bean(name = "readDruidDataSource")
    public DataSource readDruidDataSource() {
        return new DruidDataSource();
    }


    /**
     * DataSource 配置
     * @return
     */
    @ConfigurationProperties(prefix = "spring.datasource.druid.write")
    @Bean(name = "writeDruidDataSource")
    @Primary
    public DataSource writeDruidDataSource() {
        return new DruidDataSource();
    }
}
```

也许有的人看到这里会比较疑惑，prefix并没有指定配置的全限定名，那它是怎么进行配置绑定的呢？

相信大家肯定了解@Value注解，它可以通过全限定名进行配置的绑定，这里的ConfigurationProperties其实就类似于使用多个@Value同时绑定，绑定的对象就是DataSource类型的对象，而且是 **隐式绑定** 的，意味着在配置文件编写的时候需要与对应类的字段名称 **相同**，比如上述`spring.datasource.druid.write.url=jdbc:mysql://localhost:3306/jpa` ，当然了，你也可以随便写个配置，比如 `spring.datasource.druid.write.uuu=www.baidu.com`，此时你只需要在注解中加上以下参数即可

![img](https://qboshi.oss-cn-hangzhou.aliyuncs.com/pic/c5a81a7e-8d26-42be-af66-5af0c713fa5e.png)

以上就完成了多个数据源的配置，为读写分离做了铺垫

## 作用于Class类及其用法

### 配置文件内容

```
spring.datasource.url=jdbc:mysql://127.0.0.1:8888/test?useUnicode=false&autoReconnect=true&characterEncoding=utf-8
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.type=com.alibaba.druid.pool.DruidDataSource
```

### java代码

```
ConfigurationProperties(prefix = "spring.datasource")
@Component
public class DatasourcePro {

    private String url;

    private String username;

    private String password;

    // 配置文件中是driver-class-name, 转驼峰命名便可以绑定成
    private String driverClassName;

    private String type;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
```

### 用法

```java
Copy@Controller
@RequestMapping(value = "/config")
public class ConfigurationPropertiesController {

    @Autowired
    private DatasourcePro datasourcePro;

    @RequestMapping("/test")
    @ResponseBody
    public Map<String, Object> test(){

        Map<String, Object> map = new HashMap<>();
        map.put("url", datasourcePro.getUrl());
        map.put("userName", datasourcePro.getUsername());
        map.put("password", datasourcePro.getPassword());
        map.put("className", datasourcePro.getDriverClassName());
        map.put("type", datasourcePro.getType());

        return map;
    }
}
```

## 自定义配置Bean

通过上文我们知道了，与spring有关的starter项目都采用了Java config的方式，因此我们也可以仿照它们来定义自己的java config

### 依赖引入

````xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>${spring.boot.version}</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <version>${spring.boot.version}</version>
            <optional>true</optional>
        </dependency>
    </dependencies>
````

* 引入spring-boot-starter-web是为了偷懒，因为该包就引用了我们所需要的的spring-boot的包了
* 引入spring-boot-configuration-processor可以使得IDE在我们在配置文件的时候给我们提示，后文会讲到。

### 创建项目

创建项目confg-test

目录结构：

````java
├─src
│  ├─main
│  │  ├─java
│  │  │  └─com
│  │  │      └─heng
│  │  │          └─config
|  |  |             └─MyConfig.java
|  |  |             └─TestConfig.java
│  │  └─resources
│  │      └─META-INF
│  │      	└─spring.factories    
│  └─test
│      └─java
````

编写MyConfig类

```java
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "my.config")
public class MyConfig {

    private String hostname;

    private String port;

    private String protocol;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
```

编写TestConfig类

```
@Configuration
@EnableConfigurationProperties(MyConfig.class)
public class TestConfig {

}
```

编写spring.factories 文件

````java
org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.heng.config.TestConfig
````

然后将confg-test打包

### 使用自定义的配置bean

在spring-boot项目中引入confg-test依赖

````xml

        <dependency>
            <groupId>com.heng</groupId>
            <artifactId>config-test</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
````

然后编写applicatoin.properties

![](./imgs/1.png)

> 注：只有引入了spring-boot-configuration-processor依赖后，在IDE编写application.properties的时候才会有自动提示

## 总结

1. @ConfigurationProperties 和 @value 有着相同的功能,但是 @ConfigurationProperties的写法更为方便
2. @ConfigurationProperties 的 POJO类的命名比较严格,因为它必须和prefix的后缀名要一致, 不然值会绑定不上, 特殊的后缀名是“driver-class-name”这种带横杠的情况,在POJO里面的命名规则是 **下划线转驼峰** 就可以绑定成功，所以就是 “driverClassName”

> 本文参考：https://www.cnblogs.com/tian874540961/p/12146467.html