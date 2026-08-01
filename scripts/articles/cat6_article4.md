# try-with-resources 与异常链详解

资源管理和异常处理是 Java 编程中的重要主题。Java 7 引入的 try-with-resources 语法极大地简化了资源管理，而异常链机制则帮助我们更好地处理和传播异常信息。本文将深入探讨这两个重要特性。

## try-with-resources 语法

try-with-resources 是 Java 7 引入的语法糖，用于自动管理实现了 `AutoCloseable` 接口的资源。使用该语法，资源会在 try 块结束后自动关闭，无需手动编写 finally 块。

```java
// 传统方式（繁琐且容易出错）
BufferedReader reader = null;
try {
    reader = new BufferedReader(new FileReader("data.txt"));
    String line = reader.readLine();
    System.out.println(line);
} catch (IOException e) {
    e.printStackTrace();
} finally {
    if (reader != null) {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// try-with-resources（简洁且安全）
try (BufferedReader reader = new BufferedReader(new FileReader("data.txt"))) {
    String line = reader.readLine();
    System.out.println(line);
} catch (IOException e) {
    e.printStackTrace();
}
// reader 自动关闭，无需 finally
```

## AutoCloseable 和 Closeable 接口

这两个接口定义了资源关闭的契约：

```java
// AutoCloseable - 所有可关闭资源的基础接口
public interface AutoCloseable {
    void close() throws Exception;
}

// Closeable - 继承自 AutoCloseable，专门用于 I/O 资源
public interface Closeable extends AutoCloseable {
    void close() throws IOException;  // 只能抛出 IOException
}
```

两者的主要区别：

| 特性 | AutoCloseable | Closeable |
|------|---------------|-----------|
| close() 抛出的异常 | Exception | IOException |
| 幂等性 | 不要求 | 要求（多次调用安全） |
| 用途 | 通用资源 | I/O 资源 |
| 引入版本 | Java 7 | Java 5 |

```java
// 自定义 AutoCloseable 实现
public class DatabaseConnection implements AutoCloseable {
    private boolean closed = false;

    public void executeQuery(String sql) {
        if (closed) {
            throw new IllegalStateException("连接已关闭");
        }
        System.out.println("执行查询: " + sql);
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            System.out.println("关闭数据库连接");
            closed = true;
        }
    }
}

// 使用
try (DatabaseConnection conn = new DatabaseConnection()) {
    conn.executeQuery("SELECT * FROM users");
}
// 自动调用 conn.close()
```

## 多资源管理

try-with-resources 可以同时管理多个资源，用分号分隔：

```java
try (FileInputStream fis = new FileInputStream("input.txt");
     FileOutputStream fos = new FileOutputStream("output.txt");
     BufferedInputStream bis = new BufferedInputStream(fis);
     BufferedOutputStream bos = new BufferedOutputStream(fos)) {

    byte[] buffer = new byte[1024];
    int bytesRead;
    while ((bytesRead = bis.read(buffer)) != -1) {
        bos.write(buffer, 0, bytesRead);
    }
}
```

**关闭顺序**：资源按照声明的逆序关闭。在上面的例子中，关闭顺序为：`bos` → `bis` → `fos` → `fis`。

```java
public class CloseOrderDemo {
    public static void main(String[] args) {
        try (Resource r1 = new Resource("资源1");
             Resource r2 = new Resource("资源2");
             Resource r3 = new Resource("资源3")) {

            System.out.println("使用资源");
        }
    }
}

class Resource implements AutoCloseable {
    private final String name;

    public Resource(String name) {
        this.name = name;
        System.out.println("打开: " + name);
    }

    @Override
    public void close() {
        System.out.println("关闭: " + name);
    }
}

// 输出：
// 打开: 资源1
// 打开: 资源2
// 打开: 资源3
// 使用资源
// 关闭: 资源3
// 关闭: 资源2
// 关闭: 资源1
```

