# Spring事务之Transaction注解事务源码解析

## 1、环境

1. jdk1.8
2. Spring版本：5.2.3.RELEASE
3. mysql5.7

## 2、@Transaction 事务的用法

咱们先来回顾一下，@Transaction 事务的用法，特别简单，2个步骤

1、在需要让spring管理事务的方法上添加 @Transaction 注解

2、在spring配置类上添加 @EnableTransactionManagement 注解，这步特别重要，别给忘了，有了这个注解之后，@Trasaction标注的方法才会生效。

## 3、@Transaction事务原理

原理比较简单，内部是通过spring aop的功能，通过拦截器拦截 @Transaction 方法的执行，在方法前后添加事务的功能。

## 4、@EnableTransactionManagement注解作用

@EnableTransactionManagement注解会开启spring自动管理事务的功能，有了这个注解之后，spring容器启动的过程中，会拦截所有bean的创建过程，判断bean 是否需要让spring来管理事务，即判断bean中是否有@Transaction注解，判断规则如下

1、一直沿着当前bean的类向上找，先从当前类中，然后父类、父类的父类，当前类的接口、接口父接口，父接口的父接口，一直向上找，一下这些类型上面是否有 @Transaction注解

2、类的任意public方法上面是否有@Transaction注解

如果bean满足上面任意一个规则，就会被spring容器通过aop的方式创建代理，代理中会添加一个拦截器

```
org.springframework.transaction.interceptor.TransactionInterceptor
```

TransactionInterceptor 拦截器是关键，它会拦截@Trasaction方法的执行，在方法执行前后添加事务的功能，这个拦截器中大部分都是编程式事务的代码，若 [编程式事务的源码](../Spring事务源码解析/Spring事务源码解析.md) 大家看懂了，这个拦截器源码看起来就是小儿科了。

## 5、@EnableTransactionManagement源码解析

````java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TransactionManagementConfigurationSelector.class) //@1
public @interface EnableTransactionManagement {
    // 是基于类的代理（cglib），还是基于接口的代理（jdk动态代理），默认为false，表示是基于jdk动态代理
    boolean proxyTargetClass() default false;
    //通知的模式，默认是通过aop的方式
    AdviceMode mode() default AdviceMode.PROXY;
    // 我们知道这个注解的功能最终是通过aop的方式来实现的，对bean创建了一个代理，代理中添加了一个拦截器
    // 当代理中还有其他拦截器的是时候，可以通过order这个属性来指定事务拦截器的顺序
    // 默认值是 LOWEST_PRECEDENCE = Integer.MAX_VALUE,拦截器的执行顺序是order升序
    int order() default Ordered.LOWEST_PRECEDENCE;
}
````

注意`@1`这个代码

1. `@Import(TransactionManagementConfigurationSelector.class)`

用到了@Import注解，对这个注解不熟悉的可以看一下[Spring系列第18篇：@import详解(bean批量注册)](https://mp.weixin.qq.com/s?__biz=MzA5MTkxMDQ4MQ==&mid=2648934173&idx=1&sn=60bb7d58fd408db985a785bfed6e1eb2&chksm=88621f23bf15963589f06b7ce4e521a7c8d615b1675788f383cbb0bcbb05b117365327e1941a&token=704646761&lang=zh_CN#rd)，这个注解的value是TransactionManagementConfigurationSelector，看一下这个类的源码，重点是他的`selectImports`方法，这个方法会返回一个类名数组，spring容器启动过程中会自动调用这个方法，将这个方法指定的类注册到spring容器中；方法的参数是AdviceMode，这个就是@EnableTransactionManagement注解中mode属性的值，默认是PROXY，所以会走到@1代码处

````java
public class TransactionManagementConfigurationSelector extends AdviceModeImportSelector<EnableTransactionManagement> {
    @Override
    protected String[] selectImports(AdviceMode adviceMode) {
        switch (adviceMode) {
            case PROXY: //@1
                return new String[] {AutoProxyRegistrar.class.getName(),
                        ProxyTransactionManagementConfiguration.class.getName()};
            case ASPECTJ:
                return new String[] {determineTransactionAspectClass()};
            default:
                return null;
        }
    }
}
````

最终会在spirng容器中注册下面这2个bean

```
AutoProxyRegistrar
ProxyTransactionManagementConfiguration
```

下面来看一下这2个类的代码。

#### AutoProxyRegistrar

这个类实现了ImportBeanDefinitionRegistrar接口，这个接口中有个方法`registerBeanDefinitions`，spring容器在启动过程中会调用这个方法，开发者可以在这个方法中做一些bean注册的事情，而AutoProxyRegistrar在这个方法中主要做的事情就是下面`@1`的代码，大家可以点进去看看，这里我就不点进去了，这个代码的作用就是在容器中做了一个非常关键的bean：InfrastructureAdvisorAutoProxyCreator，这个类之前在aop中介绍过，是bean后置处理器，会拦截所有bean的创建，对符合条件的bean创建代理。

````java
public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        boolean candidateFound = false;
        Set<String> annTypes = importingClassMetadata.getAnnotationTypes();
        for (String annType : annTypes) {
            AnnotationAttributes candidate = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
            if (candidate == null) {
                continue;
            }
            Object mode = candidate.get("mode");
            Object proxyTargetClass = candidate.get("proxyTargetClass");
            if (mode != null && proxyTargetClass != null && AdviceMode.class == mode.getClass() &&
                    Boolean.class == proxyTargetClass.getClass()) {
                candidateFound = true;
                if (mode == AdviceMode.PROXY) {
                    AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);//@1
                    if ((Boolean) proxyTargetClass) {
                        AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
                        return;
                    }
                }
            }
        }
    }
}
````

