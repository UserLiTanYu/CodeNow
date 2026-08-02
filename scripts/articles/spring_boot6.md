# 嵌入式容器与 SSL 配置

## 嵌入式容器的革命

传统 Java Web 应用需要打包成 WAR 文件，部署到外部的 Tomcat、Jetty 等容器中。Spring Boot 打破了这个模式——它将容器嵌入到应用内部，打包成可执行的 JAR 文件，直接 `java -jar` 启动。

这个改变带来了巨大的便利：部署只需要一个 JAR 文件，不需要安装和配置外部容器，每个应用可以独立运行不同版本的容器。

## 容器选择

Spring Boot 支持三种嵌入式容器：

| 容器 | 特点 | 适用场景 |
|------|------|----------|
| Tomcat | 默认选择，功能全面，社区活跃 | 通用 Web 应用 |
| Jetty | 轻量级，长连接支持好 | 微服务、WebSocket |
| Undertow | 高性能，内存占用小 | 高并发场景 |

### 切换容器

默认使用 Tomcat，切换到 Jetty：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

切换到 Undertow：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

## 容器配置

### 基本配置

```yaml
server:
  port: 8080
  address: 0.0.0.0
  servlet:
    context-path: /
    session:
      timeout: 30m
```

### Tomcat 连接池配置

```yaml
server:
  tomcat:
    accept-count: 100       # 等待队列长度
    max-connections: 10000  # 最大连接数
    threads:
      max: 200              # 最大工作线程数
      min-spare: 10         # 最小空闲线程数
    connection-timeout: 20s
    max-swallow-size: 2MB
    max-http-form-post-size: 2MB
```

各参数的含义：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| accept-count | 100 | 当所有线程都在忙时，等待队列的长度 |
| max-connections | 10000（NIO）| 最大连接数，超出后拒绝连接 |
| threads.max | 200 | 最大工作线程数 |
| threads.min-spare | 10 | 最小空闲线程数 |
| connection-timeout | 20s | 连接超时时间 |

### 线程池调优

```yaml
server:
  tomcat:
    threads:
      max: 400
      min-spare: 50
    accept-count: 200
    max-connections: 10000
```

线程池大小的经验公式：

```
线程数 = CPU 核心数 × (1 + 线程等待时间 / 线程计算时间)
```

对于 I/O 密集型应用（数据库查询、远程调用），线程数可以设置较大；对于 CPU 密集型应用，线程数接近 CPU 核心数即可。

### Jetty 配置

```yaml
server:
  jetty:
    threads:
      max: 200
      min: 10
    connection-idle-timeout: 30s
    max-connections: 10000
```

### Undertow 配置

```yaml
server:
  undertow:
    threads:
      io: 8                # I/O 线程数（默认 CPU 核心数）
      worker: 256           # 工作线程数
    buffer-size: 1024
    direct-buffers: true
```

## SSL/TLS 配置

### 生成自签名证书

```bash
# 生成 PKCS12 格式的证书
keytool -genkeypair -alias codenow \
    -keyalg RSA -keysize 2048 \
    -storetype PKCS12 \
    -keystore keystore.p12 \
    -validity 3650 \
    -storepass changeit \
    -dname "CN=codenow.com, OU=IT, O=CodeNow, L=Beijing, ST=Beijing, C=CN"
```

### SSL 配置

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: codenow
    protocol: TLS
    enabled-protocols: TLSv1.2,TLSv1.3
  port: 8443
```

### JKS 格式证书

```bash
keytool -genkeypair -alias codenow \
    -keyalg RSA -keysize 2048 \
    -storetype JKS \
    -keystore keystore.jks \
    -validity 3650 \
    -storepass changeit
```

```yaml
server:
  ssl:
    key-store: classpath:keystore.jks
    key-store-password: changeit
    key-store-type: JKS
```

## HTTP 到 HTTPS 重定向

### Tomcat Connector 配置

```java
@Configuration
public class HttpsConfig {

