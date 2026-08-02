# MongoDB 聚合管道与索引优化

MongoDB 的聚合管道（Aggregation Pipeline）是数据处理的核心能力，能够实现复杂的数据转换、统计和分析。索引则是查询性能的基石。本文系统讲解聚合管道的各个阶段、索引类型及优化技巧，以及分片集群的架构设计。

## 聚合管道基础

聚合管道由多个阶段（Stage）串联而成，文档依次流过每个阶段，每个阶段对文档进行特定的处理。

```
db.collection.aggregate([
    { $阶段1: { ... } },
    { $阶段2: { ... } },
    { $阶段3: { ... } }
])
```

### $match — 过滤

`$match` 相当于 SQL 的 `WHERE`，尽量放在管道最前面以减少后续阶段处理的数据量。

```javascript
// 查询状态为已发布的文章
db.articles.aggregate([
    { $match: { status: "published", created_at: { $gte: ISODate("2025-01-01") } } }
])

// $match 可以使用普通的查询操作符
db.orders.aggregate([
    { $match: {
        amount: { $gt: 100 },
        status: { $in: ["paid", "shipped"] }
    }}
])
```

**优化建议**：`$match` 放在管道最前面，利用索引过滤数据。

### $group — 分组聚合

`$group` 相当于 SQL 的 `GROUP BY`，支持多种累加器操作符。

```javascript
// 按类别统计文章数量和平均阅读量
db.articles.aggregate([
    { $match: { status: "published" } },
    { $group: {
        _id: "$category",
        count: { $sum: 1 },
        avgViews: { $avg: "$views" },
        maxViews: { $max: "$views" },
        minViews: { $min: "$views" },
        totalViews: { $sum: "$views" },
        titles: { $push: "$title" }  // 收集所有标题到数组
    }}
])
```

常用累加器：

| 累加器 | 说明 |
|---|---|
| `$sum` | 求和 |
| `$avg` | 平均值 |
| `$max` / `$min` | 最大/最小值 |
| `$push` | 将值添加到数组 |
| `$addToSet` | 将值添加到数组（去重） |
| `$first` / `$last` | 第一个/最后一个值 |
| `$stdDevPop` / `$stdDevSamp` | 标准差 |

多字段分组：

```javascript
// 按年月统计订单
db.orders.aggregate([
    { $group: {
        _id: {
            year: { $year: "$created_at" },
            month: { $month: "$created_at" }
        },
        totalAmount: { $sum: "$amount" },
        orderCount: { $sum: 1 }
    }},
    { $sort: { "_id.year": -1, "_id.month": -1 } }
])
```

### $project — 字段投影

`$project` 相当于 SQL 的 `SELECT`，控制输出的字段，支持字段计算和重命名。

```javascript
db.articles.aggregate([
    { $project: {
        title: 1,                           // 包含
        author: 1,
        category: 1,
        views: 1,
        score: { $multiply: ["$views", 2] }, // 计算字段
        year: { $year: "$created_at" },       // 提取年份
        _id: 0                                // 排除 _id
    }}
])
```

条件投影：

```javascript
db.users.aggregate([
    { $project: {
        name: 1,
        level: {
            $switch: {
                branches: [
                    { case: { $gte: ["$score", 1000] }, then: "VIP" },
                    { case: { $gte: ["$score", 100] }, then: "高级" },
                    { case: { $gte: ["$score", 0] }, then: "普通" }
                ],
                default: "未知"
            }
        }
    }}
])
```

### $lookup — 关联查询

`$lookup` 相当于 SQL 的 `LEFT JOIN`。

```javascript
// 关联查询文章和作者信息
db.articles.aggregate([
    { $lookup: {
        from: "users",              // 关联的集合
        localField: "author_id",    // 本集合的关联字段
        foreignField: "_id",        // 目标集合的关联字段
        as: "author"                // 输出字段名
    }},
    { $unwind: "$author" }          // 展开数组
])

// 更复杂的 lookup：带条件和投影
db.orders.aggregate([
    { $lookup: {
        from: "products",
        let: { productId: "$product_id" },
        pipeline: [
            { $match: { $expr: { $eq: ["$_id", "$$productId"] } } },
            { $project: { name: 1, price: 1 } }
        ],
        as: "product"
    }}
])
```

### $unwind — 数组展开

`$unwind` 将数组字段拆分为多条文档，每条文档包含数组中的一个元素。

```javascript
// 展开标签数组
db.articles.aggregate([
    { $unwind: "$tags" },
    { $group: {
        _id: "$tags",
        count: { $sum: 1 }
    }},
    { $sort: { count: -1 } },
    { $limit: 10 }
])

// preserveNullAndEmptyArrays: 保留数组为空或 null 的文档
db.articles.aggregate([
    { $unwind: { path: "$tags", preserveNullAndEmptyArrays: true } }
])
```

### $sort 和 $limit

```javascript
// 按阅读量排序取 Top 10
db.articles.aggregate([
    { $match: { status: "published" } },
    { $sort: { views: -1 } },
    { $limit: 10 },
    { $project: { title: 1, views: 1, author: 1 } }
])
```

