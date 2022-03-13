# LockSupport

**locaksupport**是一个线程阻塞工具类，所有的方法都是静态方法，可以让线程在程序的任意位置阻塞，又阻塞方法同时也就有唤醒的方法

````java
public static void park(Object blocker); // 暂停当前线程
public static void parkNanos(Object blocker, long nanos); // 暂停当前线程，不过有超时时间的限制
public static void parkUntil(Object blocker, long deadline); // 暂停当前线程，直到某个时间
public static void park(); // 无期限暂停当前线程
public static void parkNanos(long nanos); // 暂停当前线程，不过有超时时间的限制
public static void parkUntil(long deadline); // 暂停当前线程，直到某个时间
public static void unpark(Thread thread); // 恢复当前线程
public static Object getBlocker(Thread t);
````

特别要支持的是，park有停车的意思，而unpark则是让车启动的意思咯。这里很形象的将线程比作了一辆车，它的阻塞和唤醒就分别对应park和unpark方法



写一个例子，是先两个线程打印出1A2B3C4D5E6F7G的效果。

````java
import java.util.concurrent.locks.LockSupport;

public class Test {

    static Thread t1 , t2;

    public static void main(String[] args) {
        char[] a1 = "1234567".toCharArray();
        char[] c2 = "ABCDEFG".toCharArray();

        t1 = new Thread(() ->{
            for (char c : a1){
                System.out.print(c);
                LockSupport.unpark(t2);
                LockSupport.park();
            }
        });

        t2 = new Thread(() -> {
           for (char c : c2){
               LockSupport.park();
               System.out.print(c);
               LockSupport.unpark(t1);
           }
        });

        t1.start();
        t2.start();
    }
}
````



### 方法二

````java
public class Test {

    static Thread t1 , t2;

    public static void main(String[] args) {
        char[] a1 = "1234567".toCharArray();
        char[] c2 = "ABCDEFG".toCharArray();
        Object object = new Object();

        t1 = new Thread(() ->{
            synchronized (object){
                try {
                    for (char c : a1){
                        System.out.print(c);
                        object.notifyAll();
                        object.wait();
                    }
                    object.notifyAll();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        t2 = new Thread(() -> {
           synchronized (object){
               try {
                   for (char c : c2){
                       System.out.print(c);
                       object.notifyAll();
                       object.wait();
                   }
                   object.notifyAll();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }
        });

        t1.start();
        t2.start();
    }
}

思考一下，为什么最后要加object.notifyAll()。
````



这儿`park`和`unpark`其实实现了`wait`和`notify`的功能，不过还是有一些差别的。

1. `park`不需要获取某个对象的锁
2. 因为中断的时候`park`不会抛出`InterruptedException`异常，所以需要在`park`之后自行判断中断状态，然后做额外的处理

还有一个地方需要注意，相对于线程的`stop和resume`，`park和unpark`的先后顺序并不是那么严格。`stop和resume`如果顺序反了，会出现死锁现象。而`park和unpark`却不会。这又是为什么呢？还是看一个例子

````java
public class LockSupportDemo {

    public static Object u = new Object();
    static ChangeObjectThread t1 = new ChangeObjectThread("t1");

    public static class ChangeObjectThread extends Thread {

        public ChangeObjectThread(String name) {
            super(name);
        }

        @Override public void run() {
            synchronized (u) {
                System.out.println("in " + getName());
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LockSupport.park();
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("被中断了");
                }
                System.out.println("继续执行");
            }
        }
    }

    public static void main(String[] args) {
        t1.start();
        LockSupport.unpark(t1);
        System.out.println("unpark invoked");
    }
}
````



# 总结

`park和unpark`可以实现类似`wait和notify`的功能，但是并不和`wait和notify`交叉，也就是说`unpark`不会对`wait`起作用，`notify`也不会对`park`起作用。

`park和unpark`的使用不会出现死锁的情况

