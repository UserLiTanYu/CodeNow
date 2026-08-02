# 数据类型、变量与运算符

Java 是一门强类型语言，每个变量在使用前都必须声明其类型。理解数据类型、变量和运算符是编写任何 Java 程序的基础。本文将系统梳理这些核心概念。

## 基本数据类型

Java 提供了 8 种基本数据类型（primitive types），分为四大类：

| 类型 | 字节数 | 取值范围 | 默认值 |
|------|--------|---------|--------|
| `byte` | 1 | -128 ~ 127 | 0 |
| `short` | 2 | -32768 ~ 32767 | 0 |
| `int` | 4 | -2^31 ~ 2^31-1（约 ±21 亿） | 0 |
| `long` | 8 | -2^63 ~ 2^63-1 | 0L |
| `float` | 4 | 约 ±3.4×10^38，6~7 位有效数字 | 0.0f |
| `double` | 8 | 约 ±1.7×10^308，15~16 位有效数字 | 0.0d |
| `char` | 2 | 0 ~ 65535（Unicode 字符） | '\u0000' |
| `boolean` | 1* | true / false | false |

> *boolean 的大小在 JVM 规范中没有严格定义，实际取决于虚拟机实现。

**整数类型**的选择建议：日常开发优先使用 `int`，需要处理超大整数时使用 `long`。`byte` 和 `short` 主要用于节省内存的特殊场景（如处理二进制数据或大型数组）。

**浮点类型**中，`double` 的精度足以满足大多数计算需求，是默认选择。`float` 适用于对精度要求不高但需要节省空间的场景（如图形处理中的坐标）。注意：浮点数不适合做精确的金融计算，应使用 `BigDecimal`。

**char** 类型占用 2 字节，采用 UTF-16 编码，可以表示一个基本多文种平面（BMP）中的 Unicode 字符。

```java
char letter = 'A';
char chinese = '中';
char unicode = '\u0041'; // 等同于 'A'
```

## 引用类型

除了 8 种基本类型外，其余都是引用类型（reference types），包括类、接口、数组和枚举。引用变量存储的是对象在堆内存中的地址，而非对象本身。

```java
String name = "码上记";       // String 是引用类型
int[] nums = {1, 2, 3};      // 数组是引用类型
Object obj = null;            // null 表示不指向任何对象
```

基本类型与引用类型的关键区别：

- 基本类型存储实际值，引用类型存储地址
- 基本类型有默认值（如 `int` 默认为 0），引用类型默认为 `null`
- 基本类型在栈上分配，引用类型的对象在堆上分配
- 基本类型不能调用方法，引用类型可以

Java 为每种基本类型提供了对应的包装类（Wrapper Class），可以在需要对象的场合使用：

| 基本类型 | 包装类 |
|---------|--------|
| byte | Byte |
| short | Short |
| int | Integer |
| long | Long |
| float | Float |
| double | Double |
| char | Character |
| boolean | Boolean |

自动装箱（autoboxing）和拆箱（unboxing）机制使得基本类型与包装类之间可以自动转换：

```java
Integer num = 42;        // 自动装箱：int → Integer
int value = num;          // 自动拆箱：Integer → int
```

## 变量声明与初始化

Java 中变量必须先声明后使用。声明时指定类型和变量名，可以在声明时赋初值：

```java
int age;                // 声明
age = 25;               // 赋值

int score = 100;        // 声明并初始化
double pi = 3.14159;
boolean active = true;
String title = "码上记";
```

**命名规范：**

- 变量名和方法名使用小驼峰：`userName`、`getMaxValue`
- 类名使用大驼峰：`UserService`、`ArrayList`
- 常量使用全大写下划线分隔：`MAX_VALUE`、`DEFAULT_TIMEOUT`
- 包名全部小写：`com.example.myapp`
- 不能使用保留字（如 `class`、`public`、`if`）作为标识符
- 以字母、下划线 `_` 或美元符 `$` 开头，后续可以是字母、数字、`_`、`$`

Java 10 引入了 `var` 关键字，允许局部变量的类型由编译器推断：

