# InnoDB 存储引擎与事务深入

MySQL 默认的 InnoDB 存储引擎是整个数据库的核心。理解 InnoDB 的架构和事务实现机制，是做好性能调优和故障排查的前提。本文从 InnoDB 的内存与磁盘架构讲起，深入事务 ACID 的实现原理、四种隔离级别以及 MVCC 机制。

## InnoDB 架构总览

InnoDB 的架构可以分为内存结构和磁盘结构两大部分。

### 内存结构

#### Buffer Pool

Buffer Pool 是 InnoDB 最重要的内存区域，用于缓存表数据和索引数据。当查询需要读取某页时，InnoDB 首先检查 Buffer Pool 中是否已有该页（命中），没有则从磁盘读入（未命中）。

```sql
-- 查看 Buffer Pool 大小
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';

-- 查看命中率
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_read%';
-- 命中率 = 1 - (Innodb_buffer_pool_reads / Innodb_buffer_pool_read_requests)
```

Buffer Pool 采用改进的 LRU（最近最少使用）算法管理页面。传统 LRU 的问题是全表扫描会污染缓存，InnoDB 将 LRU 列表分为两部分：

- **Young 区（热数据）**：约 5/8，存放最近频繁访问的页
- **Old 区（冷数据）**：约 3/8，新读入的页先进入 Old 区，只有在被再次访问且间隔超过 `innodb_old_blocks_time`（默认 1 秒）后才移到 Young 区

#### Change Buffer

Change Buffer 用于缓存非唯一二级索引页的修改操作。当更新操作命中了不在 Buffer Pool 中的二级索引页时，InnoDB 不立即从磁盘读取该页，而是将变更暂存到 Change Buffer，等该页被读入时再合并（Merge）。

```sql
-- 查看 Change Buffer 配置
SHOW VARIABLES LIKE 'innodb_change_buffer%';
-- innodb_change_buffer_max_size 默认 25，表示 Change Buffer 最多占 Buffer Pool 的 25%
```

#### Log Buffer

Log Buffer 缓存待写入磁盘的 redo log。通过批量写入减少磁盘 I/O。

```sql
SHOW VARIABLES LIKE 'innodb_log_buffer_size';  -- 默认 16MB
```

Log Buffer 刷新到磁盘的时机：
- 每秒刷新一次（由 `innodb_flush_log_at_timeout` 控制）
- 事务提交时（取决于 `innodb_flush_log_at_trx_commit` 设置）
- Log Buffer 空间不足时

#### Adaptive Hash Index

InnoDB 会监控索引页的访问模式，对频繁访问的页自动建立哈希索引，将 B+树的 O(log n) 查找优化为 O(1)。

```sql
SHOW VARIABLES LIKE 'innodb_adaptive_hash_index';  -- 默认 ON
```

### 磁盘结构

#### 表空间（Tablespace）

- **系统表空间（ibdata1）**：存储数据字典、undo log（MySQL 5.7 之前）、doublewrite buffer、change buffer
- **独立表空间（.ibd 文件）**：`innodb_file_per_table=ON`（默认）时，每张表一个 `.ibd` 文件
- **通用表空间**：用户自定义的共享表空间
- **临时表空间**：存储临时表数据
- **Undo 表空间**：MySQL 8.0 将 undo log 独立到 undo 表空间

#### Redo Log

Redo Log 是物理日志，记录的是"某个数据页上的某个偏移量做了什么修改"。用于崩溃恢复（Crash Recovery）。

```sql
-- Redo Log 文件配置
SHOW VARIABLES LIKE 'innodb_log_file_size';     -- 默认 48MB
SHOW VARIABLES LIKE 'innodb_log_files_in_group'; -- 默认 2
```

MySQL 8.0.30 之前，redo log 存储在 `ib_logfile0`、`ib_logfile1` 中。8.0.30+ 使用 `innodb_redo_log_capacity` 统一管理。

#### Undo Log

Undo Log 是逻辑日志，记录的是"与当前操作相反的操作"（INSERT 对应 DELETE，UPDATE 对应旧值）。用于事务回滚和 MVCC 的多版本读取。

#### Doublewrite Buffer

Doublewrite Buffer 解决部分写失效（Partial Write）问题。InnoDB 先将脏页写入 Doublewrite Buffer（顺序写），再写入数据文件（随机写）。如果写数据文件时崩溃，恢复时从 Doublewrite Buffer 中找到完整页。

```sql
SHOW VARIABLES LIKE 'innodb_doublewrite';  -- 默认 ON
```

## 事务 ACID 实现原理