## 与传统 try-finally 的对比

try-with-resources 相比传统 try-finally 的优势：

```java
// 传统方式的问题
public String readFirstLine1(String file) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(file));
    try {
        return reader.readLine();
    } finally {
        reader.close();  // 如果 readLine() 和 close() 都抛异常，close 的异常会覆盖 readLine 的异常
    }
}

// try-with-resources 的优势
public String readFirstLine2(String file) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        return reader.readLine();
    }
    // 如果有抑制异常，可以通过 getSuppressed() 获取
}
```

try-with-resources 的优势总结：

| 方面 | try-finally | try-with-resources |
|------|-------------|-------------------|
| 代码简洁性 | 冗长 | 简洁 |
| 异常信息 | 可能丢失 | 完整保留 |
| 多资源管理 | 嵌套复杂 | 分号分隔 |
| 关闭顺序 | 需要手动控制 | 自动逆序关闭 |

## 抑制异常（Suppressed Exceptions）

当 try 块和 close() 都抛出异常时，try 块的异常是主异常，close() 的异常会被抑制：

```java
public class SuppressedExceptionDemo {
    public static void main(String[] args) {
        try {
            tryWithException();
        } catch (Exception e) {
            System.out.println("主异常: " + e.getMessage());

            // 获取被抑制的异常
            Throwable[] suppressed = e.getSuppressed();
            for (Throwable t : suppressed) {
                System.out.println("抑制异常: " + t.getMessage());
            }
        }
    }

    static void tryWithException() {
        try (AutoCloseableResource resource = new AutoCloseableResource()) {
            resource.doSomething();  // 抛出 RuntimeException
        } catch (Exception e) {
            // 这里会捕获主异常，抑制异常包含在 e.getSuppressed() 中
            throw new RuntimeException("包装异常", e);
        }
    }
}

class AutoCloseableResource implements AutoCloseable {
    void doSomething() {
        throw new RuntimeException("操作失败");
    }

    @Override
    public void close() {
        throw new RuntimeException("关闭失败");
    }
}

// 输出：
// 主异常: 包装异常
// 抑制异常: 关闭失败
```

## 自定义 AutoCloseable

实现自定义的 AutoCloseable 资源管理：

```java
public class ConnectionPool implements AutoCloseable {
    private final List<Connection> connections = new ArrayList<>();
    private boolean closed = false;

    public ConnectionPool(int size) {
        for (int i = 0; i < size; i++) {
            connections.add(createConnection(i));
        }
    }

    private Connection createConnection(int id) {
        System.out.println("创建连接 #" + id);
        return new Connection(id);
    }

    public Connection getConnection() {
        if (closed) {
            throw new IllegalStateException("连接池已关闭");
        }
        return connections.remove(connections.size() - 1);
    }

    public void returnConnection(Connection conn) {
        if (!closed) {
            connections.add(conn);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            for (Connection conn : connections) {
                conn.close();
            }
            connections.clear();
            System.out.println("连接池已关闭");
        }
    }
}

class Connection {
    private final int id;

    Connection(int id) {
        this.id = id;
    }

    public void execute(String sql) {
        System.out.println("连接 #" + id + " 执行: " + sql);
    }

    public void close() {
        System.out.println("连接 #" + id + " 已关闭");
    }
}

// 使用连接池
try (ConnectionPool pool = new ConnectionPool(5)) {
    Connection conn = pool.getConnection();
    try {
        conn.execute("SELECT * FROM users");
    } finally {
        pool.returnConnection(conn);
    }
}
// 连接池自动关闭
```

更实用的示例 - 文件处理管道：

