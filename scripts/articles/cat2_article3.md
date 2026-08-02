# 控制流与数组

程序的执行顺序并非总是从上到下依次进行。控制流语句让程序可以根据条件做出选择、重复执行某段代码。数组则是存储同类型数据集合的基础数据结构。

## 条件语句

### if-else if-else

`if` 语句根据布尔表达式的值决定执行哪段代码：

```java
int score = 85;

if (score >= 90) {
    System.out.println("优秀");
} else if (score >= 80) {
    System.out.println("良好");
} else if (score >= 60) {
    System.out.println("及格");
} else {
    System.out.println("不及格");
}
```

条件按顺序判断，一旦某个条件为 `true`，执行对应代码块后跳过剩余分支。花括号可以省略（仅当代码块只有一条语句时），但强烈建议始终使用花括号，避免歧义。

### switch 语句

当需要对一个变量进行多值匹配时，`switch` 比 `if-else` 链更清晰：

**传统语法：**

```java
int day = 3;
switch (day) {
    case 1:
        System.out.println("星期一");
        break;
    case 2:
        System.out.println("星期二");
        break;
    case 3:
        System.out.println("星期三");
        break;
    default:
        System.out.println("其他");
        break;
}
```

传统 `switch` 的注意事项：

- 每个 `case` 后必须加 `break`，否则会发生"穿透"（fall-through）——继续执行下一个 `case` 的代码
- `case` 的值必须是编译时常量（`int`、`char`、`String`、枚举）
- `default` 处理所有未匹配的情况

**穿透的合理利用**——多个 `case` 共享同一段逻辑：

```java
String month = "FEB";
int days;
switch (month) {
    case "JAN": case "MAR": case "MAY": case "JUL":
    case "AUG": case "OCT": case "DEC":
        days = 31;
        break;
    case "APR": case "JUN": case "SEP": case "NOV":
        days = 30;
        break;
    case "FEB":
        days = 28;
        break;
    default:
        days = -1;
}
```

**增强 switch（Java 14+ 箭头语法）：**

Java 14 引入了箭头语法（arrow case），消除了穿透问题，每个分支自动只执行右侧的表达式或代码块：

```java
int day = 3;
String dayName = switch (day) {
    case 1 -> "星期一";
    case 2 -> "星期二";
    case 3 -> "星期三";
    case 4 -> "星期四";
    case 5 -> "星期五";
    case 6, 7 -> "周末";
    default -> "无效";
};
```

箭头语法的特点：

- 不需要 `break`，不会穿透
- 多个值可以用逗号分隔（`case 6, 7 ->`）
- 可以作为表达式返回值（用 `yield` 关键字返回复杂代码块的值）

```java
String result = switch (input) {
    case "A" -> "优秀";
    case "B" -> {
        log("选择了 B");
        yield "良好";  // 用 yield 返回值
    }
    default -> "未知";
};
```

## 循环语句

### for 循环

`for` 循环适用于已知循环次数的场景：

```java
// 经典 for 循环
for (int i = 0; i < 10; i++) {
    System.out.println(i);
}

// 倒序遍历
for (int i = 9; i >= 0; i--) {
    System.out.println(i);
}

// 步长不为 1
for (int i = 0; i < 100; i += 5) {
    System.out.println(i);
}
```

`for` 循环的三个部分（初始化、条件、更新）都可以省略，但分号不能省：

```java
// 无限循环
for (;;) {
    // ...
}
```

### while 循环

`while` 循环在条件为 `true` 时反复执行代码块，适用于不确定循环次数的场景：

```java
int n = 1;
while (n < 1000) {
    n *= 2;
}
// n 最终为 1024
```

### do-while 循环

`do-while` 与 `while` 类似，但保证代码块至少执行一次：

```java
Scanner scanner = new Scanner(System.in);
int input;
do {
    System.out.print("请输入正整数：");
    input = scanner.nextInt();
} while (input <= 0);
```

### for-each 循环（增强 for）

`for-each` 循环用于遍历数组或实现了 `Iterable` 接口的集合，语法更简洁：

```java
int[] nums = {1, 2, 3, 4, 5};
for (int n : nums) {
    System.out.println(n);
}

List<String> names = List.of("Alice", "Bob", "Charlie");
for (String name : names) {
    System.out.println(name);
}
```

`for-each` 的局限性：

- 无法获取当前元素的索引
- 无法在遍历时修改数组/集合（修改局部变量不影响原数组）
- 只能正向遍历

