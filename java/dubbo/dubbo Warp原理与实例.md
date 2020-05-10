# Dubbo Wrapper 原理与实例

> 转自https://www.jianshu.com/p/57d53ff17062

Dubbo Wrapper 可以认为是一种反射机制。它既可以读写目标实例的字段，也可以调用目标实例的方法。比如

- Car是接口；RaceCar是实现类，实现了Car；ferrari和porsche是RaceCar的两个实例
- Dubbo为接口Car生成一个Warpper子类，比如Wrapper0；然后创建Wrapper0的实例wrapper0
- 通过wrapper0#setPropertyValue来修改ferrari的字段，也可以修改porsche的字段
- 通过wrapper0#invokeMethod来调用ferrari的方法，也可以调用porsche的方法
- 优点：通过一个Wrapper0实例就可以操作N个目标接口Car的实例

比如我们有一个Car接口，定义了3个方法：



```java
package com.alibaba.dubbo.demo;

public interface DemoService {

    String sayHello(String name);

    String sayGoodBye();
    
    String sayGoodMorning(String word1,String word2);

}
```

Wrapper#makeWrapper之后生成的Wrapper子类代码如下：



```java
package com.alibaba.dubbo.common.bytecode;

import com.alibaba.dubbo.common.bytecode.ClassGenerator.DC;
import com.alibaba.dubbo.demo.DemoService;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class Wrapper0 extends Wrapper implements DC {
    public static String[] pns;
    public static Map pts;
    public static String[] mns;
    public static String[] dmns;
    public static Class[] mts0;
    public static Class[] mts1;
    public static Class[] mts2;

    public String[] getPropertyNames() {
        return pns;
    }

    public boolean hasProperty(String var1) {
        return pts.containsKey(var1);
    }

    public Class getPropertyType(String var1) {
        return (Class)pts.get(var1);
    }

    public String[] getMethodNames() {
        return mns;
    }

    public String[] getDeclaredMethodNames() {
        return dmns;
    }

    public void setPropertyValue(Object var1, String var2, Object var3) {
        try {
            DemoService var4 = (DemoService)var1;
        } catch (Throwable var6) {
            throw new IllegalArgumentException(var6);
        }

        throw new NoSuchPropertyException("Not found property \"" + var2 + "\" filed or setter method in class com.alibaba.dubbo.demo.DemoService.");
    }

    public Object getPropertyValue(Object var1, String var2) {
        try {
            DemoService var3 = (DemoService)var1;
        } catch (Throwable var5) {
            throw new IllegalArgumentException(var5);
        }

        throw new NoSuchPropertyException("Not found property \"" + var2 + "\" filed or setter method in class com.alibaba.dubbo.demo.DemoService.");
    }

    public Object invokeMethod(Object var1, String var2, Class[] var3, Object[] var4) throws InvocationTargetException {
        DemoService var5;
        try {
            var5 = (DemoService)var1;
        } catch (Throwable var8) {
            throw new IllegalArgumentException(var8);
        }

        try {
            if ("sayGoodMorning".equals(var2) && var3.length == 2) {
                return var5.sayGoodMorning((String)var4[0], (String)var4[1]);
            }

            if ("sayHello".equals(var2) && var3.length == 1) {
                return var5.sayHello((String)var4[0]);
            }

            if ("sayGoodBye".equals(var2) && var3.length == 0) {
                return var5.sayGoodBye();
            }
        } catch (Throwable var9) {
            throw new InvocationTargetException(var9);
        }

        throw new NoSuchMethodException("Not found method \"" + var2 + "\" in class com.alibaba.dubbo.demo.DemoService.");
    }

    public Wrapper0() {
    }
}

```

可以认为是重写了JDK的反射机制。



0人点赞
