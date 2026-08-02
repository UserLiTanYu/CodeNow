# 全栈项目安全加固清单

安全不是事后补丁，而是需要从设计阶段就考虑的核心要素。本文提供一份完整的安全加固清单，涵盖认证鉴权、常见攻击防护、依赖安全、传输安全等方面，帮助团队系统性地提升项目安全性。

## 认证鉴权审查

### Token安全

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expiration;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    
    // 生成Token时使用强密钥
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put("roles", userDetails.getAuthorities());
        claims.put("iat", new Date());
        claims.put("jti", UUID.randomUUID().toString()); // 唯一标识，防止重放
        
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
            .compact();
    }
    
    // 验证Token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Token签名验证失败: {}", e.getMessage());
        }
        return false;
    }
    
    // Token黑名单（用于登出）
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public void invalidateToken(String token) {
        String jti = extractJti(token);
        Date expiration = extractExpiration(token);
        long ttl = expiration.getTime() - System.currentTimeMillis();
        
        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                "token:blacklist:" + jti, 
                "invalidated", 
                ttl, 
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    public boolean isTokenInvalidated(String token) {
        String jti = extractJti(token);
        return redisTemplate.hasKey("token:blacklist:" + jti);
    }
}
```

### 密码安全

```java
@Service
public class PasswordService {
    
    // 使用BCrypt加密密码
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    
    public String encodePassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }
    
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
    
    // 密码强度校验
    public void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new BusinessException("密码长度不能少于8个字符");
        }
        if (password.length() > 64) {
            throw new BusinessException("密码长度不能超过64个字符");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("密码必须包含大写字母");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("密码必须包含小写字母");
        }
        if (!password.matches(".*\\d.*")) {
            throw new BusinessException("密码必须包含数字");
        }
    }
    
    // 密码历史检查（防止重复使用）
    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;
    
    public void checkPasswordHistory(Long userId, String newPassword) {
        List<PasswordHistory> history = passwordHistoryRepository
            .findTop5ByUserIdOrderByCreatedAtDesc(userId);
        
        for (PasswordHistory record : history) {
            if (encoder.matches(newPassword, record.getEncodedPassword())) {
                throw new BusinessException("不能使用最近5次使用过的密码");
            }
        }
    }
}
```

### 登录安全

```java
@Service
public class LoginSecurityService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // 登录失败次数限制
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    
    public void checkLoginAttempts(String username) {
        String key = "login:attempts:" + username;
        String attempts = redisTemplate.opsForValue().get(key);
        
        if (attempts != null && Integer.parseInt(attempts) >= MAX_LOGIN_ATTEMPTS) {
            throw new BusinessException("登录失败次数过多，账户已锁定" + LOCKOUT_DURATION_MINUTES + "分钟");
        }
    }
    
    public void recordLoginFailure(String username) {
        String key = "login:attempts:" + username;
        Long attempts = redisTemplate.opsForValue().increment(key);
        
        if (attempts == 1) {
            redisTemplate.expire(key, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
        }
        
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            log.warn("用户 {} 登录失败次数过多，账户已锁定", username);
            // 发送告警通知
            notificationService.sendAlert("账户锁定告警", 
                "用户 " + username + " 登录失败" + attempts + "次，账户已锁定");
        }
    }
    
    public void clearLoginAttempts(String username) {
        redisTemplate.delete("login:attempts:" + username);
    }
    
    // 验证码防暴力破解
    public void validateCaptcha(String captchaKey, String captcha) {
        String cacheKey = "captcha:" + captchaKey;
        String cachedCaptcha = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedCaptcha == null) {
            throw new BusinessException("验证码已过期");
        }
        
        // 使用后立即删除
        redisTemplate.delete(cacheKey);
        
        if (!cachedCaptcha.equalsIgnoreCase(captcha)) {
            throw new BusinessException("验证码错误");
        }
    }
}
```

## SQL注入防护

### 参数化查询

```java
// 错误示例：拼接SQL（存在SQL注入风险）
@Repository
public class ArticleRepositoryBad {
    
    public List<Article> search(String keyword) {
        String sql = "SELECT * FROM articles WHERE title LIKE '%" + keyword + "%'";
        return jdbcTemplate.query(sql, new ArticleRowMapper());
    }
}

// 正确示例：使用参数化查询
@Repository
public class ArticleRepositoryGood {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public List<Article> search(String keyword) {
        String sql = "SELECT * FROM articles WHERE title LIKE ?";
        return jdbcTemplate.query(sql, new ArticleRowMapper(), "%" + keyword + "%");
    }
}

// 使用JPA/MyBatis-Plus（自动防注入）
@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword%")
    List<Article> searchByTitle(@Param("keyword") String keyword);
    
    // MyBatis-Plus条件查询（自动防注入）
    default List<Article> searchByKeyword(String keyword) {
        return selectList(new QueryWrapper<Article>()
            .like("title", keyword)
            .or()
            .like("content", keyword));
    }
}
```

### MyBatis防注入

```xml
<!-- 错误示例：使用${}拼接 -->
<select id="searchArticles" resultType="Article">
    SELECT * FROM articles 
    WHERE title LIKE '%${keyword}%'
