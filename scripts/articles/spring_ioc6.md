# Spring Validation 数据校验

数据校验是任何应用不可或缺的一部分，它确保了数据的完整性和安全性。Spring 框架提供了强大的数据校验支持，结合 Bean Validation（JSR 380）规范，能够以声明式的方式实现数据校验。本文将全面介绍 Spring Validation 的使用方法、最佳实践以及常见场景。

## Bean Validation（JSR 380）概述

Bean Validation 是 Java EE 的数据校验规范，最新版本是 JSR 380（Bean Validation 2.0）。它定义了一套注解和 API，用于对 Java Bean 进行约束校验。

### 核心概念

| 概念 | 说明 |
|------|------|
| 约束（Constraint） | 定义在字段、方法参数或返回值上的注解 |
| 约束验证器（Validator） | 执行校验逻辑的实现类 |
| 校验组（Group） | 对约束进行分组，实现条件校验 |
| 对象图（Object Graph） | 嵌套对象的级联校验 |

### 常用实现

- **Hibernate Validator**：最流行的 Bean Validation 实现
- **Apache BVal**：Apache 的参考实现

Spring Boot 默认集成了 Hibernate Validator，无需额外配置。

## 常用校验注解

### 基本校验注解

```java
public class UserDTO {

    @NotNull(message = "用户名不能为空")
    @NotBlank(message = "用户名不能为空白")
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
    private String username;

    @NotNull(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Past(message = "出生日期必须是过去的日期")
    private LocalDate birthday;

    @Future(message = "过期时间必须是未来的日期")
    private LocalDateTime expireTime;

    @Positive(message = "金额必须为正数")
    private BigDecimal amount;

    @PositiveOrZero(message = "库存不能为负数")
    private Integer stock;

    @URL(message = "URL格式不正确")
    private String website;

    @CreditCardNumber(message = "信用卡号不合法")
    private String creditCard;
}
```

### 校验注解分类

| 分类 | 注解 | 说明 |
|------|------|------|
| 空值校验 | `@NotNull`、`@NotEmpty`、`@NotBlank` | 非空约束 |
| 范围校验 | `@Min`、`@Max`、`@DecimalMin`、`@DecimalMax` | 数值范围 |
| 长度校验 | `@Size`、`@Length` | 字符串/集合长度 |
| 格式校验 | `@Email`、`@URL`、`@Pattern` | 格式匹配 |
| 日期校验 | `@Past`、`@Future`、`@PastOrPresent`、`@FutureOrPresent` | 日期约束 |
| 数值校验 | `@Positive`、`@PositiveOrZero`、`@Negative`、`@NegativeOrZero` | 数值符号 |
| 其他 | `@AssertTrue`、`@AssertFalse`、`@Digits`、`@Valid` | 其他约束 |

### Hibernate Validator 扩展注解

```java
public class ProductDTO {

    @Length(min = 2, max = 100, message = "商品名称长度必须在2-100之间")
    private String name;

    @Range(min = 1, max = 9999, message = "价格范围必须在1-9999之间")
    private BigDecimal price;

    @CreditCardNumber(message = "信用卡号不合法")
    private String creditCard;

    @Currency("CNY")
    private BigDecimal amount;

    @ISBN
    private String isbn;

    @LuhnCheck(message = "校验位不正确")
    private String cardNumber;

    @UniqueElements(message = "列表中存在重复元素")
    private List<String> tags;
}
```

## 分组校验

分组校验允许在不同场景下应用不同的校验规则。

### 定义校验组

```java
public interface ValidationGroups {
    interface Create {}
    interface Update {}
    interface Delete {}
}
```

### 使用分组校验

```java
public class UserDTO {

    @NotNull(groups = Update.class, message = "更新时ID不能为空")
    @Null(groups = Create.class, message = "创建时ID必须为空")
    private Long id;

    @NotBlank(groups = {Create.class, Update.class}, message = "用户名不能为空")
    @Size(min = 2, max = 50, groups = {Create.class, Update.class})
    private String username;

    @NotBlank(groups = Create.class, message = "创建时密码不能为空")
    private String password;

    @Email(groups = {Create.class, Update.class}, message = "邮箱格式不正确")
    private String email;
}
```