```java
var list = new ArrayList<String>();  // 推断为 ArrayList<String>
var num = 42;                         // 推断为 int
```

`var` 只能用于局部变量，不能用于成员变量、方法参数或返回类型。

## 类型转换

### 自动类型提升（Widening）

当小范围类型赋值给大范围类型时，Java 会自动进行类型提升，不会丢失数据：

```
byte → short → int → long → float → double
char → int → long → float → double
```

```java
int a = 100;
long b = a;        // int → long，自动提升
double c = b;      // long → double，自动提升

char ch = 'A';
int code = ch;     // char → int，得到 65
```

### 强制类型转换（Narrowing）

大范围类型赋值给小范围类型时，必须显式强制转换，可能丢失精度或溢出：

```java
double pi = 3.14159;
int truncated = (int) pi;      // 结果为 3，小数部分丢失

int big = 130;
byte small = (byte) big;       // 结果为 -126，发生溢出（130 超出 byte 范围）
```

`long` 转 `float` 虽然是从小范围到大范围（8 字节到 4 字节），但因为 `float` 是浮点类型，实际上会自动提升，不过可能丢失精度：

```java
long l = 123456789L;
float f = l;    // 自动转换，但 f 的值可能不精确
```

## 字面量

字面量（literal）是直接出现在源代码中的固定值。

**整数字面量**支持多种进制：

```java
int decimal = 42;           // 十进制
int hex = 0x2A;             // 十六进制，以 0x 开头
int octal = 052;            // 八进制，以 0 开头（不推荐使用）
int binary = 0b101010;      // 二进制，以 0b 开头（Java 7+）
```

可以在数字字面量中使用下划线增强可读性：

```java
int million = 1_000_000;
long creditCard = 1234_5678_9012_3456L;
```

`long` 类型需要在数字后加 `L` 或 `l`（推荐大写 `L`，避免与数字 1 混淆）。未加后缀的整数字面量默认为 `int` 类型。

**浮点字面量**默认为 `double` 类型，加 `F` 或 `f` 后缀表示 `float`：

```java
double d1 = 3.14;           // double
double d2 = 3.14D;          // double（D 后缀可选）
float f = 3.14F;            // float
double sci = 1.0e-9;        // 科学计数法
```

**字符字面量**用单引号括起来，支持 Unicode 转义：

```java
char c1 = 'A';
char c2 = '\n';             // 换行符
char c3 = '\u0041';         // Unicode 转义，等同于 'A'
char c4 = '\\';             // 反斜杠本身
```

**字符串字面量**用双引号括起来：

```java
String s1 = "Hello, World!";
String s2 = "";             // 空字符串
String s3 = "包含\n换行";   // 支持转义字符
```

## 运算符

### 算术运算符

| 运算符 | 说明 | 示例 |
|--------|------|------|
| `+` | 加法 | `3 + 2` → 5 |
| `-` | 减法 | `3 - 2` → 1 |
| `*` | 乘法 | `3 * 2` → 6 |
| `/` | 除法 | `7 / 2` → 3（整数除法截断） |
| `%` | 取模（求余） | `7 % 2` → 1 |
| `++` | 自增 | `i++`（后置）、`++i`（前置） |
| `--` | 自减 | `i--`（后置）、`--i`（前置） |

注意整数除法会截断小数部分。如果需要精确结果，至少一个操作数应为浮点类型：

```java
int a = 7 / 2;          // 结果为 3
double b = 7.0 / 2;     // 结果为 3.5
```

前置 `++` 先自增再返回值，后置 `++` 先返回值再自增：

```java
int i = 5;
int a = ++i;    // i 变为 6，a 为 6
int b = i++;    // b 为 6，i 变为 7
```

### 关系运算符

| 运算符 | 说明 |
|--------|------|
| `==` | 等于 |
| `!=` | 不等于 |
| `<` | 小于 |
| `>` | 大于 |
| `<=` | 小于等于 |
| `>=` | 大于等于 |

关系运算符返回 `boolean` 值。注意 `==` 对于引用类型比较的是地址而非内容。

