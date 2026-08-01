# 异常体系与 try-catch-finally

## 为什么需要异常处理

在早期的 C 语言时代，程序员通常使用**错误码**来表示函数执行是否成功。例如返回 `0` 表示成功，返回 `-1` 表示失败。这种方式存在明显的问题：

- **调用方容易忽略错误检查**：编译器不会强制检查错误码，程序员可能忘记判断返回值
- **错误码与正常返回值混杂**：某些函数的正常返回值范围很大，难以设计错误码
- **错误传播困难**：每一层调用都需要手动检查和传递错误码，代码臃肿

Java 引入了**异常机制**来解决这些问题：

```java
// 错误码方式（C 风格）
int result = readFile(path);
if (result == -1) {
    // 处理错误
}

// Java 异常方式
try {
    readFile(path);
} catch (IOException e) {
    // 处理错误
}
```

异常机制的核心优势：

| 特性 | 错误码 | 异常机制 |
|------|--------|----------|
| 强制处理 | 否 | 受检异常强制处理 |
| 错误传播 | 手动逐层传递 | 自动向上传播 |
| 错误信息 | 通常只是一个数字 | 包含详细错误信息和堆栈 |
| 代码分离 | 错误处理与业务逻辑混杂 | 正常逻辑与异常处理分离 |

## Java 异常体系结构

Java 的异常体系以 `Throwable` 为根，分为两个主要分支：

```
Throwable
├── Error（错误）
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── ...
└── Exception（异常）
    ├── RuntimeException（运行时异常，非受检）
    │   ├── NullPointerException
    │   ├── ArrayIndexOutOfBoundsException
    │   └── ...
    └── 其他 Exception（受检异常）
        ├── IOException
        ├── SQLException
        └── ...
```

### Error vs Exception

**Error** 表示 JVM 层面的严重错误，应用程序通常无法处理也不应该捕获：

- `OutOfMemoryError`：JVM 堆内存不足
- `StackOverflowError`：栈溢出（通常是无限递归导致）
- `NoClassDefFoundError`：类定义找不到

```java
// 不要捕获 Error！
try {
    // 可能导致 OOM 的代码
} catch (OutOfMemoryError e) {
    // 这是错误的做法，Error 不应该被捕获
}
```

**Exception** 表示应用程序可以处理的异常情况，是我们在编码时主要关注的对象。

## 受检异常 vs 非受检异常

Java 将 Exception 分为两类：

### 受检异常（Checked Exception）

- 编译器**强制要求**处理（try-catch 或 throws 声明）
- 表示可预见的、合理的异常情况
- 例如：`IOException`、`SQLException`、`FileNotFoundException`

```java
// 受检异常必须处理，否则编译报错
public void readFile(String path) throws IOException {
    FileReader reader = new FileReader(path);  // 可能抛出 FileNotFoundException
}
```

### 非受检异常（Unchecked Exception / RuntimeException）

- 编译器**不强制**处理
- 通常是编程错误导致的
- 例如：`NullPointerException`、`ArrayIndexOutOfBoundsException`

```java
// 非受检异常可以不处理
public int divide(int a, int b) {
    return a / b;  // 可能抛出 ArithmeticException，但编译器不强制处理
}
```

| 类型 | 编译器检查 | 典型场景 | 示例 |
|------|-----------|----------|------|
| 受检异常 | 强制处理 | 外部资源不可用 | IOException |
| 非受检异常 | 不强制 | 编程逻辑错误 | NullPointerException |

## try-catch 语句

### 基本语法

```java
try {
    // 可能抛出异常的代码
    FileReader reader = new FileReader("test.txt");
} catch (FileNotFoundException e) {
    // 处理异常
    System.out.println("文件不存在: " + e.getMessage());
}
```

### 多 catch 分支

可以针对不同类型的异常进行不同的处理：

```java
try {
    String text = Files.readString(Path.of("data.txt"));
    int number = Integer.parseInt(text.trim());
    int[] arr = new int[5];
    arr[number] = 100;
} catch (FileNotFoundException e) {
    System.out.println("文件不存在");
} catch (NumberFormatException e) {
    System.out.println("数字格式错误");
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("数组越界");
}
```

### 多异常捕获（Java 7+）

从 Java 7 开始，可以在一个 catch 块中捕获多种异常，使用 `|` 分隔：

```java
try {
    // 可能抛出多种异常的代码
} catch (FileNotFoundException | NoSuchFileException e) {
    System.out.println("文件相关异常");
} catch (NumberFormatException | ArithmeticException e) {
    System.out.println("数值相关异常");
}
```

**注意**：多异常捕获时，异常变量 `e` 是隐式 final 的，不能对其重新赋值。

### 嵌套 try

```java
try {
    FileReader reader = new FileReader("config.txt");
    try {
        int data = reader.read();
    } catch (IOException e) {
        System.out.println("读取数据失败");
    } finally {
        reader.close();
    }
} catch (FileNotFoundException e) {
    System.out.println("配置文件不存在");
}
```

