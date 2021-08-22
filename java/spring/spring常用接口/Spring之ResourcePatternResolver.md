# Spring之ResourcePatternResolver

继承自ResourceLoader，但是功能更加强大，普通的ResourceLoader只能找到本工程内的资源文件，但是可以通过ResourcePatternResolver去所有的jar包的类路径下找到jar包，并且它还支持ant表达式。

```java
 public static void main(String[] args) throws IOException {
    ResourceLoader resourceLoader = new DefaultResourceLoader();
    Resource resource = resourceLoader.getResource("classpath:META-INF/spring.factories");
    // 因为`classpath:`只在本工程内查找，所以肯定找不到 spring.factories
    System.out.println(resource.exists()); //false
```


```java
    PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resourcePatternResolver.getResources("classpath*:META-INF/spring.factories");
    // 它会去找所有的jar包的类路径开始查找，所以现在是可议找到多个的~~~
    System.out.println(resources.length); //2
    System.out.println(Arrays.asList(resources));
    //[URL [jar:file:/E:/repository/org/springframework/spring-beans/5.0.6.RELEASE/spring-beans-5.0.6.RELEASE.jar!/META-INF/spring.factories],
    //URL [jar:file:/E:/repository/org/springframework/spring-test/5.0.6.RELEASE/spring-test-5.0.6.RELEASE.jar!/META-INF/spring.factories]]

    // 还能使用Ant风格进行匹配~~~  太强大了：
    resources = resourcePatternResolver.getResources("classpath*:META-INF/*.factories");
    System.out.println(resources); // 能匹配上所有了路径下，`META-INF/*.factories`匹配上的所有文件
    resources = resourcePatternResolver.getResources("classpath*:com/fsx/**/*.class");
    System.out.println(resources.length); //42 相当于把我当前项目所有的类都拿出来了
```

