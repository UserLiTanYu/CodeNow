# 微服务安全与可观测性实战

在微服务架构中，安全与可观测性是保障系统稳定运行的两大基石。安全确保服务间的访问控制和数据保护，可观测性则帮助我们快速定位和解决问题。本文将深入探讨微服务安全架构设计、分布式链路追踪、集中式日志管理以及告警策略。

## 微服务安全架构

### 安全架构概览

微服务安全需要解决的核心问题：

| 安全层面 | 挑战 | 解决方案 |
|---------|------|---------|
| 外部访问 | 用户身份认证 | 网关统一鉴权 |
| 服务间调用 | 服务身份验证 | mTLS / ServiceAccount Token |
| 数据传输 | 防止窃听篡改 | TLS 加密 |
| 权限控制 | 细粒度授权 | RBAC + OAuth2 |

### 网关统一鉴权

网关作为系统的入口，负责统一处理认证逻辑：

```java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    // 白名单路径，无需认证
    private static final Set<String> WHITE_LIST = Set.of(
        "/api/auth/login",
        "/api/auth/captcha",
        "/api/blog/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 白名单直接放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 提取 Token
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return unauthorized(exchange, "缺少认证令牌");
        }

        // 验证 Token
        if (!tokenProvider.validateToken(token)) {
            return unauthorized(exchange, "令牌无效或已过期");
        }

        // 解析用户信息并透传给下游
        Claims claims = tokenProvider.getClaims(token);
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .header("X-User-Id", claims.getSubject())
            .header("X-User-Role", claims.get("role", String.class))
            .header("X-Request-Id", UUID.randomUUID().toString())
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

### 服务间信任模型

微服务间的调用需要建立信任关系：

```
┌─────────────────────────────────────────────────────────┐
│                      网关层                              │
│               (统一鉴权 + Token 解析)                    │
└───────────────────────┬─────────────────────────────────┘
                        │ X-User-Id / X-User-Role
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
   ┌─────────┐    ┌─────────┐    ┌─────────┐
   │ 服务 A  │───▶│ 服务 B  │───▶│ 服务 C  │
   │         │    │         │    │         │
   │ mTLS    │    │ mTLS    │    │ mTLS    │
   └─────────┘    └─────────┘    └─────────┘
```

## JWT 在微服务中的传递

### 完整的 Token 传递流程

JWT 在微服务架构中的传递遵循以下流程：

**第一步：网关解析并透传用户信息**

```java
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 从网关透传的请求头中获取用户信息
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        String requestId = request.getHeader("X-Request-Id");

        if (userId != null) {
            UserContext.setUserId(userId);
            UserContext.setRole(userRole);
            UserContext.setRequestId(requestId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
```

**第二步：用户上下文持有类**

```java
public class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void setRole(String role) {
        ROLE.set(role);
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
        REQUEST_ID.remove();
    }
}
```

**第三步：Feign 调用时自动携带用户信息**

```java
@Configuration
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String userId = UserContext.getUserId();
        String role = UserContext.getRole();
        String requestId = UserContext.getRequestId();

        if (userId != null) {
            template.header("X-User-Id", userId);
        }
        if (role != null) {
            template.header("X-User-Role", role);
        }
        if (requestId != null) {
            template.header("X-Request-Id", requestId);
        }
    }
}
```

## 服务间认证

### mTLS 双向认证

mTLS（Mutual TLS）要求服务端和客户端互相验证证书身份：

```yaml
# application.yml - 服务端 TLS 配置
server:
  ssl:
    enabled: true
    key-store: classpath:server-keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    trust-store: classpath:truststore.p12
    trust-store-password: ${TRUSTSTORE_PASSWORD}
    client-auth: need
```

```java
// 客户端配置 mTLS
@Configuration
public class WebClientSslConfig {

