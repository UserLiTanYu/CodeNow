# MySQL 运维：备份恢复与高可用

数据库是系统的核心资产，备份恢复能力决定了数据安全的底线，高可用架构决定了服务的连续性。本文讲解 MySQL 的备份方案、主从复制、故障切换以及读写分离的实现。

## 备份方案

### mysqldump 逻辑备份

`mysqldump` 是 MySQL 自带的逻辑备份工具，将数据库导出为 SQL 文件。

#### 全量备份

```bash
# 备份单个数据库
mysqldump -u root -p --single-transaction --routines --triggers \
    --databases mydb > mydb_backup.sql

# 备份所有数据库
mysqldump -u root -p --single-transaction --routines --triggers \
    --all-databases > full_backup.sql

# 只备份表结构
mysqldump -u root -p --no-data mydb > schema_only.sql

# 只备份数据（不含建表语句）
mysqldump -u root -p --no-create-info mydb > data_only.sql
```

关键参数说明：

| 参数 | 说明 |
|---|---|
| `--single-transaction` | InnoDB 一致性快照备份，不锁表 |
| `--routines` | 包含存储过程和函数 |
| `--triggers` | 包含触发器 |
| `--master-data=2` | 记录 binlog 位点（注释形式） |
| `--flush-logs` | 刷新 binlog，便于增量恢复 |
| `--hex-blob` | 二进制数据用十六进制导出 |

#### 恢复

```bash
# 恢复数据库
mysql -u root -p mydb < mydb_backup.sql

# 恢复所有数据库
mysql -u root -p < full_backup.sql
```

#### mysqldump 的局限

- 备份速度慢（逐行导出）
- 恢复速度慢（逐条执行 SQL）
- 大数据库（超过 50GB）不适合
- 备份期间数据库仍可读写（`--single-transaction`），但 DDL 操作可能导致备份不一致

### XtraBackup 物理备份

Percona XtraBackup 是物理备份工具，直接拷贝数据文件，速度远快于 mysqldump。

#### 安装

```bash
# Ubuntu/Debian
apt install percona-xtrabackup-80

# CentOS/RHEL
yum install percona-xtrabackup-80
```

#### 全量备份

```bash
# 全量备份
xtrabackup --backup --target-dir=/backup/full \
    --user=root --password=123456

# 准备备份（应用 redo log）
xtrabackup --prepare --target-dir=/backup/full

# 恢复
# 1. 停止 MySQL
systemctl stop mysql

# 2. 清空数据目录
rm -rf /var/lib/mysql/*

# 3. 恢复备份
xtrabackup --copy-back --target-dir=/backup/full \
    --datadir=/var/lib/mysql

# 4. 修改权限
chown -R mysql:mysql /var/lib/mysql

# 5. 启动 MySQL
systemctl start mysql
```

#### 增量备份

```bash
# 全量备份
xtrabackup --backup --target-dir=/backup/full \
    --user=root --password=123456

# 第一次增量备份（基于全量）
xtrabackup --backup --target-dir=/backup/inc1 \
    --incremental-basedir=/backup/full \
    --user=root --password=123456

# 第二次增量备份（基于第一次增量）
xtrabackup --backup --target-dir=/backup/inc2 \
    --incremental-basedir=/backup/inc1 \
    --user=root --password=123456

# 恢复增量备份
# 1. 准备全量备份（不回滚未提交事务）
xtrabackup --prepare --apply-log-only --target-dir=/backup/full

# 2. 应用第一次增量
xtrabackup --prepare --apply-log-only --target-dir=/backup/full \
    --incremental-dir=/backup/inc1

# 3. 应用第二次增量（最后一次不加 --apply-log-only）
xtrabackup --prepare --target-dir=/backup/full \
    --incremental-dir=/backup/inc2

# 4. 恢复
xtrabackup --copy-back --target-dir=/backup/full \
    --datadir=/var/lib/mysql
```

### binlog 增量恢复

binlog 记录了所有修改数据的 SQL 语句，可以实现基于时间点的增量恢复。

```bash
# 查看 binlog 列表
SHOW BINARY LOGS;

# 查看 binlog 内容
mysqlbinlog --base64-output=DECODE-ROWS -v mysql-bin.000010

# 基于时间点恢复
mysqlbinlog --start-datetime='2025-06-15 10:00:00' \
    --stop-datetime='2025-06-15 12:00:00' \
    mysql-bin.000010 | mysql -u root -p

# 基于位点恢复
mysqlbinlog --start-position=154 --stop-position=1024 \
    mysql-bin.000010 | mysql -u root -p
```

### 备份策略建议

```
备份策略：
├── 每天凌晨 2:00 全量备份（XtraBackup）
├── 每小时增量备份（XtraBackup）
├── binlog 实时归档到远程存储
└── 每周验证备份可恢复性

备份保留策略：
├── 本地保留最近 7 天
├── 远程存储保留最近 30 天
└── 每月全量备份保留 1 年
```

