# 字符串（String）详解

字符串是 Java 中最常用的数据类型之一。`String` 类在 `java.lang` 包中，使用频率极高，但其内部机制和使用陷阱也值得深入理解。

## String 的不可变性

Java 中的 `String` 对象是不可变的（immutable）。一旦创建，其内部的字符数组就不能被修改。`String` 类的定义中，存储字符的数组被声明为 `private final char[]`（Java 9+ 改为 `private final byte[]`）。

```java
String s = "Hello";
s.concat(" World");   // 返回一个新的 String 对象，s 本身不变
System.out.println(s); // 仍然输出 "Hello"

String s2 = s.concat(" World");
System.out.println(s2); // 输出 "Hello World"
```

任何看似"修改"字符串的方法，实际上都是返回一个全新的 `String` 对象。

**不可变性的原因：**

1. **安全性**：字符串常用于存储敏感信息（如网络连接的 URL、数据库用户名密码、文件路径）。不可变性确保这些值在传递过程中不会被恶意修改。
2. **缓存哈希值**：`String` 的 `hashCode()` 在第一次计算后会被缓存，因为内容不变所以哈希值不变。这使得 `String` 非常适合作为 `HashMap` 的 key。
3. **线程安全**：不可变对象天然线程安全，无需同步机制，可以在多个线程间安全共享。
4. **字符串常量池优化**：正是因为不可变，多个变量才能安全地共享同一个字符串对象，节省内存。

## 字符串常量池

JVM 为字符串维护了一个特殊的内存区域——字符串常量池（String Pool），位于堆内存中。当使用字面量方式创建字符串时，JVM 会先检查常量池中是否已存在相同内容的字符串：

- 如果存在，直接返回常量池中已有对象的引用
- 如果不存在，在常量池中创建新对象，然后返回引用

```java
String a = "Hello";
String b = "Hello";
System.out.println(a == b);  // true，指向常量池中同一个对象

String c = new String("Hello");
System.out.println(a == c);  // false，c 是堆中新建的对象，不在常量池
System.out.println(a.equals(c)); // true，内容相同
```

**intern() 方法**可以手动将字符串放入常量池，并返回常量池中的引用：

```java
String s1 = new String("Hello");
String s2 = s1.intern();    // 将 "Hello" 放入常量池（已存在则返回已有引用）
String s3 = "Hello";
System.out.println(s2 == s3); // true
```

> 在循环中拼接字符串时，不要使用 `+` 操作符，因为每次 `+` 都会创建新的 `String` 对象，应改用 `StringBuilder`。

## String 常用方法

### 基本信息

```java
String s = "Hello, 码上记";

int len = s.length();          // 返回字符数（注意：不是字节数）
char ch = s.charAt(0);        // 返回索引 0 处的字符 'H'
boolean empty = s.isEmpty();   // 判断是否为空字符串（length() == 0）
```

### 查找与截取

```java
String text = "Hello, World! Hello, Java!";

// indexOf：返回第一次出现的位置，找不到返回 -1
int pos = text.indexOf("Hello");       // 0
int pos2 = text.indexOf("Hello", 1);   // 14（从索引 1 开始找）

// lastIndexOf：返回最后一次出现的位置
int last = text.lastIndexOf("Hello");  // 14

// substring：截取子串（左闭右开）
String sub = text.substring(0, 5);     // "Hello"
String sub2 = text.substring(7);       // "World! Hello, Java!"

// contains：是否包含
boolean has = text.contains("World");  // true

// startsWith / endsWith
boolean starts = text.startsWith("Hello");  // true
boolean ends = text.endsWith("Java!");       // true
```

### 转换与替换

