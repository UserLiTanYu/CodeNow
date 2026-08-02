# 日志体系与调试技巧

## 日志的重要性

一个运行中的应用就像一个黑盒——你无法直接观察它内部发生了什么。日志是唯一能让你了解应用运行状态的窗口：请求处理流程、异常堆栈、业务逻辑分支、性能瓶颈……

好的日志体系应该做到：**该看的时候能看到，不该看的时候不干扰**。

## Spring Boot 日志架构

Spring Boot 使用 **SLF4J** 作为日志门面（Facade），底层默认绑定 **Logback** 作为实现。

```
应用代码 → SLF4J API → Logback 实现 → 输出到控制台/文件
```

为什么需要门面模式？因为不同库可能使用不同的日志框架（Log4j、JUL、JCL），SLF4J 提供统一的 API，避免日志框架冲突。

### 日志框架桥接

如果依赖的库使用了其他日志框架，需要桥接：

| 原框架 | 桥接依赖 |
|--------|----------|
| Log4j | log4j-over-slf4j |
| Commons Logging | jcl-over-slf4j |
| java.util.logging | jul-to-slf4j |

Spring Boot 已经自动处理了大部分桥接，一般不需要手动配置。

## 日志级别

从低到高：

| 级别 | 用途 | 生产环境 |
|------|------|----------|
| TRACE | 最细粒度的跟踪信息 | 关闭 |
| DEBUG | 调试信息（变量值、分支判断） | 关闭或按需开启 |
| INFO | 关键业务节点（启动、完成） | 开启 |
| WARN | 警告（不影响运行但需关注） | 开启 |
| ERROR | 错误（异常、失败） | 开启 |
| FATAL | 致命错误（应用崩溃） | 开启 |

Logback 没有 FATAL 级别，它会将 FATAL 映射为 ERROR。

### 配置日志级别

```yaml
logging:
  level:
    root: INFO
    com.codenow: DEBUG
    com.codenow.mapper: TRACE
    org.springframework.web: WARN
    org.hibernate.SQL: DEBUG  # 显示 SQL 语句
```

也可以通过包名精确控制：

```yaml
logging:
  level:
    com.codenow.service.UserService: DEBUG
    com.codenow.controller.ArticleController: INFO
```

## logback-spring.xml 配置

`application.yml` 适合简单配置，复杂场景需要使用 XML 配置文件。

### 基本结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 变量定义 -->
    <property name="LOG_PATH" value="./logs"/>
    <property name="APP_NAME" value="codenow"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 根 Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

## 日志输出格式

### 模式说明

| 占位符 | 说明 | 示例 |
|--------|------|------|
| %d | 日期时间 | 2026-01-15 10:30:45.123 |
| %thread | 线程名 | http-nio-8080-exec-1 |
| %-5level | 日志级别（左对齐，5字符） | DEBUG |
| %logger{36} | Logger 名（最长36字符） | c.c.service.UserService |
| %msg | 日志消息 | 用户登录成功 |
| %n | 换行 | |
| %X{key} | MDC 值 | %X{traceId} |
| %file | 文件名 | UserService.java |
| %line | 行号 | 42 |

### 推荐格式

```xml
<!-- 开发环境：简洁易读 -->
<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{20} - %msg%n</pattern>

<!-- 生产环境：信息完整 -->
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId}] %logger{36}:%line - %msg%n</pattern>
```

### MDC（Mapped Diagnostic Context）

MDC 是线程级别的上下文存储，适合传递 traceId、userId 等：

```java
@Component
public class TraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

日志输出中自动携带 traceId：

```
2026-01-15 10:30:45.123 [http-nio-8080-exec-1] INFO [a1b2c3d4e5f6] c.c.service.UserService:42 - 用户登录成功
```

## 日志文件滚动

### 按时间滚动

```xml
<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
    <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.log</fileNamePattern>
    <maxHistory>30</maxHistory>
    <totalSizeCap>10GB</totalSizeCap>
</rollingPolicy>
```

- `maxHistory`：保留最近 30 天的日志
- `totalSizeCap`：总大小上限 10GB，超出后删除最旧的文件

### 按大小滚动

```xml
<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
    <maxFileSize>100MB</maxFileSize>
    <maxHistory>30</maxHistory>
    <totalSizeCap>5GB</totalSizeCap>
</rollingPolicy>
```

- 单个文件超过 100MB 时滚动，`%i` 从 0 开始递增
- 同一天内可能产生多个文件：`codenow.2026-01-15.0.log`、`codenow.2026-01-15.1.log`

### 异步日志

高并发场景下，同步写日志可能成为瓶颈：

```xml
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <appender-ref ref="FILE"/>
</appender>
```

- `queueSize`：异步队列大小
- `discardingThreshold`：队列剩余容量低于此值时丢弃 TRACE/DEBUG/INFO 级别日志（0 表示不丢弃）

## 多环境日志配置

### springProfile 标签

```xml
<configuration>
    <!-- 开发环境：控制台输出，DEBUG 级别 -->
    <springProfile name="local,dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- 生产环境：文件输出，INFO 级别 -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>
    </springProfile>
</configuration>
```

### springProperty 标签

从 Spring 配置中读取属性：

```xml
<configuration>
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="app"/>
    <springProperty scope="context" name="LOG_PATH" source="logging.path" defaultValue="./logs"/>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <!-- ... -->
    </appender>