## 主从复制

### 异步复制

异步复制是 MySQL 默认的复制模式。主库执行完事务后立即返回客户端，不等待从库确认。

```
Master → Binlog Dump Thread → Network → Slave IO Thread → Relay Log → Slave SQL Thread → 数据
```

#### 配置主库

```ini
# my.cnf
[mysqld]
server-id = 1
log-bin = mysql-bin
binlog-format = ROW           # 推荐 ROW 格式
binlog-row-image = FULL
sync-binlog = 1               # 每次事务提交刷盘
innodb_flush_log_at_trx_commit = 1
```

```sql
-- 创建复制用户
CREATE USER 'repl'@'%' IDENTIFIED BY 'repl_password';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;

-- 查看主库状态
SHOW MASTER STATUS;
-- 记录 File 和 Position
```

#### 配置从库

```ini
# my.cnf
[mysqld]
server-id = 2
relay-log = relay-bin
read-only = ON
super-read-only = ON         # 阻止超级用户写入
```

```sql
-- 配置主库信息（MySQL 8.0.23+）
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST = '192.168.1.100',
    SOURCE_USER = 'repl',
    SOURCE_PASSWORD = 'repl_password',
    SOURCE_LOG_FILE = 'mysql-bin.000001',
    SOURCE_LOG_POS = 154;

-- 启动复制
START REPLICA;

-- 查看复制状态
SHOW REPLICA STATUS\G
```

关键状态字段：

| 字段 | 正常值 | 说明 |
|---|---|---|
| Replica_IO_Running | Yes | IO 线程运行状态 |
| Replica_SQL_Running | Yes | SQL 线程运行状态 |
| Seconds_Behind_Source | 0 | 复制延迟（秒） |
| Last_IO_Error | 空 | IO 线程错误 |
| Last_SQL_Error | 空 | SQL 线程错误 |

### 半同步复制

半同步复制要求至少一个从库确认收到 binlog 后，主库才返回客户端。

```sql
-- 主库安装半同步插件
INSTALL PLUGIN rpl_semi_sync_source SONAME 'semisync_source.so';
SET GLOBAL rpl_semi_sync_source_enabled = ON;
SET GLOBAL rpl_semi_sync_source_timeout = 5000;  -- 超时 5 秒退化为异步

-- 从库安装半同步插件
INSTALL PLUGIN rpl_semi_sync_replica SONAME 'semisync_replica.so';
SET GLOBAL rpl_semi_sync_replica_enabled = ON;

-- 重启复制使生效
STOP REPLICA;
START REPLICA;
```

半同步复制的问题：
- 主库写入延迟增加（需等待从库 ACK）
- 如果所有从库都超时，自动退化为异步复制
- 从库 crash 不影响主库，但数据可能不一致

### 组复制（Group Replication）

MySQL Group Replication（MGR）是基于 Paxos 协议的多主复制方案。

```ini
# my.cnf（每个节点）
[mysqld]
server-id = 1
gtid-mode = ON
enforce-gtid-consistency = ON
binlog-checksum = NONE
log-bin = mysql-bin
binlog-format = ROW
plugin_load_add = 'group_replication.so'
group_replication_group_name = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
group_replication_start_on_boot = OFF
group_replication_local_address = "192.168.1.100:33061"
group_replication_group_seeds = "192.168.1.100:33061,192.168.1.101:33061,192.168.1.102:33061"
group_replication_single_primary_mode = ON  # 单主模式
```

```sql
-- 初始化（第一个节点）
SET GLOBAL group_replication_bootstrap_group = ON;
START GROUP_REPLICATION;
SET GLOBAL group_replication_bootstrap_group = OFF;

-- 加入其他节点
START GROUP_REPLICATION;
```

## 故障切换

### MHA（Master High Availability）

MHA 是最经典的 MySQL 高可用方案，由 Manager 和 Node 两部分组成。

**架构**：

```
MHA Manager (监控节点)
    ├── Master (主库)
    ├── Slave 1 (从库 1)
    └── Slave 2 (从库 2)
```

**故障切换流程**：
1. Manager 检测到 Master 宕机
2. 从所有 Slave 中选择数据最新的作为新 Master
3. 将其他 Slave 的差异 binlog 补齐
4. 将 VIP 漂移到新 Master
5. 重新配置其他 Slave 指向新 Master

**配置要点**：

```ini
# /etc/mha/app1.cnf
[server default]
manager_workdir=/var/log/mha/app1
manager_log=/var/log/mha/app1/manager.log
user=mha
password=mha_password
ssh_user=root
repl_user=repl
repl_password=repl_password
ping_interval=3

[server1]
hostname=192.168.1.100
port=3306

[server2]
hostname=192.168.1.101
port=3306
candidate_master=1

[server3]
hostname=192.168.1.102
port=3306
no_master=1
```