```java
String s = "  Hello, World!  ";

// 大小写转换
String upper = s.toUpperCase();       // "  HELLO, WORLD!  "
String lower = s.toLowerCase();       // "  hello, world!  "

// 去除空白
String trimmed = s.trim();            // "Hello, World!"（去除首尾空白）
String stripped = s.strip();          // "Hello, World!"（Java 11+，支持 Unicode 空白）
String stripL = s.stripLeading();     // "Hello, World!  "
String stripT = s.stripTrailing();    // "  Hello, World!"

// 替换
String replaced = s.trim().replace("World", "Java");   // "Hello, Java!"
String replacedAll = s.trim().replaceAll("\\d+", "");   // 支持正则表达式
```

### 拆分与连接

```java
// split：按分隔符拆分
String csv = "apple,banana,cherry";
String[] fruits = csv.split(",");     // ["apple", "banana", "cherry"]

String data = "one::two::three";
String[] parts = data.split("::");    // ["one", "two", "three"]

// join：连接字符串
String joined = String.join(" - ", "A", "B", "C");  // "A - B - C"
String joined2 = String.join(", ", List.of("a", "b", "c"));  // "a, b, c"
```

### 格式化（Java 15+）

```java
// formatted() 方法（Java 15+）
String msg = "Hello, %s! You have %d messages.".formatted("Alice", 5);
// "Hello, Alice! You have 5 messages."

// String.format()（所有版本通用）
String msg2 = String.format("Name: %s, Age: %d", "Bob", 25);
```

## 字符串比较

字符串比较是初学者最容易踩坑的地方。

### == vs equals()

- `==` 比较的是引用（内存地址），判断两个变量是否指向同一个对象
- `equals()` 比较的是内容是否相同

```java
String s1 = "Hello";
String s2 = "Hello";
String s3 = new String("Hello");

s1 == s2          // true（常量池中同一对象）
s1 == s3          // false（s3 是堆中新建对象）
s1.equals(s3)     // true（内容相同）
```

**经验法则：永远使用 `equals()` 比较字符串内容。**

### compareTo()

`compareTo()` 按字典顺序比较两个字符串，返回值：

- 负数：调用者排在参数之前
- 0：两个字符串相等
- 正数：调用者排在参数之后

```java
int result = "apple".compareTo("banana");  // 负数（'a' < 'b'）
int result2 = "abc".compareTo("abd");      // 负数（'c' < 'd'）
int result3 = "abc".compareTo("abc");      // 0
```

`compareToIgnoreCase()` 忽略大小写进行比较。

## StringBuilder 与 StringBuffer

由于 `String` 不可变，频繁拼接字符串时会产生大量中间对象，影响性能。Java 提供了两个可变的字符序列类：

### StringBuilder

`StringBuilder` 内部维护一个可变的字符数组，适用于单线程环境下的字符串拼接：

```java
StringBuilder sb = new StringBuilder();
sb.append("Hello");
sb.append(", ");
sb.append("World");
sb.append('!');
String result = sb.toString();  // "Hello, World!"
```

常用方法：

| 方法 | 说明 |
|------|------|
| `append(x)` | 在末尾追加（支持各种类型） |
| `insert(offset, x)` | 在指定位置插入 |
| `delete(start, end)` | 删除 [start, end) 范围的字符 |
| `replace(start, end, str)` | 替换指定范围的字符串 |
| `reverse()` | 反转字符序列 |
| `charAt(index)` / `setCharAt(index, ch)` | 获取/设置指定位置字符 |
| `length()` | 当前长度 |
| `capacity()` | 当前容量 |
| `toString()` | 转换为 `String` |

链式调用示例：

```java
String html = new StringBuilder()
    .append("<div>")
    .append("<h1>").append(title).append("</h1>")
    .append("<p>").append(content).append("</p>")
    .append("</div>")
    .toString();
```

### StringBuffer

`StringBuffer` 的 API 与 `StringBuilder` 完全相同，但它的方法是同步的（线程安全）。在多线程环境下共享可变字符串时应使用 `StringBuffer`，但性能较低。

