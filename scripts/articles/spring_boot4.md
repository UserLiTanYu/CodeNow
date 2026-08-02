# Actuator、健康检查与生产就绪

## 从开发到生产的鸿沟

应用开发完成后，部署到生产环境只是开始。运维团队需要回答一系列问题：应用是否健康运行？内存使用是否正常？接口响应时间如何？哪些端点被频繁调用？

Spring Boot Actuator 正是为了解决这些问题而设计的——它提供了生产级的监控和管理功能，让应用具备可观测性。

## 引入 Actuator

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

引入依赖后，Spring Boot 自动配置一系列端点（Endpoints），通过 HTTP 或 JMX 暴露应用的运行时信息。

### 端点暴露配置

默认只暴露 `/health` 端点。要暴露更多端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,loggers
        # include: "*"  # 暴露所有端点（不推荐生产环境）
      base-path: /actuator  # 端点基础路径
  endpoint:
    health:
      show-details: always  # 显示健康详情
```

## 内置端点详解

### /health — 健康检查

最核心的端点，用于负载均衡器、Kubernetes 探针等判断应用是否可用。

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500107862016,
        "free": 234567890123,
        "threshold": 10485760
      }
    }
  }
}
```

健康状态有三种：`UP`（正常）、`DOWN`（异常）、`OUT_OF_SERVICE`（暂停服务）。

### /info — 应用信息

显示应用的基本信息，需要配置：

```yaml
info:
  app:
    name: CodeNow
    description: 码上记技术博客
    version: 1.0.0
```

还可以集成 Git 和构建信息：

```xml
<plugin>
    <groupId>pl.project13.maven</groupId>
    <artifactId>git-commit-id-plugin</artifactId>
</plugin>
```

```yaml
management:
  info:
    env:
      enabled: true
    git:
      enabled: true
      mode: full
    build:
      enabled: true
```

访问 `/actuator/info` 返回：

```json
{
  "app": {
    "name": "CodeNow",
    "version": "1.0.0"
  },
  "git": {
    "branch": "main",
    "commit": {
      "id": "a1b2c3d",
      "time": "2026-01-15T10:30:00Z"
    }
  },
  "build": {
    "artifact": "codenow-backend",
    "version": "1.0.0"
  }
}
```

### /metrics — 指标数据

暴露应用的各种指标：

```bash
# 查看所有可用指标
GET /actuator/metrics

# 查看具体指标
GET /actuator/metrics/jvm.memory.used
GET /actuator/metrics/http.server.requests
GET /actuator/metrics/system.cpu.usage
```

常用指标分类：

| 分类 | 指标示例 | 说明 |
|------|----------|------|
| JVM | jvm.memory.used, jvm.gc.pause | 内存、GC |
| 系统 | system.cpu.usage, system.load | CPU、负载 |
| HTTP | http.server.requests | 请求统计 |
| Tomcat | tomcat.sessions.active | 会话数 |
| 数据源 | hikaricp.connections.active | 连接池 |

### /env — 环境属性

查看所有配置属性及其来源：

```bash
GET /actuator/env/server.port
```

```json
{
  "property": {
    "source": "application.yaml",
    "value": "8080"
  }
}
```

### /loggers — 日志管理

动态查看和修改日志级别：

```bash
# 查看某个 Logger 的级别
GET /actuator/loggers/com.codenow.service

# 动态修改日志级别（无需重启）
POST /actuator/loggers/com.codenow.service
Content-Type: application/json
{
  "configuredLevel": "DEBUG"
}
```

这是排查线上问题的利器——需要调试时临时开启 DEBUG，排查完毕后改回 INFO。

### /beans — Bean 列表

查看容器中所有 Bean 的依赖关系：

```bash
GET /actuator/beans
```

### /mappings — 映射路径

查看所有 @RequestMapping 映射：

```bash
GET /actuator/mappings
```

## 自定义健康检查

### HealthIndicator 接口

```java
@Component
public class EmailHealthIndicator implements HealthIndicator {

    private final MailServerProperties properties;

    @Override
    public Health health() {
        try {
            // 尝试连接邮件服务器
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(properties.getHost(), properties.getPort()), 3000);
            socket.close();

            return Health.up()
                    .withDetail("host", properties.getHost())
                    .withDetail("port", properties.getPort())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("host", properties.getHost())
                    .withException(e)
                    .build();
        }
    }
}
```

### CompositeHealthContributor

组合多个健康检查：