### 在 Controller 中使用分组

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<User> createUser(
            @Validated(ValidationGroups.Create.class) @RequestBody UserDTO dto) {
        // 仅校验 Create 分组的约束
        return ResponseEntity.ok(userService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @Validated(ValidationGroups.Update.class) @RequestBody UserDTO dto) {
        // 仅校验 Update 分组的约束
        dto.setId(id);
        return ResponseEntity.ok(userService.update(dto));
    }
}
```

### 默认分组

未指定分组的约束属于 `Default` 分组。可以通过 `@GroupSequence` 控制校验顺序：

```java
@GroupSequence({Default.class, ValidationGroups.Create.class})
public interface CreateGroupSequence {}
```

## 嵌套校验

嵌套校验用于对复杂对象图进行级联校验。

### 基本嵌套校验

```java
public class OrderDTO {

    @NotNull(message = "用户信息不能为空")
    @Valid  // 触发嵌套校验
    private UserDTO user;

    @NotNull(message = "收货地址不能为空")
    @Valid
    private AddressDTO address;

    @NotEmpty(message = "订单项不能为空")
    @Valid
    private List<OrderItemDTO> items;
}

public class AddressDTO {

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "详细地址不能为空")
    private String detail;
}

public class OrderItemDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Positive(message = "数量必须为正数")
    private Integer quantity;

    @Positive(message = "单价必须为正数")
    private BigDecimal price;
}
```

### 嵌套校验的注意事项

1. **@Valid 注解**：必须在需要嵌套校验的字段上添加 `@Valid`
2. **集合类型**：对 `List`、`Set` 等集合类型，`@Valid` 会校验集合中的每个元素
3. **循环引用**：避免循环嵌套导致的无限递归
4. **错误路径**：嵌套校验的错误路径会包含父对象的属性名

## 自定义校验注解

当内置注解无法满足需求时，可以创建自定义校验注解。

### 创建自定义注解

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
@Documented
public @interface PhoneNumber {

    String message() default "手机号格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // 自定义属性
    String region() default "CN";
}
```

### 实现约束验证器

```java
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private String region;

    private static final Map<String, Pattern> PATTERNS = Map.of(
        "CN", Pattern.compile("^1[3-9]\\d{9}$"),
        "US", Pattern.compile("^\\+1\\d{10}$"),
        "UK", Pattern.compile("^\\+44\\d{10}$")
    );

    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        this.region = constraintAnnotation.region();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;  // 使用 @NotNull 校验空值
        }

        Pattern pattern = PATTERNS.get(region);
        if (pattern == null) {
            return false;
        }
        return pattern.matcher(value).matches();
    }
}
```

### 使用自定义注解

```java
public class UserDTO {

    @PhoneNumber(region = "CN", message = "请输入正确的中国手机号")
    private String phone;

    @PhoneNumber(region = "US", message = "请输入正确的美国手机号")
    private String usPhone;
}
```

### 自定义注解的高级用法

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface DateRange {

    String message() default "开始日期必须早于结束日期";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String startDate();

    String endDate();
}

public class DateRangeValidator implements ConstraintValidator<DateRange, Object> {

    private String startDateField;
    private String endDateField;

    @Override
    public void initialize(DateRange constraintAnnotation) {
        this.startDateField = constraintAnnotation.startDate();
        this.endDateField = constraintAnnotation.endDate();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            BeanWrapper wrapper = new BeanWrapperImpl(value);
            LocalDate startDate = (LocalDate) wrapper.getPropertyValue(startDateField);
            LocalDate endDate = (LocalDate) wrapper.getPropertyValue(endDateField);

            if (startDate == null || endDate == null) {
                return true;
            }

            return !startDate.isAfter(endDate);
        } catch (Exception e) {
            return false;
        }
    }
}
```

使用类级别校验：

```java
@DateRange(startDate = "startDate", endDate = "endDate", 
           message = "开始日期必须早于结束日期")
