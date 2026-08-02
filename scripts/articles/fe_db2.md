# 数据库设计与范式理论

好的数据库设计是系统稳定的基石。表结构设计不合理，后期要么被数据冗余折磨，要么被复杂 JOIN 拖垮性能。本文从范式理论出发，讲解 ER 模型、表关系设计、主键选型、索引设计原则，以及大规模场景下的分库分表策略。

## 三大范式

范式（Normal Form）是关系数据库设计的理论基础，目标是消除数据冗余和更新异常。

### 第一范式（1NF）

**规则**：每个列的值必须是原子的，不可再分。

```sql
-- 违反 1NF：skills 列包含多个值
CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    skills VARCHAR(200)  -- 'Java,Python,Go'
);

-- 符合 1NF：每行只存一个技能
CREATE TABLE employee_skills (
    employee_id INT,
    skill VARCHAR(50),
    PRIMARY KEY (employee_id, skill)
);
```

1NF 是最基本的约束。实际开发中违反 1NF 的常见场景包括：用逗号分隔的字符串存储多值、在 JSON 列中存储结构化数据（MySQL 5.7+ 的 JSON 类型虽然是半结构化的，但严格来说违反 1NF）。

### 第二范式（2NF）

**规则**：在满足 1NF 的基础上，非主键列必须完全依赖主键，不能只依赖主键的一部分（消除部分依赖）。

2NF 针对的是**复合主键**场景。如果表只有一个列作为主键，自动满足 2NF。

```sql
-- 违反 2NF：product_name 只依赖 product_id，不依赖 order_id
CREATE TABLE order_items (
    order_id INT,
    product_id INT,
    product_name VARCHAR(100),  -- 部分依赖
    quantity INT,
    price DECIMAL(10,2),
    PRIMARY KEY (order_id, product_id)
);

-- 符合 2NF：拆分
CREATE TABLE order_items (
    order_id INT,
    product_id INT,
    quantity INT,
    price DECIMAL(10,2),
    PRIMARY KEY (order_id, product_id)
);

CREATE TABLE products (
    product_id INT PRIMARY KEY,
    product_name VARCHAR(100)
);
```

### 第三范式（3NF）

**规则**：在满足 2NF 的基础上，非主键列不能依赖其他非主键列（消除传递依赖）。

```sql
-- 违反 3NF：department_name 依赖 department_id，而 department_id 依赖 employee_id
CREATE TABLE employees (
    employee_id INT PRIMARY KEY,
    name VARCHAR(50),
    department_id INT,
    department_name VARCHAR(100)  -- 传递依赖
);

-- 符合 3NF
CREATE TABLE employees (
    employee_id INT PRIMARY KEY,
    name VARCHAR(50),
    department_id INT
);

CREATE TABLE departments (
    department_id INT PRIMARY KEY,
    department_name VARCHAR(100)
);
```

### BCNF（Boyce-Codd 范式）

BCNF 是 3NF 的加强版：每个决定因素都必须是候选键。在实际工程中，满足 3NF 通常就够了，BCNF 的场景较为少见。

## 反范式化

范式化消除冗余，但查询时需要大量 JOIN。在读多写少的场景下，适度反范式化能显著提升查询性能。

### 常见反范式化手段

| 手段 | 说明 | 示例 |
|---|---|---|
| 冗余列 | 在子表中冗余父表常用字段 | 订单表中冗余 `product_name` |
| 派生列 | 存储计算结果 | 用户表中存 `order_count`、`total_spent` |
| 预连接表 | 将多表 JOIN 结果物化到宽表 | 搜索引擎的文档宽表 |

### 反范式化的代价

冗余数据需要在写入时同步维护。常见方案：

- **应用层同步**：业务代码中同时更新多张表，简单但容易遗漏
- **触发器同步**：用数据库触发器自动维护冗余列，但增加数据库复杂度
- **异步消息**：写入主表后发消息，消费者异步更新冗余列，最终一致性

