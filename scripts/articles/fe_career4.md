# 团队协作与敏捷开发

软件开发是一项团队运动。掌握敏捷开发方法和团队协作技巧，能让你在团队中发挥更大的价值。本文将分享 Scrum 框架、看板方法、持续集成文化等实践经验。

## Scrum 框架

Scrum 是最流行的敏捷开发框架，以迭代（Sprint）为核心，通过一系列仪式和角色来管理软件开发过程。

### Scrum 的三个角色

| 角色 | 职责 |
|------|------|
| Product Owner（产品负责人） | 管理产品待办列表，确定优先级，代表业务方利益 |
| Scrum Master | 保障 Scrum 流程执行，移除障碍，促进团队协作 |
| Development Team（开发团队） | 跨职能团队，自组织完成开发工作 |

### Scrum 的五个仪式

**1. Sprint（冲刺）**

```
时间周期：1-4 周（通常 2 周）
特点：
- 固定时间盒，不延长
- 每个 Sprint 产生可交付的增量
- Sprint 目标在 Sprint 开始时确定
```

**2. Sprint 计划会（Sprint Planning）**

```
时间：Sprint 开始时，通常 2-4 小时（2周 Sprint）
参与人员：整个 Scrum 团队

议程：
1. Product Owner 介绍 Sprint 目标和优先级最高的用户故事
2. 团队评估故事点和工作量
3. 团队确定本次 Sprint 能完成的工作
4. 将用户故事拆分为具体任务

输出：
- Sprint 待办列表（Sprint Backlog）
- Sprint 目标
```

**3. 每日站会（Daily Standup）**

```
时间：每天固定时间，15 分钟以内
参与人员：开发团队（PO 和 SM 可选参加）

三个问题：
1. 昨天完成了什么？
2. 今天计划做什么？
3. 有什么阻碍？

注意：
- 不是进度汇报会，是团队同步会
- 问题要具体，不要泛泛而谈
- 遇到阻碍及时暴露，会后解决
```

**4. Sprint 评审会（Sprint Review）**

```
时间：Sprint 结束时，1-2 小时
参与人员：整个 Scrum 团队 + 利益相关者

内容：
1. 演示本次 Sprint 完成的功能
2. 收集利益相关者的反馈
3. Product Owner 更新产品待办列表
```

**5. Sprint 回顾会（Sprint Retrospective）**

```
时间：Sprint 评审会之后，1-1.5 小时
参与人员：Scrum 团队

流程：
1. 什么做得好？（继续保持）
2. 什么做得不好？（需要改进）
3. 下个 Sprint 要尝试什么改进？

输出：具体的改进行动项
```

### 用户故事

用户故事是敏捷开发中描述需求的基本单位：

```markdown
作为 <角色>
我想要 <功能>
以便 <商业价值>

示例：
作为一名注册用户
我想要重置我的密码
以便在忘记密码时能够重新登录系统
```

### 验收标准（Acceptance Criteria）

```markdown
用户故事：用户注册

验收标准：
- [ ] 用户输入用户名、密码、邮箱后可以注册
- [ ] 用户名长度 3-20 个字符
- [ ] 密码长度不少于 8 位，必须包含字母和数字
- [ ] 邮箱格式必须正确
- [ ] 用户名不能重复
- [ ] 注册成功后发送验证邮件
- [ ] 注册成功后自动登录
```

### 故事点估算

故事点是衡量用户故事工作量的相对单位，常用 Fibonacci 数列：

```
1, 2, 3, 5, 8, 13, 21, 34...

1  - 非常简单，几小时就能完成
2  - 简单，一天左右
3  - 中等，一两天
5  - 较复杂，两三天
8  - 复杂，可能需要一周
13 - 非常复杂，需要拆分
21 - 太大了，必须拆分
```

**Planning Poker 估算流程**：

```
1. Product Owner 讲解用户故事
2. 团队提问澄清
3. 每人选择一张牌（不展示）
4. 同时翻牌
5. 估算差异大的人说明理由
6. 重复 3-5 直到达成一致
```

## 看板方法

看板是一种可视化的流程管理方法，强调持续流动和限制在制品。

### 看板板的基本结构

