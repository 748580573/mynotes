# AQS源码解读（番外篇）——四种自旋锁原理详解（Java代码实现SpinLock、TicketSpinLock、CLH、MCS）

## 何为自旋锁
自旋锁是为实现保护共享资源而提出的一种锁机制。自旋锁与Java中的synchronized和Lock不同，不会引起调用线程阻塞睡眠。如果有线程持有自旋锁，调用线程就会一直循环检测锁的状态，直到其他线程释放锁，调用线程才停止自旋，获取锁。

## 自旋锁的优势和缺陷
自旋锁的优点很明显：自旋锁不会使线程状态进行切换，一直处于用户态，即不会频繁产生上下文切换，执行速度快，性能高。

正是因为其不进行上下文切换的优点，也使得某些情况下，缺陷也很明显：如果某个线程持有锁的时间过长，或者线程间竞争激烈，就会导致某些等待获取锁的线程进入长时间循环等待，消耗CPU，从而造成CPU占用率极高。

## 自旋锁的适用场景
自旋锁适用于被锁代码块执行时间很短，即加锁时间很短的场景。

## 常见自旋锁实现
比较有名的四种自旋锁：传统自旋锁SpinLock，排队自旋锁TicketSpinLock，CLH自旋锁，MCS自旋锁。这四种自旋锁的基本原理都是在CAS的基础上实现的，各有各的特点，且逐步优化。

## SpinLock传统自旋锁的优势和不足
### 实现原理
SpinLock原理很简单，多个线程循环CAS修改一个共享变量，修改成功则停止自旋获取锁。

### 代码实现
实现接口参考了Java的Lock，核心方法在tryAcquire和tryRelease。获取锁的方式实现了自旋，可中断自旋，自旋超时中断，不自旋。共享变量state不仅作为锁的状态标志（state=0锁空闲，state>0有线程持有锁），同时可作为自旋锁重入的次数。exclusiveOwnerThread记录当前持有锁的线程。

````java
public class SpinLock implements Lock {
    protected volatile int state = 0;
    private volatile Thread exclusiveOwnerThread;
    @Override
    public void lock() {
        for(;;) {
            //直到获取锁成功，才结束循环
            if (tryAcquire(1)) {
                return;
            }
        }
    }
    @Override
    public void lockInterruptibly() throws InterruptedException {
        for(;;) {
            if (Thread.interrupted()) {
                //有被中断  抛异常
                throw new InterruptedException();
            }
            if (tryAcquire(1)) {
                return;
            }
        }
    }

    /**
     * 返回获取锁的结果，不会自旋
     * @return
     */
    @Override
    public boolean tryLock() {
        return tryAcquire(1);
    }

    /**
     * 返回获取自旋锁的结果，会自旋一段时间，超时后停止自旋
     * @param time
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long nanosTimeout = unit.toNanos(time);
        if (nanosTimeout <= 0L) {
           return false;
        }
        final long deadline = System.nanoTime() + nanosTimeout;
        for(;;) {
            if (Thread.interrupted()) {
                //有被中断  抛异常
                throw new InterruptedException();
            }
            if (tryAcquire(1)) {
                return true;
            }
            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L) {
                //超时自旋，直接返回false
                return false;
            }
        }
    }

    @Override
    public void unlock() {
        tryRelease(1);
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    public int getState() {
        return state;
    }

    /**
     * 获取持有锁的当前线程
     * @return
     */
    public Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }

    /**
     * 获取当前线程重入次数
     * @return
     */
    public int getHoldCount() {
        return isHeldExclusively() ? getState() : 0;
    }

    /**
     * 释放锁
     * @param releases
     * @return
     */
    protected boolean tryRelease(int releases) {
        int c = getState() - releases;
        Thread current = Thread.currentThread();
        if (current != getExclusiveOwnerThread())
            //不是当前线程，不能unLock 抛异常
            throw new IllegalMonitorStateException();
        boolean free = false;
        if (c <= 0) {
            //每次减一，c = 0,证明没有线程持有锁了，可以释放了
            free = true;
            c = 0;
            setExclusiveOwnerThread(null);
            System.out.println(String.format("spin un lock ok, thread=%s;", current.getName()));
        }
        //排它锁，只有当前线程才会走到这，是线程安全的  修改state
        setState(c);
        return free;
    }
    /**
     * 获取锁
     * @param acquires
     * @return
     */
    protected boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) {
            //若此时锁空着，则再次尝试抢锁
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                System.out.println(String.format("spin lock ok, thread=%s;", current.getName()));
                return true;
            }
        }
        //若当前持锁线程是当前线程(重入性)
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            //重入
            setState(nextc);
            System.out.println(String.format("spin re lock ok, thread=%s;state=%d;", current.getName(), getState()));
            return true;
        }
        return false;
    }
    /**
     * 判断当前线程是否持有锁
     * @return
     */
    protected final boolean isHeldExclusively() {
        return getExclusiveOwnerThread() == Thread.currentThread();
    }

    protected void setState(int state) {
        this.state = state;
    }

    protected void setExclusiveOwnerThread(Thread exclusiveOwnerThread) {
        this.exclusiveOwnerThread = exclusiveOwnerThread;
    }

    protected final boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }
    protected static final Unsafe getUnsafe() {
        try {
            //不可以直接使用Unsafe，需要通过反射，当然也可以直接使用atomic类
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            if (unsafe != null) {
                return unsafe;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("get unsafe is null");
    }
    private static final Unsafe unsafe = getUnsafe();
    private static final long stateOffset;
    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                    (SpinLock.class.getDeclaredField("state"));
        } catch (Exception ex) { throw new Error(ex); }
    }