**说的简单点：AutoProxyRegistrar的作用就是启用spring aop的功能，对符合条件的bean创建代理。**

#### ProxyTransactionManagementConfiguration

````java
@Configuration(proxyBeanMethods = false)
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {
    //注册bean：事务顾问（spring aop中拦截器链就是一个个的Advisor对象）
    @Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor(
            TransactionAttributeSource transactionAttributeSource,
            TransactionInterceptor transactionInterceptor) {
        BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
        advisor.setTransactionAttributeSource(transactionAttributeSource);
        //设置事务拦截器
        advisor.setAdvice(transactionInterceptor);
        if (this.enableTx != null) {
            //设置aop中事务拦截器的顺序
            advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
        }
        return advisor;
    }
    //注册bean:TransactionAttributeSource，TransactionAttributeSource用来获取获取事务属性配置信息：TransactionAttribute
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public TransactionAttributeSource transactionAttributeSource() { //@1
        return new AnnotationTransactionAttributeSource();
    }
    //注册bean：事务拦截器
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public TransactionInterceptor transactionInterceptor(
            TransactionAttributeSource transactionAttributeSource) {
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionAttributeSource(transactionAttributeSource);
           //拦截器中设置事务管理器，txManager可以为空
        if (this.txManager != null) {
            interceptor.setTransactionManager(this.txManager);
        }
        return interceptor;
    }
}
````

是个配置类，代码比较简单，注册了3个bean，最重要的一点就是添加了事务事务拦截器：TransactionInterceptor。

AutoProxyRegistrar负责启用aop的功能，而ProxyTransactionManagementConfiguration负责在aop中添加事务拦截器，二者结合起来的效果就是：对@Transaction标注的bean创建代理对象，代理对象中通过TransactionInterceptor拦截器来实现事务管理的功能。

再看下代码`@1`，注册了一个TransactionAttributeSource类型的bean

TransactionAttributeSource接口源码：

````java
public interface TransactionAttributeSource {
    /**
     * 确定给定的类是否是这个TransactionAttributeSource元数据格式中的事务属性的候选类。
     * 如果此方法返回false，则不会遍历给定类上的方法，以进行getTransactionAttribute内省。
     * 因此，返回false是对不受影响的类的优化，而返回true仅仅意味着类需要对给定类上的每个方法进行完全自省。
     **/
    default boolean isCandidateClass(Class<?> targetClass) {
        return true;
    }
    //返回给定方法的事务属性，如果该方法是非事务性的，则返回null。
    TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass);
}
````

getTransactionAttribute方法用来获取指定方法上的事务属性信息TransactionAttribute，大家对TransactionDefinition比较熟悉吧，用来配置事务属性信息的，而TransactionAttribute继承了TransactionDefinition接口，源码如下，而TransactionAttribute中新定义了2个方法，一个方法用来指定事务管理器bean名称的，一个用来判断给定的异常是否需要回滚事务

```java
public interface TransactionAttribute extends TransactionDefinition {
    //事务管理器的bean名称
    @Nullable
    String getQualifier();
    //判断指定的异常是否需要回滚事务
    boolean rollbackOn(Throwable ex);
}
```

TransactionAttributeSource接口有个实现类AnnotationTransactionAttributeSource，负责将@Transaction解析为TransactionAttribute对象，大家可以去这个类中设置一下断点看一下@Transaction注解查找的顺序，这样可以深入理解@Transaction放在什么地方才会让事务起效。

AnnotationTransactionAttributeSource内部最会委托给SpringTransactionAnnotationParser#parseTransactionAnnotation方法来解析@Transaction注解，进而得到事务属性配置信息：RuleBasedTransactionAttribute，代码如下：

