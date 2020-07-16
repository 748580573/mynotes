### 处理图标不能显示问题

````
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <configuration>
                    <nonFilteredFileExtensions>
                        <nonFilteredFileExtension>ttf</nonFilteredFileExtension>
                        <nonFilteredFileExtension>woff</nonFilteredFileExtension>
                        <nonFilteredFileExtension>woff2</nonFilteredFileExtension>
                        <nonFilteredFileExtension>eot</nonFilteredFileExtension>
                        <nonFilteredFileExtension>svg</nonFilteredFileExtension>
                    </nonFilteredFileExtensions>
                </configuration>
            </plugin>
````

### dependencyManagement的作用

Maven使用dependencyManagement元素来提供一种管理以来版本号的方式。	

使用pom.xml中的dependencyManagement元素能让所有子项目中引用一个依赖而不用显示的写出版本号。

maven会沿着父子层向上走，知道找到一个拥有dependencyManagement元素的项目，然后他们就会使用dependencyManagement中指定的版本号。

例如在父项目中：

```xml
<dependencyManagement>
    <dependencies>
        <!--spring boot 2.2.2-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.2.2.RELEASE</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

然后在子项目里就可以添加spring-boot-dependencies时可以不指定版本号，例如：

````xml
    <dependencies>	
        <!--spring boot 2.2.2-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
        </dependency>
    </dependencies>
````