public class DateRangeDTO {
    private LocalDate startDate;
    private LocalDate endDate;
}
```

## Controller 层校验

### @Valid @RequestBody

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserDTO dto) {
        return ResponseEntity.ok(userService.create(dto));
    }
}
```

### BindingResult 手动处理

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO dto,
                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(errors);
        }
        return ResponseEntity.ok(userService.create(dto));
    }
}
```

### 路径变量和请求参数校验

```java
@RestController
@RequestMapping("/api/users")
@Validated  // 启用方法级别校验
public class UserController {

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam @Size(min = 2, max = 50, message = "关键词长度必须在2-50之间") 
            String keyword,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return ResponseEntity.ok(userService.search(keyword, page, size));
    }
}
```

## @Validated vs @Valid 的区别

| 特性 | @Validated | @Valid |
|------|------------|--------|
| 来源 | Spring Framework | Jakarta Bean Validation |
| 分组校验 | 支持（value 属性） | 不支持 |
| 嵌套校验 | 不支持 | 支持 |
| 方法级别校验 | 支持 | 不支持 |
| 使用位置 | 类、方法参数 | 字段、方法参数、构造器参数 |

### 最佳搭配

```java
// Controller 参数使用 @Validated 进行分组校验
@PostMapping
public ResponseEntity<User> createUser(
        @Validated(ValidationGroups.Create.class) @RequestBody UserDTO dto) {
    // ...
}

// 嵌套对象内部使用 @Valid 触发级联校验
public class OrderDTO {
    @Valid  // 嵌套校验
    private UserDTO user;
}
```

## 方法级校验

方法级校验允许在 Service 层对方法参数和返回值进行校验。

### 启用方法级校验

```java
@Configuration
@EnableMethodValidation
public class MethodValidationConfig {
}
```

### 在 Service 中使用

```java
@Service
@Validated
public class UserService {

    public User findById(@Min(1) Long id) {
        // 参数校验：id 必须大于 0
        return userRepository.findById(id).orElse(null);
    }

    @Validated(ValidationGroups.Create.class)
    public User createUser(@Valid UserDTO dto) {
        // 校验 Create 分组的约束
        User user = convertToEntity(dto);
        return userRepository.save(user);
    }

    @NotNull
    @Valid
    public List<User> searchUsers(@NotBlank String keyword,
                                   @Min(0) Integer page,
                                   @Min(1) @Max(100) Integer size) {
        // 返回值校验：不能为 null
        return userRepository.search(keyword, PageRequest.of(page, size));
    }
}
```

### 方法级校验的异常处理

方法级校验失败会抛出 `ConstraintViolationException`：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(
            ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String property = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(property, message);
        });
        return ResponseEntity.badRequest().body(errors);
    }
}
```

## 统一校验异常处理

### @ControllerAdvice 全局异常处理

```java
@RestControllerAdvice
public class ValidationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ValidationExceptionHandler.class);

    /**
     * 处理 @Valid @RequestBody 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        log.warn("请求参数校验失败: {}", ex.getMessage());

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );

        ApiResponse<Void> response = ApiResponse.error(
            HttpStatus.BAD_REQUEST.value(),
            "请求参数校验失败",
            errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理 @Validated 方法级校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex) {
        log.warn("方法参数校验失败: {}", ex.getMessage());

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String property = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(property, message);
        });

        ApiResponse<Void> response = ApiResponse.error(
            HttpStatus.BAD_REQUEST.value(),
            "方法参数校验失败",
            errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理请求参数类型错误
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型错误: {}", ex.getMessage());

        String message = String.format("参数 '%s' 类型错误，期望类型: %s",
            ex.getName(), ex.getRequiredType().getSimpleName());

        ApiResponse<Void> response = ApiResponse.error(
            HttpStatus.BAD_REQUEST.value(),
            message
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理缺少必要参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex) {
        log.warn("缺少必要参数: {}", ex.getMessage());

        String message = String.format("缺少必要参数: '%s'", ex.getParameterName());

        ApiResponse<Void> response = ApiResponse.error(
            HttpStatus.BAD_REQUEST.value(),
            message
        );
        return ResponseEntity.badRequest().body(response);
    }
}
```

