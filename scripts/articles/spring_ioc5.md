# 事件机制与 SpEL 表达式

Spring 框架提供了强大的事件机制和表达式语言，它们是构建松耦合、可扩展应用的重要基础设施。事件机制实现了观察者模式，允许应用组件之间通过发布-订阅进行通信；SpEL（Spring Expression Language）则提供了在运行时动态操作对象图的能力。本文将深入探讨这两个核心特性。

## Spring 事件模型

Spring 事件模型基于观察者模式，包含三个核心角色：事件（Event）、事件发布者（Publisher）和事件监听者（Listener）。

### 核心接口

| 接口/注解 | 说明 |
|-----------|------|
| `ApplicationEvent` | 事件基类，所有自定义事件需继承此类 |
| `ApplicationListener<E>` | 事件监听器接口，泛型指定监听的事件类型 |
| `ApplicationEventPublisher` | 事件发布者接口，通常通过注入使用 |
| `@EventListener` | 声明式事件监听注解（Spring 4.2+） |

### ApplicationEvent

`ApplicationEvent` 是所有应用事件的抽象基类，继承自 `java.util.EventObject`。

```java
public abstract class ApplicationEvent extends EventObject {

    private final long timestamp;

    public ApplicationEvent(Object source) {
        super(source);
        this.timestamp = System.currentTimeMillis();
    }

    public final long getTimestamp() {
        return this.timestamp;
    }
}
```

### ApplicationListener

`ApplicationListener` 接口用于监听特定类型的事件：

```java
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> {
    void onApplicationEvent(E event);
}
```

### ApplicationEventPublisher

事件发布者接口提供了发布事件的方法：

```java
@FunctionalInterface
public interface ApplicationEventPublisher {
    void publishEvent(ApplicationEvent event);
    void publishEvent(Object event);  // Spring 4.2+，自动包装为 PayloadApplicationEvent
}
```

## 内置事件

Spring 容器在生命周期的关键节点会发布内置事件，开发者可以监听这些事件执行自定义逻辑。

### 容器生命周期事件

| 事件 | 触发时机 |
|------|----------|
| `ContextRefreshedEvent` | 容器初始化完成或刷新时 |
| `ContextStartedEvent` | 容器启动时（调用 `start()` 方法） |
| `ContextStoppedEvent` | 容器停止时（调用 `stop()` 方法） |
| `ContextClosedEvent` | 容器关闭时 |
| `RequestHandledEvent` | HTTP 请求处理完成时（Web 应用） |

### 监听内置事件

```java
@Component
public class ApplicationStartupListener {

    @EventListener
    public void onApplicationReady(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        System.out.println("容器初始化完成，共加载 " + 
            context.getBeanDefinitionCount() + " 个 Bean");
    }

    @EventListener
    public void onApplicationShutdown(ContextClosedEvent event) {
        System.out.println("应用正在关闭，执行清理操作...");
    }
}
```

### Servlet Web 事件

在 Spring MVC 中，还有额外的 Web 相关事件：

```java
@Component
public class RequestEventListener {

    @EventListener
    public void onRequestHandled(RequestHandledEvent event) {
        System.out.println("请求处理完成: " + event.getDescription());
        System.out.println("处理耗时: " + event.getProcessingTimeMillis() + "ms");
    }
}
```

## 自定义事件

自定义事件允许应用组件之间进行解耦通信。

### 定义事件类

```java
public class OrderCreatedEvent extends ApplicationEvent {

    private final String orderId;
    private final String customerId;
    private final BigDecimal amount;

    public OrderCreatedEvent(Object source, String orderId, 
                             String customerId, BigDecimal amount) {
        super(source);
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
    }

    // getters
}
```

### 发布事件

通过注入 `ApplicationEventPublisher` 发布事件：

```java
@Service
public class OrderService {

    private final ApplicationEventPublisher eventPublisher;

    public OrderService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order(request);
        orderRepository.save(order);

        // 发布订单创建事件
        eventPublisher.publishEvent(new OrderCreatedEvent(
            this, order.getId(), order.getCustomerId(), order.getAmount()
        ));

        return order;
    }
}
```

### 监听事件

```java
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("订单创建事件: orderId={}, amount={}", 
                 event.getOrderId(), event.getAmount());
        // 执行后续业务逻辑
    }
}
```

## @EventListener 条件事件

Spring 4.2+ 的 `@EventListener` 注解提供了强大的事件处理能力。

### condition 属性

使用 SpEL 表达式定义监听条件：

