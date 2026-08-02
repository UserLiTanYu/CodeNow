# Redis 缓存策略与分布式应用

Redis 最常见的用途是作为缓存层，减轻数据库压力。但缓存引入后，如何处理缓存穿透、击穿、雪崩等问题？除了缓存，Redis 还广泛应用于分布式锁、延迟队列、限流等场景。本文逐一讲解这些经典问题和解决方案。

## 缓存穿透

缓存穿透是指查询一个**不存在的数据**，缓存和数据库都没有命中，每次请求都打到数据库。

### 问题分析

```
请求: GET /api/user/999999
→ 缓存未命中
→ 数据库查询: SELECT * FROM users WHERE id = 999999 → NULL
→ 缓存未写入（null 不缓存）
→ 下次请求仍然穿透
```

攻击者可以利用不存在的 ID 发起大量请求，直接打垮数据库。

### 解决方案一：缓存空值

```java
public User getUser(Long id) {
    String cacheKey = "user:" + id;
    String cached = redis.get(cacheKey);
    
    if (cached != null) {
        if ("NULL".equals(cached)) {
            return null;  // 命中空值缓存
        }
        return JSON.parseObject(cached, User.class);
    }
    
    User user = userMapper.selectById(id);
    if (user != null) {
        redis.setex(cacheKey, 3600, JSON.toJSONString(user));
    } else {
        redis.setex(cacheKey, 300, "NULL");  // 缓存空值，较短过期时间
    }
    return user;
}
```

**优点**：简单有效。
**缺点**：如果攻击者用大量不同的不存在 ID，会占用大量内存。

### 解决方案二：布隆过滤器

布隆过滤器（Bloom Filter）是一种概率性数据结构，用于判断元素是否**可能存在**或**一定不存在**。

```
请求流程:
→ 布隆过滤器判断 ID 是否存在
→ 不存在 → 直接返回（拦截穿透）
→ 可能存在 → 查询缓存/数据库
```

Redis 中使用布隆过滤器（RedisBloom 模块）：

```bash
# 创建布隆过滤器（误判率 0.01%，预计 100 万个元素）
BF.RESERVE user_filter 0.0001 1000000

# 添加元素
BF.ADD user_filter 1001
BF.ADD user_filter 1002

# 批量添加
BF.MADD user_filter 1003 1004 1005

# 检查元素
BF.EXISTS user_filter 1001   # 1（可能存在）
BF.EXISTS user_filter 99999  # 0（一定不存在）
```

Java 中使用 Redisson 的布隆过滤器：

```java
RBloomFilter<Long> bloomFilter = redisson.getBloomFilter("user_filter");
bloomFilter.tryInit(1000000L, 0.0001);

// 初始化时将所有存在的 ID 加入
List<Long> allIds = userMapper.selectAllIds();
allIds.forEach(id -> bloomFilter.add(id));

// 查询时先过滤
public User getUser(Long id) {
    if (!bloomFilter.contains(id)) {
        return null;  // 一定不存在
    }
    // 可能存在，查缓存/数据库
    return getFromCacheOrDB(id);
}
```

布隆过滤器的误判率与空间占用的权衡：

| 预期元素数 | 误判率 | 位数组大小 | 哈希函数数 |
|---|---|---|---|
| 1,000,000 | 1% | 1.2 MB | 7 |
| 1,000,000 | 0.1% | 1.8 MB | 10 |
| 1,000,000 | 0.01% | 2.4 MB | 14 |

### 解决方案三：参数校验

对明显非法的请求参数在接口层直接拦截：

```java
@GetMapping("/api/user/{id}")
public User getUser(@PathVariable Long id) {
    if (id == null || id <= 0) {
        throw new IllegalArgumentException("Invalid user ID");
    }
    return userService.getUser(id);
}
```

## 缓存击穿

缓存击穿是指某个**热点 key 过期**的瞬间，大量并发请求同时打到数据库。

### 问题分析

```
时间线:
T1: 热点 key "hot_article:1001" 过期
T2: 请求 A 查缓存未命中，查数据库
T3: 请求 B 查缓存未命中，查数据库
T4: 请求 C 查缓存未命中，查数据库
...
→ 数据库瞬间收到大量查询请求
```

### 解决方案一：互斥锁

只允许一个请求去数据库加载数据，其他请求等待：

```java
public Article getArticle(Long id) {
    String cacheKey = "article:" + id;
    String cached = redis.get(cacheKey);
    
    if (cached != null) {
        return JSON.parseObject(cached, Article.class);
    }
    
    // 尝试获取分布式锁
    String lockKey = "lock:" + cacheKey;
    boolean locked = redis.set(lockKey, "1", "NX", "EX", 10);
    
    if (locked) {
        try {
            // 双重检查
            cached = redis.get(cacheKey);
            if (cached != null) {
                return JSON.parseObject(cached, Article.class);
            }
            
            // 查询数据库
            Article article = articleMapper.selectById(id);
            redis.setex(cacheKey, 3600, JSON.toJSONString(article));
            return article;
        } finally {
            redis.del(lockKey);
        }
    } else {
        // 未获取到锁，短暂等待后重试
        Thread.sleep(50);
        return getArticle(id);  // 递归重试
    }
}
```

