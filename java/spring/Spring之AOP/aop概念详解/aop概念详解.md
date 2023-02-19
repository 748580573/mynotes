## 什么是AOP？

先看一下传统程序的流程，比如银行系统会有一个取款流程

![](./imgs/1.png)

我们可以把方框里的流程合为一个，另外系统还会有一个查询余额流程，我们先把这两个流程放到一起：

![](./imgs/2.png)

有没有发现，这个两者有一个相同的验证流程，我们先把它们圈起来再说下一步：

![](./imgs/3.png)

上面只是2个操作，如果有更多的操作，验证用户的功能是不是需要写很多次？

有没有想过可以把这个验证用户的代码是提取出来，不放到主流程里去呢，这就是AOP的作用了，有了AOP，你写代码时不要把这个验证用户步骤写进去，即完全不考虑验证用户，你写完之后，在另我一个地方，写好验证用户的代码，然后告诉Spring你要把这段代码加到哪几个地方，Spring就会帮你加过去，而不要你自己Copy过去，如果你有多个控制流呢，这个写代码的方法可以大大减少你的时间。

再举一个通用的例子，经常在debug的时候要打log吧，你也可以写好主要代码之后，把打log的代码写到另一个单独的地方，然后命令AOP把你的代码加过去，注意AOP不会把代码加到源文件里，但是它会正确的影响最终的机器代码。

现在大概明白了AOP了吗，我们来理一下头绪，上面那个方框像不像个平面，你可以把它当块板子，这块板子插入一些控制流程，这块板子就可以当成是AOP中的一个切面。所以AOP的本质是在一系列纵向的控制流程中，把那些相同的子流程提取成一个横向的面，这句话应该好理解吧，我们把纵向流程画成一条直线，然把相同的部分以绿色突出，如下图左，而AOP相当于把相同的地方连一条横线，如下图右，这个图没画好，大家明白意思就行。

![](./imgs/4.png)

这个验证用户这个子流程就成了一个条线，也可以理解成一个切面，这里的切面只插了两三个流程，如果其它流程也需要这个子流程，也可以插到其它地方去。

**通熟易懂的理解为：在程序中具有公共特性的某些类/某些方法上进行拦截, 在方法执行的前面/后面/执行结果返回后增加执行一些方法。**

## 先来考虑几个问题

1. aop中用什么来表示这些公共的功能？
2. aop中如何知道这些公共的功能用到哪些类的那些方法中去？
3. aop中需要将这些公共的功能用在目标方法的什么地方，前面？后面？还是其他什么地方？
4. aop底层是用什么实现的？

spring中有些概念，不是太好理解，带着这些问题，理解起来会容易很多，概念理解了，后面的路会容易很多，下面我们先来理解概念。

## Spring中AOP一些概念

### 目标对象(target)

目标对象指将要被增强的对象，即包含主业务逻辑的类对象。

### 连接点(JoinPoint)

程序执行过程中明确的点，如方法的调用或特定的异常被抛出。

连接点由两个信息确定：

- 方法(表示程序执行点，即在哪个目标方法)
- 相对点(表示方位，即目标方法的什么位置，比如调用前，后等)

简单来说，连接点就是被拦截到的程序执行点，因为Spring只支持方法类型的连接点，所以在Spring中连接点就是被拦截到的方法。

### 代理对象(Proxy)

AOP中会通过代理的方式，对目标对象生成一个代理对象，代理对象中会加入需要增强功能，通过代理对象来间接的方式目标对象，起到增强目标对象的效果。

### 通知(Advice)

需要在目标对象中增强的功能，如上面说的：业务方法前验证用户的功能、方法执行之后打印方法的执行日志。

通知中有2个重要的信息：**方法的什么地方**，**执行什么操作**，这2个信息通过通知来指定。

方法的什么地方？之前、之后、包裹目标方法、方法抛出异常后等。

如：

在方法执行之前验证用户是否有效。

在方法执行之后，打印方法的执行耗时。

