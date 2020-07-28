[toc]

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

。。。官方文档写得太抽象，很难懂。

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

#### Method

java.io.Buffer类是一个抽象类，不能被实例化。Buffer类的直接子类，如ByteBuffer等也是抽象类，所以也不能被实例化。

但是ByteBuffer提供了4个静态工厂方法来获取ByteBuffer的实例：



| Method                                          | discription                                                  |
| ----------------------------------------------- | ------------------------------------------------------------ |
| allocate(int capacity)                          | 从堆空间分配一个容量大小为capacity的byte数组作为缓冲区的byte数据存储器 |
| allocateDirect(int capacity)                    | 是不使用JVM堆栈而是通过操作系统来创建内存块用作缓冲区，它与当前操作系统能够更好的耦合，因此能进一步提高I/O操作速度。但是分配直接缓冲区的系统开销很大，因此只有在缓冲区较大并长期存在，或者需要经常重用时，才使用这种缓冲区 |
| wrap(byte[] array)                              | 这个缓冲区的数据会存放在byte数组中，bytes数组或buff缓冲区任何一方中数据的改动都会影响另一方。其实ByteBuffer底层本来就有一个bytes数组负责来保存buffer缓冲区中的数据，通过allocate方法系统会帮你构造一个byte数组 |
| wrap(byte[] array,<br/> int offset, int length) | 在上一个方法的基础上可以指定偏移量和长度，这个offset也就是包装后byteBuffer的position，而length呢就是limit-position的大小，从而我们可以得到limit的位置为length+position(offset) |

#### example

````java
public static void main(String args[]) throws FileNotFoundException {  
  
    System.out.println("----------Test allocate--------");  
    System.out.println("before alocate:"  
            + Runtime.getRuntime().freeMemory());  
      
    // 如果分配的内存过小，调用Runtime.getRuntime().freeMemory()大小不会变化？  
    // 要超过多少内存大小JVM才能感觉到？  
    ByteBuffer buffer = ByteBuffer.allocate(102400);  
    System.out.println("buffer = " + buffer);  
      
    System.out.println("after alocate:"  
            + Runtime.getRuntime().freeMemory());  
      
    // 这部分直接用的系统内存，所以对JVM的内存没有影响  
    ByteBuffer directBuffer = ByteBuffer.allocateDirect(102400);  
    System.out.println("directBuffer = " + directBuffer);  
    System.out.println("after direct alocate:"  
            + Runtime.getRuntime().freeMemory());  
      
    System.out.println("----------Test wrap--------");  
    byte[] bytes = new byte[32];  
    buffer = ByteBuffer.wrap(bytes);  
    System.out.println(buffer);  
      
    buffer = ByteBuffer.wrap(bytes, 10, 10);  
    System.out.println(buffer);   
}  
````

#### 另外一些常用的方法(不包含全部)

| Method                 | discription                                                  |
| ---------------------- | ------------------------------------------------------------ |
| clear()                | position = 0;limit = capacity;mark = -1; 有点初始化的味道，但是并不影响底层byte数组的内容 |
| flip()                 | limit = position;position = 0;mark = -1; 翻转，也就是让flip之后的position到limit这块区域变成之前的0到position这块，翻转就是将一个处于存数据状态的缓冲区变为一个处于准备取数据的状态 |
| get()                  | 相对读，从position位置读取一个byte，并将position+1，为下次读写作准备 |
| get(int index)         | 绝对读，读取byteBuffer底层的bytes中下标为index的byte，不改变position |
| put(byte b)            | 相对写，向position的位置写入一个byte，并将postion+1，为下次读写作准备 |
| put(int index, byte b) | 绝对写，向byteBuffer底层的bytes中下标为index的位置插入byte b，不改变position |

````java
public static void main(String args[]){  
  
    System.out.println("--------Test reset----------");  
    buffer.clear();  
    buffer.position(5);  
    buffer.mark();  
    buffer.position(10);  
    System.out.println("before reset:" + buffer);  
    buffer.reset();  
    System.out.println("after reset:" + buffer);  
  
    System.out.println("--------Test rewind--------");  
    buffer.clear();  
    buffer.position(10);  
    buffer.limit(15);  
    System.out.println("before rewind:" + buffer);  
    buffer.rewind();  
    System.out.println("before rewind:" + buffer);  
  
    System.out.println("--------Test compact--------");  
    buffer.clear();  
    buffer.put("abcd".getBytes());  
    System.out.println("before compact:" + buffer);  
    System.out.println(new String(buffer.array()));  
    buffer.flip();  
    System.out.println("after flip:" + buffer);  
    System.out.println((char) buffer.get());  
    System.out.println((char) buffer.get());  
    System.out.println((char) buffer.get());  
    System.out.println("after three gets:" + buffer);  
    System.out.println("\t" + new String(buffer.array()));  
    buffer.compact();  
    System.out.println("after compact:" + buffer);  
    System.out.println("\t" + new String(buffer.array()));  
  
    System.out.println("------Test get-------------");  
    buffer = ByteBuffer.allocate(32);  
    buffer.put((byte) 'a').put((byte) 'b').put((byte) 'c').put((byte) 'd')  
            .put((byte) 'e').put((byte) 'f');  
    System.out.println("before flip()" + buffer);  
    // 转换为读取模式  
    buffer.flip();  
    System.out.println("before get():" + buffer);  
    System.out.println((char) buffer.get());  
    System.out.println("after get():" + buffer);  
    // get(index)不影响position的值  
    System.out.println((char) buffer.get(2));  
    System.out.println("after get(index):" + buffer);  
    byte[] dst = new byte[10];  
    buffer.get(dst, 0, 2);  
    System.out.println("after get(dst, 0, 2):" + buffer);  
    System.out.println("\t dst:" + new String(dst));  
    System.out.println("buffer now is:" + buffer);  
    System.out.println("\t" + new String(buffer.array()));  
  
    System.out.println("--------Test put-------");  
    ByteBuffer bb = ByteBuffer.allocate(32);  
    System.out.println("before put(byte):" + bb);  
    System.out.println("after put(byte):" + bb.put((byte) 'z'));  
    System.out.println("\t" + bb.put(2, (byte) 'c'));  
    // put(2,(byte) 'c')不改变position的位置  
    System.out.println("after put(2,(byte) 'c'):" + bb);  
    System.out.println("\t" + new String(bb.array()));  
    // 这里的buffer是 abcdef[pos=3 lim=6 cap=32]  
    bb.put(buffer);  
    System.out.println("after put(buffer):" + bb);  
    System.out.println("\t" + new String(bb.array()));  
}  
````

