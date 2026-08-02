# Redis 核心数据结构与底层实现

Redis 之所以快，不仅因为纯内存操作和单线程避免上下文切换，更因为其精心设计的底层数据结构。本文逐个剖析 Redis 核心数据类型的底层实现，帮助你在实际业务中做出正确的选型。

## String — SDS

### 底层实现

Redis 的 String 类型底层使用 SDS（Simple Dynamic String）而非 C 语言原生字符串。

```
C 字符串：  \0
            ┌───┬───┬───┬───┬───┐
            │ H │ e │ l │ l │ o │ \0
            └───┴───┴───┴───┴───┘

SDS 结构：
┌──────┬──────┬───────────────────┬───┐
│ len  │ alloc│ buf[]             │ \0│
│  5   │  10  │ H │ e │ l │ l │ o│   │
└──────┴──────┴───────────────────┴───┘
```

SDS 相比 C 字符串的优势：

| 特性 | C 字符串 | SDS |
|---|---|---|
| 获取长度 | O(n) 遍历 | O(1) 读 len |
| 缓冲区溢出 | 可能 | 不会（自动扩容） |
| 二进制安全 | 否（遇 \0 截断） | 是 |
| 减少内存重分配 | 否 | 空间预分配 + 惰性释放 |

### 编码优化

Redis 会根据值的内容自动选择最省内存的编码：

- **整数**：值是整数且不超过 `long` 范围时，用 `int` 编码（不额外分配 SDS）
- **短字符串**：长度 ≤ 44 字节用 `embstr` 编码（redisObject 和 SDS 连续内存分配）
- **长字符串**：长度 > 44 字节用 `raw` 编码（redisObject 和 SDS 分开分配）

```bash
# 查看编码
redis> SET num 12345
OK
redis> OBJECT ENCODING num
"int"

redis> SET short "hello"
OK
redis> OBJECT ENCODING short
"embstr"

redis> SET long "aaaa...（超过 44 字节）"
OK
redis> OBJECT ENCODING long
"raw"
```

### 内存优化建议

- 能用整数就不用字符串（`SET counter 100` 优于 `SET counter "100"`）
- 避免存储大 value（单个 String value 不要超过 10KB，大对象用 Hash 拆分）
- 利用整数编码：计数器、ID 等场景天然省内存

## List — quicklist

### 底层实现

Redis 3.2+ 的 List 底层使用 quicklist，它是 ziplist 和 linkedlist 的混合体。

```
quicklist = 双向链表，每个节点是一个 ziplist

quicklist:
┌──────────┐    ┌──────────┐    ┌──────────┐
│ ziplist  │◄──►│ ziplist  │◄──►│ ziplist  │
│ [a, b, c]│    │ [d, e, f]│    │ [g, h]   │
└──────────┘    └──────────┘    └──────────┘
```

配置参数：

```
list-max-ziplist-size -2    # 每个 ziplist 节点大小限制
# -2: 每个节点最大 8KB
# -1: 每个节点最大 4KB（默认）
# 正数: 每个节点最多 N 个元素

list-compress-depth 0       # 压缩深度
# 0: 不压缩
# 1: 首尾各 1 个节点不压缩，中间压缩
# 2: 首尾各 2 个节点不压缩，中间压缩
```

### 常用操作性能

| 操作 | 时间复杂度 | 说明 |
|---|---|---|
| LPUSH / RPUSH | O(1) | 头尾插入 |
| LPOP / RPOP | O(1) | 头尾弹出 |
| LINDEX | O(N) | 按索引访问 |
| LRANGE | O(S+N) | 范围查询 |
| LLEN | O(1) | 获取长度 |
| LREM | O(N+M) | 删除指定值 |

### 典型应用场景

- **消息队列**：`LPUSH` + `BRPOP` 实现简单的阻塞队列
- **最新列表**：用 `LPUSH` + `LTRIM` 保留最新 N 条
- **时间线**：用户发布内容后 `LPUSH` 到粉丝的时间线列表

```bash
# 简单消息队列
LPUSH task_queue '{"task":"send_email","to":"user@example.com"}'
BRPOP task_queue 30  # 阻塞等待，超时 30 秒

# 保留最新 100 条
LPUSH timeline "new_post_id"
LTRIM timeline 0 99
```

