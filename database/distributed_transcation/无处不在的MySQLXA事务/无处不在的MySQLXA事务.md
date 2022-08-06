# 无处不在的 MySQL XA 事务

## 两阶段提交（2PC）与 XA 协议

两阶段提交是最经典、也是最常见的分布式事务方案，“两阶段提交”（2PC）这个名字仅仅描述了一个分布式事务提交的方式，从用户的视角来看，完整执行一个事务（以转账为例）过程如下：

1. 开始事务，客户端执行各种写入（更新）
2. 事务结束，由协调者发起 2PC 的 Prepare 阶段
3. 协调者持久化事务日志
4. 协调者发起 2PC 的 Commit 阶段，事务完成提交

![](./imgs/1.jpg)

其中第3步记录事务日志是非常关键的一步，我们可以将事务日志想象成一个仅追加（append-only）、无限延伸的数组，每当事务提交时，我们就把事务 ID 追加到数组的最后面，并且保证数据已经持久化地保存了（比如 fsync）。

假如协调者意外宕机，很可能整个分布式事务并未完成提交，比如 Tx0 在 DB1 上已提交，而在 DB2 上还处于 PREPARE 的状态。恢复后的协调者需要查看之前的事务日志，得知 Tx0 已经提交了，进而可以提交 DB2 上的悬挂事务，将数据库恢复到一个一致的状态；反之，如果 DB1 上没有 Tx0，DB2 上有一个名为 Tx0 的悬挂事务，协调者通过查看事务日志得知 Tx0 并未提交，因此能够正确地回滚 DB2 上的 Tx0。

XA 协议原本是指基于 2PC 定义的一套交互协议，例如定义了 XA COMMIT、XA PREPARE 这些指令。不过在 MySQL 源码的语境中，很多时候 XA 指的就是 2PC，我们后面会看到，即使在单个 MySQL 进程中，也需要用 2PC 来保证数据一致性。

## 内部 2PC 事务

MySQL 的一个很有趣的设计是允许多种存储引擎，每个存储引擎本质上就是一个独立的“数据库”，包含自己的数据文件、日志文件等，不同存储引擎之间互不相通。

举个例子，InnoDB 引擎通过 redo-log 保证自身事务的持久性和原子性，而 X-Engine 引擎通过 WAL（write-ahead log）保证自身事务的持久性和原子性。如果一个事务同时修改了 InnoDB 的表 t1 和 X-Engine 表 t2，问题来了，如果先写入 t1，可能在写 t2 之前发生宕机，于是事务只做一半，违反了原子性。光凭存储引擎自身是无法解决该问题的，不一致发生在不同的存储引擎之间。

更常见的例子发生在 binlog 和 InnoDB 之间。MySQL 的 binlog 可以看作数据的另一个副本，一旦开启 binlog，数据不仅会写入存储引擎，还会写入 binlog 中，并且这两份数据必须严格一致，否则可能出现主备不一致。

![](./imgs/2.jpg)

对于场景一，我们必须引入一个独立于存储引擎的“外部协调者“来保证 t1 和 t2 上的事务原子性。场景二中也是同理，但是可以稍微巧妙一些——不妨直接让 binlog 来充当事务日志。接下来我们看看具体是如何做的。

MySQL 启动时，`init_server_components()` 函数按以下规则选择事务协调器（本文代码都取自 MySQL 8.0.21，为了方便阅读会做适当精简。下同）：

```cpp
tc_log = &tc_log_dummy;
if (total_ha_2pc > 1 || (1 == total_ha_2pc && opt_bin_log)) {
  if (opt_bin_log)
    tc_log = &mysql_bin_log;
  else
    tc_log = &tc_log_mmap;
}
```

1. 如果 binlog 开启，使用 `mysql_binlog` 事务日志
2. 否则，如果支持 2PC 的存储引擎多于 1 个，使用 `tc_log_mmap` 事务日志
3. 否则，使用 `tc_log_dummy` 事务日志，它是一个空的实现，实际上就是不记日志

而 `TC_LOG` 是这三种事务日志具体实现的基类，它定义了事务日志需要实现的接口：

```cpp
/** Transaction Coordinator Log */
class TC_LOG {
 public:
  virtual int open(const char *opt_name) = 0;
  virtual void close() = 0;
  virtual enum_result commit(THD *thd, bool all) = 0;
  virtual int rollback(THD *thd, bool all) = 0;
  virtual int prepare(THD *thd, bool all) = 0;
};
```

其中 `tc_log_mmap` 协调器是一个比较标准的事务协调器实现，它会创建一个名为 `tc.log` 的日志并使用操作系统的内存映射（memory-map，mmap）机制将内容映射到内存中。`tc.log` 文件中分为一个一个 PAGE，每个 PAGE 上有多个事务 ID（xid），这些就是由它记录的已经确定提交的事务。

![](./imgs/3.jpg)

