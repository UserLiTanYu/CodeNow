# RESTful API 设计与版本控制

API是前后端之间的契约，也是系统与外部世界交互的窗口。一个设计良好的API应该像一份清晰的合同——让调用方一目了然地知道能做什么、怎么做、会得到什么结果。本文将深入探讨RESTful API的设计规范、版本控制策略以及文档生成实践。

## 资源命名规范

### URL设计原则

RESTful API的核心是资源，URL应该表示资源的层级关系：

```
# 正确的资源命名
GET    /api/articles              # 获取文章列表
GET    /api/articles/123          # 获取单篇文章
POST   /api/articles              # 创建文章
PUT    /api/articles/123          # 更新文章
DELETE /api/articles/123          # 删除文章

# 子资源
GET    /api/articles/123/comments  # 获取文章的评论
POST   /api/articles/123/comments  # 为文章添加评论

# 错误的命名（不是RESTful）
GET    /api/getArticles            # 动词命名，不符合REST
POST   /api/article/create         # 动词命名
GET    /api/article/list           # 动词命名
```

### 命名规则

```yaml
# URL命名规范
规则:
  - 使用名词复数: /articles 而不是 /article
  - 使用小写字母: /article-categories 而不是 /ArticleCategories
  - 使用连字符分隔: /article-tags 而不是 /article_tags 或 /articleTags
  - 避免嵌套过深: 最多3层，如 /authors/1/articles/2/comments
  - 使用查询参数过滤: /articles?status=published&categoryId=1
```

### 特殊操作的处理

有些操作无法简单映射为CRUD，可以使用以下方式：

```java
// 方式1：将操作视为资源的状态变更
PUT /api/articles/123/status
{
  "status": "published"
}

// 方式2：使用子资源表示动作
POST /api/articles/123/publish

// 方式3：使用自定义动作端点（RPC风格，慎用）
POST /api/articles/123/actions/publish
```

推荐使用方式1，因为它符合REST的状态转移思想。但如果操作不涉及资源状态变化（如发送通知），可以使用方式2或3。

## HTTP方法语义

### 方法对照表

| HTTP方法 | 语义 | 幂等性 | 安全性 | 典型用途 |
|----------|------|--------|--------|----------|
| GET | 获取资源 | 是 | 是 | 查询列表、详情 |
| POST | 创建资源 | 否 | 否 | 新增数据 |
| PUT | 全量更新 | 是 | 否 | 替换整个资源 |
| PATCH | 部分更新 | 是* | 否 | 更新部分字段 |
| DELETE | 删除资源 | 是 | 否 | 删除数据 |
| HEAD | 获取元信息 | 是 | 是 | 检查资源是否存在 |
| OPTIONS | 获取支持的方法 | 是 | 是 | CORS预检请求 |

### 幂等性的实际意义

幂等性意味着同一个请求执行多次，效果与执行一次相同。这对网络重试非常重要：

```java
// PUT是幂等的 - 多次调用结果相同
@PutMapping("/articles/{id}")
public ArticleDTO updateArticle(@PathVariable Long id, @RequestBody ArticleDTO dto) {
    return articleService.update(id, dto);
}

// POST不是幂等的 - 多次调用会创建多条记录
@PostMapping("/articles")
public ArticleDTO createArticle(@RequestBody ArticleDTO dto) {
    return articleService.create(dto);
}

// 让POST变为幂等 - 使用幂等键
@PostMapping("/articles")
public ArticleDTO createArticle(
    @RequestBody ArticleDTO dto,
    @RequestHeader("Idempotency-Key") String idempotencyKey
) {
    return articleService.create(dto, idempotencyKey);
}
```

## 状态码选择

### 常用状态码

```yaml
2xx 成功:
  200 OK: 请求成功，返回数据
  201 Created: 资源创建成功
  204 No Content: 删除成功，无返回内容

3xx 重定向:
  301 Moved Permanently: 资源永久迁移
  304 Not Modified: 资源未修改，使用缓存

4xx 客户端错误:
  400 Bad Request: 请求参数错误
  401 Unauthorized: 未认证
  403 Forbidden: 无权限
  404 Not Found: 资源不存在
  409 Conflict: 资源冲突（如重复创建）
  422 Unprocessable Entity: 参数格式正确但业务校验失败
  429 Too Many Requests: 请求频率超限

5xx 服务端错误:
  500 Internal Server Error: 服务器内部错误
  502 Bad Gateway: 网关错误
  503 Service Unavailable: 服务不可用
```

### 统一响应格式