## Hash — ziplist / hashtable

### 底层实现

Hash 在元素较少时使用 ziplist（内存紧凑），元素多时自动转为 hashtable。

```bash
# ziplist 编码（小 Hash）
HSET user:1001 name "张三" age "25"
OBJECT ENCODING user:1001
# "listpack"（Redis 7.0+ 用 listpack 替代 ziplist）

# hashtable 编码（大 Hash）
# 当 field 数量超过 hash-max-ziplist-entries（默认 512）
# 或 value 长度超过 hash-max-ziplist-value（默认 64 字节）
# 自动转为 hashtable
```

### 内存优化：大 Hash 拆分

如果一个 Hash 有上万个 field，可以拆分为多个小 Hash：

```python
# 不推荐：一个大 Hash
HSET user:1001:profile field1 val1 field2 val2 ... field10000 val10000

# 推荐：拆分到多个小 Hash（每个 ≤ 512 个 field）
# 按 field 名哈希分片
shard = hash(field_name) % 16
HSET user:1001:profile:{shard} field_name value
```

### Hash vs String 存储对象

| 方案 | 优点 | 缺点 |
|---|---|---|
| String JSON | 简单直观 | 更新单个字段需读写整个 JSON |
| Hash | 可单独读写字段 | 内存略多（每个 field 有开销） |

```bash
# String 方案
SET user:1001 '{"name":"张三","age":25,"email":"zhangsan@example.com"}'
# 更新年龄需要 GET → 解析 → 修改 → SET

# Hash 方案
HMSET user:1001 name "张三" age 25 email "zhangsan@example.com"
# 更新年龄直接
HINCRBY user:1001 age 1
```

当对象字段经常需要单独更新时，用 Hash 更合适。

## IntSet / Set — intset / hashtable

### IntSet

当 Set 中全是整数且元素数量较少时，使用 intset 编码：

```
intset 结构：
┌──────┬──────┬──────────────────┐
│ type │length│ contents[]       │
│INT16 │  3   │ 1, 5, 100        │
└──────┴──────┴──────────────────┘
```

intset 按升序存储，支持二分查找。当插入的整数超出当前类型范围时，会自动升级（如 INT16 → INT32）。

### Set

Set 元素较多或包含非整数值时，使用 hashtable 编码（与 Hash 相同，但只有 field 没有 value）。

```bash
SADD tags "java" "redis" "mysql"
SMEMBERS tags           # 所有元素
SISMEMBER tags "java"   # 判断成员是否存在，O(1)
SCARD tags              # 元素数量，O(1)
```

### Set 的典型应用

- **标签系统**：`SADD article:1001:tags "java" "spring"`
- **社交关系**：关注/粉丝/共同关注
- **抽奖**：`SRANDMEMBER`（不删除）/ `SPOP`（删除）
- **去重**：利用 Set 的唯一性

```bash
# 共同关注
SINTER user:1001:following user:1002:following

# 推荐好友（你关注的人关注了谁，但你没关注）
SDIFF user:1002:following user:1001:following
```

## ZSet — ziplist / skiplist

### 底层实现

ZSet（有序集合）在元素较少时用 ziplist，元素多时用 skiplist + hashtable 的组合结构。

```bash
# ziplist 编码（小 ZSet）
ZADD rank 100 "player1" 200 "player2"
OBJECT ENCODING rank
# "listpack"

# skiplist 编码（大 ZSet）
# 元素数量超过 zset-max-ziplist-entries（默认 128）
# 或 value 长度超过 zset-max-ziplist-value（默认 64 字节）
```

### 跳表（Skip List）结构

```
Level 3:  HEAD ──────────────────────────────► 91 ──────────► NULL
Level 2:  HEAD ──────────────► 37 ────────────► 91 ──────────► NULL
Level 1:  HEAD ──► 19 ──► 37 ──► 56 ──► 72 ──► 91 ──────────► NULL
```

跳表通过多层索引实现 O(log n) 的查找。每一层是下一层的"快速通道"。