### 逻辑运算符

| 运算符 | 说明 | 短路特性 |
|--------|------|---------|
| `&&` | 逻辑与 | 左侧为 false 时不再计算右侧 |
| `\|\|` | 逻辑或 | 左侧为 true 时不再计算右侧 |
| `!` | 逻辑非 | 无 |

短路求值可以避免不必要的计算和潜在错误：

```java
if (list != null && list.size() > 0) {
    // 当 list 为 null 时，不会执行 list.size()，避免 NullPointerException
}
```

### 位运算符

| 运算符 | 说明 | 示例 |
|--------|------|------|
| `&` | 按位与 | `0b1010 & 0b1100` → 0b1000 |
| `\|` | 按位或 | `0b1010 \| 0b1100` → 0b1110 |
| `^` | 按位异或 | `0b1010 ^ 0b1100` → 0b0110 |
| `~` | 按位取反 | `~0b1010` → ...0101 |
| `<<` | 左移 | `1 << 3` → 8 |
| `>>` | 右移（符号位填充） | `-8 >> 2` → -2 |
| `>>>` | 无符号右移（零填充） | `-1 >>> 28` → 15 |

### 赋值运算符

```java
int x = 10;
x += 5;     // 等价于 x = x + 5
x -= 3;     // 等价于 x = x - 3
x *= 2;     // 等价于 x = x * 2
x /= 4;     // 等价于 x = x / 4
x %= 3;     // 等价于 x = x % 3
x &= 0xFF;  // 等价于 x = x & 0xFF
```

### 三元运算符

三元运算符是 `if-else` 的简写形式：

```java
int max = (a > b) ? a : b;
String status = (score >= 60) ? "及格" : "不及格";
```

### instanceof 运算符

`instanceof` 检查对象是否是某个类或其子类的实例：

```java
Object obj = "Hello";
if (obj instanceof String) {
    String s = (String) obj;
}

// Java 16+ 模式匹配（pattern matching）
if (obj instanceof String s) {
    System.out.println(s.length());  // 自动转型，无需手动强转
}
```

## 运算符优先级

下表按优先级从高到低排列（行号越小优先级越高）：

| 优先级 | 运算符 | 结合性 |
|--------|--------|-------|
| 1 | `()` `.` `[]` | 左 |
| 2 | `!` `~` `++` `--` `+`(正) `-`(负) `(type)` | 右 |
| 3 | `*` `/` `%` | 左 |
| 4 | `+` `-` | 左 |
| 5 | `<<` `>>` `>>>` | 左 |
| 6 | `<` `<=` `>` `>=` `instanceof` | 左 |
| 7 | `==` `!=` | 左 |
| 8 | `&` | 左 |
| 9 | `^` | 左 |
| 10 | `\|` | 左 |
| 11 | `&&` | 左 |
| 12 | `\|\|` | 左 |
| 13 | `? :` | 右 |
| 14 | `=` `+=` `-=` `*=` `/=` `%=` 等 | 右 |

> 实际开发中，优先级不确定时应该使用括号明确意图，提高代码可读性。

## 数值类型转换规则图

当不同数值类型混合运算时，Java 会按以下规则自动提升：

```
         char ──┐
                ▼
byte → short → int → long → float → double
                ▲
         char ──┘
```

规则总结：

1. **有 `double` 参与** → 结果为 `double`
2. **有 `float` 参与**（无 `double`）→ 结果为 `float`
3. **有 `long` 参与**（无浮点类型）→ 结果为 `long`
4. **其余情况**（`byte`、`short`、`char`、`int`）→ 结果为 `int`

```java
byte b = 10;
short s = 20;
int result = b + s;     // byte + short → int，需要 int 接收

float f = 1.5f;
int i = 3;
double d = f * i;       // float * int → float → 赋给 double（自动提升）
```

掌握这些基础数据类型和运算规则，是编写正确、高效 Java 代码的第一步。在实际开发中，应根据数据的实际范围选择合适的类型，避免不必要的类型转换，同时注意整数溢出和浮点精度问题。