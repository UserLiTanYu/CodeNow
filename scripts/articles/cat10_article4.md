# 正则表达式与常见异常速查

正则表达式是文本处理的瑞士军刀，Java 通过 `java.util.regex` 包提供了完整支持。本文先梳理正则核心语法与 `Pattern`/`Matcher` 用法，再以速查表形式汇总日常开发中最常遇到的异常类型及其应对策略。

---

## 正则表达式基础

### Pattern 和 Matcher 类

Java 中正则表达式的使用围绕两个核心类展开：

- **`Pattern`**：编译后的正则表达式对象，不可变且线程安全。
- **`Matcher`**：对输入字符串执行匹配操作的引擎，有状态、非线程安全。

典型用法：

```java
import java.util.regex.Pattern;
import java.util.regex.Matcher;

Pattern p = Pattern.compile("\\d{3}-\\d{8}");
Matcher m = p.matcher("电话：010-12345678");
if (m.find()) {
    System.out.println(m.group()); // 010-12345678
}
```

`Pattern.compile()` 接受第二个 `int flags` 参数来开启模式标志：

```java
Pattern p = Pattern.compile("hello", Pattern.CASE_INSENSITIVE);
```

常用标志：

| 标志 | 简写（正则内联） | 作用 |
|------|------------------|------|
| `CASE_INSENSITIVE` | `(?i)` | 忽略大小写（仅 ASCII） |
| `MULTILINE` | `(?m)` | `^` 和 `$` 匹配每一行 |
| `DOTALL` | `(?s)` | `.` 匹配任意字符含换行 |
| `UNICODE_CASE` | `(?iu)` | Unicode 忽略大小写 |
| `COMMENTS` | `(?x)` | 允许空白和注释 |

### matches() vs find() vs lookingAt()

三者都返回 `boolean`，但匹配策略不同：

| 方法 | 匹配范围 | 起始位置 | 典型场景 |
|------|----------|----------|----------|
| `matches()` | **整个**输入串 | 固定从头到尾 | 验证格式（如邮箱校验） |
| `find()` | 任意**子串** | 从上次匹配结束之后 | 搜索所有匹配项 |
| `lookingAt()` | 从**开头**的子串 | 固定从头 | 检查前缀 |

```java
Pattern p = Pattern.compile("\\d+");
Matcher m = p.matcher("abc123def456");

m.matches();     // false — "abc123def456" 不全是数字
m.lookingAt();   // false — 开头是 "abc" 不是数字
m.find();        // true  — 找到 "123"
m.find();        // true  — 找到 "456"
m.find();        // false — 没有更多匹配
```

### 常用元字符速查表

#### 字符类

| 元字符 | 含义 | 示例 |
|--------|------|------|
| `.` | 任意字符（默认不含换行） | `a.c` 匹配 `abc`、`a1c` |
| `\d` | 数字 `[0-9]` | `\d+` 匹配 `123` |
| `\D` | 非数字 `[^0-9]` | `\D+` 匹配 `abc` |
| `\w` | 单词字符 `[a-zA-Z0-9_]` | `\w+` 匹配 `hello_123` |
| `\W` | 非单词字符 | `\W` 匹配 `@`、空格 |
| `\s` | 空白字符 `[ \t\n\r\f]` | `\s+` 匹配一个或多个空白 |
| `\S` | 非空白字符 | `\S+` 匹配连续非空白串 |

#### 量词

| 量词 | 含义 | 示例 |
|------|------|------|
| `*` | 零次或多次 | `ab*c` 匹配 `ac`、`abc`、`abbc` |
| `+` | 一次或多次 | `ab+c` 匹配 `abc`、`abbc`，不匹配 `ac` |
| `?` | 零次或一次 | `colou?r` 匹配 `color`、`colour` |
| `{n}` | 恰好 n 次 | `\d{3}` 匹配 `123` |
| `{n,}` | 至少 n 次 | `\d{2,}` 匹配 `12`、`12345` |
| `{n,m}` | n 到 m 次 | `\d{2,4}` 匹配 `12`、`1234` |

#### 位置锚点

| 元字符 | 含义 |
|--------|------|
| `^` | 输入开头（`MULTILINE` 模式下为行首） |
| `$` | 输入结尾（`MULTILINE` 模式下为行尾） |
| `\b` | 单词边界 |
| `\B` | 非单词边界 |

#### 分组与引用

| 语法 | 含义 | 示例 |
|------|------|------|
| `(pattern)` | 捕获分组 | `(ab)+` 捕获 `ab` |
| `(?:pattern)` | 非捕获分组 | `(?:ab)+` 不捕获，仅分组 |
| `\1` | 向后引用第 1 个捕获组 | `(['"]).*?\1` 匹配成对引号 |
| `(?<name>pattern)` | 命名捕获组 | `(?<year>\d{4})` 用 `group("year")` 获取 |