    @Bean
    public WebClient secureWebClient() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(getClass().getResourceAsStream("/client-keystore.p12"),
                      "password".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, "password".toCharArray());

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(getClass().getResourceAsStream("/truststore.p12"),
                        "password".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        SslContext sslContext = SslContextBuilder.forClient()
            .keyManager(kmf)
            .trustManager(tmf)
            .build();

        HttpClient httpClient = HttpClient.create()
            .secure(ssl -> ssl.sslContext(sslContext));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
```

### ServiceAccount Token

在 Kubernetes 环境中，服务可以使用 ServiceAccount Token 进行身份认证：

```yaml
# Kubernetes ServiceAccount 配置
apiVersion: v1
kind: ServiceAccount
metadata:
  name: order-service
  namespace: codenow
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  template:
    spec:
      serviceAccountName: order-service
      containers:
        - name: order-service
          image: codenow/order-service:latest
          volumeMounts:
            - name: sa-token
              mountPath: /var/run/secrets/kubernetes.io/serviceaccount
              readOnly: true
      volumes:
        - name: sa-token
          projected:
            sources:
              - serviceAccountToken:
                  path: token
                  expirationSeconds: 3600
                  audience: api
```

```java
// 服务间调用时携带 ServiceAccount Token
@Component
public class K8sServiceAuthInterceptor implements ClientHttpRequestInterceptor {

    @Value("${kubernetes.service-account.token-path}")
    private String tokenPath;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String token = Files.readString(Path.of(tokenPath));
        request.getHeaders().setBearerAuth(token);
        return execution.execute(request, body);
    }
}
```

## 分布式链路追踪

### Micrometer Tracing 架构

Micrometer Tracing 是 Spring Boot 3 的链路追踪抽象层，支持多种后端：

```
┌─────────────────────────────────────────────────────────┐
│                   应用层 (Spring Boot)                   │
│         Micrometer Tracing API (Tracer, Span)           │
└───────────────────────┬─────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
   ┌─────────┐    ┌──────────┐    ┌─────────┐
   │  Brave   │    │  OTLP    │    │ Zipkin  │
   │ Bridge   │    │ Bridge   │    │ Bridge  │
   └────┬────┘    └────┬─────┘    └────┬────┘
        │              │               │
        ▼              ▼               ▼
   ┌─────────┐   ┌──────────┐   ┌─────────┐
   │ Zipkin  │   │  Jaeger  │   │ Grafana │
   │ Server  │   │ Collector│   │ Tempo   │
   └─────────┘   └──────────┘   └─────────┘
```

### 依赖配置

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Micrometer Tracing -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-brave</artifactId>
    </dependency>

    <!-- Zipkin Reporter -->
    <dependency>
        <groupId>io.zipkin.reporter2</groupId>
        <artifactId>zipkin-reporter-brave</artifactId>
    </dependency>

    <!-- 或者使用 OTLP -->
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
      probability: 1.0  # 采样率，生产环境建议 0.1
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

### TraceId 传递

**MDC 注入 TraceId**

```java
@Component
public class TracingMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从当前 Span 获取 TraceId 和 SpanId
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                MDC.put("traceId", currentSpan.context().traceId());
                MDC.put("spanId", currentSpan.context().spanId());
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }
}
```

**Feign 拦截器透传 TraceId**

```java
@Configuration
public class TracingFeignInterceptor implements RequestInterceptor {

    private final Tracer tracer;

    @Override
    public void apply(RequestTemplate template) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            // 透传 TraceContext
            template.header("X-Trace-Id", currentSpan.context().traceId());
            template.header("X-Span-Id", currentSpan.context().spanId());
            template.header("X-Parent-Span-Id", currentSpan.context().parentId());
        }
    }
}
```

## 集中式日志：ELK 架构

### ELK 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                        应用服务器                            │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │
│  │ App 1   │  │ App 2   │  │ App 3   │  │ App N   │       │
│  │ Filebeat│  │ Filebeat│  │ Filebeat│  │ Filebeat│       │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘       │
└───────┼────────────┼────────────┼────────────┼──────────────┘
        │            │            │            │
        └────────────┼────────────┼────────────┘
                     ▼
            ┌─────────────────┐
            │    Logstash      │
            │  (解析/过滤/转换) │
            └────────┬────────┘
                     ▼
            ┌─────────────────┐
            │  Elasticsearch   │
            │    (存储/索引)    │
            └────────┬────────┘
                     ▼
            ┌─────────────────┐
            │     Kibana       │
            │   (查询/可视化)   │
            └─────────────────┘
```

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

output.logstash:
  hosts: ["logstash:5044"]
  loadbalance: true

