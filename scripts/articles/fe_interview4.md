# 项目经验表达与系统设计面试

技术面试中，项目经验和系统设计是区分初级和高级工程师的关键环节。很多技术扎实的候选人因为不会表达项目经验、缺乏系统设计思路而错失机会。本文将分享一套实用的方法论，帮助你在面试中清晰地展现技术能力。

## STAR 法则：讲好你的项目故事

STAR 法则是描述项目经验的黄金框架：

- **S（Situation）**：项目背景和挑战
- **T（Task）**：你的具体职责
- **A（Action）**：你采取的技术方案和行动
- **R（Result）**：取得的成果（最好量化）

### 反面示例

> 我做了一个电商系统，用了 Spring Boot 和 MySQL，实现了商品管理、订单、支付等功能。

这种描述过于笼统，面试官无法判断你的技术深度。

### 正面示例

> **Situation**：我负责公司的订单系统重构。原有系统是单体架构，日订单量 5 万时经常出现超时和数据不一致问题。
>
> **Task**：我作为核心开发，负责订单模块的架构设计和核心代码实现。
>
> **Action**：
> - 将订单系统拆分为独立服务，使用 RocketMQ 实现订单创建和库存扣减的最终一致性
> - 引入 Redis 缓存热点商品数据，使用 Lua 脚本保证库存扣减的原子性
> - 设计了订单号生成算法：时间戳 + 机器 ID + 序列号，支持分布式环境下的全局唯一
>
> **Result**：系统支撑日订单量从 5 万提升到 50 万，订单创建 P99 延迟从 2 秒降到 200 毫秒，上线半年零故障。

### 准备项目的 STAR 模板

建议在面试前，针对你做过的每个项目，准备一份 STAR 模板：

```markdown
## 项目名称：XX 电商订单系统

### Situation（背景）
- 业务背景：公司从 ToB 转型 ToC，订单量快速增长
- 技术挑战：单体架构无法支撑高并发，数据库瓶颈明显
- 时间压力：3 个月内完成重构上线

### Task（职责）
- 负责订单模块的架构设计
- 带领 3 人小组完成开发
- 制定技术方案和评审

### Action（行动）
1. 技术选型：
   - 消息队列选择 RocketMQ（团队有经验，社区活跃）
   - 缓存选择 Redis（支持丰富数据结构）
   - 数据库分库分表使用 ShardingSphere

2. 核心设计：
   - 订单号生成：雪花算法变体
   - 库存扣减：Redis + Lua + 异步落库
   - 订单状态机：状态流转 + 事件驱动

3. 难点攻克：
   - 分布式事务：最终一致性方案
   - 订单超时取消：延迟队列 + 定时补偿

### Result（成果）
- 性能：P99 延迟 200ms，QPS 5000+
- 稳定性：上线半年零故障
- 业务：支撑日订单量 50 万
```

## 技术选型理由阐述

面试官经常会问"为什么选择这个技术"。回答这个问题需要从多个维度进行对比分析。

### 技术选型的思考框架

```
1. 业务需求分析
   → 并发量、数据量、一致性要求

2. 技术方案对比
   → 性能、可靠性、复杂度、学习成本

3. 团队实际情况
   → 现有技术栈、团队规模、时间约束

4. 社区和生态
   → 文档质量、社区活跃度、长期维护
```

### 示例：消息队列选型

| 维度 | RabbitMQ | RocketMQ | Kafka |
|------|----------|----------|-------|
| 吞吐量 | 万级 | 十万级 | 百万级 |
| 延迟 | 微秒级 | 毫秒级 | 毫秒级 |
| 可用性 | 高（镜像队列） | 非常高（主从） | 非常高（副本） |
| 消息可靠性 | 高 | 非常高 | 高 |
| 功能特性 | 完善（死信、延迟） | 完善（事务、延迟） | 基础（需扩展） |
| 学习曲线 | 中等 | 中等 | 较陡 |
| 适用场景 | 业务消息 | 电商/金融 | 大数据/日志 |

**面试回答示例**：

