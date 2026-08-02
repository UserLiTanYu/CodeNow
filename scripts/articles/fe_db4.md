# MySQL 锁机制与死锁排查

锁是数据库并发控制的核心手段。MySQL InnoDB 实现了多种粒度的锁来平衡并发性能和数据一致性。本文全面讲解 InnoDB 的锁类型、加锁规则，并通过实际案例演示死锁的产生原因和排查方法。

## 锁的分类

### 按粒度划分

#### 全局锁

全局锁锁住整个数据库实例，使其处于只读状态。典型场景是全库逻辑备份。

```sql
-- 加全局锁
FLUSH TABLES WITH READ LOCK;

-- 释放
UNLOCK TABLES;
```

更好的替代方案是使用 `mysqldump --single-transaction`，它利用 MVCC 在一致性快照下导出数据，不需要加全局锁：

```bash
mysqldump --single-transaction --routines --triggers --all-databases > backup.sql
```

#### 表锁

表锁锁住整张表，MyISAM 时代的主要锁类型。InnoDB 主要使用行锁，但某些场景仍会用到表锁：

```sql
-- 显式表锁
LOCK TABLES articles READ;   -- 读锁（共享）
LOCK TABLES articles WRITE;  -- 写锁（排他）

-- 释放
UNLOCK TABLES;
```

InnoDB 的表锁更多是内部使用的"表级意向锁"，由行锁自动触发表级意向锁，不需要用户显式加锁。

#### 行锁

行锁是 InnoDB 的核心，锁住索引记录（不是数据行本身）。行锁的实现依赖于索引：如果没有可用索引，InnoDB 会退化为锁住所有行（实际效果等同表锁）。

```sql
-- 行锁的类型
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;     -- 排他行锁
SELECT * FROM accounts WHERE id = 1 FOR SHARE;      -- 共享行锁（MySQL 8.0+）
SELECT * FROM accounts WHERE id = 1 LOCK IN SHARE MODE;  -- 共享行锁（旧语法）
```

### 按模式划分

#### 共享锁（S Lock）

共享锁又称读锁，多个事务可以同时持有同一资源的共享锁。

```sql
-- 事务 A
BEGIN;
SELECT * FROM accounts WHERE id = 1 FOR SHARE;
-- 事务 B 可以同时读
SELECT * FROM accounts WHERE id = 1 FOR SHARE;
-- 但事务 B 不能修改
UPDATE accounts SET balance = 900 WHERE id = 1;  -- 阻塞，等待事务 A 释放
```

#### 排他锁（X Lock）

排他锁又称写锁，与任何其他锁互斥。

```sql
-- 事务 A
BEGIN;
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;
-- 事务 B 读取（不加锁）仍然可以，走 MVCC 快照读
SELECT * FROM accounts WHERE id = 1;  -- 正常返回
-- 事务 B 加共享锁被阻塞
SELECT * FROM accounts WHERE id = 1 FOR SHARE;  -- 阻塞
```

### 意向锁（Intention Lock）

意向锁是表级锁，用于表明事务意图在表中某些行上加共享锁或排他锁。意向锁的作用是让表锁和行锁的检查更高效，不需要逐行检查。

| 意向锁类型 | 含义 |
|---|---|
| IS（Intention Shared） | 事务打算对表中的某些行加 S 锁 |
| IX（Intention Exclusive） | 事务打算对表中的某些行加 X 锁 |

意向锁之间不互斥，意向锁只与表锁互斥（用于 `LOCK TABLES` 场景）。实际开发中不需要关注意向锁，它是 InnoDB 内部自动管理的。

## InnoDB 的行锁类型

InnoDB 的行锁细分三种类型：

### 记录锁（Record Lock）

锁定索引中的一条记录。等值查询命中唯一索引时，只加记录锁。

```sql
-- 假设 id 是主键
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;
-- 仅锁住 id=1 这条记录
```

### 间隙锁（Gap Lock）

锁定索引记录之间的间隙（左开右开区间），防止其他事务在间隙中插入新记录。间隙锁只在 REPEATABLE READ 隔离级别下存在。

```sql
-- 假设表中 id 有 1, 5, 10 三条记录
SELECT * FROM accounts WHERE id = 7 FOR UPDATE;
-- 锁住间隙 (5, 10)，防止插入 id=6,7,8,9
```

间隙锁之间不互斥，两个事务可以同时对同一间隙加间隙锁。间隙锁只防止插入（Insert Intention Lock）。

### 临键锁（Next-Key Lock）