</configuration>
```

### 按环境分离配置文件

```
src/main/resources/
├── logback-spring.xml          # 默认配置
├── logback-dev.xml             # 开发环境
└── logback-prod.xml            # 生产环境
```

```yaml
logging:
  config: classpath:logback-${spring.profiles.active}.xml
```

## 结构化日志

### JSON 格式输出

ELK（Elasticsearch + Logstash + Kibana）等日志系统通常要求 JSON 格式：

```xml
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/${APP_NAME}-json.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>${LOG_PATH}/${APP_NAME}-json.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <customFields>{"service":"codenow","version":"1.0.0"}</customFields>
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
    </encoder>
</appender>
```

依赖：

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

输出示例：

```json
{
  "@timestamp": "2026-01-15T10:30:45.123+08:00",
  "level": "INFO",
  "logger_name": "com.codenow.service.UserService",
  "thread_name": "http-nio-8080-exec-1",
  "message": "用户登录成功",
  "traceId": "a1b2c3d4e5f6",
  "userId": "12345",
  "service": "codenow",
  "version": "1.0.0"
}
```

### ELK 采集配置

Logstash 配置：

```ruby
input {
  file {
    path => "/var/log/codenow/*.log"
    codec => json
    type => "codenow"
  }
}

filter {
  if [type] == "codenow" {
    date {
      match => ["@timestamp", "ISO8601"]
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "codenow-%{+YYYY.MM.dd}"
  }
}
```

## Spring Boot 调试技巧

### --debug 参数

开启自动配置报告：

```bash
java -jar app.jar --debug
```

输出显示哪些自动配置生效、哪些未生效：

```
=========================
AUTO-CONFIGURATION REPORT
=========================

Positive matches:
  DataSourceAutoConfiguration matched:
    - @ConditionalOnClass found required class 'javax.sql.DataSource'

Negative matches:
  RabbitAutoConfiguration did not match:
    - @ConditionalOnClass did not find required class 'com.rabbitmq.client.Channel'
```

### 远程调试

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar app.jar
```

在 IDE 中配置 Remote Debug，连接到 5005 端口即可断点调试。

### DevTools 热重载

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

DevTools 的能力：

| 功能 | 说明 |
|------|------|
| 自动重启 | 类路径文件变化时重启应用 |
| LiveReload | 静态资源变化时自动刷新浏览器 |
| 属性默认值 | 缓存禁用、模板热重载等 |

LiveReload 服务默认在 35729 端口启动，浏览器安装 LiveReload 插件后可自动刷新。

### 自定义重启排除

```yaml
spring:
  devtools:
    restart:
      exclude: static/**,public/**,templates/**
      additional-paths: src/main/resources
      poll-period: 1000
```

### 远程调试（Remote）

DevTools 支持远程重启：

```yaml
spring:
  devtools:
    remote:
      secret: mysecret
```

```bash
# 服务端
java -cp app.jar -Dspring.devtools.remote.secret=mysecret org.springframework.boot.devtools.RemoteSpringApplication

# 客户端连接后，类文件变化会自动同步到远程
```

## @ConditionalOnProperty 控制日志组件

```java
@Configuration
public class LoggingConfig {

    @Bean
    @ConditionalOnProperty(name = "logging.json.enabled", havingValue = "true")
    public LayoutWrappingEncoder<ILoggingEvent> jsonEncoder() {
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setCustomFields("{\"service\":\"codenow\"}");
        return encoder;
    }

    @Bean
    @ConditionalOnProperty(name = "logging.json.enabled", havingValue = "false", matchIfMissing = true)
    public PatternLayoutEncoder patternEncoder() {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{20} - %msg%n");
        return encoder;
    }
}
```

```yaml
# application-prod.yml
logging:
  json:
    enabled: true
```

### 日志组件条件装配

```java
@Bean
@ConditionalOnProperty(name = "logging.async.enabled", havingValue = "true")
public AsyncAppender asyncAppender() {
    AsyncAppender appender = new AsyncAppender();
    appender.setQueueSize(512);
    appender.setDiscardingThreshold(0);
    appender.addAppender(fileAppender());
    return appender;
}
```

## 日志最佳实践

### 日志命名规范

```java
// 推荐：使用当前类作为 Logger
private static final Logger log = LoggerFactory.getLogger(UserService.class);

// 或使用 @Slf4j 注解（Lombok）
@Slf4j
@Service
public class UserService {
    public void login(String username) {
        log.info("用户登录: {}", username);
    }
}
```

### 避免的日志写法

```java
// 错误：字符串拼接，即使 DEBUG 关闭也会执行
log.debug("用户 " + user.getName() + " 登录成功，IP: " + ip);

// 正确：使用占位符，延迟求值
log.debug("用户 {} 登录成功，IP: {}", user.getName(), ip);

// 错误：打印完整异常栈但无上下文
log.error(e.getMessage());

// 正确：保留异常对象
log.error("用户登录失败: {}", username, e);
```

### 敏感信息脱敏

```java
// 手机号脱敏
log.info("发送验证码: {}", maskPhone(phone));

private String maskPhone(String phone) {
    return phone.substring(0, 3) + "****" + phone.substring(7);
}
```

### 日志采样

高并发场景下，避免日志过多：

```java
private static final RateLimiter logRateLimiter = RateLimiter.create(100); // 每秒 100 条

public void processRequest(Request request) {
    if (logRateLimiter.tryAcquire()) {
        log.debug("处理请求: {}", request.getId());
    }
    // 业务逻辑...
}
```