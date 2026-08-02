# 运算符优先级与控制流速查

理解运算符优先级和掌握控制流语法是编写正确 Java 代码的基础。本文提供完整的优先级速查表和控制流语句参考。

## 运算符优先级完整表

优先级从高到低排列，同一行优先级相同。优先级高的先执行，同优先级按结合性决定。

| 优先级 | 运算符 | 说明 | 结合性 | 示例 |
|--------|--------|------|--------|------|
| 1 | `()` `[]` `.` | 括号、数组下标、成员访问 | 左→右 | `obj.method()` `arr[0]` |
| 2 | `++` `--` | 后缀自增/自减 | 左→右 | `i++` `j--` |
| 3 | `++` `--` `+` `-` `~` `!` | 前缀自增/自减、正负号、按位取反、逻辑非 | 右→左 | `++i` `-x` `~mask` `!flag` |
| 4 | `*` `/` `%` | 乘、除、取模 | 左→右 | `a * b` `10 % 3` |
| 5 | `+` `-` | 加、减 | 左→右 | `a + b` `x - y` |
| 6 | `<<` `>>` `>>>` | 左移、算术右移、逻辑右移 | 左→右 | `1 << 3` `-1 >>> 1` |
| 7 | `<` `<=` `>` `>=` `instanceof` | 关系比较、类型检查 | 左→右 | `a < b` `obj instanceof String` |
| 8 | `==` `!=` | 相等、不等 | 左→右 | `a == b` `x != null` |
| 9 | `&` | 按位与、逻辑与（不短路） | 左→右 | `a & b` |
| 10 | `^` | 按位异或 | 左→右 | `a ^ b` |
| 11 | `\|` | 按位或、逻辑或（不短路） | 左→右 | `a \| b` |
| 12 | `&&` | 短路逻辑与 | 左→右 | `a != null && a > 0` |
| 13 | `\|\|` | 短路逻辑或 | 左→右 | `x == 0 \|\| y == 0` |
| 14 | `? :` | 三元条件 | 右→左 | `a > b ? a : b` |
| 15 | `=` `+=` `-=` `*=` `/=` `%=` `&=` `^=` `\|=` `<<=` `>>=` `>>>=` | 赋值及复合赋值 | 右→左 | `a += 5` `x = y = 1` |

**记忆口诀**：单目 > 算术 > 移位 > 关系 > 位运算 > 逻辑 > 三元 > 赋值。拿不准的地方加括号。

## 运算符详解与示例

### 算术运算符

| 运算符 | 说明 | 示例 |
|--------|------|------|
| `+` | 加法 | `3 + 2 → 5` |
| `-` | 减法 | `3 - 2 → 1` |
| `*` | 乘法 | `3 * 2 → 6` |
| `/` | 除法 | `7 / 2 → 3`（整数除法截断） |
| `%` | 取模（求余） | `7 % 2 → 1` `-7 % 2 → -1` |

```java
// 整数除法陷阱
int result = 7 / 2;       // 结果是 3，不是 3.5
double correct = 7.0 / 2;  // 结果是 3.5

// 负数取模：结果符号与被除数一致
System.out.println(-7 % 2);   // -1
System.out.println(7 % -2);   // 1
```

### 自增自减运算符

| 运算符 | 说明 | 示例 |
|--------|------|------|
| `i++` | 后缀自增：先返回值，再加 1 | `int a = i++;` |
| `++i` | 前缀自增：先加 1，再返回值 | `int a = ++i;` |
| `i--` | 后缀自减 | `int a = i--;` |
| `--i` | 前缀自减 | `int a = --i;` |

```java
int i = 5;
int a = i++;  // a = 5, i = 6
int b = ++i;  // b = 7, i = 7
```

### 位运算符

