# Spring编程式事务源码深度解析，理解spring事务的本质

## 环境

1. jdk1.8
2. Spring版本：5.2.3.RELEASE
3. mysql5.7

## 回顾一下编程式事务用法

```java
@Test
public void test1() throws Exception {
    //定义一个数据源
    org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
    dataSource.setDriverClassName("com.mysql.jdbc.Driver");
    dataSource.setUrl("jdbc:mysql://localhost:3306/javacode2018?characterEncoding=UTF-8");
    dataSource.setUsername("root");
    dataSource.setPassword("root123");
    dataSource.setInitialSize(5);
    //定义一个JdbcTemplate，用来方便执行数据库增删改查
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    //1.定义事务管理器，给其指定一个数据源（可以把事务管理器想象为一个人，这个人来负责事务的控制操作）
    PlatformTransactionManager platformTransactionManager = new DataSourceTransactionManager(dataSource);
    //2.定义事务属性：TransactionDefinition，TransactionDefinition可以用来配置事务的属性信息，比如事务隔离级别、事务超时时间、事务传播方式、是否是只读事务等等。
    TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
    //3.获取事务：调用platformTransactionManager.getTransaction开启事务操作，得到事务状态(TransactionStatus)对象
    TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
    //4.执行业务操作，下面就执行2个插入操作
    try {
        System.out.println("before:" + jdbcTemplate.queryForList("SELECT * from t_user"));
        jdbcTemplate.update("insert into t_user (name) values (?)", "test1-1");
        jdbcTemplate.update("insert into t_user (name) values (?)", "test1-2");
        //5.提交事务：platformTransactionManager.commit
        platformTransactionManager.commit(transactionStatus);
    } catch (Exception e) {
        //6.回滚事务：platformTransactionManager.rollback
        platformTransactionManager.rollback(transactionStatus);
    }
    System.out.println("after:" + jdbcTemplate.queryForList("SELECT * from t_user"));
}
```

## 编程式事务过程

编程式事务过程，我们简化了一下，如下：

```java
1、定义事务属性信息：TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
2、定义事务管理器：PlatformTransactionManager platformTransactionManager = new DataSourceTransactionManager(dataSource);
3、获取事务：TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
4、执行sql操作：比如上面通过JdbcTemplate的各种方法执行各种sql操作
5、提交事务(platformTransactionManager.commit)或者回滚事务(platformTransactionManager.rollback)
```

下面通过源码来解析上面4步操作，带大家理解原理。

## 1、定义事务属性信息(TransactionDefinition)

事务启动的过程中需要定义事务的一些配置信息，如：事务传播行为、隔离级别、超时时间、是否是只读事务、事务名称，spring中使用**TransactionDefinition接口**表示事务定义信息，下面看一下TransactionDefinition接口源码，主要有5个信息

- 事务传播行为
- 事务隔离级别
- 事务超时时间
- 是否是只读事务
- 事务名称

```java
public interface TransactionDefinition {
    //传播行为:REQUIRED
    int PROPAGATION_REQUIRED = 0;
    //传播行为:REQUIRED
    int PROPAGATION_SUPPORTS = 1;
    //传播行为:REQUIRED
    int PROPAGATION_MANDATORY = 2;
    //传播行为:REQUIRED
    int PROPAGATION_REQUIRES_NEW = 3;
    //传播行为:REQUIRED
    int PROPAGATION_NOT_SUPPORTED = 4;
    //传播行为:REQUIRED
    int PROPAGATION_NEVER = 5;
    //传播行为:REQUIRED
    int PROPAGATION_NESTED = 6;
    //默认隔离级别
    int ISOLATION_DEFAULT = -1;
    //隔离级别：读未提交
    int ISOLATION_READ_UNCOMMITTED = 1;
    //隔离级别：读已提交
    int ISOLATION_READ_COMMITTED = 2;
    //隔离级别：可重复读
    int ISOLATION_REPEATABLE_READ = 4;
    //隔离级别：序列化的方式
    int ISOLATION_SERIALIZABLE = 8;
    //默认超时时间
    int TIMEOUT_DEFAULT = -1;
    //返回事务传播行为，默认是REQUIRED
    default int getPropagationBehavior() {
        return PROPAGATION_REQUIRED;
    }
    //返回事务的隔离级别
    default int getIsolationLevel() {
        return ISOLATION_DEFAULT;
    }
    //返回事务超时时间(秒)
    default int getTimeout() {
        return -1;
    }
    //是否是只读事务
    default boolean isReadOnly() {
        return false;
    }
    //获取事务名称
    @Nullable
    default String getName() {
        return null;
    }
    //获取默认的事务定义信息
    static TransactionDefinition withDefaults() {
        return StaticTransactionDefinition.INSTANCE;
    }
}
```

TransactionDefinition接口的实现类，比较多，我们重点关注用的比较多的2个

- **DefaultTransactionDefinition**：TransactionDefinition接口的默认的一个实现，编程式事务中通常可以使用这个
- **RuleBasedTransactionAttribute**：声明式事务中用到的是这个，这个里面对于事务回滚有一些动态匹配的规则，稍后在声明式事务中去讲。

![](./imgs/1.png)

编程式事务中通常使用DefaultTransactionDefinition，如下：

```java
DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();//设置事务传播行为transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);//设置事务隔离级别transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);//设置是否是只读事务transactionDefinition.setReadOnly(true);//设置事务超时时间(s)，事务超过了指定的时间还未结束，会抛异常transactionDefinition.setTimeout(5);//设置事务名称，这个名称可以随便设置，不过最好起个有意义的名字，在debug的过程中会输出transactionDefinition.setName("class完整类名.方法名称");
```

