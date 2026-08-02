# 断言与日志

## 断言（Assertion）

### 基本语法

Java 使用 `assert` 关键字进行断言，用于在开发阶段验证程序的正确性：

```java
// 语法1：只判断条件
assert condition;

// 语法2：条件不满足时输出消息
assert condition : message;
```

实际使用示例：

```java
public int divide(int a, int b) {
    assert b != 0 : "除数不能为零";
    return a / b;
}

public void processArray(int[] arr) {
    assert arr != null : "数组不能为 null";
    assert arr.length > 0 : "数组不能为空";
    
    for (int i = 0; i < arr.length; i++) {
        // 处理元素
    }
}
```

### 启用断言

断言在运行时**默认关闭**，需要通过 JVM 参数显式启用：

```bash
# 启用所有断言
java -ea MainClass

# 启用特定包的断言
java -ea:com.example.service... MainClass

# 禁用特定包的断言
java -da:com.example.service... MainClass

# 同时启用和禁用
java -ea:com.example.service... -da:com.example.service.Test... MainClass
```

### 使用场景

断言适合用于**不应该发生**的情况，而不是处理业务逻辑中的可预见错误：

```java
// 适合使用断言的场景

// 1. 内部不变量
private int calculateDiscount(int price) {
    int discount = price / 10;
    assert discount >= 0 && discount <= 100 : "折扣率异常: " + discount;
    return discount;
}

// 2. 控制流不应该到达的位置
public String getDayName(int day) {
    switch (day) {
        case 1: return "周一";
        case 2: return "周二";
        // ...
        case 7: return "周日";
        default:
            assert false : "无效的日期: " + day;
            return "未知";
    }
}

// 3. 方法前置条件（私有方法）
private void internalProcess(Data data) {
    assert data != null : "data 不能为 null";
    assert data.isValid() : "data 状态无效";
    // 处理数据...
}

// 4. 方法后置条件
public List<User> findActiveUsers() {
    List<User> result = doFindActiveUsers();
    assert result != null : "返回值不能为 null";
    assert result.stream().allMatch(User::isActive) : "结果中包含非活跃用户";
    return result;
}
```

### 断言的局限性

| 局限性 | 说明 |
|--------|------|
| 生产环境默认关闭 | 断言代码不会执行，不能依赖断言保证程序正确性 |
| 不适合参数校验 | 公共 API 的参数校验应该使用异常 |
| 不能替代异常处理 | 断言用于发现编程错误，不是处理业务异常 |

```java
// 错误：用断言做参数校验
public void setAge(int age) {
    assert age >= 0 && age <= 150;  // 生产环境关闭后，校验失效
    this.age = age;
}

// 正确：用异常做参数校验
public void setAge(int age) {
    if (age < 0 || age > 150) {
        throw new IllegalArgumentException("年龄不合法: " + age);
    }
    this.age = age;
}
```

## Java 日志体系概述

### JUL（java.util.logging）

Java 标准库自带的日志框架，从 JDK 1.4 开始提供：

```java
import java.util.logging.Logger;
import java.util.logging.Level;

public class MyService {
    private static final Logger logger = Logger.getLogger(MyService.class.getName());

    public void doSomething() {
        logger.info("开始执行任务");
        
        try {
            // 业务逻辑
            logger.fine("任务执行细节");  // 低级别日志
        } catch (Exception e) {
            logger.log(Level.SEVERE, "任务执行失败", e);
        }
        
        logger.info("任务执行完成");
    }
}
```

JUL 核心组件：

| 组件 | 说明 |
|------|------|
| Logger | 日志记录器，用于记录日志 |
| Handler | 日志处理器，决定日志输出到哪里（控制台、文件等） |
| Level | 日志级别（SEVERE、WARNING、INFO、CONFIG、FINE、FINER、FINEST） |
| Formatter | 格式化器，决定日志的输出格式 |

### 日志框架演进

Java 日志框架经历了多次演进：

```
时间线：
2001  ──  Log4j 1.x（Apache，最早的流行日志框架）
2002  ──  JUL（JDK 1.4 内置）
2003  ──  JCL（Apache Commons Logging，日志门面）
2006  ──  SLF4J（Simple Logging Facade for Java，日志门面）
2006  ──  Logback（SLF4J 的原生实现）
2012  ──  Log4j 2（Apache，Log4j 1.x 的重写）
```

### SLF4J 作为统一门面

SLF4J（Simple Logging Facade for Java）是一个**日志门面**，提供统一的日志 API，底层可以切换不同的日志实现：

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    public void createOrder(Order order) {
        logger.info("创建订单: {}", order.getId());
        // ...
    }
}
```

SLF4J 的优势：

1. **解耦**：业务代码只依赖 SLF4J API，不绑定具体实现
2. **可切换**：可以在 Logback、Log4j2、JUL 之间切换，无需修改业务代码
3. **统一门面**：项目中不同库使用不同日志框架时，可以统一到 SLF4J

## Logback 基本使用

### 依赖配置

在 Maven 项目中添加 Logback 依赖：

```xml
<dependencies>
    <!-- SLF4J API -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    
    <!-- Logback 实现 -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.14</version>
    </dependency>
</dependencies>
```

**注意**：如果使用 Spring Boot，会自动引入 Logback，无需手动添加依赖。

### Logger 和 LoggerFactory

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    // 推荐方式：使用当前类作为 Logger 名称
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // 也可以使用字符串
    // private static final Logger logger = LoggerFactory.getLogger("UserService");

    public User findById(Long id) {
        logger.debug("查询用户, id={}", id);
        
        try {
            User user = userRepository.findById(id);
            if (user == null) {
                logger.warn("用户不存在, id={}", id);
            } else {
                logger.info("查询成功, userId={}, userName={}", user.getId(), user.getName());
            }
            return user;
        } catch (Exception e) {
            logger.error("查询用户失败, id={}", id, e);
            throw e;
        }
    }
}
```

