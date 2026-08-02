# 领域建模与技术方案设计

当需求分析完成之后，下一步就是将业务需求转化为技术实现。这个过程不是简单地"翻译"，而是需要深入理解业务领域，建立准确的领域模型，然后基于领域模型设计技术方案。本文将介绍领域驱动设计（DDD）的核心概念，以及如何将领域模型转化为可落地的技术方案。

## 领域驱动设计核心概念

### 实体（Entity）

实体是具有唯一标识的对象，它的生命周期跨越多个状态变化。在技术博客系统中，`Article`就是一个实体：

```java
@Entity
@Table(name = "articles")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String content;
    private String slug;
    private ArticleStatus status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // 业务方法
    public void publish() {
        if (this.status != ArticleStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的文章才能发布");
        }
        this.status = ArticleStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isOwnedBy(User user) {
        return this.author.getId().equals(user.getId());
    }
}
```

实体的关键特征：
- **唯一标识**：两个实体即使所有属性都相同，只要ID不同就是不同的实体
- **可变性**：实体的状态可以变化，但标识不变
- **行为**：实体应该包含与其相关的业务逻辑，而不是纯粹的数据容器

### 值对象（Value Object）

值对象没有唯一标识，它通过属性值来定义相等性。值对象通常是不可变的：

```java
@Embeddable
public class ArticleSlug {
    private final String value;
    
    public ArticleSlug(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slug不能为空");
        }
        if (!value.matches("^[a-z0-9-]+$")) {
            throw new IllegalArgumentException("Slug只能包含小写字母、数字和连字符");
        }
        this.value = value;
    }
    
    public static ArticleSlug fromTitle(String title) {
        String slug = title.toLowerCase()
            .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        return new ArticleSlug(slug);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticleSlug that = (ArticleSlug) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
```

值对象的优势：
- **简化设计**：不需要管理生命周期
- **线程安全**：不可变对象天然线程安全
- **表达力强**：用类型系统表达业务规则

### 聚合根（Aggregate Root）

聚合是一组相关对象的集合，聚合根是进入聚合的唯一入口。外部对象只能通过聚合根来访问聚合内部的对象。

```java
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String slug;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Category> children = new ArrayList<>();
    
    private Integer sortOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
    
    // 业务方法：通过聚合根操作子分类
    public Category addChild(String name, String slug) {
        if (this.children.size() >= 20) {
            throw new IllegalStateException("子分类数量不能超过20个");
        }
        Category child = new Category(name, slug, this.author);
        child.setParent(this);
        child.setSortOrder(this.children.size());
        this.children.add(child);
        return child;
    }
    
    public void removeChild(Category child) {
        if (!this.children.contains(child)) {
            throw new IllegalArgumentException("该分类不属于此父分类");
        }
        if (!child.getArticles().isEmpty()) {
            throw new IllegalStateException("不能删除包含文章的分类");
        }
        this.children.remove(child);
    }
    
    public void reorder(List<Long> categoryIds) {
        // 验证所有ID都是当前分类的子分类
        Set<Long> childIds = this.children.stream()
            .map(Category::getId)
            .collect(Collectors.toSet());
        if (!childIds.containsAll(new HashSet<>(categoryIds))) {
            throw new IllegalArgumentException("包含无效的分类ID");
        }
        
        // 重新排序
        for (int i = 0; i < categoryIds.size(); i++) {
            this.children.stream()
                .filter(c -> c.getId().equals(categoryIds.get(i)))
                .findFirst()
                .ifPresent(c -> c.setSortOrder(i));
        }
    }
}
```

聚合设计的原则：
- **小聚合优先**：聚合越大，一致性边界越大，并发冲突越多
- **通过ID引用其他聚合**：避免聚合之间的直接对象引用
- **聚合内强一致性**：聚合内的修改在一次事务中完成
- **聚合间最终一致性**：通过领域事件实现聚合之间的异步同步

### 领域服务（Domain Service）

当业务逻辑不属于任何一个实体时，使用领域服务：

```java
@Service
@Transactional
public class ArticleDomainService {
    
    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    
    public Article createArticle(CreateArticleCommand command, User author) {
        // 验证分类归属
        Category category = categoryRepository.findById(command.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));
        if (!category.isOwnedBy(author)) {
            throw new AccessDeniedException("无权使用该分类");
        }
        
        // 处理标签：已有的直接关联，不存在的创建
        Set<Tag> tags = processTags(command.getTags());
        
        // 创建文章
        Article article = Article.builder()
            .title(command.getTitle())
            .content(command.getContent())
            .slug(ArticleSlug.fromTitle(command.getTitle()))
            .category(category)
            .tags(tags)
            .author(author)
            .status(ArticleStatus.DRAFT)
            .build();
        
        return articleRepository.save(article);
    }
    
    private Set<Tag> processTags(Set<String> tagNames) {
        return tagNames.stream()
            .map(name -> tagRepository.findByName(name)
                .orElseGet(() -> tagRepository.save(new Tag(name))))
            .collect(Collectors.toSet());
    }
}
```

### 领域事件（Domain Event）

