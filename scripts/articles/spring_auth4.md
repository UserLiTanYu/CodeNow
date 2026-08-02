# OAuth2 与第三方登录集成

"使用 GitHub 账号登录"——这个功能如今几乎成了开发者工具类产品的标配。背后的技术就是 OAuth2 协议。OAuth2 不仅解决了第三方登录问题，更是现代微服务架构中服务间授权的基础。本文将从协议原理讲起，逐步深入 Spring Security 的 OAuth2 集成实现。

## OAuth2 协议概述

OAuth2 是一个**授权框架**，允许第三方应用在用户授权的前提下，访问用户在某个服务提供商上的资源，而无需获取用户的账号密码。

核心角色：

| 角色 | 说明 | 示例 |
|------|------|------|
| Resource Owner | 资源所有者（用户） | GitHub 用户 |
| Client | 第三方应用 | 码上记博客 |
| Authorization Server | 授权服务器 | GitHub OAuth |
| Resource Server | 资源服务器 | GitHub API |

### 四种授权模式

| 模式 | 适用场景 | 安全性 |
|------|---------|--------|
| 授权码模式（Authorization Code） | 服务端 Web 应用 | 最高，推荐 |
| 隐式模式（Implicit） | 纯前端 SPA（已不推荐） | 较低，Token 暴露在 URL |
| 密码模式（Resource Owner Password） | 高度信任的第一方应用 | 需要用户交出密码 |
| 客户端凭证模式（Client Credentials） | 服务间通信（无用户参与） | 适用于 M2M |

实际项目中，**授权码模式**是使用最广泛的方案，也是第三方登录的标准流程。

## 授权码模式流程详解

授权码模式的核心思想是：第三方应用不直接获取用户凭证，而是通过一个**授权码**中转，再用授权码换取 Token。

**完整流程：**

```
用户 → 码上记 → GitHub 授权页面 → 用户授权 → GitHub 回调码上记（带 code）
→ 码上记用 code 换 Token → 码上记用 Token 获取用户信息 → 创建/登录本地账号
```

**分步说明：**

**第一步：构造授权 URL，重定向用户到授权服务器**

```
https://github.com/login/oauth/authorize
    ?client_id=YOUR_CLIENT_ID
    &redirect_uri=https://yoursite.com/callback
    &scope=user:email
    &state=random_string_for_csrf
```

**第二步：用户授权后，授权服务器回调 redirect_uri，携带授权码**

```
https://yoursite.com/callback?code=AUTH_CODE&state=random_string_for_csrf
```

**第三步：后端用授权码换取 Access Token**

```http
POST https://github.com/login/oauth/access_token
Content-Type: application/json

{
    "client_id": "YOUR_CLIENT_ID",
    "client_secret": "YOUR_CLIENT_SECRET",
    "code": "AUTH_CODE"
}
```

响应：

```json
{
    "access_token": "gho_xxxxxxxxxxxx",
    "token_type": "bearer",
    "scope": "user:email"
}
```

**第四步：使用 Access Token 调用资源服务器 API 获取用户信息**

```http
GET https://api.github.com/user
Authorization: Bearer gho_xxxxxxxxxxxx
```

## Spring Security OAuth2 Client

Spring Security 提供了开箱即用的 OAuth2 登录支持，核心组件是 `OAuth2LoginAuthenticationFilter`。

### 添加依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

### 配置 OAuth2 提供商

`application.yml` 中配置 GitHub 登录：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          github:
            authorization-uri: https://github.com/login/oauth/authorize
            token-uri: https://github.com/login/oauth/access_token
            user-info-uri: https://api.github.com/user
            user-name-attribute: login
```

Spring Boot 对常见的 OAuth2 提供商（GitHub、Google、Facebook、Okta 等）内置了默认配置，只需提供 `client-id` 和 `client-secret` 即可。

### 安全配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .failureUrl("/login?error=true")
            );
        return http.build();
    }
}
```

配置完成后，访问受保护资源时会自动重定向到 GitHub 登录页面。用户授权后，Spring Security 自动完成 Token 交换、用户信息获取等后续步骤。

## GitHub/Google 第三方登录集成

### GitHub 登录

1. 在 GitHub Settings → Developer settings → OAuth Apps 中创建应用
2. 获取 `Client ID` 和 `Client Secret`
3. 配置回调 URL（Authorization callback URL）
4. 将凭证填入 `application.yml`

### Google 登录

