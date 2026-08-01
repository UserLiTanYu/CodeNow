# 接口设计与默认方法

## 接口的概念与作用

在 Java 中，接口（Interface）是一种引用类型，它定义了一组行为契约，规定了类必须实现哪些方法。接口的核心思想是"面向接口编程"，通过抽象出共同的行为规范，实现多重继承的效果。

接口的主要作用包括：

- **定义行为规范**：接口规定了实现类必须提供的方法，确保不同类之间具有统一的行为
- **实现多重继承**：Java 类只能单继承，但可以实现多个接口，从而获得多种能力
- **解耦合**：通过接口依赖而非具体实现类，提高代码的灵活性和可维护性
- **支持多态**：接口变量可以指向任何实现该接口的对象

## 接口的定义

使用 `interface` 关键字定义接口：

```java
public interface Printable {
    // 常量（隐式 public static final）
    int MAX_SIZE = 100;
    
    // 抽象方法（隐式 public abstract）
    void print();
    String getFormat();
}
```

接口的成员特点：

| 成员类型 | 修饰符 | 说明 |
|---------|--------|------|
| 常量 | `public static final` | 可以省略修饰符，必须初始化 |
| 抽象方法 | `public abstract` | 可以省略修饰符，没有方法体 |
| 默认方法 | `public default` | Java 8 引入，有方法体 |
| 静态方法 | `public static` | Java 8 引入，通过接口名调用 |
| 私有方法 | `private` | Java 9 引入，用于提取公共逻辑 |

## 接口的实现

使用 `implements` 关键字实现接口，一个类可以实现多个接口：

```java
public interface Flyable {
    void fly();
}

public interface Swimmable {
    void swim();
}

public class Duck implements Flyable, Swimmable {
    @Override
    public void fly() {
        System.out.println("鸭子在飞");
    }
    
    @Override
    public void swim() {
        System.out.println("鸭子在游泳");
    }
}
```

实现接口的规则：
- 必须实现接口中的所有抽象方法
- 实现方法必须使用 `public` 修饰
- 可以保留接口中的默认方法不变，也可以重写

## 接口与抽象类的区别

| 特性 | 接口 | 抽象类 |
|------|------|--------|
| 关键字 | `interface` | `abstract class` |
| 多继承 | 支持实现多个 | 只能继承一个 |
| 构造方法 | 没有 | 有 |
| 成员变量 | 只能是常量 | 可以有普通变量 |
| 方法 | 抽象方法、默认方法、静态方法、私有方法 | 可以有任意方法 |
| 设计理念 | "能做什么"（能力） | "是什么"（本质） |

**选择建议**：
- 当需要定义一组行为规范，且可能被多个不相关的类实现时，使用接口
- 当需要在多个相关类之间共享代码和状态时，使用抽象类
- 优先使用接口，因为接口更灵活，支持多重继承

## 默认方法（Default Method）

Java 8 引入了默认方法，允许在接口中提供方法的默认实现，解决了接口演化的问题。

### 语法

```java
public interface Vehicle {
    // 抽象方法
    void start();
    
    // 默认方法
    default void honk() {
        System.out.println("嘟嘟！");
    }
    
    default void stop() {
        System.out.println("车辆停止");
    }
}
```

### 解决接口演化问题

在 Java 8 之前，如果向接口添加新方法，所有实现类都必须修改。默认方法允许在不破坏现有实现的情况下扩展接口：

```java
// 旧版本接口
public interface Collection<E> {
    boolean add(E e);
    // ...
}

// Java 8 新增默认方法，不影响现有实现
public interface Collection<E> {
    boolean add(E e);
    
    default boolean addIfAbsent(E e) {
        // 默认实现
    }
}
```

### 默认方法冲突的解决规则

当一个类实现多个接口，且这些接口有同名默认方法时，会产生菱形继承问题。Java 的解决规则：

1. **类优先**：如果类已经重写了方法，使用类的方法
2. **接口冲突**：如果多个接口有同名默认方法，编译器报错，必须显式重写解决冲突
3. **使用 `super` 调用特定接口**：在重写方法中可以选择调用某个接口的默认方法

```java
public interface A {
    default void hello() {
        System.out.println("Hello from A");
    }
}

public interface B {
    default void hello() {
        System.out.println("Hello from B");
    }
}

// 必须显式重写解决冲突
public class C implements A, B {
    @Override
    public void hello() {
        // 选择调用 A 的默认方法
        A.super.hello();
        // 或者调用 B 的默认方法
        // B.super.hello();
        // 或者提供全新实现
    }
}
```

