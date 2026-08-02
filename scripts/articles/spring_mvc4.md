# CORS、内容协商与 RESTful 设计

现代 Web 应用开发中，前后端分离架构已成为主流。在这种架构下，跨域资源共享（CORS）、内容协商机制以及 RESTful API 设计成为后端开发者必须掌握的核心技能。Spring MVC 对这些特性提供了全面的支持，本文将深入探讨其原理与实践。

## CORS（跨域资源共享）

### 同源策略

同源策略是浏览器的安全机制，限制从一个源（Origin）加载的文档或脚本与另一个源的资源进行交互。两个 URL 的源相同需要满足：

- 协议相同（http/https）
- 域名相同
- 端口相同

```
http://example.com:80/path1  →  同源
https://example.com:80/path2 →  协议不同，跨域
http://api.example.com:80    →  域名不同，跨域
http://example.com:8080      →  端口不同，跨域
```

### CORS 工作原理

CORS 通过在 HTTP 头中添加额外信息，允许服务器声明哪些源可以访问其资源。浏览器根据响应头决定是否允许跨域请求。

**简单请求**（Simple Requests）：
- GET、HEAD、POST 方法
- Content-Type 为 `text/plain`、`multipart/form-data`、`application/x-www-form-urlencoded`
- 无自定义请求头

**预检请求**（Preflight Requests）：
- 不满足简单请求条件的请求
- 浏览器先发送 OPTIONS 请求询问服务器是否允许
- 服务器确认后才发送实际请求

### @CrossOrigin 注解

`@CrossOrigin` 注解用于在 Controller 或方法级别配置 CORS：

```java
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")  // 类级别
public class UserController {

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    @CrossOrigin(origins = "http://localhost:3000", 
                 methods = {RequestMethod.POST, RequestMethod.PUT},
                 allowedHeaders = "Content-Type")  // 方法级别
    public ResponseEntity<User> createUser(@RequestBody UserDTO dto) {
        return ResponseEntity.ok(userService.create(dto));
    }
}
```

`@CrossOrigin` 属性说明：

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `origins` | 允许的源 | `*` |
| `allowedHeaders` | 允许的请求头 | `*` |
| `exposedHeaders` | 暴露的响应头 | 空 |
| `methods` | 允许的 HTTP 方法 | 请求方法 |
| `allowCredentials` | 是否允许凭证 | `false` |
| `maxAge` | 预检请求缓存时间（秒） | `1800` |

### 全局 CorsConfigurer

通过 `WebMvcConfigurer` 配置全局 CORS：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // 匹配路径
            .allowedOrigins("http://localhost:3000", "http://localhost:5173")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Authorization", "X-Total-Count")
            .allowCredentials(true)
            .maxAge(3600);  // 预检请求缓存1小时
    }
}
```

### Spring Security 中的 CORS 配置

如果使用 Spring Security，需要额外配置：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000", 
            "http://localhost:5173"
        ));
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 预检请求处理

预检请求（OPTIONS）的响应头：

```http
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

### Credentials 配置

当需要传递 Cookie 或 Authorization 头时，必须配置 `allowCredentials = true`：

```java
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = true)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginDTO dto,
                                       HttpServletResponse response) {
        String token = authService.login(dto);
        // 设置 Cookie
        ResponseCookie cookie = ResponseCookie.from("token", token)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(Duration.ofHours(24))
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok().build();
    }
}
```

前端 JavaScript 配置：

```javascript
fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    credentials: 'include',  // 必须设置为 include
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username: 'admin', password: '123456' })
});
```

## 内容协商

内容协商允许客户端和服务器协商响应的格式，基于 `Accept` 请求头。

### Accept 头

客户端通过 `Accept` 头告知服务器期望的响应格式：

```http
Accept: application/json
Accept: text/html
Accept: application/xml
Accept: application/json, application/xml;q=0.9, */*;q=0.8
```

质量因子 `q` 表示优先级（0-1，默认为 1）。

### MediaType

Spring 使用 `MediaType` 表示媒体类型：

```java
MediaType json = MediaType.APPLICATION_JSON;
MediaType xml = MediaType.APPLICATION_XML;
MediaType html = MediaType.TEXT_HTML;
MediaType all = MediaType.ALL;

// 自定义媒体类型
MediaType custom = MediaType.parseMediaType("application/vnd.myapp.v1+json");
```

### HttpMessageConverter

`HttpMessageConverter` 负责 HTTP 请求和响应的序列化/反序列化：