下面进入第2步，定义事务管理器。

## 2、定义事务管理器(PlatformTransactionManager)

事务管理器，这是个非常重要的角色，可以把这货想象为一个人，spring就是靠这个人来管理事务的，负责：**获取事务、提交事务、回滚事务**，Spring中用**PlatformTransactionManager接口**表示事务管理器，接口中有三个方法

```java
public interface PlatformTransactionManager {
    //通过事务管理器获取一个事务，返回TransactionStatus：内部包含事务的状态信息
    TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException;
    //根据事务的状态信息提交事务
    void commit(TransactionStatus status) throws TransactionException;
    //根据事务的状态信息回滚事务
    void rollback(TransactionStatus status) throws TransactionException;
}
```

PlatformTransactionManager有多个实现类，用来应对不同的环境，比如你操作db用的是hibernate或者mybatis，那么用到的事务管理器是不一样的，常见的事务管理器实现有下面几个

![](./imgs/2.png)

**JpaTransactionManager**：如果你用jpa来操作db，那么需要用这个管理器来帮你控制事务。

**DataSourceTransactionManager**：如果你用是指定数据源的方式，比如操作数据库用的是：JdbcTemplate、mybatis、ibatis，那么需要用这个管理器来帮你控制事务。

**HibernateTransactionManager**：如果你用hibernate来操作db，那么需要用这个管理器来帮你控制事务。

**JtaTransactionManager**：如果你用的是java中的jta来操作db，这种通常是分布式事务，此时需要用这种管理器来控制事务。

我们的案例中使用的是JdbcTemplate来操作db，所以用的是`DataSourceTransactionManager`这个管理器。

```
PlatformTransactionManager platformTransactionManager = new DataSourceTransactionManager(dataSource);
```

下面进入第3步，通过事务管理器来开启事务。

## 3、获取事务

下面源码我们以REQUIRED中嵌套一个REQUIRED_NEW来进行说明，也就是事务中嵌套一个新的事务。

### 3.1、getTransaction：获取事务

通过事务管理器的getTransactiongetTransaction(transactionDefinition)方法开启事务，传递一个TransactionDefinition参数

```java
TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
```

事务管理器我们用的是DataSourceTransactionManager，下面我们看一下DataSourceTransactionManager.getTransaction源码

````java
rg.springframework.jdbc.datasource.DataSourceTransactionManager
public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
    //事务定义信息，若传入的definition如果为空，取默认的
    TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());
    //@3.1-1：获取事务对象
    Object transaction = doGetTransaction();
    boolean debugEnabled = logger.isDebugEnabled();
    //@3.1-2：当前是否存在事务
    if (isExistingTransaction(transaction)) {
        //@1-3：如果当前存在事务，走这里
        return handleExistingTransaction(def, transaction, debugEnabled);
    }
    // 当前没有事务，走下面的代码
    // 若事务传播级别是PROPAGATION_MANDATORY：要求必须存在事务，若当前没有事务，弹出异常
    if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
        throw new IllegalTransactionStateException(
                "No existing transaction found for transaction marked with propagation 'mandatory'");
    } else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
        //事务传播行为(PROPAGATION_REQUIRED|PROPAGATION_REQUIRES_NEW|PROPAGATION_NESTED)走这里
        //@3.1-4：挂起事务
        SuspendedResourcesHolder suspendedResources = suspend(null);
        try {
            //@3.1-5：是否开启新的事务同步
            boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
            //@3.1-6：创建事务状态对象DefaultTransactionStatus,DefaultTransactionStatus是TransactionStatus的默认实现
            DefaultTransactionStatus status = newTransactionStatus(def, transaction, true, newSynchronization, debugEnabled, suspendedResources);
            //@3.1-7：doBegin用于开始事务
            doBegin(transaction, def);
            //@3.1-8：准备事务同步
            prepareSynchronization(status, def);
            //@3.1-9：返回事务状态对象
            return status;
        } catch (RuntimeException | Error ex) {
            //@3.1-10：出现（RuntimeException|Error）恢复被挂起的事务
            resume(null, suspendedResources);
            throw ex;
        }
    } else {
        //@3.1-11：其他事务传播行为的走这里(PROPAGATION_SUPPORTS、PROPAGATION_NOT_SUPPORTED、PROPAGATION_NEVER)
        boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
        return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
    }
}
````

下面来看一下@3.1-1：doGetTransaction方法，用来获取事务对象

### 3.2、doGetTransaction：获取事务对象

```java
org.springframework.jdbc.datasource.DataSourceTransactionManager
protected Object doGetTransaction() {
    //@3.2-1：创建数据源事务对象
    DataSourceTransactionObject txObject = new DataSourceTransactionObject();
    //@3.2-2：是否支持内部事务
    txObject.setSavepointAllowed(isNestedTransactionAllowed());
    //@3.2-4：ConnectionHolder表示jdbc连接持有者，简单理解：数据的连接被丢到ConnectionHolder中了，ConnectionHolder中提供了一些方法来返回里面的连接，此处调用TransactionSynchronizationManager.getResource方法来获取ConnectionHolder对象
    ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());
    //@3.2-5：将conHolder丢到DataSourceTransactionObject中，第二个参数表示是否是一个新的连接，明显不是的吗，新的连接需要通过datasource来获取，通过datasource获取的连接才是新的连接
    txObject.setConnectionHolder(conHolder, false);
    return txObject;
}
```

