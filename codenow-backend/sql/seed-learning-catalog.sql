-- CodeNow 学习目录初始化（会备份并物理删除现有文章及其关联数据）
-- 由 scripts/generate-learning-catalog.mjs 生成，请先执行 migration-category-hierarchy.sql。
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
CREATE TABLE IF NOT EXISTS backup_blog_article_20260717 LIKE blog_article;
INSERT INTO backup_blog_article_20260717 SELECT * FROM blog_article WHERE NOT EXISTS (SELECT 1 FROM backup_blog_article_20260717);
CREATE TABLE IF NOT EXISTS backup_blog_article_tag_20260717 LIKE blog_article_tag;
INSERT INTO backup_blog_article_tag_20260717 SELECT * FROM blog_article_tag WHERE NOT EXISTS (SELECT 1 FROM backup_blog_article_tag_20260717);
DELETE FROM article_favorite WHERE article_id IS NOT NULL;
DELETE FROM user_notification WHERE article_id IS NOT NULL;
DELETE FROM blog_comment WHERE article_id IS NOT NULL;
DELETE FROM blog_article_tag;
DELETE FROM blog_article;
ALTER TABLE blog_article AUTO_INCREMENT = 1;
ALTER TABLE blog_article_tag AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;

UPDATE blog_category SET description='从环境搭建到 JVM 与现代 Java 的系统学习路线', parent_id=0, sort=1, is_deleted=0 WHERE name='Java 基础';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Java 入门与环境', 'Java 开发环境、工具链与第一个程序', p.id, 1, 0 FROM blog_category p
WHERE p.name='Java 基础' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Java 入门与环境');
UPDATE blog_category c JOIN blog_category p ON p.name='Java 基础'
SET c.description='Java 开发环境、工具链与第一个程序', c.parent_id=p.id, c.sort=1, c.is_deleted=0 WHERE c.name='Java 入门与环境';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Java 基础语法', '变量、运算符、流程控制与数组', p.id, 2, 0 FROM blog_category p
WHERE p.name='Java 基础' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Java 基础语法');
UPDATE blog_category c JOIN blog_category p ON p.name='Java 基础'
SET c.description='变量、运算符、流程控制与数组', c.parent_id=p.id, c.sort=2, c.is_deleted=0 WHERE c.name='Java 基础语法';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '面向对象编程', '类、对象、封装、继承与多态', p.id, 3, 0 FROM blog_category p
WHERE p.name='Java 基础' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='面向对象编程');
UPDATE blog_category c JOIN blog_category p ON p.name='Java 基础'
SET c.description='类、对象、封装、继承与多态', c.parent_id=p.id, c.sort=3, c.is_deleted=0 WHERE c.name='面向对象编程';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Java 常用 API', '字符串、时间、数学与工具类', p.id, 4, 0 FROM blog_category p
WHERE p.name='Java 基础' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Java 常用 API');
UPDATE blog_category c JOIN blog_category p ON p.name='Java 基础'
SET c.description='字符串、时间、数学与工具类', c.parent_id=p.id, c.sort=4, c.is_deleted=0 WHERE c.name='Java 常用 API';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '集合框架', 'List、Set、Map 及集合选型', p.id, 5, 0 FROM blog_category p
WHERE p.name='Java 基础' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='集合框架');
UPDATE blog_category c JOIN blog_category p ON p.name='Java 基础'
SET c.description='List、Set、Map 及集合选型', c.parent_id=p.id, c.sort=5, c.is_deleted=0 WHERE c.name='集合框架';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '异常与泛型', '可靠错误处理与类型安全', p.id, 6, 0 FROM blog_category p
WHERE p.name='Java 基础' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='异常与泛型');
UPDATE blog_category c JOIN blog_category p ON p.name='Java 基础'
SET c.description='可靠错误处理与类型安全', c.parent_id=p.id, c.sort=6, c.is_deleted=0 WHERE c.name='异常与泛型';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'IO 与 NIO', '文件、流、序列化与 NIO', p.id, 7, 0 FROM blog_category p
WHERE p.name='Java 基础' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='IO 与 NIO');
UPDATE blog_category c JOIN blog_category p ON p.name='Java 基础'
SET c.description='文件、流、序列化与 NIO', c.parent_id=p.id, c.sort=7, c.is_deleted=0 WHERE c.name='IO 与 NIO';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '多线程与并发', '线程、锁、线程池与并发工具', p.id, 8, 0 FROM blog_category p
WHERE p.name='Java 基础' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='多线程与并发');
UPDATE blog_category c JOIN blog_category p ON p.name='Java 基础'
SET c.description='线程、锁、线程池与并发工具', c.parent_id=p.id, c.sort=8, c.is_deleted=0 WHERE c.name='多线程与并发';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'JVM 基础', '内存模型、类加载与垃圾回收', p.id, 9, 0 FROM blog_category p
WHERE p.name='Java 基础' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='JVM 基础');
UPDATE blog_category c JOIN blog_category p ON p.name='Java 基础'
SET c.description='内存模型、类加载与垃圾回收', c.parent_id=p.id, c.sort=9, c.is_deleted=0 WHERE c.name='JVM 基础';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '现代 Java', 'Lambda、Stream、模块与新版本特性', p.id, 10, 0 FROM blog_category p
WHERE p.name='Java 基础' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='现代 Java');
UPDATE blog_category c JOIN blog_category p ON p.name='Java 基础'
SET c.description='Lambda、Stream、模块与新版本特性', c.parent_id=p.id, c.sort=10, c.is_deleted=0 WHERE c.name='现代 Java';
UPDATE blog_category SET description='Spring Framework、Boot、Cloud 与企业级开发路线', parent_id=0, sort=2, is_deleted=0 WHERE name='Spring 全家桶';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Spring IoC 与 AOP', '容器、依赖注入、Bean 与切面编程', p.id, 1, 0 FROM blog_category p
WHERE p.name='Spring 全家桶' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Spring IoC 与 AOP');
UPDATE blog_category c JOIN blog_category p ON p.name='Spring 全家桶'
SET c.description='容器、依赖注入、Bean 与切面编程', c.parent_id=p.id, c.sort=1, c.is_deleted=0 WHERE c.name='Spring IoC 与 AOP';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Spring MVC', 'Web 请求链路、参数、校验与异常处理', p.id, 2, 0 FROM blog_category p
WHERE p.name='Spring 全家桶' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Spring MVC');
UPDATE blog_category c JOIN blog_category p ON p.name='Spring 全家桶'
SET c.description='Web 请求链路、参数、校验与异常处理', c.parent_id=p.id, c.sort=2, c.is_deleted=0 WHERE c.name='Spring MVC';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Spring Boot', '自动配置、配置管理与应用开发', p.id, 3, 0 FROM blog_category p
WHERE p.name='Spring 全家桶' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Spring Boot');
UPDATE blog_category c JOIN blog_category p ON p.name='Spring 全家桶'
SET c.description='自动配置、配置管理与应用开发', c.parent_id=p.id, c.sort=3, c.is_deleted=0 WHERE c.name='Spring Boot';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'MyBatis 与 MyBatis-Plus', '持久层映射、动态 SQL 与工程实践', p.id, 4, 0 FROM blog_category p
WHERE p.name='Spring 全家桶' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='MyBatis 与 MyBatis-Plus');
UPDATE blog_category c JOIN blog_category p ON p.name='Spring 全家桶'
SET c.description='持久层映射、动态 SQL 与工程实践', c.parent_id=p.id, c.sort=4, c.is_deleted=0 WHERE c.name='MyBatis 与 MyBatis-Plus';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '认证与权限', 'Spring Security、Sa-Token 与权限模型', p.id, 5, 0 FROM blog_category p
WHERE p.name='Spring 全家桶' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='认证与权限');
UPDATE blog_category c JOIN blog_category p ON p.name='Spring 全家桶'
SET c.description='Spring Security、Sa-Token 与权限模型', c.parent_id=p.id, c.sort=5, c.is_deleted=0 WHERE c.name='认证与权限';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Spring Cloud', '微服务治理、配置与调用链路', p.id, 6, 0 FROM blog_category p
WHERE p.name='Spring 全家桶' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Spring Cloud');
UPDATE blog_category c JOIN blog_category p ON p.name='Spring 全家桶'
SET c.description='微服务治理、配置与调用链路', c.parent_id=p.id, c.sort=6, c.is_deleted=0 WHERE c.name='Spring Cloud';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '消息与任务', '消息队列、定时任务与异步处理', p.id, 7, 0 FROM blog_category p
WHERE p.name='Spring 全家桶' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='消息与任务');
UPDATE blog_category c JOIN blog_category p ON p.name='Spring 全家桶'
SET c.description='消息队列、定时任务与异步处理', c.parent_id=p.id, c.sort=7, c.is_deleted=0 WHERE c.name='消息与任务';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Spring 测试与监控', '自动化测试、日志、指标与健康检查', p.id, 8, 0 FROM blog_category p
WHERE p.name='Spring 全家桶' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Spring 测试与监控');
UPDATE blog_category c JOIN blog_category p ON p.name='Spring 全家桶'
SET c.description='自动化测试、日志、指标与健康检查', c.parent_id=p.id, c.sort=8, c.is_deleted=0 WHERE c.name='Spring 测试与监控';
UPDATE blog_category SET description='从 Web 基础到 Vue、React 与工程化的前端路线', parent_id=0, sort=3, is_deleted=0 WHERE name='前端开发';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'HTML 与 CSS', '语义化页面、布局、响应式与可访问性', p.id, 1, 0 FROM blog_category p
WHERE p.name='前端开发' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='HTML 与 CSS');
UPDATE blog_category c JOIN blog_category p ON p.name='前端开发'
SET c.description='语义化页面、布局、响应式与可访问性', c.parent_id=p.id, c.sort=1, c.is_deleted=0 WHERE c.name='HTML 与 CSS';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'JavaScript 基础', '语言基础、对象、异步与浏览器 API', p.id, 2, 0 FROM blog_category p
WHERE p.name='前端开发' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='JavaScript 基础');
UPDATE blog_category c JOIN blog_category p ON p.name='前端开发'
SET c.description='语言基础、对象、异步与浏览器 API', c.parent_id=p.id, c.sort=2, c.is_deleted=0 WHERE c.name='JavaScript 基础';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'TypeScript', '类型系统、泛型与工程应用', p.id, 3, 0 FROM blog_category p
WHERE p.name='前端开发' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='TypeScript');
UPDATE blog_category c JOIN blog_category p ON p.name='前端开发'
SET c.description='类型系统、泛型与工程应用', c.parent_id=p.id, c.sort=3, c.is_deleted=0 WHERE c.name='TypeScript';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Vue 3', '组合式 API、组件、状态与路由', p.id, 4, 0 FROM blog_category p
WHERE p.name='前端开发' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Vue 3');
UPDATE blog_category c JOIN blog_category p ON p.name='前端开发'
SET c.description='组合式 API、组件、状态与路由', c.parent_id=p.id, c.sort=4, c.is_deleted=0 WHERE c.name='Vue 3';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Element Plus', '后台界面组件与交互实践', p.id, 5, 0 FROM blog_category p
WHERE p.name='前端开发' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Element Plus');
UPDATE blog_category c JOIN blog_category p ON p.name='前端开发'
SET c.description='后台界面组件与交互实践', c.parent_id=p.id, c.sort=5, c.is_deleted=0 WHERE c.name='Element Plus';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'React', '组件、Hooks、状态与路由', p.id, 6, 0 FROM blog_category p
WHERE p.name='前端开发' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='React');
UPDATE blog_category c JOIN blog_category p ON p.name='前端开发'
SET c.description='组件、Hooks、状态与路由', c.parent_id=p.id, c.sort=6, c.is_deleted=0 WHERE c.name='React';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '前端工程化', 'Vite、测试、规范、性能与部署', p.id, 7, 0 FROM blog_category p
WHERE p.name='前端开发' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='前端工程化');
UPDATE blog_category c JOIN blog_category p ON p.name='前端开发'
SET c.description='Vite、测试、规范、性能与部署', c.parent_id=p.id, c.sort=7, c.is_deleted=0 WHERE c.name='前端工程化';
UPDATE blog_category SET description='数据建模、SQL、MySQL、Redis 与 NoSQL 路线', parent_id=0, sort=4, is_deleted=0 WHERE name='数据库';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '数据库与 SQL 基础', '关系模型、SQL 查询与数据库设计', p.id, 1, 0 FROM blog_category p
WHERE p.name='数据库' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='数据库与 SQL 基础');
UPDATE blog_category c JOIN blog_category p ON p.name='数据库'
SET c.description='关系模型、SQL 查询与数据库设计', c.parent_id=p.id, c.sort=1, c.is_deleted=0 WHERE c.name='数据库与 SQL 基础';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'MySQL 核心', '存储引擎、事务、锁与索引', p.id, 2, 0 FROM blog_category p
WHERE p.name='数据库' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='MySQL 核心');
UPDATE blog_category c JOIN blog_category p ON p.name='数据库'
SET c.description='存储引擎、事务、锁与索引', c.parent_id=p.id, c.sort=2, c.is_deleted=0 WHERE c.name='MySQL 核心';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'MySQL 运维', '备份恢复、复制、分库分表与监控', p.id, 3, 0 FROM blog_category p
WHERE p.name='数据库' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='MySQL 运维');
UPDATE blog_category c JOIN blog_category p ON p.name='数据库'
SET c.description='备份恢复、复制、分库分表与监控', c.parent_id=p.id, c.sort=3, c.is_deleted=0 WHERE c.name='MySQL 运维';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Redis', '数据结构、持久化、缓存与分布式能力', p.id, 4, 0 FROM blog_category p
WHERE p.name='数据库' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Redis');
UPDATE blog_category c JOIN blog_category p ON p.name='数据库'
SET c.description='数据结构、持久化、缓存与分布式能力', c.parent_id=p.id, c.sort=4, c.is_deleted=0 WHERE c.name='Redis';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'MongoDB', '文档模型、查询、索引与复制集', p.id, 5, 0 FROM blog_category p
WHERE p.name='数据库' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='MongoDB');
UPDATE blog_category c JOIN blog_category p ON p.name='数据库'
SET c.description='文档模型、查询、索引与复制集', c.parent_id=p.id, c.sort=5, c.is_deleted=0 WHERE c.name='MongoDB';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '数据访问实践', '连接池、ORM、迁移与数据安全', p.id, 6, 0 FROM blog_category p
WHERE p.name='数据库' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='数据访问实践');
UPDATE blog_category c JOIN blog_category p ON p.name='数据库'
SET c.description='连接池、ORM、迁移与数据安全', c.parent_id=p.id, c.sort=6, c.is_deleted=0 WHERE c.name='数据访问实践';
UPDATE blog_category SET description='版本控制、容器、IDE、构建、Linux 与自动化工具链', parent_id=0, sort=5, is_deleted=0 WHERE name='开发工具';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Git 教程', '版本控制、分支协作与问题恢复', p.id, 1, 0 FROM blog_category p
WHERE p.name='开发工具' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Git 教程');
UPDATE blog_category c JOIN blog_category p ON p.name='开发工具'
SET c.description='版本控制、分支协作与问题恢复', c.parent_id=p.id, c.sort=1, c.is_deleted=0 WHERE c.name='Git 教程';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Docker 教程', '镜像、容器、网络、存储与编排', p.id, 2, 0 FROM blog_category p
WHERE p.name='开发工具' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Docker 教程');
UPDATE blog_category c JOIN blog_category p ON p.name='开发工具'
SET c.description='镜像、容器、网络、存储与编排', c.parent_id=p.id, c.sort=2, c.is_deleted=0 WHERE c.name='Docker 教程';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'IntelliJ IDEA', 'Java 开发环境、调试与效率配置', p.id, 3, 0 FROM blog_category p
WHERE p.name='开发工具' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='IntelliJ IDEA');
UPDATE blog_category c JOIN blog_category p ON p.name='开发工具'
SET c.description='Java 开发环境、调试与效率配置', c.parent_id=p.id, c.sort=3, c.is_deleted=0 WHERE c.name='IntelliJ IDEA';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'VS Code', '编辑器配置、调试与远程开发', p.id, 4, 0 FROM blog_category p
WHERE p.name='开发工具' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='VS Code');
UPDATE blog_category c JOIN blog_category p ON p.name='开发工具'
SET c.description='编辑器配置、调试与远程开发', c.parent_id=p.id, c.sort=4, c.is_deleted=0 WHERE c.name='VS Code';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Maven 与 Gradle', '依赖、构建、测试与多模块项目', p.id, 5, 0 FROM blog_category p
WHERE p.name='开发工具' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Maven 与 Gradle');
UPDATE blog_category c JOIN blog_category p ON p.name='开发工具'
SET c.description='依赖、构建、测试与多模块项目', c.parent_id=p.id, c.sort=5, c.is_deleted=0 WHERE c.name='Maven 与 Gradle';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Linux 教程', '命令行、权限、进程、网络与服务', p.id, 6, 0 FROM blog_category p
WHERE p.name='开发工具' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Linux 教程');
UPDATE blog_category c JOIN blog_category p ON p.name='开发工具'
SET c.description='命令行、权限、进程、网络与服务', c.parent_id=p.id, c.sort=6, c.is_deleted=0 WHERE c.name='Linux 教程';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'CI/CD', '自动测试、制品、发布与回滚', p.id, 7, 0 FROM blog_category p
WHERE p.name='开发工具' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='CI/CD');
UPDATE blog_category c JOIN blog_category p ON p.name='开发工具'
SET c.description='自动测试、制品、发布与回滚', c.parent_id=p.id, c.sort=7, c.is_deleted=0 WHERE c.name='CI/CD';
UPDATE blog_category SET description='从需求、架构、开发、测试到部署的完整项目路线', parent_id=0, sort=6, is_deleted=0 WHERE name='项目实战';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '需求与架构设计', '需求分析、领域建模与技术方案', p.id, 1, 0 FROM blog_category p
WHERE p.name='项目实战' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='需求与架构设计');
UPDATE blog_category c JOIN blog_category p ON p.name='项目实战'
SET c.description='需求分析、领域建模与技术方案', c.parent_id=p.id, c.sort=1, c.is_deleted=0 WHERE c.name='需求与架构设计';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '后端项目实战', '接口、持久层、权限与工程质量', p.id, 2, 0 FROM blog_category p
WHERE p.name='项目实战' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='后端项目实战');
UPDATE blog_category c JOIN blog_category p ON p.name='项目实战'
SET c.description='接口、持久层、权限与工程质量', c.parent_id=p.id, c.sort=2, c.is_deleted=0 WHERE c.name='后端项目实战';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '前端项目实战', '页面、状态、接口与用户体验', p.id, 3, 0 FROM blog_category p
WHERE p.name='项目实战' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='前端项目实战');
UPDATE blog_category c JOIN blog_category p ON p.name='项目实战'
SET c.description='页面、状态、接口与用户体验', c.parent_id=p.id, c.sort=3, c.is_deleted=0 WHERE c.name='前端项目实战';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '全栈联调', '接口契约、认证、文件与实时功能', p.id, 4, 0 FROM blog_category p
WHERE p.name='项目实战' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='全栈联调');
UPDATE blog_category c JOIN blog_category p ON p.name='项目实战'
SET c.description='接口契约、认证、文件与实时功能', c.parent_id=p.id, c.sort=4, c.is_deleted=0 WHERE c.name='全栈联调';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '测试与质量保障', '测试策略、安全、性能与代码评审', p.id, 5, 0 FROM blog_category p
WHERE p.name='项目实战' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='测试与质量保障');
UPDATE blog_category c JOIN blog_category p ON p.name='项目实战'
SET c.description='测试策略、安全、性能与代码评审', c.parent_id=p.id, c.sort=5, c.is_deleted=0 WHERE c.name='测试与质量保障';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '部署与运维', '容器化、配置、发布与监控', p.id, 6, 0 FROM blog_category p
WHERE p.name='项目实战' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='部署与运维');
UPDATE blog_category c JOIN blog_category p ON p.name='项目实战'
SET c.description='容器化、配置、发布与监控', c.parent_id=p.id, c.sort=6, c.is_deleted=0 WHERE c.name='部署与运维';
UPDATE blog_category SET description='学习方法、面试、算法、设计模式与计算机基础', parent_id=0, sort=7, is_deleted=0 WHERE name='学习随笔';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '学习方法', '路线规划、知识管理与刻意练习', p.id, 1, 0 FROM blog_category p
WHERE p.name='学习随笔' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='学习方法');
UPDATE blog_category c JOIN blog_category p ON p.name='学习随笔'
SET c.description='路线规划、知识管理与刻意练习', c.parent_id=p.id, c.sort=1, c.is_deleted=0 WHERE c.name='学习方法';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT 'Java 面试', '基础、高并发、JVM 与项目表达', p.id, 2, 0 FROM blog_category p
WHERE p.name='学习随笔' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='Java 面试');
UPDATE blog_category c JOIN blog_category p ON p.name='学习随笔'
SET c.description='基础、高并发、JVM 与项目表达', c.parent_id=p.id, c.sort=2, c.is_deleted=0 WHERE c.name='Java 面试';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '算法与数据结构', '复杂度、线性结构、树图与算法思想', p.id, 3, 0 FROM blog_category p
WHERE p.name='学习随笔' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='算法与数据结构');
UPDATE blog_category c JOIN blog_category p ON p.name='学习随笔'
SET c.description='复杂度、线性结构、树图与算法思想', c.parent_id=p.id, c.sort=3, c.is_deleted=0 WHERE c.name='算法与数据结构';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '设计模式', '创建型、结构型与行为型模式', p.id, 4, 0 FROM blog_category p
WHERE p.name='学习随笔' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='设计模式');
UPDATE blog_category c JOIN blog_category p ON p.name='学习随笔'
SET c.description='创建型、结构型与行为型模式', c.parent_id=p.id, c.sort=4, c.is_deleted=0 WHERE c.name='设计模式';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '计算机基础', '操作系统、网络与组成原理', p.id, 5, 0 FROM blog_category p
WHERE p.name='学习随笔' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='计算机基础');
UPDATE blog_category c JOIN blog_category p ON p.name='学习随笔'
SET c.description='操作系统、网络与组成原理', c.parent_id=p.id, c.sort=5, c.is_deleted=0 WHERE c.name='计算机基础';
INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '职业成长', '简历、沟通、开源与长期成长', p.id, 6, 0 FROM blog_category p
WHERE p.name='学习随笔' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='职业成长');
UPDATE blog_category c JOIN blog_category p ON p.name='学习随笔'
SET c.description='简历、沟通、开源与长期成长', c.parent_id=p.id, c.sort=6, c.is_deleted=0 WHERE c.name='职业成长';