| 转换器 | 支持的媒体类型 | 说明 |
|--------|---------------|------|
| `MappingJackson2HttpMessageConverter` | `application/json` | Jackson JSON |
| `MappingJackson2XmlHttpMessageConverter` | `application/xml` | Jackson XML |
| `Jaxb2RootElementHttpMessageConverter` | `application/xml` | JAXB XML |
| `StringHttpMessageConverter` | `text/plain` | 纯文本 |
| `ByteArrayHttpMessageConverter` | `application/octet-stream` | 字节数组 |
| `ResourceHttpMessageConverter` | `*/*` | 资源文件 |

### 配置内容协商

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // 方式1：基于请求头（默认）
        configurer.favorParameter(false);

        // 方式2：基于查询参数
        configurer.favorParameter(true)
            .parameterName("format");

        // 默认媒体类型
        configurer.defaultContentType(MediaType.APPLICATION_JSON);

        // 媒体类型映射
        configurer.mediaType("json", MediaType.APPLICATION_JSON);
        configurer.mediaType("xml", MediaType.APPLICATION_XML);
        configurer.mediaType("html", MediaType.TEXT_HTML);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 添加 Jackson JSON 转换器
        MappingJackson2HttpMessageConverter jsonConverter = 
            new MappingJackson2HttpMessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jsonConverter.setObjectMapper(mapper);
        converters.add(jsonConverter);

        // 添加 Jackson XML 转换器
        converters.add(new MappingJackson2XmlHttpMessageConverter());
    }
}
```

### 内容协商的使用

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
        // 根据 Accept 头自动选择 JSON 或 XML 格式
    }
}
```

## Jackson 序列化配置

### 常用注解

```java
public class UserDTO {

    @JsonProperty("user_name")  // 自定义 JSON 属性名
    private String username;

    @JsonIgnore  // 忽略此字段
    private String password;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @JsonInclude(JsonInclude.Include.NON_NULL)  // null 值不序列化
    private String nickname;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)  // 空值不序列化
    private List<String> tags;

    @JsonView(Views.Public.class)  // 视图控制
    private String email;

    @JsonView(Views.Internal.class)
    private String internalId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)  // 只读字段
    private Long id;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)  // 只写字段
    private String confirmPassword;

    @JsonSerialize(using = MoneySerializer.class)  // 自定义序列化器
    private BigDecimal balance;

    @JsonDeserialize(using = MoneyDeserializer.class)  // 自定义反序列化器
    private BigDecimal amount;
}
```

### JSON 视图

使用 `@JsonView` 控制不同场景下的字段可见性：

```java
public class Views {
    public static class Public {}
    public static class Internal extends Public {}
}

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    @JsonView(Views.Public.class)
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/{id}/detail")
    @JsonView(Views.Internal.class)
    public ResponseEntity<User> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }
}
```

### 自定义 ObjectMapper

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 序列化配置
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        // 反序列化配置
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // 注册自定义模块
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new MoneySerializer());
        module.addDeserializer(BigDecimal.class, new MoneyDeserializer());
        mapper.registerModule(module);

        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }
}
```

## RESTful API 设计原则

### 资源命名

使用名词复数形式，避免动词：

| 资源 | 推荐 | 不推荐 |
|------|------|--------|
| 用户列表 | `/api/users` | `/api/getUsers` |
| 单个用户 | `/api/users/{id}` | `/api/user?id={id}` |
| 用户订单 | `/api/users/{id}/orders` | `/api/getUserOrders` |
| 搜索用户 | `/api/users?keyword=xxx` | `/api/searchUser` |

### HTTP 方法语义

| 方法 | 语义 | 幂等性 | 安全性 | 示例 |
|------|------|--------|--------|------|
| GET | 查询资源 | 是 | 是 | `GET /api/users/1` |
| POST | 创建资源 | 否 | 否 | `POST /api/users` |
| PUT | 全量更新 | 是 | 否 | `PUT /api/users/1` |
| PATCH | 部分更新 | 是 | 否 | `PATCH /api/users/1` |
| DELETE | 删除资源 | 是 | 否 | `DELETE /api/users/1` |
| HEAD | 获取元数据 | 是 | 是 | `HEAD /api/users/1` |
| OPTIONS | 获取支持的方法 | 是 | 是 | `OPTIONS /api/users` |

### 状态码规范

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 OK | 成功 | GET、PUT、PATCH 成功 |
| 201 Created | 已创建 | POST 成功创建资源 |
| 204 No Content | 无内容 | DELETE 成功 |
| 400 Bad Request | 请求错误 | 参数校验失败 |
| 401 Unauthorized | 未认证 | 未登录或 Token 无效 |
| 403 Forbidden | 无权限 | 已认证但无权限访问 |
| 404 Not Found | 未找到 | 资源不存在 |
| 405 Method Not Allowed | 方法不允许 | HTTP 方法不支持 |
| 409 Conflict | 冲突 | 资源状态冲突 |
| 422 Unprocessable Entity | 无法处理 | 业务逻辑校验失败 |
| 429 Too Many Requests | 请求过多 | 触发限流 |
| 500 Internal Server Error | 服务器错误 | 未知异常 |

### RESTful Controller 示例

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户列表（支持分页和筛选）
     */
    @GetMapping
    public ResponseEntity<Page<User>> listUsers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Page<User> users = userService.findAll(page, size, keyword, status);
        return ResponseEntity.ok()
            .header("X-Total-Count", String.valueOf(users.getTotalElements()))
            .body(users);
    }

    /**
     * 获取单个用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建用户
     */
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserDTO dto) {
        User user = userService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(user.getId())
            .toUri();
        return ResponseEntity.created(location).body(user);
    }

    /**
     * 更新用户（全量）
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO dto) {
        return userService.update(id, dto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 部分更新用户
     */
    @PatchMapping("/{id}")
    public ResponseEntity<User> patchUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        return userService.patch(id, updates)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 获取用户的订单
     */
    @GetMapping("/{id}/orders")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long id) {
        List<Order> orders = orderService.findByUserId(id);
        return ResponseEntity.ok(orders);
    }
}
```

