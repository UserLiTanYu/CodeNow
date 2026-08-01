# String、Math、Arrays 常用方法速查

日常 Java 开发中，String、Math、Arrays、Objects 是使用频率最高的工具类。本文按功能分类整理常用方法，方便快速查阅。

## String 常用方法速查

String 是不可变对象，所有修改操作都会返回新字符串，原字符串不变。

### 长度与判空

| 方法 | 说明 | 示例 |
|------|------|------|
| `length()` | 返回字符数 | `"hello".length()` → 5 |
| `isEmpty()` | 是否为空串（length == 0） | `"".isEmpty()` → true |
| `isBlank()` | 是否为空白串（Java 11+） | `"  ".isBlank()` → true |
| `charAt(int index)` | 返回指定索引的字符 | `"abc".charAt(1)` → `'b'` |

```java
// 判空最佳实践（Java 11+）
if (str == null || str.isBlank()) {
    System.out.println("空或空白");
}

// Java 8~10
if (str == null || str.trim().isEmpty()) {
    System.out.println("空或空白");
}
```

### 查找

| 方法 | 说明 | 示例 |
|------|------|------|
| `indexOf(String)` | 首次出现的索引，找不到返回 -1 | `"hello".indexOf("ll")` → 2 |
| `indexOf(String, int)` | 从指定位置开始查找 | `"hello".indexOf("l", 3)` → 3 |
| `lastIndexOf(String)` | 最后一次出现的索引 | `"hello".lastIndexOf("l")` → 3 |
| `contains(CharSequence)` | 是否包含子串 | `"hello".contains("ell")` → true |
| `startsWith(String)` | 是否以指定前缀开头 | `"hello".startsWith("he")` → true |
| `endsWith(String)` | 是否以指定后缀结尾 | `"hello".endsWith("lo")` → true |
| `matches(String regex)` | 是否匹配正则表达式 | `"123".matches("\\d+")` → true |

```java
String text = "Hello, World!";

// 查找所有出现位置
int idx = 0;
while ((idx = text.indexOf("l", idx)) != -1) {
    System.out.println("Found at: " + idx);
    idx++;
}
```

### 截取与空白处理

| 方法 | 说明 | 示例 |
|------|------|------|
| `substring(int begin)` | 从 begin 到末尾 | `"hello".substring(2)` → `"llo"` |
| `substring(int begin, int end)` | [begin, end) 区间 | `"hello".substring(1, 3)` → `"el"` |
| `trim()` | 去除首尾 ASCII 空白 | `" hi ".trim()` → `"hi"` |
| `strip()` | 去除首尾空白（Java 11+，支持 Unicode） | `" hi ".strip()` → `"hi"` |
| `stripLeading()` | 去除首部空白（Java 11+） | `" hi ".stripLeading()` → `"hi "` |
| `stripTrailing()` | 去除尾部空白（Java 11+） | `" hi ".stripTrailing()` → `" hi"` |

```java
// strip() vs trim() 的区别
// strip() 能正确处理全角空格等 Unicode 空白字符
String s = "\u3000hello\u3000";  // 全角空格
System.out.println(s.trim().length());   // 7，trim 无法去除全角空格
System.out.println(s.strip().length());  // 5，strip 可以
```

### 大小写转换

| 方法 | 说明 | 示例 |
|------|------|------|
| `toUpperCase()` | 全部转大写 | `"Hello".toUpperCase()` → `"HELLO"` |
| `toLowerCase()` | 全部转小写 | `"Hello".toLowerCase()` → `"hello"` |

### 替换

| 方法 | 说明 | 示例 |
|------|------|------|
| `replace(char, char)` | 替换所有指定字符 | `"hello".replace('l', 'r')` → `"herro"` |
| `replace(CharSequence, CharSequence)` | 替换所有指定子串 | `"hello".replace("ll", "rr")` → `"herro"` |
| `replaceAll(String regex, String replacement)` | 正则替换 | `"a1b2c".replaceAll("\\d", "-")` → `"a-b-c"` |
| `replaceFirst(String regex, String replacement)` | 正则替换首个匹配 | `"a1b2c".replaceFirst("\\d", "-")` → `"a-b2c"` |

```java
// replace vs replaceAll 区别
// replace 是字面量替换，replaceAll 是正则替换
String path = "C:\\Users\\test";
System.out.println(path.replace("\\", "/"));      // "C:/Users/test"
// System.out.println(path.replaceAll("\\", "/")); // 正则错误！需要转义
System.out.println(path.replaceAll("\\\\", "/"));  // "C:/Users/test"
```

