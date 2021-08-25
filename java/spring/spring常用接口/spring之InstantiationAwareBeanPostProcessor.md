# Spring之InstantiationAwareBeanPostProcessor

## 一、简介

先来看看接口定义：

```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

       /**
     * 在目标 bean 被实例化之前应用这个 BeanPostProcessor。 返回的 bean 对象可能是一个代理来代替目标 bean，有效地抑制目标 bean 的默认实例化。
     * 如果此方法返回一个非空对象，则bean 创建过程将被短路。 唯一应用的进一步处理是来自配置的BeanPostProcessors的postProcessAfterInitialization回调。
     * 此回调将应用于带有 bean 类的 bean 定义，以及工厂方法定义，在这种情况下，返回的 bean 类型将在此处传递。
     * 后处理器可以实现扩展的SmartInstantiationAwareBeanPostProcessor接口，以预测它们将在此处返回的 bean 对象的类型。
     * 默认实现返回null 。
     * 参数：
     * beanClass – 要实例化的 bean 的类
     * beanName – bean 的名称
     * 返回：
     * 要公开的 bean 对象而不是目标 bean 的默认实例，或者为null以进行默认实例化
     * 抛出：
     * BeansException – 出现错误时
     */
   @Nullable
   default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
      return null;
   }

    /**
     * 在 bean 被实例化之后，通过构造函数或工厂方法，但在 Spring 属性填充（从显式属性或自动装配）发生之前执行操作。
     * 这是在 Spring 的自动装配开始之前对给定 bean 实例执行自定义字段注入的理想回调。
     * 默认实现返回true 。
     * 参数：
     * bean – 创建的 bean 实例，尚未设置属性
     * beanName – bean 的名称
     * 返回：
     * 如果应该在 bean 上设置属性，则为true ； 如果应跳过属性填充，则为false 。 正常的实现应该返回true 。 返回false还将阻止在此 bean 实例上调用任何后续 InstantiationAwareBeanPostProcessor 实例。
     */
   default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
      return true;
   }

    /**
     * 在工厂将给定的属性值应用于给定的 bean 之前对给定的属性值进行后处理，无需任何属性描述符。
     * 如果实现提供自定义postProcessPropertyValues实现，则实现应返回null （默认值），否则返回pvs 。 在此接口的未来版本中（删除postProcessPropertyValues ），默认实现将直接按原样返回给定的pvs 。
     * 参数：
     * pvs – 工厂即将应用的属性值（永远不会为null ）
     * bean – 创建的 bean 实例，但其属性尚未设置
     * beanName – bean 的名称
     * 返回：
     * 应用于给定 bean 的实际属性值（可以是传入的 PropertyValues 实例），或者null继续处理现有属性，但特别继续调用postProcessPropertyValues （需要为当前 bean 类初始化PropertyDescriptor ）
     * 抛出：
     * BeansException – 出现错误时
     * 自从：
     * 5.1
     */
   @Nullable
   default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
         throws BeansException {

      return null;
   }

    /**
     * 在工厂将给定的属性值应用于给定的 bean 之前对给定的属性值进行后处理。 允许检查是否所有依赖项都已满足，例如基于 bean 属性设置器上的“必需”注释。
     * 还允许替换要应用的属性值，通常通过基于原始 PropertyValues 创建新的 MutablePropertyValues 实例，添加或删除特定值。
     * 默认实现按原样返回给定的pvs 。
     * 已弃用
     * 从 5.1 开始，支持postProcessProperties(PropertyValues, Object, String)
     * 参数：
     * pvs – 工厂即将应用的属性值（永远不会为null ）
     * pds - 目标 bean 的相关属性描述符（忽略依赖类型 - 工厂专门处理 - 已经过滤掉）
     * bean – 创建的 bean 实例，但其属性尚未设置
     * beanName – bean 的名称
     * 返回：
     * 应用于给定 bean 的实际属性值（可以是传入的 PropertyValues 实例），或null以跳过属性填充
     * 抛出：
     * BeansException – 出现错误时
     */
   @Deprecated
   @Nullable
   default PropertyValues postProcessPropertyValues(
         PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

      return pvs;
   }
}
```

