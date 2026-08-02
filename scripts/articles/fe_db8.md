# Redis 持久化、主从与集群

Redis 是内存数据库，数据存在内存中，一旦进程退出数据就会丢失。持久化机制保证数据在重启后可恢复。主从复制和集群则解决单点故障和水平扩展的问题。本文深入讲解 RDB、AOF、主从复制、哨兵模式和 Cluster 集群。

## RDB 快照

RDB（Redis Database）通过生成内存数据的快照文件（dump.rdb）来持久化数据。

### 触发方式

```bash
# 手动触发
SAVE          # 阻塞主线程，直到 RDB 完成（生产禁用）
BGSAVE        # 后台 fork 子进程执行，不阻塞

# 自动触发（配置文件）
save 900 1    # 900 秒内至少 1 次写入
save 300 10   # 300 秒内至少 10 次写入
save 60 10000 # 60 秒内至少 10000 次写入

# 关闭自动 RDB
save ""
```

### BGSAVE 执行流程

```
1. 主进程 fork() 创建子进程
2. 子进程遍历内存数据，写入临时 RDB 文件
3. 写入完成后，用临时文件替换旧的 dump.rdb
```

fork 利用操作系统的 Copy-On-Write（COW）机制：
- fork 时父子进程共享内存页
- 子进程写入 RDB 时，读取共享内存（不复制）
- 主进程有写操作时，内核将被修改的页复制一份给主进程

### RDB 优缺点

| 优点 | 缺点 |
|---|---|
| 文件紧凑，适合备份和恢复 | 可能丢失最后一次快照后的数据 |
| 恢复速度快（直接加载） | fork 时内存翻倍（COW 最坏情况） |
| 不影响主进程性能 | 大内存 fork 耗时长 |

### RDB 文件格式

```
┌─────────────┬─────────────┬─────────────┬─────────────┐
│ REDIS       │ db_version  │ databases   │ EOF         │
│ 魔数 (5字节) │ 版本号       │ 数据库数据   │ 结束标志     │
└─────────────┴─────────────┴─────────────┴─────────────┘
```

```bash
# 查看 RDB 文件信息
redis-check-rdb dump.rdb
```

## AOF 追加

AOF（Append Only File）以日志追加的方式记录每一条写命令。

### 配置

```bash
# 开启 AOF
appendonly yes

# AOF 文件名
appendfilename "appendonly.aof"

# 刷盘策略
appendfsync everysec    # 每秒刷盘（推荐，最多丢 1 秒数据）
# appendfsync always    # 每次写入都刷盘（最安全，性能最差）
# appendfsync no        # 由操作系统决定（最快，可能丢大量数据）
```

### AOF 文件格式

```bash
*2\r\n$6\r\nSELECT\r\n$1\r\n0\r\n
*3\r\n$3\r\nSET\r\n$5\r\nmykey\r\n$7\r\nmyvalue\r\n
*3\r\n$6\r\nEXPIRE\r\n$5\r\nmykey\r\n$2\r\n60\r\n
```

每条命令按 RESP 协议格式存储，可读性好但文件较大。

### AOF 重写

AOF 文件会不断增长（同一个 key 的多次修改会记录多条命令）。重写（Rewrite）会创建一个新的 AOF 文件，只包含重建当前数据所需的最少命令。

```bash
# 手动触发重写
BGREWRITEAOF

# 自动触发配置
auto-aof-rewrite-percentage 100   # AOF 文件比上次重写后增长 100%
auto-aof-rewrite-min-size 64mb    # AOF 文件至少 64MB 才触发重写
```

重写流程：

```
1. 主进程 fork 子进程
2. 子进程遍历内存数据，生成新的 AOF 文件
3. 主进程继续处理请求，新命令追加到旧 AOF 和 AOF 重写缓冲区
4. 子进程完成重写后，通知主进程
5. 主进程将重写缓冲区的内容追加到新 AOF 文件
6. 替换旧 AOF 文件
```

