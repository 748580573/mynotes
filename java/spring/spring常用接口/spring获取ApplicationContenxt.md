 在服务器启动过程中，spring容器也会初始化，那么当然，Spring的ApplicationContext对象肯定也在这个过程中会被初始化了，那么我们如何在java类中手动去获取得到这个对象呢？

  首先， 我们为什么要去获取这个ApplicationContext对象？，获取到了我们能干什么呢？ -- 能手动从Spring获取所需要的bean

```java
// 获取bean 方法1
public static <T>T getBean(String beanName){
	return (T) applicationContext.getBean(beanName);
}
// 获取bean 方法2
public static <T> T getBean(Class<T> c){
	return (T) applicationContext.getBean(c);
}
```

其次，怎样获得到这个ApplicationContext对象？

1、手动创建ApplicationContext对象：

```java
private LsjmUserServiceImpl user = null;
@Before
public void getBefore(){
        // 这里由于我的配置文件写的目录不是根目录，所以用FileSystemXmlApplicationContext
	String xmlPath = "WebContent/WEB-INF/config/base/applicationContext.xml";
	ApplicationContext ac = new FileSystemXmlApplicationContext(xmlPath);
	user = ac.getBean(LsjmUserServiceImpl.class);
}
// 如果是放在根目录下
public void getBefore(){
	ApplicationContext ac = new ClassPathXmlApplicationContext("applicationContext.xml");
	user = ac.getBean(LsjmUserServiceImpl.class);
}
```

  这种获取方法一般用在写junit测试的时候， 实际项目中强烈不推荐使用，因为Spring容器在服务器加载的时候，就已经被初始化了，这里相当于又重新加载了一次，没有必要而且耗资源，并且非常慢

 

2、通过Spring的工具类WebApplicationContextUtils 获取、

```java
// Spring中获取ServletContext对象，普通类中可以这样获取
ServletContext sc = ContextLoader.getCurrentWebApplicationContext().getServletContext();
// servlet中可以这样获取,方法比较多
ServletContext sc = request.getServletContext():
ServletContext sc = servletConfig.getServletContext();  //servletConfig可以在servlet的init(ServletConfig config)方法中获取得到
/* 需要传入一个参数ServletContext对象, 获取方法在上面 */
// 这种方法 获取失败时抛出异常
ApplicationContext ac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
// 这种方法 获取失败时返回null
ApplicationContext ac = WebApplicationContextUtils.getWebApplicationContext(sc);
```

 

3、写一个工具类 比如BaseHolder 实现ApplicationContextAware接口

```java
package com.chaol.system;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
public class BaseHolder implements ApplicationContextAware {
	private static ApplicationContext applicationContext;
	/**
	 * 服务器启动，Spring容器初始化时，当加载了当前类为bean组件后，
	 * 将会调用下面方法注入ApplicationContext实例
	 */
	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		System.out.println("初始化了");
		BaseHolder.applicationContext = arg0;
	}
	public static ApplicationContext getApplicationContext(){
		return applicationContext;
	}
	/**
         * 外部调用这个getBean方法就可以手动获取到bean
	 * 用bean组件的name来获取bean
	 * @param beanName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T>T getBean(String beanName){
		return (T) applicationContext.getBean(beanName);
	}
}
```

 注意：写完了工具类，还需要在applicationContext.xml中配置 一行，让Spring进行对象管理：

<bean id="baseHolder" class="com.chaol.system.BaseHolder" lazy-init="false" />  或者也可以直接用注解的方式，在类上注解@Component；

 

4、

  写一个工具类继承抽象类ApplicationObjectSupport，

  并在工具类上使用@Component交由spring管理。

  spring容器启动时，会通过父类ApplicationObjectSupport中的setApplicationContext()方法将ApplicationContext注入

  通过getApplicationContext()得到ApplicationContext对象。

（第4种方法是从别处拷贝，做了一下分行，便于层次理解；此方法并未验证，但应该不会有问题，但是因为java的单继承原理，所以在项目中这种方法可以考虑，但是推荐指数没有方法3 高）

 

5、工具类继承抽象类WebApplicationObjectSupport，

   查看源码可知WebApplicationObjectSupport是继承了ApplicationObjectSupport

   获取ApplicationContext对象的方式和上面一样，也是使用getApplicationContext()方法。（同上）

 