### 分割与拼接

| 方法 | 说明 | 示例 |
|------|------|------|
| `split(String regex)` | 按正则分割 | `"a,b,c".split(",")` → `["a","b","c"]` |
| `split(String regex, int limit)` | 限制分割次数 | `"a,b,c,d".split(",", 2)` → `["a","b,c,d"]` |
| `concat(String)` | 拼接字符串 | `"hello".concat(" world")` → `"hello world"` |
| `join(CharSequence, CharSequence...)` | 静态方法，用分隔符拼接 | `String.join("-", "a","b")` → `"a-b"` |
| `join(CharSequence, Iterable)` | 拼接集合元素 | `String.join(",", list)` |

```java
// split 的坑：末尾空字符串会被丢弃
"a,b,c,,,".split(",");       // ["a", "b", "c"]，末尾空串丢失
"a,b,c,,,".split(",", -1);   // ["a", "b", "c", "", "", ""], -1 保留所有

// 拼接大量字符串应使用 StringBuilder
StringBuilder sb = new StringBuilder();
for (String s : list) {
    sb.append(s).append(",");
}
String result = sb.toString();
```

### 转换

| 方法 | 说明 | 示例 |
|------|------|------|
| `toCharArray()` | 转为字符数组 | `"abc".toCharArray()` → `{'a','b','c'}` |
| `getBytes()` | 转为字节数组（平台默认编码） | `"abc".getBytes()` |
| `getBytes(Charset)` | 按指定编码转字节数组 | `"abc".getBytes(StandardCharsets.UTF_8)` |
| `valueOf(int)` | 静态方法，int → String | `String.valueOf(123)` → `"123"` |
| `valueOf(Object)` | 静态方法，对象 → String | `String.valueOf(null)` → `"null"` |
| `Integer.parseInt(String)` | String → int | `Integer.parseInt("123")` → 123 |
| `toString()` | 返回自身 | `"abc".toString()` → `"abc"` |

### 格式化

| 方法 | 说明 | 版本 |
|------|------|------|
| `String.format(String, Object...)` | 格式化字符串 | Java 5+ |
| `formatted(Object...)` | 实例方法格式化 | Java 15+ |

```java
String name = "Alice";
int age = 25;

String s1 = String.format("Name: %s, Age: %d", name, age);
String s2 = "Name: %s, Age: %d".formatted(name, age);  // Java 15+

// 常用格式说明符
String.format("%d", 42);          // 整数 → "42"
String.format("%05d", 42);        // 补零 → "00042"
String.format("%.2f", 3.14159);   // 小数精度 → "3.14"
String.format("%-10s", "hi");     // 左对齐，宽度 10 → "hi        "
String.format("%10s", "hi");      // 右对齐，宽度 10 → "        hi"
String.format("%x", 255);         // 十六进制 → "ff"
String.format("%%");              // 输出 % → "%"
```

## StringBuilder 常用方法

StringBuilder 是可变字符序列，适合频繁拼接字符串的场景（如循环中拼接），性能远优于 `String +` 操作。

| 方法 | 说明 | 示例 |
|------|------|------|
| `append(String)` | 追加内容 | `sb.append("world")` |
| `append(char)` | 追加字符 | `sb.append('!')` |
| `append(int/long/boolean/...)` | 追加各种类型 | `sb.append(42)` |
| `insert(int offset, String)` | 在指定位置插入 | `sb.insert(0, "hello ")` |
| `delete(int start, int end)` | 删除 [start, end) 区间 | `sb.delete(0, 5)` |
| `deleteCharAt(int index)` | 删除指定位置字符 | `sb.deleteCharAt(0)` |
| `replace(int start, int end, String)` | 替换区间内容 | `sb.replace(0, 3, "HE")` |
| `reverse()` | 反转 | `sb.reverse()` |
| `charAt(int index)` | 获取指定位置字符 | `sb.charAt(0)` |
| `setCharAt(int index, char)` | 设置指定位置字符 | `sb.setCharAt(0, 'H')` |
| `length()` | 当前长度 | `sb.length()` |
| `capacity()` | 当前容量（内部数组大小） | `sb.capacity()` |
| `ensureCapacity(int)` | 确保最小容量 | `sb.ensureCapacity(100)` |
| `substring(int start)` | 截取子串（不改变自身） | `sb.substring(2)` |
| `substring(int start, int end)` | 截取子串 | `sb.substring(0, 3)` |
| `toString()` | 转为 String | `sb.toString()` |