下面来看一下`@3.2-4`的代码，这个是重点了，这个用到了一个新的类`TransactionSynchronizationManager:事务同步管理器`，什么叫同步？一个事务过程中，被调用的方法都在一个线程中串行执行，就是同步；这个类中用到了很多ThreadLocal，用来在线程中存储事务相关的一些信息,，来瞅一眼

````java
public abstract class TransactionSynchronizationManager {
    //存储事务资源信息
    private static final ThreadLocal<Map<Object, Object>> resources =
            new NamedThreadLocal<>("Transactional resources");
    //存储事务过程中的一些回调接口(TransactionSynchronization接口，这个可以在事务的过程中给开发者提供一些回调用的)
    private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
            new NamedThreadLocal<>("Transaction synchronizations");
    //存储当前正在运行的事务的名称
    private static final ThreadLocal<String> currentTransactionName =
            new NamedThreadLocal<>("Current transaction name");
    //存储当前正在运行的事务是否是只读的
    private static final ThreadLocal<Boolean> currentTransactionReadOnly =
            new NamedThreadLocal<>("Current transaction read-only status");
    //存储当前正在运行的事务的隔离级别
    private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
            new NamedThreadLocal<>("Current transaction isolation level");
    //存储当前正在运行的事务是否是活动状态，事务启动的时候会被激活
    private static final ThreadLocal<Boolean> actualTransactionActive =
            new NamedThreadLocal<>("Actual transaction active");
    //还有很多静态方法，主要是用来操作上面这些ThreadLocal的，这里就不列出的，大家可以去看看
}
````

下面来看TransactionSynchronizationManager.getResource代码

```java
ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());
```

obtainDataSource()会返回事务管理器中的datasource对象

```java
protected DataSource obtainDataSource() {
    DataSource dataSource = getDataSource();
    Assert.state(dataSource != null, "No DataSource set");
    return dataSource;
}
```

下面看TransactionSynchronizationManager.getResource的源码

```java
public static Object getResource(Object key) {
    //通过TransactionSynchronizationUtils.unwrapResourceIfNecessary(key)获取一个actualKey，我们传入的是datasouce，实际上最后actualKey和传入的datasource是一个对象
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    //调用doGetResource方法获取对应的value
    Object value = doGetResource(actualKey);
    return value;
}
```

doGetResource(actualKey)方法如下，内部会从resources这个ThreadLocal中获取获取数据ConnectionHolder对象，到目前为止，根本没有看到向resource中放入过数据，获取获取到的conHolder肯定是null了

````java
static final ThreadLocal<Map<Object, Object>> resources = new NamedThreadLocal<>("Transactional resources");
private static Object doGetResource(Object actualKey) {
    Map<Object, Object> map = resources.get();
    if (map == null) {
        return null;
    }
    Object value = map.get(actualKey);
    return value;
}
````

TransactionSynchronizationManager.getResource：可以理解为从resource ThreadLocal中查找transactionManager.datasource绑定的ConnectionHolder对象

到此，Object transaction = doGetTransaction()方法执行完毕，下面我们再回到getTransaction方法，第一次进来，上下文中是没有事务的，所以会走下面的@3.1-4的代码，当前没有事务，导致没有事务需要挂起，所以suspend方法内部可以先忽略

````java
org.springframework.transaction.support.AbstractPlatformTransactionManager#getTransaction
public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
        throws TransactionException {
    //事务定义信息，若传入的definition如果为空，取默认的
    TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());
    //@3.1-1：获取事务对象
    Object transaction = doGetTransaction();
    if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
        //事务传播行为(PROPAGATION_REQUIRED|PROPAGATION_REQUIRES_NEW|PROPAGATION_NESTED)走这里
        //@3.1-4：挂起事务
        SuspendedResourcesHolder suspendedResources = suspend(null);
        try {
            //@3.1-5：是否开启新的事务同步
            boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
            //@3.1-6：创建事务状态对象DefaultTransactionStatus,DefaultTransactionStatus是TransactionStatus的默认实现
            DefaultTransactionStatus status = newTransactionStatus(
                    def, transaction, true, newSynchronization, debugEnabled, suspendedResources);
            //@3.1-7：doBegin用于开始事务
            doBegin(transaction, def);
            //@3.1-8：准备事务同步
            prepareSynchronization(status, def);
            //@3.1-9：返回事务状态对象
            return status;
        } catch (RuntimeException | Error ex) {
            //@3.1-10：出现（RuntimeException|Error）恢复被挂起的事务
            resume(null, suspendedResources);
            throw ex;
        }
    }
}
````

然后会执行下面代码

```java
//@3.1-5：是否开启新的事务同步，事务同步是干嘛的，是spring在事务过程中给开发者预留的一些扩展点，稍后细说；大家先这么理解，每个新的事务newSynchronization都是true，开一个一个新的事务就会启动一个新的同步
boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
//@3.1-6：创建事务状态对象DefaultTransactionStatus,DefaultTransactionStatus是TransactionStatus的默认实现
DefaultTransactionStatus status = newTransactionStatus(def, transaction, true, newSynchronization, debugEnabled, suspendedResources);
//@3.1-7：doBegin用于开始事务
doBegin(transaction, def);
//@3.1-8：准备事务同步
prepareSynchronization(status, def);
//@3.1-9：返回事务状态对象
return status;
```

上面过程我们重点来说一下@3.1-7 和 @3.1-8

### 3.3、@3.1-7：doBegin 开启事务

