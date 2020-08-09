#### 

**1、network.service: control process exited, code=exited status=1**

解决方法：

````
1、输入：systemctl stop NetworkManager，然后重启网络服务systemctl start network
````

> 参考链接：https://blog.csdn.net/m0_37970699/article/details/100573410