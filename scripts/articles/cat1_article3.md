# 第一个 Java 程序与编译运行原理

## 从 Hello World 开始

学习任何编程语言，第一步都是编写一个输出 "Hello, World!" 的程序。Java 也不例外。

### 编写源代码

创建一个名为 `HelloWorld.java` 的文件，输入以下内容：

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

**关键规则：**

- 文件名必须与类名完全一致（包括大小写），即 `HelloWorld.java`
- 每条语句以分号 `;` 结尾
- 花括号 `{}` 用于定义代码块

### 编译

打开终端，进入 `HelloWorld.java` 所在目录，执行编译命令：

```bash
javac HelloWorld.java
```

如果没有报错，当前目录下会生成一个 `HelloWorld.class` 文件——这就是字节码文件。

### 运行

使用 `java` 命令运行编译后的字节码：

```bash
java HelloWorld
```

> **注意**：运行时不加 `.class` 后缀，也不加 `.java` 后缀。直接写类名即可。

控制台输出：

```
Hello, World!
```

至此，你已经完成了 Java 程序从编写、编译到运行的完整流程。下面逐一深入分析这个程序的每个组成部分。

## 程序入口 main 方法详解

`public static void main(String[] args)` 是 Java 应用程序的入口点。这行代码看起来复杂，但每个关键字都有其存在的理由：

```java
public static void main(String[] args)
```

| 关键字/语法 | 含义 |
|------------|------|
| `public` | 访问修饰符，表示该方法对外部可见。JVM 需要调用此方法，所以必须是 `public` |
| `static` | 静态方法，无需创建对象即可调用。JVM 启动时还没有任何对象实例，所以入口方法必须是 `static` |
| `void` | 返回值类型，表示该方法不返回任何值 |
| `main` | 方法名，JVM 通过这个固定名称识别程序入口 |
| `String[] args` | 命令行参数数组。运行时传入的参数会存放在这个数组中 |

`args` 的使用示例：

```java
public class ArgsDemo {
    public static void main(String[] args) {
        System.out.println("参数个数：" + args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.println("参数 " + i + "：" + args[i]);
        }
    }
}
```

```bash
java ArgsDemo hello world
# 输出：
# 参数个数：2
# 参数 0：hello
# 参数 1：world
```

从 Java 14 开始，还支持一种简化写法（预览特性，Java 21 正式定稿）：

```java
// 简化的 main 方法（Java 21+）
void main(String[] args) {
    System.out.println("Hello, World!");
}
```

不过在实际开发中，建议仍然使用完整的 `public static void main(String[] args)` 形式，以保持兼容性和规范性。

## Java 程序的编译与执行过程

理解 Java 程序从源码到运行的完整过程，是掌握 Java 技术体系的基础。

### 完整执行流程

```
                   编译阶段                              运行阶段
┌──────────┐    ┌──────────┐    ┌──────────────┐    ┌──────────────────────┐
│ .java    │    │ javac    │    │ .class       │    │  JVM                 │
│ 源代码   │───→│ 编译器   │───→│ 字节码文件   │───→│                      │
│          │    │          │    │              │    │  ┌────────────────┐  │
└──────────┘    └──────────┘    └──────────────┘    │  │ 类加载器       │  │
                                                    │  │ (ClassLoader)  │  │
                                                    │  └───────┬────────┘  │
                                                    │          ↓           │
                                                    │  ┌────────────────┐  │
                                                    │  │ 字节码验证器   │  │
                                                    │  └───────┬────────┘  │
                                                    │          ↓           │
                                                    │  ┌────────────────┐  │
                                                    │  │ 解释执行       │  │
                                                    │  │ + JIT 编译     │  │
                                                    │  └───────┬────────┘  │
                                                    │          ↓           │
                                                    │  ┌────────────────┐  │
                                                    │  │ 机器码执行     │  │
                                                    │  └────────────────┘  │
                                                    └──────────────────────┘
```

### 第一步：编译（javac）

`javac` 编译器将 `.java` 源文件编译为 `.class` 字节码文件。这个过程包含以下阶段：