````

### SpinLock的特点

* 优势：SpinLock实现原理简单，线程间没有频繁的上下文切换，执行速度快，性能高。
* 缺陷：SpinLock是不公平的，无法满足等待时间最长的线程优先获取锁，造成 “线程饥饿”。
* 缺陷：由于每个申请自旋锁的处理器均在一个全局变量上自旋检测，系统总线将因为处理器间的缓存同步而导致繁重的流量，从而降低了系统整体的性能。

由于传统自旋锁无序竞争的本质特点，内核执行线程无法保证何时可以取到锁，某些执行线程可能需要等待很长时间，导致“不公平”问题的产生。有两个方面的原因：

1. 随着处理器个数的不断增加，自旋锁的竞争也在加剧，自然导致更长的等待时间。
2. 释放自旋锁时的重置操作将使所有其它正在自旋等待的处理器的缓存无效化，那么在处理器拓扑结构中临近自旋锁拥有者的处理器可能会更快地刷新缓存，因而增大获得自旋锁的机率。

## TicketSpinLock排队自旋锁的优化与不足
由于SpinLock传统自旋锁是不公平的，且在锁竞争激烈的服务器，”不公平“问题尤为严重。因此公平的排队自旋锁就应运而生了。（ Linux 内核开发者 Nick Piggin 在 Linux 内核 2.6.25 版本中引入了排队自旋锁，并不是他发明的排队自旋锁，排队自旋锁只是一种思想，Windows中排队自旋锁采取了不一样的实现逻辑。）

### 实现原理
排队自旋锁通过保存执行线程申请锁的顺序信息来解决“不公平”问题。TicketSpinLock仍然使用原有SpinLock的数据结构，为了保存顺序信息，加入了两个新变量，分别是锁需要服务的序号(serviceNum)和未来锁申请者的票据序号(ticketNum)。当serviceNum=ticketNum时，代表锁空闲，线程可以获取锁。

### 代码实现
基本共享变量serviceNum、ticketNum，辅助变量threadOwnerTicketNum、state、exclusiveOwnerThread。

线程获取锁，获取排队序号ticketNum，并自增排队序号，自旋比较获取的排队序号和当前服务序号是否相等（serviceNum != myTicketNum），相等则停止自旋，获取锁。

threadOwnerTicketNum变量不是必须的，但是如果要实现重入锁，是必不可少的，用于记录每个线程持有的排队序号。当检测线程持有的排队序号为空时，可获取排队序号，如果不为空，则此时有其他线程持有锁。判断持有锁的线程是否为当前线程，是则重入。

state和exclusiveOwnerThread用于重入锁的实现，但是并不能代表锁的持有状态（可能有瞬时性）。

线程释放锁，因为是重入锁，需要state自减为0时，serviceNum才自增加1。