### 解决方案二：逻辑过期

不设置实际过期时间，在 value 中存储逻辑过期时间：

```java
public void saveArticle(Article article) {
    CacheData cacheData = new CacheData();
    cacheData.setData(article);
    cacheData.setExpireTime(LocalDateTime.now().plusHours(1));  // 逻辑过期时间
    redis.set("article:" + article.getId(), JSON.toJSONString(cacheData));
    // 不设置 TTL
}

public Article getArticle(Long id) {
    String cached = redis.get("article:" + id);
    CacheData cacheData = JSON.parseObject(cached, CacheData.class);
    
    if (cacheData.getExpireTime().isAfter(LocalDateTime.now())) {
        return cacheData.getData();  // 未过期，直接返回
    }
    
    // 已过期，异步刷新
    String lockKey = "lock:article:" + id;
    if (redis.set(lockKey, "1", "NX", "EX", 10)) {
        // 异步线程去数据库加载新数据
        CompletableFuture.runAsync(() -> {
            Article fresh = articleMapper.selectById(id);
            CacheData newCacheData = new CacheData();
            newCacheData.setData(fresh);
            newCacheData.setExpireTime(LocalDateTime.now().plusHours(1));
            redis.set("article:" + id, JSON.toJSONString(newCacheData));
            redis.del(lockKey);
        });
    }
    
    return cacheData.getData();  // 返回旧数据
}
```

## 缓存雪崩

缓存雪崩是指**大量 key 同时过期**或 **Redis 宕机**，导致请求全部打到数据库。

### 问题场景

1. 批量导入数据时设置了相同的过期时间
2. Redis 实例宕机
3. 缓存层整体不可用

### 解决方案一：随机过期时间

```java
// 不推荐：所有 key 相同过期时间
redis.setex(key, 3600, value);

// 推荐：加随机偏移
int baseTTL = 3600;
int randomOffset = ThreadLocalRandom.current().nextInt(600);  // 0-600 秒随机
redis.setex(key, baseTTL + randomOffset, value);
```

### 解决方案二：多级缓存

```
请求 → L1 本地缓存（Caffeine/Guava）→ L2 分布式缓存（Redis）→ 数据库
```

```java
@Service
public class UserService {
    
    private final Cache<Long, User> localCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    public User getUser(Long id) {
        // L1: 本地缓存
        User user = localCache.getIfPresent(id);
        if (user != null) {
            return user;
        }
        
        // L2: Redis
        String cached = redis.get("user:" + id);
        if (cached != null) {
            user = JSON.parseObject(cached, User.class);
            localCache.put(id, user);
            return user;
        }
        
        // 数据库
        user = userMapper.selectById(id);
        if (user != null) {
            redis.setex("user:" + id, 3600, JSON.toJSONString(user));
            localCache.put(id, user);
        }
        return user;
    }
}
```

### 解决方案三：熔断降级

当数据库压力过大时，触发熔断，返回默认值或缓存中的旧数据：

```java
@HystrixCommand(fallbackMethod = "getUserFallback")
public User getUser(Long id) {
    return getFromCacheOrDB(id);
}

public User getUserFallback(Long id) {
    // 返回默认用户或提示稍后重试
    return new User(id, "系统繁忙，请稍后重试", null);
}
```

## 分布式锁

### 基于 SET NX EX

```bash
# 加锁（原子操作）
SET lock:order:1001 <unique_token> NX EX 30
# NX: 不存在才设置
# EX 30: 过期时间 30 秒

# 释放锁（Lua 脚本保证原子性）
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

Java 实现：

```java
public class RedisLock {
    
    private final StringRedisTemplate redis;
    
