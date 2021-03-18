# 自己动手写一个Callable

在多线程的计算中，我们可以实现Runnable接口从来启用一个线程，就像下面这样。

````java
public class MyThread implements Runnable {
    @Override
    public void run() {
        //此处省略一万行代码
        System.out.println("hello");
    }

    public static void main(String[] args) {
        MyThread myThread = new MyThread();
        Thread thread = new Thread(myThread);
        thread.start();
    }
}
````

不过这样的实现方式，没有返回值，对于一些具有延时性的任务，使用这样的线程实现方式就不是很妥当了，比如我们需要计算近十年来的天气变化情况，找出十年来平均气温。对于这样的需求，需要获得计算的结果，如果直接用Runnable接口实现就会显得比较繁琐。因此我们可以实现Callable接口来满足上面需求的计算需要。

````java
import java.util.concurrent.*;

public class MyThread implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        //此处省略一万行代码
        return 30;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        MyThread myThread = new MyThread();
        Future<Integer> future = executorService.submit(myThread);
        future.get();
    }
}

````

我们可以通过future.get()方法来等待运算的结果。Callable接口不仅能满足运算时间长的任务，同时也可以使得线程由异步变为同步。

那么我们可不可以自己动手来实现一个Callable呢？下面本文给出了一个简单实现方式。

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class MyCallable<T>  {

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private T result = null;

    public abstract T call();


    public void start(){

        Runnable runnable = () -> {
            result = call();
            lock.lock();
            condition.signalAll();
            lock.unlock();
        };
        executorService.submit(runnable);
        executorService.shutdown();
    }

    public T get(){
        lock.lock();
        try {
            if (result != null){
                return result;
            }
            condition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return result;
    }

    public static void main(String[] args) {
        MyCallable<Integer> myCallable = new MyCallable<Integer>() {
            @Override
            public Integer call() {
                try {
                    //模拟长时间计算
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return 2;
            }
        };

        myCallable.start();

        System.out.println(myCallable.get());
    }


}
```

上文中只给出了MyCallable的简单实现，类似Future的其他方法，后面有空再来补充

````java
package java.util.concurrent;

public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    
    boolean isCancelled();

    boolean isDone();

    V get() throws InterruptedException, ExecutionException;

    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}

````