临键锁 = 记录锁 + 间隙锁，锁定一个左开右闭区间 `(gap, record]`。这是 InnoDB 在 RR 隔离级别下的默认行锁类型。

```sql
-- 假设表中 id 有 1, 5, 10 三条记录
-- 临键锁可能锁住的区间：
-- (-∞, 1], (1, 5], (5, 10], (10, +∞)

SELECT * FROM accounts WHERE id >= 5 AND id < 10 FOR UPDATE;
-- 锁住 (1, 5] 和 (5, 10)
```

### 插入意向锁（Insert Intention Lock）

插入意向锁是 INSERT 操作在插入前对间隙加的一种特殊间隙锁。多个事务可以同时对同一间隙加插入意向锁（只要插入的位置不同）。

## 加锁规则

InnoDB 的加锁规则取决于查询类型、索引类型和隔离级别。

### 等值查询

**唯一索引等值查询**：
- 记录存在：只加记录锁（Next-Key Lock 退化为 Record Lock）
- 记录不存在：加间隙锁

```sql
-- id 是唯一索引，id=5 存在
SELECT * FROM t WHERE id = 5 FOR UPDATE;
-- 加 Record Lock on id=5

-- id 是唯一索引，id=7 不存在（假设 5 和 10 之间）
SELECT * FROM t WHERE id = 7 FOR UPDATE;
-- 加 Gap Lock on (5, 10)
```

**非唯一索引等值查询**：
- 记录存在：加 Next-Key Lock + 右侧间隙的 Gap Lock（覆盖所有可能的插入位置）
- 记录不存在：加间隙锁

```sql
-- age 是普通索引，age=25 对应 id=3,8
SELECT * FROM t WHERE age = 25 FOR UPDATE;
-- Next-Key Lock on age=25 (id=3), age=25 (id=8)
-- Gap Lock on 下一个 age 值的间隙
```

### 范围查询

范围查询会锁住所有满足条件的记录及其间隙。

```sql
-- id 是主键
SELECT * FROM t WHERE id >= 10 AND id < 20 FOR UPDATE;
-- 锁住 [10, 20) 范围内所有记录，以及 (20, 下一个值) 的间隙
```

### UPDATE/DELETE 的加锁

`UPDATE` 和 `DELETE` 的加锁规则与 `SELECT ... FOR UPDATE` 基本一致。区别在于 `UPDATE` 还需要对涉及的二级索引加锁。

```sql
UPDATE accounts SET balance = 900 WHERE name = '张三';
-- 如果 name 有索引，先锁 name 索引的记录
-- 再锁主键索引对应行
```

## 死锁案例与排查

### 死锁的产生

两个或多个事务互相持有对方需要的锁，形成循环等待。

```sql
-- 事务 A
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;  -- 锁住 id=1
UPDATE accounts SET balance = balance + 100 WHERE id = 2;  -- 等待 id=2 的锁

-- 事务 B
BEGIN;
UPDATE accounts SET balance = balance - 50 WHERE id = 2;   -- 锁住 id=2
UPDATE accounts SET balance = balance + 50 WHERE id = 1;   -- 等待 id=1 的锁 → 死锁
```

InnoDB 自动检测死锁，选择一个代价小的事务回滚：

```
ERROR 1213 (40001): Deadlock found when trying to get lock; try restarting transaction
```

### 间隙锁导致的死锁

间隙锁是死锁的常见来源：

```sql
-- 事务 A
BEGIN;
SELECT * FROM t WHERE id = 7 FOR UPDATE;  -- Gap Lock on (5, 10)

-- 事务 B
SELECT * FROM t WHERE id = 8 FOR UPDATE;  -- Gap Lock on (5, 10) → 不冲突

-- 事务 A
INSERT INTO t VALUES (7, 'a');  -- 需要插入意向锁，但被 B 的间隙锁阻塞

-- 事务 B
INSERT INTO t VALUES (8, 'b');  -- 需要插入意向锁，但被 A 的间隙锁阻塞
-- → 死锁
```

### SHOW ENGINE INNODB STATUS

排查死锁最重要的命令：

```sql
SHOW ENGINE INNODB STATUS\G
```

输出中的 `LATEST DETECTED DEADLOCK` 部分包含最近一次死锁的详细信息：

