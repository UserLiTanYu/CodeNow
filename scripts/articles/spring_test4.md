# Logback 日志配置与链路追踪

日志是应用可观测性的重要组成部分。Logback 作为 Spring Boot 默认的日志框架，提供了灵活的配置选项和高性能的日志处理能力。本文将深入探讨 Logback 架构、配置详解、MDC 机制、结构化日志以及与 ELK 的集成。

## Logback 架构

### 核心组件

Logback 由三个核心组件组成：

```
┌─────────────────────────────────────────────────────────┐
│                    Logback 架构                          │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │                   Logger                         │   │
│  │         (日志记录器，树形层次结构)                │   │
│  └───────────────────────┬─────────────────────────┘   │
│                          │                              │
│                          ▼                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │                   Appender                       │   │
│  │         (输出目的地：控制台、文件、网络)          │   │
│  └───────────────────────┬─────────────────────────┘   │
│                          │                              │
│                          ▼                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │             Encoder / Layout                     │   │
│  │         (格式化：Pattern、JSON)                  │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### Logger 层次结构

```java
// Logger 以包名形成层次结构
Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
Logger serviceLogger = LoggerFactory.getLogger("com.codenow.service");
Logger orderLogger = LoggerFactory.getLogger("com.codenow.service.OrderService");

// 日志级别继承：如果 orderLogger 没有配置级别，则向上查找 serviceLogger
// 日志输出继承：子 Logger 会继承父 Logger 的 Appender
```

### 日志级别

| 级别 | 用途 | 数值 |
|------|------|------|
| TRACE | 最细粒度，跟踪程序执行路径 | 0 |
| DEBUG | 调试信息，开发环境使用 | 1 |
| INFO | 普通信息，关键业务节点 | 2 |
| WARN | 警告信息，潜在问题 | 3 |
| ERROR | 错误信息，异常情况 | 4 |
| OFF | 关闭日志 | 5 |

## logback-spring.xml 配置详解

### 完整配置模板

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">

    <!-- 变量定义 -->
    <property name="LOG_PATH" value="${LOG_PATH:-./logs}" />
    <property name="APP_NAME" value="${spring.application.name:-codenow}" />
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId:-}] [%X{spanId:-}] %-5level %logger{36} - %msg%n" />

    <!-- 控制台 Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件 Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>5GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 错误日志 Appender -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}-error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}-error.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>2GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 异步 Appender -->
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <neverBlock>true</neverBlock>
        <appender-ref ref="FILE" />
    </appender>

    <!-- Logger 配置 -->
    <logger name="com.codenow" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="ASYNC_FILE" />
        <appender-ref ref="ERROR_FILE" />
    </logger>

    <!-- 第三方库日志级别 -->
    <logger name="org.springframework" level="INFO" />
    <logger name="org.mybatis" level="DEBUG" />
    <logger name="com.zaxxer.hikari" level="INFO" />
    <logger name="org.hibernate.SQL" level="DEBUG" />

    <!-- Root Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

### Appender 类型

| 类型 | 用途 | 配置类 |
|------|------|--------|
| ConsoleAppender | 输出到控制台 | `ch.qos.logback.core.ConsoleAppender` |
| FileAppender | 输出到文件 | `ch.qos.logback.core.FileAppender` |
| RollingFileAppender | 滚动文件 | `ch.qos.logback.core.rolling.RollingFileAppender` |
| AsyncAppender | 异步输出 | `ch.qos.logback.classic.AsyncAppender` |
| SocketAppender | 网络输出 | `ch.qos.logback.classic.net.SocketAppender` |

## 日志输出格式（Pattern）

### Pattern 格式说明

```
%d{yyyy-MM-dd HH:mm:ss.SSS}  → 日期时间
%thread                       → 线程名
%level                        → 日志级别
%logger{36}                   → Logger 名（缩写为 36 字符）
%msg                          → 日志消息
%n                            → 换行符
%X{key}                       → MDC 值
%mdc{key:-default}            → MDC 值（带默认值）
%replace(p){regex,replacement} → 正则替换
```

### 常用 Pattern 示例

```xml
<!-- 标准格式 -->
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>

<!-- 带 traceId -->
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId:-}] %-5level %logger{36} - %msg%n</pattern>

<!-- JSON 格式 -->
<pattern>{"timestamp":"%d{yyyy-MM-dd'T'HH:mm:ss.SSS}","thread":"%thread","level":"%level","logger":"%logger","message":"%msg","traceId":"%X{traceId:-}"}%n</pattern>

<!-- 带行号 -->
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36}:%line - %msg%n</pattern>
```

### 颜色输出（控制台）

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr([%15.15thread]){faint} %clr(%-5level) %clr(%40.40logger{39}){cyan} %clr(:){faint} %msg%n</pattern>
    </encoder>
</appender>
```

