# 性能优化：缓存、懒加载与 SQL 监控

MyBatis 的性能优化是一个系统工程，涉及缓存机制、加载策略、SQL 监控、连接池配置和索引设计等多个层面。理解这些优化手段，能让我们在面对高并发和大数据量场景时游刃有余。

## MyBatis 一级缓存

一级缓存是 MyBatis 最基础的缓存机制，作用域为单个 `SqlSession`。

### 生命周期

一级缓存的生命周期与 `SqlSession` 一致：

1. 创建 `SqlSession` 时，一级缓存随之创建
2. 执行查询时，MyBatis 将结果存入缓存（以 SQL + 参数为 key）
3. 再次执行相同 SQL 和参数时，直接从缓存返回，不再查询数据库
4. `SqlSession` 关闭或调用 `clearCache()` 时，缓存被清空

```java
SqlSession session = sqlSessionFactory.openSession();
try {
    ArticleMapper mapper = session.getMapper(ArticleMapper.class);

    // 第一次查询：执行 SQL，结果放入缓存
    Article a1 = mapper.selectById(1L);

    // 第二次查询：命中缓存，不执行 SQL
    Article a2 = mapper.selectById(1L);

    System.out.println(a1 == a2); // true，同一个对象
} finally {
    session.close();
}
```

### 失效条件

一级缓存在以下情况会被清空：

| 失效条件 | 原因 |
|---------|------|
| 执行 `INSERT`、`UPDATE`、`DELETE` | 数据可能已变更，缓存不可信 |
| 调用 `SqlSession.clearCache()` | 手动清空 |
| `SqlSession` 关闭或回滚 | 生命周期结束 |
| 配置 `flushCache="true"` | Mapper 中显式声明 |

**在 Spring 整合场景中**，每次请求通常会创建新的 `SqlSession`（通过 `SqlSessionTemplate`），因此一级缓存的实际作用非常有限。如果使用 `@Transactional`，在同一个事务内多次执行相同查询才能命中一级缓存。

## MyBatis 二级缓存

二级缓存的作用域是 **Mapper 级别**（同一个 namespace），多个 `SqlSession` 可以共享同一个 Mapper 的二级缓存。

### 配置方式

首先在 MyBatis 配置中启用：

```yaml
mybatis:
  configuration:
    cache-enabled: true
```

然后在 Mapper XML 中声明使用缓存：

```xml
<mapper namespace="com.example.mapper.ArticleMapper">
    <cache
        eviction="LRU"
        flushInterval="60000"
        size="1024"
        readOnly="true"/>
</mapper>
```

`cache` 元素的属性：

| 属性 | 说明 | 可选值 |
|------|------|-------|
| `eviction` | 缓存回收策略 | `LRU`（最近最少使用）、`FIFO`（先进先出）、`SOFT`（软引用）、`WEAK`（弱引用） |
| `flushInterval` | 自动刷新间隔（毫秒） | 默认不自动刷新 |
| `size` | 缓存对象数量 | 正整数 |
| `readOnly` | 是否只读 | `true`（返回同一引用，性能好）、`false`（返回副本，安全） |

也可以针对单条语句控制缓存行为：

```xml
<select id="selectById" resultType="Article" useCache="true" flushCache="false">
    SELECT * FROM article WHERE id = #{id}
</select>

<update id="update" flushCache="true">
    UPDATE article SET title = #{title} WHERE id = #{id}
</update>
```

### 序列化要求

二级缓存要求实体类实现 `Serializable` 接口，因为缓存可能被写入磁盘或通过网络传输：

```java
public class Article implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String title;
}
```

### 脏读风险

二级缓存的一个重要风险是**脏读**：当多个 Mapper 操作同一张表时，一个 Mapper 的写操作可能不会清空另一个 Mapper 的缓存。

例如 `ArticleMapper` 和 `CategoryMapper` 都关联了 `category` 表，`ArticleMapper` 中的 JOIN 查询结果缓存了分类信息。此时 `CategoryMapper.update()` 修改了分类名称，但 `ArticleMapper` 的缓存不会被刷新，导致读到旧数据。

**解决方案**：

- 对于关联多表的查询，设置 `flushCache="true"` 或关闭二级缓存
- 使用 Redis 等外部缓存，自行控制缓存失效策略
- 在一致性要求高的场景下，干脆不使用 MyBatis 二级缓存

## Spring 与 MyBatis 缓存整合

Spring 项目中更常见的做法是使用 Spring Cache 抽象层（`@Cacheable`、`@CacheEvict`），而非直接使用 MyBatis 的二级缓存。

```java
@Service
public class ArticleService {

    @Cacheable(value = "articles", key = "#id")
    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }

    @CacheEvict(value = "articles", key = "#article.id")
    public void update(Article article) {
        articleMapper.updateById(article);
    }

    @CacheEvict(value = "articles", allEntries = true)
    public void clearCache() {
        // 清空 articles 缓存空间的所有条目
    }
}
```