logging.level: info
logging.to_files: true
```

### Logstash 管道配置

```ruby
# logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  # 解析 JSON 日志
  json {
    source => "message"
    target => "log"
  }

  # 提取时间戳
  date {
    match => ["log.timestamp", "yyyy-MM-dd HH:mm:ss.SSS"]
    target => "@timestamp"
  }

  # 提取日志级别
  mutate {
    add_field => { "log_level" => "%{log.level}" }
    add_field => { "trace_id" => "%{log.traceId}" }
    add_field => { "span_id" => "%{log.spanId}" }
  }

  # 移除不需要的字段
  mutate {
    remove_field => ["message", "agent", "ecs", "host", "input"]
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "codenow-logs-%{+YYYY.MM.dd}"
    user => "elastic"
    password => "${ELASTIC_PASSWORD}"
  }
}
```

## 告警策略

### Prometheus AlertManager 配置

```yaml
# prometheus/alert-rules.yml
groups:
  - name: codenow-alerts
    rules:
      # 服务存活检测
      - alert: ServiceDown
        expr: up{job="codenow-backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.instance }} 已宕机"
          description: "服务 {{ $labels.instance }} 已超过 1 分钟无响应"

      # 响应时间过长
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket{uri!~"/actuator.*"}) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "接口响应时间过长"
          description: "P95 响应时间超过 2 秒，当前值: {{ $value }}"

      # 错误率过高
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          /
          sum(rate(http_server_requests_seconds_count[5m]))
          > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "HTTP 5xx 错误率过高"
          description: "5 分钟内错误率超过 5%，当前值: {{ $value | humanizePercentage }}"

      # JVM 内存使用率
      - alert: HighJvmMemory
        expr: |
          jvm_memory_used_bytes{area="heap"}
          /
          jvm_memory_max_bytes{area="heap"}
          > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM 堆内存使用率过高"
          description: "堆内存使用率超过 85%，当前值: {{ $value | humanizePercentage }}"

      # 数据库连接池
      - alert: DataSourcePoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "数据库连接池即将耗尽"
          description: "连接池使用率超过 90%"
