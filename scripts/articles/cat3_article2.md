# 包与模块系统

当一个 Java 项目只有几个类时，把所有文件放在同一个目录下没什么问题。但随着项目规模增长——几十个、几百个甚至上千个类——就需要一套机制来组织和管理这些代码。Java 的包（package）和模块（module）系统就是为解决这个问题而设计的。

## 包的概念与作用

包本质上是一个命名空间，它把相关的类和接口组织到一起。包解决了两个核心问题：

**命名冲突**：不同开发者可以各自创建名为 `Utils` 的类，只要它们位于不同的包中就不会冲突，比如 `com.myapp.Utils` 和 `com.otherapp.Utils`。

**访问控制**：包是访问权限的边界。类、字段、方法可以限制为"包访问"级别，只允许同一个包内的代码访问，从而隐藏实现细节。

在源文件的最顶部声明包名：

```java
package com.codenow.blog.model;

public class Article {
    private Long id;
    private String title;
    // ...
}
```

对应的目录结构必须与包名一致：

```
src/
  com/
    codenow/
      blog/
        model/
          Article.java
        service/
          ArticleService.java
```

## 包的命名约定

Java 社区普遍采用**反转域名模式**来命名包，以确保全球唯一性。规则很简单：把你的域名反过来写，全部小写。

| 组织 / 项目 | 包名前缀 |
|------------|---------|
| Google | `com.google` |
| Apache | `org.apache` |
| 码上记 | `com.codenow` |

常见的层级结构是：`公司域名反转.项目名.模块名.子模块名`

```
com.codenow.blog.model        -- 数据模型
com.codenow.blog.service      -- 业务逻辑
com.codenow.blog.controller   -- 控制器
com.codenow.blog.dao          -- 数据访问
com.codenow.blog.util         -- 工具类
```

包名全部使用小写字母，这是 Java 的强烈约定。避免使用 `java` 或 `javax` 开头的包名，这是 Java 官方保留的。

## import 语句

要使用其他包中的类，就需要通过 `import` 语句来引入。

### 单类型导入

明确导入某个具体的类，这是最推荐的方式，清晰明了。

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDate;
```

### 按需导入（通配符）

使用 `*` 导入某个包下的所有类。虽然写起来方便，但在大型项目中容易引起命名歧义，不推荐在生产代码中大量使用。

```java
import java.util.*; // 导入 java.util 包下的所有类
```

> 为什么不太推荐通配符导入？假设 `com.a.User` 和 `com.b.User` 都存在，使用 `import com.a.*` 和 `import com.b.*` 后，代码中写 `User` 时编译器就不知道指的是哪个了。

### 静态导入

用 `import static` 可以导入某个类的静态成员（静态方法、静态常量），使用时不需要再写类名。

```java
import static java.lang.Math.PI;
import static java.lang.Math.sqrt;

public class Circle {
    public double area(double radius) {
        return PI * radius * radius;
    }

    public double diagonal(double width, double height) {
        return sqrt(width * width + height * height);
    }
}
```

静态导入在频繁使用某个类的静态方法时（如 `Math` 工具方法、JUnit 的断言方法）可以减少冗余代码，但滥用会降低可读性——看到 `sqrt(x)` 不如看到 `Math.sqrt(x)` 那么直观地知道它来自哪里。

## 包访问权限

不加任何访问修饰符时，成员具有**包访问**（也叫默认访问）权限：同一个包内的所有类都可以访问，包外的类则不能。

```java
package com.codenow.blog.internal;

class CacheManager {
    void cleanup() { // 包访问，只有同包的类能调用
        // 清理缓存
    }
}
```

这种机制很适合做"模块内部"的实现细节隐藏——你希望在自己的包内自由调用某些方法，但不想暴露给外部使用者。

## 类路径（Classpath）

类路径是 JVM 和编译器查找 `.class` 文件的路径。理解类路径对于排查 `ClassNotFoundException` 等问题至关重要。

类路径的来源有三种：

1. **当前目录**：默认包含在类路径中（`.` 表示）
2. **JAR 文件**：打包好的类库，通常是第三方依赖
3. **显式配置**：通过 `-cp` 参数或 `CLASSPATH` 环境变量指定

```bash
# 编译时指定类路径
javac -cp lib/mysql.jar:lib/redis.jar src/Main.java