1. 在 Google Cloud Console → APIs & Services → Credentials 中创建 OAuth 2.0 Client
2. 配置 Authorized redirect URIs
3. `application.yml` 配置：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid,profile,email
```

Google 使用 OpenID Connect 协议（基于 OAuth2），`openid` scope 会返回 ID Token。

## OAuth2 用户信息映射

Spring Security 获取到 OAuth2 用户信息后，需要将其映射为本地用户。核心接口是 `OAuth2UserService`。

### 默认行为

默认情况下，Spring Security 使用 `DefaultOAuth2UserService` 获取用户信息，并封装为 `OAuth2User` 对象。

### 自定义用户信息映射

```java
@Component
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String oauthId = oauth2User.getName();
        String nickname = oauth2User.getAttribute("login"); // GitHub
        String email = oauth2User.getAttribute("email");
        String avatar = oauth2User.getAttribute("avatar_url");

        // 查找或创建本地用户
        User localUser = userService.findOrCreateByOAuth(registrationId, oauthId,
                nickname, email, avatar);

        return new CustomOAuth2User(localUser, oauth2User.getAttributes());
    }
}
```

### 自定义 OAuth2User

```java
public class CustomOAuth2User implements OAuth2User, Serializable {
    private final User localUser;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(User localUser, Map<String, Object> attributes) {
        this.localUser = localUser;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + localUser.getRole()));
    }

    @Override
    public String getName() {
        return localUser.getUsername();
    }

    public User getLocalUser() {
        return localUser;
    }
}
```

在安全配置中指定自定义的 `OAuth2UserService`：

```java
.oauth2Login(oauth2 -> oauth2
    .userInfoEndpoint(userInfo -> userInfo
        .userService(customOAuth2UserService)
    )
)
```

## JWT Token 与 OAuth2 结合

在微服务架构中，通常会将 OAuth2 获取的用户信息编码为 JWT Token，作为后续请求的认证凭证。

### 生成 JWT Token

```java
@Service
public class TokenService {

    @Autowired
    private JwtEncoder jwtEncoder;

    public String generateToken(CustomOAuth2User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("codenow")
                .issuedAt(now)
                .expiresAt(now.plus(24, ChronoUnit.HOURS))
                .subject(user.getLocalUser().getId().toString())
                .claim("username", user.getName())
                .claim("role", user.getLocalUser().getRole())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
```

### 配置 JWT 编码器

```java
@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder() {
        RSAKey key = generateRsaKey();
        JWKSet jwkSet = new JWKSet(key);
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(jwkSet));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        RSAKey key = generateRsaKey();
        return NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
    }

    private RSAKey generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
```

生产环境应使用固定密钥对（从配置文件或密钥管理服务加载），而非每次启动随机生成。

## OAuth2 Resource Server

如果项目中有多个微服务，可以在资源服务器端验证 JWT Token：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth-server.com
          # 或者直接指定 JWK Set URI
          jwk-set-uri: https://auth-server.com/.well-known/jwks.json
```

资源服务器配置：

```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain resourceFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("role");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
```

## Spring Authorization Server

Spring Authorization Server 是 Spring 官方提供的 OAuth2 授权服务器实现，用于替代已弃用的 Spring Security OAuth。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
```

基本配置：

```java
@Configuration
public class AuthorizationServerConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("codenow-web")
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:5173/login/oauth2/code/codenow")
                .scope(OidcScopes.OPENID)
                .scope("read")
                .scope("write")
                .build();
        return new InMemoryRegisteredClientRepository(webClient);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:9000")
                .build();
    }
}
```

## 实际项目中的 OAuth2 方案选择

| 场景 | 推荐方案 |
|------|---------|
| 第三方登录（GitHub、Google） | 授权码模式 + `spring-boot-starter-oauth2-client` |
| 微服务间授权 | 客户端凭证模式 + JWT |
| 企业 SSO | Spring Authorization Server + OIDC |
| 单体应用自建认证 | Sa-Token 或 Spring Security + JWT（无需 OAuth2） |
| 移动端登录 | 授权码模式 + PKCE |

## 安全注意事项

### state 参数

`state` 参数用于防止 CSRF 攻击。发起授权请求时生成随机 `state` 值并存入会话，回调时验证 `state` 是否一致：

```java
@GetMapping("/oauth2/authorization/{registrationId}")
public String authorize(@PathVariable String registrationId) {
    String state = UUID.randomUUID().toString();
    session.setAttribute("oauth2_state", state);
    return "redirect:/oauth2/authorization/" + registrationId + "?state=" + state;
}
```

Spring Security 默认已处理 `state` 参数的生成和验证。

### PKCE（Proof Key for Code Exchange）

PKCE 是授权码模式的增强安全机制，防止授权码被截获后滥用。适用于公开客户端（如 SPA、移动端）：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${CLIENT_ID}
            # 公开客户端可以不配置 client-secret
            authorization-grant-type: authorization_code
            client-authentication-method: none
```

PKCE 流程中，客户端生成一个随机的 `code_verifier`，发送授权请求时携带其哈希值 `code_challenge`。换取 Token 时必须提交原始的 `code_verifier`，授权服务器验证两者是否匹配。

### Token 存储

- **服务端**：Token 存储在数据库或 Redis 中，设置合理的过期时间
- **前端**：避免存储在 `localStorage`（容易被 XSS 窃取），推荐使用 `HttpOnly` + `Secure` 的 Cookie
- **JWT 无状态方案**：Token 过期前无法主动失效，需要配合黑名单机制或使用短 Token + Refresh Token 组合