领域事件表示领域中发生的有意义的事情，用于实现聚合之间的解耦：

```java
// 事件定义
public record ArticlePublishedEvent(
    Long articleId,
    Long authorId,
    String title,
    LocalDateTime publishedAt
) {}

// 事件发布
@Entity
public class Article {
    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    public void publish() {
        this.status = ArticleStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        domainEvents.add(new ArticlePublishedEvent(
            this.id, this.author.getId(), this.title, this.publishedAt
        ));
    }
    
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}

// 事件处理
@Component
@Slf4j
public class ArticlePublishedEventHandler {
    
    private final NotificationService notificationService;
    private final SearchIndexService searchIndexService;
    
    @EventListener
    @Async
    public void handleArticlePublished(ArticlePublishedEvent event) {
        log.info("文章发布事件处理: articleId={}", event.articleId());
        
        // 更新搜索索引
        searchIndexService.indexArticle(event.articleId());
        
        // 通知关注者
        notificationService.notifyFollowers(event.authorId(), 
            "您关注的作者发布了新文章: " + event.title());
    }
}
```

## 限界上下文划分

限界上下文（Bounded Context）是领域的边界，在这个边界内，领域模型的术语和规则是一致的。

### 博客系统的限界上下文

```
┌─────────────────────────────────────────────────────────┐
│                    博客系统                              │
├─────────────────┬─────────────────┬─────────────────────┤
│   内容管理上下文  │   用户管理上下文  │    评论上下文        │
│                 │                 │                     │
│  - Article      │  - User         │  - Comment          │
│  - Category     │  - Role         │  - CommentThread    │
│  - Tag          │  - Permission   │                     │
│  - Content      │  - Profile      │                     │
│                 │                 │                     │
│  文章的发布、     │  用户注册、       │  评论的增删改查       │
│  编辑、删除       │  登录、权限       │  审核、通知          │
├─────────────────┼─────────────────┼─────────────────────┤
│   统计分析上下文  │   消息通知上下文  │   系统管理上下文      │
│                 │                 │                     │
│  - PageView     │  - Notification │  - Config           │
│  - VisitLog     │  - Message      │  - Dictionary       │
│  - Statistics   │  - Template     │  - SystemLog        │
│                 │                 │                     │
│  流量统计、       │  站内信、邮件     │  系统配置、           │
│  用户行为分析     │  短信通知        │  字典管理            │
└─────────────────┴─────────────────┴─────────────────────┘
```

### 上下文映射

不同限界上下文之间的关系：

```java
// 上下文之间的通信通过防腐层（Anti-Corruption Layer）
@Service
public class CommentAclService {
    
    private final UserQueryGateway userQueryGateway;
    
    /**
     * 评论上下文需要用户信息，但不直接依赖用户领域模型
     * 通过防腐层转换
     */
    public CommentUserDTO getCommentUserInfo(Long userId) {
        UserInfo userInfo = userQueryGateway.getUserBasicInfo(userId);
        return new CommentUserDTO(
            userInfo.getId(),
            userInfo.getNickname(),
            userInfo.getAvatar()
        );
    }
}
```

## 技术方案设计

### 技术方案模板

一份完整的技术方案应该包含：

```markdown
# 技术方案：文章全文搜索功能

## 1. 背景与目标

### 1.1 业务背景
当前文章搜索基于MySQL的LIKE查询，存在以下问题：
- 搜索速度慢：10万篇文章时，搜索响应超过2秒
- 搜索质量差：不支持分词，只能精确匹配
- 无法按相关性排序

### 1.2 技术目标
- 搜索响应时间P99 < 200ms
- 支持中文分词和模糊匹配
- 支持按相关性和时间排序
- 支持高亮显示匹配内容

## 2. 方案设计

### 2.1 技术选型

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| MySQL全文索引 | 无额外依赖 | 中文分词效果差，性能一般 | 不采用 |
| Elasticsearch | 功能强大，生态成熟 | 运维复杂，资源消耗大 | 推荐 |
| MeiliSearch | 轻量级，易部署 | 社区较小，功能相对有限 | 备选 |

### 2.2 架构设计

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   应用层     │────▶│   搜索服务   │────▶│Elasticsearch│
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                    ┌──────┴──────┐
                    │   MQ        │
                    └──────┬──────┘
                           │
                    ┌──────┴──────┐
                    │   索引服务   │
                    └─────────────┘
```

### 2.3 索引设计

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "title": { 
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "authorId": { "type": "long" },
      "categoryId": { "type": "long" },
      "tags": { "type": "keyword" },
      "status": { "type": "keyword" },
      "publishedAt": { "type": "date" },
      "suggest": {
        "type": "completion",
        "analyzer": "simple"
      }
    }
  }
}
```

### 2.4 核心代码

```java
@Service
@Slf4j
public class ArticleSearchService {
    
    private final ElasticsearchClient esClient;
    
