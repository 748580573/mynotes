# Spring Bean 循环依赖为什么需要三级缓存

> 这里指的是单例的、非构造依赖的循环引用。很多人都知道Spring用了三层缓存来解决循环依赖，但是不知道其原因，为什么是三级缓存？二级缓存不行吗？一级缓存不可以 ？

### 三级缓存

Spring 解决循环依赖的核心就是提前暴露对象，而提前暴露的对象就是放置于第二级缓存中。缓存的底层都是Map，至于它们属于第几层是由Spring获取数据顺序以及其作用来表现的。

三级缓存的说明：

| 名称                    | 作用                                                         |
| ----------------------- | ------------------------------------------------------------ |
| `singletonObjects`      | 一级缓存，存放完整的 Bean。                                  |
| `earlySingletonObjects` | 二级缓存，存放提前暴露的Bean，Bean 是不完整的，未完成属性注入和执行 初始化（init） 方法。 |
| `singletonFactories`    | 三级缓存，存放的是 Bean 工厂，主要是生产 Bean，存放到二级缓存中。 |

在`DefaultSingletonBeanRegistry`类中：

```java
	/** Cache of singleton objects: bean name to bean instance. */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/** Cache of singleton factories: bean name to ObjectFactory. */
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

	/** Cache of early singleton objects: bean name to bean instance. */
	private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
```

### 为什么使用三级缓存？

#### 第一 先说说第一级缓存`singletonObjects`

1.先说一级缓存`singletonObjects`。实际上，一级依赖已经可以解决循环依赖的问题，假设两个beanA和beanB相互依赖，beanA被实例化后，放入一级缓存，即使没有进行初始化，但是beanA的引用已经创建（栈到堆的引用已经确定），其他依赖beanB已经可以持有beanA的引用，但是这个bean在没有初始化完成前，其内存（堆）里的字段、方法等还不能正常使用，but，这并不影响对象之间引用持有；这个时候beanA依赖的beanB实例化，beanB可以顺利拿到beanA的引用，完成beanB的实例化与初始化，并放入一级缓存，在beanB完成创建后，beanA通过缓存顺利拿到beanB的引用，至此，循环依赖只需一层缓存就能完成。

2.一级缓存的关键点在与：bean实例化与初始化的分离。从JVM的角度，实例化后，对象已经存在，其内的属性都是初始默认值，只有在初始化后才会赋值，以及持有其他对象的引用。通过这个特性，在实例化后，我们就可以将对象的引用放入缓存交给需要引用依赖的其他对象，这个过程就是提前暴露。

#### 第二 说说第三级缓存`singletonFactories`

1.上述我们通过一级缓存已经拿到的对象有什么问题？

根本问题就是，我们拿到的是bean的原始引用，如果我们需要的是bean的代理对象怎么办？Spring里充斥了大量的动态代理模式的架构，典型的AOP就是动态代理模式实现的，再比如我们经常使用的配置类注解`@Configuration`在缺省情况下（full mode），其内的所有@Bean都是处于动态代理模式，除非手动指定`proxyBeanMethods = false`将配置转成简略模式（lite mode）。

2.所以，Spring在bean实例化后，将原始bean放入第三级缓存singletonFactories中，第三级缓存里实际存入的是ObjectFactory接口签名的回调实现。

```java
# 函数签名
addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory)
    
# 具体实现由回调决定    
addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
```

那么如果有动态代理的需求，里面可以埋点进行处理，将原始bean包装后返回。

```java
	protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
		Object exposedObject = bean;
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
					SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
					exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
				}
			}
		}
		return exposedObject;
	}
```

3.通过第三级缓存我们可以拿到可能经过包装的对象，解决对象代理封装的问题。

#### 第三 说说第二级缓存`earlySingletonObjects`

1.为什么需要`earlySingletonObjects`这个二级缓存？并且，如果只有一个缓存的情况下，为什么不直接使用`singletonFactories`这个缓存，即可实现代理又可以缓存数据。

2.从软件设计角度考虑，三个缓存代表三种不同的职责，根据单一职责原理，从设计角度就需分离三种职责的缓存，所以形成三级缓存的状态。

3、再次说说三级缓存的划分及其作用。

一级缓存`singletonObjects`是完整的bean，它可以被外界任意使用，并且不会有歧义。

二级缓存`earlySingletonObjects`是不完整的bean，没有完成初始化，它与`singletonObjects`的分离主要是职责的分离以及边界划分，可以试想一个Map缓存里既有完整可使用的bean，也有不完整的，只能持有引用的bean，在复杂度很高的架构中，很容易出现歧义，并带来一些不可预知的错误。

三级缓存`singletonFactories`，其职责就是包装一个bean，有回调逻辑，所以它的作用非常清晰，并且只能处于第三层。

在实际使用中，要获取一个bean，先从一级缓存一直查找到三级缓存，缓存bean的时候是从三级到一级的顺序保存，并且缓存bean的过程中，三个缓存都是互斥的，只会保持bean在一个缓存中，而且，最终都会在一级缓存中。