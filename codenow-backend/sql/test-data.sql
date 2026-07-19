-- ============================================
-- 码上记（CodeNow）测试数据
-- 执行顺序：分类 → 标签 → 文章 → 文章-标签关联
-- ============================================

USE `codenow`;

-- -------------------------------------------
-- 1. 分类（7 个）
-- -------------------------------------------
INSERT INTO `blog_category` (`name`, `description`, `sort`) VALUES
('Java 基础',       'Java 语法、集合、IO、多线程等核心知识',         1),
('Spring 全家桶',   'Spring Boot、Spring Cloud、Spring Security 等', 2),
('前端开发',        'Vue、React、HTML/CSS/JavaScript 等前端技术',    3),
('数据库',          'MySQL、Redis、MongoDB 等数据库技术',            4),
('开发工具',        'Git、Docker、IDEA、VSCode 等提效工具',          5),
('项目实战',        '完整的项目开发记录和踩坑总结',                   6),
('学习随笔',        '学习心得、面试经验、读书笔记',                   7);

-- -------------------------------------------
-- 2. 标签（12 个）
-- -------------------------------------------
INSERT INTO `blog_tag` (`name`) VALUES
('Java'),
('Spring Boot'),
('MyBatis-Plus'),
('Vue 3'),
('MySQL'),
('Redis'),
('Docker'),
('Git'),
('JavaScript'),
('Element Plus'),
('Linux'),
('设计模式');

-- -------------------------------------------
-- 3. 文章（12 篇）
-- -------------------------------------------
INSERT INTO `blog_article` (`title`, `content`, `summary`, `category_id`, `author_id`, `status`) VALUES

-- Java 基础（分类 1）
('Java 集合框架入门：ArrayList 和 LinkedList 的区别',
'## 为什么需要集合？\n\n在 Java 开发中，我们经常需要存储一组对象。数组虽然能用，但长度固定、操作不便。Java 集合框架就是为了解决这个问题而设计的。\n\n## ArrayList\n\nArrayList 底层是**动态数组**，支持随机访问，查询快（O(1)），但插入/删除慢（O(n)），因为需要移动元素。\n\n```java\nList<String> list = new ArrayList<>();\nlist.add(\"hello\");\nlist.get(0);  // O(1)\n```\n\n## LinkedList\n\nLinkedList 底层是**双向链表**，插入/删除快（O(1)），但查询慢（O(n)），因为需要遍历。\n\n```java\nList<String> list = new LinkedList<>();\nlist.addFirst(\"hello\");\n```\n\n## 怎么选？\n\n- 频繁查询 → ArrayList\n- 频繁增删 → LinkedList\n- 不确定 → 先用 ArrayList，大多数场景够用',
'对比 ArrayList 和 LinkedList 的底层实现、性能差异和使用场景',
1, 1, 1),

('Java 多线程基础：创建线程的三种方式',
'## 为什么要学多线程？\n\n现代 CPU 都是多核的，单线程程序无法充分利用硬件资源。多线程可以让程序"同时"做多件事，提升效率。\n\n## 方式一：继承 Thread\n\n```java\nclass MyThread extends Thread {\n    @Override\n    public void run() {\n        System.out.println(\"子线程运行\");\n    }\n}\nnew MyThread().start();\n```\n\n## 方式二：实现 Runnable\n\n```java\nnew Thread(() -> System.out.println(\"子线程运行\")).start();\n```\n\n## 方式三：实现 Callable\n\n可以有返回值，配合 FutureTask 使用。\n\n```java\nCallable<Integer> task = () -> 42;\nFutureTask<Integer> future = new FutureTask<>(task);\nnew Thread(future).start();\nSystem.out.println(future.get()); // 42\n```\n\n## 推荐哪种？\n\n推荐实现 Runnable/Callable，因为 Java 单继承，接口更灵活。',
'介绍 Java 中创建线程的三种方式及其适用场景',
1, 1, 1),