| 操作 | 时间复杂度 |
|---|---|
| ZADD | O(log N) |
| ZREM | O(log N) |
| ZSCORE | O(1)（hashtable 辅助） |
| ZRANK | O(log N) |
| ZRANGE | O(log N + M) |
| ZRANGEBYSCORE | O(log N + M) |

为什么用跳表不用红黑树？
- 跳表实现更简单
- 范围查询更高效（直接遍历底层链表）
- 并发友好（局部锁）
- 内存占用可控

### ZSet 典型应用

```bash
# 排行榜
ZADD leaderboard 9500 "player:1001"
ZADD leaderboard 8700 "player:1002"
ZADD leaderboard 9200 "player:1003"
ZREVRANGE leaderboard 0 9 WITHSCORES  # Top 10

# 延迟队列
ZADD delay_queue <timestamp> '{"task":"send_reminder","data":{...}}'
# 消费者轮询
ZRANGEBYSCORE delay_queue 0 <current_timestamp> LIMIT 0 10

# 滑动窗口限流
ZADD rate:user:1001 <current_timestamp> <unique_id>
ZREMRANGEBYSCORE rate:user:1001 0 <window_start>
ZCARD rate:user:1001  # 窗口内请求数
```

## Stream

Redis 5.0 引入的 Stream 是专门为消息队列设计的数据类型，支持消费者组、消息确认。

### 基本操作

```bash
# 发送消息
XADD mystream * name "张三" action "login"
# 返回消息 ID: "1686742800000-0"

# 读取消息
XREAD COUNT 10 STREAMS mystream 0  # 从头读

# 从指定 ID 之后读
XREAD BLOCK 5000 STREAMS mystream 1686742800000-0

# 创建消费者组
XGROUP CREATE mystream mygroup 0

# 消费者组读取
XREADGROUP GROUP mygroup consumer1 COUNT 10 BLOCK 5000 STREAMS mystream >

# 确认消息
XACK mystream mygroup 1686742800000-0

# 查看 Stream 信息
XINFO STREAM mystream
XINFO GROUPS mystream
```

### 消费者组 vs List

| 特性 | List | Stream |
|---|---|---|
| 消息持久化 | 是 | 是 |
| 消费者组 | 不支持 | 支持 |
| 消息确认 | 不支持 | XACK |
| 消息回溯 | 不支持 | 支持 |
| 消息 ID | 无 | 自动生成有序 ID |
| 阻塞读取 | BRPOP | XREAD BLOCK |

Stream 是更成熟的消息队列方案，适合需要消息确认和多消费者组的场景。

## 数据结构选型指南

| 场景 | 推荐类型 | 原因 |
|---|---|---|
| 缓存对象 | String 或 Hash | Hash 适合频繁更新字段 |
| 计数器 | String（INCR） | 原子自增 |
| 分布式锁 | String（SET NX EX） | 原子操作 |
| 队列 | List 或 Stream | Stream 支持消费者组 |
| 标签/集合运算 | Set | 原生集合操作 |
| 排行榜 | ZSet | 有序 + 分数 |
| 时间窗口统计 | ZSet | 按分数（时间戳）范围查询 |
| 地理位置 | GEO（基于 ZSet） | GEORADIUS 范围查询 |
| 布隆过滤器 | Bitmap / BF | 概率性存在判断 |

## 内存优化通用建议

```bash
# 查看内存使用
INFO memory

# 查看 key 的内存占用（Redis 4.0+）
MEMORY USAGE key

# 设置最大内存
CONFIG SET maxmemory 4gb

# 淘汰策略
CONFIG SET maxmemory-policy allkeys-lru
```

淘汰策略：

| 策略 | 说明 |
|---|---|
| noeviction | 不淘汰，写满后拒绝写入（默认） |
| allkeys-lru | 所有 key 中淘汰最近最少使用 |
| allkeys-lfu | 所有 key 中淘汰最不常用 |
| volatile-lru | 有过期时间的 key 中淘汰 LRU |
| volatile-lfu | 有过期时间的 key 中淘汰 LFU |
| volatile-ttl | 淘汰最快过期的 key |
| allkeys-random | 随机淘汰 |
| volatile-random | 有过期时间的随机淘汰 |

缓存场景推荐 `allkeys-lru`，需要保证某些 key 不被淘汰时用 `volatile-lru`。