````java
org.springframework.jdbc.datasource.DataSourceTransactionManager#doBegin
protected void doBegin(Object transaction, TransactionDefinition definition) {
    //数据源事务对象
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    //数据库连接
    Connection con = null;
    try {
        //txObject.hasConnectionHolder()用来判断txObject.connectionHolder!=null，现在肯定是null，所以txObject.hasConnectionHolder()返回false
        if (!txObject.hasConnectionHolder() ||
                txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
            //调用transactionManager.datasource.getConnection()获取一个数据库连接
            Connection newCon = obtainDataSource().getConnection();
            //将数据库连接丢到一个ConnectionHolder中，放到txObject中，注意第2个参数是true，表示第一个参数的ConnectionHolder是新创建的
            txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
        }
        //连接中启动事务同步
        txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
        //获取连接
        con = txObject.getConnectionHolder().getConnection();
        //获取隔离级别
        Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
        //设置隔离级别
        txObject.setPreviousIsolationLevel(previousIsolationLevel);
        //设置是否是只读
        txObject.setReadOnly(definition.isReadOnly());
        //判断连接是否是自动提交的，如果是自动提交的将其置为手动提交
        if (con.getAutoCommit()) {
            //在txObject中存储一下连接自动提交老的值，用于在事务执行完毕之后，还原一下Connection的autoCommit的值
            txObject.setMustRestoreAutoCommit(true);
            //设置手动提交
            con.setAutoCommit(false);
        }
        //准备事务连接
        prepareTransactionalConnection(con, definition);
        //设置事务活动开启
        txObject.getConnectionHolder().setTransactionActive(true);
        //根据事务定义信息获取事务超时时间
        int timeout = determineTimeout(definition);
        if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
            //设置连接的超时时间
            txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
        }
        //txObject中的ConnectionHolder是否是一个新的，确实是新的，所以这个地方返回true
        if (txObject.isNewConnectionHolder()) {
            //将datasource->ConnectionHolder丢到resource ThreadLocal的map中
            TransactionSynchronizationManager.bindResource(obtainDataSource(), txObject.getConnectionHolder());
        }
    }
}
````

重点来看一下下面这段代码

```
TransactionSynchronizationManager.bindResource(obtainDataSource(), txObject.getConnectionHolder());
```

源码

````java
org.springframework.transaction.support.TransactionSynchronizationManager#bindResource
public static void bindResource(Object key, Object value) throws IllegalStateException {
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    Map<Object, Object> map = resources.get();
    if (map == null) {
        map = new HashMap<>();
        resources.set(map);
    }
    map.put(actualKey, value);
}
````

上面这段代码执行完毕之后，datasource->ConnectionHoloder(conn)被放到resources Threadloca的map中了。

### 3.4、@3.1-8：prepareSynchronization 准备事务同步

```
//@3.1-8：准备事务同步
prepareSynchronization(status, def);
```

源码如下，大家看一下，这个方法主要的作用是，开启一个新事务的时候，会将事务的状态、隔离级别、是否是只读事务、事务名称丢到TransactionSynchronizationManager中的各种对应的ThreadLocal中，方便在当前线程中共享这些数据。

````java
org.springframework.transaction.support.AbstractPlatformTransactionManager#prepareSynchronization
protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
    //如果是一个新的事务，status.isNewSynchronization()将返回true
    if (status.isNewSynchronization()) {
        TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
                definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?
                        definition.getIsolationLevel() : null);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
        TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
        //@3.4-1：初始化事务同步
        TransactionSynchronizationManager.initSynchronization();
    }
}
````

@3.4-1：初始化事务同步

```java
org.springframework.transaction.support.TransactionSynchronizationManager
private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
            new NamedThreadLocal<>("Transaction synchronizations");
//获取同步是否启动，新事务第一次进来synchronizations.get()是null，所以这个方法返回的是false
public static boolean isSynchronizationActive() {
    return (synchronizations.get() != null);
}
//初始化事务同步，主要就是在synchronizations ThreadLocal中放一个LinkedHashSet
public static void initSynchronization() throws IllegalStateException {
    if (isSynchronizationActive()) {
        throw new IllegalStateException("Cannot activate transaction synchronization - already active");
    }
    synchronizations.set(new LinkedHashSet<>());
}
```

### 3.5、小结

获取事务的过程已经结束了，我们来看一下这个过程中做的一些关键的事情

````java
1、获取db连接：从事务管理器的datasource中调用getConnection获取一个新的数据库连接，将连接置为手动提交
2、将datasource关联连接丢到ThreadLocal中：将第一步中获取到的连丢到ConnectionHolder中，然后将事务管理器的datasource->ConnectionHolder丢到了resource ThreadLocal中，这样我们可以通过datasource在ThreadLocal中获取到关联的数据库连接
3、准备事务同步：将事务的一些信息放到ThreadLocal中
````

## 4、事务方法中执行增删改查

以下面这个插入操作来看一下这个插入是如何参与到spring事务中的。

````java
jdbcTemplate.update("insert into t_user (name) values (?)", "test1-1");
````

最终会进入到jdbctemplate#execute方法里，无用代码我们给剔除，重点内部关注下面获取连接的方法

```java
org.springframework.jdbc.core.JdbcTemplate#execute(org.springframework.jdbc.core.PreparedStatementCreator, org.springframework.jdbc.core.PreparedStatementCallback<T>){
    //获取数据库连接
    Connection con = DataSourceUtils.getConnection(obtainDataSource());
    //通过conn执行db操作
}
```

obtainDataSource()会返回jdbctemplate.datasource对象，下面重点来看DataSourceUtils.getConnection源码，最终会进入下面这个方法

