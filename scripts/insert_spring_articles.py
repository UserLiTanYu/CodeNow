#!/usr/bin/env python3
"""批量插入 Spring 全家桶扩充文章"""
import sys
import os
import mysql.connector

sys.stdout.reconfigure(encoding='utf-8')

DB_CONFIG = {'host': 'localhost', 'port': 3306, 'user': 'root', 'password': '123456', 'database': 'codenow', 'charset': 'utf8mb4'}

# (category_id, sort, title, summary, filename)
NEW_ARTICLES = [
    # Spring IoC 与 AOP (id=18) +3
    (18, 4, '@Configuration 与条件装配', '@Configuration 配置类、@Bean 注解、组件扫描、条件装配注解（@Conditional 系列）、@Profile 环境隔离、@ConfigurationProperties 属性绑定', 'spring_ioc4.md'),
    (18, 5, '事件机制与 SpEL 表达式', 'Spring 事件模型、自定义事件、@EventListener、事务事件、SpEL 语法与在 @Value 中的使用、集合投影与过滤', 'spring_ioc5.md'),
    (18, 6, 'Spring Validation 数据校验', 'Bean Validation 注解、分组校验、嵌套校验、自定义校验注解、Controller 层校验、统一校验异常处理', 'spring_ioc6.md'),
    # Spring MVC (id=19) +2
    (19, 4, 'CORS、内容协商与 RESTful 设计', '跨域配置、内容协商机制、Jackson 序列化、RESTful 设计原则、API 版本控制、统一响应封装、Swagger 文档', 'spring_mvc4.md'),
    (19, 5, 'WebSocket 与异步请求处理', 'Servlet 异步处理、SSE 推送、WebSocket 与 STOMP 协议、消息代理、前端连接、认证与实际应用', 'spring_mvc5.md'),
    # Spring Boot (id=20) +3
    (20, 4, 'Actuator、健康检查与生产就绪', 'Actuator 端点详解、自定义健康检查、Micrometer 指标收集、Prometheus 集成、安全配置与生产最佳实践', 'spring_boot4.md'),
    (20, 5, '日志体系与调试技巧', 'SLF4J + Logback 架构、日志级别与格式、文件滚动策略、多环境日志、结构化日志、调试技巧与 DevTools', 'spring_boot5.md'),
    (20, 6, '嵌入式容器与 SSL 配置', 'Tomcat/Jetty/Undertow 选择、容器配置、SSL/TLS 配置、HTTP 到 HTTPS 重定向、Gzip 压缩、优雅停机', 'spring_boot6.md'),
    # MyBatis 与 MyBatis-Plus (id=21) +2
    (21, 4, '多表关联查询与嵌套结果映射', 'resultMap 详解、association 一对一、collection 一对多、N+1 问题、discriminator 鉴别器、实战三表关联', 'spring_mybatis4.md'),
    (21, 5, '性能优化：缓存、懒加载与 SQL 监控', '一级/二级缓存、延迟加载、SQL 监控、连接池优化、批量操作、索引优化建议', 'spring_mybatis5.md'),
    # 认证与权限 (id=22) +2
    (22, 4, 'OAuth2 与第三方登录集成', 'OAuth2 四种授权模式、授权码流程、Spring Security OAuth2 Client、GitHub 登录集成、JWT 与 OAuth2 结合', 'spring_auth4.md'),
    (22, 5, '接口安全：限流、加密与防重放', '限流算法与实现、防重放机制、请求签名、数据加密（AES/RSA）、HTTPS 配置、SQL 注入与 XSS 防护', 'spring_auth5.md'),
    # Spring Cloud (id=23) +1
    (23, 4, '微服务安全与可观测性实战', '网关统一鉴权、JWT 透传、mTLS 服务间认证、分布式链路追踪、ELK 日志采集、Prometheus 告警、Service Mesh', 'spring_cloud4.md'),
    # 消息与任务 (id=24) +1
    (24, 4, '延迟队列、死信队列与消息幂等性', '延迟队列实现、死信队列配置、消息幂等方案（唯一ID/乐观锁/状态机）、消息可靠性全链路', 'spring_mq4.md'),
    # 测试与监控 (id=25) +2
    (25, 3, 'Actuator、Micrometer 与可观测性', '可观测性三支柱、Micrometer 指标类型、自定义业务指标、Prometheus 集成、Grafana 仪表盘、分布式链路追踪', 'spring_test3.md'),
    (25, 4, 'Logback 日志配置与链路追踪', 'Logback 架构、logback-spring.xml 配置、MDC 与 traceId、结构化日志、ELK 日志采集与索引', 'spring_test4.md'),
]

AUTHOR_ID = 1

def main():
    articles_dir = os.path.join(os.path.dirname(__file__), 'articles')
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()

    inserted = 0
    skipped = []
    for cat_id, sort, title, summary, filename in NEW_ARTICLES:
        filepath = os.path.join(articles_dir, filename)
        if not os.path.exists(filepath):
            skipped.append(filename)
            continue
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read().strip()
        if not content:
            skipped.append(filename)
            continue
        cursor.execute(
            "INSERT INTO blog_article (title, content, summary, category_id, author_id, status, sort, view_count, create_time, update_time, is_deleted) "
            "VALUES (%s, %s, %s, %s, %s, 1, %s, 0, NOW(), NOW(), 0)",
            (title, content, summary, cat_id, AUTHOR_ID, sort)
        )
        inserted += 1
        print(f"  [OK] {title}")

    conn.commit()
    print(f"\n共插入 {inserted} 篇文章")
    if skipped:
        print(f"跳过: {skipped}")

    # 验证
    cursor.execute("""
        SELECT c.name, COUNT(a.id) as cnt
        FROM blog_category c
        LEFT JOIN blog_article a ON a.category_id = c.id AND a.is_deleted = 0
        WHERE c.is_deleted = 0 AND c.parent_id = 2
        GROUP BY c.id, c.name, c.sort
        ORDER BY c.sort
    """)
    print(f"\n{'分类':<25} {'文章数':<6}")
    print("-" * 35)
    for name, cnt in cursor.fetchall():
        print(f"{name:<25} {cnt:<6}")

    cursor.close()
    conn.close()

if __name__ == '__main__':
    main()