-- Spring 全家桶（分类 2）
('Spring Boot 自动配置原理浅析',
'## 什么是自动配置？\n\nSpring Boot 的核心卖点之一就是"开箱即用"。引入 `spring-boot-starter-web` 依赖，Tomcat、Jackson、Spring MVC 就自动配好了。这是怎么做到的？\n\n## @SpringBootApplication\n\n这个注解包含 `@EnableAutoConfiguration`，它会读取 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件，加载所有自动配置类。\n\n## 条件装配\n\n自动配置类用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解，只有满足条件才会生效。比如：\n\n- 类路径有 DataSource.class → 配置数据源\n- 用户没自定义 DataSource Bean → 用默认配置\n\n## 总结\n\n自动配置 = 约定大于配置 + 条件装配，减少大量样板代码。',
'深入理解 Spring Boot 自动配置的实现原理',
2, 1, 1),

('Spring Boot 整合 MyBatis-Plus 实现 CRUD',
'## 为什么选 MyBatis-Plus？\n\n原生 MyBatis 写 CRUD 要手写大量 XML 和 SQL，MyBatis-Plus 在其基础上增强了：内置通用 Mapper、条件构造器、分页插件，几乎不用写 SQL。\n\n## 整合步骤\n\n### 1. 添加依赖\n\n```xml\n<dependency>\n    <groupId>com.baomidou</groupId>\n    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>\n    <version>3.5.7</version>\n</dependency>\n```\n\n### 2. 实体类\n\n```java\n@Data\n@TableName(\"blog_article\")\npublic class BlogArticle {\n    @TableId(type = IdType.AUTO)\n    private Long id;\n    private String title;\n    private String content;\n}\n```\n\n### 3. Mapper\n\n```java\n@Mapper\npublic interface ArticleMapper extends BaseMapper<BlogArticle> {}\n```\n\n### 4. 使用\n\n```java\n// 查询全部\narticleMapper.selectList(null);\n// 条件查询\narticleMapper.selectList(new LambdaQueryWrapper<BlogArticle>()\n    .eq(BlogArticle::getStatus, 1));\n// 分页\narticleMapper.selectPage(new Page<>(1, 10), null);\n```\n\nMyBatis-Plus 让 CRUD 开发效率提升 10 倍。',
'Spring Boot 整合 MyBatis-Plus 的完整步骤和常用操作',
2, 1, 1),

('用 Sa-Token 实现 Spring Boot 项目登录认证',
'## Sa-Token 是什么？\n\nSa-Token 是一个轻量级 Java 权限认证框架，比 Spring Security 简单 10 倍。支持登录认证、权限校验、踢人下线等功能。\n\n## 基本用法\n\n### 登录\n\n```java\nStpUtil.login(userId);  // 登录，生成 Token\n```\n\n### 校验登录\n\n```java\nStpUtil.checkLogin();  // 未登录则抛异常\nLong userId = StpUtil.getLoginIdAsLong();  // 获取当前用户 ID\n```\n\n### 拦截器配置\n\n```java\nregistry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))\n    .addPathPatterns(\"/api/**\")\n    .excludePathPatterns(\"/api/auth/login\");\n```\n\n## 为什么选 Sa-Token？\n\n- 轻量，核心代码只有几千行\n- API 简洁，学习成本低\n- 文档友好，中文文档写得很好',
'Sa-Token 框架的基本使用方法和配置技巧',
2, 1, 1),

-- 前端开发（分类 3）
('Vue 3 组合式 API 入门：ref 和 reactive 的区别',
'## 为什么要用组合式 API？\n\nVue 2 的选项式 API（data、methods、computed 分开写）在复杂组件中逻辑分散。Vue 3 的组合式 API 让相关逻辑聚合在一起，更易维护。\n\n## ref\n\n用于基本类型，通过 `.value` 访问：\n\n```js\nimport { ref } from \'vue\'\nconst count = ref(0)\ncount.value++  // 在 JS 中用 .value\n```\n\n模板中直接用 `{{ count }}`，不需要 `.value`。\n\n## reactive\n\n用于对象，直接访问属性：\n\n```js\nimport { reactive } from \'vue\'\nconst state = reactive({ name: \'张三\', age: 20 })\nstate.age++  // 直接访问\n```\n\n## 怎么选？\n\n- 基本类型 → ref\n- 对象 → reactive（或 ref 也行）\n- 不确定 → 统一用 ref，简单不容易出错',
'对比 Vue 3 中 ref 和 reactive 的用法和适用场景',
3, 1, 1),