</select>

<!-- 正确示例：使用#{}参数化 -->
<select id="searchArticles" resultType="Article">
    SELECT * FROM articles 
    WHERE title LIKE CONCAT('%', #{keyword}, '%')
</select>

<!-- 动态排序防注入 -->
<select id="listArticles" resultType="Article">
    SELECT * FROM articles
    <if test="orderBy != null">
        ORDER BY ${orderBy} ${order}
        <!-- 需要在代码中校验orderBy的值是否在白名单中 -->
    </if>
</select>
```

## XSS防护

### 输入过滤

```java
@Component
public class XssFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XssHttpServletRequestWrapper((HttpServletRequest) request), response);
    }
}

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }
    
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return value != null ? cleanXSS(value) : null;
    }
    
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        
        String[] cleanValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanValues[i] = cleanXSS(values[i]);
        }
        return cleanValues;
    }
    
    private String cleanXSS(String value) {
        // HTML实体编码
        value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
        value = value.replaceAll("'", "&#39;");
        value = value.replaceAll("eval\\((.*)\\)", "");
        value = value.replaceAll("[\\\"\\'][\\s]*javascript:(.*)[\\\"\\']", "\"\"");
        return value;
    }
}
```

### 输出编码

```java
// 使用Jackson序列化时自动编码
@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 启用HTML转义
        mapper.getFactory().setCharacterEscapes(new HTMLCharacterEscapes());
        
        return mapper;
    }
}

// 模板引擎自动转义（Thymeleaf默认开启）
// <div th:text="${article.title}"> 自动转义HTML
// <div th:utext="${article.title}"> 不转义（仅用于可信内容）
```

### 前端防护

```typescript
// 使用DOMPurify净化HTML
import DOMPurify from 'dompurify';

// 渲染用户输入的内容
function renderUserContent(content: string): string {
  return DOMPurify.sanitize(content, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'code', 'pre', 'blockquote', 'ul', 'ol', 'li', 'a'],
    ALLOWED_ATTR: ['href', 'target']
  });
}

// Vue组件中使用
<template>
  <div v-html="sanitizedContent"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import DOMPurify from 'dompurify';

const props = defineProps<{
  content: string;
}>();

const sanitizedContent = computed(() => DOMPurify.sanitize(props.content));
</script>
```

## CSRF防护

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 启用CSRF防护
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/**") // API使用Token认证，可禁用CSRF
            )
            
            // 或者完全禁用CSRF（API服务通常禁用）
            .csrf(csrf -> csrf.disable())
            
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
```

## 依赖漏洞扫描

### Maven依赖检查

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.7</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <suppressionFiles>
            <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
        </suppressionFiles>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### npm依赖检查

```bash
# 审计依赖
npm audit

# 自动修复
npm audit fix

# 强制修复（可能有破坏性变更）
npm audit fix --force
```

### GitHub Dependabot配置

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/codenow-backend"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    
  - package-ecosystem: "npm"
    directory: "/codenow-frontend"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
```

## HTTPS配置

### Nginx SSL配置

```nginx
server {
    listen 80;
    server_name codenow.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name codenow.com;
    
    # SSL证书
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    
    # SSL安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-CHACHA20-POLY1305;
    ssl_prefer_server_ciphers off;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=63072000" always;
    
    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 8.8.8.8 8.8.4.4 valid=300s;
    resolver_timeout 5s;
    
    # SSL会话缓存
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_session_tickets off;
}
```

## CSP头配置

```java
@Component
public class SecurityHeadersFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Content-Security-Policy
        httpResponse.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' https://api.codenow.com; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'"
        );
        
        // X-Content-Type-Options
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        
        // X-Frame-Options
        httpResponse.setHeader("X-Frame-Options", "DENY");
        
        // X-XSS-Protection
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Referrer-Policy
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions-Policy
        httpResponse.setHeader("Permissions-Policy", 
            "camera=(), microphone=(), geolocation=(), payment=()");
        
        chain.doFilter(request, response);
    }
}
```

## 文件上传安全

```java
@Service
public class SecureFileUploadService {
    