```java
public class FileProcessor implements AutoCloseable {
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final List<AutoCloseable> resources = new ArrayList<>();

    public FileProcessor(String inputPath, String outputPath) throws IOException {
        this.reader = new BufferedReader(new FileReader(inputPath));
        this.writer = new BufferedWriter(new FileWriter(outputPath));
        resources.add(reader);
        resources.add(writer);
    }

    public void process(LineProcessor processor) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String processed = processor.process(line);
            writer.write(processed);
            writer.newLine();
        }
    }

    @Override
    public void close() throws Exception {
        Exception firstException = null;

        // 按逆序关闭资源
        for (int i = resources.size() - 1; i >= 0; i--) {
            try {
                resources.get(i).close();
            } catch (Exception e) {
                if (firstException == null) {
                    firstException = e;
                } else {
                    firstException.addSuppressed(e);
                }
            }
        }

        if (firstException != null) {
            throw firstException;
        }
    }
}

@FunctionalInterface
interface LineProcessor {
    String process(String line);
}

// 使用
try (FileProcessor processor = new FileProcessor("input.txt", "output.txt")) {
    processor.process(line -> line.toUpperCase());
}
```

## 异常链（Exception Chaining）

异常链用于将底层异常包装为更有意义的业务异常，同时保留原始异常信息。

### 为什么需要异常链

```java
// 不好的做法：丢失原始异常信息
public User getUser(Long id) {
    try {
        return userRepository.findById(id);
    } catch (SQLException e) {
        throw new ServiceException("查询用户失败");  // 丢失了原始异常
    }
}

// 好的做法：使用异常链保留原始异常
public User getUser(Long id) {
    try {
        return userRepository.findById(id);
    } catch (SQLException e) {
        throw new ServiceException("查询用户失败", e);  // 保留原始异常
    }
}
```

### 构造器用法

```java
// 自定义业务异常
public class BusinessException extends RuntimeException {
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
    }

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

// 使用
public class UserService {
    public User getUser(Long id) {
        try {
            return userRepository.findById(id);
        } catch (DatabaseException e) {
            throw new BusinessException("USER_QUERY_FAILED",
                "查询用户失败，ID: " + id, e);
        }
    }

    public void updateUser(User user) {
        try {
            validateUser(user);
            userRepository.save(user);
        } catch (ValidationException e) {
            throw new BusinessException("USER_VALIDATION_FAILED",
                "用户数据验证失败", e);
        } catch (DatabaseException e) {
            throw new BusinessException("USER_UPDATE_FAILED",
                "更新用户失败", e);
        }
    }
}
```

### getCause() 获取原始异常

```java
public class ExceptionChainDemo {
    public static void main(String[] args) {
        try {
            processRequest();
        } catch (BusinessException e) {
            System.out.println("业务异常: " + e.getMessage());
            System.out.println("错误码: " + e.getErrorCode());

            // 获取原始异常
            Throwable cause = e.getCause();
            if (cause != null) {
                System.out.println("原始异常: " + cause.getMessage());

                // 获取更深层的原因
                Throwable rootCause = cause.getCause();
                if (rootCause != null) {
                    System.out.println("根本原因: " + rootCause.getMessage());
                }
            }

            // 打印完整的异常链
            System.out.println("\n完整异常链:");
            for (Throwable t = e; t != null; t = t.getCause()) {
                System.out.println("  - " + t.getClass().getSimpleName()
                    + ": " + t.getMessage());
            }
        }
    }

    static void processRequest() {
        try {
            databaseOperation();
        } catch (DatabaseException e) {
            throw new BusinessException("REQUEST_FAILED", "处理请求失败", e);
        }
    }

    static void databaseOperation() {
        try {
            // 模拟数据库连接失败
            throw new java.sql.SQLException("连接超时");
        } catch (java.sql.SQLException e) {
            throw new DatabaseException("数据库操作失败", e);
        }
    }
}

class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### initCause() 延迟设置原因

```java
// initCause() 用于在异常创建后设置原因
public class LegacyExceptionHandling {
    public static void main(String[] args) {
        try {
            processData();
        } catch (Exception e) {
            System.out.println("异常: " + e.getMessage());
            System.out.println("原因: " + e.getCause().getMessage());
        }
    }