## finally 语句

### 基本特性

`finally` 块中的代码**无论如何都会执行**，无论 try 块是否抛出异常、是否被捕获：

```java
FileReader reader = null;
try {
    reader = new FileReader("data.txt");
    // 处理文件
} catch (FileNotFoundException e) {
    System.out.println("文件不存在");
} finally {
    // 无论是否异常，都会执行
    if (reader != null) {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### finally 的执行时机

```java
public static int testFinally() {
    try {
        return 1;
    } finally {
        System.out.println("finally 执行");  // 会在 return 之前执行
    }
}
// 输出: "finally 执行"，然后返回 1
```

### finally 中的 return 陷阱

**强烈不建议**在 finally 中使用 return，这会导致意外行为：

```java
public static int dangerousCode() {
    try {
        return 1;
    } finally {
        return 2;  // 会覆盖 try 中的 return！
    }
}
// 返回 2，而不是 1
```

```java
public static int anotherTrap() {
    try {
        throw new RuntimeException("异常");
    } finally {
        return 0;  // 会吞掉异常！
    }
}
// 返回 0，异常被静默吞掉
```

## try-with-resources 语句

Java 7 引入了 try-with-resources 语法，简化资源管理：

### AutoCloseable 接口

实现 `AutoCloseable` 接口的资源可以自动关闭：

```java
public interface AutoCloseable {
    void close() throws Exception;
}
```

### 基本用法

```java
// Java 7 之前
BufferedReader br = null;
try {
    br = new BufferedReader(new FileReader("data.txt"));
    String line = br.readLine();
} finally {
    if (br != null) {
        br.close();
    }
}

// Java 7+ try-with-resources
try (BufferedReader br = new BufferedReader(new FileReader("data.txt"))) {
    String line = br.readLine();
}  // br 自动关闭
```

### 多个资源

多个资源使用分号分隔，关闭顺序与声明顺序相反：

```java
try (
    FileReader fr = new FileReader("input.txt");
    BufferedReader br = new BufferedReader(fr);
    FileWriter fw = new FileWriter("output.txt");
    BufferedWriter bw = new BufferedWriter(fw)
) {
    // 使用资源
    String line;
    while ((line = br.readLine()) != null) {
        bw.write(line);
        bw.newLine();
    }
}  // 按顺序关闭: bw → fw → br → fr
```

### 自定义 AutoCloseable

```java
public class DatabaseConnection implements AutoCloseable {
    @Override
    public void close() throws Exception {
        System.out.println("关闭数据库连接");
    }
}

try (DatabaseConnection conn = new DatabaseConnection()) {
    // 使用连接
}  // 自动调用 close()
```

## 异常对象的常用方法

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `getMessage()` | String | 获取详细错误信息 |
| `getCause()` | Throwable | 获取导致当前异常的原因 |
| `getStackTrace()` | StackTraceElement[] | 获取堆栈跟踪信息 |
| `printStackTrace()` | void | 打印异常堆栈到标准错误流 |
| `toString()` | String | 返回异常类名和 getMessage() |

```java
try {
    int result = 10 / 0;
} catch (ArithmeticException e) {
    System.out.println(e.getMessage());    // "/ by zero"
    System.out.println(e.toString());      // "java.lang.ArithmeticException: / by zero"
    e.printStackTrace();                    // 打印完整堆栈
}
```

## 常见标准异常

### RuntimeException 子类

| 异常 | 说明 | 典型触发场景 |
|------|------|-------------|
| `NullPointerException` | 空指针访问 | 调用 null 对象的方法 |
| `ArrayIndexOutOfBoundsException` | 数组越界 | 访问非法索引 |
| `ClassCastException` | 类型转换失败 | 强制类型转换不兼容类型 |
| `IllegalArgumentException` | 非法参数 | 方法参数不合法 |
| `NumberFormatException` | 数字格式错误 | `Integer.parseInt("abc")` |
| `ArithmeticException` | 算术异常 | 除以零 |

### 受检异常

| 异常 | 说明 | 典型触发场景 |
|------|------|-------------|
| `IOException` | IO 异常 | 文件读写失败 |
| `FileNotFoundException` | 文件不存在 | 访问不存在的文件 |
| `SQLException` | SQL 异常 | 数据库操作失败 |

```java
// 常见异常示例
public void demonstrateExceptions() {
    // NullPointerException
    String str = null;
    str.length();  // 抛出 NPE

    // ArrayIndexOutOfBoundsException
    int[] arr = new int[3];
    arr[5] = 10;   // 抛出 AIOOBE

    // NumberFormatException
    int num = Integer.parseInt("hello");  // 抛出 NFE

    // ClassCastException
    Object obj = "hello";
    Integer i = (Integer) obj;  // 抛出 CCE
}
```
