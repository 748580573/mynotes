# spring之BeanNameAware与BeanFactoryAware

## 一、介绍

BeanNameAware:实现该接口的Bean能够在初始化时知道自己在BeanFactory中对应的名字。

BeanFactoryAware:实现该接口的Bean能够在初始化时知道自己所在的BeanFactory的名字。

## 二、源码验证

从介绍中可得之，两个接口都是在bean被初始化的时候被回调的。那么我们直接去bean初始化的地方开始探索把。

首先查看AbstractBeanFactory.doGetBean方法

````java
protected <T> T doGetBean(
			String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
			throws BeansException {

		String beanName = transformedBeanName(name);
		Object bean;

		// Eagerly check singleton cache for manually registered singletons.
		Object sharedInstance = getSingleton(beanName);
		if (sharedInstance != null && args == null) {
			//省略数行代码
		}
		else {
			

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
					bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}
            //省略数行代码
		}
      //省略数行代码
		return (T) bean;
	}
````

AbstractBeanFactory.doGetBean代用了createBean方法去进行bean的创建

AbstractAutowireCapableBeanFactory.createBean

```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {
    // 省略数行代码
		try {
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

AbstractAutowireCapableBeanFactory又继续调用了doCreateBean方法去创建bean

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
  //省略数行代码

   return exposedObject;
}
```

doCreateBean会先调用instanceWrapper.getWrappedInstance()方法去实例化bean，然后调用populateBean方法为bean注入依赖的属性，又调用initializeBean方法去处理一系列的回调方法。我们今天要看的两个Aware接口就在initializeBean方法中被处理。

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
    //省略数行代码

   try {
      invokeInitMethods(beanName, wrappedBean, mbd);
   }
   catch (Throwable ex) {
      throw new BeanCreationException(
            (mbd != null ? mbd.getResourceDescription() : null),
            beanName, "Invocation of init method failed", ex);
   }

   return wrappedBean;
}
```

initializeBean方法调用了invokeAwareMethods方法，去回调BeanNameAware与BeanFactoryAware两个接口

AbstractAutowireCapableBeanFactory.invokeAwareMethods

```java
	private void invokeAwareMethods(String beanName, Object bean) {
		if (bean instanceof Aware) {
			if (bean instanceof BeanNameAware) {
				((BeanNameAware) bean).setBeanName(beanName);
			}
			if (bean instanceof BeanClassLoaderAware) {
				ClassLoader bcl = getBeanClassLoader();
				if (bcl != null) {
					((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
				}
			}
			if (bean instanceof BeanFactoryAware) {
				((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
			}
		}
	}
```

以上，我们看到了bean在初始化的时候，回去调用AbstractAutowireCapableBeanFactory.invokeAwareMethods，然后将对应的实例传递给对应的实现了Aware接口的bean
