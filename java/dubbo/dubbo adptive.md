# dubbo adptive注解

> 转载自：https://zhuanlan.zhihu.com/p/87075689

Dubbo提供了一种SPI的机制用于动态的加载扩展类，但是如何在运行时动态的选用哪一种扩展类来提供服务，这就需要一种机制来进行动态的匹配。Dubbo SPI中提供的Adaptive机制就为解决这个问题提供了一种良好的解决方案，本文首先会通过一个示例来讲解Adaptive机制的用法，然后会从源码的角度对其实现原理进行讲解。

### 1. 用法示例

对应于Adaptive机制，Dubbo提供了一个注解`@Adaptive`，该注解可以用于接口的某个子类上，也可以用于接口方法上。如果用在接口的子类上，则表示Adaptive机制的实现会按照该子类的方式进行自定义实现；如果用在方法上，则表示Dubbo会为该接口自动生成一个子类，并且按照一定的格式重写该方法，而其余没有标注`@Adaptive`注解的方法将会默认抛出异常。对于第一种Adaptive的使用方式，Dubbo里只有`ExtensionFactory`接口使用了，其有一个子类`AdaptiveExtensionFactory`就使用了`@Adaptive`注解进行了标注，主要作用就是在获取目标对象时，分别通过`ExtensionLoader`和`Spring容器`两种方式获取，该类的实现原理比较简单，读者可自行阅读其源码，本文主要讲解将`@Adaptive`注解标注在接口方法上以实现Adaptive机制的使用原理。这里我们以一个"水果种植者"的示例来进行讲解，水果种植者可以种植诸如苹果和香蕉等水果。这里我们首先定义一个苹果种植者的接口：

```java
@SPI("apple")
public interface FruitGranter {

  Fruit grant();

  @Adaptive
  String watering(URL url);
}
```

这里需要注意的是，如果要使用Dubbo的SPI的支持，必须在目标接口上使用`@SPI`注解进行标注，后面的值提供了一个默认值，也就是说如果没有自定义的指定使用哪个子类，那么就使用该值所指定的子类。在该接口中，我们将`watering()`方法使用`@Adaptive`注解进行了标注，表示该方法在自动生成的子类中是需要动态实现的方法。下面是我们为`FruitGranter`提供的两个实现类：

```java
// 苹果种植者
public class AppleGranter implements FruitGranter {

  @Override
  public Fruit grant() {
    return new Apple();
  }

  @Override
  public String watering(URL url) {
    System.out.println("watering apple");
    return "watering finished";
  }
}
// 香蕉种植者
public class BananaGranter implements FruitGranter {

  @Override
  public Fruit grant() {
    return new Banana();
  }

  @Override
  public String watering(URL url) {
    System.out.println("watering banana");
    return "watering success";
  }
}
```

这里提供了`AppleGranter`和`BananaGranter`表示的其实是两种基础服务类，本质上它们三者的关系是`FruitGranter`用于对外提供一个规范，而`AppleGranter`和`BananaGranter`则是实现了这种规范的两种基础服务。至于调用方需要使用哪种基础服务来实现其功能，这就需要根据调用方指定的参数来动态选取的，而`@Adaptive`机制就是提供了这样一种选取功能。

在Dubbo的SPI中，我们指定了上述两种服务类之后，需要在`META-INF/dubbo`下创建一个文件，该文件的名称是目标接口的全限定名，这里是`org.apache.dubbo.demo.example.eg19.FruitGranter`，在该文件中需要指定该接口所有可提供服务的子类，形式如：

```java
apple=org.apache.dubbo.demo.example.eg19.AppleGranter
banana=org.apache.dubbo.demo.example.eg19.BananaGranter
```

文件中每一个子类都有一个key与之对应，这个key也就是前面`@SPI`注解后面所指定的值，也就是说这里如果调用方没有自定义指定使用哪个子类，那么默认就会使用`AppleGranter`来提供服务。下面我们来看一下调用方代码如何实现：

```java
public class ExtensionLoaderTest {

  @Test
  public void testGetExtensionLoader() {
    // 首先创建一个模拟用的URL对象
    URL url = URL.valueOf("dubbo://192.168.0.101:20880?fruit.granter=apple");
    // 通过ExtensionLoader获取一个FruitGranter对象
    FruitGranter granter = ExtensionLoader.getExtensionLoader(FruitGranter.class)
      .getAdaptiveExtension();
    // 使用该FruitGranter调用其"自适应标注的"方法，获取调用结果
    String result = granter.watering(url);
    System.out.println(result);
  }
}
```