### 日志级别

Logback 支持 5 个日志级别，从低到高：

| 级别 | 说明 | 使用场景 |
|------|------|----------|
| TRACE | 最详细的日志 | 开发调试时的详细追踪信息 |
| DEBUG | 调试信息 | 开发阶段的调试输出 |
| INFO | 一般信息 | 重要的业务流程节点 |
| WARN | 警告信息 | 不影响正常运行但需要注意的情况 |
| ERROR | 错误信息 | 异常和错误 |

日志级别可以设置，只有**高于等于**设定级别的日志才会被输出：

```xml
<configuration>
    <!-- 根日志级别设置为 INFO -->
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
    
    <!-- 特定包设置为 DEBUG -->
    <logger name="com.example.service" level="DEBUG" />
</configuration>
```

## 日志格式化

### 常用格式化占位符

| 占位符 | 说明 | 示例输出 |
|--------|------|----------|
| `%d` | 日期时间 | `2024-01-15 10:30:45` |
| `%thread` | 线程名 | `main` |
| `%level` | 日志级别 | `INFO` |
| `%logger{N}` | Logger 名称（缩写） | `c.e.s.UserService` |
| `%msg` | 日志消息 | `查询用户成功` |
| `%n` | 换行符 | |
| `%ex` | 异常堆栈 | 完整的异常信息 |

### logback.xml 配置示例

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

输出效果：
```
2024-01-15 10:30:45.123 [main] INFO  c.e.s.UserService - 查询用户成功, userId=1001
2024-01-15 10:30:45.124 [main] WARN  c.e.s.UserService - 用户不存在, userId=1002
2024-01-15 10:30:45.125 [main] ERROR c.e.s.UserService - 查询失败
    java.lang.NullPointerException: ...
        at com.example.service.UserService.findById(UserService.java:25)
        ...
```

## 日志最佳实践

### 1. 使用占位符而非字符串拼接

```java
// 错误：字符串拼接，即使日志级别不够也会执行拼接操作
logger.debug("用户登录: " + username + ", IP: " + ipAddress);

// 正确：使用占位符，日志级别不够时不会执行拼接
logger.debug("用户登录: {}, IP: {}", username, ipAddress);
```

### 2. 判断日志级别

对于**开销较大**的日志操作，可以先判断日志级别：

```java
// 如果 DEBUG 级别未启用，避免执行昂贵的操作
if (logger.isDebugEnabled()) {
    logger.debug("对象详情: {}", expensiveToString(object));
}

// SLF4J 2.0+ 支持 lambda，更简洁
logger.debug("对象详情: {}", () -> expensiveToString(object));
```

### 3. 不要在循环中频繁打日志

```java
// 错误：循环中大量日志会严重影响性能
for (int i = 0; i < 100000; i++) {
    logger.debug("处理第 {} 条数据", i);
    process(data[i]);
}

// 正确：只在关键节点打日志
logger.info("开始批量处理, 总数={}", dataList.size());
for (int i = 0; i < dataList.size(); i++) {
    process(dataList.get(i));
}
logger.info("批量处理完成");
```

### 4. 敏感信息脱敏

```java
// 错误：直接打印敏感信息
logger.info("用户登录: username={}, password={}", username, password);
logger.info("用户信息: idCard={}", idCard);

// 正确：脱敏处理
logger.info("用户登录: username={}", username);
logger.info("用户信息: idCard={}", maskIdCard(idCard));

private String maskIdCard(String idCard) {
    if (idCard == null || idCard.length() < 8) {
        return "****";
    }
    return idCard.substring(0, 4) + "****" + idCard.substring(idCard.length() - 4);
}
```

### 5. 异常日志要带完整堆栈

```java
// 错误：只打印异常消息，丢失堆栈信息
try {
    doSomething();
} catch (Exception e) {
    logger.error("发生错误: " + e.getMessage());  // 不要这样！
}

// 正确：将异常对象作为最后一个参数
try {
    doSomething();
} catch (Exception e) {
    logger.error("发生错误", e);  // SLF4J 会自动打印完整堆栈
}
```

## 实际项目推荐

### Spring Boot 默认配置

Spring Boot 默认使用 Logback，并提供了合理的默认配置：

```yaml
# application.yml
logging:
  level:
    root: INFO
    com.example: DEBUG
    org.springframework: WARN
    org.hibernate: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/app.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30
```

### 多环境日志配置

```yaml
# application-dev.yml
logging:
  level:
    com.example: DEBUG
    root: DEBUG

# application-prod.yml
logging:
  level:
    com.example: INFO
    root: WARN
```

### Logback 多环境配置文件

```
src/main/resources/
├── logback.xml              # 默认配置（开发环境）
├── logback-dev.xml          # 开发环境
├── logback-prod.xml         # 生产环境
└── logback-spring.xml       # Spring Boot 推荐（支持 profile）
```

`logback-spring.xml` 示例：

```xml
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml" />

    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE" />
        </root>
    </springProfile>

    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="FILE" />
        </root>
    </springProfile>
</configuration>
```

通过合理的日志配置，可以在开发阶段获得详细的调试信息，在生产环境保持适当的日志级别，既能满足问题排查需求，又不会因过多日志影响性能。