配置 Redis 作为缓存后端：

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 缓存过期时间 10 分钟
      cache-null-values: false
```

Spring Cache 的优势在于与业务代码解耦、支持多种缓存实现（Redis、Caffeine、EhCache）、提供条件缓存和 SpEL 表达式。

## 懒加载（延迟加载）

懒加载是指在访问关联对象时才执行查询，而非在主查询时立即加载所有关联数据。

### association 和 collection 的 lazy 属性

在 `resultMap` 中，通过 `fetchType="lazy"` 启用延迟加载：

```xml
<resultMap id="articleMap" type="com.example.entity.Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <!-- 访问 article.category 时才查询分类 -->
    <association property="category" column="category_id"
                 select="com.example.mapper.CategoryMapper.selectById"
                 fetchType="lazy"/>
    <!-- 访问 article.tags 时才查询标签 -->
    <collection property="tags" column="id"
                select="com.example.mapper.TagMapper.selectByArticleId"
                fetchType="lazy"/>
</resultMap>
```

全局配置：

```yaml
mybatis:
  configuration:
    lazy-loading-enabled: true
    aggressive-lazy-loading: false  # 仅调用延迟加载属性的 getter 时才触发
    lazy-load-trigger-methods: ""   # 空字符串表示任何方法调用都不触发（仅 getter）
```

### 懒加载的原理

MyBatis 使用**代理模式**实现懒加载。查询主对象时，关联属性（如 `category`、`tags`）会被替换为代理对象。当调用 `article.getCategory()` 时，代理对象拦截方法调用，执行预定义的 SQL 查询，将结果填充到真实对象中。

### 懒加载的注意事项

- **序列化问题**：懒加载代理在 JSON 序列化时可能触发 N+1 查询，或者序列化出意外的代理对象字段。可以在 Jackson 配置中忽略代理属性
- **事务边界**：懒加载必须在 `SqlSession` 生命周期内完成。如果 Service 方法没有事务，Mapper 返回后 `SqlSession` 已关闭，访问延迟属性会抛 `LazyInitializationException`
- **调试困难**：在日志中不容易看出实际执行了几条 SQL，建议配合 SQL 监控工具使用

## SQL 监控

生产环境中，SQL 监控是发现性能瓶颈的重要手段。

### p6spy

p6spy 是一个 JDBC 代理驱动，可以拦截所有 SQL 执行并记录日志，包括实际参数值和执行耗时。

引入依赖：

```xml
<dependency>
    <groupId>p6spy</groupId>
    <artifactId>p6spy</artifactId>
    <version>3.9.1</version>
</dependency>
```

修改数据源配置：

```yaml
spring:
  datasource:
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
    url: jdbc:p6spy:mysql://localhost:3306/codenow
```

`spy.properties` 配置：

```properties
modulelist=com.p6spy.engine.spy.P6SpyFactory,com.p6spy.engine.logging.P6LogFactory
appender=com.p6spy.engine.spy.appender.Slf4JLogger
logMessageFormat=com.p6spy.engine.spy.appender.CustomLineFormat
customLogMessageFormat=%(currentTime)|%(executionTime)ms|%(sql)
databaseDialectDateFormat=yyyy-MM-dd HH:mm:ss
```

输出效果：

```
2025-01-15 10:23:45|12ms|SELECT id, title, content FROM article WHERE category_id = 1
```

### MyBatis SQL 拦截器

通过实现 MyBatis 的 `Interceptor` 接口，可以自定义 SQL 监控逻辑：

```java
@Intercepts({
    @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
})
@Component
public class SqlMonitorInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(SqlMonitorInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            StatementHandler handler = (StatementHandler) invocation.getTarget();
            BoundSql boundSql = handler.getBoundSql();
            String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
            log.info("SQL执行耗时: {}ms | {}", elapsed, sql);
            if (elapsed > 1000) {
                log.warn("慢SQL警告: {}ms | {}", elapsed, sql);
            }
        }
    }
}
```

## 慢 SQL 检测

### MyBatis-Plus 的 SqlExplainInterceptor

MyBatis-Plus 内置了 SQL 执行分析插件，可以在开发阶段检测全表更新等危险操作：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // SQL 执行分析：全表更新/删除时抛出异常
    interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
    return interceptor;
}
```

### EXPLAIN 分析

对疑似慢 SQL 使用 `EXPLAIN` 分析执行计划：

```sql
EXPLAIN SELECT a.id, a.title, c.name
FROM article a
LEFT JOIN category c ON a.category_id = c.id
WHERE a.created_at > '2025-01-01';
```

重点关注的字段：

| 字段 | 含义 | 优化方向 |
|------|------|---------|
| `type` | 访问类型 | `ALL`（全表扫描）→ `index` → `range` → `ref` → `const`，越往右越好 |
| `key` | 实际使用的索引 | `NULL` 表示没有使用索引 |
| `rows` | 预估扫描行数 | 越小越好 |
| `Extra` | 额外信息 | `Using filesort`（文件排序）、`Using temporary`（临时表）需要优化 |