更多的时候，我们用到的都是 `mysql_bin_log` 这个基于 binlog 实现的事务日志：既然 binlog 反正都是要写的，不妨所有的 Engine 都统一以 binlog 为准，这的确是个很聪明的主意。binlog 中除了 XID 以外还包含许多的信息（比如所有的写入），但对于 `TC_LOG` 来说只要存在 XID 就足以胜任了。

## 内部 2PC 事务提交 —— 以 binlog 协调器为例

为了跟踪 MySQL 的事物提交过程，我们执行一条最简单的 UPDATE 语句（autocommit=on），然后看看事务提交是如何进行的。

事务的提交过程入口点位于 `ha_commit_trans` 函数，事务提交的过程如下：

1. 首先调用存储引擎的 prepare 接口
2. 调用 TC_LOG 的 commit 接口写入事务日志
3. 调用存储引擎的 commit 接口

![](./imgs/4.jpg)

各个存储引擎会将自己的 prepare、commit 等函数注册到 MySQL Server 层，也就是 `handlerton` 这个结构体，注册的过程在 `ha_innodb.cc` 中：

```cpp
innobase_hton->commit = innobase_commit;
innobase_hton->rollback = innobase_rollback;
innobase_hton->prepare = innobase_xa_prepare;
// 省略了很多其他函数
```

首先是 2PC 的 prepare 阶段，`trans_commit_stmt` 调用 binlog 协调器的 prepare 接口，但是它什么也不会做，直接去调用存储引擎（以 InnoDB 为例）的 prepare 接口。

```text
trans_commit_stmt(THD * thd, bool ignore_global_read_lock) (sql/transaction.cc:532)
  ha_commit_trans(THD * thd, bool all, bool ignore_global_read_lock) (sql/handler.cc:1740)
    MYSQL_BIN_LOG::prepare(MYSQL_BIN_LOG * const this, THD * thd, bool all) (sql/binlog.cc:7911)
      ha_prepare_low(THD * thd, bool all) (sql/handler.cc:2320)    
        innobase_xa_prepare(handlerton * hton, THD * thd, bool prepare_trx) (storage/innobase/handler/ha_innodb.cc:19084)
```

2PC 的 commit 阶段，`trans_commit_stmt` 调用 binlog 协调器的 commit 接口写入 binlog，事务日志被持久化。这一步之后，即使节点宕机，重启恢复时也会将事务恢复至已提交的状态。

```text
trans_commit_stmt(THD * thd, bool ignore_global_read_lock) (sql/transaction.cc:532)
  ha_commit_trans(THD * thd, bool all, bool ignore_global_read_lock) (sql/handler.cc:1755)
    MYSQL_BIN_LOG::commit(MYSQL_BIN_LOG * const this, THD * thd, bool all) (sql/binlog.cc:7943)
```

最后 binlog 协调器调用存储引擎的 commit 接口，完成事务提交：

```text
trans_commit_stmt(THD * thd, bool ignore_global_read_lock) (sql/transaction.cc:532)
  ha_commit_trans(THD * thd, bool all, bool ignore_global_read_lock) (sql/handler.cc:1755)
    MYSQL_BIN_LOG::commit(MYSQL_BIN_LOG * const this, THD * thd, bool all) (sql/binlog.cc:8171)
      MYSQL_BIN_LOG::ordered_commit(MYSQL_BIN_LOG * const this, THD * thd, bool all, bool skip_commit) (sql/binlog.cc:8924)
        MYSQL_BIN_LOG::process_commit_stage_queue(MYSQL_BIN_LOG * const this, THD * thd, THD * first) (sql/binlog.cc:8407)
          ha_commit_low(THD * thd, bool all, bool run_after_commit) (sql/handler.cc:1935)
            innobase_commit(handlerton * hton, THD * thd, bool commit_trx) (storage/innobase/handler/ha_innodb.cc:5283)
```

