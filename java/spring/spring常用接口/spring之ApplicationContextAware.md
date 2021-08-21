# Spring之ApplicationContextAware

## 一、简介

ApplicationContextAware:实现该接口的Bean能够在初始化获取到Spring的ApplicationContext。

ApplicationContextAware

```java
public interface ApplicationContextAware extends Aware {

   
   void setApplicationContext(ApplicationContext applicationContext) throws BeansException;

}
```

ApplicationContext

```java
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
      MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

    /**
     * 返回此应用程序上下文的唯一 ID。
     * 返回：
     * 上下文的唯一 ID，如果没有则为null
     */
   @Nullable
   String getId();

    /**
     * 返回此上下文所属的已部署应用程序的名称。
     * 返回：
     * 已部署应用程序的名称，或默认情况下为空字符串
     */
   String getApplicationName();

    /**
     * 返回此上下文的友好名称。
     * 返回：
     * 此上下文的显示名称（从不为null)
     */
   String getDisplayName();

    /**
     * 返回首次加载此上下文时的时间戳。
     * 返回：
     * 首次加载此上下文时的时间戳（毫秒）
     */
   long getStartupDate();

    /**
     * 返回父上下文，如果没有父上下文并且这是上下文层次结构的根，则返回null 。
     * 返回：
     * 父上下文，如果没有父上下文，则为null
     */
   @Nullable
   ApplicationContext getParent();

       /**
     * 为此上下文公开 AutowireCapableBeanFactory 功能。
     * 这通常不被应用程序代码使用，除了初始化位于应用程序上下文之外的 bean 实例，将 Spring bean 生命周期（全部或部分）应用于它们。
     * 或者，由ConfigurableApplicationContext接口公开的内部 BeanFactory 也提供对AutowireCapableBeanFactory接口的访问。 本方法主要用作 ApplicationContext 接口上的一个方便的、特定的工具。
     * 注意：从 4.2 开始，此方法将在应用程序上下文关闭后始终抛出 IllegalStateException。 在当前的 Spring Framework 版本中，只有可刷新的应用程序上下文才会这样做； 从 4.2 开始，所有应用程序上下文实现都需要遵守。
     * 返回：
     * 此上下文的 AutowireCapableBeanFactory
     * 抛出：
     * IllegalStateException – 如果上下文不支持AutowireCapableBeanFactory接口，或者还没有拥有自动装配能力的 bean 工厂（例如，如果从未调用过refresh() ），或者如果上下文已经关闭
     * 另见：
     * ConfigurableApplicationContext.refresh() ,ConfigurableApplicationContext.getBeanFactory()
     */
   AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
```



## 二、源码分析及验证

既然ApplicationContextAware接口实在bean被初始化的时候被调用的，那么在探索源码的时候就顺藤摸瓜，从bean创建的地方去探索。

AbstractAutowireCapableBeanFactory.doCreateBean

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
      throws BeanCreationException {

   // Instantiate the bean.
   BeanWrapper instanceWrapper = null;
   if (mbd.isSingleton()) {
      instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
   }
   if (instanceWrapper == null) {
      instanceWrapper = createBeanInstance(beanName, mbd, args);
   }
   Object bean = instanceWrapper.getWrappedInstance();
   //省略数行代码

   // Initialize the bean instance.
   Object exposedObject = bean;
   try {
      populateBean(beanName, mbd, instanceWrapper);
      exposedObject = initializeBean(beanName, exposedObject, mbd);
   }
   catch (Throwable ex) {
      if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
         throw (BeanCreationException) ex;
      }
      else {
         throw new BeanCreationException(
               mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
      }
   }
    //省略数行代码

   return exposedObject;
}
```

doCreateBean先通过instanceWrapper获取一个bean，该bean属性还没被注入，然后调用populateBean方法去注入bean的属性，接着有调用了initializeBean方法去对bean做一些初始化的行为，我们要寻找的Aware接口一般就能在initializeBean接口中找到。

AbstractAutowireCapableBeanFactory.initializeBean

```java
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
    //省略数行代码
   Object wrappedBean = bean;
   if (mbd == null || !mbd.isSynthetic()) {
      wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
   }
   //省略数行代码

   return wrappedBean;
}
```

initializeBean方法调用了applyBeanPostProcessorsBeforeInitialization方法调用ApplicationContextAware

AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization

```java
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
      throws BeansException {

   Object result = existingBean;
   for (BeanPostProcessor processor : getBeanPostProcessors()) {
      Object current = processor.postProcessBeforeInitialization(result, beanName);
      if (current == null) {
         return result;
      }
      result = current;
   }
   return result;
}
```

看到这里的的朋友，心里是否有这样的疑问。applyBeanPostProcessorsBeforeInitialization方法只调用了BeanPostProcessor接口的方法，并没有涉及到ApplicationContextAware方法的调用，那么问什么又要说applyBeanPostProcessorsBeforeInitialization方法调用了ApplicationContextAware方法了呢。 

其实spring自己实现了一个ApplicationContextAwareProcessor类去调用bean的ApplicationContextAware接口方法，该类实现了BeanPostProcessor接口。因此applyBeanPostProcessorsBeforeInitialization方法在遍历到ApplicationContextAwareProcessor后就会去调用BeanPostProcessor的接口，然后调用ApplicationContextAware。

```java
class ApplicationContextAwareProcessor implements BeanPostProcessor {
  //省略数行代码

   @Override
   @Nullable
   public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      //省略数行代码
      if (acc != null) {
         AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            invokeAwareInterfaces(bean);
            return null;
         }, acc);
      }
      else {
         invokeAwareInterfaces(bean);
      }

      return bean;
   }

   private void invokeAwareInterfaces(Object bean) {
      //省略数行代码
      if (bean instanceof ApplicationContextAware) {
         ((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
      }
   }

}
```

以上，本次对ApplicationContextAware的讨论就结束了。spring真的设计得太优秀了，将面向对象的思想运用得淋漓尽致，基于接口而非实现，使得程序的拓展变得有序而灵活。