```sql
-- 触发器同步示例：下单后更新用户订单统计
CREATE TRIGGER update_user_stats AFTER INSERT ON orders
FOR EACH ROW
BEGIN
    UPDATE users
    SET order_count = order_count + 1,
        total_spent = total_spent + NEW.amount
    WHERE user_id = NEW.user_id;
END;
```

## ER 模型与表关系设计

### 实体关系模型（ER Model）

ER 模型用实体（Entity）、属性（Attribute）、关系（Relationship）描述业务数据。画 ER 图是数据库设计的第一步。

**实体**对应数据库中的表，**属性**对应列，**关系**用主键/外键表达。

### 一对一关系

适用场景：表拆分（按访问频率或安全级别分离列）、可选属性扩展。

```sql
-- 用户基本信息与详细资料一对一
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100)
);

CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY,
    real_name VARCHAR(50),
    id_card VARCHAR(18),
    address TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

`user_profiles` 的主键同时是外键，指向 `users`。这种设计将敏感信息（身份证号、地址）与主表隔离，便于权限控制。

### 一对多关系

最常见的关系类型，通过在"多"端添加外键实现。

```sql
-- 一个分类下有多篇文章
CREATE TABLE categories (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL
);

CREATE TABLE articles (
    article_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    category_id INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);