| 运算符 | 说明 | 示例 | 结果 |
|--------|------|------|------|
| `&` | 按位与 | `0b1010 & 0b1100` | `0b1000` |
| `\|` | 按位或 | `0b1010 \| 0b1100` | `0b1110` |
| `^` | 按位异或 | `0b1010 ^ 0b1100` | `0b0110` |
| `~` | 按位取反 | `~0b00001111` | `0b11110000` |
| `<<` | 左移 | `1 << 3` | `8`（相当于 ×2³） |
| `>>` | 算术右移（保留符号） | `-8 >> 2` | `-2` |
| `>>>` | 逻辑右移（补 0） | `-1 >>> 28` | `15` |

```java
// 位运算实用技巧
int flags = 0b1010;
flags |= 0b0001;    // 设置第 0 位 → 0b1011
flags &= ~0b0010;   // 清除第 1 位 → 0b1001
flags ^= 0b1000;    // 翻转第 3 位 → 0b0001
boolean bit2 = (flags & 0b0100) != 0;  // 检查第 2 位

// 快速计算
int half = 100 >> 1;    // 50
int mul16 = 5 << 4;     // 80
```

### 逻辑运算符

| 运算符 | 说明 | 短路？ | 示例 |
|--------|------|--------|------|
| `&&` | 逻辑与 | 是：左为 false 则不执行右 | `a != null && a.length() > 0` |
| `\|\|` | 逻辑或 | 是：左为 true 则不执行右 | `x == 0 \|\| y / x > 1` |
| `!` | 逻辑非 | — | `!list.isEmpty()` |
| `&` | 不短路与 | 否：两边都执行 | `f() & g()` |
| `\|` | 不短路或 | 否：两边都执行 | `f() \| g()` |

### 赋值运算符

| 运算符 | 等价形式 | 示例 |
|--------|----------|------|
| `a += b` | `a = a + b` | `int x = 5; x += 3;` → 8 |
| `a -= b` | `a = a - b` | `int x = 5; x -= 3;` → 2 |
| `a *= b` | `a = a * b` | `int x = 5; x *= 3;` → 15 |
| `a /= b` | `a = a / b` | `int x = 7; x /= 2;` → 3 |
| `a %= b` | `a = a % b` | `int x = 7; x %= 2;` → 1 |
| `a &= b` | `a = a & b` | 位与赋值 |
| `a \|= b` | `a = a \| b` | 位或赋值 |
| `a ^= b` | `a = a ^ b` | 异或赋值 |
| `a <<= n` | `a = a << n` | 左移赋值 |
| `a >>= n` | `a = a >> n` | 右移赋值 |
| `a >>>= n` | `a = a >>> n` | 无符号右移赋值 |

## 控制流语句速查

### if-else if-else

```java
if (score >= 90) {
    grade = 'A';
} else if (score >= 80) {
    grade = 'B';
} else if (score >= 60) {
    grade = 'C';
} else {
    grade = 'D';
}
```

单条语句可省略花括号，但**强烈建议始终使用花括号**，避免维护时引入 bug。

### switch 语句

**传统语法**：

```java
switch (day) {
    case 1:
        System.out.println("Monday");
        break;              // 忘记 break 会穿透到下一个 case
    case 2:
        System.out.println("Tuesday");
        break;
    default:
        System.out.println("Other");
}
```

**箭头语法（Java 14+）**：不需要 break，不会穿透。

```java
String result = switch (day) {
    case 1 -> "Monday";
    case 2 -> "Tuesday";
    case 3, 4, 5 -> "Midweek";   // 多值匹配
    case 6, 7 -> {
        System.out.println("Weekend!");
        yield "Weekend";         // 代码块中用 yield 返回值
    }
    default -> "Unknown";
};
```

**switch 支持的类型**：`byte`、`short`、`char`、`int`、`enum`、`String`（Java 7+）、密封类模式匹配（Java 21+）。不支持 `long`、`float`、`double`。

### 循环语句

**for 循环**：

```java
// 标准 for
for (int i = 0; i < 10; i++) {
    System.out.println(i);
}

// 无限循环
for (;;) {
    break;  // 用 break 退出
}
```

**while 循环**：