INSERT INTO blog_tag (name, is_deleted) VALUES ('Java', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Java 入门与环境', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Java 基础语法', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('面向对象编程', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Java 常用 API', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('集合框架', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('异常与泛型', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('IO 与 NIO', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('多线程与并发', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('JVM 基础', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('现代 Java', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Spring Boot', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Spring IoC 与 AOP', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Spring MVC', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('MyBatis 与 MyBatis-Plus', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('认证与权限', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Spring Cloud', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('消息与任务', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Spring 测试与监控', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('JavaScript', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('HTML 与 CSS', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('JavaScript 基础', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('TypeScript', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Vue 3', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Element Plus', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('React', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('前端工程化', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('MySQL', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('数据库与 SQL 基础', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('MySQL 核心', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('MySQL 运维', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Redis', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('MongoDB', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('数据访问实践', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Git', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Docker', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('IntelliJ IDEA', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('VS Code', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Maven 与 Gradle', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Linux', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('CI/CD', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('需求与架构设计', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('后端项目实战', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('前端项目实战', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('全栈联调', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('测试与质量保障', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('部署与运维', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('学习方法', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('Java 面试', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('算法与数据结构', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('设计模式', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('计算机基础', 0) ON DUPLICATE KEY UPDATE is_deleted=0;
INSERT INTO blog_tag (name, is_deleted) VALUES ('职业成长', 0) ON DUPLICATE KEY UPDATE is_deleted=0;

INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Java 开发环境与 JDK 安装', '# 01.Java 开发环境与 JDK 安装

> 正文内容待完善。

系统讲解Java 开发环境与 JDK 安装，覆盖核心概念、实践方法与常见问题，是“Java 入门与环境”学习路线的第 1 步。', '系统讲解Java 开发环境与 JDK 安装，覆盖核心概念、实践方法与常见问题，是“Java 入门与环境”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Java 入门与环境';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.Java 开发环境与 JDK 安装' AND c.name='Java 入门与环境';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 入门与环境' WHERE a.title='01.Java 开发环境与 JDK 安装' AND c.name='Java 入门与环境';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.IDEA 创建并运行第一个 Java 项目', '# 02.IDEA 创建并运行第一个 Java 项目

> 正文内容待完善。

系统讲解IDEA 创建并运行第一个 Java 项目，覆盖核心概念、实践方法与常见问题，是“Java 入门与环境”学习路线的第 2 步。', '系统讲解IDEA 创建并运行第一个 Java 项目，覆盖核心概念、实践方法与常见问题，是“Java 入门与环境”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Java 入门与环境';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.IDEA 创建并运行第一个 Java 项目' AND c.name='Java 入门与环境';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 入门与环境' WHERE a.title='02.IDEA 创建并运行第一个 Java 项目' AND c.name='Java 入门与环境';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Java 程序编译、运行与常见环境问题', '# 03.Java 程序编译、运行与常见环境问题

> 正文内容待完善。

系统讲解Java 程序编译、运行与常见环境问题，覆盖核心概念、实践方法与常见问题，是“Java 入门与环境”学习路线的第 3 步。', '系统讲解Java 程序编译、运行与常见环境问题，覆盖核心概念、实践方法与常见问题，是“Java 入门与环境”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Java 入门与环境';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.Java 程序编译、运行与常见环境问题' AND c.name='Java 入门与环境';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 入门与环境' WHERE a.title='03.Java 程序编译、运行与常见环境问题' AND c.name='Java 入门与环境';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.变量、数据类型与类型转换', '# 01.变量、数据类型与类型转换

> 正文内容待完善。

系统讲解变量、数据类型与类型转换，覆盖核心概念、实践方法与常见问题，是“Java 基础语法”学习路线的第 1 步。', '系统讲解变量、数据类型与类型转换，覆盖核心概念、实践方法与常见问题，是“Java 基础语法”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Java 基础语法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.变量、数据类型与类型转换' AND c.name='Java 基础语法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 基础语法' WHERE a.title='01.变量、数据类型与类型转换' AND c.name='Java 基础语法';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.运算符、表达式与流程控制', '# 02.运算符、表达式与流程控制

> 正文内容待完善。

系统讲解运算符、表达式与流程控制，覆盖核心概念、实践方法与常见问题，是“Java 基础语法”学习路线的第 2 步。', '系统讲解运算符、表达式与流程控制，覆盖核心概念、实践方法与常见问题，是“Java 基础语法”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Java 基础语法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.运算符、表达式与流程控制' AND c.name='Java 基础语法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 基础语法' WHERE a.title='02.运算符、表达式与流程控制' AND c.name='Java 基础语法';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.数组、方法与参数传递', '# 03.数组、方法与参数传递

> 正文内容待完善。

系统讲解数组、方法与参数传递，覆盖核心概念、实践方法与常见问题，是“Java 基础语法”学习路线的第 3 步。', '系统讲解数组、方法与参数传递，覆盖核心概念、实践方法与常见问题，是“Java 基础语法”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Java 基础语法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.数组、方法与参数传递' AND c.name='Java 基础语法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 基础语法' WHERE a.title='03.数组、方法与参数传递' AND c.name='Java 基础语法';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.类与对象：从现实模型到 Java 代码', '# 01.类与对象：从现实模型到 Java 代码

> 正文内容待完善。

系统讲解类与对象：从现实模型到 Java 代码，覆盖核心概念、实践方法与常见问题，是“面向对象编程”学习路线的第 1 步。', '系统讲解类与对象：从现实模型到 Java 代码，覆盖核心概念、实践方法与常见问题，是“面向对象编程”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='面向对象编程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.类与对象：从现实模型到 Java 代码' AND c.name='面向对象编程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='面向对象编程' WHERE a.title='01.类与对象：从现实模型到 Java 代码' AND c.name='面向对象编程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.封装、继承与多态', '# 02.封装、继承与多态

> 正文内容待完善。

系统讲解封装、继承与多态，覆盖核心概念、实践方法与常见问题，是“面向对象编程”学习路线的第 2 步。', '系统讲解封装、继承与多态，覆盖核心概念、实践方法与常见问题，是“面向对象编程”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='面向对象编程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.封装、继承与多态' AND c.name='面向对象编程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='面向对象编程' WHERE a.title='02.封装、继承与多态' AND c.name='面向对象编程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.抽象类、接口与内部类', '# 03.抽象类、接口与内部类

> 正文内容待完善。

系统讲解抽象类、接口与内部类，覆盖核心概念、实践方法与常见问题，是“面向对象编程”学习路线的第 3 步。', '系统讲解抽象类、接口与内部类，覆盖核心概念、实践方法与常见问题，是“面向对象编程”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='面向对象编程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.抽象类、接口与内部类' AND c.name='面向对象编程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='面向对象编程' WHERE a.title='03.抽象类、接口与内部类' AND c.name='面向对象编程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.String、StringBuilder 与字符串处理', '# 01.String、StringBuilder 与字符串处理

> 正文内容待完善。

系统讲解String、StringBuilder 与字符串处理，覆盖核心概念、实践方法与常见问题，是“Java 常用 API”学习路线的第 1 步。', '系统讲解String、StringBuilder 与字符串处理，覆盖核心概念、实践方法与常见问题，是“Java 常用 API”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Java 常用 API';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.String、StringBuilder 与字符串处理' AND c.name='Java 常用 API';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 常用 API' WHERE a.title='01.String、StringBuilder 与字符串处理' AND c.name='Java 常用 API';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.日期时间 API 与格式化', '# 02.日期时间 API 与格式化

> 正文内容待完善。

系统讲解日期时间 API 与格式化，覆盖核心概念、实践方法与常见问题，是“Java 常用 API”学习路线的第 2 步。', '系统讲解日期时间 API 与格式化，覆盖核心概念、实践方法与常见问题，是“Java 常用 API”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Java 常用 API';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.日期时间 API 与格式化' AND c.name='Java 常用 API';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 常用 API' WHERE a.title='02.日期时间 API 与格式化' AND c.name='Java 常用 API';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.包装类、Math、Random 与常用工具', '# 03.包装类、Math、Random 与常用工具

> 正文内容待完善。

系统讲解包装类、Math、Random 与常用工具，覆盖核心概念、实践方法与常见问题，是“Java 常用 API”学习路线的第 3 步。', '系统讲解包装类、Math、Random 与常用工具，覆盖核心概念、实践方法与常见问题，是“Java 常用 API”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Java 常用 API';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.包装类、Math、Random 与常用工具' AND c.name='Java 常用 API';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 常用 API' WHERE a.title='03.包装类、Math、Random 与常用工具' AND c.name='Java 常用 API';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Collection 体系与 List', '# 01.Collection 体系与 List

> 正文内容待完善。

系统讲解Collection 体系与 List，覆盖核心概念、实践方法与常见问题，是“集合框架”学习路线的第 1 步。', '系统讲解Collection 体系与 List，覆盖核心概念、实践方法与常见问题，是“集合框架”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='集合框架';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.Collection 体系与 List' AND c.name='集合框架';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='集合框架' WHERE a.title='01.Collection 体系与 List' AND c.name='集合框架';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.Set、Map 与哈希原理', '# 02.Set、Map 与哈希原理

> 正文内容待完善。

系统讲解Set、Map 与哈希原理，覆盖核心概念、实践方法与常见问题，是“集合框架”学习路线的第 2 步。', '系统讲解Set、Map 与哈希原理，覆盖核心概念、实践方法与常见问题，是“集合框架”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='集合框架';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.Set、Map 与哈希原理' AND c.name='集合框架';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='集合框架' WHERE a.title='02.Set、Map 与哈希原理' AND c.name='集合框架';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.集合遍历、排序与选型实践', '# 03.集合遍历、排序与选型实践

> 正文内容待完善。

系统讲解集合遍历、排序与选型实践，覆盖核心概念、实践方法与常见问题，是“集合框架”学习路线的第 3 步。', '系统讲解集合遍历、排序与选型实践，覆盖核心概念、实践方法与常见问题，是“集合框架”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='集合框架';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.集合遍历、排序与选型实践' AND c.name='集合框架';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='集合框架' WHERE a.title='03.集合遍历、排序与选型实践' AND c.name='集合框架';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.异常体系、捕获与自定义异常', '# 01.异常体系、捕获与自定义异常

> 正文内容待完善。

系统讲解异常体系、捕获与自定义异常，覆盖核心概念、实践方法与常见问题，是“异常与泛型”学习路线的第 1 步。', '系统讲解异常体系、捕获与自定义异常，覆盖核心概念、实践方法与常见问题，是“异常与泛型”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='异常与泛型';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.异常体系、捕获与自定义异常' AND c.name='异常与泛型';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='异常与泛型' WHERE a.title='01.异常体系、捕获与自定义异常' AND c.name='异常与泛型';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.泛型类、泛型方法与通配符', '# 02.泛型类、泛型方法与通配符

> 正文内容待完善。

系统讲解泛型类、泛型方法与通配符，覆盖核心概念、实践方法与常见问题，是“异常与泛型”学习路线的第 2 步。', '系统讲解泛型类、泛型方法与通配符，覆盖核心概念、实践方法与常见问题，是“异常与泛型”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='异常与泛型';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.泛型类、泛型方法与通配符' AND c.name='异常与泛型';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='异常与泛型' WHERE a.title='02.泛型类、泛型方法与通配符' AND c.name='异常与泛型';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.注解、反射与运行时类型信息', '# 03.注解、反射与运行时类型信息

> 正文内容待完善。

系统讲解注解、反射与运行时类型信息，覆盖核心概念、实践方法与常见问题，是“异常与泛型”学习路线的第 3 步。', '系统讲解注解、反射与运行时类型信息，覆盖核心概念、实践方法与常见问题，是“异常与泛型”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='异常与泛型';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.注解、反射与运行时类型信息' AND c.name='异常与泛型';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='异常与泛型' WHERE a.title='03.注解、反射与运行时类型信息' AND c.name='异常与泛型';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.字节流、字符流与缓冲流', '# 01.字节流、字符流与缓冲流

> 正文内容待完善。

系统讲解字节流、字符流与缓冲流，覆盖核心概念、实践方法与常见问题，是“IO 与 NIO”学习路线的第 1 步。', '系统讲解字节流、字符流与缓冲流，覆盖核心概念、实践方法与常见问题，是“IO 与 NIO”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='IO 与 NIO';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.字节流、字符流与缓冲流' AND c.name='IO 与 NIO';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='IO 与 NIO' WHERE a.title='01.字节流、字符流与缓冲流' AND c.name='IO 与 NIO';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.文件系统、序列化与资源管理', '# 02.文件系统、序列化与资源管理

> 正文内容待完善。

系统讲解文件系统、序列化与资源管理，覆盖核心概念、实践方法与常见问题，是“IO 与 NIO”学习路线的第 2 步。', '系统讲解文件系统、序列化与资源管理，覆盖核心概念、实践方法与常见问题，是“IO 与 NIO”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='IO 与 NIO';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.文件系统、序列化与资源管理' AND c.name='IO 与 NIO';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='IO 与 NIO' WHERE a.title='02.文件系统、序列化与资源管理' AND c.name='IO 与 NIO';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.NIO、Channel、Buffer 与文件操作', '# 03.NIO、Channel、Buffer 与文件操作

> 正文内容待完善。

系统讲解NIO、Channel、Buffer 与文件操作，覆盖核心概念、实践方法与常见问题，是“IO 与 NIO”学习路线的第 3 步。', '系统讲解NIO、Channel、Buffer 与文件操作，覆盖核心概念、实践方法与常见问题，是“IO 与 NIO”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='IO 与 NIO';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.NIO、Channel、Buffer 与文件操作' AND c.name='IO 与 NIO';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='IO 与 NIO' WHERE a.title='03.NIO、Channel、Buffer 与文件操作' AND c.name='IO 与 NIO';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.线程生命周期与创建方式', '# 01.线程生命周期与创建方式

> 正文内容待完善。

系统讲解线程生命周期与创建方式，覆盖核心概念、实践方法与常见问题，是“多线程与并发”学习路线的第 1 步。', '系统讲解线程生命周期与创建方式，覆盖核心概念、实践方法与常见问题，是“多线程与并发”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='多线程与并发';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.线程生命周期与创建方式' AND c.name='多线程与并发';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='多线程与并发' WHERE a.title='01.线程生命周期与创建方式' AND c.name='多线程与并发';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.线程安全、锁与并发容器', '# 02.线程安全、锁与并发容器

> 正文内容待完善。

系统讲解线程安全、锁与并发容器，覆盖核心概念、实践方法与常见问题，是“多线程与并发”学习路线的第 2 步。', '系统讲解线程安全、锁与并发容器，覆盖核心概念、实践方法与常见问题，是“多线程与并发”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='多线程与并发';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.线程安全、锁与并发容器' AND c.name='多线程与并发';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='多线程与并发' WHERE a.title='02.线程安全、锁与并发容器' AND c.name='多线程与并发';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.线程池、CompletableFuture 与并发实践', '# 03.线程池、CompletableFuture 与并发实践

> 正文内容待完善。

系统讲解线程池、CompletableFuture 与并发实践，覆盖核心概念、实践方法与常见问题，是“多线程与并发”学习路线的第 3 步。', '系统讲解线程池、CompletableFuture 与并发实践，覆盖核心概念、实践方法与常见问题，是“多线程与并发”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='多线程与并发';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.线程池、CompletableFuture 与并发实践' AND c.name='多线程与并发';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='多线程与并发' WHERE a.title='03.线程池、CompletableFuture 与并发实践' AND c.name='多线程与并发';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.JVM 运行时内存区域', '# 01.JVM 运行时内存区域

> 正文内容待完善。

系统讲解JVM 运行时内存区域，覆盖核心概念、实践方法与常见问题，是“JVM 基础”学习路线的第 1 步。', '系统讲解JVM 运行时内存区域，覆盖核心概念、实践方法与常见问题，是“JVM 基础”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='JVM 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.JVM 运行时内存区域' AND c.name='JVM 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JVM 基础' WHERE a.title='01.JVM 运行时内存区域' AND c.name='JVM 基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.类加载机制与字节码基础', '# 02.类加载机制与字节码基础

> 正文内容待完善。

系统讲解类加载机制与字节码基础，覆盖核心概念、实践方法与常见问题，是“JVM 基础”学习路线的第 2 步。', '系统讲解类加载机制与字节码基础，覆盖核心概念、实践方法与常见问题，是“JVM 基础”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='JVM 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.类加载机制与字节码基础' AND c.name='JVM 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JVM 基础' WHERE a.title='02.类加载机制与字节码基础' AND c.name='JVM 基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.垃圾回收器、GC 日志与基础调优', '# 03.垃圾回收器、GC 日志与基础调优

> 正文内容待完善。

系统讲解垃圾回收器、GC 日志与基础调优，覆盖核心概念、实践方法与常见问题，是“JVM 基础”学习路线的第 3 步。', '系统讲解垃圾回收器、GC 日志与基础调优，覆盖核心概念、实践方法与常见问题，是“JVM 基础”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='JVM 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.垃圾回收器、GC 日志与基础调优' AND c.name='JVM 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JVM 基础' WHERE a.title='03.垃圾回收器、GC 日志与基础调优' AND c.name='JVM 基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Lambda、函数式接口与方法引用', '# 01.Lambda、函数式接口与方法引用

> 正文内容待完善。

系统讲解Lambda、函数式接口与方法引用，覆盖核心概念、实践方法与常见问题，是“现代 Java”学习路线的第 1 步。', '系统讲解Lambda、函数式接口与方法引用，覆盖核心概念、实践方法与常见问题，是“现代 Java”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='现代 Java';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.Lambda、函数式接口与方法引用' AND c.name='现代 Java';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='现代 Java' WHERE a.title='01.Lambda、函数式接口与方法引用' AND c.name='现代 Java';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.Stream API 数据处理实践', '# 02.Stream API 数据处理实践

> 正文内容待完善。

系统讲解Stream API 数据处理实践，覆盖核心概念、实践方法与常见问题，是“现代 Java”学习路线的第 2 步。', '系统讲解Stream API 数据处理实践，覆盖核心概念、实践方法与常见问题，是“现代 Java”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='现代 Java';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.Stream API 数据处理实践' AND c.name='现代 Java';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='现代 Java' WHERE a.title='02.Stream API 数据处理实践' AND c.name='现代 Java';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Java 9 到 Java 21 核心新特性', '# 03.Java 9 到 Java 21 核心新特性

> 正文内容待完善。

系统讲解Java 9 到 Java 21 核心新特性，覆盖核心概念、实践方法与常见问题，是“现代 Java”学习路线的第 3 步。', '系统讲解Java 9 到 Java 21 核心新特性，覆盖核心概念、实践方法与常见问题，是“现代 Java”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='现代 Java';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.Java 9 到 Java 21 核心新特性' AND c.name='现代 Java';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='现代 Java' WHERE a.title='03.Java 9 到 Java 21 核心新特性' AND c.name='现代 Java';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Spring 容器、IoC 与依赖注入', '# 01.Spring 容器、IoC 与依赖注入

> 正文内容待完善。

系统讲解Spring 容器、IoC 与依赖注入，覆盖核心概念、实践方法与常见问题，是“Spring IoC 与 AOP”学习路线的第 1 步。', '系统讲解Spring 容器、IoC 与依赖注入，覆盖核心概念、实践方法与常见问题，是“Spring IoC 与 AOP”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Spring IoC 与 AOP';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.Spring 容器、IoC 与依赖注入' AND c.name='Spring IoC 与 AOP';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring IoC 与 AOP' WHERE a.title='01.Spring 容器、IoC 与依赖注入' AND c.name='Spring IoC 与 AOP';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.Bean 生命周期、作用域与扩展点', '# 02.Bean 生命周期、作用域与扩展点

> 正文内容待完善。

系统讲解Bean 生命周期、作用域与扩展点，覆盖核心概念、实践方法与常见问题，是“Spring IoC 与 AOP”学习路线的第 2 步。', '系统讲解Bean 生命周期、作用域与扩展点，覆盖核心概念、实践方法与常见问题，是“Spring IoC 与 AOP”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Spring IoC 与 AOP';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.Bean 生命周期、作用域与扩展点' AND c.name='Spring IoC 与 AOP';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring IoC 与 AOP' WHERE a.title='02.Bean 生命周期、作用域与扩展点' AND c.name='Spring IoC 与 AOP';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.AOP、代理机制与声明式事务', '# 03.AOP、代理机制与声明式事务

> 正文内容待完善。

系统讲解AOP、代理机制与声明式事务，覆盖核心概念、实践方法与常见问题，是“Spring IoC 与 AOP”学习路线的第 3 步。', '系统讲解AOP、代理机制与声明式事务，覆盖核心概念、实践方法与常见问题，是“Spring IoC 与 AOP”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Spring IoC 与 AOP';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.AOP、代理机制与声明式事务' AND c.name='Spring IoC 与 AOP';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring IoC 与 AOP' WHERE a.title='03.AOP、代理机制与声明式事务' AND c.name='Spring IoC 与 AOP';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Spring MVC 请求处理流程', '# 01.Spring MVC 请求处理流程

> 正文内容待完善。

系统讲解Spring MVC 请求处理流程，覆盖核心概念、实践方法与常见问题，是“Spring MVC”学习路线的第 1 步。', '系统讲解Spring MVC 请求处理流程，覆盖核心概念、实践方法与常见问题，是“Spring MVC”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Spring MVC';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.Spring MVC 请求处理流程' AND c.name='Spring MVC';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring MVC' WHERE a.title='01.Spring MVC 请求处理流程' AND c.name='Spring MVC';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.参数绑定、数据校验与统一响应', '# 02.参数绑定、数据校验与统一响应

> 正文内容待完善。

系统讲解参数绑定、数据校验与统一响应，覆盖核心概念、实践方法与常见问题，是“Spring MVC”学习路线的第 2 步。', '系统讲解参数绑定、数据校验与统一响应，覆盖核心概念、实践方法与常见问题，是“Spring MVC”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Spring MVC';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.参数绑定、数据校验与统一响应' AND c.name='Spring MVC';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring MVC' WHERE a.title='02.参数绑定、数据校验与统一响应' AND c.name='Spring MVC';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.全局异常处理、拦截器与文件上传', '# 03.全局异常处理、拦截器与文件上传

> 正文内容待完善。

系统讲解全局异常处理、拦截器与文件上传，覆盖核心概念、实践方法与常见问题，是“Spring MVC”学习路线的第 3 步。', '系统讲解全局异常处理、拦截器与文件上传，覆盖核心概念、实践方法与常见问题，是“Spring MVC”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Spring MVC';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.全局异常处理、拦截器与文件上传' AND c.name='Spring MVC';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring MVC' WHERE a.title='03.全局异常处理、拦截器与文件上传' AND c.name='Spring MVC';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Spring Boot 项目结构与自动配置', '# 01.Spring Boot 项目结构与自动配置

> 正文内容待完善。

系统讲解Spring Boot 项目结构与自动配置，覆盖核心概念、实践方法与常见问题，是“Spring Boot”学习路线的第 1 步。', '系统讲解Spring Boot 项目结构与自动配置，覆盖核心概念、实践方法与常见问题，是“Spring Boot”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Spring Boot';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.Spring Boot 项目结构与自动配置' AND c.name='Spring Boot';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.配置文件、环境隔离与类型安全配置', '# 02.配置文件、环境隔离与类型安全配置

> 正文内容待完善。

系统讲解配置文件、环境隔离与类型安全配置，覆盖核心概念、实践方法与常见问题，是“Spring Boot”学习路线的第 2 步。', '系统讲解配置文件、环境隔离与类型安全配置，覆盖核心概念、实践方法与常见问题，是“Spring Boot”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Spring Boot';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.配置文件、环境隔离与类型安全配置' AND c.name='Spring Boot';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Starter、自定义自动配置与启动流程', '# 03.Starter、自定义自动配置与启动流程

> 正文内容待完善。

系统讲解Starter、自定义自动配置与启动流程，覆盖核心概念、实践方法与常见问题，是“Spring Boot”学习路线的第 3 步。', '系统讲解Starter、自定义自动配置与启动流程，覆盖核心概念、实践方法与常见问题，是“Spring Boot”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Spring Boot';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.Starter、自定义自动配置与启动流程' AND c.name='Spring Boot';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.MyBatis 映射、参数与结果集', '# 01.MyBatis 映射、参数与结果集

> 正文内容待完善。

系统讲解MyBatis 映射、参数与结果集，覆盖核心概念、实践方法与常见问题，是“MyBatis 与 MyBatis-Plus”学习路线的第 1 步。', '系统讲解MyBatis 映射、参数与结果集，覆盖核心概念、实践方法与常见问题，是“MyBatis 与 MyBatis-Plus”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='MyBatis 与 MyBatis-Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.MyBatis 映射、参数与结果集' AND c.name='MyBatis 与 MyBatis-Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MyBatis 与 MyBatis-Plus' WHERE a.title='01.MyBatis 映射、参数与结果集' AND c.name='MyBatis 与 MyBatis-Plus';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.动态 SQL、分页与批量操作', '# 02.动态 SQL、分页与批量操作

> 正文内容待完善。

系统讲解动态 SQL、分页与批量操作，覆盖核心概念、实践方法与常见问题，是“MyBatis 与 MyBatis-Plus”学习路线的第 2 步。', '系统讲解动态 SQL、分页与批量操作，覆盖核心概念、实践方法与常见问题，是“MyBatis 与 MyBatis-Plus”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='MyBatis 与 MyBatis-Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.动态 SQL、分页与批量操作' AND c.name='MyBatis 与 MyBatis-Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MyBatis 与 MyBatis-Plus' WHERE a.title='02.动态 SQL、分页与批量操作' AND c.name='MyBatis 与 MyBatis-Plus';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.MyBatis-Plus CRUD、插件与最佳实践', '# 03.MyBatis-Plus CRUD、插件与最佳实践

> 正文内容待完善。

系统讲解MyBatis-Plus CRUD、插件与最佳实践，覆盖核心概念、实践方法与常见问题，是“MyBatis 与 MyBatis-Plus”学习路线的第 3 步。', '系统讲解MyBatis-Plus CRUD、插件与最佳实践，覆盖核心概念、实践方法与常见问题，是“MyBatis 与 MyBatis-Plus”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='MyBatis 与 MyBatis-Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.MyBatis-Plus CRUD、插件与最佳实践' AND c.name='MyBatis 与 MyBatis-Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MyBatis 与 MyBatis-Plus' WHERE a.title='03.MyBatis-Plus CRUD、插件与最佳实践' AND c.name='MyBatis 与 MyBatis-Plus';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.认证、授权与 RBAC 权限模型', '# 01.认证、授权与 RBAC 权限模型

> 正文内容待完善。

系统讲解认证、授权与 RBAC 权限模型，覆盖核心概念、实践方法与常见问题，是“认证与权限”学习路线的第 1 步。', '系统讲解认证、授权与 RBAC 权限模型，覆盖核心概念、实践方法与常见问题，是“认证与权限”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='认证与权限';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.认证、授权与 RBAC 权限模型' AND c.name='认证与权限';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='认证与权限' WHERE a.title='01.认证、授权与 RBAC 权限模型' AND c.name='认证与权限';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.Spring Security 登录与接口鉴权', '# 02.Spring Security 登录与接口鉴权

> 正文内容待完善。

系统讲解Spring Security 登录与接口鉴权，覆盖核心概念、实践方法与常见问题，是“认证与权限”学习路线的第 2 步。', '系统讲解Spring Security 登录与接口鉴权，覆盖核心概念、实践方法与常见问题，是“认证与权限”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='认证与权限';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.Spring Security 登录与接口鉴权' AND c.name='认证与权限';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='认证与权限' WHERE a.title='02.Spring Security 登录与接口鉴权' AND c.name='认证与权限';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Sa-Token 会话、角色与安全实践', '# 03.Sa-Token 会话、角色与安全实践

> 正文内容待完善。

系统讲解Sa-Token 会话、角色与安全实践，覆盖核心概念、实践方法与常见问题，是“认证与权限”学习路线的第 3 步。', '系统讲解Sa-Token 会话、角色与安全实践，覆盖核心概念、实践方法与常见问题，是“认证与权限”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='认证与权限';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.Sa-Token 会话、角色与安全实践' AND c.name='认证与权限';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='认证与权限' WHERE a.title='03.Sa-Token 会话、角色与安全实践' AND c.name='认证与权限';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.微服务拆分原则与服务注册发现', '# 01.微服务拆分原则与服务注册发现

> 正文内容待完善。

系统讲解微服务拆分原则与服务注册发现，覆盖核心概念、实践方法与常见问题，是“Spring Cloud”学习路线的第 1 步。', '系统讲解微服务拆分原则与服务注册发现，覆盖核心概念、实践方法与常见问题，是“Spring Cloud”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Spring Cloud';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.微服务拆分原则与服务注册发现' AND c.name='Spring Cloud';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Cloud' WHERE a.title='01.微服务拆分原则与服务注册发现' AND c.name='Spring Cloud';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.OpenFeign、网关与负载均衡', '# 02.OpenFeign、网关与负载均衡

> 正文内容待完善。

系统讲解OpenFeign、网关与负载均衡，覆盖核心概念、实践方法与常见问题，是“Spring Cloud”学习路线的第 2 步。', '系统讲解OpenFeign、网关与负载均衡，覆盖核心概念、实践方法与常见问题，是“Spring Cloud”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Spring Cloud';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.OpenFeign、网关与负载均衡' AND c.name='Spring Cloud';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Cloud' WHERE a.title='02.OpenFeign、网关与负载均衡' AND c.name='Spring Cloud';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.配置中心、限流熔断与链路追踪', '# 03.配置中心、限流熔断与链路追踪

> 正文内容待完善。

系统讲解配置中心、限流熔断与链路追踪，覆盖核心概念、实践方法与常见问题，是“Spring Cloud”学习路线的第 3 步。', '系统讲解配置中心、限流熔断与链路追踪，覆盖核心概念、实践方法与常见问题，是“Spring Cloud”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Spring Cloud';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.配置中心、限流熔断与链路追踪' AND c.name='Spring Cloud';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Cloud' WHERE a.title='03.配置中心、限流熔断与链路追踪' AND c.name='Spring Cloud';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Spring 异步任务与线程池', '# 01.Spring 异步任务与线程池

> 正文内容待完善。

系统讲解Spring 异步任务与线程池，覆盖核心概念、实践方法与常见问题，是“消息与任务”学习路线的第 1 步。', '系统讲解Spring 异步任务与线程池，覆盖核心概念、实践方法与常见问题，是“消息与任务”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='消息与任务';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.Spring 异步任务与线程池' AND c.name='消息与任务';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='消息与任务' WHERE a.title='01.Spring 异步任务与线程池' AND c.name='消息与任务';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.RabbitMQ 消息模型与可靠投递', '# 02.RabbitMQ 消息模型与可靠投递

> 正文内容待完善。

系统讲解RabbitMQ 消息模型与可靠投递，覆盖核心概念、实践方法与常见问题，是“消息与任务”学习路线的第 2 步。', '系统讲解RabbitMQ 消息模型与可靠投递，覆盖核心概念、实践方法与常见问题，是“消息与任务”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='消息与任务';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.RabbitMQ 消息模型与可靠投递' AND c.name='消息与任务';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='消息与任务' WHERE a.title='02.RabbitMQ 消息模型与可靠投递' AND c.name='消息与任务';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Kafka、定时任务与分布式调度', '# 03.Kafka、定时任务与分布式调度

> 正文内容待完善。

系统讲解Kafka、定时任务与分布式调度，覆盖核心概念、实践方法与常见问题，是“消息与任务”学习路线的第 3 步。', '系统讲解Kafka、定时任务与分布式调度，覆盖核心概念、实践方法与常见问题，是“消息与任务”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='消息与任务';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.Kafka、定时任务与分布式调度' AND c.name='消息与任务';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='消息与任务' WHERE a.title='03.Kafka、定时任务与分布式调度' AND c.name='消息与任务';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.JUnit、Mockito 与单元测试', '# 01.JUnit、Mockito 与单元测试

> 正文内容待完善。

系统讲解JUnit、Mockito 与单元测试，覆盖核心概念、实践方法与常见问题，是“Spring 测试与监控”学习路线的第 1 步。', '系统讲解JUnit、Mockito 与单元测试，覆盖核心概念、实践方法与常见问题，是“Spring 测试与监控”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Spring 测试与监控';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.JUnit、Mockito 与单元测试' AND c.name='Spring 测试与监控';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring 测试与监控' WHERE a.title='01.JUnit、Mockito 与单元测试' AND c.name='Spring 测试与监控';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.Spring Boot 集成测试与 Testcontainers', '# 02.Spring Boot 集成测试与 Testcontainers

> 正文内容待完善。

系统讲解Spring Boot 集成测试与 Testcontainers，覆盖核心概念、实践方法与常见问题，是“Spring 测试与监控”学习路线的第 2 步。', '系统讲解Spring Boot 集成测试与 Testcontainers，覆盖核心概念、实践方法与常见问题，是“Spring 测试与监控”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Spring 测试与监控';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.Spring Boot 集成测试与 Testcontainers' AND c.name='Spring 测试与监控';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring 测试与监控' WHERE a.title='02.Spring Boot 集成测试与 Testcontainers' AND c.name='Spring 测试与监控';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Actuator、Micrometer 与可观测性', '# 03.Actuator、Micrometer 与可观测性

> 正文内容待完善。

系统讲解Actuator、Micrometer 与可观测性，覆盖核心概念、实践方法与常见问题，是“Spring 测试与监控”学习路线的第 3 步。', '系统讲解Actuator、Micrometer 与可观测性，覆盖核心概念、实践方法与常见问题，是“Spring 测试与监控”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Spring 测试与监控';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.Actuator、Micrometer 与可观测性' AND c.name='Spring 测试与监控';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring 测试与监控' WHERE a.title='03.Actuator、Micrometer 与可观测性' AND c.name='Spring 测试与监控';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.HTML 语义化、表单与可访问性', '# 01.HTML 语义化、表单与可访问性

> 正文内容待完善。

系统讲解HTML 语义化、表单与可访问性，覆盖核心概念、实践方法与常见问题，是“HTML 与 CSS”学习路线的第 1 步。', '系统讲解HTML 语义化、表单与可访问性，覆盖核心概念、实践方法与常见问题，是“HTML 与 CSS”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='HTML 与 CSS';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='01.HTML 语义化、表单与可访问性' AND c.name='HTML 与 CSS';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='HTML 与 CSS' WHERE a.title='01.HTML 语义化、表单与可访问性' AND c.name='HTML 与 CSS';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.CSS 盒模型、Flex 与 Grid 布局', '# 02.CSS 盒模型、Flex 与 Grid 布局

> 正文内容待完善。

系统讲解CSS 盒模型、Flex 与 Grid 布局，覆盖核心概念、实践方法与常见问题，是“HTML 与 CSS”学习路线的第 2 步。', '系统讲解CSS 盒模型、Flex 与 Grid 布局，覆盖核心概念、实践方法与常见问题，是“HTML 与 CSS”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='HTML 与 CSS';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='02.CSS 盒模型、Flex 与 Grid 布局' AND c.name='HTML 与 CSS';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='HTML 与 CSS' WHERE a.title='02.CSS 盒模型、Flex 与 Grid 布局' AND c.name='HTML 与 CSS';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.响应式设计、动画与样式组织', '# 03.响应式设计、动画与样式组织

> 正文内容待完善。

系统讲解响应式设计、动画与样式组织，覆盖核心概念、实践方法与常见问题，是“HTML 与 CSS”学习路线的第 3 步。', '系统讲解响应式设计、动画与样式组织，覆盖核心概念、实践方法与常见问题，是“HTML 与 CSS”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='HTML 与 CSS';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='03.响应式设计、动画与样式组织' AND c.name='HTML 与 CSS';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='HTML 与 CSS' WHERE a.title='03.响应式设计、动画与样式组织' AND c.name='HTML 与 CSS';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.JavaScript 类型、作用域与函数', '# 01.JavaScript 类型、作用域与函数

> 正文内容待完善。

系统讲解JavaScript 类型、作用域与函数，覆盖核心概念、实践方法与常见问题，是“JavaScript 基础”学习路线的第 1 步。', '系统讲解JavaScript 类型、作用域与函数，覆盖核心概念、实践方法与常见问题，是“JavaScript 基础”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='JavaScript 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='01.JavaScript 类型、作用域与函数' AND c.name='JavaScript 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript 基础' WHERE a.title='01.JavaScript 类型、作用域与函数' AND c.name='JavaScript 基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.对象、原型、模块与错误处理', '# 02.对象、原型、模块与错误处理

> 正文内容待完善。

系统讲解对象、原型、模块与错误处理，覆盖核心概念、实践方法与常见问题，是“JavaScript 基础”学习路线的第 2 步。', '系统讲解对象、原型、模块与错误处理，覆盖核心概念、实践方法与常见问题，是“JavaScript 基础”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='JavaScript 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='02.对象、原型、模块与错误处理' AND c.name='JavaScript 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript 基础' WHERE a.title='02.对象、原型、模块与错误处理' AND c.name='JavaScript 基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Promise、异步编程与浏览器事件', '# 03.Promise、异步编程与浏览器事件

> 正文内容待完善。

系统讲解Promise、异步编程与浏览器事件，覆盖核心概念、实践方法与常见问题，是“JavaScript 基础”学习路线的第 3 步。', '系统讲解Promise、异步编程与浏览器事件，覆盖核心概念、实践方法与常见问题，是“JavaScript 基础”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='JavaScript 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='03.Promise、异步编程与浏览器事件' AND c.name='JavaScript 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript 基础' WHERE a.title='03.Promise、异步编程与浏览器事件' AND c.name='JavaScript 基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.TypeScript 基础类型与类型收窄', '# 01.TypeScript 基础类型与类型收窄

> 正文内容待完善。

系统讲解TypeScript 基础类型与类型收窄，覆盖核心概念、实践方法与常见问题，是“TypeScript”学习路线的第 1 步。', '系统讲解TypeScript 基础类型与类型收窄，覆盖核心概念、实践方法与常见问题，是“TypeScript”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='TypeScript';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='01.TypeScript 基础类型与类型收窄' AND c.name='TypeScript';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='TypeScript' WHERE a.title='01.TypeScript 基础类型与类型收窄' AND c.name='TypeScript';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.接口、泛型与高级类型', '# 02.接口、泛型与高级类型

> 正文内容待完善。

系统讲解接口、泛型与高级类型，覆盖核心概念、实践方法与常见问题，是“TypeScript”学习路线的第 2 步。', '系统讲解接口、泛型与高级类型，覆盖核心概念、实践方法与常见问题，是“TypeScript”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='TypeScript';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='02.接口、泛型与高级类型' AND c.name='TypeScript';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='TypeScript' WHERE a.title='02.接口、泛型与高级类型' AND c.name='TypeScript';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.TypeScript 工程配置与类型设计', '# 03.TypeScript 工程配置与类型设计

> 正文内容待完善。

系统讲解TypeScript 工程配置与类型设计，覆盖核心概念、实践方法与常见问题，是“TypeScript”学习路线的第 3 步。', '系统讲解TypeScript 工程配置与类型设计，覆盖核心概念、实践方法与常见问题，是“TypeScript”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='TypeScript';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='03.TypeScript 工程配置与类型设计' AND c.name='TypeScript';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='TypeScript' WHERE a.title='03.TypeScript 工程配置与类型设计' AND c.name='TypeScript';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Vue 3 响应式与组合式 API', '# 01.Vue 3 响应式与组合式 API

> 正文内容待完善。

系统讲解Vue 3 响应式与组合式 API，覆盖核心概念、实践方法与常见问题，是“Vue 3”学习路线的第 1 步。', '系统讲解Vue 3 响应式与组合式 API，覆盖核心概念、实践方法与常见问题，是“Vue 3”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Vue 3';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='01.Vue 3 响应式与组合式 API' AND c.name='Vue 3';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='01.Vue 3 响应式与组合式 API' AND c.name='Vue 3';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.组件通信、插槽与生命周期', '# 02.组件通信、插槽与生命周期

> 正文内容待完善。

系统讲解组件通信、插槽与生命周期，覆盖核心概念、实践方法与常见问题，是“Vue 3”学习路线的第 2 步。', '系统讲解组件通信、插槽与生命周期，覆盖核心概念、实践方法与常见问题，是“Vue 3”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Vue 3';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='02.组件通信、插槽与生命周期' AND c.name='Vue 3';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='02.组件通信、插槽与生命周期' AND c.name='Vue 3';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Vue Router、Pinia 与项目组织', '# 03.Vue Router、Pinia 与项目组织

> 正文内容待完善。

系统讲解Vue Router、Pinia 与项目组织，覆盖核心概念、实践方法与常见问题，是“Vue 3”学习路线的第 3 步。', '系统讲解Vue Router、Pinia 与项目组织，覆盖核心概念、实践方法与常见问题，是“Vue 3”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Vue 3';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='03.Vue Router、Pinia 与项目组织' AND c.name='Vue 3';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='03.Vue Router、Pinia 与项目组织' AND c.name='Vue 3';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Element Plus 表单与数据校验', '# 01.Element Plus 表单与数据校验

> 正文内容待完善。

系统讲解Element Plus 表单与数据校验，覆盖核心概念、实践方法与常见问题，是“Element Plus”学习路线的第 1 步。', '系统讲解Element Plus 表单与数据校验，覆盖核心概念、实践方法与常见问题，是“Element Plus”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Element Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='01.Element Plus 表单与数据校验' AND c.name='Element Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Element Plus' WHERE a.title='01.Element Plus 表单与数据校验' AND c.name='Element Plus';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.表格、分页与弹窗交互', '# 02.表格、分页与弹窗交互

> 正文内容待完善。

系统讲解表格、分页与弹窗交互，覆盖核心概念、实践方法与常见问题，是“Element Plus”学习路线的第 2 步。', '系统讲解表格、分页与弹窗交互，覆盖核心概念、实践方法与常见问题，是“Element Plus”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Element Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='02.表格、分页与弹窗交互' AND c.name='Element Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Element Plus' WHERE a.title='02.表格、分页与弹窗交互' AND c.name='Element Plus';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.主题定制、按需加载与组件封装', '# 03.主题定制、按需加载与组件封装

> 正文内容待完善。

系统讲解主题定制、按需加载与组件封装，覆盖核心概念、实践方法与常见问题，是“Element Plus”学习路线的第 3 步。', '系统讲解主题定制、按需加载与组件封装，覆盖核心概念、实践方法与常见问题，是“Element Plus”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Element Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='03.主题定制、按需加载与组件封装' AND c.name='Element Plus';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Element Plus' WHERE a.title='03.主题定制、按需加载与组件封装' AND c.name='Element Plus';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.React 组件、JSX 与数据流', '# 01.React 组件、JSX 与数据流

> 正文内容待完善。

系统讲解React 组件、JSX 与数据流，覆盖核心概念、实践方法与常见问题，是“React”学习路线的第 1 步。', '系统讲解React 组件、JSX 与数据流，覆盖核心概念、实践方法与常见问题，是“React”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='React';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='01.React 组件、JSX 与数据流' AND c.name='React';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='React' WHERE a.title='01.React 组件、JSX 与数据流' AND c.name='React';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.Hooks、状态管理与性能优化', '# 02.Hooks、状态管理与性能优化

> 正文内容待完善。

系统讲解Hooks、状态管理与性能优化，覆盖核心概念、实践方法与常见问题，是“React”学习路线的第 2 步。', '系统讲解Hooks、状态管理与性能优化，覆盖核心概念、实践方法与常见问题，是“React”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='React';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='02.Hooks、状态管理与性能优化' AND c.name='React';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='React' WHERE a.title='02.Hooks、状态管理与性能优化' AND c.name='React';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.React Router 与项目实战结构', '# 03.React Router 与项目实战结构

> 正文内容待完善。

系统讲解React Router 与项目实战结构，覆盖核心概念、实践方法与常见问题，是“React”学习路线的第 3 步。', '系统讲解React Router 与项目实战结构，覆盖核心概念、实践方法与常见问题，是“React”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='React';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='03.React Router 与项目实战结构' AND c.name='React';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='React' WHERE a.title='03.React Router 与项目实战结构' AND c.name='React';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Vite、模块化与构建配置', '# 01.Vite、模块化与构建配置

> 正文内容待完善。

系统讲解Vite、模块化与构建配置，覆盖核心概念、实践方法与常见问题，是“前端工程化”学习路线的第 1 步。', '系统讲解Vite、模块化与构建配置，覆盖核心概念、实践方法与常见问题，是“前端工程化”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='前端工程化';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='01.Vite、模块化与构建配置' AND c.name='前端工程化';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='前端工程化' WHERE a.title='01.Vite、模块化与构建配置' AND c.name='前端工程化';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.ESLint、单元测试与端到端测试', '# 02.ESLint、单元测试与端到端测试

> 正文内容待完善。

系统讲解ESLint、单元测试与端到端测试，覆盖核心概念、实践方法与常见问题，是“前端工程化”学习路线的第 2 步。', '系统讲解ESLint、单元测试与端到端测试，覆盖核心概念、实践方法与常见问题，是“前端工程化”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='前端工程化';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='02.ESLint、单元测试与端到端测试' AND c.name='前端工程化';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='前端工程化' WHERE a.title='02.ESLint、单元测试与端到端测试' AND c.name='前端工程化';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.前端性能优化与生产部署', '# 03.前端性能优化与生产部署

> 正文内容待完善。

系统讲解前端性能优化与生产部署，覆盖核心概念、实践方法与常见问题，是“前端工程化”学习路线的第 3 步。', '系统讲解前端性能优化与生产部署，覆盖核心概念、实践方法与常见问题，是“前端工程化”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='前端工程化';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='JavaScript' WHERE a.title='03.前端性能优化与生产部署' AND c.name='前端工程化';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='前端工程化' WHERE a.title='03.前端性能优化与生产部署' AND c.name='前端工程化';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.关系型数据库、表与约束', '# 01.关系型数据库、表与约束

> 正文内容待完善。

系统讲解关系型数据库、表与约束，覆盖核心概念、实践方法与常见问题，是“数据库与 SQL 基础”学习路线的第 1 步。', '系统讲解关系型数据库、表与约束，覆盖核心概念、实践方法与常见问题，是“数据库与 SQL 基础”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='数据库与 SQL 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='01.关系型数据库、表与约束' AND c.name='数据库与 SQL 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='数据库与 SQL 基础' WHERE a.title='01.关系型数据库、表与约束' AND c.name='数据库与 SQL 基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.SQL 查询、连接与子查询', '# 02.SQL 查询、连接与子查询

> 正文内容待完善。

系统讲解SQL 查询、连接与子查询，覆盖核心概念、实践方法与常见问题，是“数据库与 SQL 基础”学习路线的第 2 步。', '系统讲解SQL 查询、连接与子查询，覆盖核心概念、实践方法与常见问题，是“数据库与 SQL 基础”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='数据库与 SQL 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='02.SQL 查询、连接与子查询' AND c.name='数据库与 SQL 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='数据库与 SQL 基础' WHERE a.title='02.SQL 查询、连接与子查询' AND c.name='数据库与 SQL 基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.范式、ER 模型与数据库设计', '# 03.范式、ER 模型与数据库设计

> 正文内容待完善。

系统讲解范式、ER 模型与数据库设计，覆盖核心概念、实践方法与常见问题，是“数据库与 SQL 基础”学习路线的第 3 步。', '系统讲解范式、ER 模型与数据库设计，覆盖核心概念、实践方法与常见问题，是“数据库与 SQL 基础”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='数据库与 SQL 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='03.范式、ER 模型与数据库设计' AND c.name='数据库与 SQL 基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='数据库与 SQL 基础' WHERE a.title='03.范式、ER 模型与数据库设计' AND c.name='数据库与 SQL 基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.MySQL 架构与 InnoDB 存储引擎', '# 01.MySQL 架构与 InnoDB 存储引擎

> 正文内容待完善。

系统讲解MySQL 架构与 InnoDB 存储引擎，覆盖核心概念、实践方法与常见问题，是“MySQL 核心”学习路线的第 1 步。', '系统讲解MySQL 架构与 InnoDB 存储引擎，覆盖核心概念、实践方法与常见问题，是“MySQL 核心”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='MySQL 核心';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='01.MySQL 架构与 InnoDB 存储引擎' AND c.name='MySQL 核心';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL 核心' WHERE a.title='01.MySQL 架构与 InnoDB 存储引擎' AND c.name='MySQL 核心';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.事务隔离、MVCC 与锁机制', '# 02.事务隔离、MVCC 与锁机制

> 正文内容待完善。

系统讲解事务隔离、MVCC 与锁机制，覆盖核心概念、实践方法与常见问题，是“MySQL 核心”学习路线的第 2 步。', '系统讲解事务隔离、MVCC 与锁机制，覆盖核心概念、实践方法与常见问题，是“MySQL 核心”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='MySQL 核心';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='02.事务隔离、MVCC 与锁机制' AND c.name='MySQL 核心';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL 核心' WHERE a.title='02.事务隔离、MVCC 与锁机制' AND c.name='MySQL 核心';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.索引结构、执行计划与查询优化', '# 03.索引结构、执行计划与查询优化

> 正文内容待完善。

系统讲解索引结构、执行计划与查询优化，覆盖核心概念、实践方法与常见问题，是“MySQL 核心”学习路线的第 3 步。', '系统讲解索引结构、执行计划与查询优化，覆盖核心概念、实践方法与常见问题，是“MySQL 核心”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='MySQL 核心';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='03.索引结构、执行计划与查询优化' AND c.name='MySQL 核心';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL 核心' WHERE a.title='03.索引结构、执行计划与查询优化' AND c.name='MySQL 核心';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.MySQL 用户权限与安全配置', '# 01.MySQL 用户权限与安全配置

> 正文内容待完善。

系统讲解MySQL 用户权限与安全配置，覆盖核心概念、实践方法与常见问题，是“MySQL 运维”学习路线的第 1 步。', '系统讲解MySQL 用户权限与安全配置，覆盖核心概念、实践方法与常见问题，是“MySQL 运维”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='MySQL 运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='01.MySQL 用户权限与安全配置' AND c.name='MySQL 运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL 运维' WHERE a.title='01.MySQL 用户权限与安全配置' AND c.name='MySQL 运维';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.备份恢复、主从复制与高可用', '# 02.备份恢复、主从复制与高可用

> 正文内容待完善。

系统讲解备份恢复、主从复制与高可用，覆盖核心概念、实践方法与常见问题，是“MySQL 运维”学习路线的第 2 步。', '系统讲解备份恢复、主从复制与高可用，覆盖核心概念、实践方法与常见问题，是“MySQL 运维”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='MySQL 运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='02.备份恢复、主从复制与高可用' AND c.name='MySQL 运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL 运维' WHERE a.title='02.备份恢复、主从复制与高可用' AND c.name='MySQL 运维';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.慢查询监控、容量规划与分库分表', '# 03.慢查询监控、容量规划与分库分表

> 正文内容待完善。

系统讲解慢查询监控、容量规划与分库分表，覆盖核心概念、实践方法与常见问题，是“MySQL 运维”学习路线的第 3 步。', '系统讲解慢查询监控、容量规划与分库分表，覆盖核心概念、实践方法与常见问题，是“MySQL 运维”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='MySQL 运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='03.慢查询监控、容量规划与分库分表' AND c.name='MySQL 运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL 运维' WHERE a.title='03.慢查询监控、容量规划与分库分表' AND c.name='MySQL 运维';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Redis 数据结构与常用命令', '# 01.Redis 数据结构与常用命令

> 正文内容待完善。

系统讲解Redis 数据结构与常用命令，覆盖核心概念、实践方法与常见问题，是“Redis”学习路线的第 1 步。', '系统讲解Redis 数据结构与常用命令，覆盖核心概念、实践方法与常见问题，是“Redis”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Redis';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='01.Redis 数据结构与常用命令' AND c.name='Redis';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Redis' WHERE a.title='01.Redis 数据结构与常用命令' AND c.name='Redis';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.持久化、主从、哨兵与集群', '# 02.持久化、主从、哨兵与集群

> 正文内容待完善。

系统讲解持久化、主从、哨兵与集群，覆盖核心概念、实践方法与常见问题，是“Redis”学习路线的第 2 步。', '系统讲解持久化、主从、哨兵与集群，覆盖核心概念、实践方法与常见问题，是“Redis”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Redis';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='02.持久化、主从、哨兵与集群' AND c.name='Redis';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Redis' WHERE a.title='02.持久化、主从、哨兵与集群' AND c.name='Redis';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.缓存设计、分布式锁与一致性', '# 03.缓存设计、分布式锁与一致性

> 正文内容待完善。

系统讲解缓存设计、分布式锁与一致性，覆盖核心概念、实践方法与常见问题，是“Redis”学习路线的第 3 步。', '系统讲解缓存设计、分布式锁与一致性，覆盖核心概念、实践方法与常见问题，是“Redis”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Redis';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='03.缓存设计、分布式锁与一致性' AND c.name='Redis';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Redis' WHERE a.title='03.缓存设计、分布式锁与一致性' AND c.name='Redis';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.MongoDB 文档模型与 CRUD', '# 01.MongoDB 文档模型与 CRUD

> 正文内容待完善。

系统讲解MongoDB 文档模型与 CRUD，覆盖核心概念、实践方法与常见问题，是“MongoDB”学习路线的第 1 步。', '系统讲解MongoDB 文档模型与 CRUD，覆盖核心概念、实践方法与常见问题，是“MongoDB”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='MongoDB';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='01.MongoDB 文档模型与 CRUD' AND c.name='MongoDB';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MongoDB' WHERE a.title='01.MongoDB 文档模型与 CRUD' AND c.name='MongoDB';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.聚合管道、索引与查询优化', '# 02.聚合管道、索引与查询优化

> 正文内容待完善。

系统讲解聚合管道、索引与查询优化，覆盖核心概念、实践方法与常见问题，是“MongoDB”学习路线的第 2 步。', '系统讲解聚合管道、索引与查询优化，覆盖核心概念、实践方法与常见问题，是“MongoDB”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='MongoDB';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='02.聚合管道、索引与查询优化' AND c.name='MongoDB';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MongoDB' WHERE a.title='02.聚合管道、索引与查询优化' AND c.name='MongoDB';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.复制集、分片与应用集成', '# 03.复制集、分片与应用集成

> 正文内容待完善。

系统讲解复制集、分片与应用集成，覆盖核心概念、实践方法与常见问题，是“MongoDB”学习路线的第 3 步。', '系统讲解复制集、分片与应用集成，覆盖核心概念、实践方法与常见问题，是“MongoDB”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='MongoDB';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='03.复制集、分片与应用集成' AND c.name='MongoDB';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MongoDB' WHERE a.title='03.复制集、分片与应用集成' AND c.name='MongoDB';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.JDBC、连接池与事务边界', '# 01.JDBC、连接池与事务边界

> 正文内容待完善。

系统讲解JDBC、连接池与事务边界，覆盖核心概念、实践方法与常见问题，是“数据访问实践”学习路线的第 1 步。', '系统讲解JDBC、连接池与事务边界，覆盖核心概念、实践方法与常见问题，是“数据访问实践”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='数据访问实践';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='01.JDBC、连接池与事务边界' AND c.name='数据访问实践';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='数据访问实践' WHERE a.title='01.JDBC、连接池与事务边界' AND c.name='数据访问实践';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.ORM 选型、N+1 问题与批量处理', '# 02.ORM 选型、N+1 问题与批量处理

> 正文内容待完善。

系统讲解ORM 选型、N+1 问题与批量处理，覆盖核心概念、实践方法与常见问题，是“数据访问实践”学习路线的第 2 步。', '系统讲解ORM 选型、N+1 问题与批量处理，覆盖核心概念、实践方法与常见问题，是“数据访问实践”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='数据访问实践';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='02.ORM 选型、N+1 问题与批量处理' AND c.name='数据访问实践';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='数据访问实践' WHERE a.title='02.ORM 选型、N+1 问题与批量处理' AND c.name='数据访问实践';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.数据库迁移、审计与敏感数据保护', '# 03.数据库迁移、审计与敏感数据保护

> 正文内容待完善。

系统讲解数据库迁移、审计与敏感数据保护，覆盖核心概念、实践方法与常见问题，是“数据访问实践”学习路线的第 3 步。', '系统讲解数据库迁移、审计与敏感数据保护，覆盖核心概念、实践方法与常见问题，是“数据访问实践”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='数据访问实践';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='MySQL' WHERE a.title='03.数据库迁移、审计与敏感数据保护' AND c.name='数据访问实践';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='数据访问实践' WHERE a.title='03.数据库迁移、审计与敏感数据保护' AND c.name='数据访问实践';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Git 安装、仓库与基础命令', '# 01.Git 安装、仓库与基础命令

> 正文内容待完善。

系统讲解Git 安装、仓库与基础命令，覆盖核心概念、实践方法与常见问题，是“Git 教程”学习路线的第 1 步。', '系统讲解Git 安装、仓库与基础命令，覆盖核心概念、实践方法与常见问题，是“Git 教程”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Git 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='01.Git 安装、仓库与基础命令' AND c.name='Git 教程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.分支、合并、Rebase 与冲突处理', '# 02.分支、合并、Rebase 与冲突处理

> 正文内容待完善。

系统讲解分支、合并、Rebase 与冲突处理，覆盖核心概念、实践方法与常见问题，是“Git 教程”学习路线的第 2 步。', '系统讲解分支、合并、Rebase 与冲突处理，覆盖核心概念、实践方法与常见问题，是“Git 教程”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Git 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='02.分支、合并、Rebase 与冲突处理' AND c.name='Git 教程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.团队协作、提交规范与历史恢复', '# 03.团队协作、提交规范与历史恢复

> 正文内容待完善。

系统讲解团队协作、提交规范与历史恢复，覆盖核心概念、实践方法与常见问题，是“Git 教程”学习路线的第 3 步。', '系统讲解团队协作、提交规范与历史恢复，覆盖核心概念、实践方法与常见问题，是“Git 教程”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Git 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='03.团队协作、提交规范与历史恢复' AND c.name='Git 教程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Docker 安装、镜像与容器', '# 01.Docker 安装、镜像与容器

> 正文内容待完善。

系统讲解Docker 安装、镜像与容器，覆盖核心概念、实践方法与常见问题，是“Docker 教程”学习路线的第 1 步。', '系统讲解Docker 安装、镜像与容器，覆盖核心概念、实践方法与常见问题，是“Docker 教程”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Docker 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='01.Docker 安装、镜像与容器' AND c.name='Docker 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Docker' WHERE a.title='01.Docker 安装、镜像与容器' AND c.name='Docker 教程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.Dockerfile、网络与数据卷', '# 02.Dockerfile、网络与数据卷

> 正文内容待完善。

系统讲解Dockerfile、网络与数据卷，覆盖核心概念、实践方法与常见问题，是“Docker 教程”学习路线的第 2 步。', '系统讲解Dockerfile、网络与数据卷，覆盖核心概念、实践方法与常见问题，是“Docker 教程”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Docker 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='02.Dockerfile、网络与数据卷' AND c.name='Docker 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Docker' WHERE a.title='02.Dockerfile、网络与数据卷' AND c.name='Docker 教程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Docker Compose 与生产实践', '# 03.Docker Compose 与生产实践

> 正文内容待完善。

系统讲解Docker Compose 与生产实践，覆盖核心概念、实践方法与常见问题，是“Docker 教程”学习路线的第 3 步。', '系统讲解Docker Compose 与生产实践，覆盖核心概念、实践方法与常见问题，是“Docker 教程”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Docker 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='03.Docker Compose 与生产实践' AND c.name='Docker 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Docker' WHERE a.title='03.Docker Compose 与生产实践' AND c.name='Docker 教程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.IDEA 安装、项目与模块管理', '# 01.IDEA 安装、项目与模块管理

> 正文内容待完善。

系统讲解IDEA 安装、项目与模块管理，覆盖核心概念、实践方法与常见问题，是“IntelliJ IDEA”学习路线的第 1 步。', '系统讲解IDEA 安装、项目与模块管理，覆盖核心概念、实践方法与常见问题，是“IntelliJ IDEA”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='IntelliJ IDEA';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='01.IDEA 安装、项目与模块管理' AND c.name='IntelliJ IDEA';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='IntelliJ IDEA' WHERE a.title='01.IDEA 安装、项目与模块管理' AND c.name='IntelliJ IDEA';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.代码导航、重构与调试技巧', '# 02.代码导航、重构与调试技巧

> 正文内容待完善。

系统讲解代码导航、重构与调试技巧，覆盖核心概念、实践方法与常见问题，是“IntelliJ IDEA”学习路线的第 2 步。', '系统讲解代码导航、重构与调试技巧，覆盖核心概念、实践方法与常见问题，是“IntelliJ IDEA”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='IntelliJ IDEA';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='02.代码导航、重构与调试技巧' AND c.name='IntelliJ IDEA';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='IntelliJ IDEA' WHERE a.title='02.代码导航、重构与调试技巧' AND c.name='IntelliJ IDEA';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.插件、快捷键与团队配置', '# 03.插件、快捷键与团队配置

> 正文内容待完善。

系统讲解插件、快捷键与团队配置，覆盖核心概念、实践方法与常见问题，是“IntelliJ IDEA”学习路线的第 3 步。', '系统讲解插件、快捷键与团队配置，覆盖核心概念、实践方法与常见问题，是“IntelliJ IDEA”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='IntelliJ IDEA';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='03.插件、快捷键与团队配置' AND c.name='IntelliJ IDEA';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='IntelliJ IDEA' WHERE a.title='03.插件、快捷键与团队配置' AND c.name='IntelliJ IDEA';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.VS Code 安装、工作区与常用插件', '# 01.VS Code 安装、工作区与常用插件

> 正文内容待完善。

系统讲解VS Code 安装、工作区与常用插件，覆盖核心概念、实践方法与常见问题，是“VS Code”学习路线的第 1 步。', '系统讲解VS Code 安装、工作区与常用插件，覆盖核心概念、实践方法与常见问题，是“VS Code”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='VS Code';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='01.VS Code 安装、工作区与常用插件' AND c.name='VS Code';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='VS Code' WHERE a.title='01.VS Code 安装、工作区与常用插件' AND c.name='VS Code';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.前端调试、任务与代码片段', '# 02.前端调试、任务与代码片段

> 正文内容待完善。

系统讲解前端调试、任务与代码片段，覆盖核心概念、实践方法与常见问题，是“VS Code”学习路线的第 2 步。', '系统讲解前端调试、任务与代码片段，覆盖核心概念、实践方法与常见问题，是“VS Code”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='VS Code';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='02.前端调试、任务与代码片段' AND c.name='VS Code';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='VS Code' WHERE a.title='02.前端调试、任务与代码片段' AND c.name='VS Code';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Remote SSH、容器与远程开发', '# 03.Remote SSH、容器与远程开发

> 正文内容待完善。

系统讲解Remote SSH、容器与远程开发，覆盖核心概念、实践方法与常见问题，是“VS Code”学习路线的第 3 步。', '系统讲解Remote SSH、容器与远程开发，覆盖核心概念、实践方法与常见问题，是“VS Code”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='VS Code';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='03.Remote SSH、容器与远程开发' AND c.name='VS Code';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='VS Code' WHERE a.title='03.Remote SSH、容器与远程开发' AND c.name='VS Code';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Maven 生命周期、依赖与仓库', '# 01.Maven 生命周期、依赖与仓库

> 正文内容待完善。

系统讲解Maven 生命周期、依赖与仓库，覆盖核心概念、实践方法与常见问题，是“Maven 与 Gradle”学习路线的第 1 步。', '系统讲解Maven 生命周期、依赖与仓库，覆盖核心概念、实践方法与常见问题，是“Maven 与 Gradle”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Maven 与 Gradle';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='01.Maven 生命周期、依赖与仓库' AND c.name='Maven 与 Gradle';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Maven 与 Gradle' WHERE a.title='01.Maven 生命周期、依赖与仓库' AND c.name='Maven 与 Gradle';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.Maven 多模块、Profile 与插件', '# 02.Maven 多模块、Profile 与插件

> 正文内容待完善。

系统讲解Maven 多模块、Profile 与插件，覆盖核心概念、实践方法与常见问题，是“Maven 与 Gradle”学习路线的第 2 步。', '系统讲解Maven 多模块、Profile 与插件，覆盖核心概念、实践方法与常见问题，是“Maven 与 Gradle”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Maven 与 Gradle';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='02.Maven 多模块、Profile 与插件' AND c.name='Maven 与 Gradle';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Maven 与 Gradle' WHERE a.title='02.Maven 多模块、Profile 与插件' AND c.name='Maven 与 Gradle';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.Gradle 基础与构建工具选型', '# 03.Gradle 基础与构建工具选型

> 正文内容待完善。

系统讲解Gradle 基础与构建工具选型，覆盖核心概念、实践方法与常见问题，是“Maven 与 Gradle”学习路线的第 3 步。', '系统讲解Gradle 基础与构建工具选型，覆盖核心概念、实践方法与常见问题，是“Maven 与 Gradle”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Maven 与 Gradle';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='03.Gradle 基础与构建工具选型' AND c.name='Maven 与 Gradle';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Maven 与 Gradle' WHERE a.title='03.Gradle 基础与构建工具选型' AND c.name='Maven 与 Gradle';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Linux 文件、目录与文本处理', '# 01.Linux 文件、目录与文本处理

> 正文内容待完善。

系统讲解Linux 文件、目录与文本处理，覆盖核心概念、实践方法与常见问题，是“Linux 教程”学习路线的第 1 步。', '系统讲解Linux 文件、目录与文本处理，覆盖核心概念、实践方法与常见问题，是“Linux 教程”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Linux 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='01.Linux 文件、目录与文本处理' AND c.name='Linux 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Linux' WHERE a.title='01.Linux 文件、目录与文本处理' AND c.name='Linux 教程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.用户权限、进程与系统服务', '# 02.用户权限、进程与系统服务

> 正文内容待完善。

系统讲解用户权限、进程与系统服务，覆盖核心概念、实践方法与常见问题，是“Linux 教程”学习路线的第 2 步。', '系统讲解用户权限、进程与系统服务，覆盖核心概念、实践方法与常见问题，是“Linux 教程”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Linux 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='02.用户权限、进程与系统服务' AND c.name='Linux 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Linux' WHERE a.title='02.用户权限、进程与系统服务' AND c.name='Linux 教程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.网络诊断、Shell 脚本与日志排查', '# 03.网络诊断、Shell 脚本与日志排查

> 正文内容待完善。

系统讲解网络诊断、Shell 脚本与日志排查，覆盖核心概念、实践方法与常见问题，是“Linux 教程”学习路线的第 3 步。', '系统讲解网络诊断、Shell 脚本与日志排查，覆盖核心概念、实践方法与常见问题，是“Linux 教程”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Linux 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='03.网络诊断、Shell 脚本与日志排查' AND c.name='Linux 教程';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Linux' WHERE a.title='03.网络诊断、Shell 脚本与日志排查' AND c.name='Linux 教程';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.持续集成、流水线与分支策略', '# 01.持续集成、流水线与分支策略

> 正文内容待完善。

系统讲解持续集成、流水线与分支策略，覆盖核心概念、实践方法与常见问题，是“CI/CD”学习路线的第 1 步。', '系统讲解持续集成、流水线与分支策略，覆盖核心概念、实践方法与常见问题，是“CI/CD”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='CI/CD';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='01.持续集成、流水线与分支策略' AND c.name='CI/CD';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='CI/CD' WHERE a.title='01.持续集成、流水线与分支策略' AND c.name='CI/CD';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.GitHub Actions 自动测试与构建', '# 02.GitHub Actions 自动测试与构建

> 正文内容待完善。

系统讲解GitHub Actions 自动测试与构建，覆盖核心概念、实践方法与常见问题，是“CI/CD”学习路线的第 2 步。', '系统讲解GitHub Actions 自动测试与构建，覆盖核心概念、实践方法与常见问题，是“CI/CD”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='CI/CD';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='02.GitHub Actions 自动测试与构建' AND c.name='CI/CD';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='CI/CD' WHERE a.title='02.GitHub Actions 自动测试与构建' AND c.name='CI/CD';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.制品管理、自动部署与回滚策略', '# 03.制品管理、自动部署与回滚策略

> 正文内容待完善。

系统讲解制品管理、自动部署与回滚策略，覆盖核心概念、实践方法与常见问题，是“CI/CD”学习路线的第 3 步。', '系统讲解制品管理、自动部署与回滚策略，覆盖核心概念、实践方法与常见问题，是“CI/CD”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='CI/CD';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Git' WHERE a.title='03.制品管理、自动部署与回滚策略' AND c.name='CI/CD';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='CI/CD' WHERE a.title='03.制品管理、自动部署与回滚策略' AND c.name='CI/CD';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.从业务需求到用户故事与验收标准', '# 01.从业务需求到用户故事与验收标准

> 正文内容待完善。

系统讲解从业务需求到用户故事与验收标准，覆盖核心概念、实践方法与常见问题，是“需求与架构设计”学习路线的第 1 步。', '系统讲解从业务需求到用户故事与验收标准，覆盖核心概念、实践方法与常见问题，是“需求与架构设计”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='需求与架构设计';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.从业务需求到用户故事与验收标准' AND c.name='需求与架构设计';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='01.从业务需求到用户故事与验收标准' AND c.name='需求与架构设计';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='需求与架构设计' WHERE a.title='01.从业务需求到用户故事与验收标准' AND c.name='需求与架构设计';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.系统分层、模块边界与领域建模', '# 02.系统分层、模块边界与领域建模

> 正文内容待完善。

系统讲解系统分层、模块边界与领域建模，覆盖核心概念、实践方法与常见问题，是“需求与架构设计”学习路线的第 2 步。', '系统讲解系统分层、模块边界与领域建模，覆盖核心概念、实践方法与常见问题，是“需求与架构设计”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='需求与架构设计';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.系统分层、模块边界与领域建模' AND c.name='需求与架构设计';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='02.系统分层、模块边界与领域建模' AND c.name='需求与架构设计';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='需求与架构设计' WHERE a.title='02.系统分层、模块边界与领域建模' AND c.name='需求与架构设计';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.技术选型、接口规范与架构文档', '# 03.技术选型、接口规范与架构文档

> 正文内容待完善。

系统讲解技术选型、接口规范与架构文档，覆盖核心概念、实践方法与常见问题，是“需求与架构设计”学习路线的第 3 步。', '系统讲解技术选型、接口规范与架构文档，覆盖核心概念、实践方法与常见问题，是“需求与架构设计”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='需求与架构设计';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.技术选型、接口规范与架构文档' AND c.name='需求与架构设计';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='03.技术选型、接口规范与架构文档' AND c.name='需求与架构设计';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='需求与架构设计' WHERE a.title='03.技术选型、接口规范与架构文档' AND c.name='需求与架构设计';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.搭建 Spring Boot 后端工程骨架', '# 01.搭建 Spring Boot 后端工程骨架

> 正文内容待完善。

系统讲解搭建 Spring Boot 后端工程骨架，覆盖核心概念、实践方法与常见问题，是“后端项目实战”学习路线的第 1 步。', '系统讲解搭建 Spring Boot 后端工程骨架，覆盖核心概念、实践方法与常见问题，是“后端项目实战”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='后端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.搭建 Spring Boot 后端工程骨架' AND c.name='后端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='01.搭建 Spring Boot 后端工程骨架' AND c.name='后端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='后端项目实战' WHERE a.title='01.搭建 Spring Boot 后端工程骨架' AND c.name='后端项目实战';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.实现 CRUD、校验与统一异常处理', '# 02.实现 CRUD、校验与统一异常处理

> 正文内容待完善。

系统讲解实现 CRUD、校验与统一异常处理，覆盖核心概念、实践方法与常见问题，是“后端项目实战”学习路线的第 2 步。', '系统讲解实现 CRUD、校验与统一异常处理，覆盖核心概念、实践方法与常见问题，是“后端项目实战”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='后端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.实现 CRUD、校验与统一异常处理' AND c.name='后端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='02.实现 CRUD、校验与统一异常处理' AND c.name='后端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='后端项目实战' WHERE a.title='02.实现 CRUD、校验与统一异常处理' AND c.name='后端项目实战';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.权限、日志、缓存与后端测试', '# 03.权限、日志、缓存与后端测试

> 正文内容待完善。

系统讲解权限、日志、缓存与后端测试，覆盖核心概念、实践方法与常见问题，是“后端项目实战”学习路线的第 3 步。', '系统讲解权限、日志、缓存与后端测试，覆盖核心概念、实践方法与常见问题，是“后端项目实战”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='后端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.权限、日志、缓存与后端测试' AND c.name='后端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='03.权限、日志、缓存与后端测试' AND c.name='后端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='后端项目实战' WHERE a.title='03.权限、日志、缓存与后端测试' AND c.name='后端项目实战';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.搭建 Vue 3 前端工程与路由', '# 01.搭建 Vue 3 前端工程与路由

> 正文内容待完善。

系统讲解搭建 Vue 3 前端工程与路由，覆盖核心概念、实践方法与常见问题，是“前端项目实战”学习路线的第 1 步。', '系统讲解搭建 Vue 3 前端工程与路由，覆盖核心概念、实践方法与常见问题，是“前端项目实战”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='前端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.搭建 Vue 3 前端工程与路由' AND c.name='前端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='01.搭建 Vue 3 前端工程与路由' AND c.name='前端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='前端项目实战' WHERE a.title='01.搭建 Vue 3 前端工程与路由' AND c.name='前端项目实战';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.实现列表、表单与接口联调', '# 02.实现列表、表单与接口联调

> 正文内容待完善。

系统讲解实现列表、表单与接口联调，覆盖核心概念、实践方法与常见问题，是“前端项目实战”学习路线的第 2 步。', '系统讲解实现列表、表单与接口联调，覆盖核心概念、实践方法与常见问题，是“前端项目实战”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='前端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.实现列表、表单与接口联调' AND c.name='前端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='02.实现列表、表单与接口联调' AND c.name='前端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='前端项目实战' WHERE a.title='02.实现列表、表单与接口联调' AND c.name='前端项目实战';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.状态管理、权限路由与前端测试', '# 03.状态管理、权限路由与前端测试

> 正文内容待完善。

系统讲解状态管理、权限路由与前端测试，覆盖核心概念、实践方法与常见问题，是“前端项目实战”学习路线的第 3 步。', '系统讲解状态管理、权限路由与前端测试，覆盖核心概念、实践方法与常见问题，是“前端项目实战”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='前端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.状态管理、权限路由与前端测试' AND c.name='前端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='03.状态管理、权限路由与前端测试' AND c.name='前端项目实战';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='前端项目实战' WHERE a.title='03.状态管理、权限路由与前端测试' AND c.name='前端项目实战';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.前后端接口契约与联调流程', '# 01.前后端接口契约与联调流程

> 正文内容待完善。

系统讲解前后端接口契约与联调流程，覆盖核心概念、实践方法与常见问题，是“全栈联调”学习路线的第 1 步。', '系统讲解前后端接口契约与联调流程，覆盖核心概念、实践方法与常见问题，是“全栈联调”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='全栈联调';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.前后端接口契约与联调流程' AND c.name='全栈联调';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='01.前后端接口契约与联调流程' AND c.name='全栈联调';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='全栈联调' WHERE a.title='01.前后端接口契约与联调流程' AND c.name='全栈联调';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.登录认证、跨域与权限联调', '# 02.登录认证、跨域与权限联调

> 正文内容待完善。

系统讲解登录认证、跨域与权限联调，覆盖核心概念、实践方法与常见问题，是“全栈联调”学习路线的第 2 步。', '系统讲解登录认证、跨域与权限联调，覆盖核心概念、实践方法与常见问题，是“全栈联调”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='全栈联调';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.登录认证、跨域与权限联调' AND c.name='全栈联调';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='02.登录认证、跨域与权限联调' AND c.name='全栈联调';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='全栈联调' WHERE a.title='02.登录认证、跨域与权限联调' AND c.name='全栈联调';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.文件上传、消息通知与异常排查', '# 03.文件上传、消息通知与异常排查

> 正文内容待完善。

系统讲解文件上传、消息通知与异常排查，覆盖核心概念、实践方法与常见问题，是“全栈联调”学习路线的第 3 步。', '系统讲解文件上传、消息通知与异常排查，覆盖核心概念、实践方法与常见问题，是“全栈联调”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='全栈联调';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.文件上传、消息通知与异常排查' AND c.name='全栈联调';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='03.文件上传、消息通知与异常排查' AND c.name='全栈联调';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='全栈联调' WHERE a.title='03.文件上传、消息通知与异常排查' AND c.name='全栈联调';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.单元测试、集成测试与测试数据', '# 01.单元测试、集成测试与测试数据

> 正文内容待完善。

系统讲解单元测试、集成测试与测试数据，覆盖核心概念、实践方法与常见问题，是“测试与质量保障”学习路线的第 1 步。', '系统讲解单元测试、集成测试与测试数据，覆盖核心概念、实践方法与常见问题，是“测试与质量保障”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='测试与质量保障';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.单元测试、集成测试与测试数据' AND c.name='测试与质量保障';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='01.单元测试、集成测试与测试数据' AND c.name='测试与质量保障';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='测试与质量保障' WHERE a.title='01.单元测试、集成测试与测试数据' AND c.name='测试与质量保障';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.接口安全、代码评审与质量门禁', '# 02.接口安全、代码评审与质量门禁

> 正文内容待完善。

系统讲解接口安全、代码评审与质量门禁，覆盖核心概念、实践方法与常见问题，是“测试与质量保障”学习路线的第 2 步。', '系统讲解接口安全、代码评审与质量门禁，覆盖核心概念、实践方法与常见问题，是“测试与质量保障”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='测试与质量保障';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.接口安全、代码评审与质量门禁' AND c.name='测试与质量保障';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='02.接口安全、代码评审与质量门禁' AND c.name='测试与质量保障';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='测试与质量保障' WHERE a.title='02.接口安全、代码评审与质量门禁' AND c.name='测试与质量保障';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.性能测试、瓶颈定位与优化', '# 03.性能测试、瓶颈定位与优化

> 正文内容待完善。

系统讲解性能测试、瓶颈定位与优化，覆盖核心概念、实践方法与常见问题，是“测试与质量保障”学习路线的第 3 步。', '系统讲解性能测试、瓶颈定位与优化，覆盖核心概念、实践方法与常见问题，是“测试与质量保障”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='测试与质量保障';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.性能测试、瓶颈定位与优化' AND c.name='测试与质量保障';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='03.性能测试、瓶颈定位与优化' AND c.name='测试与质量保障';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='测试与质量保障' WHERE a.title='03.性能测试、瓶颈定位与优化' AND c.name='测试与质量保障';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.项目容器化与环境配置', '# 01.项目容器化与环境配置

> 正文内容待完善。

系统讲解项目容器化与环境配置，覆盖核心概念、实践方法与常见问题，是“部署与运维”学习路线的第 1 步。', '系统讲解项目容器化与环境配置，覆盖核心概念、实践方法与常见问题，是“部署与运维”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='部署与运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='01.项目容器化与环境配置' AND c.name='部署与运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='01.项目容器化与环境配置' AND c.name='部署与运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='部署与运维' WHERE a.title='01.项目容器化与环境配置' AND c.name='部署与运维';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.Nginx、HTTPS 与生产部署', '# 02.Nginx、HTTPS 与生产部署

> 正文内容待完善。

系统讲解Nginx、HTTPS 与生产部署，覆盖核心概念、实践方法与常见问题，是“部署与运维”学习路线的第 2 步。', '系统讲解Nginx、HTTPS 与生产部署，覆盖核心概念、实践方法与常见问题，是“部署与运维”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='部署与运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='02.Nginx、HTTPS 与生产部署' AND c.name='部署与运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='02.Nginx、HTTPS 与生产部署' AND c.name='部署与运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='部署与运维' WHERE a.title='02.Nginx、HTTPS 与生产部署' AND c.name='部署与运维';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.日志、监控、备份与故障恢复', '# 03.日志、监控、备份与故障恢复

> 正文内容待完善。

系统讲解日志、监控、备份与故障恢复，覆盖核心概念、实践方法与常见问题，是“部署与运维”学习路线的第 3 步。', '系统讲解日志、监控、备份与故障恢复，覆盖核心概念、实践方法与常见问题，是“部署与运维”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='部署与运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Spring Boot' WHERE a.title='03.日志、监控、备份与故障恢复' AND c.name='部署与运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Vue 3' WHERE a.title='03.日志、监控、备份与故障恢复' AND c.name='部署与运维';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='部署与运维' WHERE a.title='03.日志、监控、备份与故障恢复' AND c.name='部署与运维';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.制定可执行的编程学习路线', '# 01.制定可执行的编程学习路线

> 正文内容待完善。

系统讲解制定可执行的编程学习路线，覆盖核心概念、实践方法与常见问题，是“学习方法”学习路线的第 1 步。', '系统讲解制定可执行的编程学习路线，覆盖核心概念、实践方法与常见问题，是“学习方法”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='学习方法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.制定可执行的编程学习路线' AND c.name='学习方法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='学习方法' WHERE a.title='01.制定可执行的编程学习路线' AND c.name='学习方法';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.技术笔记、复盘与知识体系构建', '# 02.技术笔记、复盘与知识体系构建

> 正文内容待完善。

系统讲解技术笔记、复盘与知识体系构建，覆盖核心概念、实践方法与常见问题，是“学习方法”学习路线的第 2 步。', '系统讲解技术笔记、复盘与知识体系构建，覆盖核心概念、实践方法与常见问题，是“学习方法”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='学习方法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.技术笔记、复盘与知识体系构建' AND c.name='学习方法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='学习方法' WHERE a.title='02.技术笔记、复盘与知识体系构建' AND c.name='学习方法';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.项目驱动学习与刻意练习', '# 03.项目驱动学习与刻意练习

> 正文内容待完善。

系统讲解项目驱动学习与刻意练习，覆盖核心概念、实践方法与常见问题，是“学习方法”学习路线的第 3 步。', '系统讲解项目驱动学习与刻意练习，覆盖核心概念、实践方法与常见问题，是“学习方法”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='学习方法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.项目驱动学习与刻意练习' AND c.name='学习方法';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='学习方法' WHERE a.title='03.项目驱动学习与刻意练习' AND c.name='学习方法';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.Java 基础与集合高频面试题', '# 01.Java 基础与集合高频面试题

> 正文内容待完善。

系统讲解Java 基础与集合高频面试题，覆盖核心概念、实践方法与常见问题，是“Java 面试”学习路线的第 1 步。', '系统讲解Java 基础与集合高频面试题，覆盖核心概念、实践方法与常见问题，是“Java 面试”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='Java 面试';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.Java 基础与集合高频面试题' AND c.name='Java 面试';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 面试' WHERE a.title='01.Java 基础与集合高频面试题' AND c.name='Java 面试';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.并发编程与 JVM 面试要点', '# 02.并发编程与 JVM 面试要点

> 正文内容待完善。

系统讲解并发编程与 JVM 面试要点，覆盖核心概念、实践方法与常见问题，是“Java 面试”学习路线的第 2 步。', '系统讲解并发编程与 JVM 面试要点，覆盖核心概念、实践方法与常见问题，是“Java 面试”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='Java 面试';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.并发编程与 JVM 面试要点' AND c.name='Java 面试';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 面试' WHERE a.title='02.并发编程与 JVM 面试要点' AND c.name='Java 面试';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.项目经历梳理与系统设计面试', '# 03.项目经历梳理与系统设计面试

> 正文内容待完善。

系统讲解项目经历梳理与系统设计面试，覆盖核心概念、实践方法与常见问题，是“Java 面试”学习路线的第 3 步。', '系统讲解项目经历梳理与系统设计面试，覆盖核心概念、实践方法与常见问题，是“Java 面试”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='Java 面试';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.项目经历梳理与系统设计面试' AND c.name='Java 面试';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java 面试' WHERE a.title='03.项目经历梳理与系统设计面试' AND c.name='Java 面试';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.复杂度、数组、链表与哈希表', '# 01.复杂度、数组、链表与哈希表

> 正文内容待完善。

系统讲解复杂度、数组、链表与哈希表，覆盖核心概念、实践方法与常见问题，是“算法与数据结构”学习路线的第 1 步。', '系统讲解复杂度、数组、链表与哈希表，覆盖核心概念、实践方法与常见问题，是“算法与数据结构”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='算法与数据结构';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.复杂度、数组、链表与哈希表' AND c.name='算法与数据结构';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='算法与数据结构' WHERE a.title='01.复杂度、数组、链表与哈希表' AND c.name='算法与数据结构';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.栈、队列、树与图基础', '# 02.栈、队列、树与图基础

> 正文内容待完善。

系统讲解栈、队列、树与图基础，覆盖核心概念、实践方法与常见问题，是“算法与数据结构”学习路线的第 2 步。', '系统讲解栈、队列、树与图基础，覆盖核心概念、实践方法与常见问题，是“算法与数据结构”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='算法与数据结构';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.栈、队列、树与图基础' AND c.name='算法与数据结构';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='算法与数据结构' WHERE a.title='02.栈、队列、树与图基础' AND c.name='算法与数据结构';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.排序、二分、递归与动态规划', '# 03.排序、二分、递归与动态规划

> 正文内容待完善。

系统讲解排序、二分、递归与动态规划，覆盖核心概念、实践方法与常见问题，是“算法与数据结构”学习路线的第 3 步。', '系统讲解排序、二分、递归与动态规划，覆盖核心概念、实践方法与常见问题，是“算法与数据结构”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='算法与数据结构';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.排序、二分、递归与动态规划' AND c.name='算法与数据结构';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='算法与数据结构' WHERE a.title='03.排序、二分、递归与动态规划' AND c.name='算法与数据结构';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.设计原则与创建型模式', '# 01.设计原则与创建型模式

> 正文内容待完善。

系统讲解设计原则与创建型模式，覆盖核心概念、实践方法与常见问题，是“设计模式”学习路线的第 1 步。', '系统讲解设计原则与创建型模式，覆盖核心概念、实践方法与常见问题，是“设计模式”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='设计模式';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.设计原则与创建型模式' AND c.name='设计模式';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='设计模式' WHERE a.title='01.设计原则与创建型模式' AND c.name='设计模式';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.结构型模式与代码重构', '# 02.结构型模式与代码重构

> 正文内容待完善。

系统讲解结构型模式与代码重构，覆盖核心概念、实践方法与常见问题，是“设计模式”学习路线的第 2 步。', '系统讲解结构型模式与代码重构，覆盖核心概念、实践方法与常见问题，是“设计模式”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='设计模式';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.结构型模式与代码重构' AND c.name='设计模式';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='设计模式' WHERE a.title='02.结构型模式与代码重构' AND c.name='设计模式';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.行为型模式与实际项目应用', '# 03.行为型模式与实际项目应用

> 正文内容待完善。

系统讲解行为型模式与实际项目应用，覆盖核心概念、实践方法与常见问题，是“设计模式”学习路线的第 3 步。', '系统讲解行为型模式与实际项目应用，覆盖核心概念、实践方法与常见问题，是“设计模式”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='设计模式';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.行为型模式与实际项目应用' AND c.name='设计模式';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='设计模式' WHERE a.title='03.行为型模式与实际项目应用' AND c.name='设计模式';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.计算机组成、进程与内存管理', '# 01.计算机组成、进程与内存管理

> 正文内容待完善。

系统讲解计算机组成、进程与内存管理，覆盖核心概念、实践方法与常见问题，是“计算机基础”学习路线的第 1 步。', '系统讲解计算机组成、进程与内存管理，覆盖核心概念、实践方法与常见问题，是“计算机基础”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='计算机基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.计算机组成、进程与内存管理' AND c.name='计算机基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='计算机基础' WHERE a.title='01.计算机组成、进程与内存管理' AND c.name='计算机基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.TCP/IP、HTTP 与网络排查', '# 02.TCP/IP、HTTP 与网络排查

> 正文内容待完善。

系统讲解TCP/IP、HTTP 与网络排查，覆盖核心概念、实践方法与常见问题，是“计算机基础”学习路线的第 2 步。', '系统讲解TCP/IP、HTTP 与网络排查，覆盖核心概念、实践方法与常见问题，是“计算机基础”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='计算机基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.TCP/IP、HTTP 与网络排查' AND c.name='计算机基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='计算机基础' WHERE a.title='02.TCP/IP、HTTP 与网络排查' AND c.name='计算机基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.编码、编译、数据表示与安全基础', '# 03.编码、编译、数据表示与安全基础

> 正文内容待完善。

系统讲解编码、编译、数据表示与安全基础，覆盖核心概念、实践方法与常见问题，是“计算机基础”学习路线的第 3 步。', '系统讲解编码、编译、数据表示与安全基础，覆盖核心概念、实践方法与常见问题，是“计算机基础”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='计算机基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.编码、编译、数据表示与安全基础' AND c.name='计算机基础';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='计算机基础' WHERE a.title='03.编码、编译、数据表示与安全基础' AND c.name='计算机基础';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '01.开发者简历与作品集建设', '# 01.开发者简历与作品集建设

> 正文内容待完善。

系统讲解开发者简历与作品集建设，覆盖核心概念、实践方法与常见问题，是“职业成长”学习路线的第 1 步。', '系统讲解开发者简历与作品集建设，覆盖核心概念、实践方法与常见问题，是“职业成长”学习路线的第 1 步。', c.id, 1, 1, 0, 1, 0, 0
FROM blog_category c WHERE c.name='职业成长';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='01.开发者简历与作品集建设' AND c.name='职业成长';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='职业成长' WHERE a.title='01.开发者简历与作品集建设' AND c.name='职业成长';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '02.代码沟通、团队协作与技术表达', '# 02.代码沟通、团队协作与技术表达

> 正文内容待完善。

系统讲解代码沟通、团队协作与技术表达，覆盖核心概念、实践方法与常见问题，是“职业成长”学习路线的第 2 步。', '系统讲解代码沟通、团队协作与技术表达，覆盖核心概念、实践方法与常见问题，是“职业成长”学习路线的第 2 步。', c.id, 1, 1, 0, 2, 0, 0
FROM blog_category c WHERE c.name='职业成长';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='02.代码沟通、团队协作与技术表达' AND c.name='职业成长';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='职业成长' WHERE a.title='02.代码沟通、团队协作与技术表达' AND c.name='职业成长';
INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '03.参与开源、持续学习与职业规划', '# 03.参与开源、持续学习与职业规划

> 正文内容待完善。

系统讲解参与开源、持续学习与职业规划，覆盖核心概念、实践方法与常见问题，是“职业成长”学习路线的第 3 步。', '系统讲解参与开源、持续学习与职业规划，覆盖核心概念、实践方法与常见问题，是“职业成长”学习路线的第 3 步。', c.id, 1, 1, 0, 3, 0, 0
FROM blog_category c WHERE c.name='职业成长';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='Java' WHERE a.title='03.参与开源、持续学习与职业规划' AND c.name='职业成长';
INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='职业成长' WHERE a.title='03.参与开源、持续学习与职业规划' AND c.name='职业成长';

-- 共生成 7 个一级分类、50 个二级分类、150 篇文章。
