# 接口安全：限流、加密与防重放

一个对外暴露的 API 接口，面临着各种安全威胁：恶意爬虫高频请求导致服务崩溃、中间人窃取敏感数据、攻击者截获请求后重放执行……接口安全不是某个单一技术能解决的问题，而是需要从限流、加密、签名、防注入等多个维度构建纵深防御体系。

## 接口安全概述

接口安全可以围绕五个核心目标来构建：

| 目标 | 含义 | 常见威胁 |
|------|------|---------|
| 认证（Authentication） | 确认调用者身份 | 伪造身份、Token 窃取 |
| 授权（Authorization） | 确认调用者权限 | 越权访问、水平越权 |
| 机密性（Confidentiality） | 数据不被窃听 | 中间人攻击、明文传输 |
| 完整性（Integrity） | 数据不被篡改 | 参数篡改、重放攻击 |
| 可用性（Availability） | 服务持续可用 | DDoS、CC 攻击 |

## 接口限流

限流是保护服务可用性的第一道防线。当请求超过系统承载能力时，直接拒绝多余请求，保证核心功能正常运行。

### 固定窗口计数器

最简单的限流算法，在固定时间窗口内计数：

```
窗口大小：1 分钟
限制次数：100 次

00:00 - 01:00 → 计数，超过 100 次拒绝
01:00 - 02:00 → 计数器重置
```

**缺点**：窗口边界处可能突发两倍流量（00:59 来 100 次 + 01:00 来 100 次 = 1 秒内 200 次）。

### 滑动窗口计数器

将时间窗口划分为多个小格，滑动统计：

```
窗口大小：1 分钟，分为 6 个 10 秒格
当前时间：01:25
统计范围：00:25 - 01:25（滑动窗口）
```

Redis 实现滑动窗口的核心思路是使用 Sorted Set，以时间戳为 score：

```lua
-- Redis Lua 滑动窗口限流脚本
local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])  -- 窗口大小（毫秒）
local limit = tonumber(ARGV[3])   -- 限制次数

-- 移除窗口外的旧记录
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

-- 当前窗口内的请求数
local count = redis.call('ZCARD', key)

if count < limit then
    redis.call('ZADD', key, now, now .. ':' .. math.random(1000000))
    redis.call('PEXPIRE', key, window)
    return 1  -- 允许
else
    return 0  -- 拒绝
end
```

### 令牌桶算法

以固定速率向桶中放入令牌，请求需要消耗令牌：

```
桶容量：100 令牌
填充速率：10 令牌/秒

请求到来 → 桶中有令牌 → 消耗 1 令牌，允许
请求到来 → 桶中无令牌 → 拒绝
```

令牌桶允许一定程度的突发流量（桶满时可以瞬间消耗所有令牌），适合大多数 API 限流场景。

### 漏桶算法

请求进入漏桶排队，以固定速率流出处理：

```
漏桶容量：100 请求
流出速率：10 请求/秒

请求到来 → 桶未满 → 入桶等待
请求到来 → 桶已满 → 拒绝
```

漏桶算法严格控制处理速率，不允许突发流量，适合对处理速率敏感的场景。

### 算法对比

| 算法 | 突发流量 | 实现复杂度 | 适用场景 |
|------|---------|-----------|---------|
| 固定窗口 | 有边界问题 | 低 | 简单限流 |
| 滑动窗口 | 平滑 | 中 | 精确限流 |
| 令牌桶 | 允许突发 | 中 | API 网关通用限流 |
| 漏桶 | 不允许突发 | 中 | 严格速率控制 |

## Spring Boot 限流实现

### Guava RateLimiter

Google Guava 提供了基于令牌桶的限流器，适合单机限流：

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.0.0-jre</version>
</dependency>
```

```java
@Service
public class RateLimitService {

    // 每秒 100 个令牌
    private final RateLimiter rateLimiter = RateLimiter.create(100);

    public boolean tryAcquire() {
        return rateLimiter.tryAcquire();
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        return rateLimiter.tryAcquire(permits, timeout, unit);
    }
}
```

在 Controller 中使用：

```java
@RestController
public class ArticleController {

    @Autowired
    private RateLimitService rateLimitService;

    @GetMapping("/api/articles")
    public ResponseEntity<?> listArticles() {
        if (!rateLimitService.tryAcquire()) {
            return ResponseEntity.status(429).body("请求过于频繁，请稍后再试");
        }
        return ResponseEntity.ok(articleService.list());
    }
}
```

### Redis + Lua 滑动窗口限流

分布式限流需要使用 Redis：

```java
@Component
public class RedisRateLimiter {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> rateLimitScript;

