# 数据访问层设计与 ORM 实践

数据访问层（DAL）是应用与数据库之间的桥梁。合理的 DAL 设计能提高开发效率、降低数据库负载、保障数据一致性。本文讲解 JDBC 连接池配置、ORM 框架选型、N+1 查询问题、批量操作优化、读写分离和数据库迁移工具。

## JDBC 连接池

### 为什么需要连接池

每次请求创建和销毁数据库连接的开销很大（TCP 握手、认证、初始化会话）。连接池复用连接，避免频繁创建销毁。

### HikariCP 配置

HikariCP 是 Spring Boot 默认的连接池，以高性能和低延迟著称。

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/codenow?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    hikari:
      # 连接池名称
      pool-name: codenow-hikari
      # 最大连接数
      maximum-pool-size: 20
      # 最小空闲连接数
      minimum-idle: 5
      # 连接超时时间（毫秒）
      connection-timeout: 30000
      # 空闲连接最大存活时间（毫秒）
      idle-timeout: 600000
      # 连接最大存活时间（毫秒）
      max-lifetime: 1800000
      # 连接测试查询
      connection-test-query: SELECT 1
      # 连接泄漏检测阈值（毫秒）
      leak-detection-threshold: 60000
```

**最大连接数计算公式**（经验法则）：

```
最大连接数 = CPU 核心数 * 2 + 有效磁盘数
```

例如 4 核 CPU + 1 块 SSD：`4 * 2 + 1 = 9`。通常设置 10-20 足够，过多反而因上下文切换降低性能。

### Druid 配置

Druid 是阿里巴巴开源的连接池，内置监控和 SQL 防火墙。

```yaml
spring:
  datasource:
    druid:
      initial-size: 5
      max-active: 20
      min-idle: 5
      max-wait: 30000
      # 监控统计
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin
      # 过滤器
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 1000
        wall:
          enabled: true
          # SQL 防火墙
          config:
            delete-where-none-check: true
```

### HikariCP vs Druid

| 特性 | HikariCP | Druid |
|---|---|---|
| 性能 | 极高（字节码优化） | 高 |
| 监控 | 基础 JMX | 内置 Web 监控 |
| SQL 防火墙 | 无 | 内置 WallFilter |
| 慢 SQL 日志 | 需配合 p6spy | 内置 |
| 扩展性 | 灵活 | 丰富 |
| Spring Boot 默认 | 是 | 否 |

## ORM 框架对比

### MyBatis

MyBatis 是半自动 ORM，SQL 与 Java 代码分离，灵活性最高。

```xml
<!-- ArticleMapper.xml -->
<mapper namespace="com.codenow.mapper.ArticleMapper">
    
    <resultMap id="articleResultMap" type="Article">
        <id property="id" column="id"/>
        <result property="title" column="title"/>
        <result property="content" column="content"/>
        <result property="authorId" column="author_id"/>
        <result property="createdAt" column="created_at"/>
        <association property="author" javaType="User">
            <id property="id" column="author_id"/>
            <result property="name" column="author_name"/>
        </association>
    </resultMap>
    
    <select id="findByIdWithAuthor" resultMap="articleResultMap">
        SELECT a.*, u.name AS author_name
        FROM articles a
        LEFT JOIN users u ON a.author_id = u.id
        WHERE a.id = #{id}
    </select>
    
    <select id="findByCondition" resultType="Article">
        SELECT * FROM articles
        <where>
            <if test="status != null">
                AND status = #{status}
            </if>
            <if test="categoryId != null">
                AND category_id = #{categoryId}
            </if>
            <if test="keyword != null">
                AND (title LIKE CONCAT('%', #{keyword}, '%')
                     OR content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
        </where>
        ORDER BY created_at DESC
    </select>
    
</mapper>
```

### JPA（Spring Data JPA）

JPA 是全自动 ORM，通过实体映射自动生成 SQL。

```java
@Entity
@Table(name = "articles")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
    
    @Enumerated(EnumType.STRING)
    private ArticleStatus status;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}

// Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    List<Article> findByStatusOrderByCreatedAtDesc(ArticleStatus status);
    
    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword% OR a.content LIKE %:keyword%")
    List<Article> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT a FROM Article a JOIN FETCH a.author WHERE a.id = :id")
    Optional<Article> findByIdWithAuthor(@Param("id") Long id);
}
```

### JOOQ

JOOQ 是类型安全的 SQL 构建器，生成 Java 代码对应数据库表结构。

```java
// 生成的代码
Result<Record> result = dslContext
    .select(ARTICLE.TITLE, ARTICLE.VIEWS, USER.NAME)
    .from(ARTICLE)
    .join(USER).on(ARTICLE.AUTHOR_ID.eq(USER.ID))
    .where(ARTICLE.STATUS.eq("published"))
    .orderBy(ARTICLE.VIEWS.desc())
    .limit(10)
    .fetch();
