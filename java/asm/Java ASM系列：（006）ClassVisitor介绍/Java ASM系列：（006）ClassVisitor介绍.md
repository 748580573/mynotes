# Java ASM系列：（006）ClassVisitor介绍

> 转载自：https://blog.51cto.com/lsieun/2928961

在ASM Core API中，最重要的三个类就是`ClassReader`、`ClassVisitor`和`ClassWriter`类。在进行Class Generation操作的时候，`ClassVisitor`和`ClassWriter`这两个类起着重要作用，而并不需要`ClassReader`类的参与。在本文当中，我们将对`ClassVisitor`类进行介绍。

![](./imgs/1.png)

## 1. ClassVisitor类

### 1.1 class info

第一个部分，`ClassVisitor`是一个抽象类。
由于`ClassVisitor类`是一个`abstract`类，所以不能直接使用`new`关键字创建`ClassVisitor`对象。

````java
public abstract class ClassVisitor {
}
````

同时，由于`ClassVisitor`类是一个`abstract`类，要想使用它，就必须有具体的子类来继承它。比较常见的`ClassVisitor`子类有`ClassWriter`类（Core API）和`ClassNode`类（Tree API）。

````java
public class ClassWriter extends ClassVisitor {
}
````

````java
public class ClassNode extends ClassVisitor {
}
````

三个类的关系如下：

- org.objectweb.asm.ClassVisitor
  - org.objectweb.asm.ClassWriter
  - org.objectweb.asm.tree.ClassNode

### 1.2 fields

第二个部分，`ClassVisitor`类定义的字段有哪些。

````java
public abstract class ClassVisitor {
    protected final int api;
    protected ClassVisitor cv;
}
````

* api字段：它是一个int类型的数据，指出了当前使用的ASM API版本，其取值有Opcodes.ASM4、Opcodes.ASM5、Opcodes.ASM6、Opcodes.ASM7、Opcodes.ASM8和Opcodes.ASM9。我们使用的ASM版本是9.0，因此我们在给api字段赋值的时候，选择Opcodes.ASM9就可以了。

* cv字段：它是一个ClassVisitor类型的数据，它的作用是将多个ClassVisitor串连起来。

![](./imgs/2.png)

### 1.3 constructors

第三个部分，`ClassVisitor`类定义的构造方法有哪些。

````java
public abstract class ClassVisitor {
    public ClassVisitor(final int api) {
        this(api, null);
    }

    public ClassVisitor(final int api, final ClassVisitor classVisitor) {
        this.api = api;
        this.cv = classVisitor;
    }
}
````

### 1.4 methods

第四个部分，`ClassVisitor`类定义的方法有哪些。在ASM当中，使用到了Visitor Pattern（访问者模式），所以`ClassVisitor`当中许多的`visitXxx()`方法。

虽然，在ClassVisitor类当中，有许多visitXxx()方法，但是，我们只需要关注这4个方法：visit()、visitField()、visitMethod()和visitEnd()。为什么只关注这4个方法呢？因为这4个方法是ClassVisitor类的精髓或骨架，认识了这4个方法，其它的visitXxx()都容易扩展；同时，我们将visitXxx()方法缩小为4个，也能减少我们在学习ASM过程中的认知负担。

````java
public abstract class ClassVisitor {
    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces);
    public FieldVisitor visitField( // 访问字段
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final Object value);
    public MethodVisitor visitMethod( // 访问方法
        final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions);
    public void visitEnd();
    // ......
}
````

在ClassVisitor的visit()方法、visitField()方法和visitMethod()方法中都带有signature参数。这个signature参数“泛型”密切相关；换句话说，如果处理的是一个带有泛型信息的类、字段或方法，那么就需要给signature参数提供一定的值；如果处理的类、字段或方法不带有“泛型”信息，那么将signature参数设置为null就可以了。在本次课程当中，我们不去考虑“泛型”相关的内容，所以我们都将signature参数设置成null值。

如果大家对`signature`参数感兴趣，我们可以使用之前介绍的`ASMPrint`类去打印一下某个泛型类的ASM代码。例如，`java.lang.Comparable`是一个泛型接口，我们就可以使用`ASMPrint`类来打印一下它的ASM代码，从来查看`signature`参数的值是什么。