因为serviceNum、state、exclusiveOwnerThread的操作环境是天生线性安全的，所以不需要CAS。
````java
public class TicketSpinLock {
    //服务序号，不需要cas，因为释放锁的只有一个线程，serviceNum++的环境是天生安全的
    private volatile int serviceNum = 0;
    //排队序号，cas
    private AtomicInteger ticketNum = new AtomicInteger(0);
    //记录当前线程的排队号，主要的作用是为了实现可重入，防止多次取号
    private ThreadLocal<Integer> threadOwnerTicketNum = new ThreadLocal<Integer>();
    //state不作为锁状态标志，只代表锁重入的次数
    protected volatile int state = 0;
    private volatile Thread exclusiveOwnerThread;
    public void lock() {
        final Thread current = Thread.currentThread();
        Integer myTicketNum = threadOwnerTicketNum.get();
        if (myTicketNum == null) {
            myTicketNum = ticketNum.getAndIncrement();
            threadOwnerTicketNum.set(myTicketNum);
            while (serviceNum != myTicketNum) {}
            //若拿的排队号刚好等于服务序号，说明可以获取锁，即获取锁成功
            setExclusiveOwnerThread(current);
            state ++ ;
            System.out.println(String.format("ticket lock ok, thread=%s;state=%d;serviceNum=%d;next-ticketNum=%d;", current.getName(), getState(), serviceNum, ticketNum.get()));
            return;
        }
        //若已经取号，判断当前持锁线程是当前线程(重入性)
        if (current == getExclusiveOwnerThread()) {
            //重入
            state++;
            System.out.println(String.format("ticket re lock ok, thread=%s;state=%d;serviceNum=%d;next-ticketNum=%d;", current.getName(), getState(), serviceNum, ticketNum.get()));
            return;
        }
    }
    public void unlock() {
        if (Thread.currentThread() != getExclusiveOwnerThread())
            //不是当前线程，不能unLock 抛异常
            throw new IllegalMonitorStateException();
        state--;
        if (state == 0) {
            //完全释放锁，owner+1
            //服务序号是线性安全的，无需cas
            threadOwnerTicketNum.remove();
            setExclusiveOwnerThread(null);
            serviceNum ++;
            System.out.println(String.format("ticket un lock ok, thread=%s;next-serviceNum=%d;ticketNum=%d;", Thread.currentThread().getName(), serviceNum, ticketNum.get()));
        }
    }

    public int getState() {
        return state;
    }

    public Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }

    public void setExclusiveOwnerThread(Thread exclusiveOwnerThread) {
        this.exclusiveOwnerThread = exclusiveOwnerThread;
    }
}
````

### TicketSpinLock的特点
TicketSpinLock是公平锁，基本解决了传统自旋锁“不公平”问题，但是并没有解决处理器缓存同步问题。

在大规模多处理器系统和 NUMA系统中，排队自旋锁（包括传统自旋锁）同样存在一个比较严重的性能问题：由于执行线程均在同一个共享变量上自旋，将导致所有参与排队自旋锁操作的处理器的缓存变得无效。如果排队自旋锁竞争比较激烈的话，频繁的缓存同步操作会导致系统总线和处理器内存的流量繁重，从而大大降低了系统整体的性能。
CLH队列自旋锁的优化与不足
CLH（Craig, Landin, and Hagersten）锁是基于链表实现的FIFO队列公平锁。CLH是其三个发明者的人名缩写（Craig, Landin, and Hagersten）。

## CLH队列自旋锁的优化与不足

CLH（Craig, Landin, and Hagersten）锁是基于链表实现的FIFO队列公平锁。CLH是其三个发明者的人名缩写（Craig, Landin, and Hagersten）。

### 实现原理
获取锁的线程先入队列，入到队列尾部后不断自旋检查前驱节点的状态，前驱为空 or检测到前驱释放锁则该节点获取锁。入队列尾部是CAS操作，保证了有序出入队列。

节点获取锁的条件：前驱为空or检测到前驱释放锁。

### 代码实现
CLH锁只是一种思想，实现的方式很多，网上有基于隐式链表实现的，即节点与节点之间不是真实连接，只是当前线程记录了前驱节点和自己的节点。

如下代码实现的链表是真实连接的，即线程当前节点有前驱指针的变量（prev）。节点除了有前驱指针外还有一个locked变量记录节点锁持有状态，locked=true代表线程节点正在持有锁，或者需要锁，初始线程节点locked为true，释放锁后将locked改为false，以让其后继自旋感知前驱释放锁了，并停止自旋获取锁。
````java
public class CLHSpinLock {
    class Node {
        volatile Node prev;
        /**
         * true表示正在持有锁，或者需要锁
         * false表示释放锁
         */
        volatile boolean locked = true;
        volatile Thread thread;
        Node(Thread thread) {
            this.thread = thread;
        }