```

### 框架选择建议

| 场景 | 推荐 |
|---|---|
| 复杂 SQL、性能敏感 | MyBatis |
| 快速开发、简单 CRUD | JPA |
| 类型安全、动态 SQL | JOOQ |
| 已有大量 SQL | MyBatis |
| 微服务 + 简单业务 | JPA |

## N+1 查询问题

N+1 查询是 ORM 中最常见的性能陷阱：查询 N 条主记录后，对每条记录再执行一次关联查询。

### 问题演示

```java
// JPA 示例
// 第 1 次查询：获取所有文章
List<Article> articles = articleRepository.findByStatus(ArticleStatus.PUBLISHED);

// 对每篇文章访问作者，触发 N 次额外查询
for (Article article : articles) {
    System.out.println(article.getAuthor().getName());  // 触发 SELECT
}
// 总共 1 + N 次查询
```

### 解决方案一：JOIN FETCH

```java
// JPA：使用 JOIN FETCH 一次性加载关联数据
@Query("SELECT a FROM Article a JOIN FETCH a.author WHERE a.status = :status")
List<Article> findByStatusWithAuthor(@Param("status") ArticleStatus status);
```

### 解决方案二：@BatchSize

```java
@Entity
public class Article {
    @ManyToOne(fetch = FetchType.LAZY)
    @BatchSize(size = 20)  // 每次加载 20 个作者
    private User author;
}
```

Hibernate 会将多次单独查询合并为一次 `IN` 查询：`SELECT * FROM users WHERE id IN (?, ?, ?, ...)`。

### 解决方案三：MyBatis 嵌套查询

```xml
<!-- 方式一：嵌套查询（有 N+1 问题） -->
<resultMap id="articleMap" type="Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <association property="author" javaType="User"
        select="com.codenow.mapper.UserMapper.findById"
        column="author_id"/>
</resultMap>

<!-- 方式二：JOIN 查询（推荐） -->
<resultMap id="articleWithAuthorMap" type="Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <association property="author" javaType="User">
        <id property="id" column="author_id"/>
        <result property="name" column="author_name"/>
    </association>
</resultMap>

<select id="findAllWithAuthor" resultMap="articleWithAuthorMap">
    SELECT a.id, a.title, u.id AS author_id, u.name AS author_name
    FROM articles a
    LEFT JOIN users u ON a.author_id = u.id
    WHERE a.status = #{status}
</select>
```

## 批量操作优化

### 逐条插入 vs 批量插入

```java
// 不推荐：逐条插入
for (Article article : articles) {
    articleMapper.insert(article);  // N 次网络往返
}

// 推荐：批量插入
articleMapper.batchInsert(articles);  // 1 次网络往返
```

MyBatis 批量插入：

```xml
<insert id="batchInsert">
    INSERT INTO articles (title, content, author_id, status)
    VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.title}, #{item.content}, #{item.authorId}, #{item.status})
    </foreach>
</insert>
```

### MyBatis ExecutorType.BATCH

```java
// 使用 SqlSession 的 BATCH 模式
try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
    ArticleMapper mapper = session.getMapper(ArticleMapper.class);
    for (Article article : articles) {
        mapper.insert(article);  // 积累到批次
    }
    session.flushStatements();   // 执行批量
}
```

### JPA 批量配置

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
```

