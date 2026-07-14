# 扩展功能一：Redis 缓存热门文章 — 操作日志

> 本功能将浏览量 Top 10 的文章 ID 缓存到 Redis ZSet，博客首页热门文章直接从缓存读取，降低数据库查询压力。

---

## 一、添加 Redis 依赖

文件：`codenow-backend/pom.xml`

新增两个依赖：
- `spring-boot-starter-data-redis` — Spring Data Redis，提供 `RedisTemplate` 和 `StringRedisTemplate`
- `commons-pool2` — Redis 连接池

Redis 连接配置已在 `application.yaml` 中预配置（`localhost:6379`）。

---

## 二、新建 RedisConfig.java

文件：`codenow-backend/src/main/java/com/codenow/config/RedisConfig.java`

配置两个 Bean：
- `RedisTemplate<String, Object>` — Key 使用 String 序列化，Value 使用 JSON 序列化（GenericJackson2JsonRedisSerializer）
- `StringRedisTemplate` — 纯字符串操作模板

JSON 序列化注册了 `JavaTimeModule`，支持 `LocalDateTime` 等时间类型。

---

## 三、新建 HotArticleService.java

文件：`codenow-backend/src/main/java/com/codenow/service/HotArticleService.java`

封装 Redis 缓存操作，核心数据结构：

```
Redis Key:   codenow:hot_articles
Type:        ZSet
Score:       view_count（浏览量）
Member:      articleId（文章 ID）
TTL:         300 秒（5 分钟）
```

提供三个方法：
- `incrementViewCount(articleId, newViewCount)` — 更新文章的 ZSet Score，刷新 TTL
- `getHotArticleIds(topN)` — ZREVRANGE 取 Top N 文章 ID
- `hasCache()` — 检查缓存是否存在

所有方法都 try-catch 包裹，Redis 异常不影响主业务。

---

## 四、修改 BlogArticleServiceImpl.java

文件：`codenow-backend/src/main/java/com/codenow/service/impl/BlogArticleServiceImpl.java`

修改 `getPublishedArticleById()` 方法：
- 原有逻辑：查询文章 → 浏览量 +1 → 返回 VO
- 新增逻辑：浏览量 +1 后，同步调用 `hotArticleService.incrementViewCount()` 更新 Redis ZSet

注入了 `HotArticleService` 依赖。

---

## 五、修改 BlogController.java

文件：`codenow-backend/src/main/java/com/codenow/controller/BlogController.java`

新增接口：`GET /api/blog/articles/hot?topN=10`

实现流程：
1. 从 Redis ZSet 获取 Top N 文章 ID 列表
2. 批量查询 MySQL 获取完整文章数据
3. 按 Redis 返回的排序顺序组装 ArticleVO 列表
4. 过滤掉非已发布文章

注入了 `HotArticleService` 依赖。

---

## 六、修改 BlogLayout.vue

文件：`codenow-frontend/src/layout/BlogLayout.vue`

在侧边栏顶部新增"热门文章"模块：
- 调用 `/api/blog/articles/hot` 获取热门文章列表
- 展示文章标题（最多两行）和阅读量
- 点击跳转到文章详情页
- 无热门文章时显示"暂无热门文章"

---

## 改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `pom.xml` | 修改 | 新增 spring-boot-starter-data-redis、commons-pool2 |
| `RedisConfig.java` | 新建 | RedisTemplate JSON 序列化配置 |
| `HotArticleService.java` | 新建 | Redis 缓存操作封装（ZSet） |
| `BlogArticleServiceImpl.java` | 修改 | 访问文章时同步更新 Redis ZSet |
| `BlogController.java` | 修改 | 新增 `/articles/hot` 接口 |
| `BlogLayout.vue` | 修改 | 侧边栏新增热门文章模块 |

---

## 验收结果

| 验收项 | 结果 |
|--------|------|
| 访问文章后 Redis ZSet Score 更新 | ✅ `getPublishedArticleById()` 中调用 `incrementViewCount()` |
| `/api/blog/articles/hot` 返回 Top 10 | ✅ `BlogController.hotArticles()` 实现 |
| 5 分钟 TTL 自动刷新 | ✅ `TTL_SECONDS = 300`，每次写入刷新 |
| 前端侧边栏显示热门文章 | ✅ `BlogLayout.vue` 新增热门文章模块 |
| Redis 异常不影响主业务 | ✅ 所有 Redis 操作 try-catch 包裹 |
| 后端编译通过 | ✅ `mvnw compile` 无报错 |
