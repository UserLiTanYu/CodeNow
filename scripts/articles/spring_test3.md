# Actuator、Micrometer 与可观测性

Spring Boot Actuator 为应用提供了生产级的监控端点，Micrometer 则是指标采集的标准抽象层。本文将深入探讨如何利用这些工具构建完整的可观测性体系，涵盖指标采集、Prometheus 集成、Grafana 可视化以及分布式链路追踪。

## 可观测性三支柱

### 概念模型

可观测性（Observability）是指从系统的外部输出推断其内部状态的能力，由三大支柱组成：

| 支柱 | 定义 | 数据类型 | 典型工具 |
|------|------|---------|---------|
| Metrics | 可聚合的数值型时间序列 | Counter, Gauge, Timer | Prometheus, Micrometer |
| Logging | 离散的事件记录 | 结构化日志、纯文本 | ELK, Loki, Logback |
| Tracing | 请求在分布式系统中的完整路径 | Span, Trace | Jaeger, Zipkin, OTLP |

### 三大支柱的关系

```
┌─────────────────────────────────────────────────────────┐
│                    可观测性体系                           │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │                 Metrics（指标）                   │   │
│  │  告诉你"发生了什么" - 错误率、延迟、吞吐量       │   │
│  └───────────────────────┬─────────────────────────┘   │
│                          │                              │
│         发现异常          │                              │
│         ───────▶         ▼                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │                 Tracing（链路）                   │   │
│  │  告诉你"在哪里发生" - 调用链、耗时分布           │   │
│  └───────────────────────┬─────────────────────────┘   │
│                          │                              │
│         定位服务          │                              │
│         ───────▶         ▼                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │                 Logging（日志）                   │   │
│  │  告诉你"为什么发生" - 错误详情、上下文           │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## Micrometer 概述

### 核心概念

Micrometer 是 Spring Boot 的指标采集标准，类似于 SLF4J 之于日志：

```java
// MeterRegistry - 指标注册中心
MeterRegistry registry = ...;

// Counter - 计数器（单调递增）
Counter counter = Counter.builder("requests.total")
    .description("请求总数")
    .tag("method", "GET")
    .tag("uri", "/api/users")
    .register(registry);
counter.increment();

// Gauge - 仪表盘（可增可减）
AtomicInteger queueSize = new AtomicInteger(0);
Gauge.builder("queue.size", queueSize, AtomicInteger::get)
    .description("队列长度")
    .register(registry);

// Timer - 计时器
Timer timer = Timer.builder("api.latency")
    .description("API 延迟")
    .publishPercentiles(0.5, 0.95, 0.99)
    .register(registry);
timer.record(() -> {
    // 业务逻辑
});

// DistributionSummary - 分布摘要
DistributionSummary summary = DistributionSummary.builder("order.amount")
    .description("订单金额分布")
    .publishPercentiles(0.5, 0.95, 0.99)
    .baseUnit("yuan")
    .register(registry);
summary.record(299.0);
```

### Meter 类型对比

| 类型 | 特点 | 适用场景 | 示例 |
|------|------|---------|------|
| Counter | 只增不减 | 累计计数 | 请求总数、错误数 |
| Gauge | 可增可减 | 瞬时值 | 队列深度、内存使用 |
| Timer | 记录耗时 | 延迟统计 | 接口响应时间 |
| LongTaskTimer | 长任务计时 | 长时间操作 | 批处理任务 |
| DistributionSummary | 值分布 | 大小统计 | 订单金额、响应体大小 |

## 内置指标

### Spring Boot 自动配置的指标

Spring Boot Actuator 自动采集以下指标：

**JVM 指标**

```
jvm.memory.used.bytes          # 内存使用量
jvm.memory.max.bytes           # 最大内存
jvm.gc.pause.seconds           # GC 暂停时间
jvm.threads.live.threads       # 活跃线程数
jvm.threads.daemon.threads     # 守护线程数
```

**HTTP 指标**

```
http.server.requests           # HTTP 请求指标
  ├── method=GET, uri=/api/users, status=200
  ├── method=POST, uri=/api/orders, status=201
  └── method=GET, uri=/api/users, status=500
```

**数据库连接池指标**

```
hikaricp.connections.active    # 活跃连接数
hikaricp.connections.idle      # 空闲连接数
hikaricp.connections.max       # 最大连接数
hikaricp.connections.pending   # 等待连接数
```

**Tomcat 指标**

```
tomcat.sessions.active.current # 当前活跃会话
tomcat.sessions.created        # 创建的会话数
tomcat.threads.busy            # 忙碌线程数
tomcat.threads.current         # 当前线程数
```

### 查看内置指标

```java
@RestController
@RequestMapping("/actuator/metrics")
public class MetricsController {

