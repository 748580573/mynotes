### URLClassloader

写这篇文章的背景是我在瞅源码的时候，发现了自定义的类加载器。然后我在想怎么用自定义的URLClassLoader去加载jar包，去找到jar中的资源。

先上代码

````java
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;
import java.util.jar.Manifest;

public class MyClassLoader extends URLClassLoader {

    private String baseUrl;

    public MyClassLoader(){
        this(new URL[]{});
    }

    public MyClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public MyClassLoader(URL[] urls) {
        super(urls);
    }

    public MyClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void addURL(String url){
        URL uurl = null;

        try {
            uurl = new URL(baseUrl + url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        addURL(uurl);
    }

    public void addURL(URL url){
        super.addURL(url);
    }

    /**
     * 返回urls
     * @return
     */
    public URL[] getURLs(){
        return super.getURLs();
    }

    /**
     * 查找类对象
     * 从以上的URLS中查找加载当前类对象【会打开所有的jars去查找指定的类】
     * @param name
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

//    public URL findResource(String name){
//        URL url = null;
//        try {
//            url = new URL(baseUrl + url);
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//        return url;
//    }

    /**
     * 查找资源列表【URL查找路径】
     * @param name
     * @return
     * @throws IOException
     */
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        return super.findResources(name);
    }

    @Override
    protected Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
        return super.definePackage(name, man, url);
    }
}
````

自定义的类加载器有几个关键点：

* 三个构造器：

  ```java
  public MyClassLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
  }
  
  public MyClassLoader(URL[] urls) {
      super(urls);
  }
  
  public MyClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
      super(urls, parent, factory);
  }
  ```

这三个构造器是继承自父类的构造器的必须实现，用默认的实现就行了。

* ```java
  public MyClassLoader(){
      this(new URL[]{});
  }
  
      public void addURL(String url){
          URL uurl = null;
  
          try {
              uurl = new URL(baseUrl + url);
          } catch (MalformedURLException e) {
              e.printStackTrace();
          }
  
          addURL(uurl);
      }
  ```

这个构造器这样写，传入的是一个长度为0的URL数组，也可以自己对URL数组进行初始化并赋值。也可以调用addURL(String url)对URL数组进行元素的添加。这个的URL可以是jar所在路径。





#### 使用jar包中的类以及资源文件

```java
public class Test {


    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, IOException {
        String jarPath = "E:/test";
        MyClassLoader classLoader = new MyClassLoader();
        classLoader.setBaseUrl("file:///e:/test");
        classLoader.addURL("/data.jar");

        Class<?> clazz = classLoader.findClass("com.xxx.Test");
        if (clazz == null){
            System.out.println("没有该类");
        }else {
            System.out.println("有该类");
        }

        URL url = clazz.getClassLoader().getResource("log4j.properties");
        if (url == null){
            System.out.println("没有该路径");
        }else {
            System.out.println("有该路径");
        }
        Properties properties = new Properties();
        properties.load(clazz.getResourceAsStream("log4j.properties"));
        System.out.println("hello");
    }
}
```

从上面代码中可以看到classLoader.findClass方法需要传入一个全类名，MyClassLoader首先会委托自己的父类去加载这个类，父类又会委托父类的父类去加载这个类，如果父类的父类找不到该类就会让父类去加载该类，如果父类找不到该类，那么MyClassLoader才会自己去加载该类。

通过使用classLoader.getResource("log4j.properties")便可以找到MyClassLoader加载的jar中的资源文件了。