### AOF 优缺点

| 优点 | 缺点 |
|---|---|
| 数据安全性高（最多丢 1 秒） | 文件比 RDB 大 |
| 可读性好，可手动修复 | 恢复速度比 RDB 慢 |
| 重写机制控制文件大小 | 重写期间有额外内存开销 |

## 混合持久化

Redis 4.0+ 支持混合持久化，结合 RDB 和 AOF 的优势。

```bash
aof-use-rdb-preamble yes  # 开启混合持久化（Redis 4.0+）
```

AOF 重写时，前半部分是 RDB 格式的全量数据，后半部分是增量的 AOF 命令：

```
┌──────────────────────┬──────────────────┐
│ RDB 格式（全量数据）   │ AOF 命令（增量）  │
└──────────────────────┴──────────────────┘
```

混合持久化的优势：恢复时先加载 RDB 部分（快），再回放 AOF 部分（数据完整性好）。

## 主从复制

### 全量同步（Full Sync）

首次连接或无法增量同步时进行全量同步：

```
1. Slave 发送 PSYNC ? -1
2. Master 执行 BGSAVE，生成 RDB 文件
3. Master 将 RDB 发送给 Slave
4. Slave 加载 RDB
5. Master 将 RDB 生成期间的新命令发送给 Slave
```

### 增量同步（Partial Sync）

网络断开重连后的增量同步：

```
1. Slave 发送 PSYNC <replication_id> <offset>
2. Master 检查 repl_backlog 中是否有 Slave 需要的数据
3. 如果有，发送增量数据（基于 repl_backlog）
4. 如果没有，退化为全量同步
```

repl_backlog 是一个环形缓冲区，默认 1MB：

```bash
# 配置 repl_backlog 大小
repl-backlog-size 256mb    # 根据写入速度和网络延迟调整
repl-backlog-ttl 3600      # 所有 Slave 断开后保留 1 小时
```

### 复制配置

```bash
# Slave 配置
replicaof 192.168.1.100 6379   # 指向 Master
masterauth "password"          # Master 密码
replica-read-only yes          # Slave 只读

# 延迟写入（避免 Slave 全量同步期间被查询到过期数据）
replica-lazy-flush yes
```

## 哨兵模式（Sentinel）

哨兵是 Redis 的高可用方案，自动监控和故障切换。

### 架构

```
Sentinel 1 ──┐
Sentinel 2 ──┼──► Master ◄── Slave 1
Sentinel 3 ──┘          └── Slave 2
```

### 配置

```bash
# sentinel.conf
port 26379
sentinel monitor mymaster 192.168.1.100 6379 2  # 至少 2 个哨兵同意才切换
sentinel auth-pass mymaster "password"
sentinel down-after-milliseconds mymaster 5000   # 5 秒无响应判为主观下线
sentinel failover-timeout mymaster 60000         # 故障切换超时 60 秒
sentinel parallel-syncs mymaster 1               # 同时同步的 Slave 数量
```

### 故障检测

1. **主观下线（SDOWN）**：单个 Sentinel 认为 Master 不可达
2. **客观下线（ODOWN）**：`quorum` 个 Sentinel 都认为 Master 不可达

### 故障切换流程

```
1. Sentinel 检测到 Master 客观下线
2. Sentinel 之间选举 Leader（Raft 协议）
3. Leader Sentinel 选择新 Master：
   - 优先选择 replica-priority 最小的
   - 优先选择复制偏移量最大的（数据最新）
   - 优先选择 runid 最小的
4. 将选中的 Slave 提升为 Master（REPLICAOF NO ONE）
5. 通知其他 Slave 指向新 Master
6. 通知客户端新 Master 地址
```

### 客户端连接哨兵

```python
from redis.sentinel import Sentinel

sentinel = Sentinel([
    ('192.168.1.100', 26379),
    ('192.168.1.101', 26379),
    ('192.168.1.102', 26379)
], socket_timeout=0.5)

# 获取 Master
master = sentinel.master_for('mymaster', password='password')
master.set('key', 'value')

# 获取 Slave（读操作）
slave = sentinel.slave_for('mymaster', password='password')
slave.get('key')
```