    // 允许的文件类型白名单
    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
        "image", Set.of("image/jpeg", "image/png", "image/gif", "image/webp"),
        "document", Set.of("application/pdf", "application/msword")
    );
    
    // 允许的文件扩展名
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS = Map.of(
        "image", Set.of("jpg", "jpeg", "png", "gif", "webp"),
        "document", Set.of("pdf", "doc", "docx")
    );
    
    // 危险的文件扩展名黑名单
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "war", "php", "asp", "jsp"
    );
    
    public FileUploadResult upload(MultipartFile file, String category) {
        // 1. 检查文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        // 2. 检查文件大小
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new BusinessException("文件大小超过限制");
        }
        
        // 3. 获取并验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename).toLowerCase();
        
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不允许上传该类型的文件");
        }
        
        Set<String> allowed = ALLOWED_EXTENSIONS.getOrDefault(category, Collections.emptySet());
        if (!allowed.contains(extension)) {
            throw new BusinessException("不允许的文件类型: " + extension);
        }
        
        // 4. 验证MIME类型
        String contentType = file.getContentType();
        Set<String> allowedMimes = ALLOWED_TYPES.getOrDefault(category, Collections.emptySet());
        if (!allowedMimes.contains(contentType)) {
            throw new BusinessException("文件MIME类型不匹配");
        }
        
        // 5. 验证文件内容（魔数）
        if (!validateFileMagic(file, extension)) {
            throw new BusinessException("文件内容与扩展名不匹配");
        }
        
        // 6. 生成安全的文件名
        String safeFilename = UUID.randomUUID().toString() + "." + extension;
        
        // 7. 存储到非Web可访问目录
        String uploadPath = "/data/uploads/" + category + "/" + getDatePath();
        Path targetPath = Paths.get(uploadPath, safeFilename);
        
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }
        
        return new FileUploadResult(safeFilename, "/api/files/" + category + "/" + safeFilename);
    }
    
    private boolean validateFileMagic(MultipartFile file, String expectedExtension) {
        try {
            byte[] header = new byte[8];
            file.getInputStream().read(header);
            
            // JPEG: FF D8 FF
            if (expectedExtension.matches("jpe?g") && header[0] == (byte) 0xFF && header[1] == (byte) 0xD8) {
                return true;
            }
            
            // PNG: 89 50 4E 47
            if (expectedExtension.equals("png") && header[0] == (byte) 0x89 && header[1] == 0x50) {
                return true;
            }
            
            // GIF: 47 49 46 38
            if (expectedExtension.equals("gif") && header[0] == 0x47 && header[1] == 0x49) {
                return true;
            }
            
            // PDF: 25 50 44 46
            if (expectedExtension.equals("pdf") && header[0] == 0x25 && header[1] == 0x50) {
                return true;
            }
            
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
```

## 日志脱敏

```java
@Component
public class LogDesensitizeHelper {
    
    // 手机号脱敏
    public static String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    // 邮箱脱敏
    public static String desensitizeEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
    
    // 身份证脱敏
    public static String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
    
    // 银行卡脱敏
    public static String desensitizeBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) return bankCard;
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }
}

// 在日志中使用
log.info("用户登录: phone={}, email={}", 
    LogDesensitizeHelper.desensitizePhone(user.getPhone()),
    LogDesensitizeHelper.desensitizeEmail(user.getEmail()));
```

## 数据库权限最小化

```sql
-- 创建应用专用账户（只授予必要权限）
CREATE USER 'codenow_app'@'%' IDENTIFIED BY 'strong_password_here';

-- 授予应用所需权限（不包括DROP、GRANT等危险权限）
GRANT SELECT, INSERT, UPDATE, DELETE ON codenow.* TO 'codenow_app'@'%';

-- 只读账户（用于报表查询）
CREATE USER 'codenow_readonly'@'%' IDENTIFIED BY 'strong_password_here';
GRANT SELECT ON codenow.* TO 'codenow_readonly'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 查看用户权限
SHOW GRANTS FOR 'codenow_app'@'%';
```

## 密钥管理

```java
@Configuration
public class SecretConfig {
    
    // 从环境变量读取密钥
    @Value("${JWT_SECRET}")
    private String jwtSecret;
    
    @Value("${DB_PASSWORD}")
    private String dbPassword;
    
    @Value("${REDIS_PASSWORD}")
    private String redisPassword;
    
    // 不要将密钥硬编码在代码中
    // 错误示例：
    // private static final String SECRET = "my-secret-key";
    
    // 使用密钥管理服务（如AWS Secrets Manager、HashiCorp Vault）
    @Bean
    public VaultTemplate vaultTemplate() {
        return new VaultTemplate(new VaultEndpoint());
    }
}

// .env文件（不要提交到版本控制）
// JWT_SECRET=your-very-long-and-random-secret-key-at-least-32-chars
// DB_PASSWORD=your-database-password
// REDIS_PASSWORD=your-redis-password
```

### .gitignore配置

```gitignore
# 环境变量文件
.env
.env.local
.env.*.local

# 密钥文件
*.pem
*.key
*.jks
*.keystore

# 配置文件中的敏感信息
application-local.yml
application-prod.yml
```

安全加固是一个持续的过程，需要定期审查和更新。以上清单涵盖了常见的安全防护措施，但安全领域不断发展，团队需要持续关注新的安全威胁和防护方案。