````java
org.springframework.jdbc.datasource.DataSourceUtils#doGetConnection
public static Connection doGetConnection(DataSource dataSource) throws SQLException {
    //用jdbctemplate.datSource从TransactionSynchronizationManager的resouce ThreadLocal中获取对应的ConnectionHolder对象，在前面获取事务环节中，transactionManager.datasource->ConnectionHolder被丢到resouce ThreadLocal，而jdbctemplate.datSource和transactionManager.datasource是同一个对象，所以是可以获取到ConnectionHolder的，此时就会使用事务开启是的数据库连接
    ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
    //conHolder不为空 && conHolder中有数据库连接对象
    if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
        //返回conHolder中的数据库连接对象
        return conHolder.getConnection();
    }
    //如果上面获取不到连接，会走这里，这里将会调用jdbctemplate.datasource.getConnection()从数据源中获取一个新的db连接
    Connection con = fetchConnection(dataSource);
    //将连接返回
    return con;
}
````

**可以得出一个结论：如果要让最终执行的sql受spring事务控制，那么事务管理器中datasource对象必须和jdbctemplate.datasource是同一个，这个结论在其他文章中说过很多次了，这里大家算是搞明白了吧。**

## 5、提交事务

调用事务管理器的commit方法，提交事务

````java
platformTransactionManager.commit(transactionStatus);
````

commit源码

```java
org.springframework.transaction.support.AbstractPlatformTransactionManager#commit
public final void commit(TransactionStatus status) throws TransactionException {
    //事务是否已经完成，此时还未完成，如果事务完成了，再来调用commit方法会报错
    if (status.isCompleted()) {
        throw new IllegalTransactionStateException(
                "Transaction is already completed - do not call commit or rollback more than once per transaction");
    }
    //事务状态
    DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
    //defStatus.rollbackOnly是否是true，如果是true，说明事务状态被标注了需要回滚，此时走回滚逻辑
    if (defStatus.isLocalRollbackOnly()) {
        //走回滚逻辑
        processRollback(defStatus, false);
        return;
    }
    //提交事务过程
    processCommit(defStatus);
}
```

processCommit源码

```java
org.springframework.transaction.support.AbstractPlatformTransactionManager#processCommit
private void processCommit(DefaultTransactionStatus status) throws TransactionException {
    try {
        try {
            //提交之前的回调（给开发提供的扩展点）
            triggerBeforeCommit(status);
            //事务完成之前的回调（给开发提供的扩展点）
            triggerBeforeCompletion(status);
            //是否是新事务，如果是新事务，将执行提交操作，比如传播行为是REQUIRED中嵌套了一个REQUIRED，那么内部的事务就不是新的事务，外部的事务是新事务
            if (status.isNewTransaction()) {
                //@5-1：执行提交操作
                doCommit(status);
            }
        } catch (UnexpectedRollbackException ex) {
            //事务完成之后执行的回调（给开发提供的扩展点）
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
            throw ex;
        } catch (RuntimeException | Error ex) {
            //提交过程中有异常,执行回滚操作
            doRollbackOnCommitException(status, ex);
            throw ex;
        }
        try {
            //事务commit之后，执行一些回调（给开发提供的扩展点）
            triggerAfterCommit(status);
        } finally {
            //事务完成之后，执行一些回调（给开发提供的扩展点）
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);
        }
    } finally {
        //事务执行完毕之后，执行一些清理操作
        cleanupAfterCompletion(status);
    }
}
```

上面这个方法看起来挺长的，重点会做**3件**事情：

**1、给开发提供的扩展点**：以trigger开头的方法，是留给开发的扩展点，可以在事务执行的过程中执行一些回调，主要是在事务提交之前，提交之后，回滚之前，回滚之后，可以执行一些回调，也就是事务同步要干的事情，这个扩展点稍后说。

**2、通过connection执行commit操作**，对应上面的 @5-1 代码：doCommit(status);

**3、完成之后执行清理操作**：finally中执行 cleanupAfterCompletion(status);

来看看**doCommit(status)方法**，内部主要就是调用connection的commit()提交事务，如下：

````java
org.springframework.jdbc.datasource.DataSourceTransactionManager#doCommit
protected void doCommit(DefaultTransactionStatus status) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
    //从ConnectionHolder中获取Connection
    Connection con = txObject.getConnectionHolder().getConnection();
    //执行commit，提交数据库事务
    con.commit();
}
````

cleanupAfterCompletion(status)：清理操作

```java
org.springframework.transaction.support.AbstractPlatformTransactionManager#cleanupAfterCompletion
private void cleanupAfterCompletion(DefaultTransactionStatus status) {
    //将事务状态置为已完成
    status.setCompleted();
    //是否是新的事务同步
    if (status.isNewSynchronization()) {
        //将TransactionSynchronizationManager中的那些ThreadLoca中的数据都清除，会调用ThreadLocal的remove()方法清除数据
        TransactionSynchronizationManager.clear();
    }
    //是否是新事务
    if (status.isNewTransaction()) {
        //执行清理操作
        doCleanupAfterCompletion(status.getTransaction());
    }
    //是否有被挂起的事务
    if (status.getSuspendedResources() != null) {
        Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
        //恢复被挂起的事务
        resume(transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
    }
}
```

doCleanupAfterCompletion源码

