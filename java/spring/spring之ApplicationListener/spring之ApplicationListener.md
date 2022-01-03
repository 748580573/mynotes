一.需求是想将我的写一个方法能在项目启动后就运行，之前使用了redis的消息监听器，感觉可以照着监听器这个思路做，于是想到了sringboot的监听器

二.目前spring boot中支持的事件类型如下

1. ApplicationFailedEvent：该事件为spring boot启动失败时的操作
2. ApplicationPreparedEvent：上下文context准备时触发
3. ApplicationReadyEvent：上下文已经准备完毕的时候触发
4. ApplicationStartedEvent：spring boot 启动监听类
5. SpringApplicationEvent：获取SpringApplication
6. ApplicationEnvironmentPreparedEvent：环境事先准备

三.监听器的使用

第一：首先定义一个自己使用的监听器类并实现ApplicationListener接口。

```java
@Componenpublic class MessageReceiver implements ApplicationListener<ApplicationReadyEvent> {
    private Logger logger = LoggerFactory.getLogger(MessageReceiver.class);
    
    private UserService userService = null;
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ConfigurableApplicationContext applicationContext = event.getApplicationContext();
　　　　　//解决userService一直为空
　　　　 userService = applicationContext.getBean(UserService.class); 　　　　 System.out.println("name"+userService.getName());
    }
}
```

第二：通过SpringApplication类中的addListeners方法将自定义的监听器注册进去

```java
public class Application {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.addListeners(new MessageReceiver());
        application.run(args);
    
    }
}
```

如果实现不知道哪种处理器在什么时候使用，就可以照在下面的测试代码进行测试

```java
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

@Component
public class AppApplicationListener implements ApplicationListener {


    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextStartedEvent){
            System.out.println("================:{}"+ "ContextStartedEvent");
        }

        if (event instanceof ApplicationPreparedEvent){
            System.out.println("================:{}"+ "ApplicationPreparedEvent");
        }
        if (event instanceof ContextRefreshedEvent){
            System.out.println("================:{}"+ "ContextRefreshedEvent");
        }
        if (event instanceof ContextClosedEvent){
            System.out.println("================:{}"+ "ContextClosedEvent");
        }
        if (event instanceof ContextStoppedEvent){
            System.out.println("================:{}" + "ContextStoppedEvent");
        }
        if (event instanceof ApplicationReadyEvent){
            System.out.println("================ ApplicationReadyEvent ");
        }
        if (event instanceof ServletWebServerInitializedEvent){
            System.out.println("==================ServletWebServerInitializedEvent的root：" + ((ServletWebServerInitializedEvent) event).getApplicationContext().getParent());
        }
        if (event instanceof ApplicationStartedEvent){
            System.out.println("==================ApplicationStartedEvent：" + (((ApplicationStartedEvent) event).getApplicationContext().getParent()));
        }
        System.out.println(">>>>>>>>>>>>>>>>" +event.getClass().getName());
    }

}
```

启动项目