## 静态方法（Java 8）

Java 8 允许在接口中定义静态方法，作为工具方法：

```java
public interface MathUtils {
    static int add(int a, int b) {
        return a + b;
    }
    
    static int multiply(int a, int b) {
        return a * b;
    }
}

// 使用方式
int result = MathUtils.add(1, 2);
```

接口静态方法的特点：
- 通过接口名直接调用，不能通过实现类对象调用
- 不能被实现类继承或重写
- 适合放置与接口相关的工具方法

## 私有方法（Java 9）

Java 9 引入了接口私有方法，用于提取默认方法中的公共逻辑，避免代码重复：

```java
public interface Logger {
    default void logInfo(String message) {
        log("INFO", message);
    }
    
    default void logError(String message) {
        log("ERROR", message);
    }
    
    // 私有方法，提取公共逻辑
    private void log(String level, String message) {
        System.out.println("[" + level + "] " + message);
    }
}
```

私有方法的类型：
- **私有实例方法**：被默认方法调用
- **私有静态方法**：被静态方法和默认方法调用

## 接口的继承

接口可以继承多个接口，使用 `extends` 关键字：

```java
public interface Readable {
    void read();
}

public interface Writable {
    void write();
}

// 继承多个接口
public interface ReadWriteable extends Readable, Writable {
    void copy();
}
```

接口继承的特点：
- 接口可以继承多个接口
- 子接口继承父接口的所有方法
- 实现类需要实现所有接口（包括继承的）中的抽象方法

## 函数式接口

函数式接口是只有一个抽象方法的接口，也称为 SAM（Single Abstract Method）接口。Java 8 引入 `@FunctionalInterface` 注解来标识函数式接口：

```java
@FunctionalInterface
public interface Calculator {
    int calculate(int a, int b);
    
    // 可以有多个默认方法
    default void print() {
        System.out.println("Calculator");
    }
    
    // 可以有静态方法
    static void info() {
        System.out.println("Functional Interface");
    }
}
```

`@FunctionalInterface` 注解的作用：
- 编译时检查接口是否只有一个抽象方法
- 提高代码可读性
- 允许使用 Lambda 表达式

## Java 标准库常见函数式接口

Java 标准库在 `java.util.function` 包中提供了大量常用的函数式接口：

### Runnable 和 Callable

```java
// Runnable - 无参数无返回值
@FunctionalInterface
public interface Runnable {
    void run();
}

// Callable - 无参数有返回值
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```

### Comparator

```java
// 比较器接口
@FunctionalInterface
public interface Comparator<T> {
    int compare(T o1, T o2);
}
```

### Predicate

```java
// 断言接口，用于条件判断
@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);
    
    // 组合方法
    default Predicate<T> and(Predicate<? super T> other) { ... }
    default Predicate<T> or(Predicate<? super T> other) { ... }
    default Predicate<T> negate() { ... }
}
```

### Function

```java
// 函数接口，有输入有输出
@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
    
    default <V> Function<V, R> compose(Function<? super V, ? extends T> before) { ... }
    default <V> Function<T, V> andThen(Function<? super R, ? extends V> after) { ... }
}
```

### Supplier 和 Consumer

```java
// Supplier - 无输入，有输出（提供者）
@FunctionalInterface
public interface Supplier<T> {
    T get();
}

// Consumer - 有输入，无输出（消费者）
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
    
    default Consumer<T> andThen(Consumer<? super T> after) { ... }
}
```

常用函数式接口总结：

| 接口 | 方法 | 参数 | 返回值 | 用途 |
|------|------|------|--------|------|
| `Runnable` | `run()` | 无 | `void` | 执行任务 |
| `Callable<V>` | `call()` | 无 | `V` | 执行任务并返回结果 |
| `Comparator<T>` | `compare()` | `(T, T)` | `int` | 比较对象 |
| `Predicate<T>` | `test()` | `T` | `boolean` | 条件判断 |
| `Function<T, R>` | `apply()` | `T` | `R` | 转换/映射 |
| `Supplier<T>` | `get()` | 无 | `T` | 提供值 |
| `Consumer<T>` | `accept()` | `T` | `void` | 消费值 |
| `UnaryOperator<T>` | `apply()` | `T` | `T` | 一元操作 |
| `BinaryOperator<T>` | `apply()` | `(T, T)` | `T` | 二元操作 |

这些函数式接口是 Lambda 表达式的基础，它们使得 Java 能够支持函数式编程风格。在后续文章中，我们将深入学习 Lambda 表达式的语法和使用方法。