```java
org.springframework.jdbc.datasource.DataSourceTransactionManager#doCleanupAfterCompletion
protected void doCleanupAfterCompletion(Object transaction) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    //是否是一个新的ConnectionHolder，如果是新的事务，那么ConnectionHolder是新的
    if (txObject.isNewConnectionHolder()) {
        //将transactionManager.datasource->ConnectionHolder从resource Threadlocal中干掉
        TransactionSynchronizationManager.unbindResource(obtainDataSource());
    }
    //下面重置Connection，将Connection恢复到最原始的状态
    Connection con = txObject.getConnectionHolder().getConnection();
    try {
        if (txObject.isMustRestoreAutoCommit()) {
            //自动提交
            con.setAutoCommit(true);
        }
        //恢复connction的隔离级别、是否是只读事务
        DataSourceUtils.resetConnectionAfterTransaction(
                con, txObject.getPreviousIsolationLevel(), txObject.isReadOnly());
    } catch (Throwable ex) {
        logger.debug("Could not reset JDBC Connection after transaction", ex);
    }
    //是否是新的连接
    if (txObject.isNewConnectionHolder()) {
        //释放连接，内部会调用conn.close()方法
        DataSourceUtils.releaseConnection(con, this.dataSource);
    }
    //还原ConnectionHoloder到最初的状态
    txObject.getConnectionHolder().clear();
}
```

终结一下，**清理工作主要做的事情就是释放当前线程占有的一切资源，然后将被挂起的事务恢复**。

## 6、回滚事务

回滚的操作和提交的操作差不多的，源码我就不讲了，大家自己去看一下。

## 7、存在事务的情况如何走？

下面来看另外一个流程，REQUIRED中嵌套一个REQUIRED_NEW，然后走到REQUIRED_NEW的时候，代码是如何运行的？大致的过程如下

**1、判断上线文中是否有事务**

**2、挂起当前事务**

**3、开启新事务，并执行新事务**

**4、恢复被挂起的事务**

### 7.1、判断是否有事务：isExistingTransaction

判断上线文中是否有事务，比较简单，如下：

````java
org.springframework.jdbc.datasource.DataSourceTransactionManager#isExistingTransaction
protected boolean isExistingTransaction(Object transaction) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    //txObject.connectionHolder!=null && connectionHolder事务处于开启状态(上面我们介绍过在doBegin开启事务的时候connectionHolder.transactionActive会被置为true)
    return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
}
````

### 7.2、若当前存在事务

我们再来看一下获取事务中，有事务如何走

````java
org.springframework.transaction.support.AbstractPlatformTransactionManager#getTransaction
public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
    //获取事务
    Object transaction = doGetTransaction();
    //是否存在事务
    if (isExistingTransaction(transaction)) {
        //存在事务会走这里
        return handleExistingTransaction(def, transaction, debugEnabled);
    }
}
````

当前存在事务，然后会进入handleExistingTransaction方法

```java
org.springframework.transaction.support.AbstractPlatformTransactionManager#handleExistingTransaction
private TransactionStatus handleExistingTransaction(TransactionDefinition definition, Object transaction, boolean debugEnabled) throws TransactionException {
    //当前有事务，被嵌套的事务传播行为是PROPAGATION_NEVER，抛出异常
    if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
        throw new IllegalTransactionStateException(
                "Existing transaction found for transaction marked with propagation 'never'");
    }
    if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
        //当前有事务，被嵌套的事务传播行为是PROPAGATION_NOT_SUPPORTED，那么将先调用suspend将当前事务挂起，然后以无事务的方式运行被嵌套的事务
        //挂起当前事务
        Object suspendedResources = suspend(transaction);
        boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
        //以无事务的方式运行
        return prepareTransactionStatus(
                definition, null, false, newSynchronization, debugEnabled, suspendedResources);
    }
    if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
        //被嵌套的事务传播行为是PROPAGATION_REQUIRES_NEW，那么会先挂起当前事务，然后会重新开启一个新的事务
        //挂起当前事务
        SuspendedResourcesHolder suspendedResources = suspend(transaction);
        try {
            //下面的过程我们就不在再介绍了，之前有介绍过
            boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
            DefaultTransactionStatus status = newTransactionStatus(
                    definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
            doBegin(transaction, definition);
            prepareSynchronization(status, definition);
            return status;
        } catch (RuntimeException | Error beginEx) {
            resumeAfterBeginException(transaction, suspendedResources, beginEx);
            throw beginEx;
        }
    }
    //其他的传播行为走下面。。。，暂时省略了
}
```

下面重点看事务挂起和事务的恢复操作。

### 7.3、事务挂起：suspend

事务挂起调用事务管理器的suspend方法，源码如下，主要做的事情：将当前事务中的一切信息保存到SuspendedResourcesHolder对象中，相当于事务的快照，后面恢复的时候用；然后将事务现场清理干净，主要是将一堆存储在ThreadLocal中的事务数据干掉。

```
org.springframework.transaction.support.AbstractPlatformTransactionManager#suspend
protected final SuspendedResourcesHolder suspend(@Nullable Object transaction) throws TransactionException {
    //当前事务同步是否被激活，如果是新事务，这个返回的是true
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        //挂起事务同步，这个地方会可以通过TransactionSynchronization接口给开发者提供了扩展点，稍后我们会单独介绍TransactionSynchronization接口，这个接口专门用来在事务执行过程中做回调的
        List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();
        try {
            Object suspendedResources = null;
            if (transaction != null) {
                //@1：获取挂起的资源
                suspendedResources = doSuspend(transaction);
            }
            //下面就是获取当前事务的各种信息(name,readyOnly,事务隔离级别,是否被激活)
            String name = TransactionSynchronizationManager.getCurrentTransactionName();
            TransactionSynchronizationManager.setCurrentTransactionName(null);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
            Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
            boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
            TransactionSynchronizationManager.setActualTransactionActive(false);
            return new SuspendedResourcesHolder(
                    suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);
        }
    }
}
```