```java
Pattern p = Pattern.compile("(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})");
Matcher m = p.matcher("2026-08-01");
if (m.matches()) {
    System.out.println(m.group("year"));  // 2026
    System.out.println(m.group("month")); // 08
}
```

#### 零宽断言（预查）

| 语法 | 名称 | 含义 |
|------|------|------|
| `(?=pattern)` | 正向肯定预查 | 右边是 pattern |
| `(?!pattern)` | 正向否定预查 | 右边不是 pattern |
| `(?<=pattern)` | 反向肯定预查 | 左边是 pattern |
| `(?<!pattern)` | 反向否定预查 | 左边不是 pattern |

```java
// 匹配后面跟着 "元" 的数字
Pattern p = Pattern.compile("\\d+(?=元)");
Matcher m = p.matcher("价格100元");
m.find();
System.out.println(m.group()); // 100
```

#### 字符类

| 语法 | 含义 |
|------|------|
| `[abc]` | 匹配 a、b 或 c |
| `[^abc]` | 不匹配 a、b、c |
| `[a-z]` | 匹配 a 到 z |
| `[a-zA-Z0-9]` | 匹配字母和数字 |
| `[a-z&&[^bc]]` | a 到 z 中除去 b、c（交集语法） |

### String 的正则方法

`String` 类内置了四个正则相关方法，无需手动创建 `Pattern`/`Matcher`：

```java
String s = "Hello 123 World 456";

// 整体匹配
boolean ok = s.matches("\\w+\\s\\d+.*"); // true

// 替换所有匹配
String r1 = s.replaceAll("\\d+", "***"); // "Hello *** World ***"

// 替换第一个匹配
String r2 = s.replaceFirst("\\d+", "***"); // "Hello *** World 456"

// 按正则分割
String[] parts = "one,,two,,three".split(",+"); // ["one", "two", "three"]
```

> **性能提示**：如果正则会被多次使用，应提前编译为 `Pattern` 对象，避免 `String.matches()` 等方法每次调用都重新编译。

### 非贪婪量词

默认量词是**贪婪**的——尽可能多地匹配。在量词后加 `?` 变为**非贪婪**——尽可能少地匹配。

| 量词 | 类型 | 含义 |
|------|------|------|
| `*` | 贪婪 | 尽可能多 |
| `*?` | 非贪婪 | 尽可能少 |
| `+` | 贪婪 | 尽可能多 |
| `+?` | 非贪婪 | 尽可能少 |
| `?` | 贪婪 | 尽可能多（0 或 1 次取 1） |
| `??` | 非贪婪 | 尽可能少（0 或 1 次取 0） |
| `{n,m}` | 贪婪 | 尽可能多（最多 m 次） |
| `{n,m}?` | 非贪婪 | 尽可能少（至少 n 次） |

```java
String html = "<b>bold</b> and <i>italic</i>";

// 贪婪：匹配 "<b>bold</b> and <i>italic</i>"
Pattern greedy = Pattern.compile("<.+>");

// 非贪婪：匹配 "<b>"、"</b>"、"<i>"、"</i>"
Pattern lazy = Pattern.compile("<.+?>");
```

### 常用正则示例

以下正则适用于一般性校验场景，生产环境建议结合业务规则做更严格的长度与边界检查：

```java
// 邮箱（简化版）
String EMAIL = "^[\\w.-]+@[\\w-]+(\\.[\\w-]+)+$";

// 中国大陆手机号（1 开头 11 位）
String PHONE = "^1[3-9]\\d{9}$";

// IPv4 地址
String IPV4 = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";

// URL（简化版）
String URL = "^https?://[\\w.-]+(:\\d+)?(/[\\w./?%&=-]*)?$";

// 中文字符（至少一个）
String CHINESE = "^[\\u4e00-\\u9fa5]+$";
```

---

## Java 常见异常速查表

### RuntimeException 子类（非受检异常）

非受检异常不需要在方法签名中声明，通常由编程错误引起。