```java
@Component
public class InfrastructureHealthContributor implements CompositeHealthContributor {

    private final Map<String, HealthContributor> contributors = new LinkedHashMap<>();

    public InfrastructureHealthContributor(EmailHealthIndicator email,
                                          S3HealthIndicator s3) {
        contributors.put("email", email);
        contributors.put("s3", s3);
    }

    @Override
    public HealthContributor getContributor(String name) {
        return contributors.get(name);
    }

    @Override
    public Iterator<NamedContributor<HealthContributor>> iterator() {
        return contributors.entrySet().stream()
                .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
                .iterator();
    }
}
```

### 磁盘空间健康检查

Spring Boot 内置了 `DiskSpaceHealthIndicator`，可以自定义阈值：

```yaml
management:
  endpoint:
    health:
      diskspace:
        threshold: 500MB
        path: /
```

### 健康状态聚合

当有多个健康检查时，状态如何聚合？

```yaml
management:
  endpoint:
    health:
      status:
        order: DOWN, OUT_OF_SERVICE, UP, UNKNOWN
        http-mapping:
          DOWN: 503
          OUT_OF_SERVICE: 503
```

自定义聚合逻辑：

```java
@Component
public class CustomStatusAggregator implements StatusAggregator {

    @Override
    public Status aggregate(Status... statuses) {
        // 任一 DOWN 则整体 DOWN
        for (Status status : statuses) {
            if (Status.DOWN.equals(status)) {
                return Status.DOWN;
            }
        }
        return Status.UP;
    }
}
```

## @ConfigurationProperties 显示

`/configprops` 端点显示所有 `@ConfigurationProperties` 绑定的属性：

```java
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    private String path = "/data/uploads";
    private long maxSize = 10485760; // 10MB
    private String[] allowedTypes = {"image/png", "image/jpeg"};
    // getters/setters
}
```

## Micrometer 指标收集

Spring Boot 2.x 开始使用 Micrometer 作为指标收集的门面，类似于 SLF4J 之于日志。

### 核心概念

| 概念 | 说明 | 示例 |
|------|------|------|
| Meter | 指标的抽象 | Counter、Gauge、Timer |
| MeterRegistry | 指标注册中心 | SimpleMeterRegistry、PrometheusMeterRegistry |
| Tag | 指标的维度标签 | uri=/api/users, method=GET |
| Sample | 计时的采样点 | Timer.start() |

### Counter — 计数器

只增不减的计数器，适合统计请求数、错误数等。

```java
@Service
public class ArticleService {

    private final Counter viewCounter;
    private final Counter publishCounter;

    public ArticleService(MeterRegistry registry) {
        this.viewCounter = Counter.builder("article.views.total")
                .description("文章浏览总次数")
                .register(registry);

        this.publishCounter = Counter.builder("article.publish.total")
                .description("文章发布总数")
                .tag("type", "blog")
                .register(registry);
    }

    public Article getArticle(Long id) {
        viewCounter.increment();
        return articleRepository.findById(id);
    }

    public void publish(Article article) {
        articleRepository.save(article);
        publishCounter.increment();
    }
}
```

使用 `@Counted` 注解（Spring Boot 3.x）：

```java
@Counted(value = "article.publish", description = "文章发布计数")
public void publish(Article article) {
    articleRepository.save(article);
}
```

### Gauge — 仪表盘

反映当前值，可增可减，适合队列大小、缓存命中率等。

```java
@Service
public class CacheService {

    private final Cache<String, Object> localCache;

    public CacheService(MeterRegistry registry, Cache<String, Object> localCache) {
        this.localCache = localCache;
        Gauge.builder("cache.size", localCache, Cache::size)
                .description("本地缓存条目数")
                .register(registry);

        Gauge.builder("cache.hit.ratio", localCache,
                    cache -> cache.stats().hitRate())
                .description("缓存命中率")
                .register(registry);
    }
}
```

### Timer — 计时器

统计耗时分布，自动计算 count、totalTime、max 等。

```java
@Service
public class SearchService {

    private final Timer searchTimer;

    public SearchService(MeterRegistry registry) {
        this.searchTimer = Timer.builder("search.duration")
                .description("搜索耗时")
                .publishPercentiles(0.5, 0.95, 0.99) // P50、P95、P99
                .register(registry);
    }

    public List<Article> search(String keyword) {
        return searchTimer.record(() -> {
            return elasticsearchClient.search(keyword);
        });
    }
}
```

使用 `@Timed` 注解：

```java
@Timed(value = "api.article.detail", description = "文章详情接口耗时")
@GetMapping("/articles/{id}")
public Article getArticle(@PathVariable Long id) {
    return articleService.getArticle(id);
}
```

### DistributionSummary — 分布摘要

统计值的分布，适合请求体大小、订单金额等。

