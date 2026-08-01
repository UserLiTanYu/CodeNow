# 大数值运算与格式化输出

在日常开发中，我们经常需要处理超出基本数据类型范围的数值，或者对数字进行精确的格式化输出。Java 提供了 `BigInteger`、`BigDecimal` 用于高精度计算，以及 `printf`、`String.format()`、`DecimalFormat` 等工具用于格式化输出。本文将系统讲解这些内容。

## BigInteger：任意精度整数

当 `long` 类型（最大值约 9.2×10¹⁸）仍不够用时，`BigInteger` 可以表示任意大小的整数。它位于 `java.math` 包中，是**不可变**对象——每次运算都会返回一个新的 `BigInteger` 实例。

### 创建 BigInteger

```java
// 方式一：通过字符串构造
BigInteger a = new BigInteger("123456789012345678901234567890");

// 方式二：通过 valueOf（推荐，可缓存小数值）
BigInteger b = BigInteger.valueOf(1000L);

// 方式三：从字节数组
BigInteger c = new BigInteger(new byte[]{0x01, 0x00, 0x00});
```

### 常用方法

| 方法 | 说明 | 示例 |
|------|------|------|
| `add(BigInteger)` | 加法 | `a.add(b)` |
| `subtract(BigInteger)` | 减法 | `a.subtract(b)` |
| `multiply(BigInteger)` | 乘法 | `a.multiply(b)` |
| `divide(BigInteger)` | 整除 | `a.divide(b)` |
| `mod(BigInteger)` | 取模 | `a.mod(b)` |
| `pow(int)` | 幂运算 | `a.pow(10)` |
| `gcd(BigInteger)` | 最大公约数 | `a.gcd(b)` |
| `compareTo(BigInteger)` | 比较大小 | `a.compareTo(b)` |
| `abs()` | 绝对值 | `a.abs()` |
| `negate()` | 取反 | `a.negate()` |

```java
BigInteger factorial = BigInteger.ONE;
for (int i = 1; i <= 50; i++) {
    factorial = factorial.multiply(BigInteger.valueOf(i));
}
System.out.println("50! = " + factorial);
// 输出：50! = 30414093201713378043612608166979581188299763898377856820553615673507270386838265...
```

`BigInteger` 没有提供 `+`、`-`、`*`、`/` 运算符重载（Java 不支持运算符重载），必须调用方法。

## BigDecimal：任意精度浮点数

`BigDecimal` 是浮点数的精确表示，常用于金融、财务等对精度要求极高的场景。

### 为什么浮点运算有精度问题

先看一个经典问题：

```java
System.out.println(0.1 + 0.2);  // 输出：0.30000000000000004
System.out.println(0.1 + 0.2 == 0.3);  // 输出：false
```

原因在于 IEEE 754 双精度浮点数的二进制表示：`0.1`（十进制）转换为二进制是 `0.0001100110011...`（无限循环），无法精确存储。十进制小数中能精确表示的只有分母为 2 的幂次的分数（如 0.5、0.25、0.75），而 0.1、0.2、0.3 都不能精确表示。

| 十进制 | 二进制表示 | 是否精确 |
|--------|-----------|----------|
| 0.5 | 0.1 | 精确 |
| 0.25 | 0.01 | 精确 |
| 0.1 | 0.000110011...（循环） | 不精确 |
| 0.2 | 0.00110011...（循环） | 不精确 |

### 创建 BigDecimal

```java
// 方式一：通过字符串（推荐，精确）
BigDecimal a = new BigDecimal("0.1");

// 方式二：通过 valueOf（推荐，内部调用 Double.toString）
BigDecimal b = BigDecimal.valueOf(0.1);

// 方式三：直接 double 构造（不推荐！）
BigDecimal c = new BigDecimal(0.1);  // 实际值：0.1000000000000000055511151231257827021181583404541015625
```

`new BigDecimal(double)` 会保留浮点数的全部精度，通常不是我们想要的结果。务必使用字符串构造器或 `valueOf` 方法。

### 精度控制与四则运算

```java
BigDecimal a = new BigDecimal("10");
BigDecimal b = new BigDecimal("3");

// 四则运算
BigDecimal sum = a.add(b);           // 13
BigDecimal diff = a.subtract(b);     // 7
BigDecimal product = a.multiply(b);  // 30
BigDecimal quotient = a.divide(b, 10, RoundingMode.HALF_UP);  // 3.3333333333
```

`divide` 方法的重载形式：

| 方法签名 | 说明 |
|----------|------|
| `divide(BigDecimal)` | 除不尽时抛出 `ArithmeticException` |
| `divide(BigDecimal, int scale, RoundingMode)` | 指定小数位数和舍入模式 |
| `divide(BigDecimal, MathContext)` | 通过上下文控制精度 |

常用舍入模式：

| 模式 | 说明 |
|------|------|
| `HALF_UP` | 四舍五入（最常用） |
| `HALF_DOWN` | 五舍六入 |
| `UP` | 远离零方向进位 |
| `DOWN` | 截断（向零方向） |
| `CEILING` | 向正无穷方向 |
| `FLOOR` | 向负无穷方向 |

### 金融计算必须用 BigDecimal

```java
// 错误示范：用 double 计算金额
double price = 19.99;
double quantity = 3;
double total = price * quantity;
System.out.println(total);  // 59.970000000000006

// 正确做法：用 BigDecimal
BigDecimal price2 = new BigDecimal("19.99");
BigDecimal quantity2 = BigDecimal.valueOf(3);
BigDecimal total2 = price2.multiply(quantity2);
System.out.println(total2);  // 59.97
```