以上仅仅是一条更新语句执行的行为，如果是多个事物并发提交，MySQL 会通过 group commit 的方式优化性能，推荐这篇 [《图解 MySQL 组提交(group commit)》](https://link.zhihu.com/?target=https%3A//developer.aliyun.com/article/617776)。

## 分布式 XA 事务

回到分布式事务上，我们知道 XA 协议本就是为一个分布式事务协议，它规定了 `XA PREPARE`、`XA COMMIT`、`XA ROLLBACK` 等命令。XA 协议规定了事务管理器（协调者）和资源管理器（数据节点）如何交互，共同完成分布式 2PC 过程。

那么，假如作为 MySQL 的设计者，你会如何实现 XA 协议呢？答案是非常显然的，和内部 2PC 事务复用完全一样的代码就可以了。

为了验证这一点，我们执行一条 `XA PREPARE` 命令，可以看到果然又来到了 `innobase_xa_prepare`。没错，上文中 InnoDB handlerton 中的 prepare 的接口就叫 `innobase_xa_prepare`，名字中还带着 `xa` 的字样。

```text
Sql_cmd_xa_prepare::execute(Sql_cmd_xa_prepare * const this, THD * thd) (sql/xa.cc:1228)
  Sql_cmd_xa_prepare::trans_xa_prepare(Sql_cmd_xa_prepare * const this, THD * thd) (sql/xa.cc:1194)
    ha_xa_prepare(THD * thd) (sql/handler.cc:1412)
      prepare_one_ht(THD * thd, handlerton * ht) (sql/handler.cc:1345)
        innobase_xa_prepare(handlerton * hton, THD * thd, bool prepare_trx) (storage/innobase/handler/ha_innodb.cc:19084)
```

**对于存储引擎来说，外部 XA 还是内部 XA 并没有什么区别，都走的是同一条代码路径。**

那为什么之前很多人认为 XA 事务性能差呢？我认为主要有两个原因：

一是分布式本身引入的网络代价，例如事务协调者和存储节点往往不在同一个节点上，这必然会增加少许延迟，并引入更多的 IO 中断代价。

二是因为提交延迟增加导致事务从开始到 commit 之间的持有锁的时间增加了。熟悉并发编程的老手一定知道，加锁并不会让性能下降，锁竞争才是性能的最大敌人。

对于原因一，很大程度上是无可避免的，我们认为这就是“分布式的代价”之一。即便如此，在 PolarDB-X 中，我们也做了许多优化，包括：

1. 异步提交（async commit）：将 2PC 提交从 3 次 RPC 缩减到 1 次 RPC，原理我们会在之后的文章中作详细介绍。
2. 一阶段提交（1PC）：对单分片事务采用 1PC 提交，避免不必要的协调开销，原理和异步提交类似
3. 合并提交（group commit）：以物理节点为单位进行 2PC 提交流程，减少 RPC 代价以及 fsync 代价

对于原因二，其实无论是在单机还是在分布式数据库中，都应该尽可能在业务上避免锁竞争。PolarDB-X 引入了全局 MVCC 事务，其中一个动机便是避免在分布式环境中为读加锁（例如 `select for update`），即便不加读锁也可以通过并发转账测试。具体原理可以阅读[《PolarDB-X 分布式事务的实现（二）：InnoDB CTS 扩展》](https://zhuanlan.zhihu.com/p/355413022)。

## 思考：2PC 的本质是什么？

> 就像世界上只有 Paxos 这一种分布式共识算法一样（除非你能避免分布式共识），世界上也只有 2PC 这一种分布式提交算法（除非你能避免分布式提交）。

为什么 Lamport 敢断言 Paxos 是唯一正确的共识算法呢？很简单，因为它是以逻辑推导的方式得到的：给定目标和约束，为了达到目的，只能选择此方案。2PC 也是如此。

分布式提交问题的目标是让所有节点的提交状态达到一致，要么全部提交、要么全部回滚。这个目标等价于：选定其中一个节点，不妨称它为协调者，如何让其他任意节点的状态与它保持一致？

1PC 无论如何也无法解决这个问题，考虑到节点可能在任一时刻宕机，一定无法保证结果一致。

![](./imgs/5.jpg)

我们想把 Node 1、2 上已提交的事务撤消，但从 DB 角度说这显然是不可能的（如果从业务上撤消，那也就是 TCC 柔性事务，这已经超出了数据库事务的范畴）。所以我们必须将提交拆成两个部分，并要求第一个部分（即 Prepare 阶段）仍然有“后悔”的机会——既可以继续提交、也可以撤消，即使宕机也不能打破这一点。

于是你就得到了 2PC。

![](./imgs/6.jpg)

通过这种方式，除协调者以外的节点就可以将选择的权力全都交给协调者，协调者决定了最终这个事务在所有节点上的状态，当然，一定是一致的。

就像我们之前说的， XA 协议不过是 2PC 的一个实现标准，几乎就是 1:1 的翻译。**批判 2PC 或是 XA 是没有必要也是不应该的，这是唯一正确的分布式提交算法。而 MySQL 的 2PC 实现不仅用于分布式事务所用，它的内部存储引擎也同样依赖 2PC 接口保证事务一致性。**

我们常见的分布式数据库基本都采用了 2PC 进行事务提交，区别仅仅在于实现。例如 TiDB 的 Percolator 模型，是 KV 模型上的一种 2PC 实现，本质上是将事务提交日志写到其中一个参与事务的 Key 上；CockrachDB 也类似，不过使用了特殊前缀的 Key 来保存事务日志。

## References

1. [MySQL 8.0.21 Source Code](https://link.zhihu.com/?target=https%3A//github.com/mysql/mysql-server/tree/mysql-cluster-8.0.21)
2. [MySQL · 引擎特性 · InnoDB 事务子系统介绍](https://link.zhihu.com/?target=http%3A//mysql.taobao.org/monthly/2015/12/01/)
3. [图解MySQL组提交(group commit)](https://link.zhihu.com/?target=https%3A//developer.aliyun.com/article/617776)



> 本文摘自： https://zhuanlan.zhihu.com/p/372300181