1. **词法分析**：将源代码拆分为一个个 Token（关键字、标识符、运算符等）
2. **语法分析**：根据 Java 语法规则，将 Token 组织成抽象语法树（AST）
3. **语义分析**：类型检查、作用域分析等
4. **字节码生成**：将 AST 转换为 JVM 可理解的字节码指令，写入 `.class` 文件

### 第二步：类加载（ClassLoader）

当执行 `java HelloWorld` 时，JVM 的类加载器负责将 `.class` 文件加载到内存中。类加载过程包括：

1. **加载（Loading）**：通过类的全限定名找到 `.class` 文件，读取字节流
2. **链接（Linking）**：
   - **验证**：确保字节码符合 JVM 规范，不会危害安全
   - **准备**：为类的静态变量分配内存并设置默认值
   - **解析**：将符号引用替换为直接引用
3. **初始化（Initialization）**：执行类构造器 `<clinit>` 方法（静态变量赋值、静态代码块）

### 第三步：执行字节码

JVM 执行字节码采用两种方式的结合：

- **解释执行**：JVM 内部的解释器逐条读取字节码指令并翻译为机器码执行。优点是启动快，缺点是执行效率较低。
- **JIT 编译（Just-In-Time Compilation）**：JVM 会监测代码的运行频率，对被多次调用的"热点代码"（Hot Spot），JIT 编译器会将其一次性编译为本地机器码并缓存。后续调用直接执行机器码，大幅提升性能。

这就是为什么 Java 程序在刚启动时可能稍慢，但运行一段时间后性能会显著提升——这被称为 **JVM 的预热（Warm-up）**。

可以使用 `javap` 工具反编译 `.class` 文件，查看字节码指令：

```bash
javap -c HelloWorld
```

输出（部分）：

```
public static void main(java.lang.String[]);
    Code:
       0: getstatic     #7    // Field java/lang/System.out
       3: ldc           #13   // String Hello, World!
       5: invokevirtual #15   // Method java/io/PrintStream.println
       8: return
```

## "一次编写，到处运行"的原理

"Write Once, Run Anywhere" 是 Java 最核心的承诺，其实现依赖于**字节码**和 **JVM** 的架构设计。

传统的编译型语言（如 C/C++）直接将源码编译为特定平台的机器码，不同操作系统和 CPU 架构需要重新编译。Java 则引入了一个中间层：

```
传统编译型语言：
源代码 ──编译──→ 平台特定的机器码（Windows .exe / Linux ELF / macOS Mach-O）

Java：
源代码 ──编译──→ 平台无关的字节码（.class）──JVM──→ 平台特定的机器码
```

字节码是一种中间表示形式，它不针对任何具体的硬件平台。不同的操作系统有各自的 JVM 实现（Windows 版、Linux 版、macOS 版），这些 JVM 负责将相同的字节码翻译为各自平台的机器码。

```
                        ┌─── Windows JVM  ──→ Windows 机器码
HelloWorld.class ──────→├─── Linux JVM    ──→ Linux 机器码
（平台无关的字节码）     └─── macOS JVM   ──→ macOS 机器码
```

这就是 Java 程序跨平台的本质——**字节码是跨平台的，JVM 是平台相关的**。开发者只需要关心字节码的生成，平台适配的工作由 JVM 实现者完成。

## 常见编译错误与运行时错误的排查

初学者在编写 Java 程序时，经常会遇到各种错误。理解错误类型和排查方法能节省大量时间。

### 编译错误（Compile-time Error）

编译错误发生在 `javac` 编译阶段，通常是语法问题。

**错误示例 1：缺少分号**

```java
public class Demo {
    public static void main(String[] args) {
        System.out.println("Hello")  // 缺少分号
    }
}
```

错误信息：`error: ';' expected`

**错误示例 2：文件名与类名不匹配**

```
# 文件名为 demo.java，但类名为 Demo
javac demo.java
# error: class Demo is public, should be declared in a file named Demo.java
```

**错误示例 3：未找到符号**

```java
public class Demo {
    public static void main(String[] args) {
        System.out.println(message);  // message 未定义
    }
}
```

错误信息：`error: cannot find symbol`

### 运行时错误（Runtime Error）

运行时错误发生在程序执行阶段，通常由逻辑问题或资源问题导致。

**错误示例 1：空指针异常**