金融系统中，每一分钱都必须准确。使用 `double` 累加大量交易会产生误差累积，最终导致账目不平。

## printf 格式化输出

Java 的 `printf` 方法借鉴了 C 语言的 `printf`，通过格式说明符控制输出格式。

### 基本格式说明符

| 说明符 | 说明 | 示例 |
|--------|------|------|
| `%d` | 十进制整数 | `printf("%d", 42)` → `42` |
| `%f` | 浮点数 | `printf("%f", 3.14)` → `3.140000` |
| `%s` | 字符串 | `printf("%s", "hello")` → `hello` |
| `%n` | 平台相关的换行符 | 跨平台换行 |
| `%c` | 字符 | `printf("%c", 'A')` → `A` |
| `%b` | 布尔值 | `printf("%b", true)` → `true` |
| `%x` | 十六进制整数 | `printf("%x", 255)` → `ff` |
| `%o` | 八进制整数 | `printf("%o", 8)` → `10` |
| `%%` | 字面量 `%` | `printf("%%")` → `%` |

### 宽度与精度控制

```java
// 宽度控制：至少占 10 个字符宽度，右对齐
System.out.printf("[%10d]%n", 123);     // [       123]

// 左对齐
System.out.printf("[%-10d]%n", 123);    // [123       ]

// 零填充
System.out.printf("[%06d]%n", 123);     // [000123]

// 浮点数精度：保留 2 位小数
System.out.printf("%.2f%n", 3.14159);   // 3.14

// 宽度 + 精度：总宽 10，保留 2 位小数
System.out.printf("[%10.2f]%n", 3.14);  // [      3.14]

// 字符串截断
System.out.printf("%.3s%n", "Hello");   // Hel
```

### 日期时间格式化

```java
LocalDateTime now = LocalDateTime.now();

// %tF = 日期（YYYY-MM-DD）
System.out.printf("%tF%n", now);   // 2026-08-01

// %tT = 时间（HH:MM:SS）
System.out.printf("%tT%n", now);   // 14:30:25

// %tY = 四位年份，%tm = 月份，%td = 日
System.out.printf("%tY年%tm月%td日%n", now, now, now);  // 2026年08月01日

// %tH = 小时（24小时制），%tM = 分钟，%tS = 秒
System.out.printf("%tH:%tM:%tS%n", now, now, now);  // 14:30:25
```

## String.format() 与 DecimalFormat

### String.format()

`String.format()` 的格式规则与 `printf` 完全相同，区别在于它返回格式化后的字符串而非直接输出。

```java
String formatted = String.format("姓名：%s，年龄：%d，成绩：%.1f", "张三", 20, 95.5);
// 输出：姓名：张三，年龄：20，成绩：95.5

// 常用于日志、异常消息
throw new IllegalArgumentException(String.format("无效的参数值：%s，范围应在 %d 到 %d 之间", value, min, max));
```

### DecimalFormat

`java.text.DecimalFormat` 提供更灵活的数字格式化，通过模式字符串定义格式。

```java
import java.text.DecimalFormat;

DecimalFormat df1 = new DecimalFormat("#,###.##");
System.out.println(df1.format(1234567.89));  // 1,234,567.89

DecimalFormat df2 = new DecimalFormat("#,###.00");
System.out.println(df2.format(1234567.8));   // 1,234,567.80（不足补零）

DecimalFormat df3 = new DecimalFormat("0.00%");
System.out.println(df3.format(0.856));        // 85.60%

DecimalFormat df4 = new DecimalFormat("¥#,###.00");
System.out.println(df4.format(9999.5));       // ¥9,999.50
```

模式符号说明：

| 符号 | 说明 |
|------|------|
| `#` | 数字，不存在则不显示 |
| `0` | 数字，不存在则补零 |
| `.` | 小数点分隔符 |
| `,` | 分组分隔符（千位分隔） |
| `%` | 乘以 100 并显示百分号 |
| `¤` | 货币符号（区域相关） |

## NumberFormat：货币与百分比格式化

`java.text.NumberFormat` 是 `DecimalFormat` 的父类，提供了针对货币、百分比等场景的专用格式化器。

```java
import java.text.NumberFormat;
import java.util.Locale;

// 货币格式化（中国）
NumberFormat currencyCN = NumberFormat.getCurrencyInstance(Locale.CHINA);
System.out.println(currencyCN.format(12345.67));  // ¥12,345.67

// 货币格式化（美国）
NumberFormat currencyUS = NumberFormat.getCurrencyInstance(Locale.US);
System.out.println(currencyUS.format(12345.67));  // $12,345.67

// 百分比格式化
NumberFormat percent = NumberFormat.getPercentInstance();
percent.setMinimumFractionDigits(1);
System.out.println(percent.format(0.8567));  // 85.7%

// 整数格式化（带分组）
NumberFormat integer = NumberFormat.getIntegerInstance();
System.out.println(integer.format(1234567));  // 1,234,567
```

在国际化应用中，`NumberFormat` 会根据 `Locale` 自动处理数字分隔符、货币符号等差异，比手动拼接字符串更可靠。

## 选择建议

| 场景 | 推荐方案 |
|------|----------|
| 金融计算 | `BigDecimal` |
| 超大整数（密码学、阶乘） | `BigInteger` |
| 简单格式化输出 | `printf` / `String.format()` |
| 复杂数字模式 | `DecimalFormat` |
| 国际化货币/百分比 | `NumberFormat` |
