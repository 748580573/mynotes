# spring之BeanDefinitionRegistryPostProcessor

## 一、简介

BeanDefinitionRegistryPostProcessor继承自BeanDefinitionPostProcessor，BeanDefinitionPostProcessor给予用户去修改已定义的BeanDefinition的扩展点，让用户可在bean被实例化之前，去修改bean的一些属性。BeanDefinitionRegistryPostProcessor则在BeanDefinitionPostProcessor之上添加了，在bean被实例化之前添加BeanDefinition的能力。

在我们常用的ORM中，mybatis `MapperScannerConfigurer`实现了`BeanDefinitionRegistryPostProcessor`接口动态的注册mapper。

## 二、使用

> 案例摘自：https://blog.csdn.net/baidu_19473529/article/details/105685333

- 创建一个实体类对象RegistryBeanSample.java

````java
public class RegistryBeanSample {
	
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
````

- 实现`BeanDefinitionRegistryPostProcessor`接口的类BeanDefinitionRegistryPostProcessorSample.java

````java
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;

@Component
public class BeanDefinitionRegistryPostProcessorSample implements BeanDefinitionRegistryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		// GenericBeanDefinition是BeanDefinition其中一个实现
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		// 设置初始化对象的类
		beanDefinition.setBeanClass(RegistryBeanSample.class);
		// 注册进spring ioc容器
		registry.registerBeanDefinition("registryBeanSample", beanDefinition);
	}	
}
````

* 因为BeanDefinitionRegistryPostProcessor是继承BeanFactoryPostProcessor后处理器的，所以要实现postProcessBeanFactory方法，这里的话我们关注点不在这儿，暂时忽略不管；
* 调用registry.registerBeanDefinition("registryBeanSample", beanDefinition);会进入DefaultListableBeanFactory#registerBeanDefinition方法，把bean信息放到beanDefinitionMap中，spring会根据这些信息生成对应的对象

## 三、源码分析及验证

BeanDefinition的加载一般是在spring启动的时候触发的操作，此时spring会调用refresh方法，来加载BeanDefinition信息。

AbstractApplicationContext.refresh

```java
public void refresh() throws BeansException, IllegalStateException {
   synchronized (this.startupShutdownMonitor) {
      // Prepare this context for refreshing.
      prepareRefresh();

      // Tell the subclass to refresh the internal bean factory.
      ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

      // Prepare the bean factory for use in this context.
      prepareBeanFactory(beanFactory);

      try {
         // Allows post-processing of the bean factory in context subclasses.
         postProcessBeanFactory(beanFactory);

         // Invoke factory processors registered as beans in the context.
         invokeBeanFactoryPostProcessors(beanFactory);

        //省略数行代码
      }
   }
}
```

在refresh方法中，调用了invokeBeanFactoryPostProcessors方法，该方法回去调用BeanDefinitionRegistryPostProcessor中的方法。

AbstractApplicationContext.invokeBeanFactoryPostProcessors

```java
protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
   PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

   // Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
   // (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
   if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
      beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
      beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
   }
}
```

invokeBeanFactoryPostProcessors方法中并没有看到调用BeanDefinitionRegistryPostProcessor的地方，而是调用了PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors

