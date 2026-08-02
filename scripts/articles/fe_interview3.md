# Spring 与数据库面试高频题

Spring 和数据库相关的面试题是 Java 后端面试的重中之重。本文整理了最高频的面试题，不仅给出答案，更深入讲解背后的原理，帮助你在面试中展现真正的技术深度。

## Spring IoC 容器原理

IoC（Inversion of Control，控制反转）是 Spring 的核心思想。传统开发中，对象的创建和依赖关系由开发者手动管理；有了 IoC 容器后，这些工作交给 Spring 来做。

### IoC 的本质

```java
// 没有 IoC：手动创建和组装依赖
UserService userService = new UserServiceImpl(
    new UserRepositoryImpl(
        new DataSource()
    )
);

// 有 IoC：容器自动管理
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
}
```

IoC 容器的核心数据结构是 `BeanDefinition`，它保存了 Bean 的所有元信息：

```java
public class BeanDefinition {
    private String beanName;
    private Class<?> beanClass;
    private Scope scope;  // singleton, prototype
    private String initMethodName;
    private String destroyMethodName;
    private ConstructorArgumentValues constructorArgs;
    private PropertyValues propertyValues;
    // ...
}
```

### Bean 的生命周期

一个 Bean 从创建到销毁经历的完整流程：

```
1. 实例化（Instantiation）
   → 通过构造函数创建对象（此时还是半成品）

2. 属性填充（Populate Properties）
   → 注入 @Autowired、@Value 等标注的依赖

3. Aware 回调
   → BeanNameAware.setBeanName()
   → BeanFactoryAware.setBeanFactory()
   → ApplicationContextAware.setApplicationContext()

4. BeanPostProcessor 前置处理
   → postProcessBeforeInitialization()

5. 初始化
   → @PostConstruct
   → InitializingBean.afterPropertiesSet()
   → 自定义 init-method

6. BeanPostProcessor 后置处理
   → postProcessAfterInitialization()
   → AOP 代理就是在这一步生成的

7. 使用 Bean

8. 销毁
   → @PreDestroy
   → DisposableBean.destroy()
   → 自定义 destroy-method
```

### 循环依赖的解决

Spring 通过三级缓存解决单例 Bean 的循环依赖：

| 缓存级别 | 名称 | 存储内容 |
|----------|------|----------|
| 一级 | singletonObjects | 完整的 Bean 实例 |
| 二级 | earlySingletonObjects | 提前暴露的 Bean（可能是代理对象） |
| 三级 | singletonFactories | Bean 的 ObjectFactory |

```java
// 简化的循环依赖解决流程
// A 依赖 B，B 依赖 A

1. 创建 A → 实例化 A → 将 A 的 ObjectFactory 放入三级缓存
2. 填充 A 的属性 → 发现依赖 B → 去创建 B
3. 创建 B → 实例化 B → 将 B 的 ObjectFactory 放入三级缓存
4. 填充 B 的属性 → 发现依赖 A → 从三级缓存获取 A 的 ObjectFactory
5. 调用 ObjectFactory.getObject() → 获取 A 的早期引用 → 放入二级缓存
6. B 创建完成 → 放入一级缓存
7. 回到 A 的创建 → 注入 B → A 创建完成 → 放入一级缓存
```

**注意**：构造器注入的循环依赖无法解决，只能解决 setter/字段注入的循环依赖。Spring Boot 2.6+ 默认禁止循环依赖。

## Spring AOP 原理

AOP（面向切面编程）是 IoC 的一个重要补充。它允许我们将横切关注点（如日志、事务、权限）从业务代码中分离出来。

### AOP 的两种代理方式

| 方式 | 机制 | 条件 |
|------|------|------|
| JDK 动态代理 | 基于接口 | 目标类实现了接口 |
| CGLIB 代理 | 基于继承 | 目标类没有实现接口 |

```java
// JDK 动态代理示例
public class JdkProxyDemo {
    interface UserService {
        void save(User user);
    }
    
    static class UserServiceImpl implements UserService {
        public void save(User user) {
            System.out.println("保存用户: " + user.getName());
        }
    }
    
    public static void main(String[] args) {
        UserService target = new UserServiceImpl();
        
        UserService proxy = (UserService) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            (obj, method, methodArgs) -> {
                System.out.println("Before: 开启事务");
                Object result = method.invoke(target, methodArgs);
                System.out.println("After: 提交事务");
                return result;
            }
        );
        
        proxy.save(new User("张三"));
    }
}
```

### @Transactional 事务失效的常见场景

这是面试中的高频问题，掌握这些场景能体现你的实战经验：

**1. 方法不是 public 的**

```java
@Service
public class UserService {
    // 事务不会生效！Spring AOP 默认只代理 public 方法
    @Transactional
    void internalMethod() {
        // ...
    }
}
```

**2. 自调用问题**

```java
@Service
public class UserService {
    public void methodA() {
        // 自调用不经过代理，事务不会生效
        this.methodB();
    }
    
    @Transactional
    public void methodB() {
        // ...
    }
}

// 解决方案：注入自身代理
@Service
public class UserService {
    @Lazy
    @Autowired
    private UserService self;
    
    public void methodA() {
        self.methodB();  // 通过代理调用
    }
    
    @Transactional
    public void methodB() {
        // ...
    }
}
```

**3. 异常类型不匹配**

```java
@Service
public class UserService {
    // 默认只回滚 RuntimeException 和 Error
    // 检查型异常不会回滚
    @Transactional
    public void save() throws Exception {
        // ...
        throw new Exception("检查型异常");  // 不会回滚！
    }
    
    // 正确做法：指定 rollbackFor
    @Transactional(rollbackFor = Exception.class)
    public void saveCorrect() throws Exception {
        // ...
    }
}
```

**4. 异常被吞掉**

```java
@Transactional
public void save() {
    try {
        // 业务逻辑
        jdbcTemplate.execute("INSERT INTO ...");
        int i = 1 / 0;  // 抛出异常
    } catch (Exception e) {
        // 异常被吞掉，事务不会回滚
        log.error("出错了", e);
    }
}
```

**5. 数据库引擎不支持事务**

MySQL 的 MyISAM 引擎不支持事务，只有 InnoDB 支持。

## MyBatis 缓存机制

MyBatis 提供了两级缓存，理解它们的工作原理对性能优化至关重要。

### 一级缓存（SqlSession 级别）

```java
// 同一个 SqlSession 中，相同的查询会命中缓存
SqlSession session = sqlSessionFactory.openSession();
UserMapper mapper = session.getMapper(UserMapper.class);

User user1 = mapper.selectById(1);  // 查询数据库
User user2 = mapper.selectById(1);  // 命中一级缓存，不查数据库

// 执行 update/insert/delete 后，一级缓存会被清空
mapper.updateUser(user1);
User user3 = mapper.selectById(1);  // 重新查询数据库
```

### 二级缓存（Mapper 级别）

```xml
<!-- 开启二级缓存 -->
<mapper namespace="com.example.mapper.UserMapper">
    <cache
        eviction="LRU"
        flushInterval="60000"
        size="1024"
        readOnly="true"/>
    
    <select id="selectById" resultType="User" useCache="true">
        SELECT * FROM user WHERE id = #{id}
    </select>
</mapper>
```

| 属性 | 说明 |
|------|------|
| eviction | 缓存回收策略：LRU、FIFO、SOFT、WEAK |
| flushInterval | 缓存刷新间隔（毫秒） |
| size | 缓存对象数量 |
| readOnly | 是否只读 |

**注意**：二级缓存在 SqlSession 提交或关闭后才会生效。在实际项目中，由于分布式环境下的缓存一致性问题，二级缓存很少使用，通常用 Redis 替代。

## MySQL 索引优化

索引是数据库性能优化的核心。理解索引的原理和使用规范，是后端开发者的必备技能。

### B+ 树索引结构

MySQL InnoDB 使用 B+ 树作为索引结构：

```
                    [10 | 20 | 30]           ← 非叶子节点（只存键值）
                   /    |     |    \
            [1,5,8] [12,15,18] [22,25,28] [31,35,38]  ← 叶子节点（存数据）
              ↓        ↓          ↓          ↓
            双向链表连接，支持范围查询
```

### 索引失效的常见场景

```sql
-- 1. 对索引列使用函数
SELECT * FROM user WHERE YEAR(create_time) = 2024;  -- 索引失效
SELECT * FROM user WHERE create_time >= '2024-01-01' 
    AND create_time < '2025-01-01';  -- 索引有效

-- 2. 隐式类型转换
-- phone 是 varchar 类型
SELECT * FROM user WHERE phone = 13800138000;  -- 索引失效
SELECT * FROM user WHERE phone = '13800138000';  -- 索引有效

-- 3. 最左前缀原则
-- 联合索引 (a, b, c)
SELECT * FROM table WHERE b = 1;  -- 索引失效
SELECT * FROM table WHERE a = 1 AND c = 3;  -- 只用到 a
SELECT * FROM table WHERE a = 1 AND b = 2 AND c = 3;  -- 完整使用

-- 4. LIKE 以通配符开头
SELECT * FROM user WHERE name LIKE '%三';  -- 索引失效
SELECT * FROM user WHERE name LIKE '张%';  -- 索引有效

-- 5. OR 条件中有非索引列
SELECT * FROM user WHERE id = 1 OR name = '张三';
-- 如果 name 没有索引，整个查询索引失效
```

### EXPLAIN 执行计划

```sql
EXPLAIN SELECT * FROM user WHERE name = '张三';
```

关键字段解读：

| 字段 | 说明 | 期望值 |
|------|------|--------|
| type | 访问类型 | system > const > eq_ref > ref > range > index > ALL |
| key | 实际使用的索引 | 不为 NULL |
| rows | 预估扫描行数 | 越小越好 |
| Extra | 额外信息 | Using index 最好 |

## Redis 缓存策略

Redis 在后端系统中承担着缓存、会话管理、分布式锁等多种角色。掌握缓存策略是面试的高频考点。

### 缓存穿透

查询一个一定不存在的数据，请求会穿透缓存直达数据库。

```java
// 解决方案1：缓存空值
public User getUser(Long id) {
    String key = "user:" + id;
    String value = redis.get(key);
    
    if (value != null) {
        if ("NULL".equals(value)) {
            return null;  // 缓存的空值
        }
        return JSON.parseObject(value, User.class);
    }
    
    User user = userMapper.selectById(id);
    if (user == null) {
        redis.setex(key, 300, "NULL");  // 缓存空值，5分钟过期
    } else {
        redis.setex(key, 3600, JSON.toJSONString(user));
    }
    return user;
}

// 解决方案2：布隆过滤器
public User getUserWithBloom(Long id) {
    if (!bloomFilter.mightContain(id)) {
        return null;  // 布隆过滤器说不存在，一定不存在
    }
    // 可能存在，查询缓存和数据库
    return getUser(id);
}
```

### 缓存击穿

热点 key 过期的瞬间，大量请求同时打到数据库。

```java
// 解决方案：互斥锁
public User getHotUser(Long id) {
    String key = "user:hot:" + id;
    String value = redis.get(key);
    
    if (value != null) {
        return JSON.parseObject(value, User.class);
    }
    
    // 尝试获取锁
    String lockKey = "lock:user:" + id;
    boolean locked = redis.setnx(lockKey, "1", 10);
    
    if (locked) {
        try {
            // 双重检查
            value = redis.get(key);
            if (value != null) {
                return JSON.parseObject(value, User.class);
            }
            
            User user = userMapper.selectById(id);
            redis.setex(key, 3600, JSON.toJSONString(user));
            return user;
        } finally {
            redis.del(lockKey);
        }
    } else {
        // 未获取到锁，休眠后重试
        Thread.sleep(50);
        return getHotUser(id);
    }
}
```

### 缓存雪崩

大量 key 同时过期，导致数据库压力骤增。

```java
// 解决方案：过期时间加随机值
public void cacheUser(User user) {
    String key = "user:" + user.getId();
    int baseExpire = 3600;
    int randomExpire = ThreadLocalRandom.current().nextInt(0, 600);
    redis.setex(key, baseExpire + randomExpire, JSON.toJSONString(user));
}
```

## 分布式锁实现

在分布式系统中，单机的 synchronized 或 ReentrantLock 无法跨 JVM 生效，需要使用分布式锁。

### Redis 分布式锁

```java
@Component
public class RedisDistributedLock {
    @Autowired
    private StringRedisTemplate redis;
    
    private static final String LOCK_PREFIX = "distributed:lock:";
    
    /**
     * 尝试获取锁
     * @param lockKey 锁的 key
     * @param requestId 请求唯一标识（用于安全释放锁）
     * @param expireTime 过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime) {
        String key = LOCK_PREFIX + lockKey;
        Boolean result = redis.opsForValue()
            .setIfAbsent(key, requestId, expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }
    
    /**
     * 释放锁（Lua 脚本保证原子性）
     */
    public boolean releaseLock(String lockKey, String requestId) {
        String key = LOCK_PREFIX + lockKey;
        String luaScript = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";
        
        Long result = redis.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(key),
            requestId
        );
        return Long.valueOf(1L).equals(result);
    }
}
```

### Redlock 算法

单个 Redis 节点的分布式锁存在单点故障问题。Redlock 算法通过在多个独立的 Redis 节点上获取锁来提高可靠性：

```
1. 获取当前时间（毫秒）
2. 依次向 N 个 Redis 节点请求锁（使用相同的 key 和随机值）
3. 计算获取锁花费的时间，必须小于锁的过期时间
4. 如果在多数节点（N/2 + 1）上获取成功，则认为锁获取成功
5. 锁的有效时间 = 初始过期时间 - 获取锁花费的时间
6. 如果获取锁失败，在所有节点上释放锁
```

### 分布式锁的对比

| 实现方式 | 优点 | 缺点 |
|----------|------|------|
| Redis | 性能高、实现简单 | 主从切换可能丢锁 |
| ZooKeeper | 强一致性、临时节点自动删除 | 性能不如 Redis |
| etcd | 强一致性、租约机制 | 运维成本高 |

在大多数场景下，Redis 分布式锁（配合 Redisson 框架）已经足够。如果对一致性要求极高，可以考虑 ZooKeeper 或 etcd。

## 面试中的回答技巧

回答 Spring 和数据库相关的问题时，建议采用"原理 + 实践 + 踩坑"的结构：

1. **先说原理**：简要说明技术点的底层机制
2. **再讲实践**：结合项目经验，说你是怎么用的
3. **最后踩坑**：分享你遇到过的问题和解决方案

比如被问到"Spring 事务失效的场景"，你可以这样回答：

> Spring 事务基于 AOP 代理实现，所以代理失效的场景都会导致事务失效。我在项目中遇到过两个典型问题：一是自调用问题，service 方法内部调用另一个带 @Transactional 的方法，事务不生效，解决方案是注入自身代理；二是异常被 catch 吞掉了，事务感知不到异常就不会回滚。后来我们团队约定，所有 service 方法的异常都要向上抛出，统一在 Controller 层处理。

这样的回答既有理论深度，又有实践经验，能给面试官留下深刻印象。
