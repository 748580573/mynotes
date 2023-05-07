# SpringCloudFeignClient调用过程

前面已经学习了两个Spring Cloud 组件：

- Eureka：实现服务注册功能；
- Ribbon：提供基于RestTemplate的HTTP客户端并且支持服务负载均衡功能。

通过这两个组件我们暂时可以完成服务注册和可配置负载均衡的服务调用。今天我们要学习的是Feign，那么Feign解决了什么问题呢？

相对于Eureka，Ribbon来说，Feign的地位好像不是那么重要，Feign是一个声明式的REST客户端，它的目的就是让REST调用更加简单。通过提供HTTP请求模板，让Ribbon请求的书写更加简单和便捷。另外，在Feign中整合了Ribbon，从而不需要显式的声明Ribbon的jar包。

前面在使用Ribbon+RestTemplate时，利用RestTemplate对http请求的封装处理，形成了一套模版化的调用方法。但是在实际开发中，由于对服务依赖的调用可能不止一处，往往一个接口会被多处调用，所以通常都会针对每个微服务自行封装一些客户端类来包装这些依赖服务的调用。所以，Feign在此基础上做了进一步封装，由他来帮助我们定义和实现依赖服务接口的定义。在Feign的实现下，我们只需创建一个接口并使用注解的方式来配置它(以前是Dao接口上面标注Mapper注解,现在是一个微服务接口上面标注一个Feign注解即可)，即可完成对服务提供方的接口绑定，简化了使用Spring Cloud Ribbon时，自动封装服务调用客户端的开发量。

#### Feign的使用：[#](https://www.cnblogs.com/rickiyang/p/11802487.html#3992816864)

1.引入依赖：

```xml
Copy<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

另外，我们需要添加*spring-cloud-dependencies*：

```xml
Copy<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.2.0.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.rickiyang.learn</groupId>
    <artifactId>feign-consumer</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>feign-consumer</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-cloud.version>Finchley.RELEASE</spring-cloud.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-eureka</artifactId>
        </dependency>

    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

2.接下来，我们需要在主类中添加 *@EnableFeignClients*：

```java
Copypackage com.rickiyang.learn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class FeignConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeignConsumerApplication.class, args);
    }

}
```

3.再来看一下用Feign写的HTTP请求的格式：

```java
Copypackage com.rickiyang.learn.service;

import com.rickiyang.learn.entity.Person;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * @author: rickiyang
 * @date: 2019/10/5
 * @description:
 */
@FeignClient(name= "eureka-client")
public interface HelloRemote {


    @RequestMapping(value = "/hello/{name}")
    String hello(@PathVariable(value = "name") String name);


    @PostMapping(value ="/add",produces = "application/json; charset=UTF-8")
    String addPerson(@RequestBody Person person);

    @GetMapping("/getPerson/{id}")
    String getPerson(@PathVariable("id") Integer id);

}
```

用FeignClient注解申明要调用的服务是哪个，该服务中的方法都有我们常见的Restful方式的API来声明，这种方式大家是不是感觉像是在写Restful接口一样。