总结一下：

1、写junit测试用例使用而且只能使用方法1； 自己可以测试一下，使用后面的几种方法在junit中获取ApplicationContext对象时，无论怎么样获取到的bean都会报NullPointerException。 因为junit并不会走web.xml去加载各种各样的配置

2、实际项目推荐使用方法3，个人爱好，，

贴一下Demo的部分核心代码：

servlet类内容：

```java
package com.chaol.controller;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.chaol.service.LsjmUserService;
import com.chaol.system.BaseHolder;
import com.chaol.vo.LsjmUser;
public class UserServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	LsjmUserService userService;
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		userService = BaseHolder.getBean(LsjmUserService.class);
		System.out.println(userService.getUser());
		System.out.println(req.getServletContext().getContextPath());  //上下文路径
		req.getRequestDispatcher("/servlet/test.jsp").forward(req, resp);
	}
	/*@Override
	public void init(ServletConfig config) throws ServletException {
		ApplicationContext ac = WebApplicationContextUtils.
				getRequiredWebApplicationContext(config.getServletContext());
		userService = ac.getBean(LsjmUserService.class);
		super.init(config);
		//userService = getBean(LsjmUserService.class);
	}*/
	public <T>T getBean(Class<T> c){
		ServletContext sc = ContextLoader.getCurrentWebApplicationContext().getServletContext();
		ApplicationContext ac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
		return (T) ac.getBean(c);
	}
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);
	}
}
```

用方法三得到ApplicationContext对象的工具类：

```java
package com.chaol.system;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
public class BaseHolder implements ApplicationContextAware {
	private static ApplicationContext applicationContext;
	/**
	 * 服务器启动，Spring容器初始化时，当加载了当前类为bean组件后，
	 * 将会调用下面方法注入ApplicationContext实例
	 */
	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		System.out.println("初始化了");
		BaseHolder.applicationContext = arg0;
	}
	public static ApplicationContext getApplicationContext(){
		return applicationContext;
	} 
	/**
	 * 用bean组件的name来获取bean
	 * @param beanName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T>T getBean(String beanName){
		return (T) applicationContext.getBean(beanName);
	}
	/**
	 * 用类来获取bean
	 * @param c
	 * @return
	 */
	public static <T> T getBean(Class<T> c){
		return (T) applicationContext.getBean(c);
	}
}
```

applicationContext.xml 配置

```bash
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"  
	xmlns:context="http://www.springframework.org/schema/context"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">
 	<!-- 自动扫描开启 -->
 	<context:component-scan base-package="com.chaol.service" />
 	<!-- 配置数据源， 简化版，那些个连接池数量啥的这里没有配了 -->
 	<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
 		<property name="driverClassName" value="com.mysql.jdbc.Driver" />
 		<property name="url" value="jdbc:mysql://localhost:3306/lsjm?useUnicode=true&amp;characterEncoding=utf8" />
 		<property name="username" value="admin" />
 		<property name="password" value="admin" />
 	</bean>
 	<!-- 配置会话工厂bean -->
 	<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
 		<!-- 数据源 -->
 		<property name="dataSource" ref="dataSource" />
 		<!-- 给实体类区别名，简化映射文件中的返回类型resultType书写 -->
 		<property name="typeAliasesPackage" value="com.chaol.vo" />
 		<!-- 指定sql映射文件路径 -->
                <property name="mapperLocations" value="classpath*:com/chaol/mapper/*Mapper.xml" />
 	</bean>
 	<!-- 配置自动扫描对象关系映射接口 -->
 	<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
 		<!-- 指定会话工厂，如配置中只有一个则可省去 -->
 		<property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
 		<!-- 指定映射接口目录 -->
 		<property name="basePackage" value="com.chaol.mapper" />
 	</bean>
 	<bean id="baseHolder" class="com.chaol.system.BaseHolder" lazy-init="false" />
 </beans>
```



总结重发一下：

1、写junit测试用例使用而且只能使用方法1； 自己可以测试一下，使用后面的几种方法在junit中获取ApplicationContext对象时，无论怎么样获取到的bean都会报NullPointerException。 因为junit并不会走web.xml去加载各种各样的配置

2、实际项目推荐使用方法3，个人爱好，