```

### AlertManager 路由配置

```yaml
# alertmanager.yml
route:
  group_by: ['alertname', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default'

  routes:
    - match:
        severity: critical
      receiver: 'pager'
      repeat_interval: 1h

    - match:
        severity: warning
      receiver: 'slack'
      repeat_interval: 4h

receivers:
  - name: 'default'
    webhook_configs:
      - url: 'http://notification-service:8080/alerts'

  - name: 'pager'
    pagerduty_configs:
      - service_key: '${PAGERDUTY_KEY}'

  - name: 'slack'
    slack_configs:
      - api_url: '${SLACK_WEBHOOK_URL}'
        channel: '#alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
```

### Grafana 告警规则

```yaml
# grafana-alert-rule.json
{
  "alert": {
    "name": "订单服务错误率告警",
    "message": "订单服务 5 分钟内错误率超过阈值",
    "conditions": [
      {
        "evaluator": {
          "params": [0.05],
          "type": "gt"
        },
        "operator": {
          "type": "and"
        },
        "query": {
          "params": ["A", "5m", "now"]
        },
        "reducer": {
          "params": [],
          "type": "avg"
        },
        "type": "query"
      }
    ],
    "frequency": "1m",
    "handler": 1,
    "notifications": []
  }
}
```

## 服务网格简介

### Istio 架构

服务网格通过 Sidecar 模式实现服务间通信的基础设施层：

```
┌─────────────────────────────────────────────────────────┐
│                     Kubernetes Pod                       │
│  ┌─────────────────────┐  ┌─────────────────────┐      │
│  │    应用容器          │  │    Envoy Sidecar    │      │
│  │                     │  │                     │      │
│  │  ┌───────────────┐  │  │  ┌───────────────┐  │      │
│  │  │  Order Service │  │  │  │   Inbound     │  │      │
│  │  │               │  │  │  │   Listener    │  │      │
│  │  └───────────────┘  │  │  └───────────────┘  │      │
│  │                     │  │  ┌───────────────┐  │      │
│  │                     │  │  │   Outbound    │  │      │
│  │                     │  │  │   Listener    │  │      │
│  │                     │  │  └───────────────┘  │      │
│  └─────────────────────┘  └─────────────────────┘      │
└─────────────────────────────────────────────────────────┘
```

### Istio 核心功能

**流量管理**

```yaml
# 虚拟服务 - 金丝雀发布
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: order-service
spec:
  hosts:
    - order-service
  http:
    - route:
        - destination:
            host: order-service
            subset: v1
          weight: 90
        - destination:
            host: order-service
            subset: v2
          weight: 10
```

**熔断配置**

```yaml
# 目标规则 - 连接池和熔断
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: order-service
spec:
  host: order-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        h2UpgradePolicy: DEFAULT
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
```

**双向 TLS 配置**

```yaml
# 启用服务间 mTLS
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: codenow
spec:
  mtls:
    mode: STRICT
```

## 微服务可观测性三大支柱

### 可观测性体系

微服务可观测性由三大支柱组成：

| 支柱 | 定义 | 工具 | 用途 |
|------|------|------|------|
| Metrics | 数值型时间序列数据 | Prometheus, Grafana | 监控趋势、设置告警 |
| Logging | 离散的事件记录 | ELK, Loki | 问题排查、审计追踪 |
| Tracing | 请求的完整调用链 | Jaeger, Zipkin | 性能分析、依赖梳理 |

### 三大支柱的关联

```java
// 统一的可观测性配置
@Configuration
public class ObservabilityConfig {

    // 自定义指标
    @Bean
    public MeterBinder orderMetrics(OrderRepository orderRepository) {
        return registry -> {
            // 订单总数计数器
            Counter.builder("orders.total")
                .description("订单总数")
                .tag("type", "all")
                .register(registry);

            // 订单处理耗时
            Timer.builder("orders.process.duration")
                .description("订单处理耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

            // 待处理订单数量
            Gauge.builder("orders.pending", orderRepository,
                         repo -> repo.countByStatus(OrderStatus.PENDING))
                .description("待处理订单数量")
                .register(registry);
        };
    }
}
```

### 统一日志格式

```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "service": "order-service",
  "traceId": "abc123def456",
  "spanId": "789ghi012",
  "userId": "user-001",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.codenow.service.OrderService",
  "message": "订单创建成功",
  "orderId": "ORD-20240115-001",
  "amount": 299.00,
  "duration": 156
}
```

### Prometheus 指标采集配置

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'codenow-backend'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['backend-1:8080', 'backend-2:8080']
        labels:
          application: codenow-backend
          environment: production

  - job_name: 'codenow-frontend'
    metrics_path: '/metrics'
    scrape_interval: 30s
    static_configs:
      - targets: ['frontend:3000']

  - job_name: 'mysql-exporter'
    static_configs:
      - targets: ['mysql-exporter:9104']

  - job_name: 'redis-exporter'
    static_configs:
      - targets: ['redis-exporter:9121']
```

### Grafana Dashboard JSON 片段

```json
{
  "panels": [
    {
      "title": "请求速率 (QPS)",
      "type": "graph",
      "targets": [
        {
          "expr": "sum(rate(http_server_requests_seconds_count[5m])) by (uri)",
          "legendFormat": "{{uri}}"
        }
      ]
    },
    {
      "title": "响应时间分布",
      "type": "heatmap",
      "targets": [
        {
          "expr": "sum(rate(http_server_requests_seconds_bucket[5m])) by (le)",
          "legendFormat": "{{le}}"
        }
      ]
    },
    {
      "title": "错误率",
      "type": "stat",
      "targets": [
        {
          "expr": "sum(rate(http_server_requests_seconds_count{status=~'5..'}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))",
          "legendFormat": "Error Rate"
        }
      ]
    }
  ]
}
```

### 完整的可观测性架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         可视化层                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Grafana     │  │    Kibana    │  │    Jaeger    │          │
│  │  (Metrics)   │  │  (Logging)   │  │  (Tracing)   │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼─────────────────┼─────────────────┼──────────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                         存储层                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Prometheus  │  │Elasticsearch │  │   Storage    │          │
│  │  (时序数据)  │  │  (日志数据)  │  │  (链路数据)  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
          ▲                 ▲                 ▲
          │                 │                 │
┌─────────────────────────────────────────────────────────────────┐
│                         采集层                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  /actuator   │  │   Filebeat   │  │   OTLP       │          │
│  │ /prometheus  │  │   Fluentd    │  │   Zipkin     │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
          │                 │                 │
┌─────────────────────────────────────────────────────────────────┐
│                      微服务应用层                                 │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐           │
│  │ 服务 A  │  │ 服务 B  │  │ 服务 C  │  │ 服务 D  │           │
│  │Micrometer│  │Micrometer│  │Micrometer│  │Micrometer│           │
│  │ Logback │  │ Logback │  │ Logback │  │ Logback │           │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

通过以上配置，我们构建了完整的微服务安全与可观测性体系。网关统一鉴权保障了外部访问安全，mTLS 确保了服务间信任，Micrometer Tracing 提供了分布式链路追踪能力，ELK 栈实现了集中式日志管理，Prometheus + Grafana 建立了完善的监控告警机制。