### Orchestrator

Orchestrator 是 GitHub 开源的 MySQL 拓扑管理和故障切换工具，比 MHA 更现代。

```bash
# 安装
wget https://github.com/openark/orchestrator/releases/download/v3.2.6/orchestrator_3.2.6_amd64.deb
dpkg -i orchestrator_3.2.6_amd64.deb
```

核心特性：
- Web UI 可视化拓扑
- 自动故障检测和切换
- 支持多种复制拓扑
- 与 Consul/etcd 集成实现服务发现

### 读写分离

读写分离是将写操作路由到主库，读操作路由到从库的架构。

#### ProxySQL 中间件

ProxySQL 是最流行的 MySQL 代理中间件，支持读写分离、连接池、查询缓存、查询路由。

**安装配置**：

```bash
# 安装
apt install proxysql

# 或 Docker
docker run -d --name proxysql -p 6033:6033 -p 6032:6032 proxysql/proxysql
```

**核心配置**：

```sql
-- 连接 ProxySQL 管理端口
mysql -u admin -padmin -h 127.0.0.1 -P 6032

-- 添加 MySQL 后端服务器
INSERT INTO mysql_servers (hostgroup_id, hostname, port, weight)
VALUES
    (10, '192.168.1.100', 3306, 1000),  -- 写组 (hostgroup 10)
    (20, '192.168.1.101', 3306, 1000),  -- 读组 (hostgroup 20)
    (20, '192.168.1.102', 3306, 1000);  -- 读组 (hostgroup 20)

-- 配置读写分离规则
INSERT INTO mysql_query_rules (rule_id, active, match_pattern, destination_hostgroup)
VALUES
    (1, 1, '^SELECT .* FOR UPDATE$', 10),  -- SELECT FOR UPDATE 走写组
    (2, 1, '^SELECT', 20);                  -- 普通 SELECT 走读组

-- 配置监控用户
UPDATE global_variables SET variable_value='monitor' WHERE variable_name='mysql-monitor_username';
UPDATE global_variables SET variable_value='monitor_pass' WHERE variable_name='mysql-monitor_password';

-- 加载配置到运行时
LOAD MYSQL SERVERS TO RUNTIME;
LOAD MYSQL QUERY RULES TO RUNTIME;
LOAD MYSQL VARIABLES TO RUNTIME;

-- 保存到磁盘
SAVE MYSQL SERVERS TO DISK;
SAVE MYSQL QUERY RULES TO SHUNNED;
SAVE MYSQL VARIABLES TO DISK;
```

**应用端连接**：

```yaml
# Spring Boot 配置
spring:
  datasource:
    url: jdbc:mysql://192.168.1.200:6033/mydb  # ProxySQL 地址
    username: app_user
    password: app_password
```

#### ShardingSphere-JDBC 读写分离

ShardingSphere-JDBC 是客户端分片方案，无需代理中间件。

```yaml
# application.yml
spring:
  shardingsphere:
    datasource:
      names: master,slave0,slave1
      master:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://192.168.1.100:3306/mydb
        username: root
        password: root
      slave0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://192.168.1.101:3306/mydb
        username: root
        password: root
      slave1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://192.168.1.102:3306/mydb
        username: root
        password: root
    rules:
      readwrite-splitting:
        data-sources:
          myds:
            write-data-source-name: master
            read-data-source-names: slave0,slave1
            load-balancer-name: round-robin
        load-balancers:
          round-robin:
            type: ROUND_ROBIN
```

## 复制延迟监控

复制延迟是主从架构最常见的问题。

### 监控方式

```sql
-- 方式一：SHOW REPLICA STATUS
SHOW REPLICA STATUS\G
-- Seconds_Behind_Source: 0

-- 方式二：pt-heartbeat（更精确）
-- 主库安装
pt-heartbeat --update --database heartbeat --create-table --daemonize

-- 从库监控
pt-heartbeat --monitor --database heartbeat --master-server-id=1
```

### 延迟原因与优化

| 原因 | 优化方案 |
|---|---|
| 从库单线程回放 | 开启多线程复制（`replica_parallel_workers`） |
| 大事务 | 拆分大事务为小事务 |
| 从库硬件差 | 升级从库配置 |
| 网络延迟 | 主从同机房部署 |
| 从库查询压力大 | 增加从库数量分摊读压力 |

```sql
-- 开启多线程复制（MySQL 8.0+）
SET GLOBAL replica_parallel_workers = 4;
SET GLOBAL replica_parallel_type = 'LOGICAL_CLOCK';
SET GLOBAL replica_preserve_commit_order = ON;
```