事务的 ACID 四个特性由不同的机制保证：

| 特性 | 含义 | 实现机制 |
|---|---|---|
| 原子性（Atomicity） | 事务要么全部成功，要么全部回滚 | Undo Log |
| 一致性（Consistency） | 事务前后数据满足完整性约束 | 由 AID 共同保证 |
| 隔离性（Isolation） | 事务间互不干扰 | MVCC + 锁 |
| 持久性（Durability） | 提交的数据不会丢失 | Redo Log |

### Redo Log 保证持久性

WAL（Write-Ahead Logging）：修改数据前先写 redo log。即使数据页尚未刷盘，崩溃恢复时可通过 redo log 重放已提交事务的修改。

```
事务修改流程：
1. 从磁盘读取数据页到 Buffer Pool
2. 修改 Buffer Pool 中的数据页（此时该页成为脏页）
3. 将修改写入 Log Buffer
4. 事务提交时将 Log Buffer 刷入磁盘（redo log 持久化）
5. 后台线程择机将脏页刷入磁盘（checkpoint）
```

如果步骤 4 之后、步骤 5 之前崩溃，重启后 InnoDB 通过 redo log 恢复数据。

### Undo Log 保证原子性

事务执行过程中，每一步操作都会写入对应的 undo log。回滚时，InnoDB 按 undo log 的逆序执行反向操作。

```
UPDATE accounts SET balance = balance - 100 WHERE id = 1;

对应的 undo log：
UPDATE accounts SET balance = balance + 100 WHERE id = 1;
（记录旧值，以便回滚时恢复）
```

## 四种隔离级别

SQL 标准定义了四种隔离级别，隔离程度从低到高：

| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
|---|---|---|---|
| READ UNCOMMITTED | 可能 | 可能 | 可能 |
| READ COMMITTED（RC） | 不会 | 可能 | 可能 |
| REPEATABLE READ（RR） | 不会 | 不会 | 可能* |
| SERIALIZABLE | 不会 | 不会 | 不会 |

*InnoDB 的 RR 通过 Next-Key Lock 解决了大部分幻读场景。

```sql
-- 查看当前隔离级别
SELECT @@transaction_isolation;

-- 设置隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

### 脏读（Dirty Read）

读到其他事务未提交的数据。RC 及以上隔离级别通过 MVCC 避免。

### 不可重复读（Non-Repeatable Read）

同一事务内两次读取同一行，结果不同（被其他事务的 UPDATE 修改了）。RR 及以上隔离级别通过 MVCC 避免。

### 幻读（Phantom Read）

同一事务内两次执行同一范围查询，第二次返回了第一次没有的行（被其他事务 INSERT 了）。InnoDB 的 RR 隔离级别通过 Next-Key Lock 防止大部分幻读，但快照读场景下仍可能出现"幻读"。

### InnoDB 默认隔离级别

InnoDB 默认使用 `REPEATABLE READ`，但实际上它通过 MVCC + Next-Key Lock 提供了接近 `SERIALIZABLE` 的隔离能力，同时保持了较好的并发性能。

## MVCC 机制

MVCC（Multi-Version Concurrency Control，多版本并发控制）是 InnoDB 实现高并发读写的核心机制。它允许读操作不加锁，通过读取数据的旧版本来实现事务隔离。

### 隐藏列

InnoDB 在每行数据中隐藏了三个字段：

| 列名 | 大小 | 说明 |
|---|---|---|
| DB_TRX_ID | 6 字节 | 最近修改该行的事务 ID |
| DB_ROLL_PTR | 7 字节 | 回滚指针，指向 undo log 中的旧版本 |
| DB_ROW_ID | 6 字节 | 隐含主键（无显式主键时使用） |

每次修改一行数据，都会将旧版本通过 `DB_ROLL_PTR` 链接到 undo log 中，形成版本链。

### 版本链

```
当前行（最新版本）
  DB_TRX_ID = 103
  DB_ROLL_PTR → undo log 旧版本
      DB_TRX_ID = 101
      DB_ROLL_PTR → 更旧版本
          DB_TRX_ID = 98
          DB_ROLL_PTR → NULL（最初版本）