```java
// 链式调用
String result = new StringBuilder()
    .append("Hello")
    .append(", ")
    .append("World")
    .append("!")
    .toString();  // "Hello, World!"

// 预分配容量，减少扩容次数
StringBuilder sb = new StringBuilder(1024);
for (int i = 0; i < 1000; i++) {
    sb.append(i).append(",");
}
```

**StringBuffer** 是 StringBuilder 的线程安全版本，方法相同，但所有方法都加了 `synchronized`。单线程场景优先使用 StringBuilder。

## Math 类常用方法速查

Math 类提供数学计算相关的静态方法，构造器私有，不能实例化。

### 基本运算

| 方法 | 说明 | 示例 |
|------|------|------|
| `abs(int/long/float/double)` | 绝对值 | `Math.abs(-5)` → 5 |
| `max(a, b)` | 取较大值 | `Math.max(3, 7)` → 7 |
| `min(a, b)` | 取较小值 | `Math.min(3, 7)` → 3 |
| `pow(double a, double b)` | a 的 b 次方 | `Math.pow(2, 10)` → 1024.0 |
| `sqrt(double)` | 平方根 | `Math.sqrt(16)` → 4.0 |
| `cbrt(double)` | 立方根 | `Math.cbrt(27)` → 3.0 |
| `hypot(double x, double y)` | √(x² + y²) | `Math.hypot(3, 4)` → 5.0 |

### 取整与舍入

| 方法 | 说明 | 示例 |
|------|------|------|
| `ceil(double)` | 向上取整 | `Math.ceil(3.2)` → 4.0 |
| `floor(double)` | 向下取整 | `Math.floor(3.8)` → 3.0 |
| `round(double)` | 四舍五入（返回 long） | `Math.round(3.5)` → 4 |
| `round(float)` | 四舍五入（返回 int） | `Math.round(3.5f)` → 4 |
| `rint(double)` | 最近的整数（同偶） | `Math.rint(2.5)` → 2.0 |

```java
// round 的边界情况
Math.round(3.4);   // 3
Math.round(3.5);   // 4
Math.round(3.6);   // 4
Math.round(-3.5);  // -3（注意不是 -4）
```

### 三角函数

| 方法 | 说明 | 示例 |
|------|------|------|
| `sin(double)` | 正弦（参数为弧度） | `Math.sin(Math.PI / 6)` → 0.5 |
| `cos(double)` | 余弦 | `Math.cos(0)` → 1.0 |
| `tan(double)` | 正切 | `Math.tan(Math.PI / 4)` → 1.0 |
| `atan2(double y, double x)` | 反正切（返回弧度） | `Math.atan2(1, 1)` → π/4 |
| `toDegrees(double)` | 弧度 → 角度 | `Math.toDegrees(Math.PI)` → 180 |
| `toRadians(double)` | 角度 → 弧度 | `Math.toRadians(180)` → π |

### 随机数与常量

| 方法/常量 | 说明 | 示例 |
|-----------|------|------|
| `random()` | [0.0, 1.0) 随机小数 | `Math.random()` |
| `PI` | 圆周率 π | `Math.PI` → 3.141592653589793 |
| `E` | 自然常数 e | `Math.E` → 2.718281828459045 |

```java
// 生成 [min, max] 范围的随机整数
int min = 1, max = 100;
int rand = (int) (Math.random() * (max - min + 1)) + min;

// Java 17+ 推荐使用 RandomGenerator
int rand2 = ThreadLocalRandom.current().nextInt(1, 101);
```

## Arrays 工具类速查

Arrays 类提供数组操作的静态方法，使用前需导入 `java.util.Arrays`。

### 排序

| 方法 | 说明 |
|------|------|
| `sort(int[])` | 基本类型数组排序（双轴快排） |
| `sort(Object[])` | 对象数组排序（TimSort） |
| `sort(T[], Comparator)` | 自定义比较器排序 |
| `sort(int[], int from, int to)` | 部分排序 [from, to) |
| `parallelSort(int[])` | 并行排序（大数据量更快） |