    private final MeterRegistry meterRegistry;

    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/jvm")
    public Map<String, Object> jvmMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // JVM 内存
        metrics.put("heapUsed", meterRegistry.find("jvm.memory.used")
            .tag("area", "heap").gauge().value());
        metrics.put("heapMax", meterRegistry.find("jvm.memory.max")
            .tag("area", "heap").gauge().value());

        // 线程数
        metrics.put("threadLive", meterRegistry.find("jvm.threads.live")
            .gauge().value());
        metrics.put("threadDaemon", meterRegistry.find("jvm.threads.daemon")
            .gauge().value());

        return metrics;
    }
}
```

## 自定义业务指标

### Counter：订单计数

```java
@Service
@Slf4j
public class OrderMetricsService {

    private final Counter orderCreatedCounter;
    private final Counter orderPaidCounter;
    private final Counter orderCancelledCounter;
    private final Counter orderFailedCounter;

    public OrderMetricsService(MeterRegistry registry) {
        // 创建订单计数器
        this.orderCreatedCounter = Counter.builder("orders.created")
            .description("创建的订单数")
            .tag("type", "created")
            .register(registry);

        this.orderPaidCounter = Counter.builder("orders.paid")
            .description("支付的订单数")
            .tag("type", "paid")
            .register(registry);

        this.orderCancelledCounter = Counter.builder("orders.cancelled")
            .description("取消的订单数")
            .tag("type", "cancelled")
            .register(registry);

        this.orderFailedCounter = Counter.builder("orders.failed")
            .description("失败的订单数")
            .tag("type", "failed")
            .register(registry);
    }

    public void incrementCreated() {
        orderCreatedCounter.increment();
    }

    public void incrementPaid() {
        orderPaidCounter.increment();
    }

    public void incrementCancelled() {
        orderCancelledCounter.increment();
    }

    public void incrementFailed() {
        orderFailedCounter.increment();
    }
}
```

### Timer：接口耗时

```java
@Service
public class ApiMetricsService {

    private final Timer orderCreateTimer;
    private final Timer paymentProcessTimer;

    public ApiMetricsService(MeterRegistry registry) {
        this.orderCreateTimer = Timer.builder("api.order.create.duration")
            .description("订单创建接口耗时")
            .publishPercentiles(0.5, 0.95, 0.99)  // 发布百分位
            .publishPercentileHistogram()           // 发布直方图
            .sla(Duration.ofMillis(100),            // SLA 桶
                 Duration.ofMillis(500),
                 Duration.ofSeconds(1))
            .register(registry);

        this.paymentProcessTimer = Timer.builder("api.payment.process.duration")
            .description("支付处理耗时")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    /**
     * 记录订单创建耗时
     */
    public <T> T recordOrderCreate(Supplier<T> supplier) {
        return orderCreateTimer.record(supplier);
    }

    /**
     * 记录支付处理耗时
     */
    public void recordPaymentProcess(long durationMillis) {
        paymentProcessTimer.record(Duration.ofMillis(durationMillis));
    }
}
```

**使用示例**

```java
@Service
public class OrderService {

    private final ApiMetricsService metricsService;

    public Order createOrder(CreateOrderRequest request) {
        return metricsService.recordOrderCreate(() -> {
            // 业务逻辑
            Order order = new Order();
            order.setOrderId(generateOrderId());
            order.setStatus(OrderStatus.CREATED);
            // ...
            return order;
        });
    }
}
```

### Gauge：队列深度

```java
@Component
public class QueueMetrics {

    private final Queue<OrderDTO> orderQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queueSize = new AtomicInteger(0);

    public QueueMetrics(MeterRegistry registry) {
        // 注册 Gauge - 监控队列大小
        Gauge.builder("queue.orders.size", queueSize, AtomicInteger::get)
            .description("待处理订单队列大小")
            .tag("queue", "orders")
            .register(registry);

        // 注册 Gauge - 监控队列内存占用
        Gauge.builder("queue.orders.memory", this, QueueMetrics::estimateMemory)
            .description("订单队列内存占用估算")
            .baseUnit("bytes")
            .register(registry);
    }

    public void enqueue(OrderDTO order) {
        orderQueue.offer(order);
        queueSize.incrementAndGet();
    }

