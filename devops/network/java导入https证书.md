一、首先要在浏览器打开需要证书的网站，然后把证书下载下来，保存的证书名称随意命名，只要保证唯一性（这个唯一性下文有解释）

二、然后把证书复制到%JAVA_HOME%/jre/bin/路径下，即保证证书与keytool.exe文件同目录（其实不同也行，但是执行命令时需要指定路径）

三、导入过程如下:

复制完成之后打开doc窗口：运行-cmd

keytool -import -v -trustcacerts -alias taobao -file taobao.cer -storepass changeit -keystore %JAVA_HOME%/jre/lib/security/cacerts

解释说明：

1、taobao 是可以自己修改的名称， taobao.cer 是导出的证书，同样，这里的证书名字也是随便取的，但前提是保证将C:\Program Files\Java\jdk1.6.0_24\jre\lib\security目录的cacerts文件内之前没有导入同名证书，就是要保证你将要导入的证书名称唯一性。

2、changeit 是密码， java默认的。 

3、keytool是jdk中bin目录的一个exe文件，是jdk默认自带的，在我电脑的目录是：C:\Program Files\Java\jdk1.6.0_24\jre\bin\keytool.exe

4、%JAVA_HOME%/jre/lib/security/cacerts 这个路径中%JAVA_HOME%/jre/lib/security/是路径，cacerts是文件（即将要把证书导入到其中的文件）。当然要确保你已经配置过java_home环境变量，我的java_home环境变量为：C:\Program Files\Java\jdk1.6.0_24

5、其他保持不变。

6、如果提示："是否信任此证书? [否]:" ，那么请输入"y"。

 

当出现：”证书已添加到密钥库中 [正在存储cacerts]“的时候，那么恭喜你已经添加成功。



注意：如果提示找不到指定文件，那么有可能是路径错了，我这里是先执行cd C:\Program Files\Java\jdk1.6.0_24\jre\bin\ 来到keytool文所在目录执行以上命令。

以后更新时，先删除原来的证书，然后导入新的证书 。
  

下面是一些常用命令：

//查看cacerts中的证书列表：

​       keytool -list -keystore "%JAVA_HOME%/jre/lib/security/cacerts" -storepass changeit

//删除cacerts中指定名称的证书：

​       keytool -delete -alias taobao -keystore "%JAVA_HOME%/jre/lib/security/cacerts" -storepass changeit

//导入指定证书到cacerts：
       keytool -import -alias taobao -file taobao.cer -keystore "%JAVA_HOME%/jre/lib/security/cacerts" -storepass changeit-trustcacerts