从源码中我们可以获知的信息是该接口除了具有父接口中的两个方法以外还自己额外定义了三个方法。所以该接口一共定义了5个方法，这5个方法的作用分别是

| 方法                            | 描述                                                         |
| :------------------------------ | :----------------------------------------------------------- |
| postProcessBeforeInitialization | BeanPostProcessor接口中的方法,在Bean的自定义初始化方法之前执行 |
| postProcessAfterInitialization  | BeanPostProcessor接口中的方法 在Bean的自定义初始化方法执行完成之后执行 |
| postProcessBeforeInstantiation  | 自身方法，是最先执行的方法，它在目标对象实例化之前调用，该方法的返回值类型是Object，我们可以返回任何类型的值。由于这个时候目标对象还未实例化，所以这个返回值可以用来代替原本该生成的目标对象的实例(比如代理对象)。如果该方法的返回值代替原本该生成的目标对象，后续只有postProcessAfterInitialization方法会调用，其它方法不再调用；否则按照正常的流程走 |
| postProcessAfterInstantiation   | 在目标对象实例化之后调用，这个时候对象已经被实例化，但是该实例的属性还未被设置，都是null。因为它的返回值是决定要不要调用postProcessPropertyValues方法的其中一个因素（因为还有一个因素是mbd.getDependencyCheck()）；如果该方法返回false,并且不需要check，那么postProcessPropertyValues就会被忽略不执行；如果返回true，postProcessPropertyValues就会被执行 |
| postProcessPropertyValues       | 对属性值进行修改，如果postProcessAfterInstantiation方法返回false，该方法可能不会被调用。可以在该方法内对属性值进行修改 |

注意两个单词

| 单词           | 含义                    |
| :------------- | :---------------------- |
| Instantiation  | 表示实例化,对象还未生成 |
| Initialization | 表示初始化,对象已经生成 |

## 二、源码解析及验证

关于BeanPostProcessor的两个接口，本文就不在阐述，本文只阐述InstantiationAwareBeanPostProcessor的两个主要接口在spring中被调用的时机。

### postProcessBeforeInstantiation

顾名思义该接口会在Bean实例化前被调用。先看看spring创建bean的接口调用。

AbstractAutowireCapableBeanFactory.createBean

```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
      throws BeanCreationException {

   if (logger.isTraceEnabled()) {
      logger.trace("Creating instance of bean '" + beanName + "'");
   }
   RootBeanDefinition mbdToUse = mbd;
    
    //省略数行代码

   try {
      // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
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

createBean中，会先调用resolveBeforeInstantiation去调用实现了postProcessBeforeInstantiation的bean。然后返回postProcessBeforeInstantiation生成的对象bean。如果bean不为空，则直接返回bean。也就是说后面创建bean的步骤（doCreateBean）就不会再进行了。



### postProcessAfterInstantiation

createBean方法会去调用AbstractAutowireCapableBeanFactory.doCreateBean创建bean。

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
   // 省略数行代码

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

    // 省略数行代码
   return exposedObject;
}
```

doCreateBean方法会调用populateBean方法，在给bean注入属性的时候会去调用postProcessAfterInstantiation方法。

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
   //省略数行代码

   // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
   // state of the bean before properties are set. This can be used, for example,
   // to support styles of field injection.
   if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
      for (BeanPostProcessor bp : getBeanPostProcessors()) {
         if (bp instanceof InstantiationAwareBeanPostProcessor) {
            InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
            if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
               return;
            }
         }
      }
   }

 //省略数行代码
}
```

以上，我们见到了postProcessAfterInstantiation是如被调用的了，在给bean注入属性值的时候，spring会判断是否有bean实现了InstantiationAwareBeanPostProcessors接口，如果实现了，则调用postProcessAfterInstantiation方法，实现bean实例化后的一些扩展操作。
