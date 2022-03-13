# Semaphore实现一个限流器

信号量是由大名鼎鼎的计算机科学家迪杰斯特拉（Dijkstra）于 1965 年提出，在这之后的 15 年，信号量一直都是并发编程领域的终结者，直到 1980 年管程被提出来，我们才有了第二选 择。目前几乎所有支持并发编程的语言都支持信号量机制，所以学好信号量还是很有必要的。

## 信号量模型

信号量模型还是很简单的，可以简单概括为：一个计数器，一个等待队列，三个方法。在信号量 模型里，计数器和等待队列对外是透明的，所以只能通过信号量模型提供的三个方法来访问它 们，这三个方法分别是：init()、down() 和 up()。你可以结合下图来形象化地理解。

![](./img/semaphorelimit/1.png)

这三个方法详细的语义具体如下所示。

* init()：设置计数器的初始值。
* down()：计数器的值减 1；如果此时计数器的值小于 0，则当前线程将被阻塞，否则当前线程 可以继续执行。
* up()：计数器的值加 1；如果此时计数器的值小于或者等于 0，则唤醒等待队列中的一个线 程，并将其从等待队列中移除。

简单的理解就是计数器表示一个并发数n，允许同一时刻有n个线程访问被up()与donw()修饰的代码块。如果此时一个线程调用down方法时，计数器的值小于0，则该线程被阻塞，进入等待队列。如果计数器大于等于0，则会唤醒等待队列中的一个线程。

## 对象池

所谓对象池，指的是一次性创建出 N 个 对象，之后所有的线程重复利用这 N 个对象，当然对象在被释放前，也是不允许其他线程使用 的。对象池，可以用 List 保存实例对象，这个很简单。但关键是限流器的设计，这里的限流，指的是不允许多于 N 个线程同时进入临界区。那如何快速实现一个这样的限流器呢？可是使用semaphore来实现。

比如医院有10个医生，1000个病人，但是10个医生一次性只能给10个病人看病，这个时候，我们就可以利用对象池来做限制，限制医生与病人的一一对应关系。

```java
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class ObjectPool<T,R> {


    private Vector<T> vector;

    private Semaphore semaphore;

    private Lock lock;

    private Integer objectNum;

    public ObjectPool(int currentNum){
        vector = new Vector<>(currentNum);
        semaphore = new Semaphore(currentNum);
        lock = new ReentrantLock();
        objectNum = currentNum;
    }

    public synchronized void addObject(T t){
        if (vector.size() <= objectNum){
            vector.add(t);
        }
    }

    public R doTask(Function<T,R> function){
        R result = null;
        T obj = null;
        try {
            semaphore.acquire();
            obj = vector.remove(0);
            result = function.apply(obj);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            vector.add(obj);
            semaphore.release();
        }
        return result;
    }


    public static void main(String[] args) throws InterruptedException {

        ObjectPool<Doctor,Object> objectPool = new ObjectPool(10);
        for (int i = 0;i < 10;i++){
            objectPool.addObject(new Doctor());
        }

        for (int i = 0;i < 1000;i++){
            new Thread(() -> {
                objectPool.doTask((doctor) -> {
                    return doctor.Cure(new Object());
                });
            });
        }

    }

}
```



```java
public class Doctor {
    
    public Object Cure(Object patient){
        System.out.println("医生给" + patient.hashCode() + "号病人看病");
        Object result = new Object();
        return result;
    }
    
}
```