在方法抛出异常后，记录异常信息发送到mq。

### 切入点(Pointcut )

用来指定需要将通知使用到哪些地方，比如需要用在哪些类的哪些方法上，切入点就是做这个配置的。

### 切面（Aspect）

通知（Advice）和切入点（Pointcut）的组合。切面来定义在哪些地方（Pointcut）执行什么操作（Advice）。

### 顾问（Advisor)

Advisor 其实它就是 Pointcut 与 Advice 的组合，Advice 是要增强的逻辑，而增强的逻辑要在什么地方执行是通过Pointcut来指定的，所以 Advice 必需与 Pointcut 组合在一起，这就诞生了 Advisor 这个类，spring Aop中提供了一个Advisor接口将Pointcut 与 Advice 的组合起来。

Advisor有好几个称呼：顾问、通知器。

其中这4个：连接点(JoinPoint)、通知(advise)、切入点(pointcut)、顾问（advisor)，在spring中都定义了接口和类来表示这些对象，下面我们一个个来看一下。

## 连接点（JoinPoint）

### JoinPoint接口

```java
package org.aopalliance.intercept;

public interface Joinpoint {

    /**
     * 转到拦截器链中的下一个拦截器
     */
    Object proceed() throws Throwable;

    /**
     * 返回保存当前连接点静态部分【的对象】，这里一般指被代理的目标对象
     */
    Object getThis();

    /**
     * 返回此静态连接点  一般就为当前的Method(至少目前的唯一实现是MethodInvocation,所以连接点得静态部分肯定就是本方法)
     */
    AccessibleObject getStaticPart();

}
```

几个重要的子接口和实现类，如下：

![](./imgs/5.png)

### Invocation接口

```java
package org.aopalliance.intercept;

/**
 * 此接口表示程序中的调用
 * 调用是一个连接点，可以被拦截器拦截。
 */
public interface Invocation extends Joinpoint {

    /**
     * 将参数作为数组对象获取。可以更改此数组中的元素值以更改参数。
     * 通常用来获取调用目标方法的参数
     */
    Object[] getArguments();

}
```

### MethodInvocation接口

```java
package org.aopalliance.intercept;

import java.lang.reflect.Method;

/**
 * 方法调用的描述，在方法调用时提供给拦截器。
 * 方法调用是一个连接点，可以被方法拦截器拦截。
 */
public interface MethodInvocation extends Invocation {

    /**
     * 返回正在被调用得方法~~~  返回的是当前Method对象。
     * 此时，效果同父类的AccessibleObject getStaticPart() 这个方法
     */
    Method getMethod();

}
```

## 通知(Advice)

通知中用来实现被增强的逻辑，通知中有2个关注点，再强调一下：方法的什么地方，执行什么操作。

### Advice接口

通知的顶层接口，这个接口内部没有定义任何方法。

```java
package org.aopalliance.aop;

public interface Advice {
}
```

### Advice 4个子接口

![](./imgs/6.png)

### MethodBeforeAdvice接口

方法执行前通知，需要在目标方法执行前执行一些逻辑的，可以通过这个实现。

通俗点说：需要在目标方法执行之前增强一些逻辑，可以通过这个接口来实现。before方法：在调用给定方法之前回调。

```java
package org.springframework.aop;

public interface MethodBeforeAdvice extends BeforeAdvice {

    /**
     * 调用目标方法之前会先调用这个before方法
     * method：需要执行的目标方法
     * args：目标方法的参数
     * target：目标对象
     */
    void before(Method method, Object[] args, @Nullable Object target) throws Throwable;
}
```

如同

```java
public Object invoke(){
    调用MethodBeforeAdvice#before方法
    return 调用目标方法;
}
```

### AfterReturningAdvice接口

方法执行后通知，需要在目标方法执行之后执行增强一些逻辑的，可以通过这个实现。

**不过需要注意一点：目标方法正常执行后，才会回调这个接口，当目标方法有异常，那么这通知会被跳过。**