下面来看看`@1：doSuspend(transaction)`源码，主要就是将datasource->connectionHolder从resource ThreadLocal中解绑，然后将connectionHolder返回，下面这个方法实际上返回的就是connectionHolder对象

````java
org.springframework.jdbc.datasource.DataSourceTransactionManager#doSuspend
protected Object doSuspend(Object transaction) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    //将connectionHolder置为null
    txObject.setConnectionHolder(null);
    //将datasource->connectionHolder从resource ThreadLocal中解绑，并返回被解绑的connectionHolder对象
    return TransactionSynchronizationManager.unbindResource(obtainDataSource());
}
````

此时，当前的事务被挂起了，然后开启一个新的事务，新的事务的过程上面已经介绍过了，下面我们来看事务的恢复过程。

### 7.4、事务恢复：resume

事务挂起调用事务管理器的resume方法，源码如下，主要做的事情：通过SuspendedResourcesHolder对象中，将被挂起的事务恢复，SuspendedResourcesHolder对象中保存了被挂起的事务所有信息，所以可以通过这个对象来恢复事务。

````java
org.springframework.transaction.support.AbstractPlatformTransactionManager#resume
protected final void resume(@Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder)
        throws TransactionException {
    if (resourcesHolder != null) {
        Object suspendedResources = resourcesHolder.suspendedResources;
        if (suspendedResources != null) {
            //恢复被挂起的资源，也就是将datasource->connectionHolder绑定到resource ThreadLocal中
            doResume(transaction, suspendedResources);
        }
        List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
        //下面就是将数据恢复到各种ThreadLocal中
        if (suspendedSynchronizations != null) {
            TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
            TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
            //恢复事务同步（将事务扩展点恢复）
            doResumeSynchronization(suspendedSynchronizations);
        }
    }
}
````

## 8、事务执行过程中的回调接口： TransactionSynchronization

### 8.1、作用

spring事务运行的过程中，给开发者预留了一些扩展点，在事务执行的不同阶段，将回调扩展点中的一些方法。

比如我们想在事务提交之前、提交之后、回滚之前、回滚之后做一些事务，那么可以通过扩展点来实现。

### 8.2、扩展点的用法

#### 1、定义事务TransactionSynchronization对象

TransactionSynchronization接口中的方法在spring事务执行的过程中会自动被回调

````java
public interface TransactionSynchronization extends Flushable {
    //提交状态
    int STATUS_COMMITTED = 0;
    //回滚状态
    int STATUS_ROLLED_BACK = 1;
    //状态未知，比如事务提交或者回滚的过程中发生了异常，那么事务的状态是未知的
    int STATUS_UNKNOWN = 2;
    //事务被挂起的时候会调用被挂起事务中所有TransactionSynchronization的resume方法
    default void suspend() {
    }
    //事务恢复的过程中会调用被恢复的事务中所有TransactionSynchronization的resume方法
    default void resume() {
    }
    //清理操作
    @Override
    default void flush() {
    }
    //事务提交之前调用
    default void beforeCommit(boolean readOnly) {
    }
    //事务提交或者回滚之前调用
    default void beforeCompletion() {
    }
    //事务commit之后调用
    default void afterCommit() {
    }
    //事务完成之后调用
    default void afterCompletion(int status) {
    }
}
````

#### 2、将TransactionSynchronization注册到当前事务中

通过下面静态方法将事务扩展点TransactionSynchronization注册到当前事务中

````java
TransactionSynchronizationManager.registerSynchronization(transactionSynchronization)
````

看一下源码，很简单，丢到ThreadLocal中了

```
private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
            new NamedThreadLocal<>("Transaction synchronizations");
public static void registerSynchronization(TransactionSynchronization synchronization)
            throws IllegalStateException {
    Set<TransactionSynchronization> synchs = synchronizations.get();
    if (synchs == null) {
        throw new IllegalStateException("Transaction synchronization is not active");
    }
    synchs.add(synchronization);
}
```

当有多个TransactionSynchronization的时候，可以指定其顺序，可以实现org.springframework.core.Ordered接口，来指定顺序，从小大的排序被调用，TransactionSynchronization有个默认适配器TransactionSynchronizationAdapter，这个类实现了Ordered接口，所以，如果我们要使用的时候，直接使用TransactionSynchronizationAdapter这个类。

#### 3、回调扩展点TransactionSynchronization中的方法

TransactionSynchronization中的方法是spring事务管理器自动调用的，本文上面有提交到，事务管理器在事务提交或者事务回滚的过程中，有很多地方会调用trigger开头的方法，这个trigger方法内部就会遍历当前事务中的transactionSynchronization列表，然后调用transactionSynchronization内部的一些指定的方法。

以事务提交的源码为例，来看一下

````java
private void processCommit(DefaultTransactionStatus status) throws TransactionException {
    triggerBeforeCommit(status);
    triggerBeforeCompletion(status);
    //....其他代码省略
}
````

triggerBeforeCommit(status)源码

```java
protected final void triggerBeforeCommit(DefaultTransactionStatus status) {
    if (status.isNewSynchronization()) {
        TransactionSynchronizationUtils.triggerBeforeCommit(status.isReadOnly());
    }
}
```

TransactionSynchronizationUtils.triggerBeforeCommit 源码

```java
public static void triggerBeforeCommit(boolean readOnly) {
    for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
        synchronization.beforeCommit(readOnly);
    }
}
```

### 8.3、来个案例

#### 1、执行sql