## 日志级别控制

### 包级别配置

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- 应用包 -->
    <logger name="com.codenow.controller" level="DEBUG" />
    <logger name="com.codenow.service" level="DEBUG" />
    <logger name="com.codenow.repository" level="DEBUG" />

    <!-- 框架包 -->
    <logger name="org.springframework" level="INFO" />
    <logger name="org.springframework.web" level="DEBUG" />
    <logger name="org.springframework.security" level="INFO" />

    <!-- 数据库 -->
    <logger name="org.hibernate.SQL" level="DEBUG" />
    <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE" />
    <logger name="com.zaxxer.hikari" level="INFO" />

    <!-- MyBatis -->
    <logger name="org.mybatis" level="DEBUG" />
    <logger name="java.sql.PreparedStatement" level="DEBUG" />
</configuration>
```

### Spring Boot 配置文件控制

```yaml
# application.yml
logging:
  level:
    root: INFO
    com.codenow: DEBUG
    com.codenow.controller: DEBUG
    com.codenow.service: INFO
    org.springframework: INFO
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: ./logs/codenow.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 5GB
```

### 运行时动态调整日志级别

```java
@RestController
@RequestMapping("/actuator/loggers")
public class LoggerController {

    private final LoggerContext loggerContext;

    public LoggerController(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    /**
     * 获取 Logger 级别
     */
    @GetMapping("/{name}")
    public Map<String, Object> getLoggerLevel(@PathVariable String name) {
        Logger logger = loggerContext.getLogger(name);
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("configuredLevel", logger.getLevel() != null ? logger.getLevel().levelStr : null);
        result.put("effectiveLevel", logger.getEffectiveLevel().levelStr);
        return result;
    }

    /**
     * 设置 Logger 级别
     */
    @PutMapping("/{name}")
    public void setLoggerLevel(@PathVariable String name,
                                @RequestBody Map<String, String> body) {
        Logger logger = loggerContext.getLogger(name);
        String level = body.get("configuredLevel");
        logger.setLevel(Level.valueOf(level));
    }
}
```

## 日志文件滚动策略

### SizeAndTimeBasedRollingPolicy

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/${APP_NAME}.log</file>

    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <!-- 按日期和大小滚动 -->
        <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>

        <!-- 单个文件最大大小 -->
        <maxFileSize>100MB</maxFileSize>

        <!-- 保留天数 -->
        <maxHistory>30</maxHistory>

        <!-- 总大小上限 -->
        <totalSizeCap>5GB</totalSizeCap>

        <!-- 启动时清理历史文件 -->
        <cleanHistoryOnStart>true</cleanHistoryOnStart>
    </rollingPolicy>

    <encoder>
        <pattern>${LOG_PATTERN}</pattern>
        <charset>UTF-8</charset>
    </encoder>
</appender>
```

### 滚动策略对比

| 策略 | 触发条件 | 适用场景 |
|------|---------|---------|
| TimeBasedRollingPolicy | 按时间（天/小时） | 日志量稳定 |
| SizeAndTimeBasedRollingPolicy | 时间 + 大小 | 日志量波动大 |
| FixedWindowRollingPolicy | 固定窗口 | 按序号滚动 |

### 按大小滚动配置

```xml
<appender name="SIZE_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/${APP_NAME}.log</file>

    <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
        <fileNamePattern>${LOG_PATH}/${APP_NAME}.%i.log</fileNamePattern>
        <minIndex>1</minIndex>
        <maxIndex>10</maxIndex>
    </rollingPolicy>

    <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
        <maxFileSize>50MB</maxFileSize>
    </triggeringPolicy>

    <encoder>
        <pattern>${LOG_PATTERN}</pattern>
    </encoder>
</appender>
```

## MDC（Mapped Diagnostic Context）

### MDC 基本使用

```java
@Service
@Slf4j
public class OrderService {

    public Order createOrder(CreateOrderRequest request) {
        // 注入 MDC 上下文
        MDC.put("userId", request.getUserId());
        MDC.put("orderId", generateOrderId());
        MDC.put("action", "CREATE_ORDER");

        try {
            log.info("开始创建订单");

            // 业务逻辑
            Order order = doCreateOrder(request);

            log.info("订单创建成功: amount={}", order.getAmount());
            return order;
        } catch (Exception e) {
            log.error("订单创建失败", e);
            throw e;
        } finally {
            // 清理 MDC
            MDC.clear();
        }
    }
}
```

### MDC 过滤器

```java
@Component
@WebFilter("/*")
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 从请求头提取 traceId
            String traceId = httpRequest.getHeader("X-Trace-Id");
            if (traceId == null) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            MDC.put("traceId", traceId);

            // 从请求头提取 userId
            String userId = httpRequest.getHeader("X-User-Id");
            if (userId != null) {
                MDC.put("userId", userId);
            }

            // 请求 ID
            MDC.put("requestId", UUID.randomUUID().toString());

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### @Async 异步日志问题

异步方法中 MDC 会丢失，需要手动传递：

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        // 使用 MDC 包装的 TaskDecorator
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }
}

/**
 * MDC 传递装饰器
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 捕获当前线程的 MDC 上下文
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // 设置到异步线程
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
```

### CompletableFuture 中传递 MDC

```java
@Service
public class AsyncOrderService {

    /**
     * 在 CompletableFuture 中传递 MDC
     */
    public CompletableFuture<Order> createOrderAsync(CreateOrderRequest request) {
        // 捕获 MDC 上下文
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 恢复 MDC 上下文
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                return createOrder(request);
            } finally {
                MDC.clear();
            }
        }, asyncExecutor);
    }
}
```

## 多环境日志

### springProfile 区分配置

```xml
<!-- logback-spring.xml -->
<configuration>

    <!-- 开发环境配置 -->
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr([%15.15thread]){faint} %clr(%-5level) %clr(%40.40logger{39}){cyan} %clr(:){faint} %msg%n</pattern>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="CONSOLE" />
        </root>

        <logger name="com.codenow" level="DEBUG" />
        <logger name="org.hibernate.SQL" level="DEBUG" />
    </springProfile>

    <!-- 测试环境配置 -->
    <springProfile name="test">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="CONSOLE" />
        </root>
    </springProfile>

    <!-- 生产环境配置 -->
    <springProfile name="prod">
        <property name="LOG_PATH" value="/var/log/codenow" />

        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>${LOG_PATH}/codenow.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>${LOG_PATH}/codenow.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
                <maxFileSize>200MB</maxFileSize>
                <maxHistory>60</maxHistory>
                <totalSizeCap>10GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <customFields>{"service":"codenow","environment":"prod"}</customFields>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="FILE" />
        </root>

        <logger name="com.codenow" level="INFO" />
    </springProfile>

</configuration>
```

### 多环境 YAML 配置

```yaml
# application-dev.yml
logging:
  level:
    com.codenow: DEBUG
  pattern:
    console: "%clr(%d{HH:mm:ss.SSS}){faint} %clr(%-5level) %clr(%logger{36}){cyan} - %msg%n"

---
# application-prod.yml
logging:
  level:
    com.codenow: INFO
  file:
    name: /var/log/codenow/codenow.log
  logback:
    rollingpolicy:
      max-file-size: 200MB
      max-history: 60
      total-size-cap: 10GB
```

## 结构化日志

### logstash-logback-encoder

```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### JSON 格式输出

```xml
<appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <!-- 自定义字段 -->
        <customFields>{"service":"codenow-backend","environment":"${spring.profiles.active}"}</customFields>