上述代码中，我们首先模拟构造了一个URL对象，这个URL对象是Dubbo中进行参数传递所使用的一个基础类，在配置文件中配置的属性都会被封装到该对象中。这里我们主要要注意该对象是通过一个url构造的，并且url的最后我们有一个参数`fruit.granter=apple`，这里其实就是我们所指定的使用哪种基础服务类的参数。比如这里指定的就是使用`apple`对应的`AppleGranter`。

在构造一个URL对象之后，我们通过`ExtensionLoader.getExtensionLoader(FruitGranter.class)`方法获取了一个`FruitGranter`对应的`ExtensionLoader`对象，然后调用其`getAdaptiveExtension()`方法获取其为`FruitGranter`接口构造的子类实例，这里的子类实际上就是`ExtensionLoader`通过一定的规则为`FruitGranter`接口编写的子类代码，然后通过`javassist`或`jdk`编译加载这段代码，加载完成之后通过反射构造其实例，最后将其实例返回。在上面我们调用该实例，也就是granter对象的`watering()`方法时，该方法内部就会通过url对象指定的参数来选择具体的实例，从而将真正的工作交给该实例进行。通过这种方式，Dubbo SPI就实现了根据传入参数动态的选用具体的实例来提供服务的功能。如下是该`ExtensionLoader`为`FruitGranter`动态生成的子类代码：

```java
package org.apache.dubbo.demo.example.eg19;

import org.apache.dubbo.common.extension.ExtensionLoader;

public class FruitGranter$Adaptive implements org.apache.dubbo.demo.example.eg19.FruitGranter {

  public org.apache.dubbo.demo.example.eg19.Fruit grant() {
    throw new UnsupportedOperationException(
        "The method public abstract org.apache.dubbo.demo.example.eg19.Fruit " 
      + "org.apache.dubbo.demo.example.eg19.FruitGranter.grant() of interface " 
      + "org.apache.dubbo.demo.example.eg19.FruitGranter is not adaptive method!");
  }

  public java.lang.String watering(org.apache.dubbo.common.URL arg0) {
    if (arg0 == null) {
      throw new IllegalArgumentException("url == null");
    }

    org.apache.dubbo.common.URL url = arg0;
    String extName = url.getParameter("fruit.granter", "apple");
    if (extName == null) {
      throw new IllegalStateException(
          "Failed to get extension (org.apache.dubbo.demo.example.eg19.FruitGranter) name " 
        + "from url (" + url.toString() + ") use keys([fruit.granter])");
    }
    org.apache.dubbo.demo.example.eg19.FruitGranter extension =
      (org.apache.dubbo.demo.example.eg19.FruitGranter) ExtensionLoader
        .getExtensionLoader(org.apache.dubbo.demo.example.eg19.FruitGranter.class)
        .getExtension(extName);
    return extension.watering(arg0);
  }
}
```

关于该生成的代码，我们主要要注意如下几个问题：

- 所有未使用`@Adaptive`注解标注的接口方法，默认都会抛出异常；
- 在使用`@Adaptive`注解标注的方法中，其参数中必须有一个参数类型为URL，或者其某个参数提供了某个方法，该方法可以返回一个URL对象；
- 在方法的实现中会通过URL对象获取某个参数对应的参数值，如果在接口的`@SPI`注解中指定了默认值，那么在使用URL对象获取参数值时，如果没有取到，就会使用该默认值；
- 最后根据获取到的参数值，在`ExtensionLoader`中获取该参数值对应的服务提供类对象，然后将真正的调用委托给该服务提供类对象进行；
- 在通过URL对象获取参数时，参数key获取的对应规则是，首先会从`@Adaptive`注解的参数值中获取，如果该注解没有指定参数名，那么就会默认将目标接口的类名转换为点分形式作为参数名，比如这里`FruitGranter`转换为点分形式就是`fruit.granter`。

### 2. 实现原理

Dubbo Adaptive的实现机制根据上面的讲解其实步骤已经比较清晰了，主要分为如下三个步骤：