```java
public class Demo {
    public static void main(String[] args) {
        String str = null;
        System.out.println(str.length());  // NullPointerException
    }
}
```

**错误示例 2：数组越界**

```java
public class Demo {
    public static void main(String[] args) {
        int[] arr = {1, 2, 3};
        System.out.println(arr[5]);  // ArrayIndexOutOfBoundsException
    }
}
```

**错误示例 3：除以零**

```java
public class Demo {
    public static void main(String[] args) {
        int result = 10 / 0;  // ArithmeticException: / by zero
    }
}
```

### 排错技巧

- **仔细阅读错误信息**：Java 的错误信息通常会指出错误的文件名、行号和原因
- **从第一个错误开始修**：一个编译错误可能引发后续的连锁错误
- **使用 IDE 的实时检查**：IntelliJ IDEA 等 IDE 会在你输入代码时实时标记错误
- **善用调试器**：设置断点，单步执行，观察变量值的变化

## 使用 IDEA 创建项目与运行调试

### 创建 Java 项目

1. 打开 IntelliJ IDEA，选择 **New Project**
2. 选择 **Java**，确认 JDK 已正确识别（显示版本号）
3. 输入项目名称（如 `HelloJava`），选择项目存放路径
4. 点击 **Create** 完成创建

### 编写并运行代码

1. 在 `src` 目录上右键 → **New** → **Java Class**
2. 输入类名 `HelloWorld`，IDEA 会自动生成类的骨架代码
3. 输入 `main` 然后按 **Tab** 键，IDEA 会自动补全 `main` 方法
4. 编写代码后，右键点击代码编辑区 → **Run 'HelloWorld.main()'**

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

### 调试程序

1. 在代码行号左侧点击，设置**断点**（红色圆点）
2. 右键 → **Debug 'HelloWorld.main()'** 以调试模式启动
3. 程序会在断点处暂停，此时可以：
   - 查看变量的当前值（Variables 面板）
   - **Step Over**（F8）：执行当前行，进入下一行
   - **Step Into**（F7）：进入方法内部
   - **Resume**（F9）：继续运行到下一个断点
4. 调试完毕后，点击 **Stop** 结束调试会话

调试是解决逻辑错误最有效的手段。相比 `System.out.println` 打印调试法，IDE 调试器能更直观地展示程序的执行流程和状态。

## Java 编码规范

良好的编码习惯从第一天开始培养。以下是 Java 社区广泛遵循的编码规范：

### 命名约定

| 元素 | 规则 | 示例 |
|------|------|------|
| 类名 | **大驼峰**（UpperCamelCase） | `StudentInfo`、`ArrayList` |
| 方法名 / 变量名 | **小驼峰**（lowerCamelCase） | `getName`、`studentAge` |
| 常量 | **全大写 + 下划线** | `MAX_VALUE`、`DEFAULT_PORT` |
| 包名 | **全小写，用点分隔** | `com.example.myapp` |

### 包的命名

包名通常采用公司或组织的域名反转形式，全部小写：

```java
// 阿里巴巴
com.alibaba.fastjson

// 本项目
com.codenow.blog

// Apache
org.apache.commons.lang3
```

### 其他规范要点

- **大括号**：左大括号 `{` 不换行（K&R 风格），与控制语句同行
- **缩进**：使用 4 个空格，不使用 Tab
- **每行长度**：建议不超过 120 个字符
- **空行**：方法之间、逻辑段落之间用空行分隔
- **变量声明**：在首次使用处附近声明，避免在方法开头集中声明所有变量

```java
// 良好的代码风格示例
public class Student {
    private static final int MAX_SCORE = 100;

    private String name;
    private int score;

    public Student(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public boolean isPassed() {
        return score >= 60;
    }

    public String getGrade() {
        if (score >= 90) {
            return "A";
        } else if (score >= 80) {
            return "B";
        } else if (score >= 70) {
            return "C";
        } else if (score >= 60) {
            return "D";
        } else {
            return "F";
        }
    }
}
```

遵循编码规范不仅让代码更易读、更易维护，也是团队协作的基本素养。当你在 IDEA 中编写代码时，IDE 会实时提示不符合规范的地方，帮助你养成良好的编码习惯。
