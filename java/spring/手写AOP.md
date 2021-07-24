# 手写AOP

## 前言

学习了Spring的AOP后，总是想着AOP是怎么实现的，经过一番google、baidu后，尝试着自己写一个aop。



## 引入依赖

````xml
       <!-- https://mvnrepository.com/artifact/aopalliance/aopalliance -->
        <dependency>
            <groupId>aopalliance</groupId>
            <artifactId>aopalliance</artifactId>
            <version>1.0</version>
        </dependency>
        <!-- https://mvnrepository.com/artifact/org.aspectj/aspectjweaver -->
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
            <version>1.9.7</version>
        </dependency>

````



##  类介绍

| 类名              | 描述                                                         |
| ----------------- | ------------------------------------------------------------ |
| PointcutPrimitive | 切点术语，aspectj将程序分为了多个切点原语，比如方法的运行时为EXECUTION，方法被调用时为CALL。 |
| PointcutParser    | 切点表达式，我们知道aspectj将程序分为了多个切点原语，但是如果我们想要去标识一系列切点，就需要用到切点表达式，比如我们想要对一个类的所有方法，或者对对一个包的所有的类的所有方法等等。就需要用到切点表达式，例如"execution (* com.heng.aspectjlearn.bean.Account.*(..))" |
| MethodInterceptor | 故名思意，方法拦截器，来自org.aopalliance.intercept.MethodInterceptor。该类的作用是在源对象执行方法的时候进行拦截，同时用户需要自己实现invoke(MethodInvocation)方法来对源对象的方法进行修改。 |
| MethodInvocation  | 对一个方法的调用的描述，在方法调用时给予拦截器。方法调用是一个连接点，可以被一个方法拦截器拦截。该类与*MethodInterceptor*搭配着使用。 |



##  类的使用案例

### 判断一个类是否符合切点表达式

```java
package com.heng.aspectjlearn;

import com.heng.aspectjlearn.bean.Account;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class ApiTest {

    @Test
    public void test(){
        Class<?> clazz = Account.class;

        Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = new HashSet<PointcutPrimitive>();
        PointcutPrimitive pointcutPrimitive = PointcutPrimitive.EXECUTION;
        SUPPORTED_PRIMITIVES.add(pointcutPrimitive);
        String expression = "execution (* com.heng.aspectjlearn.bean.Account.*(..))";
        PointcutParser pointcutParser = PointcutParser.
                getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(SUPPORTED_PRIMITIVES,this.getClass().getClassLoader());

        PointcutExpression pointcutExpression = pointcutParser.parsePointcutExpression(expression);
        //选择的类是否符合表达式
        if (pointcutExpression.couldMatchJoinPointsInType(clazz)){
            System.out.println("类型匹配");
        }else {
            System.out.println("类型不匹配");
        }
    }
}

```

### 判断一个方法是否符合切点表达式

```java
package com.heng.aspectjlearn;

import com.heng.aspectjlearn.bean.Account;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class ApiTest {

    @Test
    public void test(){
        Class<?> clazz = Account.class;

        Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = new HashSet<PointcutPrimitive>();
        PointcutPrimitive pointcutPrimitive = PointcutPrimitive.EXECUTION;
        SUPPORTED_PRIMITIVES.add(pointcutPrimitive);
        String expression = "execution (* com.heng.aspectjlearn.bean.Account.*(..))";
        PointcutParser pointcutParser = PointcutParser.
                getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(SUPPORTED_PRIMITIVES,this.getClass().getClassLoader());

        PointcutExpression pointcutExpression = pointcutParser.parsePointcutExpression(expression);


        //执行的方法是否符合表达式
        Method[] methods = clazz.getMethods();
        for (Method method : methods){
            if (pointcutExpression.matchesMethodExecution(method).alwaysMatches()){
                System.out.println("方法匹配");
            }
        }
    }
}

```



### 基于JDK动态代理实现AOP



```java
package com.heng.aspectjlearn.bean;

public interface Pay {

    public boolean pay(int amount);
}
```



```java
package com.heng.aspectjlearn.bean;

public class Account implements Pay{

    public int balance = 20;

    public boolean pay(int amount){
        if (balance < amount){
            return false;
        }
        balance -= amount;
        System.out.println("hello");
        return true;
    }
}
```