('Element Plus 快速上手：常用组件实战',
'## Element Plus 简介\n\nElement Plus 是饿了么团队基于 Vue 3 开发的 UI 组件库，提供了表格、表单、弹窗、分页等常用组件，开箱即用。\n\n## 全局注册\n\n```js\nimport ElementPlus from \'element-plus\'\nimport \'element-plus/dist/index.css\'\napp.use(ElementPlus)\n```\n\n## 常用组件示例\n\n### 表格\n\n```vue\n<el-table :data=\"list\">\n  <el-table-column prop=\"name\" label=\"名称\" />\n  <el-table-column prop=\"date\" label=\"日期\" />\n</el-table>\n```\n\n### 表单\n\n```vue\n<el-form :model=\"form\" :rules=\"rules\">\n  <el-form-item label=\"标题\" prop=\"title\">\n    <el-input v-model=\"form.title\" />\n  </el-form-item>\n</el-form>\n```\n\n### 弹窗\n\n```vue\n<el-dialog v-model=\"visible\" title=\"提示\">\n  <p>内容</p>\n</el-dialog>\n```\n\nElement Plus 的文档很完善，遇到问题先查文档。',
'Element Plus 常用组件的使用方法和实战技巧',
3, 1, 1),

-- 数据库（分类 4）
('MySQL 索引优化：让你的查询快 10 倍',
'## 为什么需要索引？\n\n没有索引的表就像没有目录的字典，查一个字要翻遍全书。索引就是数据库的"目录"。\n\n## 索引类型\n\n- **主键索引**：自动创建，查询最快\n- **唯一索引**：字段值不能重复\n- **普通索引**：最常用的索引类型\n- **联合索引**：多个字段组合的索引\n\n## 最左前缀原则\n\n联合索引 `(a, b, c)` 可以被以下查询使用：\n\n```sql\nWHERE a = 1\nWHERE a = 1 AND b = 2\nWHERE a = 1 AND b = 2 AND c = 3\n```\n\n但 `WHERE b = 2` 无法使用这个索引。\n\n## EXPLAIN 分析\n\n```sql\nEXPLAIN SELECT * FROM blog_article WHERE title = \'test\';\n```\n\n看 `type` 列：ALL（全表扫描）→ index → range → ref → const（最优）。\n\n## 总结\n\n索引不是越多越好，会影响写入性能。只在高频查询字段上建索引。',
'MySQL 索引的原理、类型和优化技巧',
4, 1, 1),

('Redis 入门：在 Spring Boot 中使用 Redis 缓存',
'## Redis 是什么？\n\nRedis 是一个内存数据库，读写速度极快（10 万+ QPS），常用来做缓存、Session 存储、排行榜等。\n\n## Spring Boot 整合\n\n### 1. 添加依赖\n\n```xml\n<dependency>\n    <groupId>org.springframework.boot</groupId>\n    <artifactId>spring-boot-starter-data-redis</artifactId>\n</dependency>\n```\n\n### 2. 配置连接\n\n```yaml\nspring:\n  data:\n    redis:\n      host: localhost\n      port: 6379\n```\n\n### 3. 使用 RedisTemplate\n\n```java\n@Autowired\nprivate StringRedisTemplate redisTemplate;\n\n// 存\nredisTemplate.opsForValue().set(\"key\", \"value\", 60, TimeUnit.SECONDS);\n// 取\nString value = redisTemplate.opsForValue().get(\"key\");\n```\n\n## 适合什么场景？\n\n- 热点数据缓存（减轻数据库压力）\n- 验证码存储（设置过期时间）\n- 分布式 Session（配合 Sa-Token）',
'Spring Boot 整合 Redis 的步骤和常见用法',
4, 1, 1),

-- 开发工具（分类 5）
('Git 分支管理：团队协作必备的 Git Flow',
'## 为什么需要分支管理？\n\n多人协作时，如果所有人都在 main 分支上开发，代码冲突会很频繁。分支管理让每个人在独立分支上开发，完成后再合并。\n\n## Git Flow 模型\n\n- **main**：生产环境代码，始终保持可发布\n- **develop**：开发分支，集成各功能分支\n- **feature/xxx**：功能分支，从 develop 创建，完成后合并回 develop\n- **release/x.x**：发布分支，从 develop 创建，测试通过后合并到 main\n- **hotfix/xxx**：紧急修复分支，从 main 创建\n\n## 常用命令\n\n```bash\n# 创建功能分支\ngit checkout -b feature/login develop\n\n# 完成后合并到 develop\ngit checkout develop\ngit merge --no-ff feature/login\n\n# 删除功能分支\ngit branch -d feature/login\n```\n\n`--no-ff` 会保留合并历史，方便追溯。',
'Git Flow 分支管理模型和团队协作最佳实践',
5, 1, 1),