### 统一响应封装

```java
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private Map<String, String> errors;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> error(int code, String message, 
                                            Map<String, String> errors) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setErrors(errors);
        return response;
    }

    // getters and setters
}
```

## 校验最佳实践

### DTO 层校验

为不同场景定义不同的 DTO：

```java
// 创建用户 DTO
public class CreateUserDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度必须在8-100之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "密码必须包含大小写字母和数字")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}

// 更新用户 DTO
public class UpdateUserDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;
}

// 查询 DTO
public class QueryUserDTO {

    @Size(max = 100, message = "关键词长度不能超过100")
    private String keyword;

    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Integer size = 10;
}
```

### Service 层校验

```java
@Service
@Validated
public class UserService {

    @Transactional
    public User createUser(@Valid CreateUserDTO dto) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        return userRepository.save(user);
    }
}
```

### 国际化错误消息

1. 定义消息文件：

```properties
# messages.properties
user.username.notBlank=Username is required
user.username.size=Username must be between {min} and {max} characters

# messages_zh_CN.properties
user.username.notBlank=用户名不能为空
user.username.size=用户名长度必须在{min}到{max}之间
```

2. 在注解中使用消息键：

```java
public class UserDTO {

    @NotBlank(message = "{user.username.notBlank}")
    @Size(min = 2, max = 50, message = "{user.username.size}")
    private String username;
}
```

3. 配置消息源：

```java
@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = 
            new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
```

### 校验性能优化

1. **缓存 Validator 实例**：避免每次校验都创建新的 Validator
2. **快速失败模式**：配置 Validator 在第一个错误时立即返回
3. **批量校验**：对批量数据使用 `validate()` 方法一次性校验
4. **异步校验**：对于耗时的校验逻辑（如数据库查询），考虑异步执行

```java
@Configuration
public class ValidatorConfig {

    @Bean
    public Validator validator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            // Hibernate Validator 特定配置
            if (validator instanceof HibernateValidator hv) {
                // 快速失败模式
                hv.unwrap(Validator.class);
            }
            return validator;
        }
    }
}
```

### 校验与业务逻辑分离

校验仅负责数据格式和基本约束，复杂业务规则应在 Service 层处理：

```java
// 校验层：格式和基本约束
public class TransferDTO {

    @NotNull(message = "转出账户不能为空")
    private Long fromAccountId;

    @NotNull(message = "转入账户不能为空")
    private Long toAccountId;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须为正数")
    @DecimalMin(value = "0.01", message = "金额最小为0.01")
    @DecimalMax(value = "1000000.00", message = "金额最大为1000000.00")
    private BigDecimal amount;
}

// 业务层：复杂业务规则
@Service
public class TransferService {

    @Transactional
    public void transfer(@Valid TransferDTO dto) {
        // 业务规则校验
        if (dto.getFromAccountId().equals(dto.getToAccountId())) {
            throw new BusinessException("转出账户和转入账户不能相同");
        }

        Account fromAccount = accountRepository.findById(dto.getFromAccountId())
            .orElseThrow(() -> new BusinessException("转出账户不存在"));

        Account toAccount = accountRepository.findById(dto.getToAccountId())
            .orElseThrow(() -> new BusinessException("转入账户不存在"));

        if (fromAccount.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new BusinessException("余额不足");
        }

        // 执行转账
        fromAccount.setBalance(fromAccount.getBalance().subtract(dto.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(dto.getAmount()));
    }
}
```

Spring Validation 提供了全面而灵活的数据校验机制。通过合理使用内置注解、分组校验、嵌套校验和自定义校验注解，可以构建健壮的数据校验体系。结合统一异常处理和国际化支持，能够为用户提供友好的错误反馈。