# 运行时指定类路径
java -cp .:lib/mysql.jar:lib/redis.jar com.codenow.Main
```

在现代项目中，Maven 和 Gradle 等构建工具会自动管理类路径，开发者很少需要手动配置。但理解这个概念仍然很重要——当你遇到类加载相关的问题时，知道去哪里找原因。

### 理解类加载器层次

JVM 采用三层类加载器：

| 类加载器 | 职责 |
|---------|------|
| Bootstrap 类加载器 | 加载 `java.lang` 等核心类库 |
| Extension/Platform 类加载器 | 加载 `javax.*` 等扩展类库 |
| Application 类加载器 | 加载类路径上的应用类 |

当一个类需要被加载时，请求会从应用类加载器向上传递，先由父加载器尝试加载。这就是"双亲委派模型"——确保核心类库的安全性（你不能通过自定义的 `java.lang.String` 来替换 JDK 自带的）。

## 模块系统（Java 9+）

Java 9 引入了模块系统（Project Jigsaw），这是 Java 历史上最大的结构性变更之一。它在包的基础上增加了一个更高层的组织单元——模块。

### module-info.java

每个模块在根目录下有一个 `module-info.java` 文件来声明模块的基本信息：

```java
module com.codenow.blog {
    requires java.sql;
    requires java.net.http;

    exports com.codenow.blog.model;
    exports com.codenow.blog.service;
}
```

### requires 与 exports

**`requires`** 声明当前模块依赖的其他模块：

```java
module com.codenow.blog {
    requires java.sql;          // 需要数据库模块
    requires com.fasterxml.jackson.core; // 需要 Jackson 库
}
```

**`exports`** 声明当前模块对外暴露哪些包。只有被导出的包才能被其他模块访问：

```java
module com.codenow.blog {
    exports com.codenow.blog.model;    // 公开
    exports com.codenow.blog.service;  // 公开
    // com.codenow.blog.internal 没有导出 → 外部模块无法访问
}
```

还可以使用 `exports ... to` 精确控制哪些模块可以访问：

```java
exports com.codenow.blog.internal to com.codenow.admin;
```

### 模块化的意义

模块系统解决了 Java 长期面临的几个痛点：

**强封装**：在模块系统之前，`public` 的类对所有人可见，没有任何机制阻止外部代码直接依赖你的内部实现。模块系统让"内部实现"真正成为内部的——即使类是 `public` 的，只要它所在的包没有被 `exports`，外部就无法访问。

**可靠配置**：模块在编译期就声明了依赖关系，启动时 JVM 会验证所有依赖是否满足。这比运行时才发现 `ClassNotFoundException` 要可靠得多。

**可扩展的 Java 平台**：JDK 本身也被拆分成了约 70 个模块（`java.base`、`java.sql`、`java.net.http` 等），应用可以声明只依赖自己需要的模块，从而减小最终打包体积（配合 `jlink` 工具）。

```bash
# 用 jlink 创建只包含所需模块的自定义运行时
jlink --module-path $JAVA_HOME/jmods \
      --add-modules java.base,java.sql \
      --output custom-jre \
      --compress=2
```

对于初学者而言，模块系统不是必须掌握的内容——大多数 Spring Boot 项目仍然运行在类路径模式下，不需要 `module-info.java`。但了解它的存在和设计动机，对理解 Java 的演进方向很有帮助。

## 常见的 Java 标准库包

Java 标准库（JDK）提供了大量现成的包，熟悉它们是高效开发的基础：

| 包名 | 用途 | 常用类 |
|------|------|-------|
| `java.lang` | 语言核心（自动导入） | `String`、`Object`、`Integer`、`Math`、`System`、`Thread` |
| `java.util` | 集合框架、日期时间、工具类 | `ArrayList`、`HashMap`、`Collections`、`Arrays`、`Optional` |
| `java.io` | 文件和流操作 | `File`、`InputStream`、`OutputStream`、`BufferedReader` |
| `java.nio` | 新 I/O（缓冲区、通道） | `ByteBuffer`、`FileChannel`、`Path`、`Files` |
| `java.time` | 现代日期时间 API（Java 8+） | `LocalDate`、`LocalDateTime`、`Duration`、`Period` |
| `java.math` | 高精度运算 | `BigDecimal`、`BigInteger` |
| `java.net` | 网络编程 | `URL`、`HttpURLConnection`、`Socket` |
| `java.sql` | 数据库访问（JDBC） | `Connection`、`PreparedStatement`、`ResultSet` |
| `java.util.stream` | Stream API（Java 8+） | `Stream`、`Collectors` |
| `java.util.concurrent` | 并发编程 | `ExecutorService`、`Future`、`ConcurrentHashMap` |

`java.lang` 包是唯一一个被自动导入的包，所以使用 `String`、`Object`、`System` 等类时不需要写 `import`。

```java
// java.lang 自动导入，无需 import
String name = "码上记";
int len = name.length();
System.out.println(Math.max(10, 20));

// 其他包需要显式 import
import java.util.List;
import java.time.LocalDate;
List<String> list = List.of("a", "b", "c");
LocalDate today = LocalDate.now();
```

掌握包和模块系统，是组织好一个 Java 项目的基础。小项目用包就够了；当项目规模增长到一定程度、需要清晰界定模块边界时，模块系统就会显现出它的价值。