    public OrderDTO dequeue() {
        OrderDTO order = orderQueue.poll();
        if (order != null) {
            queueSize.decrementAndGet();
        }
        return order;
    }

    private double estimateMemory() {
        // 简单估算：每个订单对象约 500 字节
        return queueSize.get() * 500L;
    }
}
```

### 多维度指标

```java
@Service
public class HttpMetricsService {

    private final MeterRegistry registry;

    public HttpMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 记录 HTTP 请求指标（多维度）
     */
    public void recordHttpRequest(String method, String uri, int status,
                                   long durationMillis) {
        // 使用 Timer 记录请求耗时
        Timer.builder("http.server.requests.custom")
            .tag("method", method)
            .tag("uri", uri)
            .tag("status", String.valueOf(status))
            .tag("status_family", getStatusFamily(status))
            .register(registry)
            .record(Duration.ofMillis(durationMillis));
    }

    private String getStatusFamily(int status) {
        if (status >= 200 && status < 300) return "2xx";
        if (status >= 300 && status < 400) return "3xx";
        if (status >= 400 && status < 500) return "4xx";
        return "5xx";
    }
}
```

## 标签维度设计

### 高基数标签陷阱

高基数（High Cardinality）标签会导致指标数据爆炸：

```java
// ❌ 错误示例：使用用户ID作为标签
Counter.builder("api.requests")
    .tag("userId", userId)  // 高基数！可能有数百万用户
    .register(registry);

// ❌ 错误示例：使用请求路径参数
Counter.builder("api.requests")
    .tag("uri", "/api/users/" + userId)  // 高基数！
    .register(registry);

// ✅ 正确示例：使用路径模板
Counter.builder("api.requests")
    .tag("uri", "/api/users/{id}")  // 低基数
    .register(registry);

// ✅ 正确示例：使用分桶
Counter.builder("api.requests")
    .tag("response_time_bucket", getTimeBucket(duration))  // 分桶
    .register(registry);
```

### 标签设计原则

| 原则 | 说明 | 示例 |
|------|------|------|
| 低基数 | 标签值数量有限 | method, uri, status |
| 有意义 | 能区分不同维度 | environment, service |
| 避免动态 | 不使用 ID、UUID | 避免 userId, requestId |
| 分桶处理 | 大数值范围分桶 | duration → fast/medium/slow |

```java
// 标签分桶示例
public String getDurationBucket(long durationMillis) {
    if (durationMillis < 100) return "fast";
    if (durationMillis < 500) return "medium";
    if (durationMillis < 2000) return "slow";
    return "very_slow";
}

public String getAgeBucket(int age) {
    if (age < 18) return "minor";
    if (age < 30) return "young";
    if (age < 50) return "middle";
    return "senior";
}
```

## Prometheus 集成

### 依赖配置

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Micrometer Prometheus -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

### 应用配置

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
    export:
      prometheus:
        enabled: true
        step: 1m
        descriptions: true
```

### Prometheus 配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: codenow-backend
          environment: dev

  - job_name: 'spring-boot-app-cluster'
    metrics_path: '/actuator/prometheus'
    consul_sd_configs:
      - server: 'consul:8500'
        services:
          - 'codenow-backend'
```

### 自定义 Prometheus 指标

```java
@Configuration
public class PrometheusConfig {

    @Bean
    public MeterRegistryCustomizer<PrometerMeterRegistry> prometheusCustomizer() {
        return registry -> {
            // 添加全局标签
            registry.config()
                .commonTags("application", "codenow")
                .commonTags("region", "cn-east");
        };
    }