        boolean isPrevLocked() {
            return  prev != null && prev.locked;
        }
        String getPrevName() {
            return prev == null ? "null" : prev.thread.getName();
        }
    }

    private volatile AtomicReference<Node> tail = new AtomicReference<Node>();
    //线程和node key-value
    private ThreadLocal<Node> threadNode = new ThreadLocal<Node>();
    //记录持有锁的当前线程
    private volatile Thread exclusiveOwnerThread;
    //记录重入
    protected volatile int state = 0;
    //因为exclusiveOwnerThread和state只是作为记录，线程获取锁后才会设置这两个值，具有有瞬时性，所以不能作为锁是否空闲的判断标志

    public Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }

    public void setExclusiveOwnerThread(Thread exclusiveOwnerThread) {
        this.exclusiveOwnerThread = exclusiveOwnerThread;
    }

    /**
         * cas自旋入队列->尾部
         * @return
         */
    Node enq() {
        Node node = new Node(Thread.currentThread());
        threadNode.set(node);
        for (;;) {
            Node prev = tail.get();
            //cas设置tail指针指向node
            if (tail.compareAndSet(prev, node)) {
                node.prev = prev;
                return node;
            }
        }
    }

    public void lock() {
        Node node = threadNode.get();
        if (node != null && getExclusiveOwnerThread() != null && node.thread == getExclusiveOwnerThread()) {
            /**
             * 一般情况node != null，说明有同一个线程已经调用了lock()
             * 判断持有锁的线程是node.thread，重入
             */
            state++;
            System.out.println(String.format("re lock thread=%s;state=%d;", node.thread.getName(), state));
            return;
        }
        node = enq();
        while (node.isPrevLocked()) {

        }
        //前驱未持有锁，说明可以获取锁，即获取锁成功, prev设置为null，断开与链表的连接，相当于前驱出队列
        System.out.println(String.format("clh get lock ok, thread=%s;prev=%s;", node.thread.getName(), node.getPrevName()));
        setExclusiveOwnerThread(node.thread);
        state++;
        node.prev = null;
    }

    public void unlock() {
        Node node = threadNode.get();
        if (node.thread != getExclusiveOwnerThread()) {
            throw new IllegalMonitorStateException();
        }
        //在node.setLocked(false) 之前设置 state
        --state;
        //完全释放锁，locked改为false，让其后继感知前驱锁释放并停止自旋
        if (state == 0) {
            System.out.println(String.format("clh un lock ok, thread=%s;", node.thread.getName()));
            setExclusiveOwnerThread(null);
            node.locked = false;
            threadNode.remove();
            node = null; //help gc
   }
}
````

## CLH锁的特点
CLH锁是公平的，且空间复杂度是常数级。在一定程度上减轻了排队自旋锁和传统自旋锁在同一个共享变量上自旋的问题，但是并不彻底。

CLH在SMP系统结构（每个cpu缓存一致）下是非常有效的。但在NUMA系统结构（每个cpu有各自的缓存）下，每个线程有自己的内存，如果前驱节点的内存位置比较远，自旋判断前驱节点的locked状态，性能将大打折扣。

### MCS锁对CLH锁的优化
因为CLH前驱节点的内存位置可能较远，在NUMA系统结构下导致自旋判断前驱节点的locked状态的性能很低，所以一种解决NUMA系统结构的思路就是MCS队列锁。MCS也是其发明者人名缩写（ John M. Mellor-Crummey 和 Michael L. Scott）。

### 实现原理
MCS和CLH区别在于，MCS是对自己节点的锁状态不断自旋，当前驱为空即队列中只有自己一个节点或者检测到自己节点锁状态可以获取锁，则线程获取锁。

### 代码实现
MCS入队列方式，代码中实现了两种，一种是节点有前驱指针（enq()），这样MCS中的链表队列就是双向队列，另一种是入队列后返回前驱节点（enq1()），这样节点的前驱指针就是隐式的。

内部类Node中prev属性可有可无，next必须，节点释放锁时需要主动通知后继。locked代表锁持有状态，locked=false 代表线程未持有锁，locked=true代表线程可持有锁，初始节点locked=false。

获取锁，线程先入队列尾部，并检查前驱是否为空，为空则停止自旋获取锁，不为空判断当前节点的locked是否为true，为true停止自旋获取锁。