```java
@Component
public class ConditionalEventListener {

    @EventListener(condition = "#event.amount > 1000")
    public void handleLargeOrder(OrderCreatedEvent event) {
        System.out.println("大额订单: " + event.getOrderId() + 
                         ", 金额: " + event.getAmount());
    }

    @EventListener(condition = "#event.customerId.startsWith('VIP')")
    public void handleVipOrder(OrderCreatedEvent event) {
        System.out.println("VIP 客户订单: " + event.getOrderId());
    }

    @EventListener(condition = "@featureFlags.isEnabled('order-notification')")
    public void handleOrderNotification(OrderCreatedEvent event) {
        // 仅当特性开关启用时执行
    }
}
```

### 条件表达式中的可用变量

| 变量 | 说明 |
|------|------|
| `#root` | 事件对象本身 |
| `#event` | 事件对象（需方法参数名为 event） |
| `#beanFactory` | BeanFactory 引用 |
| `@beanName` | 引用容器中的 Bean |

### 多事件监听

一个监听方法可以监听多种事件：

```java
@Component
public class MultiEventListener {

    @EventListener({OrderCreatedEvent.class, OrderUpdatedEvent.class})
    public void handleOrderChange(ApplicationEvent event) {
        if (event instanceof OrderCreatedEvent) {
            // 处理订单创建
        } else if (event instanceof OrderUpdatedEvent) {
            // 处理订单更新
        }
    }
}
```

### 事件排序

使用 `@Order` 注解控制监听器的执行顺序：

```java
@Component
public class OrderedEventListener {

    @EventListener
    @Order(1)  // 最先执行
    public void handleFirst(OrderCreatedEvent event) {
        System.out.println("第一个监听器执行");
    }

    @EventListener
    @Order(2)  // 其次执行
    public void handleSecond(OrderCreatedEvent event) {
        System.out.println("第二个监听器执行");
    }
}
```

## @Async 异步事件

默认情况下，事件监听器是同步执行的。通过 `@Async` 注解可以实现异步事件处理。

### 启用异步支持

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("eventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.initialize();
        return executor;
    }
}
```

### 异步监听器

```java
@Component
public class AsyncEventListener {

    @Async("eventExecutor")
    @EventListener
    public void handleOrderCreatedAsync(OrderCreatedEvent event) {
        // 异步执行，不会阻塞事件发布者
        System.out.println("异步处理订单事件: " + event.getOrderId());
        // 可能涉及耗时操作，如发送邮件、短信等
    }
}
```

### 异步事件的注意事项

1. **异常处理**：异步事件的异常不会传播到事件发布者
2. **事务边界**：异步监听器在独立的线程中执行，不在发布者的事务上下文中
3. **返回值**：异步监听器不支持返回值（返回类型应为 void 或 Future）

## 事务事件：@TransactionalEventListener

Spring 4.2 引入了事务事件监听器，允许在事务的特定阶段处理事件。

### 事务阶段

| 阶段 | 说明 |
|------|------|
| `AFTER_COMMIT` | 事务提交后执行（默认） |
| `AFTER_ROLLBACK` | 事务回滚后执行 |
| `AFTER_COMPLETION` | 事务完成后执行（无论提交或回滚） |
| `BEFORE_COMMIT` | 事务提交前执行 |

### 使用示例

```java
@Component
public class TransactionalEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedAfterCommit(OrderCreatedEvent event) {
        // 仅在事务成功提交后执行
        System.out.println("订单已持久化: " + event.getOrderId());
        // 发送确认邮件、更新统计等
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleOrderCreationFailed(OrderCreatedEvent event) {
        // 事务回滚后执行
        System.out.println("订单创建失败，执行补偿操作: " + event.getOrderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleBeforeCommit(OrderCreatedEvent event) {
        // 事务提交前执行
        System.out.println("事务即将提交: " + event.getOrderId());
    }
}
```

### fallbackExecution 属性

默认情况下，如果没有活跃的事务，事务事件监听器不会执行。设置 `fallbackExecution = true` 可以在无事务时也执行：

```java
@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT,
    fallbackExecution = true  // 无事务时也执行
)
public void handleWithFallback(OrderCreatedEvent event) {
    // 如果有事务，在事务提交后执行；否则直接执行
}
```

### 事件传播与事务上下文

```java
@Service
public class OrderService {

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order(request);
        orderRepository.save(order);

        // 发布事件，但此时事务尚未提交
        eventPublisher.publishEvent(new OrderCreatedEvent(this, order.getId()));

        return order;
    }
}