    @Bean
    public CollectorRegistry collectorRegistry() {
        return CollectorRegistry.defaultRegistry;
    }
}
```

## Grafana 仪表盘

### 数据源配置

```yaml
# grafana/provisioning/datasources/prometheus.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

### Dashboard 模板

```json
{
  "dashboard": {
    "title": "CodeNow 应用监控",
    "tags": ["spring-boot", "micrometer"],
    "timezone": "browser",
    "panels": [
      {
        "title": "请求速率 (QPS)",
        "type": "graph",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0},
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count[5m])) by (uri)",
            "legendFormat": "{{uri}}"
          }
        ]
      },
      {
        "title": "响应时间 P95",
        "type": "graph",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0},
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))",
            "legendFormat": "{{uri}}"
          }
        ]
      },
      {
        "title": "错误率",
        "type": "stat",
        "gridPos": {"h": 4, "w": 6, "x": 0, "y": 8},
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{status=~'5..'}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))"
          }
        ]
      },
      {
        "title": "JVM 内存使用",
        "type": "graph",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 12},
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area='heap'}",
            "legendFormat": "Heap Used"
          },
          {
            "expr": "jvm_memory_max_bytes{area='heap'}",
            "legendFormat": "Heap Max"
          }
        ]
      },
      {
        "title": "数据库连接池",
        "type": "graph",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 12},
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active"
          },
          {
            "expr": "hikaricp_connections_idle",
            "legendFormat": "Idle"
          },
          {
            "expr": "hikaricp_connections_max",
            "legendFormat": "Max"
          }
        ]
      }
    ]
  }
}
```

### Grafana 变量

```json
{
  "templating": {
    "list": [
      {
        "name": "application",
        "type": "query",
        "query": "label_values(http_server_requests_seconds_count, application)"
      },
      {
        "name": "uri",
        "type": "query",
        "query": "label_values(http_server_requests_seconds_count{application='$application'}, uri)"
      }
    ]
  }
}
```

## 分布式链路追踪

### Micrometer Tracing 配置

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Micrometer Tracing -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-brave</artifactId>
    </dependency>

    <!-- Zipkin -->
    <dependency>
        <groupId>io.zipkin.reporter2</groupId>
        <artifactId>zipkin-reporter-brave</artifactId>
    </dependency>

    <!-- 或 OTLP -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
</dependencies>
```

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

### 自定义 Span

```java
@Service
@Slf4j
public class TracedOrderService {

    private final Tracer tracer;

    public Order createOrder(CreateOrderRequest request) {
        // 创建自定义 Span
        Span span = tracer.nextSpan().name("createOrder").start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // 添加标签
            span.tag("order.userId", request.getUserId());
            span.tag("order.amount", String.valueOf(request.getAmount()));

            // 业务逻辑
            Order order = doCreateOrder(request);

            span.tag("order.id", order.getOrderId());
            span.tag("order.status", order.getStatus().name());

            return order;
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @Observed(name = "order.process")
    public void processOrder(String orderId) {
        // 使用 @Observed 注解自动创建 Span
        // ...
    }
}
```

## 日志与链路关联

### MDC 注入 traceId

```java
@Configuration
public class TracingMdcConfig {

    @Bean
    public FilterRegistrationBean<TracingFilter> tracingFilter(Tracer tracer) {
        FilterRegistrationBean<TracingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TracingFilter(tracer));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    public static class TracingFilter implements Filter {

        private final Tracer tracer;

        public TracingFilter(Tracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response,
                             FilterChain chain) throws IOException, ServletException {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                MDC.put("traceId", currentSpan.context().traceId());
                MDC.put("spanId", currentSpan.context().spanId());
            }
            try {
                chain.doFilter(request, response);
            } finally {
                MDC.remove("traceId");
                MDC.remove("spanId");
            }
        }
    }
}
```

### Logback 配置输出 traceId

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId:-}] [%X{spanId:-}] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
</configuration>
```

## Spring Boot Actuator 安全

### 端点暴露控制

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
        exclude: env,beans,configprops
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      roles: ADMIN
    shutdown:
      enabled: false
```

### Actuator 端点安全配置

```java
@Configuration
@EnableWebSecurity
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

### JMX 访问配置

```yaml
# application.yml
spring:
  jmx:
    enabled: true
    server:
      port: 9999
management:
  endpoints:
    jmx:
      exposure:
        include: health,metrics
  endpoint:
    jmx:
      enabled: true
```

### Actuator 端点清单

| 端点 | 用途 | 默认启用 |
|------|------|---------|
| `/actuator/health` | 健康检查 | ✅ |
| `/actuator/info` | 应用信息 | ✅ |
| `/actuator/metrics` | 指标列表 | ✅ |
| `/actuator/prometheus` | Prometheus 格式指标 | ✅ |
| `/actuator/env` | 环境变量 | ❌ |
| `/actuator/beans` | Spring Beans | ❌ |
| `/actuator/mappings` | URL 映射 | ❌ |
| `/actuator/configprops` | 配置属性 | ❌ |
| `/actuator/loggers` | 日志级别管理 | ❌ |
| `/actuator/threaddump` | 线程转储 | ❌ |
| `/actuator/heapdump` | 堆转储 | ❌ |
| `/actuator/shutdown` | 关闭应用 | ❌ |

通过以上配置，我们构建了完整的可观测性体系：Micrometer 负责指标采集，Prometheus 存储时序数据，Grafana 提供可视化，Micrometer Tracing 实现链路追踪，Actuator 提供运维端点。
