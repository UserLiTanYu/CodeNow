# 监控告警与运维手册

系统上线只是开始，真正的挑战在于如何保证系统稳定运行。完善的监控体系能让我们在用户感知之前发现问题，而规范的运维手册则能帮助团队快速响应和处理故障。本文将介绍如何搭建监控告警系统，以及编写实用的运维手册。

## 监控体系架构

```
┌─────────────────────────────────────────────────────────────┐
│                        监控体系架构                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    │
│  │  应用指标    │    │  日志数据    │    │  链路追踪    │    │
│  │  Prometheus  │    │   ELK Stack │    │   SkyWalking │    │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘    │
│         │                  │                  │            │
│         └──────────────────┼──────────────────┘            │
│                            │                               │
│                     ┌──────┴──────┐                        │
│                     │   Grafana   │                        │
│                     │   可视化     │                        │
│                     └──────┬──────┘                        │
│                            │                               │
│                     ┌──────┴──────┐                        │
│                     │   告警系统   │                        │
│                     │   AlertManager │                     │
│                     └─────────────┘                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Spring Boot Actuator

### 配置Actuator

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,beans,mappings
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      show-components: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
```

### 自定义健康检查

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("connection", "valid")
                    .build();
            }
            return Health.down()
                .withDetail("database", "MySQL")
                .withDetail("connection", "invalid")
                .build();
        } catch (SQLException e) {
            return Health.down()
                .withDetail("database", "MySQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

@Component
public class RedisHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection().ping();
            if ("PONG".equals(pong)) {
                return Health.up()
                    .withDetail("redis", "connected")
                    .build();
            }
            return Health.down()
                .withDetail("redis", "unexpected response")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("redis", e.getMessage())
                .build();
        }
    }
}
```

### 自定义指标

```java
@Service
public class ArticleMetricsService {
    
    private final Counter articleCreatedCounter;
    private final Counter articleViewCounter;
    private final Timer articleQueryTimer;
    private final Gauge activeUsersGauge;
    
    public ArticleMetricsService(MeterRegistry registry) {
        // 文章创建计数
        this.articleCreatedCounter = Counter.builder("article.created.total")
            .description("文章创建总数")
            .tag("type", "article")
            .register(registry);
        
        // 文章浏览计数
        this.articleViewCounter = Counter.builder("article.views.total")
            .description("文章浏览总数")
            .register(registry);
        
        // 查询耗时
        this.articleQueryTimer = Timer.builder("article.query.duration")
            .description("文章查询耗时")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        // 活跃用户数
        this.activeUsersGauge = Gauge.builder("users.active.current", 
                this, ActiveUsersGauge::getValue)
            .description("当前活跃用户数")
            .register(registry);
    }
    
    public void incrementArticleCreated() {
        articleCreatedCounter.increment();
    }
    
    public void incrementArticleViews(Long articleId) {
        articleViewCounter.increment();
    }
    
    public <T> T recordQueryDuration(Supplier<T> supplier) {
        return articleQueryTimer.record(supplier);
    }
}
```

## Prometheus配置

```yaml
# prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

rule_files:
  - "rules/*.yml"

scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
        labels:
          application: 'codenow-backend'
          
  - job_name: 'nginx'
    static_configs:
      - targets: ['nginx-exporter:9113']
        labels:
          application: 'nginx'
          
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysqld-exporter:9104']
        labels:
          application: 'mysql'
          
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
        labels:
          application: 'redis'
