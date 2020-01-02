# Java IO

### ByteBuffer

````java
A byte buffer.
This class defines six categories of operations upon byte buffers:
● Absolute and relative get and put methods that read and write single bytes;
● Relative bulk get methods that transfer contiguous sequences of bytes from this buffer into an array;
● Relative bulk put methods that transfer contiguous sequences of bytes from a byte array or some other byte buffer into this buffer;
● Absolute and relative get and put methods that read and write values of other primitive types, translating them to and from sequences of bytes in a particular byte order;
● Methods for creating view buffers, which allow a byte buffer to be viewed as a buffer containing values of some other primitive type; and
● Methods for compacting, duplicating, and slicing a byte buffer.
Byte buffers can be created either by allocation, which allocates space for the buffer's content, or by wrapping an existing byte array into a buffer. 
一种字节缓存
该class再字节缓存上定义了6种操作策略
● get和method方法绝对或相对的读取和写入单个字节
● 批get方法相对的传输连续的字节序列从该缓存到一个数组中
● 批put方法相对的传输连续的字节序从一个字节数据或者从其他的字节缓存中到该缓存中
● get和put方法绝对或相对的读取和写入其他原始类型的值，用一种特定的字节顺序将他们在字节序列间来回转换
● 创建视图缓冲区的方法，该方法允许将字节缓冲区视为包含某些其他原始类型值的缓冲区（这个我也没看懂，以后再看看文档）
● 压缩、复制和分片字节缓存区的方法。
字节缓存可以通过 分配缓存空间或者通过包装已经存在的字节数组到缓存中 进行创建

````

使用缓存有两个好处：

1. 减少实际的物理读写次数
2. 缓存在创建时就分配了内存，这块内存区域一种被重复使用，可以减少动态分配和回收内存的次数。



#### 属性

ByteBuffer有四个属性：capacity、limit、position、mark，并遵守：mark <= position <= limit <= capacity，下表对应着4个属性的解释

| property |                         discription                          |
| :------: | :----------------------------------------------------------: |
| capacity | 容量，即缓存区可容纳的最大数据量，在缓存区被创建时被设定并且不能被改变 |
|  Limit   | 表示缓存区当前的终点，不能对缓存区超过Limit的位置进行读写，limit可以被修改 |
| position | 位置，下一个要被读或写的元素的索引，每次读写缓存区数据时都会改变该值，为下一次读写做准备 |
|   mark   | 标记，调用mark()方法来设置mark=position，再调用reset()可以让position恢复到标记的位置 |



````java
    public final Buffer mark() {
        mark = position;
        return this;
    }

    public final Buffer reset() {
        int m = mark;
        if (m < 0)
            throw new InvalidMarkException();
        position = m;
        return this;
    }
````