        <!-- 时间格式 -->
        <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</timestampPattern>

        <!-- 包含 MDC 字段 -->
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>

        <!-- 包含堆栈信息 -->
        <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
            <maxDepthPerThrowable>30</maxDepthPerThrowable>
            <maxLength>2048</maxLength>
            <shortenedClassNameLength>20</shortenedClassNameLength>
            <exclude>sun\.reflect\..*</exclude>
            <exclude>java\.lang\.reflect\..*</exclude>
        </throwableConverter>
    </encoder>
</appender>
```

### JSON 输出示例

```json
{
  "@timestamp": "2024-01-15T10:30:45.123Z",
  "@version": "1",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.codenow.service.OrderService",
  "message": "订单创建成功",
  "service": "codenow-backend",
  "environment": "prod",
  "traceId": "abc123def456",
  "spanId": "789ghi012",
  "userId": "user-001",
  "orderId": "ORD-20240115-001",
  "amount": 299.00
}
```

### 自定义 JSON 字段

```java
@Service
@Slf4j
public class StructuredLogService {

    /**
     * 使用 Logstash Marker 添加自定义字段
     */
    public void logWithMarker(String orderId, String userId) {
        LogstashMarker marker = Markers.append("orderId", orderId)
            .and(Markers.append("userId", userId))
            .and(Markers.append("businessType", "ORDER"));

        log.info(marker, "订单处理完成");
    }