```java
package org.springframework.aop;

public interface AfterReturningAdvice extends AfterAdvice {

    /**
     * 目标方法执行之后会回调这个方法
     * method：需要执行的目标方法
     * args：目标方法的参数
     * target：目标对象
     */
    void afterReturning(@Nullable Object returnValue, Method method, Object[] args, @Nullable Object target) throws Throwable;

}
```

如同

```java
public Object invoke(){
    Object retVal = 调用目标方法;
    调用AfterReturningAdvice#afterReturning方法
    return retVal;
}
```

### ThrowsAdvice接口

```java
package org.springframework.aop;

public interface ThrowsAdvice extends AfterAdvice {

}
```

此接口上没有任何方法，因为方法由反射调用，实现类必须实现以下形式的方法，前3个参数是可选的，最后一个参数为需要匹配的异常的类型。

```jva
void afterThrowing([Method, args, target], ThrowableSubclass);
```

有效方法的一些例子如下：

```java
public void afterThrowing(Exception ex)
public void afterThrowing(RemoteException)
public void afterThrowing(Method method, Object[] args, Object target, Exception ex)
public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)
```

### MethodInterceptor接口

方法拦截器，这个接口最强大，可以实现上面3种类型的通知，上面3种通知最终都通过适配模式将其转换为MethodInterceptor方式去执行。

```java
package org.aopalliance.intercept;

@FunctionalInterface
public interface MethodInterceptor extends Interceptor {

    /**
     * 拦截目标方法的执行，可以在这个方法内部实现需要增强的逻辑，以及主动调用目标方法
     */
    Object invoke(MethodInvocation invocation) throws Throwable;

}
```

使用方式如：

```java
public class TracingInterceptor implements MethodInterceptor {
    Object invoke(MethodInvocation i) throws Throwable {
        System.out.println("method "+i.getMethod()+" is called on "+ i.getThis()+" with args "+i.getArguments());
        Object ret=i.proceed();//转到拦截器链中的下一个拦截器
        System.out.println("method "+i.getMethod()+" returns "+ret);
        return ret;
    }
}
```

### 拦截器链

一个目标方法中可以添加很多Advice，这些Advice最终都会被转换为`MethodInterceptor`类型的方法拦截器，最终会有多个`MethodInterceptor`，这些`MethodInterceptor`会组成一个方法调用链。

Aop内部会给目标对象创建一个代理，代理对象中会放入这些`MethodInterceptor`会组成一个方法调用链，当调用代理对象的方法的时候，会按顺序执行这些方法调用链，一个个执行，最后会通过反射再去调用目标方法，进而对目标方法进行增强。

![](./imgs/7.png)

## 切入点(PointCut)

通知（Advice）用来指定需要增强的逻辑，但是哪些类的哪些方法中需要使用这些通知呢？这个就是通过切入点来配置的，切入点在spring中对应了一个接口

### PointCut接口

```java
package org.springframework.aop;

public interface Pointcut {

    /**
     * 类过滤器, 可以知道哪些类需要拦截
     */
    ClassFilter getClassFilter();

    /**
     * 方法匹配器, 可以知道哪些方法需要拦截
     */
    MethodMatcher getMethodMatcher();

    /**
     * 匹配所有对象的 Pointcut，内部的2个过滤器默认都会返回true
     */
    Pointcut TRUE = TruePointcut.INSTANCE;

}
```

### ClassFilter接口

比较简单，用来过滤类的

```java
@FunctionalInterface
public interface ClassFilter {

    /**
     * 用来判断目标类型是否匹配
     */
    boolean matches(Class<?> clazz);

}
```

### MethodMatcher接口

用来过滤方法的。