```

## 告警规则

```yaml
# prometheus/rules/alerts.yml
groups:
  - name: application_alerts
    rules:
      # 应用实例宕机
      - alert: AppDown
        expr: up{job="spring-boot-app"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "应用实例宕机"
          description: "{{ $labels.instance }} 已宕机超过1分钟"
          
      # 响应时间过长
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API响应时间过长"
          description: "P95响应时间超过1秒，当前值: {{ $value }}s"
          
      # 错误率过高
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API错误率过高"
          description: "5分钟内错误率超过5%，当前值: {{ $value | humanizePercentage }}"
          
      # 内存使用过高
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM堆内存使用率过高"
          description: "堆内存使用率超过85%，当前值: {{ $value | humanizePercentage }}"
          
      # 数据库连接池耗尽
      - alert: DatabasePoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "数据库连接池即将耗尽"
          description: "连接池使用率超过90%"
          
  - name: infrastructure_alerts
    rules:
      # CPU使用率过高
      - alert: HighCpuUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU使用率过高"
          description: "{{ $labels.instance }} CPU使用率超过80%"
          
      # 磁盘空间不足
      - alert: LowDiskSpace
        expr: (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}) * 100 < 20
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "磁盘空间不足"
          description: "{{ $labels.instance }} 剩余磁盘空间不足20%"
          
      # MySQL宕机
      - alert: MySQLDown
        expr: mysql_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "MySQL服务宕机"
          description: "MySQL服务已宕机超过1分钟"
          
      # Redis宕机
      - alert: RedisDown
        expr: redis_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis服务宕机"
          description: "Redis服务已宕机超过1分钟"
```

## AlertManager配置

```yaml
# alertmanager/alertmanager.yml
global:
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alert@example.com'
  smtp_auth_username: 'alert@example.com'
  smtp_auth_password: 'password'

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'critical'
      repeat_interval: 5m
    - match:
        severity: warning
      receiver: 'warning'
      repeat_interval: 30m

receivers:
  - name: 'default'
    email_configs:
      - to: 'ops@example.com'
        
  - name: 'critical'
    email_configs:
      - to: 'ops@example.com'
    webhook_configs:
      - url: 'http://dingtalk-webhook:8060/dingtalk/ops/send'
        send_resolved: true
        
  - name: 'warning'
    email_configs:
      - to: 'dev@example.com'
```

## 日志采集

### ELK配置

```yaml
# docker-compose-elk.yml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
    networks:
      - elk

  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    container_name: logstash
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline
    ports:
      - "5044:5044"
    environment:
      - "LS_JAVA_OPTS=-Xms256m -Xmx256m"
    depends_on:
      - elasticsearch
    networks:
      - elk

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    container_name: kibana
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - elk

  filebeat:
    image: docker.elastic.co/beats/filebeat:8.11.0
    container_name: filebeat
    user: root
    volumes:
      - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    depends_on:
      - elasticsearch
    networks:
      - elk

volumes:
  elasticsearch_data:

networks:
  elk:
    driver: bridge
```

### Filebeat配置

```yaml
# filebeat/filebeat.yml
filebeat.inputs:
  - type: container
    paths:
      - '/var/lib/docker/containers/*/*.log'
    processors:
      - add_docker_metadata:
          host: "unix:///var/run/docker.sock"

processors:
  - decode_json_fields:
      fields: ["message"]
      target: "json"
      overwrite_keys: true

output.logstash:
  hosts: ["logstash:5044"]

logging.level: info
logging.to_stderr: true
```

### Logstash配置

```ruby
# logstash/pipeline/logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  if [container][name] =~ /codenow-backend/ {
    grok {
      match => { 
        "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{DATA:logger} - %{GREEDYDATA:message}" 
      }
    }
    
    date {
      match => [ "timestamp", "yyyy-MM-dd HH:mm:ss.SSS" ]
      target => "@timestamp"
    }
    
    mutate {
      remove_field => [ "timestamp" ]
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "codenow-logs-%{+YYYY.MM.dd}"
  }
}
```

## Grafana仪表盘

```json
{
  "dashboard": {
    "title": "码上记系统监控",
    "panels": [
      {
        "title": "请求速率",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      },
      {
        "title": "响应时间分布",
        "type": "heatmap",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_bucket[5m])",
            "legendFormat": "{{le}}"
          }
        ]
      },
      {
        "title": "JVM内存使用",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"}",
            "legendFormat": "已使用"
          },
          {
            "expr": "jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "最大值"
          }
        ]
      },
      {
        "title": "数据库连接池",
        "type": "gauge",
        "targets": [
          {
            "expr": "hikaricp_connections_active / hikaricp_connections_max * 100",
            "legendFormat": "使用率"
          }
        ]
      }
    ]
  }
}
```

## 运维手册模板

```markdown
# 系统运维手册