## HATEOAS 简介

HATEOAS（Hypermedia As The Engine Of Application State）是 REST 的最高成熟度，响应中包含相关资源的链接。

### Spring HATEOAS

```java
// 添加依赖
// implementation 'org.springframework.boot:spring-boot-starter-hateoas'

// 资源表示
@Getter
public class UserResource extends RepresentationModel<UserResource> {
    private Long id;
    private String username;
    private String email;

    public UserResource(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();

        // 添加链接
        add(linkTo(methodOn(UserController.class).getUser(user.getId())).withSelfRel());
        add(linkTo(methodOn(UserController.class).getUserOrders(user.getId())).withRel("orders"));
    }
}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public ResponseEntity<UserResource> getUser(@PathVariable Long id) {
        User user = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(new UserResource(user));
    }
}
```

### HAL 格式响应

```json
{
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "_links": {
        "self": {
            "href": "http://localhost:8080/api/users/1"
        },
        "orders": {
            "href": "http://localhost:8080/api/users/1/orders"
        }
    }
}
```

## API 版本控制

### URL 路径版本控制

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {
    // V1 版本
}

@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 {
    // V2 版本，新增字段或改变响应结构
}
```

### 请求头版本控制

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping(value = "/{id}", headers = "X-API-VERSION=1")
    public ResponseEntity<UserV1> getUserV1(@PathVariable Long id) {
        // V1 版本
    }

    @GetMapping(value = "/{id}", headers = "X-API-VERSION=2")
    public ResponseEntity<UserV2> getUserV2(@PathVariable Long id) {
        // V2 版本
    }
}
```

### 媒体类型版本控制

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping(value = "/{id}", 
                produces = "application/vnd.myapp.v1+json")
    public ResponseEntity<UserV1> getUserV1(@PathVariable Long id) {
        // V1 版本
    }

    @GetMapping(value = "/{id}", 
                produces = "application/vnd.myapp.v2+json")
    public ResponseEntity<UserV2> getUserV2(@PathVariable Long id) {
        // V2 版本
    }
}
```

### 版本控制策略对比

| 策略 | 优点 | 缺点 |
|------|------|------|
| URL 路径 | 简单直观，易于路由 | URL 变化，影响 SEO |
| 请求头 | URL 保持不变 | 客户端需要设置请求头 |
| 媒体类型 | 最符合 REST 规范 | 实现复杂，不易调试 |

## 统一响应封装

### R&lt;T&gt; 模式

```java
public class R<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }

    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }

    // getters and setters
}
```

### 状态码与业务码分离

```java
public class R<T> {

    private int httpStatus;      // HTTP 状态码
    private int bizCode;         // 业务状态码
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setHttpStatus(200);
        r.setBizCode(0);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(BizException e) {
        R<T> r = new R<>();
        r.setHttpStatus(e.getHttpStatus());
        r.setBizCode(e.getBizCode());
        r.setMessage(e.getMessage());
        return r;
    }
}

// 业务异常定义
public class BizException extends RuntimeException {
    private final int httpStatus;
    private final int bizCode;