```java
// 成功响应
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 123,
    "title": "Spring Boot实战",
    "content": "..."
  }
}

// 列表响应（带分页）
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [...],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}

// 错误响应
{
  "code": 400,
  "message": "参数校验失败",
  "errors": [
    {
      "field": "title",
      "message": "标题不能为空"
    },
    {
      "field": "content",
      "message": "内容长度不能少于100个字符"
    }
  ]
}

// 全局异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException e) {
        return ApiResponse.error(404, e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
            .collect(Collectors.toList());
        return ApiResponse.error(400, "参数校验失败", errors);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException e) {
        return ApiResponse.error(403, "无权限访问");
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

## HATEOAS

HATEOAS（Hypermedia As The Engine Of Application State）是REST的最高成熟度，它让响应中包含相关操作的链接：

```java
// 使用Spring HATEOAS
@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    
    @GetMapping("/{id}")
    public EntityModel<ArticleDTO> getArticle(@PathVariable Long id) {
        ArticleDTO article = articleService.findById(id);
        
        EntityModel<ArticleDTO> model = EntityModel.of(article);
        
        // 添加自链接
        model.add(linkTo(methodOn(ArticleController.class)
            .getArticle(id)).withSelfRel());
        
        // 添加相关链接
        model.add(linkTo(methodOn(ArticleController.class)
            .getComments(id)).withRel("comments"));
        
        // 根据状态添加操作链接
        if (article.getStatus() == ArticleStatus.DRAFT) {
            model.add(linkTo(methodOn(ArticleController.class)
                .publishArticle(id)).withRel("publish"));
        }
        
        return model;
    }
}

// 响应示例
{
  "id": 123,
  "title": "Spring Boot实战",
  "status": "DRAFT",
  "_links": {
    "self": { "href": "/api/articles/123" },
    "comments": { "href": "/api/articles/123/comments" },
    "publish": { "href": "/api/articles/123/publish" }
  }
}
```

## 分页、过滤与排序

### 分页参数设计

```java
// 请求
GET /api/articles?page=1&size=20

// 响应
{
  "items": [...],
  "pagination": {
    "page": 1,
    "size": 20,
    "total": 156,
    "pages": 8,
    "hasNext": true,
    "hasPrev": false
  }
}

// 分页参数封装
public class PageQuery {
    @Min(1)
    private int page = 1;
    
    @Min(1)
    @Max(100)
    private int size = 20;
    
    public Pageable toPageable() {
        return PageRequest.of(page - 1, size);
    }
}

// 游标分页（适合大数据量）
GET /api/articles?cursor=eyJpZCI6MTIzfQ&limit=20
{
  "items": [...],
  "next_cursor": "eyJpZCI6MTQzfQ",
  "has_more": true
}
```

### 过滤参数

```java
// 简单过滤
GET /api/articles?status=published&categoryId=1

// 复杂过滤（使用查询参数）
GET /api/articles?filter=status:published,categoryId:1,authorId:5

// 日期范围
GET /api/articles?createdAfter=2024-01-01&createdBefore=2024-12-31

// 实现
@GetMapping("/articles")
public PageResult<ArticleDTO> listArticles(ArticleQuery query) {
    Specification<Article> spec = (root, cb, criteriaQuery) -> {
        List<Predicate> predicates = new ArrayList<>();
        
        if (query.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), query.getStatus()));
        }
        if (query.getCategoryId() != null) {
            predicates.add(cb.equal(root.get("category").get("id"), query.getCategoryId()));
        }
        if (query.getKeyword() != null) {
            predicates.add(cb.or(
                cb.like(root.get("title"), "%" + query.getKeyword() + "%"),
                cb.like(root.get("content"), "%" + query.getKeyword() + "%")
            ));
        }
        
        return cb.and(predicates.toArray(new Predicate[0]));
    };
    
    return articleRepository.findAll(spec, query.toPageable());
}
```

### 排序参数

```java
// 请求
GET /api/articles?sort=createdAt,desc
GET /api/articles?sort=views,desc&sort=createdAt,desc

// 实现
public class SortParam {
    private String field;
    private Sort.Direction direction = Sort.Direction.DESC;
    
    public Sort toSort() {
        // 验证字段名，防止SQL注入
        Set<String> allowedFields = Set.of("createdAt", "updatedAt", "views", "title");
        if (!allowedFields.contains(field)) {
            throw new IllegalArgumentException("不支持的排序字段: " + field);
        }
        return Sort.by(direction, field);
    }
}
```

## API版本控制

### 版本策略对比

| 策略 | 示例 | 优点 | 缺点 |
|------|------|------|------|
| URL路径 | /api/v1/articles | 直观，易于路由 | URL变更，缓存失效 |
| 请求头 | Accept: application/vnd.api.v1+json | URL保持不变 | 不直观，调试麻烦 |
| 查询参数 | /api/articles?version=1 | 简单 | 不够RESTful |
| 媒体类型 | Accept: application/vnd.codenow.article.v1+json | 灵活 | 复杂度高 |

推荐使用URL路径方式，简单直观：

```java
// 版本化控制器
@RestController
@RequestMapping("/api/v1/articles")
public class ArticleControllerV1 {
    @GetMapping("/{id}")
    public ArticleV1DTO getArticle(@PathVariable Long id) {
        // V1版本返回格式
    }
}

