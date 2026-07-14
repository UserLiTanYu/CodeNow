# 扩展功能二：操作日志（AOP 实现）— 操作日志

> 本功能基于 AOP 切面编程，自动记录管理员在后台的所有写操作，异步写入数据库，支持分页查询。

---

## 一、创建自定义注解 @OperationLog

文件：`codenow-backend/src/main/java/com/codenow/annotation/OperationLog.java`

- `@Target(ElementType.METHOD)` — 标注在方法上
- `@Retention(RetentionPolicy.RUNTIME)` — 运行时保留，AOP 可读取
- `value()` 属性 — 操作描述（如"发布文章"、"删除分类"）

---

## 二、创建 SysOperationLog 实体类

文件：`codenow-backend/src/main/java/com/codenow/entity/SysOperationLog.java`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键自增 |
| userId | Long | 操作人 ID |
| username | String | 操作人用户名 |
| operation | String | 操作描述 |
| method | String | 请求方法（类名.方法名） |
| params | String | 请求参数 JSON |
| ip | String | 请求 IP |
| duration | Integer | 耗时（毫秒） |
| status | Integer | 状态（0=失败, 1=成功） |
| createTime | LocalDateTime | 创建时间 |

---

## 三、创建 Mapper 和 Service

文件：
- `SysOperationLogMapper.java` — 继承 `BaseMapper<SysOperationLog>`
- `OperationLogService.java` — 接口，定义 `saveAsync()` 方法
- `OperationLogServiceImpl.java` — 实现类，`saveAsync()` 方法标注 `@Async` 异步执行

---

## 四、创建 AOP 切面

文件：`codenow-backend/src/main/java/com/codenow/aspect/OperationLogAspect.java`

核心逻辑：
1. `@Around("@annotation(com.codenow.annotation.OperationLog)")` 拦截带注解的方法
2. 记录开始时间，执行业务方法
3. 方法执行完毕后（无论成功失败），在 `finally` 块中组装日志对象：
   - 操作描述 — 从 `@OperationLog.value()` 获取
   - 方法名 — `类名.方法名`
   - 请求参数 — Jackson 序列化方法参数（排除 HttpServletRequest）
   - IP 地址 — 从 Request 获取（兼容 X-Forwarded-For 代理）
   - 操作人 — 从 Sa-Token Session 获取 userId 和 username
   - 耗时 — `System.currentTimeMillis()` 差值
   - 状态 — 方法正常执行为 1，抛异常为 0
4. 调用 `operationLogService.saveAsync()` 异步写入数据库

---

## 五、创建异步配置

文件：`codenow-backend/src/main/java/com/codenow/config/AsyncConfig.java`

- `@EnableAsync` 启用 Spring 异步支持
- 使用默认的 `SimpleAsyncTaskExecutor`（开发阶段够用）

---

## 六、新增依赖

文件：`codenow-backend/pom.xml`

新增 `spring-boot-starter-aop` 依赖（AOP 切面需要 AspectJ 支持）。

---

## 七、数据库建表

文件：`codenow-backend/init.sql` 新增 `sys_operation_log` 建表语句

同时在 MySQL 中执行建表（codenow 数据库）。

---

## 八、Controller 添加注解

| Controller | 方法 | 注解值 |
|-----------|------|--------|
| AuthController | login() | `@OperationLog("用户登录")` |
| AuthController | logout() | `@OperationLog("用户登出")` |
| ArticleController | save() | `@OperationLog("新增文章")` |
| ArticleController | update() | `@OperationLog("修改文章")` |
| ArticleController | delete() | `@OperationLog("删除文章")` |
| ArticleController | toggleStatus() | `@OperationLog("切换文章状态")` |
| ArticleController | toggleTop() | `@OperationLog("切换文章置顶")` |
| CategoryController | save() | `@OperationLog("新增分类")` |
| CategoryController | update() | `@OperationLog("修改分类")` |
| CategoryController | delete() | `@OperationLog("删除分类")` |
| TagController | save() | `@OperationLog("新增标签")` |
| TagController | delete() | `@OperationLog("删除标签")` |

额外改动：AuthController 登录成功后，将 username 存入 Sa-Token Session，供 AOP 获取操作人信息。

---

## 九、日志查询接口

文件：`codenow-backend/src/main/java/com/codenow/controller/OperationLogController.java`

| 接口 | 路径 | 说明 |
|------|------|------|
| GET | `/api/logs?pageNum=1&pageSize=10` | 分页查询操作日志，按时间倒序 |

---

## 改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `pom.xml` | 修改 | 新增 spring-boot-starter-aop |
| `annotation/OperationLog.java` | 新建 | 自定义注解 |
| `entity/SysOperationLog.java` | 新建 | 操作日志实体类 |
| `mapper/SysOperationLogMapper.java` | 新建 | Mapper 接口 |
| `service/OperationLogService.java` | 新建 | Service 接口（含 saveAsync） |
| `service/impl/OperationLogServiceImpl.java` | 新建 | Service 实现（@Async 异步写入） |
| `aspect/OperationLogAspect.java` | 新建 | AOP 切面（@Around 环绕通知） |
| `config/AsyncConfig.java` | 新建 | @EnableAsync 异步配置 |
| `controller/OperationLogController.java` | 新建 | 日志查询接口 |
| `controller/AuthController.java` | 修改 | 登录/登出加 @OperationLog + Session 存 username |
| `controller/ArticleController.java` | 修改 | 写操作加 @OperationLog |
| `controller/CategoryController.java` | 修改 | 写操作加 @OperationLog |
| `controller/TagController.java` | 修改 | 写操作加 @OperationLog |
| `init.sql` | 修改 | 新增 sys_operation_log 建表语句 |

---

## 验收结果

| 验收项 | 结果 |
|--------|------|
| @OperationLog 注解可标注在任意 Controller 方法 | ✅ |
| 执行操作后 sys_operation_log 自动插入记录 | ✅ |
| 日志写入使用 @Async 异步执行 | ✅ |
| GET /api/logs 分页查询日志接口 | ✅ |
| 覆盖管理端所有写操作（12 个方法） | ✅ |
| 后端编译通过 | ✅ |
