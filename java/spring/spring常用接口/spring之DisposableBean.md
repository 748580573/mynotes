# spring之DisposableBean

## 一、说明

spring提供DisposableBean接口，是的实现该接口的bean在被销毁时，可以触发一些处理逻辑。

```java
public interface DisposableBean {

   /**
    * Invoked by the containing {@code BeanFactory} on destruction of a bean.
    * @throws Exception in case of shutdown errors. Exceptions will get logged
    * but not rethrown to allow other beans to release their resources as well.
    */
   void destroy() throws Exception;

}
```

## 二、源码分析及验证

为了验证DisposableBean是否真的是在bean被销毁的时候会调用destroy方法，以及如何实现bean被销毁时调用destroy方法，我们一起跟着源码来一步步看看该接口的实现逻辑吧。

按照一般逻辑，spring会在初始化bean的时候对一些指定的接口进行处理，因此以AbstractAutowireCapableBeanFactory.createBean方法为切入口，去剖析源码，往往不失为一种好的选择。

AbstractAutowireCapableBeanFactory.createBean

```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
      throws BeanCreationException {

    //省略数行代码
   try {
       //调用doCreateBean方法创建bean
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

AbstractAutowireCapableBeanFactory.createBean方法并没有直接去创建bean，而是调用了doCreateBean去创建bean。我们接着看doCreateBean方法的实现。

AbstractAutowireCapableBeanFactory.doCreateBean

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
      throws BeanCreationException {

   //省略数行代码，被省略的代码主要是去初始化bean

   // Register bean as disposable.
   try {
   //  注册DisposableBean接口
      registerDisposableBeanIfNecessary(beanName, bean, mbd);
   }
   catch (BeanDefinitionValidationException ex) {
      throw new BeanCreationException(
            mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
   }

   return exposedObject;
}
```

至此，可以看到doCreateBean方法调动了registerDisposableBeanIfNecessary方法去注册DisposableBean接口。直接进registerDisposableBeanIfNecessary方法，看能否接口其神秘的面纱。

AbstractAutowireCapableBeanFactory.registerDisposableBeanIfNecessary

```java
protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
   AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
   if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
      if (mbd.isSingleton()) {
         // Register a DisposableBean implementation that performs all destruction
         // work for the given bean: DestructionAwareBeanPostProcessors,
         // DisposableBean interface, custom destroy method.
         //到这里实现了DisposableBean接口的注册
         registerDisposableBean(beanName,
               new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
      }
      else {
         // A bean with a custom scope...
         Scope scope = this.scopes.get(mbd.getScope());
         if (scope == null) {
            throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
         }
         scope.registerDestructionCallback(beanName,
               new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
      }
   }
}
```

到这里，spring将bean注册到一个map里，将实现DisposableBean接口的bean封装为DisposableBeanAdapter接口，然后放进一个名叫disposableBeans的Map中。

当spring被调用close方法后就会触发去触发bean的DisposableBean接口方法。调用链为：context.close() -> doClose() -> destroyBeans() -> destroySingletons() ->destroySingleton()。

````java
public void destroySingleton(String beanName) {
    // Remove a registered singleton of the given name, if any.
    // 移除缓存
    removeSingleton(beanName);

    // Destroy the corresponding DisposableBean instance.
    DisposableBean disposableBean;
    // 加锁
    synchronized (this.disposableBeans) {
        // 根据名称，从需要销毁的是实例集合移除实例，得到disposableBean
        disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
    }
    // 销毁bean
    destroyBean(beanName, disposableBean);
}
````

