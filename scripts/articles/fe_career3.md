# 代码沟通与技术写作

代码是写给人看的，只是顺便让机器执行。良好的代码沟通能力不仅能让你的代码更易维护，还能提升团队协作效率。本文将分享代码命名、注释、文档编写和技术写作的最佳实践。

## 命名的艺术

好的命名是代码自文档化的基础。

### 命名的基本原则

**1. 名字应该表达意图**

```java
// 差：看不出来要做什么
int d;  // 消逝的时间（天）

// 好：意图清晰
int elapsedTimeInDays;
int daysSinceCreation;
```

**2. 避免误导性命名**

```java
// 差：accountList 暗示是 List 类型，但实际可能是 Set
Set<Account> accountList;

// 好：使用更通用的名称
Set<Account> accounts;
Set<Account> accountGroup;
```

**3. 做有意义的区分**

```java
// 差：数字编号区分
public static void copyChars(char a1[], char a2[]) {}

// 好：有意义的参数名
public static void copyChars(char[] source, char[] target) {}
```

**4. 使用可搜索的名字**

```java
// 工：很难搜索
for (int j = 0; j < 34; j++) {
    s += (t[j] * 4) / 5;
}

// 好：名字可以被搜索
int realDaysPerIdealDay = 4;
int workDaysPerWeek = 5;
int numWeeks = 34;

for (int j = 0; j < numWeeks; j++) {
    realTaskEstimate += (taskEstimate[j] * realDaysPerIdealDay) / workDaysPerWeek;
}
```

### 类命名

```java
// 名词或名词短语
public class UserAccount {}
public class OrderProcessor {}
public class DatabaseConnectionPool {}

// 避免使用 Manager、Processor、Handler 等模糊词汇（除非真的合适）
// 避免使用 Info、Data、Object 等无意义后缀
```

### 方法命名

```java
// 动词或动词短语
public void saveUser(User user) {}
public User findUserById(Long id) {}
public boolean isValidEmail(String email) {}
public int calculateTotalPrice(List<Item> items) {}

// getter/setter
public String getName() {}
public void setName(String name) {}

// 布尔返回值
public boolean hasPermission() {}
public boolean isEmpty() {}
public boolean canExecute() {}
```

### 常量命名

```java
// 全大写，下划线分隔
public static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_ENCODING = "UTF-8";
public static final long TIMEOUT_IN_MILLIS = 5000L;

// 避免魔法数字
// 差
if (status == 3) { ... }

// 好
private static final int ORDER_STATUS_CANCELLED = 3;
if (status == ORDER_STATUS_CANCELLED) { ... }
```

## 注释的最佳实践

注释应该解释"为什么"，而不是"是什么"。

### 好的注释

```java
// 法律注释
/*
 * Copyright (c) 2024, Example Corp. All rights reserved.
 */

// 意图注释
// 我们用二分查找是因为数据量可能很大（>100万）
// 线性查找在这种规模下会超时
int index = binarySearch(sortedArray, target);

// 警告注释
// 不要删除这行！这是为了兼容旧版本客户端的协议
String legacyFormat = convertToLegacyFormat(data);

// TODO 注释
// TODO: 2024-12-31 之前完成迁移，届时可以删除这个兼容逻辑
if (useLegacyApi) {
    return legacyService.process(request);
}

// 解释正则表达式
// 匹配中国大陆手机号：1开头，第二位是3-9，后面9位数字
Pattern phonePattern = Pattern.compile("^1[3-9]\\d{9}$");

// 解释复杂算法的原理
// 使用 KMP 算法进行字符串匹配，时间复杂度 O(n+m)
// 当模式串很长且需要多次匹配时，比暴力匹配高效
int position = kmpSearch(text, pattern);
```

### 差的注释

```java
// 差：注释只是重复代码
// 获取用户名称
String userName = user.getName();

// 差：被注释掉的代码
// User user = userService.findById(id);
// if (user != null) {
//     return user.getName();
// }

// 差：日志式注释
// 2024-01-15 张三 修改了这个方法
// 2024-02-20 李四 添加了参数验证

// 差：废话注释
// 构造函数
public UserService(UserRepository repository) {
    this.repository = repository;
}
```

### 注释的黄金法则

```
好的代码 > 差的代码 + 好的注释
但好的代码 + 好的注释 > 好的代码

注释不能弥补差的代码，如果你发现自己需要写注释来解释代码，
考虑重写代码让它更清晰。
```

## README 编写

README 是项目的门面，好的 README 能让用户快速了解和使用你的项目。

### README 模板

```markdown
# 项目名称

一句话描述项目是什么、做什么用的。

## 功能特性

- 特性1：简要说明
- 特性2：简要说明
- 特性3：简要说明

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+

### 安装

```bash
git clone https://github.com/example/project.git
cd project
mvn clean install
```

### 配置

复制配置模板并填入你的配置：

```bash
cp application-example.yml application.yml
```

### 运行

```bash
mvn spring-boot:run
```

访问 http://localhost:8080 查看效果。

## 使用示例

### 基本用法

```java
UserService userService = new UserService();
User user = userService.findById(1L);
System.out.println(user.getName());
```

### 高级用法

```java
// 更复杂的使用示例...
```

## API 文档

启动应用后访问 http://localhost:8080/doc.html 查看 API 文档。

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/example/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       └── model/
│   └── resources/
│       ├── application.yml
│       └── db/migration/
└── test/
```