- 加载标注有`@Adaptive`注解的接口，如果不存在，则不支持Adaptive机制；
- 为目标接口按照一定的模板生成子类代码，并且编译生成的代码，然后通过反射生成该类的对象；
- 结合生成的对象实例，通过传入的URL对象，获取指定key的配置，然后加载该key对应的类对象，最终将调用委托给该类对象进行。

可以看到，通过这种方式，Dubbo就实现了一种通过配置参数动态选择所使用的服务的目的，而实现这种机制的入口主要在`ExtensionLoader.getAdaptiveExtension()`方法，如下是该方法的实现：

```java
public T getAdaptiveExtension() {
  Object instance = cachedAdaptiveInstance.get();
  if (instance == null) {
    if (createAdaptiveInstanceError == null) {
      synchronized (cachedAdaptiveInstance) {
        instance = cachedAdaptiveInstance.get();
        if (instance == null) {
          try {
            // 创建Adaptive实例
            instance = createAdaptiveExtension();
            cachedAdaptiveInstance.set(instance);
          } catch (Throwable t) {
            createAdaptiveInstanceError = t;
            throw new IllegalStateException("Failed to create adaptive " 
                + "instance: " + t.toString(), t);
          }
        }
      }
    } else {
      throw new IllegalStateException("Failed to create adaptive instance: " 
          + createAdaptiveInstanceError.toString(), createAdaptiveInstanceError);
    }
  }

  return (T) instance;
}
```

上面的代码比较简单，其实就是首先通过双检查法来从缓存中获取Adaptive实例，如果没获取到，则创建一个。我们这里继续看`createAdaptiveExtension()`方法的实现：

```java
private T createAdaptiveExtension() {
  try {
    return injectExtension((T) getAdaptiveExtensionClass().newInstance());
  } catch (Exception e) {
    throw new IllegalStateException("Can't create adaptive extension " 
        + type + ", cause: " + e.getMessage(), e);
  }
}
```

这里创建Adaptive实例的方法是一个主干方法，从这里调用方法的顺序就可以看出其主要作用：

- 获取一个Adaptive类的class对象，不存在则创建一个，该方法会保证一定存在一个该class对象；
- 通过反射创建一个Adaptive类的实例；
- 对创建的Adaptive注入相关属性，需要注意的是，Dubbo目前只支持通过setter方法注入属性。

上面的通过setter方法注入属性的方法比较简单，主要是通过反射读取相关的参数，然后分别在Dubbo的SPI和Spring容器中查找对应的bean，并且将其注入进来，这段代码比较简单，读者可自行阅读。我们这里主要关注Dubbo如何创建Adaptive类对象，也即`getAdaptiveExtensionClass()`方法的实现：

```java
private Class<?> getAdaptiveExtensionClass() {
  // 通过读取Dubbo的配置文件，获取其中的SPI类，其主要处理了四部分的类：
  // 1. 标注了@Activate注解的类，该注解的主要作用是将某个实现子类标注为自动激活，也就是在加载
  //    实例的时候也会加载该类的对象；
  // 2. 记录目标接口是否标注了@Adaptive注解，如果标注了该注解，则表示需要为该接口动态生成子类，或者说
  //    目标接口是否存在标注了@Adaptive注解的子类，如果存在，则直接使用该子类作为Adaptive类；
  // 3. 检查加载到的类是否包含有传入目标接口参数的构造方法，如果是，则表示该类是一个代理类，也可以
  //    将其理解为最终会被作为责任链进行调用的类，这些类最终会在目标类被调用的时候以类似于AOP的方式，
  //    将目标类包裹起来，然后将包裹之后的类对外提供服务；
  // 4. 剩余的一般类就是实现了目标接口，并且作为基础服务提供的类。
  getExtensionClasses();
  // 经过上面的类加载过程，如果目标接口某个子类存在@Adaptive注解，就会将其class对象缓存到
  // cachedAdaptiveClass对象中。这里我们就可以看到@Adaptive注解的两种使用方式的分界点，也就是说，
  // 如果某个子类标注了@Adaptive注解，那么就会使用该子类所自定义的Adaptive机制，如果没有子类标注了
  // 该注解，那么就会使用下面的createAdaptiveExtensionClass()方式来创建一个目标类class对象
  if (cachedAdaptiveClass != null) {
    return cachedAdaptiveClass;
  }
  // 创建一个目标接口的子类class对象
  return cachedAdaptiveClass = createAdaptiveExtensionClass();
}

private Class<?> createAdaptiveExtensionClass() {
  // 为目标接口生成子类代码，以字符串形式表示
  String code = new AdaptiveClassCodeGenerator(type, cachedDefaultName).generate();
  // 获取classloader
  ClassLoader classLoader = findClassLoader();
  // 通过jdk或者javassist的方式编译生成的子类字符串，从而得到一个class对象
  org.apache.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(
    org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
  return compiler.compile(code, classLoader);
}
```