```java
public interface MethodMatcher {

    /**
     * 执行静态检查给定方法是否匹配
     * @param method 目标方法
     * @param targetClass 目标对象类型
     */
    boolean matches(Method method, Class<?> targetClass);

    /**
     * 是否是动态匹配，即是否每次执行目标方法的时候都去验证一下
     */
    boolean isRuntime();

    /**
     * 动态匹配验证的方法，比第一个matches方法多了一个参数args，这个参数是调用目标方法传入的参数
     */
    boolean matches(Method method, Class<?> targetClass, Object... args);


    /**
     * 匹配所有方法，这个内部的2个matches方法任何时候都返回true
     */
    MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

}
```

我估计大家看MethodMatcher还是有点晕的，为什么需要2个maches方法？什么是动态匹配？

比如下面一个类

```java
public class UserService{
    public void work(String userName){
        System.out.print(userName+",开始工作了!");
    }
}
```

work方法表示当前用户的工作方法，内部可以实现一些工作的逻辑。

我们希望通过aop对这个类进行增强，调用这个方法的时候，当传入的用户名是`路人的粉丝的`的时候，需要先进行问候，其他用户的时候，无需问候，将这个问题的代码可以放在MethodBeforeAdvice中实现，这种情况就是当参数满足一定的条件了，才会使用这个通知，不满足的时候，通知无效，此时就可以使用上面的动态匹配来实现，MethodMatcher类中3个参数的matches方法可以用来对目标方法的参数做校验。

来看一下`MethodMatcher`过滤的整个过程

```java
1.调用matches(Method method, Class<?> targetClass)方法，验证方法是否匹配
2.isRuntime方法是否为true，如果为false，则以第一步的结果为准，否则继续向下
3.调用matches(Method method, Class<?> targetClass, Object... args)方法继续验证，这个方法多了一个参数，可以对目标方法传入的参数进行校验。
```

通过上面的过程，大家可以看出来，如果`isRuntime`为false的时候，只需要对方法名称进行校验，当目标方法调用多次的时候，实际上第一步的验证结果是一样的，所以如果`isRuntime`为false的情况，可以将验证结果放在缓存中，提升效率，而spring内部就是这么做的，`isRuntime`为false的时候，需要每次都进行校验，效率会低一些，不过对性能的影响基本上可以忽略。

## 顾问(Advisor)

通知定义了需要做什么，切入点定义了在哪些类的哪些方法中执行通知，那么需要将他们2个组合起来才有效啊。

顾问（Advisor）就是做这个事情的。

### Advisor接口

```java
package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * 包含AOP通知（在joinpoint处执行的操作）和确定通知适用性的过滤器（如切入点[PointCut]）的基本接口。
 * 这个接口不是供Spring用户使用的，而是为了支持不同类型的建议的通用性。
 */
public interface Advisor {
    /**
     * 返回引用的通知
     */
    Advice getAdvice();

}
```

上面这个接口通常不会直接使用，这个接口有2个子接口，通常我们会和这2个子接口来打交道，下面看一下这2个子接口。

![](./imgs/8.png)

### PointcutAdvisor接口

通过名字就能看出来，这个和Pointcut有关，内部有个方法用来获取`Pointcut`，AOP使用到的大部分Advisor都属于这种类型的。

在目标方法中实现各种增强功能基本上都是通过PointcutAdvisor来实现的。

```java
package org.springframework.aop;

/**
 * 切入点类型的Advisor
 */
public interface PointcutAdvisor extends Advisor {

    /**
     * 获取顾问中使用的切入点
     */
    Pointcut getPointcut();

}
```

### IntroductionAdvisor接口

这个接口，估计大家比较陌生，干什么的呢？

**一个Java类，没有实现A接口，在不修改Java类的情况下，使其具备A接口的功能。可以通过IntroductionAdvisor给目标类引入更多接口的功能，这个功能是不是非常牛逼。**

## 案例

上面都是一些概念，看起来比较枯燥乏味，下面来个使用硬编码的方式来用一下上面提到的一些类或者接口，加深理解。

### 来个类

```java
package com.javacode2018.aop.demo3;

public class UserService {
    public void work(String userName) {
        System.out.println(userName + ",正在和路人甲java一起学Spring Aop，欢迎大家一起来！");
    }
}
```