    /**
     * 使用 MDC 添加字段
     */
    public void logWithMdc(String orderId, String userId) {
        MDC.put("orderId", orderId);
        MDC.put("userId", userId);
        MDC.put("businessType", "ORDER");

        try {
            log.info("订单处理完成");
        } finally {
            MDC.remove("orderId");
            MDC.remove("userId");
            MDC.remove("businessType");
        }
    }
}
```

## ELK 日志采集

### Filebeat 配置

```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/codenow/*.log
    json.keys_under_root: true
    json.add_error_key: true
    fields:
      service_name: codenow-backend
      environment: production
    fields_under_root: true
    multiline.pattern: '^\d{4}-\d{2}-\d{2}'
    multiline.negate: true
    multiline.match: after

output.logstash:
  hosts: ["logstash:5044"]
  loadbalance: true
  bulk_max_size: 2048

logging.level: info
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat.log
  keepfiles: 7
```

### Logstash 管道配置

```ruby
# logstash/pipeline/logstash.conf
input {
  beats {
    port => 5044
    ssl => false
  }
}

filter {
  # 解析 JSON 日志
  if [message] =~ /^\{/ {
    json {
      source => "message"
      target => "log"
      skip_on_invalid_json => true
    }
  }

  # 提取时间戳
  if [log][timestamp] {
    date {
      match => ["log.timestamp", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd HH:mm:ss.SSS"]
      target => "@timestamp"
      timezone => "Asia/Shanghai"
    }
  }

  # 提取日志级别
  if [log][level] {
    mutate {
      add_field => { "log_level" => "%{[log][level]}" }
    }
  }

  # 提取 traceId
  if [log][traceId] {
    mutate {
      add_field => { "trace_id" => "%{[log][traceId]}" }
    }
  }

  # 提取 spanId
  if [log][spanId] {
    mutate {
      add_field => { "span_id" => "%{[log][spanId]}" }
    }
  }

  # 提取异常堆栈
  if [log][stack_trace] {
    mutate {
      add_field => { "stack_trace" => "%{[log][stack_trace]}" }
    }
  }

  # 移除不需要的字段
  mutate {
    remove_field => ["agent", "ecs", "host", "input", "log"]
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "codenow-logs-%{+YYYY.MM.dd}"
    user => "elastic"
    password => "${ELASTIC_PASSWORD}"
    ssl => false
  }

  # 调试输出
  # stdout {
  #   codec => rubydebug
  # }
}
```

### Elasticsearch 索引模板

```json
{
  "index_patterns": ["codenow-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "index.lifecycle.name": "codenow-logs-policy",
      "index.lifecycle.rollover_alias": "codenow-logs"
    },
    "mappings": {
      "properties": {
        "@timestamp": {"type": "date"},
        "level": {"type": "keyword"},
        "thread": {"type": "keyword"},
        "logger": {"type": "keyword"},
        "message": {"type": "text", "analyzer": "standard"},
        "trace_id": {"type": "keyword"},
        "span_id": {"type": "keyword"},
        "user_id": {"type": "keyword"},
        "service_name": {"type": "keyword"},
        "environment": {"type": "keyword"},
        "stack_trace": {"type": "text", "analyzer": "standard"},
        "exception_class": {"type": "keyword"}
      }
    }
  }
}
```

### Kibana 查询示例

```
# 按 traceId 查询完整链路日志
trace_id: "abc123def456"

# 按服务和日志级别查询
service_name: "codenow-backend" AND log_level: "ERROR"

# 按时间范围和用户查询
@timestamp: [now-1h TO now] AND user_id: "user-001"

# 搜索异常日志
message: "Exception" OR stack_trace: *

# 按订单号查询业务日志
message: "ORD-20240115-001"
```

### ELK 完整架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         应用服务器                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   Spring Boot App                        │   │
│  │   ┌─────────────┐                                       │   │
│  │   │   Logback    │──JSON──▶ /var/log/codenow/app.log    │   │
│  │   │   Encoder    │                                       │   │
│  │   └─────────────┘                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                          │                                      │
│                          ▼                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Filebeat                               │   │
│  │   读取日志文件 → 解析 JSON → 发送到 Logstash             │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                          │
                          │ TCP 5044
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Logstash                                   │
│   输入 → 过滤(解析/转换) → 输出                                 │
│   ┌──────────┐   ┌──────────────┐   ┌─────────────────────┐   │
│   │  Beats   │──▶│  JSON Filter │──▶│  Elasticsearch      │   │
│   │  Input   │   │  Date Filter │   │  Output             │   │
│   └──────────┘   └──────────────┘   └─────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Elasticsearch                                 │
│   ┌──────────────────────────────────────────────────────┐     │
│   │  索引: codenow-logs-2024.01.15                        │     │
│   │  文档: {timestamp, level, message, traceId, ...}      │     │
│   └──────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Kibana                                     │
│   ┌──────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│   │  Discover    │  │  Visualize   │  │  Dashboard          │ │
│   │  日志查询    │  │  图表分析    │  │  监控面板            │ │
│   └──────────────┘  └──────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

通过以上配置，我们构建了完整的日志体系：Logback 负责日志生成和格式化，MDC 提供上下文信息，结构化日志便于机器解析，Filebeat + Logstash + Elasticsearch 实现集中式日志管理，Kibana 提供日志查询和分析能力。