@Component
public class EmailEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendConfirmationEmail(OrderCreatedEvent event) {
        // 事务提交后发送邮件，确保订单已持久化
        emailService.sendOrderConfirmation(event.getOrderId());
    }
}
```

## SpEL（Spring Expression Language）概述

SpEL 是一种强大的表达式语言，支持在运行时查询和操作对象图。它可以在 XML 配置、注解和编程中使用。

### SpEL 的核心特性

1. **字面量表达式**：字符串、数字、布尔值
2. **属性访问**：直接访问对象属性
3. **方法调用**：调用对象方法
4. **运算符**：算术、关系、逻辑运算
5. **正则表达式**：模式匹配
6. **集合操作**：投影、过滤、选择
7. **模板表达式**：`#{}` 占位符

### ExpressionParser

SpEL 的核心接口，用于解析和计算表达式：

```java
ExpressionParser parser = new SpelExpressionParser();

// 解析字面量
Expression exp = parser.parseExpression("'Hello World'");
String message = (String) exp.getValue();  // "Hello World"

// 解析属性
exp = parser.parseExpression("name");
String name = (String) exp.getValue(myObject);  // 访问 myObject.getName()
```

## SpEL 语法详解

### 字面量

```java
// 字符串
parser.parseExpression("'Hello'").getValue(String.class);

// 数字
parser.parseExpression("42").getValue(Integer.class);
parser.parseExpression("3.14").getValue(Double.class);

// 布尔值
parser.parseExpression("true").getValue(Boolean.class);

// null
parser.parseExpression("null").getValue();
```

### Bean 引用

```java
// 引用容器中的 Bean（使用 @ 前缀）
parser.parseExpression("@userService").getValue();

// 引用 BeanFactory
parser.parseExpression("&userService").getValue();
```

### 属性访问与方法调用

```java
// 嵌套属性访问
parser.parseExpression("user.address.city").getValue(context);

// 方法调用
parser.parseExpression("name.toUpperCase()").getValue(context);

// 链式调用
parser.parseExpression("users.?[active].size()").getValue(context);
```

### 运算符

```java
// 算术运算符
parser.parseExpression("2 + 3").getValue(Integer.class);  // 5
parser.parseExpression("10 / 3").getValue(Integer.class);  // 3
parser.parseExpression("10 % 3").getValue(Integer.class);  // 1

// 关系运算符
parser.parseExpression("2 > 1").getValue(Boolean.class);  // true
parser.parseExpression("2 == 2").getValue(Boolean.class);  // true

// 逻辑运算符
parser.parseExpression("true and false").getValue(Boolean.class);  // false
parser.parseExpression("true or false").getValue(Boolean.class);   // true
parser.parseExpression("!true").getValue(Boolean.class);           // false

// 三元运算符
parser.parseExpression("2 > 1 ? 'yes' : 'no'").getValue(String.class);  // "yes"
```

### 正则表达式

```java
parser.parseExpression("'12345' matches '\\d{5}'").getValue(Boolean.class);  // true
parser.parseExpression("'abc' matches '\\d+'").getValue(Boolean.class);      // false
```

## SpEL 在 @Value 中的使用

### #{...} 与 ${...} 的区别

| 语法 | 说明 | 示例 |
|------|------|------|
| `${...}` | 属性占位符，从配置文件读取值 | `${app.name}` |
| `#{...}` | SpEL 表达式，支持复杂计算 | `#{systemProperties['user.home']}` |

### 使用示例

```java
@Component
public class SpelConfig {

    // 从配置文件读取
    @Value("${app.name}")
    private String appName;

    // SpEL 表达式
    @Value("#{systemProperties['user.home']}")
    private String userHome;

    // 混合使用
    @Value("#{ '${app.name}' + ' v' + '${app.version}' }")
    private String fullAppName;

    // 默认值
    @Value("${app.unknown:defaultValue}")
    private String withDefault;

    // 复杂表达式
    @Value("#{T(java.lang.Math).random() * 100}")
    private double randomValue;

    // Bean 属性访问
    @Value("#{@dataSourceProperties.url}")
    private String dataSourceUrl;

    // 条件表达式
    @Value("#{ '${app.env}' == 'prod' ? 'production-server' : 'localhost' }")
    private String serverHost;

    // 集合操作
    @Value("#{{'a', 'b', 'c'}}")
    private List<String> letters;

    // 数组访问
    @Value("#{{'a', 'b', 'c'}[1]}")
    private String secondLetter;
}
```

### @Value 与 @ConfigurationProperties 的选择