    public static final BizException USER_NOT_FOUND = 
        new BizException(404, 1001, "用户不存在");
    public static final BizException USERNAME_EXISTS = 
        new BizException(409, 1002, "用户名已存在");
}
```

### 统一响应处理

```java
@RestControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, 
                            Class converterType) {
        // 排除已经是 R 类型的响应
        return !returnType.getParameterType().equals(R.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                   MediaType selectedContentType,
                                   Class selectedConverterType,
                                   ServerHttpRequest request,
                                   ServerHttpResponse response) {
        if (body instanceof String) {
            // String 类型需要特殊处理
            return JSON.toJSONString(R.ok(body));
        }
        return R.ok(body);
    }
}
```

## Swagger/OpenAPI 文档生成

### springdoc-openapi 配置

```java
// 添加依赖
// implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("码上记 API")
                .version("1.0.0")
                .description("码上记技术博客 API 文档")
                .contact(new Contact()
                    .name("Admin")
                    .email("admin@codenow.com")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .components(new Components()
                .addSecuritySchemes("Bearer",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

### 注解使用

```java
@Tag(name = "用户管理", description = "用户 CRUD 操作")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Operation(summary = "获取用户列表", description = "分页查询用户")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping
    public ResponseEntity<Page<User>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(userService.findAll(page, size, keyword));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public ResponseEntity<User> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "用户信息", required = true)
            @Valid @RequestBody UserDTO dto) {
        return ResponseEntity.ok(userService.create(dto));
    }
}
```

### Schema 注解

```java
@Schema(description = "用户实体")
public class User {

    @Schema(description = "用户ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "用户名", example = "admin", required = true)
    private String username;

    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createTime;
}
```

### 访问 Swagger UI

启动应用后访问：
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

## 分页查询最佳实践

### Pageable 参数

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    public ResponseEntity<Page<User>> listUsers(Pageable pageable) {
        // Spring MVC 自动解析 ?page=0&size=10&sort=username,asc
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @GetMapping("/custom")
    public ResponseEntity<Page<User>> listUsersCustom(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sortObj = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        return ResponseEntity.ok(userService.findAll(pageable));
    }
}
```

### 自定义 Pageable 解析

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 自定义分页参数解析
        PageableHandlerMethodArgumentResolver resolver = 
            new PageableHandlerMethodArgumentResolver();
        resolver.setFallbackPageable(PageRequest.of(0, 20));  // 默认每页20条
        resolver.setMaxPageSize(100);  // 最大每页100条
        resolvers.add(resolver);
    }
}
```

### Page 响应封装

```java
// 使用 Spring Data 的 Page
@GetMapping("/v1")
public ResponseEntity<Page<User>> listUsersV1(Pageable pageable) {
    return ResponseEntity.ok(userService.findAll(pageable));
}

// 自定义分页响应
public class PageResult<T> {
    private List<T> content;
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public static <T> PageResult<T> from(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setContent(page.getContent());
        result.setPage(page.getNumber());
        result.setSize(page.getSize());
        result.setTotal(page.getTotalElements());
        result.setTotalPages(page.getTotalPages());
        return result;
    }
}

@GetMapping("/v2")
public ResponseEntity<PageResult<User>> listUsersV2(Pageable pageable) {
    Page<User> page = userService.findAll(pageable);
    return ResponseEntity.ok(PageResult.from(page));
}
```

### 头部返回分页信息

```java
@GetMapping("/v3")
public ResponseEntity<List<User>> listUsersV3(Pageable pageable) {
    Page<User> page = userService.findAll(pageable);
    return ResponseEntity.ok()
        .header("X-Total-Count", String.valueOf(page.getTotalElements()))
        .header("X-Total-Pages", String.valueOf(page.getTotalPages()))
        .header("X-Current-Page", String.valueOf(page.getNumber()))
        .header("X-Page-Size", String.valueOf(page.getSize()))
        .body(page.getContent());
}
```

### 分页查询性能优化

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 避免 N+1 查询：使用 JOIN FETCH
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.status = :status")
    Page<User> findAllWithRoles(@Param("status") String status, Pageable pageable);

    // 使用投影减少数据传输
    @Query("SELECT u.id as id, u.username as username, u.email as email FROM User u")
    Page<UserSummary> findAllSummary(Pageable pageable);

    // 游标分页（大数据量推荐）
    @Query("SELECT u FROM User u WHERE u.id > :lastId ORDER BY u.id ASC")
    List<User> findByIdAfter(@Param("lastId") Long lastId, Pageable pageable);
}

// 投影接口
public interface UserSummary {
    Long getId();
    String getUsername();
    String getEmail();
}
```

CORS、内容协商和 RESTful 设计是构建现代 Web API 的基础。Spring MVC 提供了全面的支持，通过合理配置和最佳实践，可以构建出高质量、易维护的 API 服务。结合 Swagger 文档生成和分页查询优化，能够显著提升开发效率和用户体验。