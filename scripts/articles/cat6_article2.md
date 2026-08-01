# 自定义异常与异常设计

## 自定义受检异常

当标准异常无法准确描述业务场景中的错误时，可以创建自定义异常。自定义受检异常需要继承 `Exception` 类：

```java
public class BusinessException extends Exception {
    private String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

使用自定义受检异常：

```java
public void processOrder(Order order) throws BusinessException {
    if (order.getAmount() <= 0) {
        throw new BusinessException("INVALID_AMOUNT", "订单金额必须大于0");
    }
    // 处理订单...
}

// 调用方必须处理
try {
    processOrder(order);
} catch (BusinessException e) {
    log.error("业务异常: [{}] {}", e.getErrorCode(), e.getMessage());
}
```

## 自定义非受检异常

自定义非受检异常需要继承 `RuntimeException`：

```java
public class ResourceNotFoundException extends RuntimeException {
    private String resourceName;
    private String resourceId;

    public ResourceNotFoundException(String resourceName, String resourceId) {
        super(String.format("资源不存在: %s (id=%s)", resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    // getter 方法...
}
```

### 何时选择受检 vs 非受检

| 类型 | 继承自 | 适用场景 | 示例 |
|------|--------|----------|------|
| 受检异常 | Exception | 调用方可以合理恢复 | 业务规则校验失败 |
| 非受检异常 | RuntimeException | 编程错误或不可恢复情况 | 资源不存在、参数非法 |

一般原则：如果调用方能够并且应该处理这个异常，使用受检异常；否则使用非受检异常。

## 自定义异常的最佳实践

### 添加有意义的构造器参数

```java
public class PaymentException extends RuntimeException {
    private final String orderId;
    private final String paymentMethod;
    private final BigDecimal amount;

    public PaymentException(String orderId, String paymentMethod, 
                           BigDecimal amount, String message) {
        super(message);
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    public PaymentException(String orderId, String paymentMethod,
                           BigDecimal amount, String message, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    // getter 方法...
}
```

### 包含错误码

```java
public class ErrorCode {
    public static final String USER_NOT_FOUND = "USER_001";
    public static final String INVALID_PASSWORD = "USER_002";
    public static final String ACCOUNT_LOCKED = "USER_003";
}

public class UserException extends RuntimeException {
    private final String errorCode;

    public UserException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

## throw 与 throws 的区别

### throw：抛出异常

`throw` 用于在方法体内显式抛出一个异常实例：

```java
public void setAge(int age) {
    if (age < 0 || age > 150) {
        throw new IllegalArgumentException("年龄不合法: " + age);
    }
    this.age = age;
}
```

### throws：声明异常

`throws` 用于方法签名，声明该方法可能抛出的异常类型：

```java
public void readFile(String path) throws IOException, FileNotFoundException {
    FileReader reader = new FileReader(path);
    // ...
}
```

### 对比总结

| 特性 | throw | throws |
|------|-------|--------|
| 位置 | 方法体内 | 方法签名 |
| 作用 | 抛出异常实例 | 声明可能的异常类型 |
| 数量 | 每次只能抛出一个 | 可以声明多个异常 |
| 执行 | 程序执行流中断 | 仅声明，不抛出 |

```java
// 综合示例
public User findUser(Long id) throws UserNotFoundException {
    if (id == null) {
        throw new IllegalArgumentException("用户ID不能为空");
    }
    User user = userDao.findById(id);
    if (user == null) {
        throw new UserNotFoundException("用户不存在: " + id);
    }
    return user;
}
```

## 异常链（Exception Chaining）

在实际开发中，底层异常往往不适合直接暴露给调用方。异常链允许我们将底层异常包装为业务异常，同时保留原始原因：

```java
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class UserRepository {
    public User findById(Long id) {
        try {
            // 执行 SQL 查询
            return jdbcTemplate.queryForObject(sql, rowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (DataAccessException e) {
            // 包装底层异常，保留原始原因
            throw new DataAccessException("查询用户失败: " + id, e);
        }
    }
}
```

### 异常链的好处

1. **封装性**：隐藏实现细节，调用方不需要了解底层使用的是 JDBC 还是 MyBatis
2. **可追溯性**：通过 `getCause()` 可以获取原始异常，便于排查问题
3. **统一抽象**：上层只需要处理业务异常类型

```java
try {
    User user = userRepository.findById(1L);
} catch (DataAccessException e) {
    log.error("业务操作失败", e);
    // e.getCause() 可以获取底层的 SQLException 等
}
```

## 异常处理的最佳实践

### 1. 何时捕获、何时传播

```java
// 应该传播：不知道如何处理时
public List<Order> getOrders(Long userId) throws DataAccessException {
    // 异常向上传播，让调用方决定如何处理
    return orderDao.findByUserId(userId);
}

// 应该捕获：知道如何处理时
public String getOrderDisplay(Long orderId) {
    try {
        Order order = orderService.getOrder(orderId);
        return formatOrder(order);
    } catch (OrderNotFoundException e) {
        return "订单不存在";
    }
}
```

### 2. 不要捕获 Exception 或 Throwable

```java
// 错误做法
try {
    // 业务代码
} catch (Exception e) {
    // 过于宽泛，可能捕获到意料之外的异常
    log.error("发生错误", e);
}

// 正确做法
try {
    // 业务代码
} catch (IOException e) {
    // 明确捕获具体异常
    log.error("IO 操作失败", e);
} catch (SQLException e) {
    log.error("数据库操作失败", e);
}
```

### 3. 不要忽略空 catch 块

```java
// 极其错误的做法
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    // 异常被吞掉，问题无法排查
}

// 至少要记录日志
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    log.warn("线程被中断", e);
    Thread.currentThread().interrupt();  // 恢复中断状态
}
```

### 4. 优先使用标准异常

```java
// 不推荐：自定义不必要的异常
public class InvalidAgeException extends RuntimeException { }

// 推荐：使用标准异常
public void setAge(int age) {
    if (age < 0 || age > 150) {
        throw new IllegalArgumentException("年龄必须在 0-150 之间: " + age);
    }
}
```

常用的标准异常：

| 异常类型 | 适用场景 |
|----------|----------|
| `IllegalArgumentException` | 参数值不合法 |
| `IllegalStateException` | 对象状态不正确 |
| `NullPointerException` | 不允许 null 参数 |
| `UnsupportedOperationException` | 方法未实现 |
| `IndexOutOfBoundsException` | 索引越界 |

### 5. 早抛出、晚捕获（Fail Fast）

```java
// 早抛出：在错误发生的地方立即抛出
public void process(String data) {
    if (data == null) {
        throw new NullPointerException("data 不能为 null");
    }
    // 后续代码可以安全使用 data
}

// 晚捕获：在有能力处理的地方才捕获
public void handleRequest(Request request) {
    try {
        validateRequest(request);
        processRequest(request);
        saveResult(request);
    } catch (ValidationException e) {
        return ErrorResponse.badRequest(e.getMessage());
    } catch (ProcessingException e) {
        log.error("处理失败", e);
        return ErrorResponse.serverError();
    }
}
```

## Spring 中的异常处理

### @ExceptionHandler

在 Controller 中处理特定异常：

```java
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException e) {
        ErrorResponse error = new ErrorResponse("USER_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse("INVALID_PARAM", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
}
```

### @ControllerAdvice

全局异常处理器，统一处理所有 Controller 的异常：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        ErrorResponse error = new ErrorResponse("NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("未预期的异常", e);
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "服务器内部错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

通过 `@ControllerAdvice`，可以将异常处理逻辑从业务 Controller 中分离出来，保持代码整洁，同时实现统一的错误响应格式。