## 连接池优化：HikariCP 配置

Spring Boot 2.x+ 默认使用 HikariCP 连接池。关键配置项：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20          # 最大连接数
      minimum-idle: 5                # 最小空闲连接数
      connection-timeout: 30000      # 获取连接超时时间（毫秒）
      idle-timeout: 600000           # 空闲连接最大存活时间（毫秒）
      max-lifetime: 1800000          # 连接最大存活时间（毫秒）
      validation-timeout: 5000       # 连接校验超时时间
      leak-detection-threshold: 60000 # 连接泄漏检测阈值（毫秒）
```

连接池大小的经验公式：

```
最大连接数 ≈ CPU 核心数 × 2 + 磁盘数
```

对于 4 核 CPU、单磁盘的服务器，`maximum-pool-size` 设为 10 左右即可。过大的连接池反而会因为上下文切换和锁竞争导致性能下降。

## 批量操作优化

### rewriteBatchedStatements

MySQL JDBC 驱动的 `rewriteBatchedStatements` 参数可以将多条 INSERT 语句重写为一条批量插入：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/codenow?rewriteBatchedStatements=true
```

效果：

```sql
-- 开启前：逐条执行
INSERT INTO tag (name) VALUES ('Java');
INSERT INTO tag (name) VALUES ('Spring');
INSERT INTO tag (name) VALUES ('MyBatis');

-- 开启后：合并为一条
INSERT INTO tag (name) VALUES ('Java'), ('Spring'), ('MyBatis');
```

### ExecutorType.BATCH

MyBatis 的 `BATCH` 执行器可以批量发送 SQL：

```java
SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH);
try {
    TagMapper mapper = session.getMapper(TagMapper.class);
    for (Tag tag : tags) {
        mapper.insert(tag); // 累积到一定数量后批量发送
    }
    session.commit(); // 必须手动提交
} finally {
    session.close();
}
```

在 Spring 中配置全局 Batch 执行器：

```java
@Bean
public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
    SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
    factory.setDataSource(dataSource);
    factory.setConfiguration(mybatisConfig());
    // 注意：BATCH 模式下 @Transactional 的行为会变化
    return factory.getObject();
}
```

**注意**：`BATCH` 模式下，`insert` 后不会立即生成自增 ID，需要在 `commit` 后才能获取。如果业务依赖即时返回的 ID，需谨慎使用。

## 索引优化建议

### 覆盖索引

当查询的所有列都包含在索引中时，MySQL 可以直接从索引返回结果，无需回表查询数据行：

```sql
-- 为 article 表创建覆盖索引
ALTER TABLE article ADD INDEX idx_cat_created (category_id, created_at, title);

-- 这个查询可以直接从索引获取结果
SELECT category_id, created_at, title FROM article WHERE category_id = 1;
```

在 `EXPLAIN` 的 `Extra` 列中会显示 `Using index`，表示使用了覆盖索引。

### 联合索引的最左前缀原则

联合索引 `(a, b, c)` 可以被以下查询使用：

```sql
WHERE a = 1
WHERE a = 1 AND b = 2
WHERE a = 1 AND b = 2 AND c = 3
WHERE a = 1 AND c = 3          -- 只用到 a
WHERE a = 1 ORDER BY b         -- a 等值 + b 排序，可以用到索引
```

以下查询无法使用索引：

```sql
WHERE b = 2                    -- 跳过了 a
WHERE b = 2 AND c = 3          -- 跳过了 a
WHERE a = 1 AND b > 2 AND c = 3 -- c 无法使用索引（范围查询后的列失效）
```

## MyBatis-Plus 逻辑删除与性能

MyBatis-Plus 的逻辑删除通过在 SQL 中自动追加 `WHERE deleted = 0` 来实现：

```java
@TableName(value = "article", autoResultMap = true)
public class Article {
    @TableLogic
    private Integer deleted; // 0=未删除, 1=已删除
}
```

逻辑删除对性能的影响：

**索引失效风险**：如果 `deleted` 列选择性很低（例如 99% 的记录 `deleted=0`），MySQL 优化器可能认为使用该索引不如全表扫描高效，导致索引失效。

```sql
-- 逻辑删除后的查询实际执行
SELECT * FROM article WHERE deleted = 0 AND category_id = 1;
```

**优化建议**：

1. 为 `deleted` 列创建联合索引时，将其放在索引末尾：

```sql
-- 推荐：deleted 在联合索引末尾
ALTER TABLE article ADD INDEX idx_cat_deleted (category_id, deleted);

-- 不推荐：deleted 在最前面
ALTER TABLE article ADD INDEX idx_deleted_cat (deleted, category_id);
```

2. 对于数据量大且已删除记录占比高的表，考虑定期物理删除已标记记录：

```sql
-- 定期清理已删除数据
DELETE FROM article WHERE deleted = 1 AND updated_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

3. 如果表数据量达到千万级，逻辑删除带来的索引膨胀问题会更加明显，此时应评估是否改用物理删除 + 归档表的方案。