## 1. 系统概述

### 1.1 系统架构
- 前端：Vue 3 + Vite，部署在Nginx
- 后端：Spring Boot 3.x，JDK 21
- 数据库：MySQL 8.0
- 缓存：Redis 7.x
- 监控：Prometheus + Grafana

### 1.2 服务信息
| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80/443 | 反向代理 |
| Backend | 8080 | 后端API |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Prometheus | 9090 | 监控 |
| Grafana | 3000 | 可视化 |

## 2. 日常运维

### 2.1 服务启停
```bash
# 启动所有服务
docker compose up -d

# 停止所有服务
docker compose down

# 重启单个服务
docker compose restart backend

# 查看服务状态
docker compose ps

# 查看服务日志
docker compose logs -f backend
```

### 2.2 数据库维护
```bash
# 进入MySQL容器
docker compose exec mysql mysql -u root -p

# 备份数据库
docker compose exec mysql mysqldump -u root -p codenow > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker compose exec -T mysql mysql -u root -p codenow < backup.sql
```

### 2.3 Redis维护
```bash
# 进入Redis容器
docker compose exec redis redis-cli -a $REDIS_PASSWORD

# 查看内存使用
docker compose exec redis redis-cli -a $REDIS_PASSWORD info memory

# 清空缓存
docker compose exec redis redis-cli -a $REDIS_PASSWORD flushdb
```

## 3. 故障排查

### 3.1 服务无法启动
1. 检查日志：`docker compose logs backend`
2. 检查端口占用：`netstat -tlnp | grep 8080`
3. 检查环境变量：确认.env文件配置正确
4. 检查依赖服务：确认MySQL和Redis已启动

### 3.2 接口响应慢
1. 查看Grafana监控，确认瓶颈位置
2. 检查数据库慢查询日志
3. 检查Redis缓存命中率
4. 检查服务器CPU/内存使用

### 3.3 数据库连接异常
1. 检查连接池配置
2. 检查MySQL最大连接数：`show variables like 'max_connections'`
3. 检查当前连接数：`show processlist`
4. 检查网络连通性

## 4. 备份恢复

### 4.1 备份策略
- 数据库：每日全量备份，保留7天
- Redis：开启AOF持久化
- 上传文件：每日增量备份

### 4.2 备份脚本
```bash
#!/bin/bash
BACKUP_DIR="/backup/$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR

# 备份MySQL
docker compose exec mysql mysqldump -u root -p$DB_PASSWORD codenow | gzip > $BACKUP_DIR/mysql.sql.gz

# 备份上传文件
tar czf $BACKUP_DIR/uploads.tar.gz /path/to/uploads

# 清理7天前的备份
find /backup -type d -mtime +7 -exec rm -rf {} +
```

### 4.3 恢复流程
1. 停止应用服务
2. 恢复数据库
3. 恢复上传文件
4. 启动服务并验证

## 5. 扩容方案

### 5.1 水平扩容
```yaml
# docker-compose.scale.yml
services:
  backend:
    deploy:
      replicas: 3
```

### 5.2 垂直扩容
- 增加JVM内存：修改JAVA_OPTS
- 增加MySQL缓冲池：修改innodb_buffer_pool_size
- 增加Redis内存：修改maxmemory

## 6. 安全加固

### 6.1 定期更新
- 每月更新系统补丁
- 每季度更新依赖版本
- 及时修复安全漏洞

### 6.2 访问控制
- 限制SSH访问IP
- 使用密钥登录
- 定期轮换密码
```

监控告警和运维手册是系统稳定运行的保障。监控让我们能够及时发现问题，告警让我们能够快速响应，运维手册让我们能够规范处理。三者缺一不可，需要在系统上线前就准备好。