需要索引或修改元素时，使用传统 `for` 循环。

## break、continue 与标签

### break

`break` 用于立即终止当前循环或 `switch` 语句：

```java
for (int i = 0; i < 100; i++) {
    if (i == 10) {
        break;  // 当 i 为 10 时退出循环
    }
    System.out.println(i);
}
// 输出 0 到 9
```

### continue

`continue` 跳过当前迭代的剩余部分，直接进入下一次循环：

```java
for (int i = 0; i < 10; i++) {
    if (i % 2 == 0) {
        continue;  // 跳过偶数
    }
    System.out.println(i);
}
// 输出 1 3 5 7 9
```

### 带标签的 break/continue

当存在嵌套循环时，普通的 `break`/`continue` 只作用于最内层循环。使用标签可以控制外层循环：

```java
outer:
for (int i = 0; i < 10; i++) {
    for (int j = 0; j < 10; j++) {
        if (i * j > 20) {
            break outer;  // 直接跳出外层循环
        }
        System.out.println(i + " * " + j + " = " + (i * j));
    }
}
```

标签是一个合法的标识符后跟冒号，放在循环语句之前。虽然标签有效，但在实际开发中应尽量避免多层嵌套循环——通常可以通过提取方法、使用 `return` 或重新设计算法来替代。

## 局部变量作用域

Java 中的局部变量（在方法或代码块内声明的变量）具有块作用域（block scope），只在声明它的花括号 `{}` 内有效：

```java
public void example() {
    int x = 10;          // x 的作用域从这里开始

    if (x > 5) {
        int y = 20;      // y 的作用域仅在 if 块内
        System.out.println(x + y);
    }

    // System.out.println(y);  // 编译错误：y 不在作用域内

    for (int i = 0; i < 5; i++) {
        // i 的作用域仅在 for 循环内
    }

    // System.out.println(i);  // 编译错误：i 不在作用域内
}
```

局部变量必须在使用前初始化，Java 不会给局部变量赋默认值（成员变量有默认值）：

```java
int a;
// System.out.println(a);  // 编译错误：变量 a 可能未初始化
```

## 数组

数组是存储固定大小同类型元素序列的数据结构。数组一旦创建，长度不可改变。

### 声明、创建与初始化

**声明数组变量：**

```java
int[] nums;        // 推荐写法（类型紧邻方括号）
int nums2[];       // C 风格声明（合法但不推荐）
```

**动态初始化**——指定长度，元素为默认值（`int` 为 0，`boolean` 为 `false`，引用类型为 `null`）：

```java
int[] nums = new int[5];       // 创建长度为 5 的数组，元素默认为 0
String[] names = new String[3]; // 元素默认为 null
```

**静态初始化**——直接指定元素值，编译器自动推断长度：

```java
int[] nums = {1, 2, 3, 4, 5};
String[] colors = {"red", "green", "blue"};
double[] prices = new double[]{9.99, 19.99, 29.99};
```

注意：`new double[]{...}` 语法在重新赋值数组变量时是必需的，不能直接写 `prices = {1, 2, 3}`。

### 数组的基本操作

**length 属性**：

```java
int[] arr = {10, 20, 30, 40, 50};
int len = arr.length;   // 5（注意：是属性不是方法，没有括号）
```

**遍历数组**：

```java
int[] arr = {10, 20, 30, 40, 50};

// 方式一：传统 for 循环
for (int i = 0; i < arr.length; i++) {
    System.out.println("arr[" + i + "] = " + arr[i]);
}

// 方式二：for-each 循环
for (int value : arr) {
    System.out.println(value);
}

// 方式三：Arrays.toString()（快速查看数组内容）
System.out.println(Arrays.toString(arr));
// 输出：[10, 20, 30, 40, 50]
```

### 多维数组

Java 中的多维数组实际上是"数组的数组"：

```java
// 声明并初始化二维数组
int[][] matrix = {
    {1, 2, 3},
    {4, 5, 6},
    {7, 8, 9}
};

// 动态创建（第二维长度可以不同——锯齿数组）
int[][] jagged = new int[3][];
jagged[0] = new int[]{1, 2};
jagged[1] = new int[]{3, 4, 5};
jagged[2] = new int[]{6};
```

**遍历二维数组**：

