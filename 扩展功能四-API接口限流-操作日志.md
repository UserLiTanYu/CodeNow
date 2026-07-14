# 扩展功能四：API 接口限流 — 操作日志

> 本功能基于 Redis + Lua 实现滑动窗口限流，通过自定义注解标记需限流的接口，支持按 IP 维度配置限流规则，Redis 不可用时自动降级放行。

---

## 一、创建 @RateLimit 注解

文件：`annotation/RateLimit.java`

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| key | String | "" | 限流 Key 前缀（默认使用请求路径） |
| maxCount | int | 10 | 时间窗口内最大请求数 |
| timeWindow | int | 1 | 时间窗口（秒） |
| message | String | "请求过于频繁，请稍后再试" | 触发限流时的提示 |

---

## 二、创建 Lua 脚本

文件：`src/main/resources/scripts/rate_limit.lua`

滑动窗口算法：
1. ZREMRANGEBYSCORE 删除窗口外的旧记录
2. ZCARD 统计窗口内请求数
3. 判断是否超限，未超限则 ZADD 记录本次请求
4. PEXPIRE 设置 Key 过期时间（防止 Key 堆积）

Redis Key 格式：`rate_limit:{路径}:{IP}`

---

## 三、创建限流异常

文件：`exception/RateLimitException.java`

- 继承 RuntimeException
- code = 429

文件：`exception/GlobalExceptionHandler.java`

- 新增 `@ExceptionHandler(RateLimitException.class)`
- 返回 `R.error(429, message)`

---

## 四、创建 AOP 切面

文件：`aspect/RateLimitAspect.java`

核心逻辑：
1. `@Before("@annotation(com.codenow.annotation.RateLimit)")` 拦截限流方法
2. 从 `@RateLimit` 注解读取限流参数
3. 构建 Redis Key：`rate_limit:{请求路径}:{客户端IP}`
4. 执行 Lua 脚本判断是否放行
5. 返回 0 则抛出 RateLimitException
6. Redis 异常时 catch 后 log.warn 放行（降级策略）

---

## 五、接口添加限流注解

| Controller | 接口 | maxCount | timeWindow | 理由 |
|-----------|------|----------|------------|------|
| AuthController | POST /api/auth/login | 5 | 60s | 防暴力破解密码 |
| CommentController | POST /api/comments | 5 | 60s | 防垃圾评论刷屏 |
| BlogController | GET /api/blog/articles | 30 | 10s | 防恶意刷接口 |
| BlogController | GET /api/blog/articles/hot | 20 | 10s | 防热门文章接口被刷 |

---

## 六、前端处理

文件：`utils/request.js`

无需额外改动。现有的响应拦截器已处理 `code !== 200` 的情况，限流返回 `code: 429` 时会自动弹出 `ElMessage.error(message)`。

---

## 改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `annotation/RateLimit.java` | 新建 | 限流注解 |
| `resources/scripts/rate_limit.lua` | 新建 | 滑动窗口 Lua 脚本 |
| `exception/RateLimitException.java` | 新建 | 限流异常（code=429） |
| `exception/GlobalExceptionHandler.java` | 修改 | 新增限流异常处理 |
| `aspect/RateLimitAspect.java` | 新建 | AOP 切面（Redis + Lua 执行） |
| `controller/AuthController.java` | 修改 | 登录接口加限流 |
| `controller/CommentController.java` | 修改 | 评论接口加限流 |
| `controller/BlogController.java` | 修改 | 文章列表和热门文章接口加限流 |

---

## 验收结果

| 验收项 | 结果 |
|--------|------|
| @RateLimit 可配置 maxCount 和 timeWindow | ✅ |
| 超过限制返回 `{code: 429, message: "请求过于频繁"}` | ✅ |
| 窗口过期后计数器重置，请求恢复正常 | ✅ |
| 限流数据存 Redis，Key 格式 `rate_limit:{路径}:{IP}` | ✅ |
| Redis 不可用时降级放行，主业务不受影响 | ✅ |
| 前端收到限流响应有提示 | ✅ |
| 后端编译通过 | ✅ |