## Cluster 分片

Redis Cluster 是官方的分布式方案，支持数据分片和自动故障转移。

### 数据分片

Redis Cluster 将数据划分为 16384 个槽（slot），每个节点负责一部分槽。

```
槽分配示例（3 主 3 从）：
Node A (Master): 槽 0 - 5460      (5461 个)
Node B (Master): 槽 5461 - 10922  (5462 个)
Node C (Master): 槽 10923 - 16383 (5461 个)
```

Key 到槽的映射：

```
slot = CRC16(key) % 16384
```

```bash
# 查看 key 属于哪个槽
CLUSTER KEYSLOT mykey

# 使用 hash tag 控制 key 分配到同一槽
SET {user:1001}:name "张三"
SET {user:1001}:age 25
# {user:1001} 相同，两个 key 会分配到同一个槽
```

### 集群搭建

```bash
# 创建 6 节点集群（3 主 3 从）
redis-cli --cluster create \
    192.168.1.100:6379 192.168.1.101:6379 192.168.1.102:6379 \
    192.168.1.103:6379 192.168.1.104:6379 192.168.1.105:6379 \
    --cluster-replicas 1
```

每个节点配置：

```bash
# redis.conf
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 15000
```

### MOVED 和 ASK 重定向

当客户端发送命令到错误的节点时：

```bash
# MOVED 重定向（槽已确定在目标节点）
GET mykey
# (error) MOVED 1234 192.168.1.101:6379

# ASK 重定向（槽正在迁移中）
GET mykey
# (error) ASK 1234 192.168.1.101:6379
```

- **MOVED**：永久重定向，客户端应更新本地槽映射表
- **ASK**：临时重定向，只本次请求转发，不更新映射表

### 槽迁移

在线迁移槽用于扩容和缩容：

```bash
# 1. 在目标节点导入槽
CLUSTER SETSLOT 1234 IMPORTING <source-node-id>

# 2. 在源节点导出槽
CLUSTER SETSLOT 1234 MIGRATING <target-node-id>

# 3. 逐个迁移 key
CLUSTER GETKEYSINSLOT 1234 100        # 获取槽中的 key
MIGRATE <target-host> <target-port> "" 0 5000 KEYS key1 key2 ...  # 迁移

# 4. 通知所有节点槽的新归属
CLUSTER SETSLOT 1234 NODE <target-node-id>
```

### 集群故障转移

```
1. 节点之间通过 Gossip 协议互相通信
2. 当半数以上 Master 认为某 Master 下线（PFAIL → FAIL）
3. 该 Master 的 Slave 发起选举
4. 其他 Master 投票
5. 获得多数票的 Slave 提升为新 Master
```

### 集群限制

| 限制 | 说明 |
|---|---|
| 不支持多 key 跨节点操作 | 除非使用 hash tag |
| 不支持 SELECT db | 只能用 db 0 |
| 事务受限 | 事务中的 key 必须在同一节点 |
| Lua 脚本受限 | 所有 key 必须在同一节点 |
| 数据库数量 | 只有 db 0 |

## 运维建议

```bash
# 监控集群状态
redis-cli --cluster check 192.168.1.100:6379

# 查看集群信息
redis-cli -c -h 192.168.1.100 CLUSTER INFO
redis-cli -c -h 192.168.1.100 CLUSTER NODES

# 集群扩容：添加新节点
redis-cli --cluster add-node <new-host>:<new-port> <existing-host>:<existing-port>

# 分配槽给新节点
redis-cli --cluster reshard <host>:<port>

# 集群缩容：移除节点
redis-cli --cluster del-node <host>:<port> <node-id>
```

生产环境建议至少 3 主 3 从，跨机房部署时要考虑网络延迟对集群通信的影响。