```
| 待办 | 开发中 | 代码评审 | 测试中 | 已完成 |
|------|--------|----------|--------|--------|
|      | ■ ■ ■  | ■ ■      | ■      | ■ ■ ■ ■|
| ■ ■  |        |          |        |        |
| ■    |        |          |        |        |

WIP 限制：
- 开发中：3
- 代码评审：2
- 测试中：2
```

### 看板的核心实践

1. **可视化工作流**：让所有人看到工作的状态
2. **限制在制品（WIP）**：限制每个阶段同时进行的工作数量
3. **管理流动**：关注工作的流动效率，减少等待
4. **明确流程规则**：定义什么条件可以进入下一阶段
5. **持续改进**：定期分析瓶颈并优化

### Scrum vs 看板

| 特性 | Scrum | 看板 |
|------|-------|------|
| 节奏 | 固定 Sprint | 持续流动 |
| 角色 | 固定三个角色 | 无固定角色 |
| 变更 | Sprint 内不变 | 随时可以变更 |
| 交付 | Sprint 末交付 | 持续交付 |
| 度量 | 速度（Velocity） | 周期时间（Cycle Time） |

## 持续集成文化

### 什么是持续集成

持续集成（CI）是一种开发实践，团队成员频繁地将代码集成到主分支，每次集成都通过自动化构建和测试来验证。

```
开发者提交代码
      ↓
自动触发 CI 流水线
      ↓
┌─────────────────────────────┐
│ 1. 代码检出                 │
│ 2. 依赖下载                 │
│ 3. 编译构建                 │
│ 4. 单元测试                 │
│ 5. 代码质量检查             │
│ 6. 集成测试                 │
│ 7. 构建制品                 │
└─────────────────────────────┘
      ↓
  测试通过？ → 是 → 自动部署到测试环境
      ↓
    否 → 通知开发者修复
```

### CI 的核心原则

```
1. 频繁提交：每天至少提交一次代码
2. 快速反馈：构建时间控制在 10 分钟以内
3. 修复优先：构建失败后立即修复
4. 自动化一切：构建、测试、部署都自动化
5. 保持主分支健康：主分支始终处于可部署状态
```

### GitHub Actions CI 示例

```yaml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with Maven
        run: ./mvnw --batch-mode clean package

      - name: Run tests
        run: ./mvnw --batch-mode test

      - name: Code coverage
        run: ./mvnw jacoco:report

      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

### 代码所有权

**强所有权**：

```
- 每个模块有明确的 Owner
- 修改别人的代码需要 Owner 审批
- 优点：代码质量有保障
- 缺点：知识孤岛，瓶颈
```

**集体所有权**：

```
- 任何人可以修改任何代码
- 通过代码评审保证质量
- 优点：知识共享，无瓶颈
- 缺点：可能缺乏责任感
```

**实践建议**：

```
采用混合模式：
- 核心模块有主要负责人
- 但鼓励跨模块贡献
- 通过代码评审保证质量
- 定期进行知识分享
```

## 结对编程

结对编程是两个人一起在一台电脑上工作的方式。

### 结对编程的模式

**驾驶员-导航员模式**：

```
驾驶员：写代码的人
导航员：审查代码、思考策略

工作流程：
1. 驾驶员写代码
2. 导航员实时审查
3. 导航员发现问题及时指出
4. 定期交换角色（每 30-60 分钟）
```

**弹力结对**：

```
两个人各有各的电脑
一起讨论设计和方案
各自实现自己负责的部分
定期同步和评审
```

### 结对编程的好处

```
1. 知识共享：两个人都能了解整个模块
2. 实时评审：问题在产生时就被发现
3. 提高专注度：有人看着不容易分心
4. 技能提升：初级开发者可以快速学习
5. 减少巴士因子：至少两个人了解每个模块
```

### 结对编程的适用场景

```
适合：
- 复杂的技术问题
- 新人入职培训
- 关键功能开发
- 跨团队协作

不适合：
- 简单重复的工作
- 需要深度思考的任务
- 个人偏好不同太大的情况
```

## 技术债务管理

### 什么是技术债务

技术债务是软件开发中为了快速交付而做出的权宜之计，这些选择会在未来增加维护成本。

```java
// 技术债务示例

// 债务1：硬编码的配置
String dbUrl = "jdbc:mysql://localhost:3306/prod";  // 应该放在配置文件