```java
int[] nums = {5, 2, 8, 1, 9};
Arrays.sort(nums);  // [1, 2, 5, 8, 9]

// 自定义排序
String[] names = {"Charlie", "Alice", "Bob"};
Arrays.sort(names);                          // 字典序
Arrays.sort(names, Comparator.reverseOf());  // 逆序
Arrays.sort(names, Comparator.comparingInt(String::length)); // 按长度

// 部分排序
int[] arr = {5, 2, 8, 1, 9};
Arrays.sort(arr, 1, 4);  // [5, 1, 2, 8, 9]（索引 1~3 排序）
```

### 查找

| 方法 | 说明 | 前提 |
|------|------|------|
| `binarySearch(int[], int)` | 二分查找，返回索引 | 数组必须已排序 |
| `binarySearch(Object[], Object)` | 对象数组二分查找 | 必须已排序 |

```java
int[] sorted = {1, 3, 5, 7, 9};
int idx = Arrays.binarySearch(sorted, 5);  // 2
int notFound = Arrays.binarySearch(sorted, 4);  // -3（插入点 -1）
```

### 复制与填充

| 方法 | 说明 | 示例 |
|------|------|------|
| `copyOf(int[], int)` | 复制指定长度（截断或补默认值） | `Arrays.copyOf(a, 5)` |
| `copyOfRange(int[], int, int)` | 复制区间 [from, to) | `Arrays.copyOfRange(a, 1, 3)` |
| `fill(int[], int)` | 全部填充为指定值 | `Arrays.fill(a, 0)` |
| `fill(int[], int, int, int)` | 区间填充 [from, to) | `Arrays.fill(a, 1, 3, -1)` |

```java
int[] a = {1, 2, 3, 4, 5};

int[] b = Arrays.copyOf(a, 3);        // [1, 2, 3]
int[] c = Arrays.copyOf(a, 7);        // [1, 2, 3, 4, 5, 0, 0]
int[] d = Arrays.copyOfRange(a, 1, 4); // [2, 3, 4]
```

### 比较与转换

| 方法 | 说明 |
|------|------|
| `equals(int[], int[])` | 比较两个数组是否相等 |
| `deepEquals(Object[], Object[])` | 深度比较（支持嵌套数组） |
| `asList(T...)` | 数组转为固定大小的 List |
| `toString(int[])` | 数组转为可读字符串 |
| `deepToString(Object[])` | 嵌套数组转字符串 |
| `stream(int[])` | 数组转为 IntStream |

```java
int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
System.out.println(a == b);                // false（引用不同）
System.out.println(Arrays.equals(a, b));   // true（内容相同）

// asList 注意事项
List<String> list = Arrays.asList("a", "b", "c");
list.set(0, "x");    // ✅ 可以修改
// list.add("d");    // ❌ 不支持 add/remove，返回的是固定大小 List

// stream 操作
int sum = Arrays.stream(a).sum();           // 6
int[] filtered = Arrays.stream(a).filter(x -> x > 1).toArray(); // [2, 3]
```

## Objects 工具类速查

Objects 是 Java 7 引入的工具类，提供空安全的对象操作方法。使用前需导入 `java.util.Objects`。

| 方法 | 说明 | 示例 |
|------|------|------|
| `equals(Object, Object)` | 空安全的 equals | `Objects.equals(a, b)`，null 安全 |
| `deepEquals(Object, Object)` | 深度相等（支持数组） | `Objects.deepEquals(arr1, arr2)` |
| `hashCode(Object)` | 空安全的 hashCode | `Objects.hashCode(null)` → 0 |
| `hash(Object...)` | 计算多个对象的哈希值 | `Objects.hash(name, age)` |
| `toString(Object)` | 空安全的 toString | `Objects.toString(null)` → `"null"` |
| `toString(Object, String)` | null 时返回默认值 | `Objects.toString(null, "N/A")` → `"N/A"` |
| `isNull(Object)` | 判断是否为 null | `Objects.isNull(obj)` |
| `nonNull(Object)` | 判断是否非 null | `Objects.nonNull(obj)` |
| `requireNonNull(T)` | 非 null 则返回，否则抛 NPE | `Objects.requireNonNull(param)` |
| `requireNonNull(T, String)` | 带消息的非 null 检查 | `Objects.requireNonNull(p, "p 不能为 null")` |
| `compare(T, T, Comparator)` | 空安全的比较 | `Objects.compare(a, b, comp)` |