> 我们选择 RocketMQ 主要考虑三个因素：一是团队之前用过，学习成本低；二是 RocketMQ 支持事务消息，我们的订单场景需要保证消息和本地事务的一致性；三是 RocketMQ 的延迟消息功能可以直接用于订单超时取消，不需要额外引入延时队列。Kafka 虽然吞吐量更高，但我们的业务消息量没有那么大，而且 Kafka 的消息可靠性保障需要更多配置。

### 示例：数据库选型

```java
// 面试时可以这样阐述 MySQL vs PostgreSQL 的选型

/*
选择 MySQL 的理由：
1. 团队熟悉度：团队成员都有 MySQL 经验
2. 生态成熟：MyBatis-Plus、ShardingSphere 等中间件支持好
3. 运维成本：公司 DBA 团队只支持 MySQL
4. 业务匹配：电商场景的 OLTP 需求，MySQL 完全胜任

选择 PostgreSQL 的场景：
1. 需要复杂的 JSON 查询
2. 地理位置相关业务（PostGIS）
3. 需要更严格的 SQL 标准支持
*/
```

## 系统设计面试框架

系统设计面试考察的是你的架构思维和全局视野。以下是一个经过验证的系统设计框架：

### 六步法

```
Step 1: 需求澄清（Requirements Clarification）
   → 功能需求、非功能需求、约束条件

Step 2: 容量估算（Capacity Estimation）
   → QPS、存储量、带宽、服务器数量

Step 3: 接口设计（API Design）
   → 核心接口定义、请求/响应格式

Step 4: 数据模型（Data Model）
   → 表结构、索引设计、分库分表策略

Step 5: 核心组件（Core Components）
   → 架构图、核心流程、关键算法

Step 6: 扩展讨论（Scalability）
   → 高并发、高可用、监控告警
```

### 案例：设计一个短链接服务

**Step 1: 需求澄清**

```
功能需求：
- 用户输入长链接，生成短链接
- 用户访问短链接，重定向到长链接
- 短链接有过期时间

非功能需求：
- 读多写少（读写比 100:1）
- 高可用（99.99%）
- 低延迟（重定向 < 100ms）
- 短链接不可预测
```

**Step 2: 容量估算**

```
假设：
- 日活用户 1000 万
- 每人每天创建 0.1 个短链接 → 写 QPS = 1000万 * 0.1 / 86400 ≈ 120
- 每人每天点击 1 次短链接 → 读 QPS = 1000万 / 86400 ≈ 120
- 短链接有效期 5 年
- 每条记录 500 字节

存储估算：
- 5 年总量 = 120 * 86400 * 365 * 5 ≈ 190 亿条
- 存储空间 = 190亿 * 500B ≈ 9.5TB

短链接长度：
- 使用 Base62（a-z, A-Z, 0-9）
- 6 位 Base62 = 62^6 ≈ 568 亿，足够使用
```

**Step 3: 接口设计**

```java
// 创建短链接
POST /api/v1/shorten
Request: {
    "longUrl": "https://example.com/very/long/path",
    "expireAt": "2025-12-31T23:59:59Z"
}
Response: {
    "shortUrl": "https://short.link/abc123",
    "shortCode": "abc123",
    "expireAt": "2025-12-31T23:59:59Z"
}

// 重定向（这个接口的 QPS 最高）
GET /{shortCode}
Response: 302 Redirect → longUrl
```

**Step 4: 数据模型**

```sql
-- 短链接表
CREATE TABLE short_url (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10) UNIQUE NOT NULL,
    long_url VARCHAR(2048) NOT NULL,
    user_id BIGINT,
    expire_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_short_code (short_code),
    INDEX idx_expire_at (expire_at)
);

-- 分库分表策略
-- 按 short_code 的 hash 值分片
-- 分 16 个库，每个库 16 张表，共 256 张表
```

**Step 5: 核心组件**

```
用户请求 → 负载均衡 → 应用服务器 → 缓存（Redis）
                                    ↓
                              数据库（MySQL）
```

短链接生成算法：