## 2. 方法的调用顺序

在`ClassVisitor`类当中，定义了多个`visitXxx()`方法。这些`visitXxx()`方法，遵循一定的调用顺序。这个调用顺序，是参考自`ClassVisitor`类的API文档。

````java
visit
[visitSource][visitModule][visitNestHost][visitPermittedSubclass][visitOuterClass]
(
 visitAnnotation |
 visitTypeAnnotation |
 visitAttribute
)*
(
 visitNestMember |
 visitInnerClass |
 visitRecordComponent |
 visitField |
 visitMethod
)* 
visitEnd
````

其中，涉及到一些符号，它们的含义如下：

- `[]`: 表示最多调用一次，可以不调用，但最多调用一次。
- `()`和`|`: 表示在多个方法之间，可以选择任意一个，并且多个方法之间不分前后顺序。
- `*`: 表示方法可以调用0次或多次。

在本次课程当中，我们只关注`ClassVisitor`类当中的`visit()`方法、`visitField()`方法、`visitMethod()`方法和`visitEnd()`方法这4个方法，所以上面的方法调用顺序可以简化如下：

````java
visit
(
 visitField |
 visitMethod
)* 
visitEnd
````

也就是说，先调用`visit()`方法，接着调用`visitField()`方法或`visitMethod()`方法，最后调用`visitEnd()`方法。

## 3. visitXxx()方法与ClassFile

`ClassVisitor`的`visitXxx()`方法与`ClassFile`之间存在对应关系。在`ClassVisitor`中定义的`visitXxx()`方法，并不是凭空产生的，这些方法存在的目的就是为了生成一个合法的`.class`文件，而这个`.class`文件要符合ClassFile的结构，所以这些`visitXxx()`方法与ClassFile的结构密切相关。

### 3.1 visit()方法

````java
public void visit(
    final int version,
    final int access,
    final String name,
    final String signature,
    final String superName,
    final String[] interfaces);
````

````java
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
````

![](./imgs/3.png)

### 3.2 visitField()方法

````java
public FieldVisitor visitField( // 访问字段
    final int access,
    final String name,
    final String descriptor,
    final String signature,
    final Object value);
````

````java
field_info {
    u2             access_flags;
    u2             name_index;
    u2             descriptor_index;
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
````

![](./imgs/4.png)

### 3.3 visitMethod()方法

````java
public MethodVisitor visitMethod( // 访问方法
    final int access,
    final String name,
    final String descriptor,
    final String signature,
    final String[] exceptions);
````

````java
method_info {
    u2             access_flags;
    u2             name_index;
    u2             descriptor_index;
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
````

![](./imgs/5.png)

### 3.4 visitEnd()方法

`visitEnd()`方法，它是这些`visitXxx()`方法当中最后一个调用的方法。

为什么`visitEnd()`方法是“最后一个调用的方法”呢？是因为在`ClassVisitor`当中，定义了多个`visitXxx()`方法，这些个`visitXxx()`方法之间要遵循一个先后调用的顺序，而`visitEnd()`方法是最后才去调用的。等到`visitEnd()`方法调用之后，就表示说再也不去调用其它的`visitXxx()`方法了，所有的“工作”已经做完了，到了要结束的时候了。

````java
/*
 * Visits the end of the class.
 * This method, which is the last one to be called,
 * is used to inform the visitor that all the fields and methods of the class have been visited.
 */
public void visitEnd() {
    if (cv != null) {
        cv.visitEnd();
    }
}
````

## 4. 总结

本文主要对`ClassVisitor`类进行介绍，内容总结如下：

- 第一点，介绍了`ClassVisitor`类的不同部分。我们去了解这个类不同的部分，是为了能够熟悉`ClassVisitor`这个类。
- 第二点，在`ClassVisitor`类当中，定义了许多`visitXxx()`方法，这些方法的调用要遵循一定的顺序。
- 第三点，在`ClassVisitor`类当中，定义的`visitXxx()`方法中的参数与ClassFile结构密切相关。