```java
// JPA 批量保存
@Transactional
public void batchSave(List<Article> articles) {
    for (int i = 0; i < articles.size(); i++) {
        entityManager.persist(articles.get(i));
        if (i % 50 == 0) {
            entityManager.flush();
            entityManager.clear();  // 清除一级缓存，释放内存
        }
    }
}
```

### 批量更新

```xml
<!-- MyBatis 批量更新 -->
<update id="batchUpdateStatus">
    UPDATE articles SET status = #{newStatus}
    WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</update>
```

## 读写分离

在应用层实现读写分离，将读操作路由到从库。

### 手动切换数据源

```java
public class DataSourceContextHolder {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();
    
    public static void setMaster() { CONTEXT.set("master"); }
    public static void setSlave() { CONTEXT.set("slave"); }
    public static String get() { return CONTEXT.get(); }
    public static void clear() { CONTEXT.remove(); }
}

// AOP 自动切换
@Aspect
@Component
public class DataSourceAspect {
    
    @Before("@annotation(slaveRead)")
    public void switchToSlave(SlaveRead slaveRead) {
        DataSourceContextHolder.setSlave();
    }
    
    @After("@annotation(slaveRead)")
    public void clearDataSource(SlaveRead slaveRead) {
        DataSourceContextHolder.clear();
    }
}

// 使用注解标记读方法
@SlaveRead
public Article getArticle(Long id) {
    return articleMapper.findById(id);
}
```

### ShardingSphere 读写分离

```yaml
# ShardingSphere-JDBC 配置
spring:
  shardingsphere:
    datasource:
      names: master,slave0
      master:
        type: com.zaxxer.hikari.HikariDataSource
        jdbc-url: jdbc:mysql://master:3306/mydb
        username: root
        password: root
      slave0:
        type: com.zaxxer.hikari.HikariDataSource
        jdbc-url: jdbc:mysql://slave0:3306/mydb
        username: root
        password: root
    rules:
      readwrite-splitting:
        data-sources:
          myds:
            write-data-source-name: master
            read-data-source-names: slave0
            load-balancer-name: round-robin
        load-balancers:
          round-robin:
            type: ROUND_ROBIN
```

## 数据库迁移工具

### Flyway

Flyway 通过版本化的 SQL 脚本管理数据库 Schema 变更。

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- V2__add_user_status.sql
ALTER TABLE users ADD COLUMN status TINYINT DEFAULT 1;

-- V3__create_articles_table.sql
CREATE TABLE articles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    author_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id)
);
```

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
```

Flyway 的命名规则：
- `V{版本号}__{描述}.sql`：版本迁移（只执行一次）
- `R__{描述}.sql`：可重复执行的迁移
- `U{版本号}__{描述}.sql`：撤销迁移（付费版）

### Liquibase

Liquibase 支持 XML、YAML、JSON、SQL 四种格式的变更集。

```yaml
# changelog.yaml
databaseChangeLog:
  - changeSet:
      id: 1
      author: admin
      changes:
        - createTable:
            tableName: users
            columns:
              - column:
                  name: id
                  type: bigint
                  autoIncrement: true
                  constraints:
                    primaryKey: true
              - column:
                  name: username
                  type: varchar(50)
                  constraints:
                    nullable: false
                    unique: true
  - changeSet:
      id: 2
      author: admin
      changes:
        - addColumn:
            tableName: users
            columns:
              - column:
                  name: status
                  type: tinyint
                  defaultValueNumeric: 1
```

### Flyway vs Liquibase

| 特性 | Flyway | Liquibase |
|---|---|---|
| 脚本格式 | SQL | XML/YAML/JSON/SQL |
| 学习曲线 | 低 | 中 |
| 回滚支持 | 付费版 | 免费 |
| 社区版功能 | 基础迁移 | 完整功能 |
| 适用场景 | 简单项目 | 复杂项目 |
