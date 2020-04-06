

dubbo的目的：回去一个实现类的对象

途径：通过ExtensionLoader.getExtension(String name)

实现路径：

* getExtensionLoader(Class<T> type)：就是为type，new 一个ExtensionLoader，然后缓存起来。
* getAdaptiveExtension() ：获取一个扩展装饰类的对象，这个类有一个规则，如果它没有一个@Adaptive注解，就动态创建一个装饰类，例如Protocol$Adaptive对象。
* getExtension(String name)：获取一个对象。



adaptive注解在类和方法上的区别：

1.注解在类上：代表人工实现编码，即实现了一个装饰类（设计模式中的装饰模式）

2.注解在方法上：代表自动生成和编译一个动态的adpative类，例如：Protocol$Adpative