```

读取数据时，根据 Read View 的规则沿版本链找到当前事务可见的版本。

### Read View

Read View 是事务在某个时刻创建的"快照"，包含以下关键信息：

| 字段 | 说明 |
|---|---|
| m_ids | 创建 Read View 时所有活跃（未提交）事务的 ID 列表 |
| min_trx_id | m_ids 中的最小值 |
| max_trx_id | 系统应分配的下一个事务 ID（当前最大事务 ID + 1） |
| creator_trx_id | 创建该 Read View 的事务 ID |

### 可见性判断规则

当事务读取一行数据时，取该行的 `DB_TRX_ID` 与 Read View 比较：

1. **DB_TRX_ID == creator_trx_id**：自己修改的，可见
2. **DB_TRX_ID < min_trx_id**：该版本在 Read View 创建前已提交，可见
3. **DB_TRX_ID >= max_trx_id**：该版本在 Read View 创建后才产生，不可见
4. **min_trx_id <= DB_TRX_ID < max_trx_id**：检查 DB_TRX_ID 是否在 m_ids 中
   - 在 m_ids 中：该版本创建时事务未提交，不可见
   - 不在 m_ids 中：该版本创建时事务已提交，可见

如果当前版本不可见，沿版本链找旧版本，直到找到可见版本或链尾。

### RC 与 RR 的 Read View 差异

- **READ COMMITTED**：每次 SELECT 都创建新的 Read View（能看到其他事务已提交的最新数据）
- **REPEATABLE READ**：只在事务第一次 SELECT 时创建 Read View（整个事务期间看到的快照一致）

```sql
-- 演示 RR 的不可重复读解决
-- Session A
BEGIN;
SELECT balance FROM accounts WHERE id = 1;  -- 读到 1000

-- Session B
UPDATE accounts SET balance = 900 WHERE id = 1;
COMMIT;

-- Session A（同一事务，再次读取）
SELECT balance FROM accounts WHERE id = 1;  -- 仍然读到 1000
COMMIT;
```

Session A 在事务开始时创建了 Read View，Session B 的修改对 Session A 不可见，因为 Session B 的事务 ID 在 Session A 的 m_ids 范围内。

## 当前读与快照读

### 快照读（Snapshot Read）

普通的 `SELECT` 语句（不加锁）是快照读，读取的是 MVCC 版本链中的某个历史版本：

```sql
-- 快照读
SELECT * FROM accounts WHERE id = 1;
```

### 当前读（Current Read）

读取数据的最新版本，并加锁。以下操作都是当前读：

```sql
-- 显式加锁的 SELECT
SELECT * FROM accounts WHERE id = 1 FOR SHARE;     -- 共享锁
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;    -- 排他锁

-- DML 操作
INSERT INTO accounts VALUES (...);
UPDATE accounts SET balance = 900 WHERE id = 1;
DELETE FROM accounts WHERE id = 1;
```

### 幻读在 RR 下的真实表现

在 RR 隔离级别下，如果事务先用快照读，再用当前读，可能出现"幻读"：

```
Session A                           Session B
BEGIN;
SELECT * FROM t WHERE id > 5;
-- 快照读，返回 id=6,8
                                    INSERT INTO t VALUES (7, 'new');
                                    COMMIT;
SELECT * FROM t WHERE id > 5
FOR UPDATE;
-- 当前读，返回 id=6,7,8
-- 7 是"幻行"
```

InnoDB 通过 Next-Key Lock（临键锁）在当前读时锁住范围，防止其他事务插入新行，从而避免幻读。但快照读和当前读混用时，仍可能看到不一致的结果。

## 脏页刷盘与 Checkpoint

Buffer Pool 中被修改的页称为脏页（Dirty Page），需要择机刷入磁盘。

### 刷盘时机

- **Redo Log 写满**：必须暂停所有更新，推进 checkpoint
- **Buffer Pool 不足**：LRU 淘汰脏页时需要先刷盘
- **后台线程定期刷**：`innodb_io_capacity` 控制刷盘速度
- **数据库正常关闭**

### 刷盘策略（innodb_flush_log_at_trx_commit）

该参数控制 redo log 的刷盘策略：

| 值 | 行为 | 性能 | 安全性 |
|---|---|---|---|
| 0 | 每秒写入 OS buffer 并 fsync | 最好 | 可能丢失 1 秒数据 |
| 1 | 每次提交都 fsync | 最差 | 最安全（默认） |
| 2 | 每次提交写入 OS buffer，每秒 fsync | 好 | 操作系统崩溃可能丢失 1 秒 |

生产环境建议使用默认值 `1`，确保数据不丢失。从库可以设为 `2` 降低 I/O 压力。

### Doublewrite 的作用

即使有 redo log，也需要 doublewrite buffer。因为 redo log 记录的是对某个数据页偏移量的修改，如果数据页本身就损坏了（部分写失效），redo log 的重放也是错误的。Doublewrite 保证数据页的完整性。