| 特性 | @Value | @ConfigurationProperties |
|------|--------|-------------------------|
| 松散绑定 | 不支持 | 支持 |
| SpEL | 支持 | 不支持 |
| 数据校验 | 不支持 | 支持（@Validated） |
| 复杂类型 | 有限支持 | 完全支持 |
| 元数据 | 无 | 支持生成配置元数据 |

## SpEL 集合操作

SpEL 提供了强大的集合操作能力。

### 投影 .![]

投影（Projection）用于从集合中提取特定属性，生成新集合：

```java
// 提取所有用户的姓名
parser.parseExpression("users.![name]").getValue(context);  // List<String>

// 提取所有订单的金额
parser.parseExpression("orders.![amount]").getValue(context);  // List<BigDecimal>

// 复杂投影
parser.parseExpression("users.![name + ' (' + age + ')']").getValue(context);
```

### 过滤 .?[]

过滤（Selection）用于从集合中筛选满足条件的元素：

```java
// 筛选活跃用户
parser.parseExpression("users.?[active == true]").getValue(context);

// 筛选金额大于100的订单
parser.parseExpression("orders.?[amount > 100]").getValue(context);

// 筛选 VIP 用户
parser.parseExpression("users.?[name.startsWith('VIP')]").getValue(context);
```

### 选择 .^[] 和 .$[]

从过滤结果中选择第一个或最后一个元素：

```java
// 第一个活跃用户
parser.parseExpression("users.^[active == true]").getValue(context);

// 最后一个活跃用户
parser.parseExpression("users.$[active == true]").getValue(context);
```

### 集合操作的组合使用

```java
// 获取所有活跃用户的姓名列表
parser.parseExpression("users.?[active].![name]").getValue(context);

// 获取金额最大的订单
parser.parseExpression("orders.^[amount == orders.![amount].max()]").getValue(context);

// 计算所有订单的总金额
parser.parseExpression("orders.![amount].![doubleValue()].sum()").getValue(context);
```

## 实际应用：动态配置与权限表达式

### 动态配置

结合 SpEL 实现运行时动态配置：

```java
@Configuration
public class DynamicConfig {

    @Value("#{${app.cache.ttl:{'default':300}}}")
    private Map<String, Integer> cacheTtlMap;

    @Value("#{'${app.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Value("#{${app.feature-flags:{}}}")
    private Map<String, Boolean> featureFlags;
}
```

### 安全权限表达式

Spring Security 大量使用 SpEL 进行权限控制：

```java
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() {
    // 仅管理员可访问
}

@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public void adminOrOwnerMethod(Long userId) {
    // 管理员或资源所有者可访问
}

@PreAuthorize("@permissionChecker.hasPermission(authentication, #resourceId, 'READ')")
public Resource getResource(Long resourceId) {
    // 自定义权限检查
}

@PostAuthorize("returnObject.owner == authentication.name")
public Document getDocument(Long id) {
    // 方法执行后检查返回值
}

@PreFilter("filterObject.owner == authentication.name")
public void updateDocuments(List<Document> documents) {
    // 方法执行前过滤参数
}

@PostFilter("filterObject.owner == authentication.name")
public List<Document> getAllDocuments() {
    // 方法执行后过滤返回值
}
```

### 自定义 SpEL 函数

可以注册自定义函数扩展 SpEL：

```java
@Configuration
public class SpelConfig {

    @Bean
    public SpelFunctionRegistrar spelFunctionRegistrar() {
        SpelFunctionRegistrar registrar = new SpelFunctionRegistrar();
        registrar.registerFunction("mask", 
            (String s) -> s.replaceAll("(?<=.{3}).(?=.{4})", "*"));
        return registrar;
    }
}
```

使用自定义函数：

```java
@Value("#{mask('13812345678')}")
private String maskedPhone;  // "138****5678"
```

### 动态路由与消息

```java
@Component
public class DynamicRouter {

    @Value("#{${app.routes:{'default':'/api/v1'}}}")
    private Map<String, String> routeMap;

    @EventListener
    @EventListener(condition = "#event.priority == 'HIGH'")
    public void handleHighPriorityEvent(CustomEvent event) {
        // 仅处理高优先级事件
    }

    public String resolveRoute(String tenantId) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(
            "#tenantId == 'premium' ? '/api/v2' : '/api/v1'"
        );
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("tenantId", tenantId);
        return exp.getValue(context, String.class);
    }
}
```

Spring 事件机制和 SpEL 表达式是构建灵活、可扩展应用的重要工具。事件机制实现了组件间的松耦合通信，而 SpEL 提供了强大的运行时动态能力。掌握这两个特性，能够编写更加优雅和可维护的 Spring 应用。