代码示例：[点击这里](https://github.com/rickiyang/SpringCloud-learn)。

*note：*

*示例代码的正确打开方式：先启动服务端，然后启动一个client端，再次启动 feign-consumer，调用 feign-consumer中的接口即可。*

还记得在Ribbon学习的时候使用RestTemplate发起HTTP请求的方式吗：

```java
CopyrestTemplate.getForEntity("http://eureka-client/hello/" + name, String.class).getBody();
```

将整个的请求URL和参数都放在一起，虽然没有什么问题，总归不是那么优雅。使用Feign之后你可以使用Restful方式进行调用，写起来也会更加清晰。

#### Feign调用过程分析[#](https://www.cnblogs.com/rickiyang/p/11802487.html#2100766900)

上面简单介绍了Feign的使用方式，大家可以结合着代码示例运行一下，了解基本的使用方式。接下来我们一起分析Feign的调用过程，我们带着两个问题去跟踪：

1.请求如何被Feign 统一托管；

2.Feign如何分发请求。

这两个问题应该就涵盖了Feign的功能，下面就出发去一探究竟。

我们还和以前一样从一个入口进行深入，首先看启动类上的 @EnableFeignClients 注解：

```java
Copypackage org.springframework.cloud.openfeign;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Scans for interfaces that declare they are feign clients (via {@link FeignClient
 * <code>@FeignClient</code>}). Configures component scanning directives for use with
 * {@link org.springframework.context.annotation.Configuration
 * <code>@Configuration</code>} classes.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(FeignClientsRegistrar.class)
public @interface EnableFeignClients {

    //等价于basePackages属性，更简洁的方式
	String[] value() default {};
    //指定多个包名进行扫描
	String[] basePackages() default {};

    //指定多个类或接口的class,扫描时会在这些指定的类和接口所属的包进行扫描
	Class<?>[] basePackageClasses() default {};

	 //为所有的Feign Client设置默认配置类
	Class<?>[] defaultConfiguration() default {};

	 //指定用@FeignClient注释的类列表。如果该项配置不为空，则不会进行类路径扫描
	Class<?>[] clients() default {};
}
```

注释上说了该注解用于扫描 FeignClient 声明的类。我们用 FeignClient 注解去声明一个 Eureka 客户端，那么猜想这里应该是取到我们声明的Eureka client名称，然后去访问Eureka server获取服务提供者。

同样的，为所有Feign Client 也支持文件属性的配置，如下 :

```yml
Copyfeign:
  client:
    config:                                         
    # 默认为所有的feign client做配置(注意和上例github-client是同级的)
      default:                                      
        connectTimeout: 5000                        # 连接超时时间
        readTimeout: 5000                           # 读超时时间设置  
```

*注 : 如果通过Java代码进行了配置，又通过配置文件进行了配置，则配置文件的中的Feign配置会覆盖Java代码的配置。*

但也可以设置feign.client.defalult-to-properties=false，禁用掉feign配置文件的方式让Java配置生效。

注意到类头声明的 @Import 注解引用的 FeignClientsRegistrar 类，这个类的作用是在 EnableFeignClients 初始化的时候扫描该注解对应的配置。

接着看 FeignClient 注解：

```java
Copypackage org.springframework.cloud.openfeign;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeignClient {

    //指定Feign Client的名称，如果项目使用了 Ribbon，name属性会作为微服务的名称，用于服务发现
	@AliasFor("name")
	String value() default "";
	//用serviceId做服务发现已经被废弃，所以不推荐使用该配置
	@Deprecated
	String serviceId() default "";
	//指定Feign Client的serviceId，如果项目使用了 Ribbon，将使用serviceId用于服务发现,但上面可以看到serviceId做服务发现已经被废弃，所以也不推荐使用该配置
	@AliasFor("value")
	String name() default "";
	//为Feign Client 新增注解@Qualifier
	String qualifier() default "";
    //请求地址的绝对URL，或者解析的主机名
	String url() default "";
    //调用该feign client发生了常见的404错误时，是否调用decoder进行解码异常信息返回,否则抛出FeignException
	boolean decode404() default false;
     //Feign Client设置默认配置类
	Class<?>[] configuration() default {};
    //定义容错的处理类，当调用远程接口失败或超时时，会调用对应接口的容错逻辑,fallback 指定的类必须实现@FeignClient标记的接口。实现的法方法即对应接口的容错处理逻辑
	Class<?> fallback() default void.class;
    //工厂类，用于生成fallback 类示例，通过这个属性我们可以实现每个接口通用的容错逻辑，减少重复的代码
	Class<?> fallbackFactory() default void.class;
    //定义当前FeignClient的所有方法映射加统一前缀
	String path() default "";
    //是否将此Feign代理标记为一个Primary Bean，默认为ture
	boolean primary() default true;
}
```

同样在 FeignClientsRegistrar 类中也会去扫描 FeignClient 注解对应的配置信息。我们直接看 FeignClientsRegistrar 的逻辑：

```java
Copyclass FeignClientsRegistrar implements ImportBeanDefinitionRegistrar,
		ResourceLoaderAware, EnvironmentAware {

	// patterned after Spring Integration IntegrationComponentScanRegistrar
	// and RibbonClientsConfigurationRegistgrar

	private ResourceLoader resourceLoader;

	private Environment environment;

	public FeignClientsRegistrar() {
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

   //在这个重载的方法里面做了两件事情：
   //1.将EnableFeignClients注解对应的配置属性注入
   //2.将FeignClient注解对应的属性注入
	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
        //注入EnableFeignClients注解对应的配置属性
		registerDefaultConfiguration(metadata, registry);
       //注入FeignClient注解对应的属性
		registerFeignClients(metadata, registry);
	}

   /**
   * 拿到 EnableFeignClients注解 defaultConfiguration 字段的值
   * 然后进行注入
   *
   **/
	private void registerDefaultConfiguration(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
		Map<String, Object> defaultAttrs = metadata
				.getAnnotationAttributes(EnableFeignClients.class.getName(), true);

		if (defaultAttrs != null && defaultAttrs.containsKey("defaultConfiguration")) {
			String name;
			if (metadata.hasEnclosingClass()) {
				name = "default." + metadata.getEnclosingClassName();
			}
			else {
				name = "default." + metadata.getClassName();
			}
			registerClientConfiguration(registry, name,
					defaultAttrs.get("defaultConfiguration"));
		}
	}

	public void registerFeignClients(AnnotationMetadata metadata,
								 BeanDefinitionRegistry registry) {
        // 获取ClassPath扫描器
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        // 为扫描器设置资源加载器
        scanner.setResourceLoader(this.resourceLoader);

        Set<String> basePackages;
        // 1. 从@EnableFeignClients注解中获取到配置的各个属性值
        Map<String, Object> attrs = metadata
                .getAnnotationAttributes(EnableFeignClients.class.getName());
        // 2. 注解类型过滤器，只过滤@FeignClient   
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(
                FeignClient.class);
        // 3. 从1. 中的属性值中获取clients属性的值        
        final Class<?>[] clients = attrs == null ? null
                : (Class<?>[]) attrs.get("clients");
        if (clients == null || clients.length == 0) {
            // 扫描器设置过滤器且获取需要扫描的基础包集合
            scanner.addIncludeFilter(annotationTypeFilter);
            basePackages = getBasePackages(metadata);
        }else {
            // clients属性值不为null，则将其clazz路径转为包路径
            final Set<String> clientClasses = new HashSet<>();
            basePackages = new HashSet<>();
            for (Class<?> clazz : clients) {
                basePackages.add(ClassUtils.getPackageName(clazz));
                clientClasses.add(clazz.getCanonicalName());
            }
            AbstractClassTestingTypeFilter filter = new AbstractClassTestingTypeFilter() {
                @Override
                protected boolean match(ClassMetadata metadata) {
                    String cleaned = metadata.getClassName().replaceAll("\\$", ".");
                    return clientClasses.contains(cleaned);
                }
            };
            scanner.addIncludeFilter(
                    new AllTypeFilter(Arrays.asList(filter, annotationTypeFilter)));
        }

        // 3. 扫描基础包，且满足过滤条件下的接口封装成BeanDefinition
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner
                    .findCandidateComponents(basePackage);
            // 遍历扫描到的bean定义        
            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof AnnotatedBeanDefinition) {
                    // 并校验扫描到的bean定义类是一个接口
                    AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                    Assert.isTrue(annotationMetadata.isInterface(),
                            "@FeignClient can only be specified on an interface");

                    // 获取@FeignClient注解上的各个属性值
                    Map<String, Object> attributes = annotationMetadata
                            .getAnnotationAttributes(
                                    FeignClient.class.getCanonicalName());

                    String name = getClientName(attributes);
                    // 可以看到这里也注册了一个FeignClient的配置bean
                    registerClientConfiguration(registry, name,
                            attributes.get("configuration"));
                    // 注册bean定义到spring中
                    registerFeignClient(registry, annotationMetadata, attributes);
                }
            }
        }
	}

   /**
   * 注册bean
   **/
	private void registerFeignClient(BeanDefinitionRegistry registry,
       AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {
        // 1.获取类名称，也就是本例中的FeignService接口
        String className = annotationMetadata.getClassName();

        // 2.BeanDefinitionBuilder的主要作用就是构建一个AbstractBeanDefinition
        // AbstractBeanDefinition类最终被构建成一个BeanDefinitionHolder
        // 然后注册到Spring中
        // 注意：beanDefinition类为FeignClientFactoryBean，故在Spring获取类的时候实际返回的是
        // FeignClientFactoryBean类
        BeanDefinitionBuilder definition = BeanDefinitionBuilder
            .genericBeanDefinition(FeignClientFactoryBean.class);
        validate(attributes);

        // 3.添加FeignClientFactoryBean的属性，
        // 这些属性也都是我们在@FeignClient中定义的属性
        definition.addPropertyValue("url", getUrl(attributes));
        definition.addPropertyValue("path", getPath(attributes));
        String name = getName(attributes);
        definition.addPropertyValue("name", name);
        definition.addPropertyValue("type", className);
        definition.addPropertyValue("decode404", attributes.get("decode404"));
        definition.addPropertyValue("fallback", attributes.get("fallback"));
        definition.addPropertyValue("fallbackFactory", attributes.get("fallbackFactory"));
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        // 4.设置别名 name就是我们在@FeignClient中定义的name属性
        String alias = name + "FeignClient";
        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();

        boolean primary = (Boolean)attributes.get("primary"); // has a default, won't be null

        beanDefinition.setPrimary(primary);

        String qualifier = getQualifier(attributes);
        if (StringUtils.hasText(qualifier)) {
            alias = qualifier;
        }

        // 5.定义BeanDefinitionHolder，
        // 在本例中 名称为FeignService，类为FeignClientFactoryBean
        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, new String[] { alias });
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}

	private void validate(Map<String, Object> attributes) {
		AnnotationAttributes annotation = AnnotationAttributes.fromMap(attributes);
		// This blows up if an aliased property is overspecified
		// FIXME annotation.getAliasedString("name", FeignClient.class, null);
		Assert.isTrue(
			!annotation.getClass("fallback").isInterface(),
			"Fallback class must implement the interface annotated by @FeignClient"
		);
		Assert.isTrue(
			!annotation.getClass("fallbackFactory").isInterface(),
			"Fallback factory must produce instances of fallback classes that implement the interface annotated by @FeignClient"
		);
	}

   ......
   ......
   ......
}
```

在这里做了两件事情：

1. 将EnableFeignClients注解对应的配置属性注入；
2. 将FeignClient注解对应的属性注入。

生成FeignClient对应的bean，注入到Spring 的IOC容器。

在registerFeignClient方法中构造了一个BeanDefinitionBuilder对象，BeanDefinitionBuilder的主要作用就是构建一个AbstractBeanDefinition，AbstractBeanDefinition类最终被构建成一个BeanDefinitionHolder 然后注册到Spring中。

beanDefinition类为FeignClientFactoryBean，故在Spring获取类的时候实际返回的是FeignClientFactoryBean类。

`FeignClientFactoryBean`作为一个实现了`FactoryBean`的工厂类，那么每次在Spring Context 创建实体类的时候会调用它的`getObject()`方法。

```java
Copypublic Object getObject() throws Exception {
    FeignContext context = applicationContext.getBean(FeignContext.class);
    Feign.Builder builder = feign(context);

    if (!StringUtils.hasText(this.url)) {
        String url;
        if (!this.name.startsWith("http")) {
            url = "http://" + this.name;
        }
        else {
            url = this.name;
        }
        url += cleanPath();
        return loadBalance(builder, context, new HardCodedTarget<>(this.type, this.name, url));
    }
    if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
        this.url = "http://" + this.url;
    }
    String url = this.url + cleanPath();
    Client client = getOptional(context, Client.class);
    if (client != null) {
        if (client instanceof LoadBalancerFeignClient) {
            // not lod balancing because we have a url,
            // but ribbon is on the classpath, so unwrap
            client = ((LoadBalancerFeignClient)client).getDelegate();
        }
        builder.client(client);
    }
    Targeter targeter = get(context, Targeter.class);
    return targeter.target(this, builder, context, new HardCodedTarget<>(
        this.type, this.name, url));
}
```

这里的`getObject()`其实就是将`@FeinClient`中设置value值进行组装起来，此时或许会有疑问，因为在配置`FeignClientFactoryBean`类时特意说过并没有将Configuration传过来，那么Configuration中的属性是如何配置的呢？看其第一句是：

```java
CopyFeignContext context = applicationContext.getBean(FeignContext.class);
```

从Spring容器中获取`FeignContext.class`的类，我们可以看下这个类是从哪加载的。点击该类查看被引用的地方，可以找到在`FeignAutoConfiguration`类中有声明bean：

```java
Copy@Configuration
@ConditionalOnClass(Feign.class)
@EnableConfigurationProperties({FeignClientProperties.class, FeignHttpClientProperties.class})
public class FeignAutoConfiguration {

	@Autowired(required = false)
	private List<FeignClientSpecification> configurations = new ArrayList<>();

	@Bean
	public HasFeatures feignFeature() {
		return HasFeatures.namedFeature("Feign", Feign.class);
	}

	@Bean
	public FeignContext feignContext() {
		FeignContext context = new FeignContext();
		context.setConfigurations(this.configurations);
		return context;
	}
    ......
    ......
    ......
        
}     
```

从上面的代码中可以看到在set属性的时候将 FeignClientSpecification 类型的类全部加入此类的属性中。还记得在上面分析`registerFeignClients`方法的时候里面有一行代码调用：`registerClientConfiguration()`方法：

```java
Copyprivate void registerClientConfiguration(BeanDefinitionRegistry registry, Object name,
                                         Object configuration) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder
        .genericBeanDefinition(FeignClientSpecification.class);
    builder.addConstructorArgValue(name);
    builder.addConstructorArgValue(configuration);
    registry.registerBeanDefinition(
        name + "." + FeignClientSpecification.class.getSimpleName(),
        builder.getBeanDefinition());
}
```

在注册BeanDefinition的时候， configuration 其实也被作为参数，传给了 FeignClientSpecification。 所以这时候在FeignContext中是带着configuration配置信息的。

至此我们已经完成了配置属性的装配工作，那么是如何执行的呢？我们可以看`getObject()`最后一句可以看到返回了`Targeter.target`的方法。

```java
Copyreturn targeter.target(this, builder, context, new HardCodedTarget<>(this.type, this.name, url));
```

那么这个`Targeter`是哪来的？我们还是看上面的`FeignAutoConfiguration`类，可以看到其中有两个`Targeter`类，一个是`DefaultTargeter`，一个是`HystrixTargeter`。当配置了`feign.hystrix.enabled = true`的时候，Spring容器中就会配置`HystrixTargeter`此类，如果为false那么Spring容器中配置的就是`DefaultTargeter`。

我们以`DefaultTargeter`为例介绍一下接下来是如何通过创建代理对象的：

```java
Copyclass DefaultTargeter implements Targeter {

	@Override
	public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign, FeignContext context,
						Target.HardCodedTarget<T> target) {
		return feign.target(target);
	}
}

public static class Builder {

    public <T> T target(Target<T> target) {
      return build().newInstance(target);
    }

    public Feign build() {
      SynchronousMethodHandler.Factory synchronousMethodHandlerFactory =
          new SynchronousMethodHandler.Factory(client, retryer, requestInterceptors, logger,
                                               logLevel, decode404);
      ParseHandlersByName handlersByName =
          new ParseHandlersByName(contract, options, encoder, decoder,
                                  errorDecoder, synchronousMethodHandlerFactory);
      return new ReflectiveFeign(handlersByName, invocationHandlerFactory);
    }
 }
```

在target方法中有个参数：Feign.Builder：

```java
Copypublic static class Builder {

    private final List<RequestInterceptor> requestInterceptors =
        new ArrayList<RequestInterceptor>();
    private Logger.Level logLevel = Logger.Level.NONE;
    private Contract contract = new Contract.Default();
    private Client client = new Client.Default(null, null);
    private Retryer retryer = new Retryer.Default();
    private Logger logger = new NoOpLogger();
    private Encoder encoder = new Encoder.Default();
    private Decoder decoder = new Decoder.Default();
    private ErrorDecoder errorDecoder = new ErrorDecoder.Default();
    private Options options = new Options();
    private InvocationHandlerFactory invocationHandlerFactory =
        new InvocationHandlerFactory.Default();
    private boolean decode404;
    
    ......
    ......
    ......
}
```

构建feign.builder时会向FeignContext获取配置的Encoder，Decoder等各种信息 。Builder中的参数来自于配置文件的 feign.client.config里面的属性。

查看`ReflectiveFeign`类中`newInstance`方法是返回一个代理对象：

```java
Copypublic <T> T newInstance(Target<T> target) {
    //为每个方法创建一个SynchronousMethodHandler对象，并放在 Map 里面
    Map<String, MethodHandler> nameToHandler = targetToHandlersByName.apply(target);
    Map<Method, MethodHandler> methodToHandler = new LinkedHashMap<Method, MethodHandler>();
    List<DefaultMethodHandler> defaultMethodHandlers = new LinkedList<DefaultMethodHandler>();

    for (Method method : target.type().getMethods()) {
        if (method.getDeclaringClass() == Object.class) {
            continue;
        } else if(Util.isDefault(method)) {
            //如果是 default 方法，说明已经有实现了，用 DefaultHandler
            DefaultMethodHandler handler = new DefaultMethodHandler(method);
            defaultMethodHandlers.add(handler);
            methodToHandler.put(method, handler);
        } else {
            //否则就用上面的 SynchronousMethodHandler
            methodToHandler.put(method, nameToHandler.get(Feign.configKey(target.type(), method)));
        }
    }
    // 设置拦截器
    // 创建动态代理，factory 是 InvocationHandlerFactory.Default，创建出来的是 
    // ReflectiveFeign.FeignInvocationHanlder，也就是说后续对方法的调用都会进入到该对象的 inovke 方法
    InvocationHandler handler = factory.create(target, methodToHandler);
    // 创建动态代理对象
    T proxy = (T) Proxy.newProxyInstance(target.type().getClassLoader(), new Class<?>[]{target.type()}, handler);

    for(DefaultMethodHandler defaultMethodHandler : defaultMethodHandlers) {
        defaultMethodHandler.bindTo(proxy);
    }
    return proxy;
}
```

这个方法大概的逻辑是：

1. 根据target，解析生成`MethodHandler`对象；
2. 对`MethodHandler`对象进行分类整理，整理成两类：default 方法和 SynchronousMethodHandler 方法；
3. 通过jdk动态代理生成代理对象，这里是最关键的地方；
4. 将`DefaultMethodHandler`绑定到代理对象。

最终都是执行了`SynchronousMethodHandler`拦截器中的`invoke`方法：

```java
Copy@Override
  public Object invoke(Object[] argv) throws Throwable {
    RequestTemplate template = buildTemplateFromArgs.create(argv);
    Retryer retryer = this.retryer.clone();
    while (true) {
      try {
        return executeAndDecode(template);
      } catch (RetryableException e) {
        retryer.continueOrPropagate(e);
        if (logLevel != Logger.Level.NONE) {
          logger.logRetry(metadata.configKey(), logLevel);
        }
        continue;
      }
    }
  }
```

`invoke`方法方法首先生成 RequestTemplate 对象，应用 encoder，decoder 以及 retry 等配置，下面有一个死循环调用：executeAndDecode，从名字上看就是执行调用逻辑并对返回结果解析。

```java
CopyObject executeAndDecode(RequestTemplate template) throws Throwable {
    //根据  RequestTemplate生成Request对象
    Request request = targetRequest(template);

    if (logLevel != Logger.Level.NONE) {
        logger.logRequest(metadata.configKey(), logLevel, request);
    }

    Response response;
    long start = System.nanoTime();
    try {
        // 调用client对象的execute()方法执行http调用逻辑,
        //execute()内部可能设置request对象，也可能不设置，所以需要response.toBuilder().request(request).build();这一行代码
        response = client.execute(request, options);
        // ensure the request is set. TODO: remove in Feign 10
        response.toBuilder().request(request).build();
    } catch (IOException e) {
        if (logLevel != Logger.Level.NONE) {
            logger.logIOException(metadata.configKey(), logLevel, e, elapsedTime(start));
        }
        // IOException的时候，包装成 RetryableException异常,上面的while循环 catch里捕捉的就是这个异常
        throw errorExecuting(request, e);
    }
    //统计 执行调用花费的时间
    long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    boolean shouldClose = true;
    try {
        if (logLevel != Logger.Level.NONE) {
            response =
                logger.logAndRebufferResponse(metadata.configKey(), logLevel, response, elapsedTime);
            // ensure the request is set. TODO: remove in Feign 10
            response.toBuilder().request(request).build();
        }
        //如果元数据返回类型是 Response，直接返回回去即可，不需要decode()解码
        if (Response.class == metadata.returnType()) {
            if (response.body() == null) {
                return response;
            }
            if (response.body().length() == null ||
                response.body().length() > MAX_RESPONSE_BUFFER_SIZE) {
                shouldClose = false;
                return response;
            }
            // Ensure the response body is disconnected
            byte[] bodyData = Util.toByteArray(response.body().asInputStream());
            return response.toBuilder().body(bodyData).build();
        }
        //主要对2xx和404等进行解码，404需要特别的开关控制。其他情况，使用errorDecoder进行解码，以异常的方式返回
        if (response.status() >= 200 && response.status() < 300) {
            if (void.class == metadata.returnType()) {
                return null;
            } else {
                return decode(response);
            }
        } else if (decode404 && response.status() == 404 && void.class != metadata.returnType()) {
            return decode(response);
        } else {
            throw errorDecoder.decode(metadata.configKey(), response);
        }
    } catch (IOException e) {
        if (logLevel != Logger.Level.NONE) {
            logger.logIOException(metadata.configKey(), logLevel, e, elapsedTime);
        }
        throw errorReading(request, response, e);
    } finally {
        if (shouldClose) {
            ensureClosed(response.body());
        }
    }
}
```

这里主要就是使用：client.execute(request, options) 来发起调用，下面基本都是处理返回结果的逻辑。到此我们的整个调用生态已经解析完毕。

我们可以整理一下上面的分析：

**首先调用接口为什么会直接发送请求？**

原因就是Spring扫描了`@FeignClient`注解，并且根据配置的信息生成代理类，调用的接口实际上调用的是生成的代理类。

**其次请求是如何被Feign接管的？**

1. Feign通过扫描`@EnableFeignClients`注解中配置包路径，扫描`@FeignClient`注解并将注解配置的信息注入到Spring容器中，类型为`FeignClientFactoryBean`；
2. 然后通过`FeignClientFactoryBean`的`getObject()`方法得到不同动态代理的类并为每个方法创建一个`SynchronousMethodHandler`对象；
3. 为每一个方法创建一个动态代理对象， 动态代理的实现是 `ReflectiveFeign.FeignInvocationHanlder`，代理被调用的时候，会根据当前调用的方法，转到对应的 `SynchronousMethodHandler`。

这样我们发出的请求就能够被已经配置好各种参数的Feign handler进行处理，从而被Feign托管。

**请求如何被Feign分发的？**

上一个问题已经回答了Feign将每个方法都封装成为代理对象，那么当该方法被调用时，真正执行逻辑的是封装好的代理对象进行处理，执行对应的服务调用逻辑。



> 本文摘自；https://www.cnblogs.com/rickiyang/p/11802487.html