```java
// 传统 for 循环
for (int i = 0; i < matrix.length; i++) {
    for (int j = 0; j < matrix[i].length; j++) {
        System.out.print(matrix[i][j] + " ");
    }
    System.out.println();
}

// for-each 循环
for (int[] row : matrix) {
    for (int val : row) {
        System.out.print(val + " ");
    }
    System.out.println();
}

// Arrays.deepToString()（快速查看多维数组）
System.out.println(Arrays.deepToString(matrix));
// 输出：[[1, 2, 3], [4, 5, 6], [7, 8, 9]]
```

## Arrays 工具类

`java.util.Arrays` 提供了丰富的数组操作方法：

### 排序

```java
int[] arr = {5, 2, 8, 1, 9, 3};
Arrays.sort(arr);
// arr 变为 [1, 2, 3, 5, 8, 9]

// 部分排序（索引 [2, 5) 范围排序）
Arrays.sort(arr, 2, 5);
```

### 二分查找

```java
int[] arr = {1, 2, 3, 5, 8, 9};
int index = Arrays.binarySearch(arr, 5);   // 3（找到的索引）
int notFound = Arrays.binarySearch(arr, 4); // 负数（未找到）

// 注意：必须先排序才能使用 binarySearch
```

### 复制

```java
int[] original = {1, 2, 3, 4, 5};

// copyOf：复制指定长度，多余位置填默认值
int[] copy1 = Arrays.copyOf(original, 3);   // [1, 2, 3]
int[] copy2 = Arrays.copyOf(original, 7);   // [1, 2, 3, 4, 5, 0, 0]

// copyOfRange：复制指定范围 [from, to)
int[] copy3 = Arrays.copyOfRange(original, 1, 4);  // [2, 3, 4]
```

### 填充与比较

```java
int[] arr = new int[5];
Arrays.fill(arr, 42);          // [42, 42, 42, 42, 42]

int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
boolean same = Arrays.equals(a, b);  // true

// 多维数组用 deepEquals
int[][] m1 = {{1, 2}, {3, 4}};
int[][] m2 = {{1, 2}, {3, 4}};
boolean deepSame = Arrays.deepEquals(m1, m2);  // true
```

### 数组转集合

```java
String[] arr = {"apple", "banana", "cherry"};
List<String> list = Arrays.asList(arr);
// 返回的 List 是固定大小的，不能 add/remove，但可以 set

// 如果需要可修改的 List
List<String> mutable = new ArrayList<>(Arrays.asList(arr));
```

`Arrays.asList` 对基本类型数组的行为需要特别注意：

```java
int[] nums = {1, 2, 3};
List<int[]> list = Arrays.asList(nums);
// 不会得到 List<Integer>，而是 List<int[]>，只有一个元素

Integer[] nums2 = {1, 2, 3};
List<Integer> list2 = Arrays.asList(nums2);
// 正确得到 List<Integer>，包含三个元素
```

## 命令行参数

Java 程序的 `main` 方法接收一个 `String[]` 参数，即命令行参数：

```java
public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("未提供参数");
            return;
        }
        for (int i = 0; i < args.length; i++) {
            System.out.println("args[" + i + "] = " + args[i]);
        }
    }
}
```

运行方式：

```bash
java Main hello world 123
```

输出：

```
args[0] = hello
args[1] = world
args[2] = 123
```

`args` 就是一个普通的字符串数组，可以用所有数组相关的方法来处理。

## 常见的数组错误

### ArrayIndexOutOfBoundsException

访问了不存在的数组索引（小于 0 或大于等于 `length`）：

```java
int[] arr = {1, 2, 3};
System.out.println(arr[3]);  // 运行时异常：索引 3 超出范围（有效范围 0~2）
```

这是最常见的数组错误。遍历时务必确保索引范围为 `0` 到 `arr.length - 1`。

### NullPointerException

对一个 `null` 引用调用方法或访问属性：

```java
int[] arr = null;
System.out.println(arr.length);  // 运行时异常：空指针

String[] names = new String[3];
System.out.println(names[0].length());  // 运行时异常：names[0] 为 null
```

在使用数组或数组元素之前，养成检查 `null` 的习惯：

```java
if (arr != null && index >= 0 && index < arr.length) {
    // 安全访问
}
```

控制流语句和数组是 Java 编程中最基础的构件。熟练掌握条件判断、循环遍历和数组操作，是编写任何有意义程序的前提。数组虽然简单，但在实际开发中，当需要动态调整大小时，通常使用 `ArrayList` 等集合类来替代。