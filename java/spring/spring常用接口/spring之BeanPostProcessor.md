# spring之BeanPostProcessor

## 一、简介

BeanPostProcessor为spring初始化bean的时候提供了扩展点，用户可以在bean被初始化前后的两个实际进行一些自定操作。我们先看看BeanPostProcessor长啥样。

```java
    public interface BeanPostProcessor {

  
   /**
     * 在任何 bean 初始化回调（如 InitializingBean 的afterPropertiesSet或自定义初始化方法）之前，将此BeanPostProcessor应用于给定的新 bean 实例。 bean 已经被填充了属性值。 返回的 bean 实例可能是原始实例的包装器。
     * 默认实现按原样返回给定的bean 。
     * 参数：
     * bean – 新的 bean 实例
     * beanName – bean 的名称
     * 返回：
     * 要使用的 bean 实例，可以是原始实例，也可以是包装好的实例； 如果为null ，则不会调用后续的 BeanPostProcessors
     * 抛出：
     * BeansException – 出现错误时
     * @throws Exception
     */
   @Nullable
   default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      return bean;
   }
    
     /**
     * 在任何 bean 初始化回调（如 InitializingBean 的afterPropertiesSet或自定义初始化方法）之后，将此BeanPostProcessor应用于给定的新 bean 实例。 bean 已经被填充了属性值。 返回的 bean 实例可能是原始实例的包装器。
     * 在 FactoryBean 的情况下，将为 FactoryBean 实例和 FactoryBean 创建的对象调用此回调（从 Spring 2.0 开始）。 后处理器可以通过相应的bean instanceof FactoryBean检查来决定是应用于 FactoryBean 或创建的对象还是两者。
     * 与所有其他BeanPostProcessor回调相比，此回调也将在由InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation方法触发的短路后调用。
     * 默认实现按原样返回给定的bean 。
     * 参数：
     * bean – 新的 bean 实例
     * beanName – bean 的名称
     * 返回：
     * 要使用的 bean 实例，可以是原始实例，也可以是包装好的实例； 如果为null ，则不会调用后续的 BeanPostProcessors
     * 抛出：
     * BeansException – 出现错误时
     * 也可以看看：
     * org.springframework.beans.factory.InitializingBean.afterPropertiesSet , org.springframework.beans.factory.FactoryBean
     * @throws Exception
     */
   @Nullable
   default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      return bean;
   }

}
```

感叹一句，spring的实现实在太优雅了，我们可以通过它的方法名去猜测这个方法时干嘛的。`postProcessBeforeInitialization`表示在bean实例化之前被调用，`postProcessAfterInitialization`则表示在bean实例化后被调用。这样，BeanPostProcessor的调用顺序则为：

````shell
 -->  Spring IOC容器实例化Bean
 -->  调用BeanPostProcessor的postProcessBeforeInitialization方法
 -->  调用bean实例的初始化方法
 -->  调用BeanPostProcessor的postProcessAfterInitialization方法
````

## 二、源码验证

探究源码的实现，既可以提高我们对spring的理解，也能在这过程提升我们的编程能力。既然BeanPostProcessor是在bean被初始化的前后被调用的，那么就从创建bean开始，去理清BeanPostProcessor被调用的过程吧。

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

以上，方法中先调用instanceWrapper.getWrappedInstance()方法生成一个bean实例，不过该bean实例的属性没有被初始化。

然后调用populateBean方法去为bean注入属性。

然后调用initializeBean方法执行一些动作来初始化bean。我们接着进入initializeBean方法。

AbstractAutowireCapableBeanFactory.initializeBean

```java
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
   if (System.getSecurityManager() != null) {
      AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
         invokeAwareMethods(beanName, bean);
         return null;
      }, getAccessControlContext());
   }
   else {
      invokeAwareMethods(beanName, bean);
   }

   Object wrappedBean = bean;
   if (mbd == null || !mbd.isSynthetic()) {
      wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
   }

   try {
      invokeInitMethods(beanName, wrappedBean, mbd);
   }
   catch (Throwable ex) {
      throw new BeanCreationException(
            (mbd != null ? mbd.getResourceDescription() : null),
            beanName, "Invocation of init method failed", ex);
   }
   if (mbd == null || !mbd.isSynthetic()) {
      wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
   }

   return wrappedBean;
}
```

以上，我们找到了`applyBeanPostProcessorsBeforeInitialization`方法和`applyBeanPostProcessorsAfterInitialization`方法。这两个方法就会去调用BeanPostProcessor中的两个回调方法。至此，我们验证BeanPostProcessor是如何被调用的。

