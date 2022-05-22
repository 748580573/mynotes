# Spring动态装载卸载Bean

有时候我们需要在项目中动态加载或者卸载Bean，这就需要Bean的class文件事先是存在的，只是在需要的时候才加载进来。

比如我定义一个接口OneService和它的一个实现类,下面是比较常规的实现，调用OneService的时候，调的就是OneServiceImplA

````java
public interface OneService {
    String say();
}



//--------------------------------------------------
@Service
public class OneServiceImplA implements OneService {
    @Override
    public String say() {
        System.out.println("I'm OneServiceImplA");
        return "OneServiceImplA";
    }
}
````

但有些情况下，OneServiceImplA不能满足我的需求，我想用OneServiceImplB，可以发现这个类上没有注解，springboot启动的时候不会加载它

````java
public class OneServiceImplB implements OneService, InitializingBean {
    @Override
    public String say() {
        System.out.println("I'm OneServiceImplB");
        return "OneServiceImplB";
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("我是动态注册的你,不是容器启动的时候注册的你");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("我被动态删除了！");
    }
}
````

当我想在项目中调用OneServiceImplB的时候怎么办？我们需要实现一个工具类

````java
public class SpringContextUtil {
    private static ApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        SpringContextUtil.applicationContext = applicationContext;
    }

    //通过名字获取上下文中的bean
    public static Object getBean(String name) {
        try {
            return applicationContext.getBean(name);
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }

    //通过类型获取上下文中的bean
    public static Object getBean(Class<?> requiredType) {
        return applicationContext.getBean(requiredType);
    }
}

````

在项目启动的时候，把ApplicationContext对象注入进去。

启动类：

````java
@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(ServerApplication.class, args);
        SpringContextUtil.setApplicationContext(applicationContext);
    }
}
````

这样在有需要的时候，我们就可以动态的加载和删除这个bean了。

````java
/**
     * 动态注册Bean
     *
     * @param beanName
     * @return
     */
    @GetMapping("dynamicInit")
    public String initBean(String beanName) {
        //获取ApplicationContext
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) SpringContextUtil.getApplicationContext();
        //通过ApplicationContext获取DefaultListableBeanFactory
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        //获取BeanDefinitionBuilder
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(OneServiceImplB.class);
        //注册bean
        beanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getRawBeanDefinition());
        OneServiceImplB oneServiceImplB = (OneServiceImplB) SpringContextUtil.getBean(beanName);
        return oneServiceImplB.say();
    }

    /**
     * 根据beanName删除bean
     * @param beanName
     * @return
     */
    @GetMapping("dynamicRemove")
    public String removeBean(String beanName) {
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) SpringContextUtil.getApplicationContext();
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        //动态删除bean
        beanFactory.removeBeanDefinition(beanName);
        return "remove ok";
    }
````