@RestController
@RequestMapping("/api/v2/articles")
public class ArticleControllerV2 {
    @GetMapping("/{id}")
    public ArticleV2DTO getArticle(@PathVariable Long id) {
        // V2版本返回格式，增加了新字段
    }
}

// 或者使用请求头版本控制
@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    
    @GetMapping(value = "/{id}", headers = "X-API-Version=1")
    public ArticleV1DTO getArticleV1(@PathVariable Long id) {
        return articleService.findByIdV1(id);
    }
    
    @GetMapping(value = "/{id}", headers = "X-API-Version=2")
    public ArticleV2DTO getArticleV2(@PathVariable Long id) {
        return articleService.findByIdV2(id);
    }
}
```

### 版本兼容性

```java
// 向后兼容的变更
// 1. 添加新字段（兼容）
public class ArticleV2DTO extends ArticleV1DTO {
    private String summary;  // 新增字段
    private Integer wordCount;  // 新增字段
}

// 2. 废弃字段（添加@Deprecated）
public class ArticleV1DTO {
    @Deprecated  // 将在V3移除
    private String oldField;
}

// 3. 不兼容的变更需要新版本
// - 字段类型改变
// - 字段语义改变
// - 删除字段
```

## OpenAPI/Swagger文档生成

### 配置Swagger

```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "码上记博客系统API",
        version = "1.0.0",
        description = "技术博客系统RESTful API文档",
        contact = @Contact(name = "开发团队", email = "dev@codenow.com")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "本地开发"),
        @Server(url = "https://api.codenow.com", description = "生产环境")
    }
)
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("bearer-jwt",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
```

### API文档注解

```java
@Tag(name = "文章管理", description = "文章的增删改查操作")
@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {
    
    @Operation(summary = "获取文章列表", description = "分页获取文章列表，支持过滤和排序")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(schema = @Schema(implementation = PageResult.class))),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping
    public PageResult<ArticleDTO> listArticles(
        @ParameterObject ArticleQuery query
    ) {
        return articleService.findAll(query);
    }
    
    @Operation(summary = "创建文章", description = "创建新文章，需要作者权限")
    @ApiResponse(responseCode = "201", description = "创建成功")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleDTO createArticle(
        @Valid @RequestBody CreateArticleRequest request
    ) {
        return articleService.create(request);
    }
}

// DTO文档注解
@Schema(description = "文章数据")
public class ArticleDTO {
    @Schema(description = "文章ID", example = "123")
    private Long id;
    
    @Schema(description = "文章标题", example = "Spring Boot实战", required = true)
    @NotBlank
    private String title;
    
    @Schema(description = "文章内容", minLength = 100)
    @Size(min = 100)
    private String content;
    
    @Schema(description = "文章状态", allowableValues = {"DRAFT", "PUBLISHED", "ARCHIVED"})
    private ArticleStatus status;
    
    @Schema(description = "创建时间", format = "date-time")
    private LocalDateTime createdAt;
}
```

## API网关路由

### Nginx反向代理配置

```nginx
upstream backend {
    server localhost:8080;
}

upstream frontend {
    server localhost:5173;
}

server {
    listen 80;
    server_name codenow.com;
    
    # API路由
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # API版本路由
        location /api/v1/ {
            proxy_pass http://backend/api/v1/;
        }
        
        location /api/v2/ {
            proxy_pass http://backend/api/v2/;
        }
    }
    
    # 静态资源
    location / {
        proxy_pass http://frontend;
    }
    
    # Knife4j文档
    location /doc.html {
        proxy_pass http://backend;
    }
}
```

### Spring Cloud Gateway配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: api-v1
          uri: http://localhost:8080
          predicates:
            - Path=/api/v1/**
          filters:
            - StripPrefix=0
            
        - id: api-v2
          uri: http://localhost:8080
          predicates:
            - Path=/api/v2/**
          filters:
            - StripPrefix=0
            
      default-filters:
        - name: Retry
          args:
            retries: 3
            statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
            
        - name: CircuitBreaker
          args:
            name: apiCircuitBreaker
            fallbackUri: forward:/fallback
```

RESTful API设计是一项需要平衡理论与实践的工作。遵循REST原则能让API更加规范和易于理解，但也不必教条主义。关键是在团队内建立统一的设计规范，让API具有良好的一致性和可维护性。
