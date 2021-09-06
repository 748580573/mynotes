# new与newInstance的区别

在初始化一个类，生成一个实例的时候；newInstance() 和 new 有什么区别？

用newInstance与用new是区别的，区别在于创建对象的方式不一样，前者是使用类加载机制，那么为什么会有两种创建对象方式？这个就要从可伸缩、可扩展，可重用等软件思想上解释了。

Java中工厂模式经常使用newInstance来创建对象，因此从为什么要使用工厂模式上也可以找到具体答案。
例如：

````java
Class c = Class.forName(“A”);
factory = (AInterface)c.newInstance();
````

其中AInterface是A的接口，如果下面这样写，你可能会理解：

````java
String className = “A”;
Class c = Class.forName(className);
factory = (AInterface)c.newInstance();
````

进一步，如果下面写，你可能会理解：

````java
//从xml 配置文件中获得字符串Class c = Class.forName(className);factory = (AInterface)c.newInstance();
String className = readfromXMlConfig;
Class c = Class.forName(className);
factory = (AInterface)c.newInstance();
````

上面代码就消灭了A类名称，优点：无论A类怎么变化，上述代码不变，甚至可以更换A的兄弟类B , C , D….等，只要他们继承Ainterface就可以。
从jvm的角度看，我们使用new的时候，这个要new的类可以没有加载；

但是使用newInstance时候，就必须保证：

1、这个类已经加载；

2、这个类已经连接了。

而完成上面两个步骤的正是class的静态方法forName（）方法，这个静态方法调用了启动类加载器（就是加载javaAPI的那个加载器）。

有了上面jvm上的理解，那么我们可以这样说，newInstance实际上是把new这个方式分解为两步,即，首先调用class的加载方法加载某个类，然后实例化。

这样分步的好处是显而易见的。我们可以在调用class的静态加载方法forName时获得更好的灵活性，提供给了我们降耦的手段。