下面通过aop来实现一些需求，对work方法进行增强。

### 案例1

需求：在work方法执行之前，打印一句：你好：userName

下面直接上代码，注释比较详细，就不细说了。

```java
@Test
public void test1() {
    //定义目标对象
    UserService target = new UserService();
    //创建pointcut，用来拦截UserService中的work方法
    Pointcut pointcut = new Pointcut() {
        @Override
        public ClassFilter getClassFilter() {
            //判断是否是UserService类型的
            return clazz -> UserService.class.isAssignableFrom(clazz);
        }

        @Override
        public MethodMatcher getMethodMatcher() {
            return new MethodMatcher() {
                @Override
                public boolean matches(Method method, Class<?> targetClass) {
                    //判断方法名称是否是work
                    return "work".equals(method.getName());
                }

                @Override
                public boolean isRuntime() {
                    return false;
                }

                @Override
                public boolean matches(Method method, Class<?> targetClass, Object... args) {
                    return false;
                }
            };
        }
    };
    //创建通知，此处需要在方法之前执行操作，所以需要用到MethodBeforeAdvice类型的通知
    MethodBeforeAdvice advice = (method, args, target1) -> System.out.println("你好:" + args[0]);

    //创建Advisor，将pointcut和advice组装起来
    DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, advice);

    //通过spring提供的代理创建工厂来创建代理
    ProxyFactory proxyFactory = new ProxyFactory();
    //为工厂指定目标对象
    proxyFactory.setTarget(target);
    //调用addAdvisor方法，为目标添加增强的功能，即添加Advisor，可以为目标添加很多个Advisor
    proxyFactory.addAdvisor(advisor);
    //通过工厂提供的方法来生成代理对象
    UserService userServiceProxy = (UserService) proxyFactory.getProxy();

    //调用代理的work方法
    userServiceProxy.work("路人");
}
```

运行输出

```java
你好:路人
路人,正在和路人甲java一起学Spring Aop，欢迎大家一起来！
```

上面是采用硬编码的方式来感受一下aop的用法，大家看了上面代码之后，估计会有疑问：我晕，这么复杂？？？

如果大家有使用过spring中的aop经验，可能只需要几行代码就实现了上面的功能，的确，spring中把整个功能简化了很多，不过我们得去了解他的内部是如何实现的，然后才能走的更远。

### 案例2

需求：统计一下work方法的耗时，将耗时输出

```java
@Test
public void test2() {
    //定义目标对象
    UserService target = new UserService();
    //创建pointcut，用来拦截UserService中的work方法
    Pointcut pointcut = new Pointcut() {
        @Override
        public ClassFilter getClassFilter() {
            //判断是否是UserService类型的
            return clazz -> UserService.class.isAssignableFrom(clazz);
        }

        @Override
        public MethodMatcher getMethodMatcher() {
            return new MethodMatcher() {
                @Override
                public boolean matches(Method method, Class<?> targetClass) {
                    //判断方法名称是否是work
                    return "work".equals(method.getName());
                }

                @Override
                public boolean isRuntime() {
                    return false;
                }

                @Override
                public boolean matches(Method method, Class<?> targetClass, Object... args) {
                    return false;
                }
            };
        }
    };
    //创建通知，需要拦截方法的执行，所以需要用到MethodInterceptor类型的通知
    MethodInterceptor advice = new MethodInterceptor() {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            System.out.println("准备调用:" + invocation.getMethod());
            long starTime = System.nanoTime();
            Object result = invocation.proceed();
            long endTime = System.nanoTime();
            System.out.println(invocation.getMethod() + "，调用结束！");
            System.out.println("耗时(纳秒):" + (endTime - starTime));
            return result;
        }
    };

    //创建Advisor，将pointcut和advice组装起来
    DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, advice);

    //通过spring提供的代理创建工厂来创建代理
    ProxyFactory proxyFactory = new ProxyFactory();
    //为工厂指定目标对象
    proxyFactory.setTarget(target);
    //调用addAdvisor方法，为目标添加增强的功能，即添加Advisor，可以为目标添加很多个Advisor
    proxyFactory.addAdvisor(advisor);
    //通过工厂提供的方法来生成代理对象
    UserService userServiceProxy = (UserService) proxyFactory.getProxy();

    //调用代理的work方法
    userServiceProxy.work("路人");
}
```