**选择建议**：单线程场景用 `StringBuilder`，多线程场景用 `StringBuffer`。实际开发中绝大多数情况使用 `StringBuilder`。

> 在现代 Java 编译器中，简单的 `String` 拼接（如 `"a" + "b" + "c"`）会自动优化为 `StringBuilder`，但在循环中拼接时编译器无法优化，需要手动使用 `StringBuilder`。

## 文本块（Text Blocks）

Java 13 引入了文本块（预览特性），Java 15 正式可用。文本块使用三个双引号 `"""` 定义，可以方便地编写多行字符串：

```java
String json = """
        {
            "name": "码上记",
            "version": "1.0",
            "description": "个人技术博客"
        }
        """;
```

**缩进处理规则**：编译器会自动去除"公共前导空白"（即所有行共同具有的前导空格）。结束的 `"""` 的位置决定了去除多少空白：

```java
String text1 = """
        Hello
        World
        """;
// 实际内容："Hello\nWorld\n"（去除 8 个前导空格）

String text2 = """
            Hello
            World
        """;
// 实际内容："    Hello\n    World\n"（结束 """ 前有 4 个空格，所以保留 4 个空格）
```

文本块特别适合编写 SQL、JSON、HTML、XML 等多行文本：

```java
String sql = """
        SELECT u.id, u.name, u.email
        FROM users u
        WHERE u.status = 'ACTIVE'
          AND u.created_at > ?
        ORDER BY u.name
        """;
```

## 字符编码基础

### ASCII、Unicode 与 UTF-8

**ASCII** 使用 7 位（0~127）编码英文字母、数字和常见符号，无法表示中文等字符。

**Unicode** 为世界上每个字符分配一个唯一的码点（code point），范围从 U+0000 到 U+10FFFF。其中 U+0000 到 U+FFFF 为基本多文种平面（BMP），U+10000 到 U+10FFFF 为补充平面（如 emoji 表情）。

**UTF-8** 是一种变长编码方案：

- 1 字节：U+0000 ~ U+007F（兼容 ASCII）
- 2 字节：U+0080 ~ U+07FF
- 3 字节：U+0800 ~ U+FFFF（大部分中文在此范围）
- 4 字节：U+10000 ~ U+10FFFF（emoji 等）

### Java 中的 char 与 codePoint

Java 的 `char` 类型采用 UTF-16 编码，占 2 字节。BMP 中的字符可以用一个 `char` 表示，但补充平面的字符（如 emoji）需要两个 `char`（代理对，surrogate pair）：

```java
String emoji = "😀";
System.out.println(emoji.length());      // 2（两个 char）
System.out.println(emoji.codePointCount(0, emoji.length())); // 1（一个码点）
System.out.println(emoji.charAt(0));     // '\uD83D'（高代理项）
System.out.println(emoji.codePointAt(0)); // 128512（U+1F600）
```

在处理包含 emoji 或特殊字符的文本时，应使用码点（code point）相关方法而非 `charAt()`：

```java
String text = "Hello 😀 World";

// 正确遍历（支持补充字符）
text.codePoints().forEach(cp -> {
    if (Character.isBmpCodePoint(cp)) {
        System.out.print((char) cp);
    } else {
        System.out.print(new String(Character.toChars(cp)));
    }
});
```

`String` 类提供了码点相关的方法：

| 方法 | 说明 |
|------|------|
| `codePointAt(index)` | 返回指定位置的码点 |
| `codePointBefore(index)` | 返回指定位置之前的码点 |
| `codePointCount(begin, end)` | 返回范围内的码点数量 |
| `offsetByCodePoints(index, codePointOffset)` | 按码点偏移索引 |

理解字符串的内部机制——不可变性、常量池、编码方式——能帮助你写出更高效、更安全的 Java 代码。在日常开发中，选择合适的字符串操作方式（`String` 拼接 vs `StringBuilder`）和正确的比较方法（`equals()` vs `==`）是最基本也最重要的实践。