释放锁，当前节点将后继节点的locked改为true，以让后继感知到自己的锁状态是可以获取锁了。如果当前节点后继为空，则自旋清空队列。
````java
public class MCSSpinLock {
    class Node {
        //prev 可有可无
        volatile Node prev;
        volatile Node next;
        //false代表未持有锁，true代表可持有锁
        volatile boolean locked = false;
        volatile Thread thread;
        Node(Thread thread) {
            this.thread = thread;
        }

        public boolean shouldLocked() {
            return prev == null || locked;
        }

        public String getNextName() {
            return next == null ? "null" : next.thread.getName();
        }
    }
    private volatile AtomicReference<Node> tail = new AtomicReference<Node>();
    //线程和node key-value
    private ThreadLocal<Node> threadNode = new ThreadLocal<Node>();
    //记录持有锁的当前线程
    private volatile Thread exclusiveOwnerThread;
    //记录重入
    protected volatile int state = 0;
    //因为exclusiveOwnerThread和state只是作为记录，线程获取锁后才会设置这两个值，具有有瞬时性，所以不能作为锁是否空闲的判断标志


    public Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }

    public void setExclusiveOwnerThread(Thread exclusiveOwnerThread) {
        this.exclusiveOwnerThread = exclusiveOwnerThread;
    }
    /**
     * cas自旋入队列->尾部
     * @return
     */
    Node enq() {
        Node node = new Node(Thread.currentThread());
        threadNode.set(node);
        for (;;) {
            Node t = tail.get();
            if (tail.compareAndSet(t, node)) {
                if (t != null) {
                    t.next = node;
                    node.prev = t;
                }
                return node;
            }
        }
    }

    /**
     *
     * @return 返回前驱
     */
    Node enq1() {
        Node node = new Node(Thread.currentThread());
        threadNode.set(node);
        Node prev = tail.getAndSet(node);
        if (prev != null) {
            prev.next = node;
        }
        return prev;
    }

    public void lock() {
        Node node = threadNode.get();
        if (node != null && getExclusiveOwnerThread() != null && node.thread == getExclusiveOwnerThread()) {
            /**
             * 一般情况node != null，说明有同一个线程已经调用了lock()
             * 持有锁的线程是node.thread，重入
             */
            state++;
            System.out.println("re lock thread=" + node.thread.getId() + "state=" + state);

            return;
        }
        node = enq();
        while (!node.shouldLocked()) {}
        //判断node是否应该获取锁，若prev == null or node.locked=true，代表应该获取锁。则结束自旋
        if (!node.locked) {
            //当前驱为空时的情况，不过不改也问题不大
            node.locked = true;
        }
        state++;
        setExclusiveOwnerThread(node.thread);
        System.out.println(String.format("mcs get lock ok, thread=%s;locked=%b;node==tail=%b;next=%s;", node.thread.getName(), node.locked, node == tail.get(), node.getNextName()));
    }

    public void lock1() {
        Node node = threadNode.get();
        if (node != null && getExclusiveOwnerThread() != null && node.thread == getExclusiveOwnerThread()) {
            /**
             * 一般情况node != null，说明有同一个线程已经调用了lock()
             * 持有锁的线程是node.thread，重入
             */
            state++;
            System.out.println("re lock thread=" + node.thread.getId() + "state=" + state);

            return;
        }
        Node prev = enq1();
        node = threadNode.get();
        while (prev != null && !node.locked) {}
        //判断node是否应该获取锁，若prev == null or node.locked=true，代表应该获取锁。则结束自旋
        if (!node.locked) {
            //当前驱为空时的情况，不过不改也问题不大
            node.locked = true;
        }
        state++;
        setExclusiveOwnerThread(node.thread);
        System.out.println(String.format("mcs get lock ok, thread=%s;locked=%b;node==tail=%b;next=%s;", node.thread.getName(), node.locked, node == tail.get(), node.getNextName()));
    }

    public void unlock() {
        Node node = threadNode.get();
        if (node.thread != getExclusiveOwnerThread()) {
            throw new IllegalMonitorStateException();
        }
        //在node.setLocked(false) 之前设置 state
        state--;
        //完全释放锁，将前驱 locked改为false，让其后继感知锁空闲并停止自旋
        if (state != 0) {
            return;
        }
        //后继为空，则清空队列，将tail  cas为null，
        //如果此时刚好有节点入队列则退出循环，继续主动通知后继
        while (node.next == null) {
            if (tail.compareAndSet(node, null)) {
                //设置 tail为 null,threadNode remove
                threadNode.remove();
                System.out.println(String.format("mcs un lock ok, thread=%s;clear queue", node.thread.getName()));
                return;
            }
        }
        //threadNode 后继不为空 设置后继的locked=true，主动通知后继获取锁
        System.out.println(String.format("mcs un lock ok, thread=%s;next-thread=%s;", node.thread.getName(), node.getNextName()));
        //在node.next.locked前，设置setExclusiveOwnerThread 为null
        setExclusiveOwnerThread(null);
        node.next.locked = true;
        threadNode.remove();
        node = null; //help gc
    }
}
````

