import fs from 'node:fs'
import path from 'node:path'

const catalog = [
  { name: 'Java 基础', description: '从环境搭建到 JVM 与现代 Java 的系统学习路线', tags: ['Java'], children: [
    ['Java 入门与环境', 'Java 开发环境、工具链与第一个程序', ['Java 开发环境与 JDK 安装', 'IDEA 创建并运行第一个 Java 项目', 'Java 程序编译、运行与常见环境问题']],
    ['Java 基础语法', '变量、运算符、流程控制与数组', ['变量、数据类型与类型转换', '运算符、表达式与流程控制', '数组、方法与参数传递']],
    ['面向对象编程', '类、对象、封装、继承与多态', ['类与对象：从现实模型到 Java 代码', '封装、继承与多态', '抽象类、接口与内部类']],
    ['Java 常用 API', '字符串、时间、数学与工具类', ['String、StringBuilder 与字符串处理', '日期时间 API 与格式化', '包装类、Math、Random 与常用工具']],
    ['集合框架', 'List、Set、Map 及集合选型', ['Collection 体系与 List', 'Set、Map 与哈希原理', '集合遍历、排序与选型实践']],
    ['异常与泛型', '可靠错误处理与类型安全', ['异常体系、捕获与自定义异常', '泛型类、泛型方法与通配符', '注解、反射与运行时类型信息']],
    ['IO 与 NIO', '文件、流、序列化与 NIO', ['字节流、字符流与缓冲流', '文件系统、序列化与资源管理', 'NIO、Channel、Buffer 与文件操作']],
    ['多线程与并发', '线程、锁、线程池与并发工具', ['线程生命周期与创建方式', '线程安全、锁与并发容器', '线程池、CompletableFuture 与并发实践']],
    ['JVM 基础', '内存模型、类加载与垃圾回收', ['JVM 运行时内存区域', '类加载机制与字节码基础', '垃圾回收器、GC 日志与基础调优']],
    ['现代 Java', 'Lambda、Stream、模块与新版本特性', ['Lambda、函数式接口与方法引用', 'Stream API 数据处理实践', 'Java 9 到 Java 21 核心新特性']],
  ]},
  { name: 'Spring 全家桶', description: 'Spring Framework、Boot、Cloud 与企业级开发路线', tags: ['Spring Boot'], children: [
    ['Spring IoC 与 AOP', '容器、依赖注入、Bean 与切面编程', ['Spring 容器、IoC 与依赖注入', 'Bean 生命周期、作用域与扩展点', 'AOP、代理机制与声明式事务']],
    ['Spring MVC', 'Web 请求链路、参数、校验与异常处理', ['Spring MVC 请求处理流程', '参数绑定、数据校验与统一响应', '全局异常处理、拦截器与文件上传']],
    ['Spring Boot', '自动配置、配置管理与应用开发', ['Spring Boot 项目结构与自动配置', '配置文件、环境隔离与类型安全配置', 'Starter、自定义自动配置与启动流程']],
    ['MyBatis 与 MyBatis-Plus', '持久层映射、动态 SQL 与工程实践', ['MyBatis 映射、参数与结果集', '动态 SQL、分页与批量操作', 'MyBatis-Plus CRUD、插件与最佳实践']],
    ['认证与权限', 'Spring Security、Sa-Token 与权限模型', ['认证、授权与 RBAC 权限模型', 'Spring Security 登录与接口鉴权', 'Sa-Token 会话、角色与安全实践']],
    ['Spring Cloud', '微服务治理、配置与调用链路', ['微服务拆分原则与服务注册发现', 'OpenFeign、网关与负载均衡', '配置中心、限流熔断与链路追踪']],
    ['消息与任务', '消息队列、定时任务与异步处理', ['Spring 异步任务与线程池', 'RabbitMQ 消息模型与可靠投递', 'Kafka、定时任务与分布式调度']],
    ['Spring 测试与监控', '自动化测试、日志、指标与健康检查', ['JUnit、Mockito 与单元测试', 'Spring Boot 集成测试与 Testcontainers', 'Actuator、Micrometer 与可观测性']],
  ]},
  { name: '前端开发', description: '从 Web 基础到 Vue、React 与工程化的前端路线', tags: ['JavaScript'], children: [
    ['HTML 与 CSS', '语义化页面、布局、响应式与可访问性', ['HTML 语义化、表单与可访问性', 'CSS 盒模型、Flex 与 Grid 布局', '响应式设计、动画与样式组织']],
    ['JavaScript 基础', '语言基础、对象、异步与浏览器 API', ['JavaScript 类型、作用域与函数', '对象、原型、模块与错误处理', 'Promise、异步编程与浏览器事件']],
    ['TypeScript', '类型系统、泛型与工程应用', ['TypeScript 基础类型与类型收窄', '接口、泛型与高级类型', 'TypeScript 工程配置与类型设计']],
    ['Vue 3', '组合式 API、组件、状态与路由', ['Vue 3 响应式与组合式 API', '组件通信、插槽与生命周期', 'Vue Router、Pinia 与项目组织']],
    ['Element Plus', '后台界面组件与交互实践', ['Element Plus 表单与数据校验', '表格、分页与弹窗交互', '主题定制、按需加载与组件封装']],
    ['React', '组件、Hooks、状态与路由', ['React 组件、JSX 与数据流', 'Hooks、状态管理与性能优化', 'React Router 与项目实战结构']],
    ['前端工程化', 'Vite、测试、规范、性能与部署', ['Vite、模块化与构建配置', 'ESLint、单元测试与端到端测试', '前端性能优化与生产部署']],
  ]},
  { name: '数据库', description: '数据建模、SQL、MySQL、Redis 与 NoSQL 路线', tags: ['MySQL'], children: [
    ['数据库与 SQL 基础', '关系模型、SQL 查询与数据库设计', ['关系型数据库、表与约束', 'SQL 查询、连接与子查询', '范式、ER 模型与数据库设计']],
    ['MySQL 核心', '存储引擎、事务、锁与索引', ['MySQL 架构与 InnoDB 存储引擎', '事务隔离、MVCC 与锁机制', '索引结构、执行计划与查询优化']],
    ['MySQL 运维', '备份恢复、复制、分库分表与监控', ['MySQL 用户权限与安全配置', '备份恢复、主从复制与高可用', '慢查询监控、容量规划与分库分表']],
    ['Redis', '数据结构、持久化、缓存与分布式能力', ['Redis 数据结构与常用命令', '持久化、主从、哨兵与集群', '缓存设计、分布式锁与一致性']],
    ['MongoDB', '文档模型、查询、索引与复制集', ['MongoDB 文档模型与 CRUD', '聚合管道、索引与查询优化', '复制集、分片与应用集成']],
    ['数据访问实践', '连接池、ORM、迁移与数据安全', ['JDBC、连接池与事务边界', 'ORM 选型、N+1 问题与批量处理', '数据库迁移、审计与敏感数据保护']],
  ]},
  { name: '开发工具', description: '版本控制、容器、IDE、构建、Linux 与自动化工具链', tags: ['Git'], children: [
    ['Git 教程', '版本控制、分支协作与问题恢复', ['Git 安装、仓库与基础命令', '分支、合并、Rebase 与冲突处理', '团队协作、提交规范与历史恢复']],
    ['Docker 教程', '镜像、容器、网络、存储与编排', ['Docker 安装、镜像与容器', 'Dockerfile、网络与数据卷', 'Docker Compose 与生产实践']],
    ['IntelliJ IDEA', 'Java 开发环境、调试与效率配置', ['IDEA 安装、项目与模块管理', '代码导航、重构与调试技巧', '插件、快捷键与团队配置']],
    ['VS Code', '编辑器配置、调试与远程开发', ['VS Code 安装、工作区与常用插件', '前端调试、任务与代码片段', 'Remote SSH、容器与远程开发']],
    ['Maven 与 Gradle', '依赖、构建、测试与多模块项目', ['Maven 生命周期、依赖与仓库', 'Maven 多模块、Profile 与插件', 'Gradle 基础与构建工具选型']],
    ['Linux 教程', '命令行、权限、进程、网络与服务', ['Linux 文件、目录与文本处理', '用户权限、进程与系统服务', '网络诊断、Shell 脚本与日志排查']],
    ['CI/CD', '自动测试、制品、发布与回滚', ['持续集成、流水线与分支策略', 'GitHub Actions 自动测试与构建', '制品管理、自动部署与回滚策略']],
  ]},
  { name: '项目实战', description: '从需求、架构、开发、测试到部署的完整项目路线', tags: ['Spring Boot', 'Vue 3'], children: [
    ['需求与架构设计', '需求分析、领域建模与技术方案', ['从业务需求到用户故事与验收标准', '系统分层、模块边界与领域建模', '技术选型、接口规范与架构文档']],
    ['后端项目实战', '接口、持久层、权限与工程质量', ['搭建 Spring Boot 后端工程骨架', '实现 CRUD、校验与统一异常处理', '权限、日志、缓存与后端测试']],
    ['前端项目实战', '页面、状态、接口与用户体验', ['搭建 Vue 3 前端工程与路由', '实现列表、表单与接口联调', '状态管理、权限路由与前端测试']],
    ['全栈联调', '接口契约、认证、文件与实时功能', ['前后端接口契约与联调流程', '登录认证、跨域与权限联调', '文件上传、消息通知与异常排查']],
    ['测试与质量保障', '测试策略、安全、性能与代码评审', ['单元测试、集成测试与测试数据', '接口安全、代码评审与质量门禁', '性能测试、瓶颈定位与优化']],
    ['部署与运维', '容器化、配置、发布与监控', ['项目容器化与环境配置', 'Nginx、HTTPS 与生产部署', '日志、监控、备份与故障恢复']],
  ]},
  { name: '学习随笔', description: '学习方法、面试、算法、设计模式与计算机基础', tags: ['Java'], children: [
    ['学习方法', '路线规划、知识管理与刻意练习', ['制定可执行的编程学习路线', '技术笔记、复盘与知识体系构建', '项目驱动学习与刻意练习']],
    ['Java 面试', '基础、高并发、JVM 与项目表达', ['Java 基础与集合高频面试题', '并发编程与 JVM 面试要点', '项目经历梳理与系统设计面试']],
    ['算法与数据结构', '复杂度、线性结构、树图与算法思想', ['复杂度、数组、链表与哈希表', '栈、队列、树与图基础', '排序、二分、递归与动态规划']],
    ['设计模式', '创建型、结构型与行为型模式', ['设计原则与创建型模式', '结构型模式与代码重构', '行为型模式与实际项目应用']],
    ['计算机基础', '操作系统、网络与组成原理', ['计算机组成、进程与内存管理', 'TCP/IP、HTTP 与网络排查', '编码、编译、数据表示与安全基础']],
    ['职业成长', '简历、沟通、开源与长期成长', ['开发者简历与作品集建设', '代码沟通、团队协作与技术表达', '参与开源、持续学习与职业规划']],
  ]},
]