| 异常名 | 所属包 | 产生原因 | 解决方案 |
|--------|--------|----------|----------|
| `NullPointerException` | `java.lang` | 对 `null` 引用调用方法或访问字段 | 使用 `Optional`、`Objects.requireNonNull()` 做防御；调用前判空 |
| `ArrayIndexOutOfBoundsException` | `java.lang` | 数组下标越界 | 检查 `array.length`；使用增强 for 循环 |
| `StringIndexOutOfBoundsException` | `java.lang` | 字符串下标越界 | 检查 `str.length()`；使用前判空 |
| `ClassCastException` | `java.lang` | 类型转换不兼容 | 转换前先用 `instanceof` 检查 |
| `NumberFormatException` | `java.lang` | 数字格式解析失败，如 `Integer.parseInt("abc")` | 校验输入格式；使用 `Integer.valueOf()` 并捕获异常 |
| `IllegalArgumentException` | `java.lang` | 方法收到不合法参数 | 在方法入口做参数校验，提前抛出 |
| `IllegalStateException` | `java.lang` | 对象状态不允许当前操作 | 检查前置条件是否满足 |
| `UnsupportedOperationException` | `java.lang` | 调用了不支持的操作（如不可变集合的 `add`） | 使用可变集合包装；确认集合是否可修改 |
| `ArithmeticException` | `java.lang` | 算术异常，典型如除以零 | 除法前检查除数；使用 `BigDecimal` 做精确计算 |
| `ConcurrentModificationException` | `java.util` | 迭代集合时结构性修改 | 使用 `Iterator.remove()`；使用并发集合（如 `CopyOnWriteArrayList`） |
| `StackOverflowError` | `java.lang` | 递归调用过深，栈空间耗尽 | 检查递归终止条件；改用迭代实现 |
| `OutOfMemoryError` | `java.lang` | 堆内存不足 | 增大 `-Xmx`；检查内存泄漏；使用弱引用/软引用 |

#### NullPointerException 防御示例

```java
// 传统写法
if (user != null && user.getAddress() != null) {
    String city = user.getAddress().getCity();
}

// Optional 写法
String city = Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("未知");

// Objects.requireNonNull — 快速失败
public void setName(String name) {
    this.name = Objects.requireNonNull(name, "name 不能为 null");
}
```

#### ConcurrentModificationException 示例

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c"));

// 错误写法 — 抛出 ConcurrentModificationException
for (String s : list) {
    if ("b".equals(s)) list.remove(s);
}

// 正确写法一 — 使用 Iterator
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if ("b".equals(it.next())) it.remove();
}

// 正确写法二 — 使用 removeIf（Java 8+）
list.removeIf("b"::equals);
```

### 受检异常

受检异常必须在方法签名中声明或捕获，代表可预期的外部错误。

| 异常名 | 所属包 | 产生原因 | 解决方案 |
|--------|--------|----------|----------|
| `IOException` | `java.io` | 通用 IO 错误 | 检查文件/流是否可用；使用 try-with-resources 自动关闭 |
| `FileNotFoundException` | `java.io` | 文件不存在或无法打开 | 检查文件路径；使用 `Files.exists()` 预判 |
| `SQLException` | `java.sql` | 数据库访问错误 | 检查连接/SQL 语法；使用连接池管理资源 |
| `ClassNotFoundException` | `java.lang` | 动态加载类时未找到 | 检查类路径（classpath）；确认依赖已引入 |
| `NoSuchMethodException` | `java.lang.reflect` | 反射获取方法时不存在 | 检查方法名和参数签名 |
| `NoSuchFieldException` | `java.lang.reflect` | 反射获取字段时不存在 | 检查字段名；注意访问修饰符 |
| `ParseException` | `java.text` | 日期/数字解析格式不匹配 | 校验输入格式；使用 `DateTimeFormatter`（线程安全） |
| `MalformedURLException` | `java.net` | URL 格式不合法 | 使用 `URI.create()` 先校验 |

### 异常处理决策流程

在编写代码时，面对异常的处理可以参考以下决策路径：

```
异常发生
  │
  ├─ 能否在当前方法中合理恢复？
  │    ├─ 能 → try-catch 捕获，执行恢复逻辑
  │    └─ 不能 → 传播给调用者
  │
  ├─ 是编程错误（bug）吗？
  │    ├─ 是 → 抛出 RuntimeException / 子类，快速失败
  │    └─ 否 → 声明受检异常，让调用者决定
  │
  └─ 异常选择指南：
       ├─ 参数非法 → IllegalArgumentException
       ├─ 对象状态非法 → IllegalStateException
       ├─ 空值 → NullPointerException 或 Objects.requireNonNull
       ├─ 未实现 → UnsupportedOperationException
       └─ 需要调用者处理 → 受检异常（自定义或已有）
```

### 自定义异常的建议

当内置异常无法精确描述业务错误时，可自定义异常：

```java
// 受检异常 — 调用者必须处理
public class OrderNotFoundException extends Exception {
    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super("订单不存在: " + orderId);
        this.orderId = orderId;
    }

    public Long getOrderId() { return orderId; }
}

// 非受检异常 — 编程错误，快速失败
public class InvalidConfigException extends RuntimeException {
    public InvalidConfigException(String message) {
        super(message);
    }
}
```

命名惯例：异常类名以 `Exception` 结尾；若继承 `Error` 则表示不可恢复的严重错误（一般不自定义 `Error`）。