    public RedisRateLimiter() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptText(RATE_LIMIT_LUA);
        rateLimitScript.setResultType(Long.class);
    }

    public boolean isAllowed(String key, int limit, int windowSeconds) {
        Long result = redisTemplate.execute(rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(windowSeconds * 1000L),
                String.valueOf(limit));
        return result != null && result == 1L;
    }

    private static final String RATE_LIMIT_LUA = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local limit = tonumber(ARGV[3])
        redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
        local count = redis.call('ZCARD', key)
        if count < limit then
            redis.call('ZADD', key, now, now .. ':' .. math.random(1000000))
            redis.call('PEXPIRE', key, window)
            return 1
        else
            return 0
        end
        """;
}
```

## @RateLimit 自定义注解 + AOP

将限流逻辑封装为注解，提升代码可读性。

**定义注解：**

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 100;           // 窗口内允许的最大请求数
    int windowSeconds() default 60;    // 时间窗口（秒）
    String key() default "";           // 限流 key（支持 SpEL）
}
```

**AOP 切面：**

```java
@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private RedisRateLimiter rateLimiter;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = resolveKey(point, rateLimit);
        if (!rateLimiter.isAllowed(key, rateLimit.limit(), rateLimit.windowSeconds())) {
            throw new TooManyRequestsException("请求过于频繁，请稍后再试");
        }
        return point.proceed();
    }

    private String resolveKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        if (!rateLimit.key().isEmpty()) {
            return SpELParser.parse(rateLimit.key(), point);
        }
        // 默认按 方法名 + IP 限流
        String methodName = point.getSignature().toShortString();
        String ip = IpUtils.getClientIp();
        return "rate_limit:" + methodName + ":" + ip;
    }
}
```

**使用注解：**

```java
@RateLimit(limit = 10, windowSeconds = 60)  // 每分钟最多 10 次
@PostMapping("/api/auth/captcha")
public Result<String> getCaptcha() {
    return Result.ok(captchaService.generate());
}

@RateLimit(limit = 5, windowSeconds = 300)  // 每 5 分钟最多 5 次
@PostMapping("/api/auth/login")
public Result<String> login(@RequestBody LoginRequest request) {
    return authService.login(request);
}
```

## 接口防重放

重放攻击是指攻击者截获合法请求后原样重新发送，导致操作被重复执行（如重复扣款、重复下单）。

### nonce + timestamp 签名验证

防重放的核心思路：每个请求携带唯一标识（nonce）和时间戳，服务端验证请求是否过期且未被使用过。

**客户端生成签名：**

```java
public class RequestSigner {

    public static Map<String, String> sign(String appKey, String appSecret, String body) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");

        // 待签名字符串
        String signString = appKey + timestamp + nonce + body;

        // HMAC-SHA256 签名
        String signature = HmacUtils.hmacSha256Hex(appSecret, signString);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-App-Key", appKey);
        headers.put("X-Timestamp", timestamp);
        headers.put("X-Nonce", nonce);
        headers.put("X-Signature", signature);
        return headers;
    }
}
```

**服务端验证：**

```java
@Component
public class AntiReplayFilter implements Filter {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final long MAX_AGE_MS = 300_000; // 5 分钟过期

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String timestamp = req.getHeader("X-Timestamp");
        String nonce = req.getHeader("X-Nonce");
        String signature = req.getHeader("X-Signature");
        String appKey = req.getHeader("X-App-Key");

        // 1. 验证参数完整性
        if (timestamp == null || nonce == null || signature == null) {
            ((HttpServletResponse) response).sendError(401, "缺少签名参数");
            return;
        }

        // 2. 验证时间戳是否过期
        long requestTime = Long.parseLong(timestamp);
        if (Math.abs(System.currentTimeMillis() - requestTime) > MAX_AGE_MS) {
            ((HttpServletResponse) response).sendError(401, "请求已过期");
            return;
        }

        // 3. 验证 nonce 是否已使用
        String nonceKey = "nonce:" + nonce;
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", Duration.ofMillis(MAX_AGE_MS));
        if (Boolean.FALSE.equals(isNew)) {
            ((HttpServletResponse) response).sendError(401, "重复请求");
            return;
        }

        // 4. 验证签名
        String body = getRequestBody(req);
        String appSecret = getAppSecret(appKey);
        String expectedSig = HmacUtils.hmacSha256Hex(appSecret, appKey + timestamp + nonce + body);
        if (!expectedSig.equals(signature)) {
            ((HttpServletResponse) response).sendError(401, "签名验证失败");
            return;
        }

        chain.doFilter(request, response);
    }
}
```

## 请求签名：HMAC-SHA256

HMAC-SHA256 是最常用的 API 签名算法，兼具安全性和实现简便性。

### 签名生成流程

```
1. 将请求参数按字典序排序
2. 拼接为 key1=value1&key2=value2 形式
3. 在首尾拼接 appSecret：appSecret + sortedParams + appSecret
4. 对拼接结果做 SHA-256 哈希
```

```java
public class SignatureUtils {

    public static String generate(Map<String, String> params, String secret) {
        // 1. 按 key 字典序排序
        TreeMap<String, String> sorted = new TreeMap<>(params);

        // 2. 拼接
        StringBuilder sb = new StringBuilder(secret);
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        sb.deleteCharAt(sb.length() - 1); // 去掉末尾 &
        sb.append(secret);

        // 3. SHA-256
        return DigestUtils.sha256Hex(sb.toString());
    }
}
```

### 签名验证服务端实现

```java
public boolean verifySignature(HttpServletRequest request, String appSecret) {
    Map<String, String> params = new HashMap<>();
    request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));

    String clientSignature = request.getHeader("X-Signature");
    String serverSignature = SignatureUtils.generate(params, appSecret);

    return MessageDigest.isEqual(
            clientSignature.getBytes(StandardCharsets.UTF_8),
            serverSignature.getBytes(StandardCharsets.UTF_8)
    );
}
```

注意：使用 `MessageDigest.isEqual()` 进行比较，该方法使用**常量时间比较**，可以防止时序攻击（Timing Attack）。

## 数据加密

### 对称加密（AES）

AES 是最常用的对称加密算法，适合加密大量数据：

```java
public class AESUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH = 128;

    public static String encrypt(String plaintext, String key) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH, iv));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // IV + 密文一起 Base64 编码
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(String ciphertext, String key) throws Exception {
        byte[] combined = Base64.getDecoder().decode(ciphertext);
        byte[] iv = Arrays.copyOfRange(combined, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);

        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }
}
```

GCM 模式同时提供加密和完整性校验（AEAD），比 CBC 模式更安全。

### 非对称加密（RSA）

RSA 适合密钥交换和少量数据加密：

```java
public class RSAUtils {

    public static KeyPair generateKeyPair(int keySize) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }

    public static String encrypt(String plaintext, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes()));
    }

    public static String decrypt(String ciphertext, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)));
    }
}
```

### 国密（SM2/SM4）

国内项目可能需要使用国密算法：

```java
// SM4 对称加密（需要 Bouncy Castle 依赖）
public class SM4Utils {
    public static String encrypt(String plaintext, String hexKey) {
        SM4Engine engine = new SM4Engine();
        // ... 初始化和加密逻辑
    }
}

// SM2 非对称加密 / 签名
public class SM2Utils {
    public static String sign(String data, String privateKeyHex) {
        // ... SM2 签名逻辑
    }
}
```

## HTTPS/TLS

所有对外 API 必须使用 HTTPS，防止数据在传输过程中被窃听或篡改。

### Spring Boot HTTPS 配置

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    protocol: TLS
```

### HSTS 头

HTTP Strict Transport Security 告知浏览器只通过 HTTPS 访问：

```java
@Component
public class HstsFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        chain.doFilter(req, res);
    }
}
```

## SQL 注入与 XSS 防护

### SQL 注入防护

MyBatis 使用 `#{}` 占位符可以有效防止 SQL 注入，因为参数会被预编译为 `PreparedStatement`：

```xml
<!-- 安全：使用 #{} -->
<select id="findByName" resultType="Article">
    SELECT * FROM article WHERE title = #{title}
</select>

<!-- 危险：使用 ${}，直接拼接字符串 -->
<select id="findByName" resultType="Article">
    SELECT * FROM article WHERE title = '${title}'
</select>
```

`${}` 只在动态表名、列名、ORDER BY 等无法参数化的场景中使用，且必须由代码内部控制值，不能接受用户输入。

### XSS 防护

**输入校验**：对用户输入进行白名单过滤：

```java
public class InputSanitizer {
    public static String sanitize(String input) {
        if (input == null) return null;
        // 移除 HTML 标签
        return input.replaceAll("<[^>]*>", "")
                    .replaceAll("javascript:", "")
                    .replaceAll("on\\w+\\s*=", "");
    }
}
```

**输出编码**：在模板引擎中自动转义。Thymeleaf 默认会转义 HTML：

```html
<!-- Thymeleaf 自动转义，安全 -->
<p th:text="${article.title}"></p>

<!-- 使用 th:utext 不转义，危险 -->
<p th:utext="${article.content}"></p>
```

**Content-Security-Policy 头**：

```java
response.setHeader("Content-Security-Policy",
    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'");
```

## 接口安全最佳实践清单

| 层面 | 措施 | 优先级 |
|------|------|--------|
| 传输层 | 全站 HTTPS + HSTS | 必须 |
| 认证 | JWT Token + 合理过期时间 | 必须 |
| 授权 | RBAC 细粒度权限控制 | 必须 |
| 限流 | 接口级别限流（注解 + Redis） | 必须 |
| 防重放 | nonce + timestamp + 签名 | 推荐 |
| 签名 | HMAC-SHA256 请求签名 | 推荐 |
| 加密 | 敏感字段 AES-GCM 加密 | 视场景 |
| 输入校验 | 参数校验 + SQL 注入防护 | 必须 |
| 输出编码 | XSS 转义 + CSP 头 | 必须 |
| 日志审计 | 关键操作记录日志 | 推荐 |
| 错误处理 | 生产环境不暴露堆栈信息 | 必须 |