```sql
DROP DATABASE IF EXISTS javacode2018;
CREATE DATABASE if NOT EXISTS javacode2018;
USE javacode2018;
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user(
  id int PRIMARY KEY AUTO_INCREMENT,
  name varchar(256) NOT NULL DEFAULT '' COMMENT '姓名'
);
```

#### 2、案例代码

代码比较简单，不多解释了，运行测试方法m0

````java
package com.javacode2018.tx.demo9;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
/**
 * 公众号：路人甲java，工作10年的前阿里P7,所有文章以系列的方式呈现，带领大家成为java高手，
 * 目前已出：java高并发系列、mysq|高手系列、Maven高手系列、mybatis系列、spring系列，
 * 正在连载springcloud系列，欢迎关注！
 */
public class Demo9Test {
    JdbcTemplate jdbcTemplate;
    PlatformTransactionManager platformTransactionManager;
    @Before
    public void before() {
        //定义一个数据源
        org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/javacode2018?characterEncoding=UTF-8");
        dataSource.setUsername("root");
        dataSource.setPassword("root123");
        dataSource.setInitialSize(5);
        //定义一个JdbcTemplate，用来方便执行数据库增删改查
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.platformTransactionManager = new DataSourceTransactionManager(dataSource);
        this.jdbcTemplate.update("truncate table t_user");
    }
    @Test
    public void m0() throws Exception {
        System.out.println("PROPAGATION_REQUIRED start");
        //2.定义事务属性：TransactionDefinition，TransactionDefinition可以用来配置事务的属性信息，比如事务隔离级别、事务超时时间、事务传播方式、是否是只读事务等等。
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        //3.开启事务：调用platformTransactionManager.getTransaction开启事务操作，得到事务状态(TransactionStatus)对象
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        this.addSynchronization("ts-1", 2);
        this.addSynchronization("ts-2", 1);
        //4.执行业务操作，下面就执行2个插入操作
        jdbcTemplate.update("insert into t_user (name) values (?)", "test1-1");
        jdbcTemplate.update("insert into t_user (name) values (?)", "test1-2");
        this.m1();
        //5.提交事务：platformTransactionManager.commit
        System.out.println("PROPAGATION_REQUIRED 准备commit");
        platformTransactionManager.commit(transactionStatus);
        System.out.println("PROPAGATION_REQUIRED commit完毕");
        System.out.println("after:" + jdbcTemplate.queryForList("SELECT * from t_user"));
    }
    public void m1() {
        System.out.println("PROPAGATION_REQUIRES_NEW start");
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        jdbcTemplate.update("insert into t_user (name) values (?)", "test2-1");
        jdbcTemplate.update("insert into t_user (name) values (?)", "test2-2");
        this.addSynchronization("ts-3", 2);
        this.addSynchronization("ts-4", 1);
        System.out.println("PROPAGATION_REQUIRES_NEW 准备commit");
        platformTransactionManager.commit(transactionStatus);
        System.out.println("PROPAGATION_REQUIRES_NEW commit完毕");
    }
    public void addSynchronization(final String name, final int order) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public int getOrder() {
                    return order;
                }
                @Override
                public void suspend() {
                    System.out.println(name + ":suspend");
                }
                @Override
                public void resume() {
                    System.out.println(name + ":resume");
                }
                @Override
                public void flush() {
                    System.out.println(name + ":flush");
                }
                @Override
                public void beforeCommit(boolean readOnly) {
                    System.out.println(name + ":beforeCommit:" + readOnly);
                }
                @Override
                public void beforeCompletion() {
                    System.out.println(name + ":beforeCompletion");
                }
                @Override
                public void afterCommit() {
                    System.out.println(name + ":afterCommit");
                }
                @Override
                public void afterCompletion(int status) {
                    System.out.println(name + ":afterCompletion:" + status);
                }
            });
        }
    }
}
````

#### 3、输出

```java
PROPAGATION_REQUIRED start
PROPAGATION_REQUIRES_NEW start
ts-2:suspend
ts-1:suspend
PROPAGATION_REQUIRES_NEW 准备commit
ts-4:beforeCommit:false
ts-3:beforeCommit:false
ts-4:beforeCompletion
ts-3:beforeCompletion
ts-4:afterCommit
ts-3:afterCommit
ts-4:afterCompletion:0
ts-3:afterCompletion:0
ts-2:resume
ts-1:resume
PROPAGATION_REQUIRES_NEW commit完毕
PROPAGATION_REQUIRED 准备commit
ts-2:beforeCommit:false
ts-1:beforeCommit:false
ts-2:beforeCompletion
ts-1:beforeCompletion
ts-2:afterCommit
ts-1:afterCommit
ts-2:afterCompletion:0
ts-1:afterCompletion:0
PROPAGATION_REQUIRED commit完毕
after:[{id=1, name=test1-1}, {id=2, name=test1-2}, {id=3, name=test2-1}, {id=4, name=test2-2}]
```

输出配合案例源码，大家理解一下。

输出配合案例源码，大家理解一下。

## 总结一下

**今天的内容挺长的，辛苦大家了，不过我相信spring事务这块吃透了，会让你收获很多，加油！**

**事务方面有任何问题的欢迎给我留言，下篇文章将解析声明式事务的源码，敬请期待！！\****

## 案例源码

```
git地址：
https://gitee.com/javacode2018/spring-series
本文案例对应源码：
    spring-series\lesson-002-tx\src\main\java\com\javacode2018\tx\demo9
```

> 本文摘自： http://www.itsoku.com/course/12/129#8.3%E3%80%81%E6%9D%A5%E4%B8%AA%E6%A1%88%E4%BE%8B