    @Value("${server.port}")
    private int httpsPort;

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint constraint = new SecurityConstraint();
                constraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                constraint.addCollection(collection);
                context.addConstraint(constraint);
            }
        };

        factory.addAdditionalTomcatConnectors(httpConnector());
        return factory;
    }

    private Connector httpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(httpsPort);
        return connector;
    }
}
```

这样访问 HTTP 8080 端口会自动重定向到 HTTPS 8443 端口。

### 纯配置方式

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit

# Spring Boot 3.x 简化配置
server:
  http2:
    enabled: true
```

## 容器定制

### WebServerFactoryCustomizer

```java
@Component
public class TomcatCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        // 设置访问日志
        factory.addContextValves(createAccessLogValve());

        // 添加初始页面
        factory.addInitializers(new MyServletContextInitializer());
    }

    private AccessLogValve createAccessLogValve() {
        AccessLogValve valve = new AccessLogValve();
        valve.setDirectory("./logs");
        valve.setPrefix("access_log");
        valve.setSuffix(".log");
        valve.setPattern("%h %l %u %t \"%r\" %s %b %D");
        valve.setRotatable(true);
        valve.setMaxDays(30);
        return valve;
    }
}
```

### 嵌入式 Servlet 容器定制

```java
@Configuration
public class ContainerConfig {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer() {
        return factory -> {
            factory.setContextPath("/api");
            factory.setPort(8080);
            factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/404.html"));
            factory.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500.html"));
        };
    }
}
```

### 错误页面配置

```java
@Configuration
public class ErrorPageConfig {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> errorPageCustomizer() {
        return factory -> {
            factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/error/404"));
            factory.addErrorPages(new ErrorPage(HttpStatus.FORBIDDEN, "/error/403"));
            factory.addErrorPages(new ErrorPage(Throwable.class, "/error/500"));
        };
    }
}
```

## 静态资源处理

### 静态资源位置

```yaml
spring:
  web:
    resources:
      static-locations:
        - classpath:/static/
        - classpath:/public/
        - classpath:/resources/
        - file:/var/www/static/
```

默认静态资源位置（按优先级）：

1. `classpath:/META-INF/resources/`
2. `classpath:/resources/`
3. `classpath:/static/`
4. `classpath:/public/`

### 缓存策略

```yaml
spring:
  web:
    resources:
      cache:
        cachecontrol:
          max-age: 365d
          cache-public: true
        period: 365d
      chain:
        strategy:
          content:
            enabled: true
            paths: /**
          fixed:
            enabled: true
            version: 1.0.0
```

内容版本策略（文件内容变化时 URL 变化）：

```
/css/style.css → /css/style-abc123.css
```

固定版本策略：

```
/css/style.css → /css/style.css?v=1.0.0
```

### CDN 集成

```yaml
spring:
  web:
    resources:
      static-locations: file:/var/www/static/
      cache:
        cachecontrol:
          max-age: 365d
```

配置 Nginx 反向代理：

```nginx
location /static/ {
    alias /var/www/static/;
    expires 365d;
    add_header Cache-Control "public, immutable";
}
```

## Gzip 压缩

```yaml
server:
  compression:
    enabled: true
    mime-types: text/html,text/css,application/javascript,application/json,image/svg+xml
    min-response-size: 1024
    excluded-user-agents: "MSIE 6"
```

| 参数 | 默认值 | 说明 |
|------|--------|------|
| enabled | false | 是否启用压缩 |
| mime-types | text/html,text/xml,text/plain,text/css | 需要压缩的 MIME 类型 |
| min-response-size | 2048 | 最小压缩大小（字节） |
| excluded-user-agents | - | 排除的 User-Agent |

### Nginx 层压缩

在 Nginx 层做压缩更高效（不需要应用处理）：

```nginx
gzip on;
gzip_types text/plain text/css application/json application/javascript text/xml;
gzip_min_length 1024;
gzip_comp_level 6;
gzip_vary on;
```

