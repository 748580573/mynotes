# IDEA远程调试

  

## 1 概述

原理：本机和远程主机的两个 VM 之间使用 Debug 协议通过 Socket 通信，传递调试指令和调试信息。
被调试程序的远程虚拟机：作为 Debug 服务端，监听 Debug 调试指令。jdwp是Java Debug Wire Protocol的缩写。
调试程序的本地虚拟机：IDEA 中配置的 Remote Server，指定 Debug 服务器的Host:Port，以供 Debug 客户端程序连接。

## 2 设置
### 2.1 IDEA 中指定 Debug 服务器
点击主窗口菜单 Run / Edit Configurations，打开“Run/Debug Configurations”窗口；
点击工具栏上的“+”按钮，下拉菜单中选择“Remote”；
设置 Host 为远程服务器的域名或IP，保持 Port=5005 无需调整；
复制命令行参数，形如 -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005

![](./imgs/1.png)




### 2.2 远程服务中开启 Debug 服务
#### 2.2.1 对于 SpringBoot
命令行添加选项，并重启：

````shell
## 注意新参数必须在 -jar 之前

jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar test.jar
````



#### 2.2.2 对于 Tomcat
启动脚本中添加选项，并重启：

````shell
## sudo vim $CATALINA_HOME/bin/catalina.sh

JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
````

#### 2.3 远程服务器防火墙端口放行

````shell
### sudo vim /etc/sysconfig/iptables

-A INPUT -m state --state NEW -m tcp -p tcp --dport 5005 -j ACCEPT

### 重启生效: sudo systemctl restart iptables
````

### 3 开始调试

* 要求：双方代码一致，否则远程调试无法启动；
* 本地启动刚刚配置的 Remote Server，正常时会看到日志: Connected to the target VM, address: 'xxx:5005', transport: 'socket'
* 本地 IDEA 代码中设置断点
* 浏览器或手机 HTTP 访问服务器
* IDEA 即可在断点暂停并跟踪

### 4 关闭调试

服务器上多开放个端口是不安全的，调试完毕后可恢复防火墙设置。
而 Java 服务器开启 Debug 服务器的功能可以保留，以便之后再次调试。