### MCS锁的特点

MCS锁是公平的，且空间复杂度是常数级。彻底解决了CLH锁、排队自旋锁和传统自旋锁在同一个共享变量上自旋的问题，所以MCS锁在没有处理器缓存一致性协议保证的系统中也能很好地工作。

## SMP和NUMP简介

### SMP(Symmetric Multi-Processor)

对称多处理器结构，所有的CPU共享全部资源，如总线，内存和I/O系统等。

* 多个CPU之间没有区别，平等地访问内存、外设等共享资源，每个 CPU 访问内存中的任何地址所需时间相同。因此 SMP 也被称为一致存储器访问结构 (UMA ： Uniform Memory Access) 。
* 如果多个处理器同时请求访问一个资源（例如同一段内存地址），由硬件、软件的锁机制去解决资源争用问题。
* SMP 性能扩展有限，每一个共享的环节都可能造成 SMP 服务器扩展时的瓶颈，而最受限制的则是内存。由于每个 CPU 必须通过相同的内存总线访问相同的内存资源，因此随着 CPU 数量的增加，内存访问冲突将迅速增加，最终会造成 CPU 资源的浪费，使 CPU 性能的有效性大大降低。



### NUMA(Non-Uniform Memory Access)

非一致存储访问结构，具有多个 CPU 模块，每个 CPU 模块由多个 CPU组成，并且具有独立的本地内存、 I/O 槽口等等，模块之间可以通过互联模块 ( 如称为 Crossbar Switch) 进行连接和信息交互，因此每个 CPU 可以访问整个系统的内存。

显然，访问本地内存的速度将远远高于访问远地内存 ( 系统内其它节点的内存 ) 的速度，这也是非一致存储访问 NUMA 的由来。由于这个特点，为了更好地发挥系统性能，开发应用程序时需要尽量减少不同 CPU 模块之间的信息交互。

利用 NUMA 技术，可以较好地解决原来 SMP 系统的扩展问题，在一个物理服务器内可以支持上百个 CPU 。但 NUMA 技术同样有一定缺陷，由于访问远地内存的延时远远超过本地内存，因此当 CPU 数量增加时，系统性能无法线性增加。
## 总结

* 自旋锁，线程间不会进行上下文切换（即用户态和内核态间切换），执行速度快，性能高。
* 自旋锁适用于线程竞争少，被锁代码执行时间短的情况。
* 传统自旋锁是非公平锁，线程竞争激烈会导致“不公平”问题加重，出现“线程饥饿”情况。
  排队自旋锁是公平的，但是其和传统自旋锁都有一个致命的缺陷，在NUMP系统结构下，多个线程自旋同一个全局变量，导致处理器缓存频繁失效刷新，竞争
* 激烈的情况下系统总线和处理器流量加大，使得系统整体性能降低。
* CLH是基于链表实现的FIFO队列公平锁，其不断自旋检测前驱节点的锁状态，以判断当前节点是否应该获取锁。在NUMA系统结构（每个cpu有各自的缓存）下，如果前驱节点的内存位置比较远，自旋判断前驱节点的locked状态，性能将大打折扣。
* MCS也是基于链表实现的FIFO队列公平锁，其不断自旋检测自己节点的锁状态，判断是否应该获取锁。因为是自旋自己本地的节点的变量，所以不存在CLH因为前驱内存位置较远导致自旋性能折扣问题。

四种自旋锁实现代码都经过测试，如有问题请评论留言，我会及时改正。实际使用中不要打印日志，频繁的io操作会影响锁的性能。

> 摘自：https://blog.csdn.net/weixin_36586120/article/details/108817678