const repoRoot = path.resolve(import.meta.dirname, '..')
const sqlPath = path.join(repoRoot, 'codenow-backend', 'seed-learning-catalog.sql')
const docsRoot = process.argv[2] || 'D:/RuyiTypora/项目/ai/CodeNow/文档'

const sqlEscape = (value) => String(value).replaceAll('\\', '\\\\').replaceAll("'", "''")
const safeName = (value) => value.replace(/[<>:"/\\|?*]/g, '-').replace(/[. ]+$/g, '').slice(0, 100)
const quoteYaml = (value) => JSON.stringify(value)

const allTags = new Set()
for (const root of catalog) {
  root.tags.forEach((tag) => allTags.add(tag))
  for (const child of root.children) {
    allTags.add(child[0].replace(' 教程', ''))
  }
}

const sql = [
  '-- CodeNow 学习目录初始化（会备份并物理删除现有文章及其关联数据）',
  '-- 由 scripts/generate-learning-catalog.mjs 生成，请先执行 migration-category-hierarchy.sql。',
  'SET NAMES utf8mb4;',
  'SET FOREIGN_KEY_CHECKS = 0;',
  'CREATE TABLE IF NOT EXISTS backup_blog_article_20260717 LIKE blog_article;',
  'INSERT INTO backup_blog_article_20260717 SELECT * FROM blog_article WHERE NOT EXISTS (SELECT 1 FROM backup_blog_article_20260717);',
  'CREATE TABLE IF NOT EXISTS backup_blog_article_tag_20260717 LIKE blog_article_tag;',
  'INSERT INTO backup_blog_article_tag_20260717 SELECT * FROM blog_article_tag WHERE NOT EXISTS (SELECT 1 FROM backup_blog_article_tag_20260717);',
  'DELETE FROM article_favorite WHERE article_id IS NOT NULL;',
  'DELETE FROM user_notification WHERE article_id IS NOT NULL;',
  'DELETE FROM blog_comment WHERE article_id IS NOT NULL;',
  'DELETE FROM blog_article_tag;',
  'DELETE FROM blog_article;',
  'ALTER TABLE blog_article AUTO_INCREMENT = 1;',
  'ALTER TABLE blog_article_tag AUTO_INCREMENT = 1;',
  'SET FOREIGN_KEY_CHECKS = 1;',
  '',
]

catalog.forEach((root, rootIndex) => {
  sql.push(`UPDATE blog_category SET description='${sqlEscape(root.description)}', parent_id=0, sort=${rootIndex + 1}, is_deleted=0 WHERE name='${sqlEscape(root.name)}';`)
  root.children.forEach((child, childIndex) => {
    const [name, description] = child
    sql.push(`INSERT INTO blog_category (name, description, parent_id, sort, is_deleted)
SELECT '${sqlEscape(name)}', '${sqlEscape(description)}', p.id, ${childIndex + 1}, 0 FROM blog_category p
WHERE p.name='${sqlEscape(root.name)}' AND NOT EXISTS (SELECT 1 FROM blog_category c WHERE c.name='${sqlEscape(name)}');`)
    sql.push(`UPDATE blog_category c JOIN blog_category p ON p.name='${sqlEscape(root.name)}'
SET c.description='${sqlEscape(description)}', c.parent_id=p.id, c.sort=${childIndex + 1}, c.is_deleted=0 WHERE c.name='${sqlEscape(name)}';`)
  })
})

sql.push('')
for (const tag of allTags) {
  sql.push(`INSERT INTO blog_tag (name, is_deleted) VALUES ('${sqlEscape(tag)}', 0) ON DUPLICATE KEY UPDATE is_deleted=0;`)
}
sql.push('')

let articleCount = 0
for (const root of catalog) {
  const rootDir = path.join(docsRoot, safeName(root.name))
  fs.mkdirSync(rootDir, { recursive: true })
  for (const [childName, childDescription, topics] of root.children) {
    const childDir = path.join(rootDir, safeName(childName))
    fs.mkdirSync(childDir, { recursive: true })
    topics.forEach((topic, index) => {
      articleCount++
      const order = index + 1
      const title = `${String(order).padStart(2, '0')}.${topic}`
      const summary = `系统讲解${topic}，覆盖核心概念、实践方法与常见问题，是“${childName}”学习路线的第 ${order} 步。`
      const tags = [...new Set([...root.tags, childName.replace(' 教程', '')])]
      const content = `# ${title}\n\n> 正文内容待完善。\n\n${summary}`
      sql.push(`INSERT INTO blog_article (title, content, summary, category_id, author_id, status, is_top, sort, view_count, is_deleted)
SELECT '${sqlEscape(title)}', '${sqlEscape(content)}', '${sqlEscape(summary)}', c.id, 1, 1, 0, ${order}, 0, 0
FROM blog_category c WHERE c.name='${sqlEscape(childName)}';`)
      for (const tag of tags) {
        sql.push(`INSERT INTO blog_article_tag (article_id, tag_id, is_deleted)
SELECT a.id, t.id, 0 FROM blog_article a JOIN blog_category c ON c.id=a.category_id
JOIN blog_tag t ON t.name='${sqlEscape(tag)}' WHERE a.title='${sqlEscape(title)}' AND c.name='${sqlEscape(childName)}';`)
      }
      const articleDir = path.join(childDir, safeName(title))
      fs.mkdirSync(articleDir, { recursive: true })
      const markdown = [
        '---',
        `title: ${quoteYaml(title)}`,
        `category: ${quoteYaml(root.name)}`,
        `subcategory: ${quoteYaml(childName)}`,
        `summary: ${quoteYaml(summary)}`,
        `tags: [${tags.map(quoteYaml).join(', ')}]`,
        `sort: ${order}`,
        'status: published',
        '---',
        '',
        `# ${title}`,
        '',
        '> 正文内容待完善。',
        '',
      ].join('\n')
      fs.writeFileSync(path.join(articleDir, 'index.md'), markdown, 'utf8')
    })
  }
}

sql.push('', `-- 共生成 ${catalog.length} 个一级分类、${catalog.reduce((sum, root) => sum + root.children.length, 0)} 个二级分类、${articleCount} 篇文章。`)
fs.writeFileSync(sqlPath, `${sql.join('\n')}\n`, 'utf8')
console.log(JSON.stringify({ sqlPath, docsRoot, roots: catalog.length, subcategories: 50, articles: articleCount }))