    static void processData() {
        Exception exception = new Exception("数据处理失败");

        try {
            parseData();
        } catch (ParseException e) {
            exception.initCause(e);  // 延迟设置原因
        }

        throw (RuntimeException) exception;
    }
}
```

注意：`initCause()` 只能调用一次，且必须在异常创建后立即调用。推荐使用构造器方式设置原因。

## 异常转译的最佳实践

```java
public class ExceptionTranslationBestPractices {

    // 1. 保留原始异常信息
    public User getUser(Long id) {
        try {
            return userRepository.findById(id);
        } catch (SQLException e) {
            // 好：保留原始异常
            throw new DataAccessException("查询用户失败, id=" + id, e);
        }
    }

    // 2. 提供有意义的业务错误消息
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        try {
            accountRepository.transfer(fromId, toId, amount);
        } catch (InsufficientFundsException e) {
            // 好：提供业务上下文
            throw new BusinessException("TRANSFER_FAILED",
                String.format("转账失败: 余额不足，账户=%d, 金额=%s", fromId, amount), e);
        } catch (AccountNotFoundException e) {
            throw new BusinessException("ACCOUNT_NOT_FOUND",
                String.format("账户不存在: %d", e.getAccountId()), e);
        }
    }

    // 3. 不要捕获不应处理的异常
    public void process() {
        try {
            riskyOperation();
        } catch (BusinessException e) {
            // 可以处理的业务异常
            handleBusinessException(e);
        } catch (OutOfMemoryError e) {
            // 不要捕获 Error，让它传播
            throw e;
        }
    }

    // 4. 使用 try-with-resources 简化资源管理
    public String readFile(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new BusinessException("FILE_READ_FAILED",
                "读取文件失败: " + path, e);
        }
    }

    // 5. 异常处理层次分明
    @Service
    public class OrderService {
        public void createOrder(OrderDTO dto) {
            try {
                validateOrder(dto);
                Order order = convertToOrder(dto);
                orderRepository.save(order);
                sendNotification(order);
            } catch (ValidationException e) {
                // 验证异常：直接抛出，让上层处理
                throw e;
            } catch (DatabaseException e) {
                // 数据库异常：包装为业务异常
                throw new BusinessException("ORDER_CREATE_FAILED",
                    "创建订单失败", e);
            } catch (Exception e) {
                // 未知异常：记录日志并包装
                log.error("创建订单时发生未知错误", e);
                throw new BusinessException("UNKNOWN_ERROR",
                    "系统内部错误", e);
            }
        }
    }
}
```

## Java 9 增强：effectively final 资源变量

Java 9 允许在 try-with-resources 中使用 effectively final 的变量：

```java
// Java 7/8：必须在 try 括号中声明资源
try (BufferedReader reader = new BufferedReader(new FileReader("file.txt"))) {
    System.out.println(reader.readLine());
}

// Java 9：可以使用 effectively final 的变量
BufferedReader reader = new BufferedReader(new FileReader("file.txt"));
try (reader) {  // 变量是 effectively final
    System.out.println(reader.readLine());
}

// 多个资源
FileInputStream fis = new FileInputStream("input.txt");
FileOutputStream fos = new FileOutputStream("output.txt");
try (fis; fos) {  // 分号分隔
    // 使用 fis 和 fos
}

// 更实用的示例
public class Java9TryWithResources {
    public void processFiles(List<String> filePaths) {
        for (String path : filePaths) {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            try (reader) {
                String content = reader.lines()
                    .collect(Collectors.joining("\n"));
                processContent(content);
            } catch (IOException e) {
                System.err.println("处理文件失败: " + path);
            }
        }
    }
}
```

try-with-resources 和异常链是 Java 异常处理的两大利器。掌握它们可以让代码更简洁、更安全、更易维护。