**优化建议**：如果前面有 `$match` 且走了索引，`$sort` 可能不需要额外排序操作。`$limit` 应尽早执行以减少数据量。

### $facet — 多管道并行

`$facet` 允许在同一个查询中执行多个聚合管道，返回多个结果集。

```javascript
db.articles.aggregate([
    { $match: { status: "published" } },
    { $facet: {
        // 按分类统计
        byCategory: [
            { $group: { _id: "$category", count: { $sum: 1 } } },
            { $sort: { count: -1 } }
        ],
        // 按月统计
        byMonth: [
            { $group: {
                _id: { $dateToString: { format: "%Y-%m", date: "$created_at" } },
                count: { $sum: 1 }
            }},
            { $sort: { _id: -1 } }
        ],
        // Top 10 文章
        topArticles: [
            { $sort: { views: -1 } },
            { $limit: 10 },
            { $project: { title: 1, views: 1 } }
        ]
    }}
])
```

### 其他常用阶段

| 阶段 | 说明 | SQL 等价 |
|---|---|---|
| `$limit` | 限制返回数量 | `LIMIT` |
| `$skip` | 跳过前 N 条 | `OFFSET` |
| `$sort` | 排序 | `ORDER BY` |
| `$addFields` | 添加新字段 | `SELECT ... AS` |
| `$replaceRoot` | 替换文档根 | - |
| `$out` | 输出到集合 | `CREATE TABLE AS` |
| `$merge` | 合并到集合 | `MERGE INTO` |
| `$count` | 计数 | `COUNT(*)` |
| `$set` | 设置字段值 | `UPDATE SET` |
| `$unset` | 删除字段 | - |

## 索引类型

### 单字段索引

```javascript
// 升序索引
db.articles.createIndex({ created_at: -1 })

// 对嵌套文档的字段建索引
db.users.createIndex({ "address.city": 1 })
```

### 复合索引

```javascript
// 复合索引
db.articles.createIndex({ status: 1, created_at: -1 })

// 等值查询 + 排序的复合索引
db.orders.createIndex({ user_id: 1, created_at: -1 })
```

复合索引遵循最左前缀原则，与 MySQL 一致：

```javascript
// 以下查询可以使用索引 { status: 1, created_at: -1 }
db.articles.find({ status: "published" })
db.articles.find({ status: "published" }).sort({ created_at: -1 })
db.articles.find({ status: "published", created_at: { $gte: ISODate("2025-01-01") } })

// 以下查询无法使用该索引
db.articles.find({ created_at: { $gte: ISODate("2025-01-01") } })  // 跳过最左字段
```

### 多键索引

对数组字段自动创建多键索引：

```javascript
// tags 是数组字段
db.articles.createIndex({ tags: 1 })
// 查询包含特定标签的文章
db.articles.find({ tags: "mongodb" })
```

### 文本索引

```javascript
// 创建文本索引
db.articles.createIndex({ title: "text", content: "text" })

// 全文搜索
db.articles.find({ $text: { $search: "mongodb 索引优化" } })

// 按相关度排序
db.articles.find(
    { $text: { $search: "mongodb 优化" } },
    { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } })
```

文本索引的限制：
- 每个集合只能有一个文本索引
- 不支持前缀搜索
- 中文分词效果一般（需要额外配置）

### 地理空间索引

```javascript
// 创建 2dsphere 索引
db.places.createIndex({ location: "2dsphere" })

// 附近搜索
db.places.find({
    location: {
        $nearSphere: {
            $geometry: { type: "Point", coordinates: [116.4074, 39.9042] },
            $maxDistance: 5000  // 5 公里
        }
    }
})

// 范围查询
db.places.find({
    location: {
        $geoWithin: {
            $geometry: {
                type: "Polygon",
                coordinates: [[[116.3, 39.8], [116.5, 39.8], [116.5, 40.0], [116.3, 40.0], [116.3, 39.8]]]
            }
        }
    }
})
```

### 哈希索引

```javascript
// 哈希索引（只支持等值查询，用于分片）
db.users.createIndex({ user_id: "hashed" })
```

哈希索引主要用于分片集群的哈希分片策略。

## explain 分析

```javascript
// 查看查询计划
db.articles.find({ status: "published" }).explain("executionStats")
```

关键输出字段：

```javascript
{
    "executionStats": {
        "nReturned": 156,                    // 返回文档数
        "executionTimeMillis": 12,           // 执行耗时
        "totalKeysExamined": 156,            // 扫描的索引键数
        "totalDocsExamined": 156             // 扫描的文档数
    },
    "winningPlan": {
        "stage": "IXSCAN",                   // 索引扫描
        "indexName": "status_1_created_at_-1",
        "direction": "forward"
    }
}
```

### 常见 stage

