# Spring之InitiallizingBean

## 一、说明

InitiallizingBean接口为bean在属性实例化后提供了一个扩展用的操作，它只包括afterPropertiesSet方法，凡是继承该接口的类，在bean的属性初始化后都会执行该方法。

```java
public interface InitializingBean {

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     * <p>This method allows the bean instance to perform initialization only
     * possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     * @throws Exception in the event of misconfiguration (such
     * as failure to set an essential property) or if initialization fails.
     */
    void afterPropertiesSet() throws Exception;

}
```

从类名和方法名可以轻松知道，这个接口的作用就是在bean初始化期间且bean属性被设置后被调用的。



## 二、源码分析验证

通过查看AbstractAutowireCapableBeanFactory类的源码，我们一步一步的探索InitiallizingBean接口在什么时候被调用，并且验证该接口是否是在bean属性被填充后才调用。

首先查看AbstractBeanFactory.doGetBean方法

```java
protected <T> T doGetBean(
      String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
      throws BeansException {

     // 此处省略n行代码

         // Create bean instance.
         if (mbd.isSingleton()) {
            sharedInstance = getSingleton(beanName, () -> {
               try {
                   //createBean方法，在这里开始准备创建bean
                  return createBean(beanName, mbd, args);
               }
               catch (BeansException ex) {
                  // Explicitly remove instance from singleton cache: It might have been put there
                  // eagerly by the creation process, to allow for circular reference resolution.
                  // Also remove any beans that received a temporary reference to the bean.
                  destroySingleton(beanName);
                  throw ex;
               }
            });
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
         }

         else if (mbd.isPrototype()) {
            // It's a prototype -> create a new instance.
            Object prototypeInstance = null;
            try {
               beforePrototypeCreation(beanName);
               prototypeInstance = createBean(beanName, mbd, args);
            }
            finally {
               afterPrototypeCreation(beanName);
            }
            bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
         }
     //此处省略N行代码
   return (T) bean;
}
```

在doGetBean方法中调用了AbstractAutowireCapableBeanFactory.createBean方法对bean进行创建，接下来我们就进入createBean方法看看，它是怎么运行的。

```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
      throws BeanCreationException {
    //此处省略n行
   try {
      // 让 BeanPostProcessors 有机会返回一个代理而不是目标 bean 实例。
      Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
      if (bean != null) {
         return bean;
      }
   }
   catch (Throwable ex) {
      throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
            "BeanPostProcessor before instantiation of bean failed", ex);
   }

   try {
       //这里调用doCreateBean方法对bean进行创建
      Object beanInstance = doCreateBean(beanName, mbdToUse, args);
      if (logger.isTraceEnabled()) {
         logger.trace("Finished creating instance of bean '" + beanName + "'");
      }
      return beanInstance;
   }
   catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
      // A previously detected exception with proper bean creation context already,
      // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
      throw ex;
   }
   catch (Throwable ex) {
      throw new BeanCreationException(
            mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
   }
}
```

createBean方法并没有完成对bean的创建，从上面的内容看出，程序继续调用了doCreateBean方法去创建bean。

继续看AbstractAutowireCapableBeanFactory.doCreateBean方法

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
      throws BeanCreationException {
   //省略数行代码

   // Initialize the bean instance.
   Object exposedObject = bean;
   try {
      //为bean填充属性值
      populateBean(beanName, mbd, instanceWrapper);
      //调用InitiallizingBean接口的afterPropertiesSet方法
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

   // 省略数行代码

   return exposedObject;
}
```

以上，我们找到了调用InitiallizingBean被调用的位置，并且刚好调用initializeBean方法的前一个动作就是populateBean方法填充bean属性。

