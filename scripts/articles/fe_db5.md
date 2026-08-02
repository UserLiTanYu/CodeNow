# MySQL 索引优化与慢查询分析

索引是 MySQL 性能优化的第一利器。理解 B+树的结构原理、掌握索引的使用规则和失效场景、学会分析慢查询日志，是每个后端开发者必须具备的能力。本文系统讲解 MySQL 索引的底层结构、优化技巧和慢查询排查方法。

## B+树结构

InnoDB 使用 B+树作为索引的底层数据结构。

### B+树的特点

```
                    [30 | 60]                    ← 非叶子节点（索引）
                   /    |    \
          [10|20]    [40|50]    [70|80]          ← 非叶子节点
         /  |  \    /  |  \    /  |  \
       [数据] [数据] [数据] [数据] [数据] [数据]   ← 叶子节点（数据）
        ↔      ↔      ↔      ↔      ↔      ↔
       叶子节点间用双向链表连接
```

B+树的关键特性：
- **所有数据都在叶子节点**，非叶子节点只存索引键值
- **叶子节点用双向链表连接**，支持范围查询
- **树高通常为 3-4 层**，亿级数据也只需 3-4 次磁盘 I/O
- **非叶子节点可以存更多键值**，因为不存数据，扇出（Fan-out）大

### 为什么不用 B 树、红黑树、哈希表

| 数据结构 | 不适合的原因 |
|---|---|
| B 树 | 非叶子节点也存数据，扇出小，树高大 |
| 红黑树 | 二叉树，树高 O(log n)，数据量大时树太高 |
| 哈希表 | 不支持范围查询，只支持等值查询 |
| 跳表 | 内存结构，不适合磁盘存储 |

## 聚簇索引与非聚簇索引

### 聚簇索引（Clustered Index）

聚簇索引的叶子节点存储的是完整的行数据。InnoDB 的聚簇索引就是主键索引。

```
主键索引（聚簇索引）叶子节点：
[id=1, name=张三, email=zhangsan@example.com, age=25, ...]
[id=2, name=李四, email=lisi@example.com, age=30, ...]
```

每张表只能有一个聚簇索引。InnoDB 选择索引的优先顺序：
1. 显式定义的主键
2. 第一个非空的唯一索引
3. 自动生成隐藏的 6 字节 ROW_ID

### 非聚簇索引（Secondary Index）

非聚簇索引的叶子节点存储的不是完整行数据，而是主键值。通过非聚簇索引查找数据需要"回表"。

```
二级索引（email）叶子节点：
[email=abc@example.com, id=5]
[email=def@example.com, id=2]

回表流程：
1. 在 email 索引中找到 email=abc@example.com，得到 id=5
2. 用 id=5 去主键索引中查找完整行数据
```

### 回表的代价

每次回表都是一次随机 I/O。如果回表次数太多，优化器可能放弃使用二级索引，直接全表扫描。

```sql
-- 假设 status 只有 0 和 1 两个值，且数据均匀分布
SELECT * FROM orders WHERE status = 0;
-- 如果表有 100 万行，status=0 约 50 万行
-- 使用 status 索引需要回表 50 万次 → 优化器会选择全表扫描
```

## 覆盖索引

覆盖索引（Covering Index）是指查询所需的所有列都包含在索引中，无需回表。

```sql
-- 联合索引
ALTER TABLE orders ADD INDEX idx_status_created (status, created_at);

-- 覆盖索引查询（只需索引，无需回表）
EXPLAIN SELECT status, created_at FROM orders WHERE status = 1;
-- Extra: Using index

-- 非覆盖索引查询（需要回表）
EXPLAIN SELECT * FROM orders WHERE status = 1;
-- Extra: 无 Using index
```

覆盖索引是性能优化的利器。设计索引时，尽量让高频查询的 SELECT 列和 WHERE 条件列都在同一个索引中。

### 如何判断是否覆盖索引

`EXPLAIN` 输出中 `Extra` 列包含 `Using index` 表示使用了覆盖索引。

## 索引下推（ICP）

索引条件下推（Index Condition Pushdown，ICP）是 MySQL 5.6 引入的优化。它将部分 WHERE 条件的判断下推到存储引擎层，在索引遍历时就过滤不满足条件的记录，减少回表次数。

```sql
ALTER TABLE users ADD INDEX idx_name_age (name, age);

-- ICP 生效的查询
SELECT * FROM users WHERE name LIKE '张%' AND age = 25;
```

**无 ICP 的执行流程**：
1. 存储引擎通过 `name LIKE '张%'` 找到所有 name 以"张"开头的记录
2. 逐条回表获取完整行
3. Server 层过滤 `age = 25`

**有 ICP 的执行流程**：
1. 存储引擎通过 `name LIKE '张%'` 找到所有 name 以"张"开头的记录
2. 在索引层直接检查 `age = 25`，不满足的直接跳过
3. 只对满足条件的记录回表

