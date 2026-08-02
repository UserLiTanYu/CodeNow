# 基本数据类型与类型转换速查

Java 是强类型语言，每个变量都必须声明类型。掌握 8 种基本数据类型及其转换规则是写出正确代码的基础。

## 基本数据类型一览表

| 类型 | 关键字 | 字节数 | 位数 | 取值范围 | 默认值 | 包装类 |
|------|--------|--------|------|----------|--------|--------|
| 字节型 | `byte` | 1 | 8 | -128 ~ 127 | `0` | `Byte` |
| 短整型 | `short` | 2 | 16 | -32768 ~ 32767 | `0` | `Short` |
| 整型 | `int` | 4 | 32 | -2^31 ~ 2^31-1（约 ±21 亿） | `0` | `Integer` |
| 长整型 | `long` | 8 | 64 | -2^63 ~ 2^63-1 | `0L` | `Long` |
| 单精度浮点 | `float` | 4 | 32 | ±3.4E38，约 6~7 位有效数字 | `0.0f` | `Float` |
| 双精度浮点 | `double` | 8 | 64 | ±1.7E308，约 15~16 位有效数字 | `0.0d` | `Double` |
| 字符型 | `char` | 2 | 16 | 0 ~ 65535（Unicode） | `'\u0000'` | `Character` |
| 布尔型 | `boolean` | — | — | `true` 或 `false` | `false` | `Boolean` |

## 整型：byte、short、int、long

整型用于存储整数值，根据范围选择合适的类型。

- **byte**：1 字节，适合存储小范围整数（如文件字节流、网络协议字段）。
- **short**：2 字节，很少使用，偶尔用于兼容 C 语言数据结构或节省数组内存。
- **int**：4 字节，最常用的整数类型，日常开发优先使用。
- **long**：8 字节，用于表示时间戳、文件大小等超出 int 范围的值。

**关键语法**：long 字面量必须加 `L`（或 `l`）后缀，否则编译器会当作 int 处理导致溢出。

```java
int a = 100;
long b = 100L;           // 正确
long c = 2147483648L;    // 超出 int 范围，必须加 L
// long d = 2147483648;  // 编译错误：整数字面量超出 int 范围
```

其他整数字面量表示法：

```java
int bin = 0b1010;   // 二进制 → 10
int oct = 012;      // 八进制 → 10
int hex = 0xA;      // 十六进制 → 10
int big = 1_000_000; // 下划线分隔，提高可读性（Java 7+）
```

## 浮点型：float、double

浮点型用于存储小数，采用 IEEE 754 标准。

- **float**：4 字节，精度约 6~7 位有效数字，需加 `F` 后缀。
- **double**：8 字节，精度约 15~16 位有效数字，是浮点数的默认类型。

**浮点数精度问题**：浮点数无法精确表示某些十进制小数，这是二进制存储的固有限制。

```java
System.out.println(0.1 + 0.2);          // 0.30000000000000004
System.out.println(0.1 + 0.2 == 0.3);   // false

// 比较浮点数应使用容差
double epsilon = 1e-10;
boolean equal = Math.abs((0.1 + 0.2) - 0.3) < epsilon;
```

**BigDecimal 适用场景**：涉及金额计算、金融数据等对精度要求严格的场景，必须使用 `BigDecimal`。

```java
import java.math.BigDecimal;

BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");
System.out.println(a.add(b));  // 0.3，精确

// 推荐使用 String 构造器，避免 double 构造器引入精度问题
BigDecimal bad = new BigDecimal(0.1);   // 0.1000000000000000055511151231257827021181583404541015625
BigDecimal good = new BigDecimal("0.1"); // 0.1
```

特殊浮点值：

```java
double inf = Double.POSITIVE_INFINITY;   // 正无穷
double ninf = Double.NEGATIVE_INFINITY;  // 负无穷
double nan = Double.NaN;                 // 非数字
System.out.println(Double.NaN == Double.NaN);  // false！用 Double.isNaN() 判断
```

## 字符型：char

`char` 占 2 字节，采用 Unicode 编码，可表示全球大部分字符。

```java
char c1 = 'A';
char c2 = '中';
char c3 = 65;        // 等价于 'A'，char 可直接赋整数
char c4 = '\u0041';  // Unicode 编码表示 'A'
```

**常用转义字符表**：

| 转义序列 | 含义 | 示例 |
|----------|------|------|
| `\n` | 换行 | `"Hello\nWorld"` |
| `\t` | 制表符 | `"Name\tAge"` |
| `\\` | 反斜杠 | `"C:\\Users"` |
| `\'` | 单引号 | `'\''` |
| `\"` | 双引号 | `"He said \"Hi\""` |
| `\r` | 回车 | `"Line1\rLine2"` |
| `\b` | 退格 | `"AB\bC"` → `"AC"` |
| `\0` | 空字符 | `'\0'` |
| `\uXXXX` | Unicode 字符 | `'\u4F60'` → `'你'` |

## 布尔型：boolean

`boolean` 只有两个值：`true` 和 `false`。Java 中 boolean 不能与整数互换（不像 C/C++）。

```java
boolean flag = true;
// int x = 1;
// if (x) {}  // 编译错误！Java 不允许布尔与整数互操作
```