```java
import com.heng.aspectjlearn.bean.Account;
import com.heng.aspectjlearn.bean.Pay;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;
import org.junit.Test;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

public class ApiTest {

    @Test
    public void test(){
        Class<?> clazz = Account.class;

        Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = new HashSet<PointcutPrimitive>();
        PointcutPrimitive pointcutPrimitive = PointcutPrimitive.EXECUTION;
        SUPPORTED_PRIMITIVES.add(pointcutPrimitive);
        String expression = "execution (* com.heng.aspectjlearn.bean.*.*(..))";
        PointcutParser pointcutParser = PointcutParser.
                getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(SUPPORTED_PRIMITIVES,this.getClass().getClassLoader());

        PointcutExpression pointcutExpression = pointcutParser.parsePointcutExpression(expression);


        Account object = new Account();
        InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                if (pointcutExpression.matchesMethodExecution(method).alwaysMatches()){
                    MethodInterceptor methodInterceptor = new MethodInterceptor() {
                        @Override
                        public Object invoke(MethodInvocation invocation) throws Throwable {
                            try {
                                return invocation.proceed();
                            }finally {
                                System.out.println("使用AOP代理");
                            }
                        }
                    };

                    MethodInvocation methodInvocation = new MethodInvocation() {
                        @Override
                        public Method getMethod() {
                            return method;
                        }

                        @Override
                        public Object[] getArguments() {
                            return args;
                        }

                        @Override
                        public Object proceed() throws Throwable {
                            return method.invoke(object,args);
                        }

                        @Override
                        public Object getThis() {
                            return object;
                        }

                        @Override
                        public AccessibleObject getStaticPart() {
                            return method;
                        }
                    };
                    return methodInterceptor.invoke(methodInvocation);
                }
                return method.invoke(object,args);
            }
        };


        Pay pay = (Pay) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),object.getClass().getInterfaces(),invocationHandler);
        pay.pay(1);
    }
}
```



### 基于Cglib动态代理实现AOP

**引入依赖**

```java
<!-- https://mvnrepository.com/artifact/cglib/cglib -->
<dependency>
    <groupId>cglib</groupId>
    <artifactId>cglib</artifactId>
    <version>2.2.2</version>
</dependency>
```



**使用Cglib生成代理类**

Say.class

```
public interface Say {

    String say();
}
```

Person.class

```java
public class Person implements Say{

    @Override
    public String say() {
        return "hello man";
    }
}
```

Cglib生成代理类

类介绍

| 类名        | 描述                                                         |
| ----------- | ------------------------------------------------------------ |
| MethodProxy | 当调用拦截的方法时， Enhancer生成的类将此对象传递给注册的MethodInterceptor对象。 它可用于调用原始方法，或在相同类型的不同对象上调用相同方法。 |



````java
import com.heng.service.Person;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;

public class Test {




    @org.junit.Test
    public void cglibTest(){
        Person person = new Person();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Person.class);
        enhancer.setInterfaces(Person.class.getInterfaces());
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                System.out.println("生成代理类");
                return proxy.invoke(person,args);
            }
        });
        Person personProxy = (Person) enhancer.create();
        System.out.println(personProxy.say());
    }
}
````

Cglib实现AOP

```
import com.heng.service.Person;
import com.heng.service.Say;
import com.heng.test.aop.AdvisedSupport;
import com.heng.test.handler.MyMethodInterceptor;
import com.heng.test.invocation.JdkDynamicAopProxy;
import com.heng.test.pointcut.PointCutSupport;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

public class Test {
    @org.junit.Test
    public void cglibTest(){
        Person person = new Person();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Person.class);
        enhancer.setInterfaces(Person.class.getInterfaces());
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                MethodInvocation methodInvocation = new MethodInvocation() {
                    @Override
                    public Method getMethod() {
                        return method;
                    }

                    @Override
                    public Object[] getArguments() {
                        return args;
                    }

                    @Override
                    public Object proceed() throws Throwable {
                        return method.invoke(person,args);
                    }

                    @Override
                    public Object getThis() {
                        return person;
                    }

                    @Override
                    public AccessibleObject getStaticPart() {
                        return method;
                    }
                };
                org.aopalliance.intercept.MethodInterceptor methodInterceptor = new org.aopalliance.intercept.MethodInterceptor() {
                    @Override
                    public Object invoke(MethodInvocation invocation) throws Throwable {
                        try {
                            System.out.println("方法调用前");
                            return invocation.proceed();
                        }finally {
                            System.out.println("方法调用后");
                        }
                    };
                };
                return methodInterceptor.invoke(methodInvocation);
            }
        });
        Person personProxy = (Person) enhancer.create();
        System.out.println(personProxy.say());
    }
}
```