    public boolean tryLock(String lockKey, String requestId, long expireSeconds) {
        Boolean result = redis.opsForValue()
            .setIfAbsent(lockKey, requestId, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }
    
    public void unlock(String lockKey, String requestId) {
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";
        
        redis.execute(
            new DefaultRedisScript<>(script, Long.class),
            List.of(lockKey),
            requestId
        );
    }
}
```

### Redisson 分布式锁

Redisson 是最成熟的 Redis Java 客户端，提供了完善的分布式锁实现：

```java
RLock lock = redisson.getLock("myLock");

try {
    // 尝试加锁，最多等待 10 秒，锁自动过期 30 秒
    boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
    if (locked) {
        // 执行业务逻辑
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

Redisson 的锁支持看门狗（Watchdog）机制：如果业务执行时间超过锁的过期时间，会自动续期。

### Redlock 算法

单个 Redis 实例的分布式锁在主从切换时可能丢失。Redlock 算法通过多个独立 Redis 实例来提高可靠性：

```
1. 获取当前时间 T1
2. 依次向 N 个独立 Redis 实例加锁（相同的 key 和过期时间）
3. 计算加锁耗时 = 当前时间 T2 - T1
4. 如果在 N/2+1 个以上实例加锁成功，且耗时小于锁过期时间 → 加锁成功
5. 锁的有效时间 = 过期时间 - 加锁耗时
6. 如果加锁失败，向所有实例释放锁
```

```java
// Redisson Redlock
RLock lock1 = redisson1.getLock("lock");
RLock lock2 = redisson2.getLock("lock");
RLock lock3 = redisson3.getLock("lock");

RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);
try {
    boolean locked = redLock.tryLock(10, 30, TimeUnit.SECONDS);
    if (locked) {
        // 执行业务
    }
} finally {
    redLock.unlock();
}
```

**注意**：Redlock 的正确性在学术界有争议（Martin Kleppmann 的批评），对于强一致性要求极高的场景，建议使用 ZooKeeper 或 etcd 的分布式锁。

## 延迟队列

利用 ZSet 实现延迟队列：

```java
// 生产者：添加延迟消息
public void addDelayTask(String queue, String task, long delaySeconds) {
    double score = System.currentTimeMillis() + delaySeconds * 1000;
    redis.opsForZSet().add(queue, task, score);
}

// 消费者：轮询获取到期任务
public List<String> pollExpiredTasks(String queue, int batchSize) {
    double now = System.currentTimeMillis();
    Set<String> tasks = redis.opsForZSet()
        .rangeByScore(queue, 0, now, 0, batchSize);
    
    if (tasks != null && !tasks.isEmpty()) {
        // 原子性移除
        redis.opsForZSet().remove(queue, tasks.toArray());
    }
    
    return tasks != null ? new ArrayList<>(tasks) : Collections.emptyList();
}
```

使用 Lua 脚本保证原子性：

```lua
-- 原子性获取并移除到期任务
local tasks = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1], 'LIMIT', 0, ARGV[2])
if #tasks > 0 then
    redis.call('ZREM', KEYS[1], unpack(tasks))
end
return tasks
```

典型应用场景：
- 订单超时未支付自动取消
- 定时提醒
- 延迟重试

## 限流

### 固定窗口计数器

```java
public boolean isAllowed(String key, int limit, int windowSeconds) {
    Long count = redis.opsForValue().increment(key);
    if (count == 1) {
        redis.expire(key, windowSeconds, TimeUnit.SECONDS);
    }
    return count <= limit;
}

// 使用
boolean allowed = isAllowed("rate:user:1001:" + currentMinute, 100, 60);
```

**缺点**：窗口边界可能有突发流量（如前一窗口最后 1 秒和当前窗口前 1 秒的请求叠加）。

### 滑动窗口（Lua 脚本）

```lua
-- KEYS[1]: 限流 key
-- ARGV[1]: 窗口大小（毫秒）
-- ARGV[2]: 最大请求数
-- ARGV[3]: 当前时间戳（毫秒）
-- ARGV[4]: 唯一请求 ID

local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local uuid = ARGV[4]

-- 移除窗口外的请求
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

-- 当前窗口内的请求数
local count = redis.call('ZCARD', key)

if count < limit then
    -- 允许请求，添加到窗口
    redis.call('ZADD', key, now, uuid)
    redis.call('PEXPIRE', key, window)
    return 1
else
    return 0
end
```

Java 调用：

```java
public boolean slidingWindowRateLimit(String key, int limit, int windowMillis) {
    String script = "...";  // 上面的 Lua 脚本
    Long result = redis.execute(
        new DefaultRedisScript<>(script, Long.class),
        List.of(key),
        String.valueOf(windowMillis),
        String.valueOf(limit),
        String.valueOf(System.currentTimeMillis()),
        UUID.randomUUID().toString()
    );
    return result != null && result == 1;
}
```

### 令牌桶（Lua 脚本）

```lua
-- KEYS[1]: 令牌桶 key
-- ARGV[1]: 桶容量
-- ARGV[2]: 每秒填充令牌数
-- ARGV[3]: 当前时间戳（微秒）
-- ARGV[4]: 请求数量

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local info = redis.call('HMGET', key, 'tokens', 'last_time')
local tokens = tonumber(info[1]) or capacity
local last_time = tonumber(info[2]) or now

-- 计算时间间隔，填充令牌
local elapsed = (now - last_time) / 1000000
local new_tokens = math.min(capacity, tokens + elapsed * rate)

local allowed = 0
if new_tokens >= requested then
    new_tokens = new_tokens - requested
    allowed = 1
end

redis.call('HMSET', key, 'tokens', new_tokens, 'last_time', now)
redis.call('PEXPIRE', key, math.ceil(capacity / rate) * 1000)

return allowed
```

令牌桶的优势是允许一定的突发流量（桶满时），同时保持长期的平均速率。