    public SearchResult<ArticleSearchDTO> search(SearchQuery query) {
        try {
            SearchResponse<ArticleSearchDTO> response = esClient.search(s -> s
                .index("articles")
                .query(q -> q
                    .multiMatch(m -> m
                        .fields("title^2", "content")
                        .query(query.getKeyword())
                        .type(TextQueryType.BestFields)
                        .fuzziness("AUTO")
                    ))
                .highlight(h -> h
                    .fields("title", f -> f.preTags("<em>").postTags("</em>"))
                    .fields("content", f -> f.preTags("<em>").postTags("</em>"))
                )
                .from(query.getPage() * query.getSize())
                .size(query.getSize()),
                ArticleSearchDTO.class
            );
            
            return SearchResult.from(response);
        } catch (IOException e) {
            log.error("搜索异常", e);
            throw new SearchException("搜索服务暂时不可用", e);
        }
    }
}
```

## 3. 风险评估

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| ES集群不稳定 | 搜索不可用 | 低 | 降级到MySQL查询 |
| 索引同步延迟 | 搜索结果不一致 | 中 | 最终一致性，延迟<5s |
| 中文分词不准 | 搜索体验差 | 中 | 自定义词典，持续优化 |

## 4. 排期

| 阶段 | 任务 | 工时 | 负责人 |
|------|------|------|--------|
| P1 | ES集群搭建 | 2天 | 运维 |
| P1 | 索引设计与映射 | 1天 | 后端 |
| P2 | 索引同步服务 | 2天 | 后端 |
| P2 | 搜索API开发 | 2天 | 后端 |
| P3 | 前端搜索页面 | 2天 | 前端 |
| P3 | 测试与调优 | 2天 | 测试 |
```

## 架构决策记录（ADR）

架构决策记录用于记录重要的技术决策及其上下文：

```markdown
# ADR-001: 选择Elasticsearch作为搜索引擎

## 状态
已采纳

## 上下文
博客系统需要全文搜索功能，当前MySQL LIKE查询性能不足。

## 决策
采用Elasticsearch作为搜索引擎，版本8.x。

## 理由
1. 中文分词支持成熟（IK分词器）
2. 社区活跃，文档完善
3. 支持分布式部署，可扩展性好
4. 与Spring Boot集成方便

## 后果
- 需要额外的运维成本
- 需要处理数据同步的一致性问题
- 需要学习ES的运维和调优

## 替代方案
- MeiliSearch：更轻量但生态较小
- Algolia：托管服务但成本较高
```

## C4模型

C4模型从四个层次描述系统架构：

### Level 1：系统上下文图

```
┌──────────────────────────────────────────────────────────┐
│                        博客系统                           │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │   博客前台    │    │   管理后台    │    │   API服务    │  │
│  └─────────────┘    └─────────────┘    └─────────────┘  │
│                                                          │
└──────────────────────────────────────────────────────────┘
        ▲                  ▲                  ▲
        │                  │                  │
        ▼                  ▼                  ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   读者用户    │    │   管理员     │    │   第三方应用  │
└─────────────┘    └─────────────┘    └─────────────┘
```

### Level 2：容器图

```
┌─────────────────────────────────────────────────────────────┐
│                        博客系统                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Vue.js SPA  │  │  Vue.js SPA  │  │  Nginx       │      │
│  │  (前台)       │  │  (后台)       │  │  (反向代理)   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                  │               │
│         └─────────────────┴──────────────────┘               │
│                           │                                  │
│                           ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Spring Boot Application                   │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐           │  │
│  │  │ Web层    │  │ Service层│  │ Repository层│          │  │
│  │  └──────────┘  └──────────┘  └──────────┘           │  │
│  └──────────────────────────────────────────────────────┘  │
│         │                 │                  │               │
│         ▼                 ▼                  ▼               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  MySQL 8.0   │  │  Redis 7.0   │  │ Elasticsearch│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Level 3：组件图

```
┌─────────────────────────────────────────────────────────┐
│              Spring Boot Application                     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │                   Web层                          │   │
│  │  ┌───────────┐  ┌───────────┐  ┌───────────┐   │   │
│  │  │ArticleCtrl│  │ AuthCtrl  │  │CommentCtrl │   │   │
│  │  └───────────┘  └───────────┘  └───────────┘   │   │
│  └─────────────────────────────────────────────────┘   │
│                         │                               │
│  ┌─────────────────────────────────────────────────┐   │
│  │                 Service层                        │   │
│  │  ┌───────────┐  ┌───────────┐  ┌───────────┐   │   │
│  │  │ArticleSvc │  │ AuthSvc   │  │CommentSvc  │   │   │
│  │  └───────────┘  └───────────┘  └───────────┘   │   │
│  └─────────────────────────────────────────────────┘   │
│                         │                               │
│  ┌─────────────────────────────────────────────────┐   │
│  │                Repository层                      │   │
│  │  ┌───────────┐  ┌───────────┐  ┌───────────┐   │   │
│  │  │ArticleRepo│  │ UserRepo  │  │CommentRepo │   │   │
│  │  └───────────┘  └───────────┘  └───────────┘   │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

领域建模和技术方案设计是连接业务需求和技术实现的桥梁。好的领域模型能让代码更贴近业务语言，让业务人员和开发人员能够用同一套术语沟通。技术方案则确保了实现路径的可行性和可预期性。两者结合，才能让项目在正确的轨道上前进。