## 多端口监听

### 管理端口与业务端口分离

```yaml
server:
  port: 8080        # 业务端口

management:
  server:
    port: 8081      # 管理端口
    address: 127.0.0.1  # 仅本机访问
  endpoints:
    web:
      base-path: /manage
```

这样 Actuator 端点走 8081 端口，可以做独立的访问控制和网络隔离。

### 应用健康检查配置

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

## Graceful Shutdown

优雅停机确保正在处理的请求能够正常完成，避免用户看到连接中断的错误。

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### 优雅停机流程

1. 收到停机信号（SIGTERM）
2. 停止接受新请求
3. 等待正在处理的请求完成（最多 30 秒）
4. 关闭连接池、释放资源
5. 应用退出

### 验证优雅停机

```bash
# 发送停机信号
kill -15 <pid>

# 观察日志
# "Commencing graceful shutdown. Waiting for active requests to complete"
# "Graceful shutdown complete"
```

### 与 Kubernetes 集成

```yaml
# Deployment 配置
spec:
  terminationGracePeriodSeconds: 60
  containers:
    - name: codenow
      lifecycle:
        preStop:
          exec:
            command: ["sh", "-c", "sleep 10"]
```

`preStop` 中的 sleep 确保 Pod 从 Service 的 Endpoints 中移除后再开始停机，避免流量继续打到正在关闭的实例。

## 容器监控

### Tomcat 线程池指标

Spring Boot Actuator 自动暴露 Tomcat 指标：

```
# 线程池
tomcat_threads_busy_threads    # 忙碌线程数
tomcat_threads_current_threads # 当前线程数
tomcat_threads_config_max      # 最大线程数

# 连接池
tomcat_connections_active       # 活跃连接数
tomcat_connections_accepted     # 已接受连接数
tomcat_connections_received     # 已接收连接数

# 请求
tomcat_sessions_active_current # 当前活跃会话数
tomcat_sessions_active_max     # 最大活跃会话数
```

### Prometheus 查询

```promql
# 线程使用率
tomcat_threads_busy_threads / tomcat_threads_config_max

# 连接使用率
tomcat_connections_active / tomcat_connections_config_max

# 请求处理中的数量
tomcat_threads_busy_threads
```

### Grafana 告警

```yaml
# 线程池告警
- alert: TomcatThreadPoolExhaustion
  expr: tomcat_threads_busy_threads / tomcat_threads_config_max > 0.8
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Tomcat 线程池使用率超过 80%"

# 连接数告警
- alert: HighConnectionCount
  expr: tomcat_connections_active > 5000
  for: 5m
  labels:
    severity: warning
```

### 自定义容器指标

```java
@Component
public class TomcatMetricsConfig {

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 获取 Tomcat 连接池
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        // 注册自定义指标...
    }
}
```

### JVM 指标监控

Spring Boot Actuator 同时暴露 JVM 指标：

```promql
# 堆内存使用
jvm_memory_used_bytes{area="heap"}

# GC 暂停时间
jvm_gc_pause_seconds_sum

# 线程数
jvm_threads_live_threads
```

### 监控最佳实践

| 指标 | 警告阈值 | 严重阈值 |
|------|----------|----------|
| 线程池使用率 | > 70% | > 90% |
| 连接池使用率 | > 80% | > 95% |
| 堆内存使用率 | > 75% | > 90% |
| GC 暂停时间 | > 500ms | > 1s |
| 活跃会话数 | > 8000 | > 10000 |

### 容器性能调优清单

```yaml
server:
  tomcat:
    # 连接配置
    max-connections: 10000
    accept-count: 200
    connection-timeout: 20s

    # 线程配置
    threads:
      max: 400
      min-spare: 50

    # 访问日志
    accesslog:
      enabled: true
      directory: ./logs
      pattern: "%h %t \"%r\" %s %b %D"
      max-days: 30

    # 静态资源缓存
    mbeanregistry:
      enabled: true
```