## 贡献指南

欢迎贡献！请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE)。
```

## API 文档

### Swagger/OpenAPI 注解

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户增删改查接口")
public class UserController {

    @Operation(summary = "根据ID查询用户", description = "返回指定ID的用户信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/{id}")
    public User getUser(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long id) {
        return userService.findById(id);
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public User createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }
}

@Data
@Schema(description = "创建用户请求")
public class CreateUserRequest {

    @Schema(description = "用户名", example = "zhangsan", required = true)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

## 技术方案文档

### 技术方案模板

```markdown
# 技术方案：XX 功能

## 1. 背景与目标

### 1.1 业务背景
描述为什么要做这个功能，解决什么业务问题。

### 1.2 技术目标
- 目标1
- 目标2

## 2. 现状分析

### 2.1 现有架构
描述当前的系统架构和存在的问题。

### 2.2 问题与挑战
- 问题1
- 问题2

## 3. 方案设计

### 3.1 整体架构

```
[架构图]
```

### 3.2 详细设计

#### 3.2.1 数据模型

```sql
CREATE TABLE new_feature (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### 3.2.2 接口设计

```java
POST /api/v1/feature
Request: { "name": "test" }
Response: { "id": 1, "name": "test" }
```

#### 3.2.3 核心流程

```
[流程图]
```

### 3.3 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| 消息队列 | RocketMQ | 团队熟悉度高 |
| 缓存 | Redis | 支持丰富数据结构 |

## 4. 影响评估

### 4.1 性能影响
- 预计增加数据库 QPS：500
- 预计增加 Redis 内存：2GB

### 4.2 兼容性
- 数据库需要新增表
- 需要修改现有接口

## 5. 测试计划

### 5.1 单元测试
- 核心业务逻辑覆盖率 > 80%

### 5.2 集成测试
- 接口联调测试
- 性能压测

## 6. 上线计划

### 6.1 灰度策略
- 第一周：5% 流量
- 第二周：30% 流量
- 第三周：100% 流量

### 6.2 回滚方案
- 关闭功能开关
- 回滚数据库变更

## 7. 排期

| 阶段 | 时间 | 负责人 |
|------|------|--------|
| 设计评审 | W1 | 张三 |
| 开发 | W2-W3 | 李四 |
| 测试 | W4 | 王五 |
| 上线 | W5 | 赵六 |
```

## 代码评审中的沟通

### 评审原则

```
1. 对事不对人：关注代码，不是写代码的人
2. 提问而非命令："这里为什么用 static？" 而不是 "去掉 static"
3. 给出理由：不仅说要改，还要说为什么
4. 肯定好的地方：不要只挑毛病
5. 区分严重程度：必须改 / 建议改 / 可选优化
```

### 评审评论模板

```markdown
**必须修改**：这里会导致 NPE，需要加空判断。

**建议修改**：这个方法超过 80 行了，建议拆分成几个小方法，提高可读性。

**疑问**：这里为什么选择用 HashMap 而不是 ConcurrentHashMap？是有特殊的并发考虑吗？

**肯定**：这个设计很好，职责划分清晰，后续扩展很方便。

**建议**：考虑用 Builder 模式来构建这个对象，参数比较多。
```

## 技术博客写作技巧

### 文章结构

```
1. 引言（100-200字）
   - 问题是什么
   - 为什么重要

2. 背景知识（200-500字）
   - 必要的概念解释
   - 前置知识

3. 核心内容（1000-2000字）
   - 问题分析
   - 解决方案
   - 代码示例

4. 实战案例（300-500字）
   - 真实场景
   - 踩坑经验

5. 总结（100-200字）
   - 要点回顾
   - 最佳实践
```

### 写作技巧

1. **标题要具体**：不要"Java 学习笔记"，要"深入理解 HashMap 的扩容机制"
2. **开头要抓人**：用一个实际问题或有趣的例子开头
3. **代码要完整**：给出可运行的代码，而不是片段
4. **图文并茂**：流程图、架构图能让复杂概念更易理解
5. **加入自己的思考**：不要只是搬运文档，要有自己的理解和实践

## 开源贡献指南

### 如何提交 Issue

```markdown
## Bug 报告

### 环境信息
- JDK 版本：21
- 操作系统：Windows 11
- 框架版本：Spring Boot 3.2.0

### 问题描述
简要描述问题。

### 复现步骤
1. 执行 xxx
2. 调用 xxx 接口
3. 观察到 xxx

### 期望行为
描述你期望的正确行为。

### 实际行为
描述实际发生的行为。

### 日志/截图
粘贴相关日志或截图。
```

### 如何提交 PR

```markdown
## 变更说明

### 目的
这个 PR 解决了什么问题。

### 变更内容
- 变更1
- 变更2

### 测试
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 手动测试通过

### 关联 Issue
Closes #123
```

良好的代码沟通和文档能力是高级工程师的必备技能。代码会过时，但好的文档和知识沉淀会持续产生价值。投资在文档上的时间，会在未来以更高的效率回报给你。