```java
@Component
public class ShortCodeGenerator {
    
    private static final String BASE62 = 
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    
    /**
     * 方案1：哈希 + 截取
     * 用 MurmurHash 对长链接取哈希，截取前 6 位
     * 问题：冲突概率较高
     */
    public String generateByHash(String longUrl) {
        int hash = MurmurHash.hash32(longUrl.getBytes());
        hash = Math.abs(hash);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(BASE62.charAt(hash % 62));
            hash /= 62;
        }
        return sb.toString();
    }
    
    /**
     * 方案2：自增 ID + Base62（推荐）
     * 用分布式 ID 生成器获取唯一 ID，转为 Base62
     * 优点：无冲突、可预测长度
     */
    public String generateById(long id) {
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(BASE62.charAt((int)(id % 62)));
            id /= 62;
        }
        // 补齐到 6 位
        while (sb.length() < 6) {
            sb.append('0');
        }
        return sb.reverse().toString();
    }
}
```

**Step 6: 扩展讨论**

```
高并发优化：
- Redis 缓存热点短链接，命中率可达 95%+
- 读写分离，主库写，从库读
- CDN 缓存静态资源

高可用保障：
- 应用服务器无状态，水平扩展
- MySQL 主从 + 自动故障转移
- Redis 哨兵模式
- 限流降级：超过阈值返回 429

监控告警：
- QPS、延迟、错误率监控
- 缓存命中率监控
- 数据库慢查询告警
```

## 高并发/高可用方案表达

面试中经常被问到"你的系统是怎么扛住高并发的"。以下是一些常见的高并发方案：

### 缓存策略

```java
// 多级缓存架构
public class MultiLevelCache {
    
    @Autowired
    private LocalCache localCache;  // Caffeine 本地缓存
    
    @Autowired
    private RedisTemplate redis;    // Redis 分布式缓存
    
    @Autowired
    private UserMapper userMapper;  // 数据库
    
    public User getUser(Long id) {
        String key = "user:" + id;
        
        // L1: 本地缓存
        User user = localCache.get(key);
        if (user != null) {
            return user;
        }
        
        // L2: Redis 缓存
        user = (User) redis.opsForValue().get(key);
        if (user != null) {
            localCache.put(key, user, 60, TimeUnit.SECONDS);
            return user;
        }
        
        // L3: 数据库
        user = userMapper.selectById(id);
        if (user != null) {
            redis.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
            localCache.put(key, user, 60, TimeUnit.SECONDS);
        }
        return user;
    }
}
```

### 限流方案

```java
// 令牌桶限流
@Component
public class RateLimiter {
    
    @Autowired
    private StringRedisTemplate redis;
    
    /**
     * 令牌桶算法实现
     * @param key 限流 key
     * @param rate 每秒生成的令牌数
     * @param capacity 桶容量
     * @return 是否允许通过
     */
    public boolean tryAcquire(String key, int rate, int capacity) {
        String luaScript = 
            "local key = KEYS[1] " +
            "local rate = tonumber(ARGV[1]) " +
            "local capacity = tonumber(ARGV[2]) " +
            "local now = tonumber(ARGV[3]) " +
            "local requested = tonumber(ARGV[4]) " +
            
            "local fill_time = capacity / rate " +
            "local ttl = math.floor(fill_time * 2) " +
            
            "local last_tokens = tonumber(redis.call('hget', key, 'tokens')) " +
            "if last_tokens == nil then " +
            "  last_tokens = capacity " +
            "end " +
            
            "local last_refreshed = tonumber(redis.call('hget', key, 'last_refreshed')) " +
            "if last_refreshed == nil then " +
            "  last_refreshed = 0 " +
            "end " +
            
            "local delta = math.max(0, now - last_refreshed) " +
            "local filled_tokens = math.min(capacity, last_tokens + (delta * rate)) " +
            "local allowed = filled_tokens >= requested " +
            "local new_tokens = filled_tokens " +
            "if allowed then " +
            "  new_tokens = filled_tokens - requested " +
            "end " +
            
            "redis.call('hset', key, 'tokens', new_tokens) " +
            "redis.call('hset', key, 'last_refreshed', now) " +
            "redis.call('expire', key, ttl) " +
            
            "return allowed";
        
        Long result = redis.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(key),
            String.valueOf(rate),
            String.valueOf(capacity),
            String.valueOf(System.currentTimeMillis() / 1000),
            "1"
        );
        return Long.valueOf(1L).equals(result);
    }
}
```