| stage | 说明 |
|---|---|
| COLLSCAN | 全集合扫描（相当于全表扫描） |
| IXSCAN | 索引扫描 |
| FETCH | 从磁盘获取完整文档 |
| SORT | 内存排序（应尽量避免） |
| SORT_KEY_GENERATOR | 排序键生成 |
| SHARD_MERGE | 分片结果合并 |

### 优化指标

- `totalDocsExamined` 应接近 `nReturned`
- `totalKeysExamined` 应接近 `nReturned`
- 避免 `stage: "SORT"`（应通过索引排序）
- `executionTimeMillis` 应在可接受范围内

```javascript
// 查看集合所有索引
db.articles.getIndexes()

// 查看索引使用统计
db.articles.aggregate([{ $indexStats: {} }])

// 删除未使用的索引
db.articles.dropIndex("unused_index_name")
```

## 分片集群架构

### 架构组件

```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │
┌──────▼───────┐
│  mongos      │  ← 路由（可多个）
│  (Router)    │
└──────┬───────┘
       │
┌──────▼───────────────────────────────────────┐
│              Config Servers                   │
│         (存储元数据和分片映射)                  │
└──────┬───────────────────────────────────────┘
       │
┌──────▼───────┐  ┌─────────────┐  ┌──────────┐
│  Shard 1     │  │  Shard 2    │  │ Shard 3  │
│  (Replica Set)│  │(Replica Set)│  │(Replica) │
│  P-S-S       │  │  P-S-S      │  │ P-S-S    │
└──────────────┘  └─────────────┘  └──────────┘
```

### 分片策略

**范围分片（Range Sharding）**：

```javascript
// 按 created_at 范围分片
sh.shardCollection("mydb.orders", { created_at: 1 })
```

适合范围查询多的场景，但可能导致数据分布不均（新数据集中在一个分片）。

**哈希分片（Hashed Sharding）**：

```javascript
// 按 user_id 哈希分片
sh.shardCollection("mydb.orders", { user_id: "hashed" })
```

数据分布均匀，但范围查询需要扫描所有分片。

**Zone Sharding（区域分片）**：

```javascript
// 将中国用户数据分到亚洲分片
sh.addShardToZone("shard0001", "Asia")
sh.updateZoneKeyRange("mydb.users", { country: "CN" }, { country: "CN\uFFFF" }, "Asia")
```

### 分片键选择

分片键的选择直接影响集群性能：

| 原则 | 说明 |
|---|---|
| 高基数 | 分片键的值要足够多（如 user_id 优于 status） |
| 写分布均匀 | 避免所有写入集中在一个分片 |
| 查询局部化 | 大多数查询包含分片键，避免 scatter-gather |
| 不可变 | 分片键的值不可修改 |

推荐的分片键模式：

```javascript
// 组合分片键：高基数 + 查询常用字段
sh.shardCollection("mydb.logs", { user_id: 1, created_at: 1 })

// 哈希分片键：均匀分布
sh.shardCollection("mydb.sessions", { session_id: "hashed" })
```

### 分片管理

```javascript
// 查看分片状态
sh.status()

// 添加分片
sh.addShard("rs1/shard1:27017,shard2:27017,shard3:27017")

// 移除分片（需要先迁移数据）
sh.removeShard("shard0001")

// 手动分裂块
sh.splitAt("mydb.orders", { user_id: 10000 })

// 手动迁移块
sh.moveChunk("mydb.orders", { user_id: 10000 }, "shard0002")
```

## 聚合管道优化技巧

### 优化原则

1. **尽早过滤**：`$match` 和 `$limit` 放在管道前面
2. **利用索引**：确保 `$match` 和 `$sort` 阶段使用索引
3. **减少文档大小**：`$project` 尽早排除不需要的字段
4. **避免内存排序**：通过索引支持 `$sort`
5. **allowDiskUse**：大数据量时允许使用磁盘

```javascript
db.articles.aggregate(
    [
        { $match: { status: "published" } },   // 先过滤
        { $project: { title: 1, views: 1 } },  // 减小文档
        { $sort: { views: -1 } },
        { $limit: 100 }
    ],
    { allowDiskUse: true }  // 允许磁盘排序
)
```

### 索引支持聚合

```javascript
// 确保 $match 和 $sort 使用索引
db.articles.createIndex({ status: 1, views: -1 })

// 此管道可以完全使用索引
db.articles.aggregate([
    { $match: { status: "published" } },
    { $sort: { views: -1 } },
    { $limit: 10 }
])
```

### $merge 优化大批量写入

```javascript
// 将聚合结果写入新集合（增量更新）
db.orders.aggregate([
    { $match: { created_at: { $gte: ISODate("2025-06-01") } } },
    { $group: {
        _id: { user_id: "$user_id", date: { $dateToString: { format: "%Y-%m-%d", date: "$created_at" } } },
        dailyTotal: { $sum: "$amount" },
        orderCount: { $sum: 1 }
    }},
    { $merge: {
        into: "user_daily_stats",
        on: "_id",
        whenMatched: "replace",
        whenNotMatched: "insert"
    }}
])
```

`$merge` 比 `$out` 更灵活，支持增量更新而非全量替换。