```

外键约束的级联策略：

- `CASCADE`：父表删除/更新时，子表联动删除/更新
- `SET NULL`：父表删除时，子表外键设为 NULL
- `RESTRICT`：子表有引用时，禁止父表删除/更新（默认行为）
- `NO ACTION`：与 `RESTRICT` 类似，但检查时机不同

### 多对多关系

通过中间表（关联表）实现。

```sql
-- 文章与标签多对多
CREATE TABLE tags (
    tag_id INT PRIMARY KEY AUTO_INCREMENT,
    tag_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE article_tags (
    article_id BIGINT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);
```

中间表的主键通常是两个外键的联合主键。如果中间表自身还有属性（如 `added_at`、`added_by`），可以在主键之外添加额外列。

## 主键选择策略

主键的选择影响存储效率、索引性能和分布式扩展能力。

### 自增主键（AUTO_INCREMENT）

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50)
);
```

**优点**：简单、有序（B+树写入友好）、占用空间小（8 字节 BIGINT）。

**缺点**：分布式环境下需要协调自增步长、可预测（安全风险）、数据迁移合并麻烦。

### UUID

```sql
CREATE TABLE users (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    username VARCHAR(50)
);
```

**优点**：全局唯一、无中心化依赖、不可预测。

**缺点**：36 字符占用空间大、无序导致 B+树频繁页分裂、不利于主键索引维护。

优化方案：使用 UUID 的二进制形式（16 字节）：

```sql
CREATE TABLE users (
    id BINARY(16) PRIMARY KEY DEFAULT (UUID_TO_BIN(UUID())),
    username VARCHAR(50)
);
```

或者使用有序 UUID（UUID v7），前 48 位为时间戳，保持插入有序性。

### 雪花算法（Snowflake）

雪花算法生成 64 位长整型 ID，结构如下：

```
0 | 41 位时间戳 | 10 位机器 ID | 12 位序列号
```

| 部分 | 位数 | 说明 |
|---|---|---|
| 符号位 | 1 | 固定为 0 |
| 时间戳 | 41 | 毫秒级，可用约 69 年 |
| 机器 ID | 10 | 支持 1024 个节点 |
| 序列号 | 12 | 每毫秒 4096 个 ID |

**优点**：趋势递增（对 B+树友好）、高性能（单机每毫秒 4096 个 ID）、不依赖数据库。

**缺点**：依赖系统时钟，时钟回拨会导致 ID 重复（需做兜底处理）。

实际应用中通常由应用层（如 Java 的 Hutool、百度的 uid-generator）生成雪花 ID，数据库用 `BIGINT` 存储。

## 索引设计原则

索引是数据库性能优化最重要的手段。设计索引时遵循以下原则：

### 选择性原则

选择性（Selectivity）= 不同值的数量 / 总行数。选择性越高的列越适合建索引。

```sql
-- 计算列的选择性
SELECT
    COUNT(DISTINCT status) / COUNT(*) AS status_selectivity,
    COUNT(DISTINCT email) / COUNT(*) AS email_selectivity
FROM users;
-- status 通常只有几个值（选择性低），email 几乎唯一（选择性高）
```

### 覆盖索引

索引包含查询所需的所有列，避免回表查询：

```sql
-- 联合索引覆盖查询
ALTER TABLE orders ADD INDEX idx_user_date_amount (user_id, order_date, amount);

-- 这个查询只需要索引，无需回表
SELECT order_date, amount FROM orders WHERE user_id = 1001;
```

### 前缀索引

对长字符串列使用前缀索引节省空间：

```sql
-- 取 email 前 10 个字符作为索引
ALTER TABLE users ADD INDEX idx_email_prefix (email(10));
```

前缀长度的选择：使前缀的选择性接近完整列的选择性。

```sql
SELECT
    COUNT(DISTINCT LEFT(email, 8)) / COUNT(*) AS sel_8,
    COUNT(DISTINCT LEFT(email, 10)) / COUNT(*) AS sel_10,
    COUNT(DISTINCT LEFT(email, 12)) / COUNT(*) AS sel_12,
    COUNT(DISTINCT email) / COUNT(*) AS sel_full
FROM users;
```

### 索引列顺序

联合索引中，将选择性高的列放在前面，或根据查询频率和排序需求调整。核心原则：让索引能被尽量多的查询利用。

## 分库分表策略

当单表数据量超过千万级，或单库写入压力过大时，需要考虑分库分表。

### 垂直分库

按业务模块拆分到不同数据库实例：

```
用户库（user_db）：users, user_profiles, user_settings
商品库（product_db）：products, categories, inventories
订单库（order_db）：orders, order_items, payments
```

垂直分库是微服务架构的自然选择，每个服务独占数据库。

### 垂直分表

将宽表按列拆分，将访问频率不同的列分离：

```sql
-- 文章主表（高频访问列）
CREATE TABLE articles (
    article_id BIGINT PRIMARY KEY,
    title VARCHAR(200),
    author_id BIGINT,
    status TINYINT,
    created_at DATETIME
);

-- 文章详情表（低频访问的大字段）
CREATE TABLE article_details (
    article_id BIGINT PRIMARY KEY,
    content LONGTEXT,
    content_html LONGTEXT
);
```

### 水平分表

按行拆分，将数据分布到多张结构相同的表中：

```
orders_0 (id % 4 == 0)
orders_1 (id % 4 == 1)
orders_2 (id % 4 == 2)
orders_3 (id % 4 == 3)
```

常见分片策略：

| 策略 | 说明 | 优点 | 缺点 |
|---|---|---|---|
| 取模分片 | id % N | 均匀分布 | 扩容困难 |
| 范围分片 | 按 ID 或时间区间 | 扩容简单 | 可能热点 |
| 一致性哈希 | 哈希环 | 扩容平滑 | 实现复杂 |

### 水平分库

将数据分散到多个数据库实例，既分库又分表：

```
db_0.orders_0, db_0.orders_1
db_1.orders_0, db_1.orders_1
```

### 分库分表的中间件

常用的分库分表中间件：

- **ShardingSphere**：Apache 顶级项目，支持 ShardingJDBC（客户端分片）和 ShardingProxy（代理分片）
- **MyCat**：基于 Cobar 的数据库中间件
- **Vitess**：YouTube 开源的 MySQL 集群方案，CNCF 项目

### 分库分表带来的问题

| 问题 | 说明 |
|---|---|
| 跨分片查询 | 需要聚合多个分片结果，性能差 |
| 分布式事务 | 跨库事务需要 XA 或 Seata 等方案 |
| 全局唯一 ID | 自增 ID 不再适用，需要雪花算法或独立 ID 服务 |
| 跨分片 JOIN | 无法直接 JOIN，需要冗余数据或应用层组装 |
| 扩容迁移 | 数据重新分片需要停机或双写方案 |

分库分表是最后的手段。在做分库分表之前，应先尝试：读写分离、缓存优化、SQL 优化、归档历史数据。当这些手段都用尽仍无法满足需求时，再考虑分库分表。