```
------------------------
LATEST DETECTED DEADLOCK
------------------------
2025-06-15 14:30:22
*** (1) TRANSACTION:
TRANSACTION 12345, ACTIVE 3 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 2 lock struct(s), heap size 1136, 1 row lock(s)
MySQL thread id 10, OS thread handle 140234567890, query id 100 localhost root updating
UPDATE accounts SET balance = balance + 100 WHERE id = 2

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 10 page no 3 n bits 72 index PRIMARY of table `test`.`accounts`
trx id 12345 lock_mode X locks rec but not gap

*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 10 page no 3 n bits 72 index PRIMARY of table `test`.`accounts`
trx id 12345 lock_mode X locks rec but not gap waiting

*** (2) TRANSACTION:
...

*** WE ROLL BACK TRANSACTION (1)
```

关键信息：
- **HOLDS THE LOCK(S)**：事务已持有的锁
- **WAITING FOR THIS LOCK**：事务等待的锁
- **WE ROLL BACK TRANSACTION**：被回滚的事务

### 死锁排查工具

```sql
-- 查看当前锁等待
SELECT
    r.trx_id AS waiting_trx,
    r.trx_mysql_thread_id AS waiting_thread,
    r.trx_query AS waiting_query,
    b.trx_id AS blocking_trx,
    b.trx_mysql_thread_id AS blocking_thread,
    b.trx_query AS blocking_query
FROM information_schema.innodb_lock_waits w
JOIN information_schema.innodb_trx b ON w.blocking_trx_id = b.trx_id
JOIN information_schema.innodb_trx r ON w.requesting_trx_id = r.trx_id;

-- MySQL 8.0+ 使用 performance_schema
SELECT * FROM performance_schema.data_lock_waits;
SELECT * FROM performance_schema.data_locks;

-- 查看当前所有事务
SELECT * FROM information_schema.innodb_trx;
```

### 死锁预防策略

| 策略 | 说明 |
|---|---|
| 统一加锁顺序 | 多表操作时，按固定顺序（如表名排序）加锁 |
| 缩小事务范围 | 减少事务持锁时间，避免事务内做远程调用 |
| 使用低隔离级别 | RC 隔离级别没有间隙锁，减少死锁概率 |
| 合理设计索引 | 让查询走索引，避免行锁升级为表锁 |
| 重试机制 | 捕获死锁异常后自动重试 |

```java
// Java 应用中的死锁重试示例
@Retryable(value = DeadlockLoserDataAccessException.class, maxAttempts = 3)
public void transfer(Long fromId, Long toId, BigDecimal amount) {
    accountMapper.deduct(fromId, amount);
    accountMapper.add(toId, amount);
}
```

## 乐观锁与悲观锁

### 悲观锁

悲观锁假设冲突会发生，在读取数据时就加锁，直到事务结束才释放。

```sql
-- 通过 SELECT ... FOR UPDATE 实现悲观锁
BEGIN;
SELECT balance FROM accounts WHERE id = 1 FOR UPDATE;
-- 检查余额
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
COMMIT;
```

悲观锁适合冲突频繁的场景，但会降低并发性能。

### 乐观锁

乐观锁假设冲突不会发生，只在提交时检查数据是否被其他事务修改。常用实现方式：

**版本号方式**：

```sql
-- 表结构
ALTER TABLE accounts ADD COLUMN version INT DEFAULT 0;

-- 更新时检查版本号
UPDATE accounts
SET balance = balance - 100, version = version + 1
WHERE id = 1 AND version = 5;

-- 如果 affected_rows = 0，说明被其他事务修改了，需要重试
```

**时间戳方式**：

```sql
ALTER TABLE accounts ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE accounts
SET balance = balance - 100, updated_at = NOW()
WHERE id = 1 AND updated_at = '2025-06-15 14:00:00';
```

### 选择建议

| 场景 | 推荐 |
|---|---|
| 读多写少 | 乐观锁 |
| 冲突频繁 | 悲观锁 |
| 响应时间要求高 | 乐观锁 |
| 数据一致性要求极高 | 悲观锁 |

## 锁监控最佳实践

生产环境应持续监控锁相关指标：

```sql
-- 查看锁等待超时设置
SHOW VARIABLES LIKE 'innodb_lock_wait_timeout';  -- 默认 50 秒

-- 查看行锁等待情况
SHOW GLOBAL STATUS LIKE 'Innodb_row_lock%';
-- Innodb_row_lock_time：总等待时间
-- Innodb_row_lock_waits：等待次数
-- Innodb_row_lock_time_avg：平均等待时间

-- 开启死锁日志记录
SET GLOBAL innodb_print_all_deadlocks = ON;
-- 死锁信息会记录到错误日志中
```

对于高并发系统，建议将 `innodb_lock_wait_timeout` 设置为较小的值（如 5-10 秒），避免长时间锁等待导致连接堆积。
