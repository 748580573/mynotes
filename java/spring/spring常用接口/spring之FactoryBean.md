# spring之FactoryBean

## 一、说明

在spring中有两种bean存在，一种是我们经常用到的普通bean，spring管理普通bean的时候，一般就是管理该bean实例化后对应的对象。

````java
//普通bean
@Component
publica class MyBean{
    
}
````

spring中的另一种bean则是工厂bean，即FactoryBean。这种bean与普通bean不同之处在于，spring管理的是FactoryBean.getObject()返回的对象，而不是FactoryBean本身。这样可以让我们自定义Bean的创建过程更加灵活。

```java
@Component
public class MapperFactoryBean implements FactoryBean {
    @Override
    public Object getObject() throws Exception {
        return new MyBean();
    }

    @Override
    public Class<?> getObjectType() {
        return MyBean.class;
    }
}
```

## 源码解析及验证

进入源码，去探索功能是如何实现，既可以加深我们对FactoryBean接口的理解，也能加强我们的开发能力。

我们从AbstractBeanFactory.doGetBean开始

```java
protected <T> T doGetBean(
      String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
      throws BeansException {

    //省略数行代码
         // Create bean instance.
         if (mbd.isSingleton()) {
            sharedInstance = getSingleton(beanName, () -> {
               try {
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
            //getObjectForBeanInstance方法用于获取bean的实例
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
         }
         
    //省略数行代码
   return (T) bean;
}
```

doGetBean方法通过调用getObjectForBeanInstance方法去获取bean，我们接着看看getObjectForBeanInstance的实现。

AbstractBeanFactory.getObjectForBeanInstance

```java
protected Object getObjectForBeanInstance(
      Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

   // Don't let calling code try to dereference the factory if the bean isn't a factory.
   if (BeanFactoryUtils.isFactoryDereference(name)) {
      if (beanInstance instanceof NullBean) {
         return beanInstance;
      }
      if (!(beanInstance instanceof FactoryBean)) {
         throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
      }
      if (mbd != null) {
         mbd.isFactoryBean = true;
      }
      return beanInstance;
   }

   // Now we have the bean instance, which may be a normal bean or a FactoryBean.
   // If it's a FactoryBean, we use it to create a bean instance, unless the
   // caller actually wants a reference to the factory.
   //
   if (!(beanInstance instanceof FactoryBean)) {
      return beanInstance;
   }

   Object object = null;
   if (mbd != null) {
      mbd.isFactoryBean = true;
   }
   else {
       //获取工厂bean生产的bean
      object = getCachedObjectForFactoryBean(beanName);
   }
   if (object == null) {
      // Return bean instance from factory.
      FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
      // Caches object obtained from FactoryBean if it is a singleton.
      if (mbd == null && containsBeanDefinition(beanName)) {
         mbd = getMergedLocalBeanDefinition(beanName);
      }
      boolean synthetic = (mbd != null && mbd.isSynthetic());
      object = getObjectFromFactoryBean(factory, beanName, !synthetic);
   }
   return object;
}
```

值得注意的是getObjectForBeanInstance方法的beanInstance参数是一个被实例化的bean，也就是说其实如果一个bean是FactoryBean，那么这个bean会被先实例化，然后再调用getObjectFromFactoryBean方法去获取这个bean的getObject方法去获取实际需要被spring管理的bean。下面我们看看getObjectFromFactoryBean方法。

FactoryBeanRegistrySupport.getObjectFromFactoryBean

```java
protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
   if (factory.isSingleton() && containsSingleton(beanName)) {
      synchronized (getSingletonMutex()) {
         Object object = this.factoryBeanObjectCache.get(beanName);
         if (object == null) {
            object = doGetObjectFromFactoryBean(factory, beanName);
            // Only post-process and store if not put there already during getObject() call above
            // (e.g. because of circular reference processing triggered by custom getBean calls)
            Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
            //省略N行代码
         }
         return object;
      }
   }
   }
}
```

getObjectFromFactoryBean调用了doGetObjectFromFactoryBean方法获取工厂方法生产的bean，我们接着看doGetObjectFromFactoryBean方法

FactoryBeanRegistrySupport.doGetObjectFromFactoryBean

```java
private Object doGetObjectFromFactoryBean(FactoryBean<?> factory, String beanName) throws BeanCreationException {
   Object object;
   try {
      if (System.getSecurityManager() != null) {
         AccessControlContext acc = getAccessControlContext();
         try {
            object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
         }
         catch (PrivilegedActionException pae) {
            throw pae.getException();
         }
      }
      else {
          //这里调用getObject获取FactoryBean生产的bean
         object = factory.getObject();
      }
   }
   catch (FactoryBeanNotInitializedException ex) {
      throw new BeanCurrentlyInCreationException(beanName, ex.toString());
   }
   catch (Throwable ex) {
      throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
   }

   // Do not accept a null value for a FactoryBean that's not fully
   // initialized yet: Many FactoryBeans just return null then.
   if (object == null) {
      if (isSingletonCurrentlyInCreation(beanName)) {
         throw new BeanCurrentlyInCreationException(
               beanName, "FactoryBean which is currently in creation returned null from getObject");
      }
      object = new NullBean();
   }
   return object;
}
```

以上，我们最后找到了调用FactoryBean.getObject方法的地方，在spring生产bean的时候会先实例化FactoryBean，但是这个bean并没有被纳入spring的管理中，而是去获取这个FactoryBean.getObject的实例，然后纳入spring的管理中。