### 降级策略

```java
// 服务降级示例
@Service
public class ProductService {
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private RedisTemplate redis;
    
    @HystrixCommand(fallbackMethod = "getProductFallback")
    public Product getProduct(Long id) {
        // 正常逻辑：从数据库获取最新数据
        return productMapper.selectById(id);
    }
    
    /**
     * 降级方案：返回缓存数据（可能是旧数据）
     */
    public Product getProductFallback(Long id) {
        String key = "product:backup:" + id;
        return (Product) redis.opsForValue().get(key);
    }
}
```

## 常见追问应对

面试官经常会针对你的回答进行追问，以下是一些常见追问及应对策略：

### 追问1：为什么不用 XX 方案？

**应对策略**：承认其他方案的优点，说明你的场景为什么选择当前方案。

> 问：为什么用 Redis 而不用 Memcached？
>
> 答：Memcached 的性能确实很好，但我们需要 Redis 的几个特性：一是支持丰富的数据结构，我们的排行榜用到了 Sorted Set；二是 Redis 支持持久化，虽然缓存丢了可以从数据库重建，但能减少一些恢复时间；三是 Redis 的集群方案更成熟，我们的运维团队有 Redis 集群的运维经验。

### 追问2：这个方案有什么缺点？

**应对策略**：展示你的全面思考能力，不要只说优点。

> 问：你用的最终一致性方案有什么缺点？
>
> 答：最终一致性确实有几个问题：一是消息可能丢失，所以我们加了定时补偿任务；二是存在中间状态，用户可能看到"已支付但未发货"的短暂状态，前端做了状态提示；三是排查问题比较复杂，需要链路追踪。但这些缺点相比强一致性的性能损耗，是我们可以接受的。

### 追问3：如果数据量增长 10 倍怎么办？

**应对策略**：展示你的架构扩展性思考。

> 问：如果日订单量从 50 万增长到 500 万，你的方案还能撑住吗？
>
> 答：目前的架构设计已经考虑了扩展性。如果增长 10 倍，我会做以下调整：
> 1. 数据库分库分表从 4 库扩展到 16 库
> 2. Redis 从单机扩展到集群模式
> 3. 应用服务器从 4 台扩展到 20 台
> 4. 引入本地缓存（Caffeine）减少 Redis 压力
>
> 这些都是水平扩展，不需要修改核心代码，主要是配置和运维层面的调整。

### 追问4：线上出过什么问题？

**应对策略**：展示你的问题解决能力和经验积累。

> 问：你这个系统上线后遇到过什么线上问题？
>
> 答：遇到过一个缓存和数据库不一致的问题。场景是：用户修改了收货地址，但订单页面还是显示旧地址。排查发现是缓存更新的时序问题——我们先更新数据库再删缓存，但在删除缓存之前，另一个请求读到了旧数据并写入了缓存。解决方案是采用"先删缓存 + 延迟双删"，在删除缓存后延迟 500 毫秒再删一次，降低不一致的概率。

## 面试前的准备清单

```
□ 项目 STAR 模板（每个项目准备 5 分钟的讲述）
□ 技术选型对比表（为什么选 A 不选 B）
□ 系统架构图（能快速画出核心架构）
□ 核心数据模型（表结构、索引设计）
□ 性能优化案例（优化前后对比数据）
□ 线上问题案例（问题描述、排查过程、解决方案）
□ 系统设计练习（短链接、Feed 流、秒杀系统等）
```

记住，面试不是背答案，而是展示你的思考过程。面试官更关心的是你"怎么想"，而不仅仅是"怎么做"。保持逻辑清晰、表达简洁，即使遇到不会的问题，也能给面试官留下好印象。