上面的代码中主要是一个骨架代码，首先通过`getExtensionClasses()`获取配置文件中配置的各个类对象，其加载的原理读者可阅读本人前面的文章[Dubbo之SPI原理详解](https://link.zhihu.com/?target=https%3A//my.oschina.net/zhangxufeng/blog/2982932)；加载完成后，会通过`AdaptiveClassCodeGenerator`来为目标类生成子类代码，并以字符串的形式返回，最后通过javassist或jdk的方式进行编译然后返回class对象。这里我们主要阅读`AdaptiveClassCodeGenerator.generate()`方法是如何生成目标接口的子类的：

```java
public String generate() {
  // 判断目标接口是否有方法标注了@Adaptive注解，如果没有则抛出异常
  if (!hasAdaptiveMethod()) {
    throw new IllegalStateException("No adaptive method exist on extension " 
        + type.getName() + ", refuse to create the adaptive class!");
  }

  StringBuilder code = new StringBuilder();
  code.append(generatePackageInfo());   // 生成package信息
  // 生成import信息，这里只导入了ExtensionLoader类，其余的类都通过全限定名的方式来使用
  code.append(generateImports());
  code.append(generateClassDeclaration());  // 生成类声明信息

  Method[] methods = type.getMethods();
  for (Method method : methods) {
    code.append(generateMethod(method));    // 为各个方法生成实现方法信息
  }
  code.append("}");

  if (logger.isDebugEnabled()) {
    logger.debug(code.toString());
  }
  return code.toString();   // 返回生成的class代码
}
```

这里`generate()`方法是生成目标类的主干方法，其主要分为如下几个步骤：

- 生成package信息；
- 生成import信息；
- 生成类声明信息；
- 生成各个方法的实现；

这里前面几个步骤实现原理都相较比较简单，我们继续阅读`generateMethod()`方法的实现原理：

```java
private String generateMethod(Method method) {
   String methodReturnType = method.getReturnType().getCanonicalName(); // 生成返回值信息
   String methodName = method.getName();    // 生成方法名信息
   String methodContent = generateMethodContent(method);    // 生成方法体信息
   String methodArgs = generateMethodArguments(method); // 生成方法参数信息
   String methodThrows = generateMethodThrows(method);  // 生成异常信息
   return String.format(CODE_METHOD_DECLARATION, methodReturnType, methodName, 
        methodArgs, methodThrows, methodContent);   // 对方法进行格式化返回
 }
```

可以看到，方法的生成，也拆分成了几个子步骤，主要包括：

- 生成返回值信息；
- 生成方法名信息；
- 生成方法参数信息；
- 生成方法的异常信息；
- 生成方法体信息；

需要注意的是，这里所使用的所有类都是使用的其全限定类名，通过前面我们展示的`FruitGranter`的子类代码也可以看出这一点。上面生成的信息中，方法的返回值，方法名，方法参数以及异常信息都可以通过接口声明获取到，而方法体则需要根据一定的逻辑来生成。关于方法参数，需要说明的是，Dubbo并没有使用接口中对应参数的名称，而是对每一个参数的参数名依次使用`arg0`、`arg1`等名称，后续在阅读代码时读者需要注意这一点。这里我们继续阅读Dubbo生成方法体内容的代码：

```java
private String generateMethodContent(Method method) {
  // 获取方法上标注的@Adaptive注解，前面讲到，Dubbo会使用该注解的值作为动态参数的key值
  Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);
  StringBuilder code = new StringBuilder(512);
  if (adaptiveAnnotation == null) {
    // 如果当前方法没有标注@Adaptive注解，该方法的实现就会默认抛出异常
    return generateUnsupported(method);
  } else {
    // 获取参数中类型为URL的参数所在的参数索引位置，因为我们的参数都是通过arg[i]的形式编排的，因而
    // 获取其索引就可以得到该参数的引用。这里URL参数的主要作用是获取目标参数对应的参数值
    int urlTypeIndex = getUrlTypeIndex(method);

    if (urlTypeIndex != -1) {
      // 如果参数中存在URL类型的参数，那么就为该参数进行空值检查，如果为空，则抛出异常
      code.append(generateUrlNullCheck(urlTypeIndex));
    } else {
      // 如果参数中不存在URL类型的参数，那么就会检查每个参数，判断其是否有某个方法的返回值是URL类型，
      // 如果存在该方法，则首先对该参数进行空指针检查，如果为空则抛出异常。然后调用该对象的目标方法，
      // 以获取到一个URL对象，然后对获取到的URL对象进行空值检查，为空也会抛出异常。
      code.append(generateUrlAssignmentIndirectly(method));
    }

    // 这里主要是获取@Adaptive注解的参数，如果没有配置，就会使用目标接口的类型由驼峰形式转换为点分形式
    // 的名称作为将要获取的参数值的key名称，比如前面的FruitGranter转换后为fruit.granter。
    // 这里需要注意的是，返回值是一个数组类型，这是因为Dubbo会通过嵌套获取的方式来的到目标参数，
    // 比如我们使用了@Adaptive({"client", "transporter"})的形式，那么最终就会在URL对象中获取两次
    // 参数，如String extName = url.getParameter("client", url.getParameter("transporter"))
    String[] value = getMethodAdaptiveValue(adaptiveAnnotation);

    // 判断是否存在Invocation类型的参数
    boolean hasInvocation = hasInvocationArgument(method);

    // 为Invocation类型的参数添加空值检查的逻辑
    code.append(generateInvocationArgumentNullCheck(method));

    // 生成获取extName的逻辑，也即前面通过String[] value生成的通过url.getParameter()的
    // 逻辑代码，最终会得到用户配置的扩展的名称，从而对应某个基础服务类
    code.append(generateExtNameAssignment(value, hasInvocation));
    // 为extName添加空值检查代码
    code.append(generateExtNameNullCheck(value));

    // 通过extName在ExtensionLoader中获取其对应的基础服务类，比如前面的FruitGranter，在这里就是
    // FruitGranter extension = ExtensionLoader.getExtensionLoader(ExtensionLoader.class)
    // .getExtension(extName)，这样就得到了一个FruitGranter的实例对象
    code.append(generateExtensionAssignment());

    // 生成目标实例的当前方法的调用逻辑，然后将结果返回。比如FruitGranter就是
    // return extension.watering(arg0);
    // 这里方法名就是当前实现的方法的名称，而参数就是当前方法传入的参数，读者不要忘记了当前方法
    // 就是目标接口中的同一方法，而方法参数前面已经讲到，都是使用arg[i]的形式命名的，因而这里直接
    // 将其依次罗列出来即可
    code.append(generateReturnAndInovation(method));
  }

  // 将生成的代码返回
  return code.toString();
}
```

上面的代码中，整体逻辑还是比较清晰的，读者可以根据上面的讲解，然后结合前面展示的`FruitGranter`生成的子类代码进行对比来看，这样就可以对整个实现逻辑有比较好的理解了。上面的逻辑主要分为了如下几个步骤：

- 判断当前方法是否标注了`@Adaptive`注解，如果没有标注，则为其生成一个默认实现，该实现中会默认抛出异常，也就是说只有使用`@Adaptive`注解标注的方法才是作为自适应机制的方法；
- 获取方法参数中类型为URL的参数，如果不存在，则获取参数中某个存在可以返回URL类型对象的方法的参数，并且调用该方法获取URL参数；
- 通过`@Adaptive`注解的配置获取目标参数的key值，然后通过前面得到的URL参数获取该key对应的参数值，从而得到了基础服务类对应的名称；
- 通过`ExtensionLoader`获取该名称对应的基础服务类实例；
- 通过调用基础服务类的实例的当前方法来实现最终的基础服务。

可以看到，这里实现的自适应机制逻辑结构是非常清晰的，读者通过阅读这里的源码也就比较好的理解了Dubbo所提供的自适应机制的原理，也能够比较好的通过自适应机制来完成某些定制化的工作。

### 3. 小结

本文首先通过一个示例来讲解了Dubbo的自适应机制的使用方式，然后在源码的层面对自适应机制的实现原理进行了讲解。