`EXPLAIN` 输出中 `Extra` 列包含 `Using index condition` 表示使用了 ICP。

## 最左前缀原则

联合索引遵循最左前缀原则：查询条件必须从索引的最左列开始，按顺序使用，索引才能生效。

```sql
ALTER TABLE t ADD INDEX idx_a_b_c (a, b, c);
```

| 查询条件 | 是否使用索引 |
|---|---|
| `WHERE a = 1` | 是（使用 a） |
| `WHERE a = 1 AND b = 2` | 是（使用 a, b） |
| `WHERE a = 1 AND b = 2 AND c = 3` | 是（使用 a, b, c） |
| `WHERE b = 2` | 否（跳过最左列 a） |
| `WHERE b = 2 AND c = 3` | 否（跳过最左列 a） |
| `WHERE a = 1 AND c = 3` | 部分（只使用 a，c 无法使用索引） |
| `WHERE a = 1 AND b > 2 AND c = 3` | 部分（a 等值，b 范围，c 无法使用） |
| `WHERE a = 1 ORDER BY b` | 是（a 等值 + b 排序，避免 filesort） |

最左前缀原则的本质：联合索引 `(a, b, c)` 的数据先按 a 排序，a 相同再按 b 排序，b 相同再按 c 排序。跳过 a 直接查 b，数据在 b 维度是无序的，无法使用索引。

### 索引列的顺序设计

设计联合索引时的优先级：
1. 等值查询的列放前面
2. 范围查询的列放后面
3. 排序列放最后（如果需要 ORDER BY）

```sql
-- 查询：WHERE status = 1 AND created_at > '2025-01-01' ORDER BY id
-- 推荐索引
ALTER TABLE orders ADD INDEX idx_status_created_id (status, created_at, id);
```

## 索引失效场景

以下情况会导致索引无法使用：

### 对索引列使用函数或运算

```sql
-- 索引失效
SELECT * FROM users WHERE YEAR(created_at) = 2025;
SELECT * FROM users WHERE age + 1 > 20;

-- 改写为
SELECT * FROM users WHERE created_at >= '2025-01-01' AND created_at < '2026-01-01';
SELECT * FROM users WHERE age > 19;
```

### 隐式类型转换

```sql
-- phone 是 VARCHAR 类型
-- 索引失效：WHERE phone = 13800138000（数字比较，隐式转换）
SELECT * FROM users WHERE phone = 13800138000;

-- 正确写法
SELECT * FROM users WHERE phone = '13800138000';
```

隐式转换的本质等同于对索引列使用 `CAST()` 函数。

### LIKE 左模糊

```sql
-- 索引失效（左模糊）
SELECT * FROM users WHERE name LIKE '%张';

-- 索引生效（右模糊）
SELECT * FROM users WHERE name LIKE '张%';
```

### OR 条件中有未索引列

```sql
-- 如果 age 没有索引，整个查询无法使用索引
SELECT * FROM users WHERE name = '张三' OR age = 25;

-- 改写为 UNION
SELECT * FROM users WHERE name = '张三'
UNION
SELECT * FROM users WHERE age = 25;
```

### NOT IN / NOT EXISTS / !=

```sql
-- 通常不走索引（取决于优化器判断）
SELECT * FROM users WHERE status != 0;
SELECT * FROM users WHERE status NOT IN (1, 2);
```

### IS NULL / IS NOT NULL

MySQL 8.0+ 对 `IS NULL` 的索引使用有了很大改善，但 `IS NOT NULL` 在大部分场景下仍然不走索引。

## EXPLAIN 输出解读

`EXPLAIN` 是分析查询性能最重要的工具。

```sql
EXPLAIN SELECT * FROM orders WHERE user_id = 1001 AND status = 1;
```

### 各列含义

| 列名 | 说明 |
|---|---|
| id | 查询序号，id 相同从上到下执行，id 不同大的先执行 |
| select_type | 查询类型：SIMPLE/PRIMARY/SUBQUERY/DERIVED/UNION |
| table | 访问的表 |
| partitions | 匹配的分区 |
| **type** | **访问类型，性能从好到差** |
| possible_keys | 可能使用的索引 |
| **key** | **实际使用的索引** |
| key_len | 索引使用的字节长度 |
| ref | 索引的哪一列被使用 |
| **rows** | **预估扫描行数** |
| filtered | 按条件过滤后的行百分比 |
| **Extra** | **额外信息** |

### type 列的值（从好到差）

| type | 说明 |
|---|---|
| system | 表只有一行 |
| const | 通过主键或唯一索引等值查询，最多一行 |
| eq_ref | JOIN 时驱动表每行在被驱动表通过主键/唯一索引匹配一行 |
| ref | 通过普通索引等值查询 |
| range | 索引范围扫描 |
| index | 全索引扫描 |
| ALL | 全表扫描 |

一般优化目标是让查询达到 `ref` 或 `range` 级别。

### Extra 列的常见值