```java
int i = 0;
while (i < 10) {
    System.out.println(i);
    i++;
}
```

**do-while 循环**：至少执行一次。

```java
int i = 0;
do {
    System.out.println(i);  // 即使条件不满足也会执行一次
    i++;
} while (i < 10);
```

**for-each 循环（增强 for）**：

```java
int[] arr = {1, 2, 3, 4, 5};
for (int num : arr) {
    System.out.println(num);
}

List<String> list = List.of("a", "b", "c");
for (String s : list) {
    System.out.println(s);
}
```

### break、continue、return

| 关键字 | 作用 | 使用场景 |
|--------|------|----------|
| `break` | 跳出当前循环或 switch | 提前终止循环 |
| `continue` | 跳过本次迭代，进入下一次循环 | 跳过不需要处理的元素 |
| `return` | 结束方法并返回值 | 从方法中返回 |

```java
// break：找到后立即退出
for (int i = 0; i < arr.length; i++) {
    if (arr[i] == target) {
        System.out.println("Found at index " + i);
        break;
    }
}

// continue：跳过偶数
for (int i = 0; i < 10; i++) {
    if (i % 2 == 0) continue;
    System.out.println(i);  // 只输出奇数
}

// return：提前返回结果
public String getStatus(int code) {
    if (code == 200) return "OK";
    if (code == 404) return "Not Found";
    return "Unknown";
}
```

### 带标签的 break/continue

标签用于从多层嵌套循环中直接跳出。

```java
outer:
for (int i = 0; i < 10; i++) {
    for (int j = 0; j < 10; j++) {
        if (i * j > 20) {
            System.out.println("Breaking at i=" + i + ", j=" + j);
            break outer;  // 直接跳出外层循环
        }
    }
}

// 带标签的 continue
outer:
for (int i = 0; i < 5; i++) {
    for (int j = 0; j < 5; j++) {
        if (j == 3) continue outer;  // 跳到外层循环的下一次迭代
        System.out.println(i + "," + j);
    }
}
```

## 常见运算符陷阱

### 短路求值

`&&` 和 `||` 是短路运算符，右侧表达式可能不执行。

```java
// 安全：null 检查在前，不会触发 NPE
if (str != null && str.length() > 0) { ... }

// 危险：如果用 & 替代 &&，即使 str 为 null 也会执行 str.length()
if (str != null & str.length() > 0) { ... }  // 可能 NPE
```

### 整数除法

```java
// 陷阱：两个 int 相除结果仍是 int
double half = 1 / 2;       // 0.0，不是 0.5！先除后赋值
double half2 = 1.0 / 2;    // 0.5，正确
double half3 = (double) 1 / 2;  // 0.5，显式转换
```

### 字符串拼接 + 的优先级

`+` 用于字符串拼接时，与算术 `+` 混用要注意优先级。

```java
System.out.println("Sum: " + 1 + 2);    // "Sum: 12"，从左到右拼接
System.out.println("Sum: " + (1 + 2));   // "Sum: 3"，括号优先
System.out.println(1 + 2 + "Result");    // "3Result"，先算术后拼接
```

### 比较运算符

```java
// == 比较的是引用（地址），不是内容
String a = new String("hello");
String b = new String("hello");
System.out.println(a == b);       // false
System.out.println(a.equals(b));  // true

// Integer 缓存陷阱
Integer x = 127;
Integer y = 127;
System.out.println(x == y);       // true（缓存范围内）

Integer m = 128;
Integer n = 128;
System.out.println(m == n);       // false（超出缓存范围）
System.out.println(m.equals(n));  // true
```

### 三元运算符类型推断

```java
// 陷阱：编译器会统一类型
Object result = true ? 1 : "hello";  // 结果是 Integer 1，不是 int
// 实际执行：true ? Integer.valueOf(1) : "hello"

// 更隐蔽的情况
boolean flag = true;
Integer a = null;
int b = 42;
// int c = flag ? a : b;  // NullPointerException！编译器将 a 拆箱
```
