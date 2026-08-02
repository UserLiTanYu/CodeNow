#!/usr/bin/env python3
"""批量插入剩余 4 个模块的扩充文章"""
import sys
import os
import mysql.connector

sys.stdout.reconfigure(encoding='utf-8')

DB_CONFIG = {'host': 'localhost', 'port': 3306, 'user': 'root', 'password': '123456', 'database': 'codenow', 'charset': 'utf8mb4'}

# (category_id, sort, title, summary, filename)
NEW_ARTICLES = [
    # === 数据库 ===
    (33, 4, 'SQL 查询进阶与子查询', '窗口函数、CTE、递归查询、标量/关联子查询、EXISTS vs IN 性能对比', 'fe_db1.md'),
    (33, 5, '数据库设计与范式理论', '三大范式与反范式化、ER 模型、主键选择、索引设计、分库分表策略', 'fe_db2.md'),
    (34, 4, 'InnoDB 存储引擎与事务深入', 'InnoDB 架构（Buffer Pool/Redo/Undo Log）、事务隔离级别、MVCC 机制', 'fe_db3.md'),
    (34, 5, 'MySQL 锁机制与死锁排查', '行锁/间隙锁/临键锁、加锁规则、死锁排查、乐观锁与悲观锁', 'fe_db4.md'),
    (34, 6, 'MySQL 索引优化与慢查询分析', 'B+树、覆盖索引、索引下推、最左前缀、EXPLAIN 解读、慢查询日志', 'fe_db5.md'),
    (35, 4, 'MySQL 运维：备份恢复与高可用', 'mysqldump/xtrabackup、binlog 恢复、主从复制、MHA、ProxySQL', 'fe_db6.md'),
    (36, 4, 'Redis 核心数据结构与底层实现', 'SDS/quicklist/ziplist/skiplist 底层结构、内存优化与选型', 'fe_db7.md'),
    (36, 5, 'Redis 持久化、主从与集群', 'RDB/AOF/混合持久化、主从复制、哨兵模式、Cluster 分片', 'fe_db8.md'),
    (36, 6, 'Redis 缓存策略与分布式应用', '穿透/击穿/雪崩防护、分布式锁、延迟队列、Lua 限流', 'fe_db9.md'),
    (37, 4, 'MongoDB 聚合管道与索引优化', '聚合阶段、索引类型、explain 分析、分片集群架构', 'fe_db10.md'),
    (38, 4, '数据访问层设计与 ORM 实践', '连接池、ORM 对比、N+1 问题、批量优化、读写分离、Flyway', 'fe_db11.md'),
    (38, 5, '数据库安全与 SQL 注入防护', 'SQL 注入原理与防护、权限最小化、审计日志、数据脱敏', 'fe_db12.md'),
    # === 开发工具 ===
    (39, 4, 'Git Flow、GitHub Flow 与分支策略', 'Git Flow/GitHub Flow/GitLab Flow/Trunk-Based 对比、分支保护、PR 最佳实践', 'fe_git4.md'),
    (39, 5, 'Git 高级操作与问题恢复', 'stash/cherry-pick/bisect/reflog/交互式变基/worktree/submodule', 'fe_git5.md'),
    (40, 4, 'Dockerfile 最佳实践与多阶段构建', '分层缓存、多阶段构建、非 root 运行、镜像瘦身、安全扫描', 'fe_docker4.md'),
    (40, 5, 'Docker Compose 编排与网络模式', '服务依赖、网络模式、数据卷、环境变量、Compose profiles', 'fe_docker5.md'),
    (40, 6, 'Docker 安全与镜像仓库', '最小权限原则、seccomp/AppArmor、镜像签名、Harbor、漏洞扫描', 'fe_docker6.md'),
    (41, 4, 'IDEA 高级调试与性能分析', '条件断点、表达式求值、远程调试、Async Profiler、数据库工具', 'fe_idea4.md'),
    (42, 4, 'VS Code 远程开发与 Dev Container', 'Remote SSH/WSL/Container、Dev Containers、Codespaces、Settings Sync', 'fe_vscode4.md'),
    (43, 4, 'Maven 多模块项目与 Profile 管理', '父子模块、dependencyManagement、Profile、assembly 打包、Nexus', 'fe_maven4.md'),
    (43, 5, 'Gradle 构建与 Groovy/Kotlin DSL', 'Gradle vs Maven、build.gradle、Task 自定义、增量构建、多项目构建', 'fe_maven5.md'),
    (44, 4, 'Linux 网络排查与性能监控', 'netstat/ss/tcpdump、top/vmstat/iostat、lsof/strace', 'fe_linux4.md'),
    (44, 5, 'Shell 脚本编程与自动化', '变量/条件/循环、管道、正则/grep/sed/awk、crontab、运维脚本', 'fe_linux5.md'),
    (45, 4, 'GitHub Actions 工作流进阶', 'workflow 语法、矩阵构建、缓存、制品、密钥管理、自托管 Runner', 'fe_cicd4.md'),
    (45, 5, '制品管理与发布策略', '语义化版本、Docker 镜像标签、私有仓库、蓝绿/金丝雀/滚动发布', 'fe_cicd5.md'),
    (45, 6, 'CI/CD 流水线设计与安全', '流水线阶段设计、SAST/DAST/SCA 扫描、密钥防护、GitOps', 'fe_cicd6.md'),
    # === 项目实战 ===
    (46, 4, '需求分析与用户故事编写', '需求收集方法、用户故事格式、验收标准、MoSCoW 优先级、PRD 结构', 'fe_proj1.md'),
    (46, 5, '领域建模与技术方案设计', 'DDD 核心概念、限界上下文、技术方案模板、ADR、C4 模型', 'fe_proj2.md'),
    (47, 4, 'RESTful API 设计与版本控制', '资源命名、HTTP 方法语义、状态码、版本策略、OpenAPI 文档', 'fe_proj3.md'),
    (47, 5, '后端缓存策略与性能优化', '多级缓存架构、@Cacheable 注解、缓存一致性、热点数据处理', 'fe_proj4.md'),
    (48, 4, '前端状态管理与路由设计', '全局/局部/URL 状态、Pinia 模块化、路由守卫、动态路由、KeepAlive', 'fe_proj5.md'),
    (48, 5, '前端接口对接与错误处理', 'Axios 封装、API 模块化、Loading 管理、Token 刷新、Mock 方案', 'fe_proj6.md'),
    (49, 4, '前后端接口契约与联调流程', 'OpenAPI/Apifox、Mock Server、联调环境、CORS 处理、问题排查', 'fe_proj7.md'),
    (49, 5, '文件上传与实时通信实现', '分片上传、断点续传、OSS 直传、WebSocket 通知、SSE 推送', 'fe_proj8.md'),
    (50, 4, '测试策略与代码质量保障', '测试金字塔、JUnit+Mockito、Vitest、E2E 测试、代码评审、SonarQube', 'fe_proj9.md'),
    (50, 5, 'Docker Compose 全栈部署实战', 'Compose 编排、环境变量、健康检查、数据卷、Nginx 反代、验收脚本', 'fe_proj10.md'),
    (51, 4, '监控告警与运维手册', 'Prometheus+Grafana、ELK 日志、告警规则、运维手册、故障排查 SOP', 'fe_proj11.md'),
    (51, 5, '全栈项目安全加固清单', '认证鉴权审查、XSS/CSRF 防护、依赖扫描、HTTPS/CSP、日志脱敏', 'fe_proj12.md'),
    # === 学习随笔 ===
    (52, 4, '技术笔记方法与知识体系构建', '费曼学习法、Zettelkasten、Anki、技术博客写作、Obsidian/Notion', 'fe_learn3.md'),
    (52, 5, '高效编程习惯与开发环境优化', '快捷键、命令行效率、Git 别名、IDE 模板、自动化脚本、番茄工作法', 'fe_learn4.md'),
    (53, 3, 'Spring 与数据库面试高频题', 'IoC/AOP/Bean 生命周期、事务失效、MySQL 索引、Redis 缓存、分布式锁', 'fe_interview3.md'),
    (53, 4, '项目经验表达与系统设计面试', 'STAR 法则、系统设计框架、高并发/高可用方案表达、追问应对', 'fe_interview4.md'),
    (54, 4, '排序算法与二分查找', '七大排序算法对比、二分查找变体、时间空间复杂度分析', 'fe_algo3.md'),
    (54, 5, '动态规划与贪心算法', 'DP 思想、背包/LCS/编辑距离、贪心对比、区间调度', 'fe_algo4.md'),
    (54, 6, '图算法与高级数据结构', 'BFS/DFS/Dijkstra/拓扑排序、并查集/字典树/线段树/布隆过滤器', 'fe_algo5.md'),
    (55, 4, '创建型模式：工厂、建造者与原型', '简单工厂/工厂方法/抽象工厂、Builder/Prototype/单例、JDK/Spring 应用', 'fe_design3.md'),
    (55, 5, '结构型模式与行为型模式', '代理/适配器/装饰器/外观、策略/观察者/模板方法/责任链、选型决策', 'fe_design4.md'),
    (55, 6, '设计模式实战综合案例', 'Spring/MyBatis/Vue/React 中的设计模式、电商订单系统综合案例', 'fe_design5.md'),
    (56, 4, '操作系统：进程、线程与内存管理', '进程调度、进程间通信、虚拟内存、分页分段、页面置换、死锁', 'fe_cs3.md'),
    (56, 5, 'TCP/IP 协议栈与 HTTP 深入', 'TCP 握手挥手、流量拥塞控制、HTTP/1.1/2/3、HTTPS、DNS、CDN', 'fe_cs4.md'),
    (57, 4, '代码沟通与技术写作', '命名艺术、注释规范、README/API 文档、技术方案、博客写作、开源贡献', 'fe_career3.md'),
    (57, 5, '团队协作与敏捷开发', 'Scrum/看板、用户故事估算、持续集成、代码所有权、结对编程、技术债', 'fe_career4.md'),
    (57, 6, '开源参与与技术影响力', '选择项目、Issue/PR 流程、技术演讲、社区参与、个人品牌建设', 'fe_career5.md'),
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

    # 验证全部模块
    cursor.execute("""
        SELECT p.name as root, c.name as sub, COUNT(a.id) as cnt
        FROM blog_category c
        JOIN blog_category p ON c.parent_id = p.id AND p.is_deleted = 0
        LEFT JOIN blog_article a ON a.category_id = c.id AND a.is_deleted = 0
        WHERE c.is_deleted = 0 AND p.parent_id = 0 AND p.id IN (4,5,6,7)
        GROUP BY p.name, c.id, c.name, c.sort
        ORDER BY p.sort, c.sort
    """)
    current_root = None
    for root, sub, cnt in cursor.fetchall():
        if root != current_root:
            print(f"\n【{root}】")
            current_root = root
        print(f"  {sub:<25} {cnt} 篇")

    cursor.close()
    conn.close()

if __name__ == '__main__':
    main()