| Extra | 含义 |
|---|---|
| Using index | 覆盖索引 |
| Using index condition | 索引下推（ICP） |
| Using where | Server 层过滤 |
| Using temporary | 使用临时表 |
| Using filesort | 额外排序（需优化） |
| Using join buffer | JOIN 缓冲区 |

`Using filesort` 和 `Using temporary` 是需要重点关注的优化信号。

## 慢查询日志

### 配置

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = ON;

-- 设置慢查询阈值（秒）
SET GLOBAL long_query_time = 1;

-- 日志文件路径
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

-- 记录没有使用索引的查询
SET GLOBAL log_queries_not_using_indexes = ON;
```

永久配置写入 `my.cnf`：

```ini
[mysqld]
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 1
log_queries_not_using_indexes = 1
```

### 慢查询日志格式

```
# Time: 2025-06-15T06:30:22.123456Z
# User@Host: root[root] @ localhost []  Id:    42
# Query_time: 2.500000  Lock_time: 0.000100  Rows_sent: 100  Rows_examined: 500000
SET timestamp=1718431822;
SELECT * FROM orders WHERE status = 0 ORDER BY created_at DESC LIMIT 100;
```

关键字段：
- **Query_time**：查询耗时
- **Lock_time**：锁等待时间
- **Rows_sent**：返回行数
- **Rows_examined**：扫描行数（与 Rows_sent 差异大说明效率低）

## pt-query-digest

`pt-query-digest` 是 Percona Toolkit 中的慢查询分析工具，比 MySQL 自带的 `mysqldumpslow` 更强大。

### 安装

```bash
# Ubuntu/Debian
apt install percona-toolkit

# CentOS/RHEL
yum install percona-toolkit
```

### 基本用法

```bash
# 分析慢查询日志
pt-query-digest /var/log/mysql/slow.log > slow_report.txt

# 只显示前 10 条最慢的查询
pt-query-digest --limit 10 /var/log/mysql/slow.log

# 分析特定时间范围的查询
pt-query-digest --since '2025-06-15 00:00:00' --until '2025-06-15 23:59:59' /var/log/mysql/slow.log

# 分析 binlog
mysqlbinlog mysql-bin.000001 | pt-query-digest --type binlog
```

### 报告解读

```
# Profile
# Rank Query ID                     Response time  Calls  R/Call  V/M
# ==== ============================== ============== ====== ======= =====
#    1 0xABCDEF1234567890            120.0000  50.0%    200  0.6000  0.01 SELECT orders
#    2 0x1234567890ABCDEF             80.0000  33.3%    100  0.8000  0.02 SELECT users

# Query 1: 20.00 QPS, 12.00x concurrency, ID 0xABCDEF...
# Ratio 10.00x (RW), 1.00x (full scan)
# Attribute        pct   total     min     max     avg     95%  stddev
# ============ === ======= ======= ======= ======= ======= ======
# Count           50     200
# Exec time       50    120s   300ms      2s   600ms   900ms   100ms
# Rows sent       40   10000      50     500      50      50       0
# Rows examine    60  500000    1000   10000    2500    5000    1000
```

关注指标：
- **Response time**：总响应时间占比
- **95%**：95 分位响应时间
- **Rows examine vs Rows sent**：扫描行数与返回行数比值，比值越大效率越低

### 实时分析

```bash
# 实时捕获慢查询并分析
pt-query-digest --processlist h=localhost --interval 1 --output slowlog

# 通过 tcpdump 抓包分析
tcpdump -s 65535 -x -nn -q -tttt -i any port 3306 -c 10000 > mysql.tcp.txt
pt-query-digest --type tcpdump mysql.tcp.txt
```

## 索引优化实战案例

### 案例一：分页查询优化

```sql
-- 慢查询：OFFSET 越大越慢
SELECT * FROM orders ORDER BY id LIMIT 1000000, 10;

-- 优化方案：延迟关联
SELECT o.* FROM orders o
JOIN (SELECT id FROM orders ORDER BY id LIMIT 1000000, 10) t
ON o.id = t.id;
```

子查询走覆盖索引，只取主键，再回表取 10 条完整数据。

### 案例二：COUNT 优化

```sql
-- InnoDB 的 COUNT(*) 需要遍历索引
-- 如果不需要精确计数，可以：
-- 1. 使用缓存计数
-- 2. 使用 SHOW TABLE STATUS 的估算值
SHOW TABLE STATUS LIKE 'orders'\G

-- 3. 维护计数表
CREATE TABLE table_counts (
    table_name VARCHAR(50) PRIMARY KEY,
    row_count BIGINT
);
```

### 案例三：ORDER BY 优化

```sql
-- filesort 的查询
SELECT * FROM orders WHERE user_id = 1001 ORDER BY created_at DESC;

-- 添加索引消除 filesort
ALTER TABLE orders ADD INDEX idx_user_created (user_id, created_at);
```

当索引的列顺序与 `ORDER BY` 一致且方向相同时，可以避免额外排序。