**JVM 实现细节**：JVM 规范没有明确规定 boolean 的底层大小。在 HotSpot JVM 中，单个 boolean 变量通常用 `int`（4 字节）存储，而 boolean 数组则用 `byte` 数组实现（每个元素 1 字节）。

## 自动装箱与拆箱

Java 5 引入自动装箱（Autoboxing）和拆箱（Unboxing），在基本类型和包装类之间自动转换。

```java
// 自动装箱：基本类型 → 包装类
Integer a = 100;        // 编译器自动调用 Integer.valueOf(100)

// 自动拆箱：包装类 → 基本类型
int b = a;              // 编译器自动调用 a.intValue()
```

**缓存池机制**：`Integer.valueOf()` 对 -128 ~ 127 范围内的值会返回缓存对象。

```java
Integer x = 127;
Integer y = 127;
System.out.println(x == y);   // true，同一缓存对象

Integer m = 128;
Integer n = 128;
System.out.println(m == n);   // false，不同对象

// 比较包装类的值应始终使用 equals()
System.out.println(m.equals(n)); // true
```

**常见陷阱**：

```java
// 陷阱 1：拆箱可能抛出 NullPointerException
Integer num = null;
int val = num;  // 运行时 NullPointerException

// 陷阱 2：三元运算符中的隐式拆箱
Integer a = null;
Integer b = 2;
// Integer c = true ? a : b;   // NullPointerException！编译器会将结果拆箱再装箱
Integer c = true ? a : (Integer) b;  // 安全，显式保留 Integer 类型

// 陷阱 3：循环中的自动装箱影响性能
Long sum = 0L;
for (long i = 0; i < 1000000; i++) {
    sum += i;  // 每次循环都拆箱再装箱，应使用 long 而非 Long
}
```

## 类型转换规则

### 隐式转换（Widening，安全）

小范围类型可以自动转换为大范围类型，不丢失精度：

```
byte → short → int → long → float → double
              char → int → long → float → double
```

```java
int i = 100;
long l = i;       // int → long，自动
double d = l;     // long → double，自动
float f = 100L;   // long → float，自动（可能损失精度）
```

### 显式转换（Narrowing，可能丢失数据）

大范围转小范围需要强制转换，使用 `(目标类型)` 语法：

```java
double d = 9.78;
int i = (int) d;      // 截断小数部分，i = 9

int big = 130;
byte b = (byte) big;  // 溢出！130 超出 byte 范围，b = -126

long l = 3000000000L;
int i2 = (int) l;     // 溢出！i2 = -1294967296
```

### 类型转换规则汇总表

| 转换方向 | 是否需要强制转换 | 是否可能丢失数据 | 示例 |
|----------|------------------|------------------|------|
| byte → short | 否 | 否 | 自动提升 |
| byte → int | 否 | 否 | 自动提升 |
| int → long | 否 | 否 | 自动提升 |
| int → double | 否 | 否 | 自动提升 |
| long → float | 否 | 是（大 long 精度丢失） | `123456789L → 1.23456792E8` |
| float → double | 否 | 否 | 自动提升 |
| int → byte | 是 | 是（高位截断） | `(byte) 130 → -126` |
| double → int | 是 | 是（小数截断） | `(int) 9.78 → 9` |
| long → int | 是 | 是（高位截断） | `(int) 3000000000L → -1294967296` |
| char → short | 是 | 否（但语义不同） | `(short) 'A' → 65` |

## 常见数值溢出问题

### 整数溢出

Java 整数运算**不会抛出异常**，溢出后会静默回绕：

```java
int max = Integer.MAX_VALUE;  // 2147483647
System.out.println(max + 1);  // -2147483648，回绕到最小值

int min = Integer.MIN_VALUE;  // -2147483648
System.out.println(min - 1);  // 2147483647，回绕到最大值

// 安全溢出检查（Java 8+）
int safe = Math.addExact(Integer.MAX_VALUE, 1); // 抛出 ArithmeticException
```

### 浮点精度丢失

```java
// 经典案例
System.out.println(0.1 + 0.2);           // 0.30000000000000004
System.out.println(1.0 - 0.9);           // 0.09999999999999998
System.out.println(4.015 * 100);         // 401.49999999999994

// 使用 BigDecimal 避免精度丢失
BigDecimal price = new BigDecimal("4.015");
BigDecimal qty = new BigDecimal("100");
System.out.println(price.multiply(qty));  // 401.500
```

## var 局部变量类型推断（Java 10+）

Java 10 引入 `var` 关键字，允许编译器根据初始化表达式推断局部变量类型，减少冗余代码。

```java
// 之前
Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();

// 使用 var
var map = new HashMap<String, List<Integer>>();

// 其他示例
var list = List.of(1, 2, 3);         // List<Integer>
var name = "Hello";                   // String
var stream = list.stream();           // Stream<Integer>
```

**使用限制**：

```java
// ✅ 可以使用
var x = 10;                    // 局部变量
for (var item : list) {}       // for-each 循环
for (var i = 0; i < 10; i++) {} // for 循环

// ❌ 不能使用
var field = 10;                // 类成员变量
var method = () -> {};         // 方法参数
var arr = {1, 2, 3};           // 数组初始化（需要 new int[]{...}）
var x;                         // 没有初始化
```

**注意**：`var` 只是编译期语法糖，不影响运行时类型。Java 11 还引入了 `var` 在 lambda 参数中的使用（`var x -> x + 1`），可以同时使用注解和类型推断。