// 债务2：缺少错误处理
public void process() {
    String data = fetchData();  // 可能返回 null
    int value = Integer.parseInt(data);  // 可能抛异常
    save(value);
}

// 债务3：重复代码
public void sendEmail(User user) {
    // 50 行邮件发送逻辑
}

public void sendNotification(User user) {
    // 50 行几乎相同的逻辑
}

// 债务4：过时的依赖
// pom.xml 中还在用 Spring Boot 2.x
```

### 技术债务的分类

| 类型 | 描述 | 优先级 |
|------|------|--------|
| 故意且谨慎 | "我们知道这不好，但先上线再说" | 中 |
| 故意且鲁莽 | "没时间写测试了" | 高 |
| 不谨慎且谨慎 | "原来这样做更好" | 低 |
| 不谨慎且鲁莽 | "什么是设计模式？" | 高 |

### 技术债务管理策略

**1. 识别和记录**

```markdown
## 技术债务清单

### TD-001: 用户服务缺少单元测试
- 严重程度：高
- 影响范围：用户服务模块
- 估计工时：3天
- 优先级：P1

### TD-002: 订单查询使用了 N+1 查询
- 严重程度：中
- 影响范围：订单列表页面
- 估计工时：1天
- 优先级：P2
```

**2. 定期偿还**

```
每个 Sprint 分配 15-20% 的时间处理技术债务

Sprint 规划：
- 功能开发：40 故事点
- 技术债务：10 故事点
- 总计：50 故事点
```

**3. 重构原则**

```java
// 重构前
public void processOrder(Order order) {
    // 验证
    if (order == null) throw new IllegalArgumentException();
    if (order.getItems() == null || order.getItems().isEmpty()) throw new IllegalArgumentException();
    if (order.getUser() == null) throw new IllegalArgumentException();

    // 计算总价
    double total = 0;
    for (Item item : order.getItems()) {
        total += item.getPrice() * item.getQuantity();
        if (item.getQuantity() > 10) {
            total *= 0.9;  // 打9折
        }
    }

    // 保存
    order.setTotal(total);
    order.setStatus("PAID");
    orderRepository.save(order);

    // 发送通知
    emailService.send(order.getUser().getEmail(), "订单确认", "...");
    smsService.send(order.getUser().getPhone(), "...");
}

// 重构后
public void processOrder(Order order) {
    validateOrder(order);
    BigDecimal total = calculateTotal(order);
    completePayment(order, total);
    sendNotifications(order);
}

private void validateOrder(Order order) {
    Objects.requireNonNull(order, "订单不能为空");
    Preconditions.checkArgument(
        order.getItems() != null && !order.getItems().isEmpty(),
        "订单项不能为空");
    Preconditions.checkNotNull(order.getUser(), "用户不能为空");
}

private BigDecimal calculateTotal(Order order) {
    return order.getItems().stream()
        .map(item -> {
            BigDecimal price = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return item.getQuantity() > 10 ? price.multiply(BigDecimal.valueOf(0.9)) : price;
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

## 团队协作最佳实践

### 沟通方式选择

| 场景 | 推荐方式 |
|------|----------|
| 紧急问题 | 电话/即时消息 |
| 技术讨论 | 面对面/视频会议 |
| 任务跟踪 | 项目管理工具 |
| 知识沉淀 | 文档/Wiki |
| 代码讨论 | Pull Request 评论 |

### 会议效率

```
高效会议的原则：
1. 明确目的：这个会议要解决什么问题？
2. 限定时间：会议时间不超过 1 小时
3. 提前准备：参会者提前了解议题
4. 有主持人：控制讨论节奏
5. 有结论：会议结束要有明确的行动项
6. 有跟进：行动项有负责人和截止日期
```

### 冲突处理

```
技术冲突的处理方式：
1. 数据说话：用性能测试、用户数据支撑观点
2. 小规模试验：先做一个 PoC 验证
3. 时间盒限定：讨论 30 分钟没有结论就搁置
4. 架构决策记录（ADR）：记录决策过程和理由
5. 请教前辈：必要时寻求更有经验的人的意见
```

敏捷不是银弹，没有一套方法能适合所有团队。关键是理解敏捷的价值观和原则，根据团队的实际情况进行调整和改进。持续反馈、持续改进，才是敏捷的核心。