```java
@Service
public class OrderService {

    private final DistributionSummary orderAmountSummary;

    public OrderService(MeterRegistry registry) {
        this.orderAmountSummary = DistributionSummary.builder("order.amount")
                .description("订单金额分布")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void createOrder(Order order) {
        orderRepository.save(order);
        orderAmountSummary.record(order.getAmount().doubleValue());
    }
}
```

## 自定义指标注册

### MeterBinder

```java
@Component
public class CustomMetricsBinder implements MeterBinder {

    @Override
    public void bindTo(MeterRegistry registry) {
        // 注册 JVM 线程状态
        Gauge.builder("jvm.threads.states", Thread.class,
                    thread -> Thread.getAllStackTraces().keySet().size())
                .description("活跃线程数")
                .register(registry);

        // 注册应用启动时间
        Gauge.builder("app.uptime", ManagementFactory.getRuntimeMXBean(),
                    mx -> mx.getUptime() / 1000.0)
                .description("应用运行时间（秒）")
                .baseUnit("seconds")
                .register(registry);
    }
}
```

### 标签最佳实践

标签应保持**有限的基数**（Cardinality），避免高基数导致指标爆炸：

```java
// 好的做法：有限的标签值
Counter.builder("http.requests")
    .tag("method", "GET")
    .tag("status", "200")
    .tag("uri", "/api/articles")  // 使用模板路径，不要用实际路径参数
    .register(registry);

// 错误的做法：标签值无限增长
Counter.builder("http.requests")
    .tag("userId", userId)  // 每个用户一个指标，指标数量爆炸
    .register(registry);
```

## Prometheus 集成

### 依赖配置

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health
  metrics:
    tags:
      application: codenow  # 全局标签
```

### Prometheus 端点

访问 `/actuator/prometheus` 返回 Prometheus 格式的指标：

```
# HELP article_views_total 文章浏览总次数
# TYPE article_views_total counter
article_views_total{application="codenow",} 1234.0

# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/api/articles",} 567.0
http_server_requests_seconds_sum{method="GET",status="200",uri="/api/articles",} 23.456
```

### Prometheus 配置

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'codenow'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### Grafana 可视化

Grafana 通过 Prometheus 数据源查询指标，常用的 Dashboard：

- **JVM Dashboard**：内存、GC、线程
- **HTTP Metrics**：请求量、响应时间、错误率
- **Custom Dashboard**：业务指标

查询示例（PromQL）：

```promql
# 每秒请求数
rate(http_server_requests_seconds_count[5m])

# P99 响应时间
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))

# 错误率
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

## 安全配置

### 端点暴露控制

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
        exclude: env,beans,configprops  # 敏感端点不暴露
  endpoint:
    health:
      show-details: when-authorized  # 仅认证用户可见详情
```

### 管理端口分离

```yaml
management:
  server:
    port: 8081  # 管理端口与业务端口分离
    address: 127.0.0.1  # 仅本机访问
```

这样业务流量走 8080，监控流量走 8081，可以对管理端口做独立的网络隔离。

### Spring Security 集成

```java
@Configuration
@ConditionalOnProperty(name = "management.server.port")
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/prometheus").hasRole("MONITOR")
                .anyRequest().hasRole("ADMIN")
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

### JMX 端点

除了 HTTP，Actuator 还支持 JMX：

```yaml
management:
  endpoints:
    jmx:
      exposure:
        include: health,metrics
    web:
      exposure:
        include: none  # 禁用 HTTP 端点
```

## 生产环境最佳实践

### 端点配置建议

| 端点 | 生产环境 | 说明 |
|------|----------|------|
| /health | 暴露 | 负载均衡器和探针需要 |
| /info | 暴露 | 版本信息 |
| /prometheus | 暴露（受限） | 监控系统采集 |
| /metrics | 关闭 | 用 /prometheus 替代 |
| /env | 关闭 | 可能泄露敏感配置 |
| /beans | 关闭 | 内部实现细节 |
| /loggers | 受限 | 动态调整日志级别 |

### 健康检查与 Kubernetes

```yaml
# Deployment 配置
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 5
```

Spring Boot 提供了内置的 Kubernetes 探针支持：

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState,db,redis
```

### 指标采集策略

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true  # 开启直方图
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      slo:
        http.server.requests: 50ms, 100ms, 200ms  # SLA 边界
```

### 告警规则示例

基于 Prometheus 指标设置告警：

```yaml
# alertmanager.yml
groups:
  - name: codenow-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "错误率超过 1%"

      - alert: HighResponseTime
        expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "P99 响应时间超过 1 秒"
```