```java
// 重写 equals 和 hashCode 的标准写法
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User)) return false;
    User user = (User) o;
    return age == user.age && Objects.equals(name, user.name);
}

@Override
public int hashCode() {
    return Objects.hash(name, age);
}

// 方法参数校验
public void process(String input) {
    Objects.requireNonNull(input, "input 不能为 null");
    // ...
}
```

## 正则表达式基础

Java 中正则表达式通过 `java.util.regex.Pattern` 和 `Matcher` 类实现。

### 常用元字符

| 元字符 | 说明 | 示例 |
|--------|------|------|
| `.` | 任意单个字符（除换行） | `a.c` 匹配 `"abc"`、`"a1c"` |
| `*` | 前一个字符出现 0 次或多次 | `ab*c` 匹配 `"ac"`、`"abbc"` |
| `+` | 前一个字符出现 1 次或多次 | `ab+c` 匹配 `"abc"`、`"abbc"` |
| `?` | 前一个字符出现 0 次或 1 次 | `colou?r` 匹配 `"color"`、`"colour"` |
| `\d` | 数字 [0-9] | `\d+` 匹配 `"123"` |
| `\D` | 非数字 | `\D+` 匹配 `"abc"` |
| `\w` | 单词字符 [a-zA-Z0-9_] | `\w+` 匹配 `"hello_123"` |
| `\W` | 非单词字符 | `\W` 匹配 `"@"` |
| `\s` | 空白字符 | `\s+` 匹配 `"  \t\n"` |
| `\S` | 非空白字符 | `\S+` 匹配 `"hello"` |
| `[]` | 字符类 | `[abc]` 匹配 `"a"`、`"b"` 或 `"c"` |
| `[^]` | 否定字符类 | `[^0-9]` 匹配非数字 |
| `^` | 行首 | `^Hello` 匹配行首的 `"Hello"` |
| `$` | 行尾 | `world$` 匹配行尾的 `"world"` |
| `()` | 分组 | `(ab)+` 匹配 `"abab"` |
| `\|` | 或 | `cat\|dog` 匹配 `"cat"` 或 `"dog"` |
| `{n}` | 恰好 n 次 | `\d{3}` 匹配 3 位数字 |
| `{n,m}` | n 到 m 次 | `\d{2,4}` 匹配 2~4 位数字 |

### Pattern 与 Matcher

```java
import java.util.regex.Pattern;
import java.util.regex.Matcher;

// 方式 1：Pattern + Matcher（推荐，可复用 Pattern）
Pattern p = Pattern.compile("\\d+");
Matcher m = p.matcher("abc123def456");

while (m.find()) {
    System.out.println("Found: " + m.group() + " at " + m.start());
}
// Found: 123 at 3
// Found: 456 at 9

// 方式 2：String.matches()（每次编译正则，性能较差）
boolean match = "123".matches("\\d+");  // true

// 方式 3：预编译常量（最佳实践）
private static final Pattern EMAIL_PATTERN =
    Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$");
```

### matches() vs find() 的区别

| 方法 | 说明 | 匹配范围 |
|------|------|----------|
| `matches()` | 整个字符串完全匹配正则 | 全文匹配 |
| `find()` | 在字符串中查找下一个匹配子串 | 部分匹配 |
| `lookingAt()` | 从开头开始匹配 | 开头匹配 |

```java
Pattern p = Pattern.compile("\\d+");
Matcher m = p.matcher("abc123def");

m.matches();    // false，整个字符串不是纯数字
m.find();       // true，找到 "123"
m.group();      // "123"
```

### 常用正则示例

```java
// 手机号（中国大陆，简化版）
String phone = "13812345678";
boolean isPhone = phone.matches("1[3-9]\\d{9}");

// 邮箱
String email = "user@example.com";
boolean isEmail = email.matches("[\\w.-]+@[\\w.-]+\\.\\w{2,}");

// IP 地址（简化版）
String ip = "192.168.1.1";
boolean isIp = ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

// 提取数字
Pattern nums = Pattern.compile("-?\\d+(\\.\\d+)?");
Matcher m = nums.matcher("温度从 -3.5°C 升到 28°C");
while (m.find()) {
    System.out.println(m.group()); // -3.5, 28
}

// 替换敏感词
String text = "这个商品很垃圾，太差了";
String clean = text.replaceAll("垃圾|差劲|差", "***");
```