````java
org.springframework.transaction.annotation.SpringTransactionAnnotationParser
protected TransactionAttribute parseTransactionAnnotation(AnnotationAttributes attributes) {
    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    Propagation propagation = attributes.getEnum("propagation");
    rbta.setPropagationBehavior(propagation.value());
    Isolation isolation = attributes.getEnum("isolation");
    rbta.setIsolationLevel(isolation.value());
    rbta.setTimeout(attributes.getNumber("timeout").intValue());
    rbta.setReadOnly(attributes.getBoolean("readOnly"));
    rbta.setQualifier(attributes.getString("value"));
    //回滚规则
    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();
    for (Class<?> rbRule : attributes.getClassArray("rollbackFor")) {
        rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (String rbRule : attributes.getStringArray("rollbackForClassName")) {
        rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (Class<?> rbRule : attributes.getClassArray("noRollbackFor")) {
        rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    for (String rbRule : attributes.getStringArray("noRollbackForClassName")) {
        rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    rbta.setRollbackRules(rollbackRules);
    return rbta;
}
````

下面来看重点了事务拦截器。

## 6、TransactionInterceptor

负责拦截@Transaction方法的执行，在方法执行之前开启spring事务，方法执行完毕之后提交或者回滚事务。

在讲这个类的源码之前，先提几个问题，大家带着问题去看代码，理解更深一些。

1、事务管理器是如何获取的？

2、什么情况下事务会提交？

3、什么异常会导致事务回滚？

### 6.1、invokeWithinTransaction方法

这个方法是事务拦截器的入口，需要spring管理事务的业务方法会被这个方法拦截，大家可以设置断点跟踪一下

````java
protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
            final InvocationCallback invocation) throws Throwable {
    TransactionAttributeSource tas = getTransactionAttributeSource();
    //@6-1：获取事务属性配置信息：通过TransactionAttributeSource.getTransactionAttribute解析@Trasaction注解得到事务属性配置信息
    final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
    //@6-2：获取事务管理器
    final TransactionManager tm = determineTransactionManager(txAttr);
    //将事务管理器tx转换为 PlatformTransactionManager
    PlatformTransactionManager ptm = asPlatformTransactionManager(tm);
    if (txAttr == null || !(ptm instanceof CallbackPreferringPlatformTransactionManager)) {
        // createTransactionIfNecessary内部，这里就不说了，内部主要就是使用spring事务硬编码的方式开启事务，最终会返回一个TransactionInfo对象
        TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);
        // 业务方法返回值
        Object retVal;
        try {
            //调用aop中的下一个拦截器，最终会调用到业务目标方法，获取到目标方法的返回值
            retVal = invocation.proceedWithInvocation();
        }
        catch (Throwable ex) {
            //6-3：异常情况下，如何走？可能只需提交，也可能只需回滚，这个取决于事务的配置
            completeTransactionAfterThrowing(txInfo, ex);
            throw ex;
        }
        finally {
            //清理事务信息
            cleanupTransactionInfo(txInfo);
        }
        //6-4：业务方法返回之后，只需事务提交操作
        commitTransactionAfterReturning(txInfo);
        //返回执行结果
        return retVal;
    }
}
````

### 6.2、获取事务管理器

````java
//@6-2：获取事务管理器
final TransactionManager tm = determineTransactionManager(txAttr);
````

determineTransactionManager源码如下：

```
org.springframework.transaction.interceptor.TransactionAspectSupport#determineTransactionManager
protected TransactionManager determineTransactionManager(@Nullable TransactionAttribute txAttr) {
    // txAttr == null || this.beanFactory == null ，返回拦截器中配置的事务管理器
    if (txAttr == null || this.beanFactory == null) {
        return getTransactionManager();
    }
    //qualifier就是@Transactional注解中通过value或者transactionManager来指定事务管理器的bean名称
    String qualifier = txAttr.getQualifier();
    if (StringUtils.hasText(qualifier)) {
        //从spring容器中查找[beanName:qualifier,type:TransactionManager]的bean
        return determineQualifiedTransactionManager(this.beanFactory, qualifier);
    }
    else if (StringUtils.hasText(this.transactionManagerBeanName)) {
        //从spring容器中查找[beanName:this.transactionManagerBeanName,type:TransactionManager]的bean
        return determineQualifiedTransactionManager(this.beanFactory, this.transactionManagerBeanName);
    }
    else {
        //最后通过类型TransactionManager在spring容器中找事务管理器
        TransactionManager defaultTransactionManager = getTransactionManager();
        if (defaultTransactionManager == null) {
            defaultTransactionManager = this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
            if (defaultTransactionManager == null) {
                defaultTransactionManager = this.beanFactory.getBean(TransactionManager.class);
                this.transactionManagerCache.putIfAbsent(
                    DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
            }
        }
        return defaultTransactionManager;
    }
}
```

**从上面可知，事务管理器的查找顺序：**

1、先看@Transactional中是否通过value或者transactionManager指定了事务管理器

2、TransactionInterceptor.transactionManagerBeanName是否有值，如果有，将通过这个值查找事务管理器

3、如果上面2种都没有，将从spring容器中查找TransactionManager类型的事务管理器

### 6.3、异常情况下，如何走？

````java
try{
    //....
}catch (Throwable ex) {
    //6-3：异常情况下，如何走？可能只需提交，也可能只需回滚，这个取决于事务的配置
    completeTransactionAfterThrowing(txInfo, ex);
    throw ex;
}
````

源码中可以看出，发生异常了会进入`completeTransactionAfterThrowing`方法，completeTransactionAfterThrowing 源码如下

```java
protected void completeTransactionAfterThrowing(@Nullable TransactionInfo txInfo, Throwable ex) {
    if (txInfo != null && txInfo.getTransactionStatus() != null) {
        //@6-3-1：判断事务是否需要回滚
        if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
            //通过事务管理器回滚事务
            txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
        }
        else {
            //通过事务管理器提交事务
            txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
        }
    }
}
```

注意上面的`@6-3-1`代码，判断事务是否需要回滚，调用的是`transactionAttribute.rollbackOn(ex)`，最终会进入下面这个方法内部

````java
org.springframework.transaction.interceptor.RuleBasedTransactionAttribute#rollbackOn
public boolean rollbackOn(Throwable ex) {
    RollbackRuleAttribute winner = null;
    int deepest = Integer.MAX_VALUE;
    //@Trasaction中可以通过rollbackFor指定需要回滚的异常列表，通过noRollbackFor属性指定不需要回滚的异常
    //根据@Transactional中指定的回滚规则判断ex类型的异常是否需要回滚
    if (this.rollbackRules != null) {
        for (RollbackRuleAttribute rule : this.rollbackRules) {
            int depth = rule.getDepth(ex);
            if (depth >= 0 && depth < deepest) {
                deepest = depth;
                winner = rule;
            }
        }
    }
    //若@Transactional注解中没有匹配到，这走默认的规则，将通过super.rollbackOn来判断
    if (winner == null) {
        return super.rollbackOn(ex);
    }
    return !(winner instanceof NoRollbackRuleAttribute);
}
````

super.rollbackOn(ex)源码如下，**可以看出默认情况下，异常类型是RuntimeException或者Error的情况下，事务才会回滚**。

```
@Override
public boolean rollbackOn(Throwable ex) {
    return (ex instanceof RuntimeException || ex instanceof Error);
}
```

### 6.4、没有异常如何走？

```
//6-4：业务方法返回之后，只需事务提交操作
commitTransactionAfterReturning(txInfo);
```

没有异常的情况下会进入`commitTransactionAfterReturning`方法，`commitTransactionAfterReturning`源码如下，比较简单，就是调用事务管理器的commit方法提交事务

````java
protected void commitTransactionAfterReturning(@Nullable TransactionInfo txInfo) {
    if (txInfo != null && txInfo.getTransactionStatus() != null) {
        txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
    }
}
````

源码解析的差不多了，建议大家设置断点跟踪一下，加深对事务原理的理解。

## 7、重点回顾

1、使用@Transaction的时候，一定别忘记@EnableTransactionManagement注解，否则事务不起效

2、@Transaction的功能主要是通过aop来实现的，关键代码在TransactionInterceptor拦截器中

3、默认情况下，事务只会在 RuntimeException 或 Error 异常下回滚，可以通@Transaction来配置其他需要回滚或不需要回滚的异常类型

## 8、课堂讨论

下面代码的执行结果是什么？为什么？

````java
@Component
public class ServiceA {
    @Autowired
    ServiceB serviceB;
    @Transactional
    public void m1() {
        try {
            serviceB.m2();
        } catch (Exception e) {
            e.printStackTrace();
        }
        "insert into t_user (name) values ('张三')";
    }
}
@Component
public class ServiceB {
    @Transactional
    public void m2() {
        "insert into t_user (name) values ('李四')";
        throw new RuntimeException("手动抛出异常!");
    }
}
````



> 本文摘自： http://www.itsoku.com/course/12/130#4%E3%80%81@EnableTransactionManagement%E6%B3%A8%E8%A7%A3%E4%BD%9C%E7%94%A8