-- 项目实战（分类 6）
('从零搭建一个 Spring Boot + Vue 3 全栈项目',
'## 前言\n\n作为大三学生，我决定从零搭建一个完整的全栈项目——个人博客系统"码上记"，记录整个开发过程。\n\n## 技术选型\n\n- 后端：Spring Boot 3.4 + MyBatis-Plus + Sa-Token\n- 前端：Vue 3 + Element Plus + Axios\n- 数据库：MySQL 8.0\n\n## 开发步骤\n\n1. **环境搭建**：JDK 21、Node.js 22、MySQL 8.0\n2. **后端开发**：建表 → 实体类 → Mapper → Service → Controller\n3. **前端开发**：路由 → 布局 → 登录页 → 各管理页面\n4. **前后端联调**：解决跨域、Token 传递、数据格式等问题\n\n## 踩坑记录\n\n- Spring Boot 4.0 与 MyBatis-Plus 不兼容，降级到 3.4\n- Sa-Token 的 `@Mapper` 注解不生效，需要加 `@MapperScan`\n- md-editor-v3 是命名导出，不是默认导出\n\n## 收获\n\n通过这个项目，我对全栈开发有了完整的认识，也积累了排查问题的经验。',
'记录从零搭建 Spring Boot + Vue 3 全栈博客系统的完整过程',
6, 1, 1),

-- 学习随笔（分类 7）
('大三学生的 Java 学习路线：从入门到能做项目',
'## 我的学习经历\n\n大一学了 C 语言，大二学了 Java 语法，大三开始做项目。回顾这段经历，总结一下学习路线。\n\n## 第一阶段：Java 基础\n\n- 语法：变量、条件、循环、数组\n- 面向对象：类、继承、多态、接口\n- 集合框架：List、Map、Set\n- IO 和多线程基础\n\n## 第二阶段：Web 开发\n\n- HTML/CSS/JavaScript 基础\n- Servlet 和 JSP（了解原理即可）\n- Spring Boot 快速入门\n- MyBatis / MyBatis-Plus\n\n## 第三阶段：前端 + 数据库\n\n- Vue 3 基础\n- Element Plus 组件库\n- MySQL 基础和优化\n- Redis 基础\n\n## 第四阶段：项目实战\n\n- 做一个完整的全栈项目\n- 学会用 Git 管理代码\n- 学会用 Docker 部署\n\n## 建议\n\n- 不要只看视频，要动手写代码\n- 遇到问题先自己查，再问别人\n- 做项目比刷题更有成就感',
'分享大三学生的 Java 学习路线和经验心得',
7, 1, 1);

-- -------------------------------------------
-- 4. 文章-标签关联（每篇文章 1~3 个标签）
-- -------------------------------------------
INSERT INTO `blog_article_tag` (`article_id`, `tag_id`) VALUES
-- 文章 1：Java 集合框架 → Java、设计模式
(1, 1),
(1, 12),
-- 文章 2：Java 多线程 → Java
(2, 1),
-- 文章 3：Spring Boot 自动配置 → Spring Boot、Java
(3, 2),
(3, 1),
-- 文章 4：MyBatis-Plus CRUD → Spring Boot、MyBatis-Plus、MySQL
(4, 2),
(4, 3),
(4, 5),
-- 文章 5：Sa-Token → Spring Boot、Java
(5, 2),
(5, 1),
-- 文章 6：Vue 3 ref/reactive → Vue 3、JavaScript
(6, 4),
(6, 9),
-- 文章 7：Element Plus → Vue 3、Element Plus
(7, 4),
(7, 10),
-- 文章 8：MySQL 索引 → MySQL
(8, 5),
-- 文章 9：Redis → Redis、Spring Boot
(9, 6),
(9, 2),
-- 文章 10：Git 分支 → Git
(10, 8),
-- 文章 11：全栈项目 → Spring Boot、Vue 3、Docker
(11, 2),
(11, 4),
(11, 7),
-- 文章 12：学习路线 → Java、设计模式
(12, 1),
(12, 12);