运行输出

```java
准备调用:public void com.javacode2018.aop.demo3.UserService.work(java.lang.String)
路人,正在和路人甲java一起学Spring Aop，欢迎大家一起来！
public void com.javacode2018.aop.demo3.UserService.work(java.lang.String)，调用结束！
耗时(纳秒):9526200
```

### 案例3

需求：userName中包含“粉丝”关键字，输出一句：感谢您一路的支持

此处需要用到 MethodMatcher 中的动态匹配了，通过参数来进行判断。

重点在于Pointcut中的getMethodMatcher方法，返回的MethodMatcher，`@1`必须返回true，此时才会进入到`@2`中对参数进行校验。

代码如下：

```java
@Test
public void test2() {
    //定义目标对象
    UserService target = new UserService();
    //创建pointcut，用来拦截UserService中的work方法
    Pointcut pointcut = new Pointcut() {
        @Override
        public ClassFilter getClassFilter() {
            //判断是否是UserService类型的
            return clazz -> UserService.class.isAssignableFrom(clazz);
        }

        @Override
        public MethodMatcher getMethodMatcher() {
            return new MethodMatcher() {
                @Override
                public boolean matches(Method method, Class<?> targetClass) {
                    //判断方法名称是否是work
                    return "work".equals(method.getName());
                }

                @Override
                public boolean isRuntime() {
                    return true; // @1：注意这个地方要返回true
                }

                @Override
                public boolean matches(Method method, Class<?> targetClass, Object... args) {
                    // @2：isRuntime为true的时候，会执行这个方法
                    if (Objects.nonNull(args) && args.length == 1) {
                        String userName = (String) args[0];
                        return userName.contains("粉丝");
                    }
                    return false;
                }
            };
        }
    };
    //创建通知，此处需要在方法之前执行操作，所以需要用到MethodBeforeAdvice类型的通知
    MethodBeforeAdvice advice = (method, args, target1) -> System.out.println("感谢您一路的支持!");

    //创建Advisor，将pointcut和advice组装起来
    DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, advice);

    //通过spring提供的代理创建工厂来创建代理
    ProxyFactory proxyFactory = new ProxyFactory();
    //为工厂指定目标对象
    proxyFactory.setTarget(target);
    //调用addAdvisor方法，为目标添加增强的功能，即添加Advisor，可以为目标添加很多个Advisor
    proxyFactory.addAdvisor(advisor);
    //通过工厂提供的方法来生成代理对象
    UserService userServiceProxy = (UserService) proxyFactory.getProxy();

    //调用代理的work方法
    userServiceProxy.work("粉丝：A");
}
```

运行输出

```java
感谢您一路的支持!
粉丝：A,正在和路人甲java一起学Spring Aop，欢迎大家一起来！
```

### 本文案例代码入口

```java
com.javacode2018.aop.demo3.Test3
```

**上面的一些案例中都用到了`ProxyFactory`这个类，内部将各种对象进行组装，然后创建代理对象，`ProxyFactory`这块关联的的东西挺多的，下一篇文章将详说这块的东西，是非常重要的内容。**



> 本文摘自： https://mp.weixin.qq.com/s?__biz=MzA5MTkxMDQ4MQ==&mid=2648934876&idx=1&sn=7794b50e658e0ec3e0aff6cf5ed4aa2e&chksm=886211e2bf1598f4e0e636170a4b36a5a5edd8811c8b7c30d61135cb114b0ce506a6fa84df0b&token=690771459&lang=zh_CN&scene=21#wechat_redirect