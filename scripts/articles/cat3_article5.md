# static 关键字与单例模式

`static` 是 Java 中最常用的关键字之一，用于定义属于类而非实例的成员。理解 `static` 的本质，是掌握单例模式、工厂方法等设计模式的基础。

## static 字段（类变量）

`static` 字段属于类本身，所有实例共享同一份数据：

```java
public class User {
    private String name;
    private static int userCount = 0;  // 类变量
    
    public User(String name) {
        this.name = name;
        userCount++;  // 每创建一个实例，计数加一
    }
    
    public static int getUserCount() {
        return userCount;
    }
}

User u1 = new User("张三");
User u2 = new User("李四");
System.out.println(User.getUserCount());  // 2
```

### 内存分配时机

`static` 字段在**类加载时**分配内存，而非创建实例时。整个 JVM 中只有一份。

| 特性 | 实例变量 | 静态变量 |
|------|----------|----------|
| 所属 | 对象实例 | 类本身 |
| 内存位置 | 堆（Heap） | 方法区（Metaspace） |
| 生命周期 | 随对象创建/销毁 | 随类加载/卸载 |
| 访问方式 | `obj.field` | `ClassName.field` |
| 共享性 | 每个实例独立 | 所有实例共享 |

## static 方法

`static` 方法属于类，不依赖于任何实例，因此**不能访问 `this` 和实例成员**：

```java
public class MathUtils {
    // 静态方法：工具方法
    public static int max(int a, int b) {
        return a > b ? a : b;
    }
    
    public static int min(int a, int b) {
        return a < b ? a : b;
    }
    
    // 错误示例
    private int value;
    
    public static void badMethod() {
        // System.out.println(this.value);  // 编译错误：不能访问 this
        // System.out.println(value);        // 编译错误：不能访问实例变量
    }
}

// 调用方式
int result = MathUtils.max(10, 20);
```

### 工具方法模式

Java 标准库中大量使用 `static` 方法作为工具方法：

```java
// java.lang.Math
Math.random();      // 随机数
Math.max(a, b);     // 最大值
Math.abs(x);        // 绝对值

// java.util.Collections
Collections.sort(list);           // 排序
Collections.unmodifiableList(list);  // 不可变包装

// java.util.Arrays
Arrays.sort(array);
Arrays.binarySearch(array, key);
Arrays.copyOf(original, newLength);
```

## static 初始化块

`static` 初始化块在类加载时执行，用于初始化静态资源：

```java
public class Cache {
    private static Map<String, Object> cache;
    
    // 静态初始化块
    static {
        cache = new HashMap<>();
        cache.put("version", "1.0.0");
        cache.put("startTime", LocalDateTime.now());
        System.out.println("Cache 类加载完成，静态缓存初始化");
    }
}
```

多个静态块按顺序执行：

```java
public class MultiStaticBlock {
    static { System.out.println("第一个静态块"); }
    static { System.out.println("第二个静态块"); }
    static { System.out.println("第三个静态块"); }
}

// 输出：
// 第一个静态块
// 第二个静态块
// 第三个静态块
```

## static 内部类

`static` 内部类不持有外部类的引用，可以独立于外部类实例存在：

```java
public class LinkedList<E> {
    private Node<E> head;
    
    // 静态内部类：不依赖外部类实例
    private static class Node<E> {
        E data;
        Node<E> next;
        
        Node(E data, Node<E> next) {
            this.data = data;
            this.next = next;
        }
    }
}
```

| 内部类类型 | 是否持有外部类引用 | 能否访问外部类实例成员 |
|------------|-------------------|----------------------|
| 普通内部类 | 是 | 能 |
| static 内部类 | 否 | 不能 |

`static` 内部类适合只在外部类中使用的辅助类，如 `Map.Entry`、`Builder` 等。

## static final 常量

```java
public class Constants {
    // 编译期常量（值在编译时确定）
    public static final int MAX_SIZE = 100;
    public static final String APP_NAME = "CodeNow";
    
    // 运行时常量（值在运行时确定）
    public static final LocalDateTime START_TIME = LocalDateTime.now();
    public static final UUID INSTANCE_ID = UUID.randomUUID();
}
```

| 类型 | 示例 | 特点 |
|------|------|------|
| 编译期常量 | `static final int X = 10` | 编译时内联，修改需重新编译所有引用 |
| 运行时常量 | `static final List<T> list = ...` | 类加载时初始化，引用不可变但内容可能可变 |

注意：`static final` 引用类型只保证引用不变，不保证对象内容不变：

```java
public static final List<String> NAMES = new ArrayList<>();
// NAMES = newList;        // 编译错误：引用不可变
// NAMES.add("张三");      // 可以！对象内容可变
```

如果需要真正的不可变集合：

```java
public static final List<String> NAMES = List.of("张三", "李四", "王五");
// NAMES.add("赵六");  // 抛出 UnsupportedOperationException
```

## 工厂方法模式

工厂方法是 `static` 方法的重要应用，用于替代构造器创建对象：

```java
// 构造器方式
LocalDate date = new LocalDate(2026, 8, 1);  // 假设可以

// 工厂方法方式（推荐）
LocalDate date = LocalDate.of(2026, 8, 1);
LocalDate date = LocalDate.now();
LocalDate date = LocalDate.parse("2026-08-01");

// Java 标准库中的工厂方法
List<String> list = List.of("a", "b", "c");
Set<Integer> set = Set.of(1, 2, 3);
Map<String, Integer> map = Map.of("key", 1);
Integer num = Integer.valueOf(42);
```

工厂方法的优势：

| 优势 | 说明 |
|------|------|
| 有意义的名称 | `LocalDate.of()` 比 `new LocalDate()` 更清晰 |
| 可复用实例 | `Integer.valueOf()` 缓存 -128~127 |
| 返回子类 | 可以返回接口的实现类 |
| 参数灵活性 | 可以接受不同类型参数 |

## 单例模式

单例模式确保一个类只有一个实例，并提供全局访问点。以下是五种常见实现方式。

### 饿汉式

```java
public class Singleton {
    // 类加载时就创建实例
    private static final Singleton INSTANCE = new Singleton();
    
    // 私有构造器，防止外部创建
    private Singleton() {}
    
    public static Singleton getInstance() {
        return INSTANCE;
    }
}
```

优点：实现简单，线程安全（JVM 保证类加载过程线程安全）
缺点：无论是否使用都会创建实例，可能浪费资源

### 懒汉式（synchronized）

```java
public class Singleton {
    private static Singleton instance;
    
    private Singleton() {}
    
    // 每次调用都加锁，性能差
    public static synchronized Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}
```

优点：延迟加载
缺点：每次调用都需要同步，性能开销大

### 双重检查锁定（DCL）

```java
public class Singleton {
    // volatile 禁止指令重排序
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {              // 第一次检查（无锁）
            synchronized (Singleton.class) {
                if (instance == null) {      // 第二次检查（有锁）
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

`volatile` 的作用：`new Singleton()` 实际上分三步执行：
1. 分配内存
2. 初始化对象
3. 将引用指向内存

步骤 2 和 3 可能被重排序，导致其他线程看到未初始化完成的对象。`volatile` 禁止这种重排序。

### 静态内部类方式（推荐）

```java
public class Singleton {
    private Singleton() {}
    
    // 静态内部类，只有在被引用时才会加载
    private static class SingletonHolder {
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
```

优点：
- 延迟加载（内部类在 `getInstance()` 调用时才加载）
- 线程安全（JVM 类加载机制保证）
- 无锁，性能好

### 枚举方式（Effective Java 推荐）

```java
public enum Singleton {
    INSTANCE;
    
    // 可以添加字段和方法
    private int count;
    
    public void doSomething() {
        count++;
        System.out.println("操作次数：" + count);
    }
}

// 使用
Singleton.INSTANCE.doSomething();
```

优点：
- 实现最简洁
- 天然线程安全
- 自动防止序列化创建新实例
- 自动防止反射攻击

## 各种单例实现对比

| 实现方式 | 延迟加载 | 线程安全 | 防序列化 | 防反射 | 性能 |
|----------|----------|----------|----------|--------|------|
| 饿汉式 | 否 | 是 | 否 | 否 | 高 |
| 懒汉式（synchronized） | 是 | 是 | 否 | 否 | 低 |
| DCL | 是 | 是 | 否 | 否 | 高 |
| 静态内部类 | 是 | 是 | 否 | 否 | 高 |
| 枚举 | 否 | 是 | 是 | 是 | 高 |

### 序列化问题

除了枚举方式，其他单例在反序列化时都会创建新实例，破坏单例：

```java
// 解决方案：添加 readResolve 方法
public class Singleton implements Serializable {
    private static final Singleton INSTANCE = new Singleton();
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        return INSTANCE;
    }
    
    // 反序列化时返回同一个实例
    private Object readResolve() {
        return INSTANCE;
    }
}
```

### 反射攻击

除了枚举方式，其他单例都可以通过反射创建新实例：

```java
// 解决方案：在构造器中检查
private Singleton() {
    if (SingletonHolder.INSTANCE != null) {
        throw new IllegalStateException("单例实例已存在");
    }
}
```

## 选择建议

| 场景 | 推荐方式 |
|------|----------|
| 简单项目，资源占用小 | 饿汉式 |
| 需要延迟加载 | 静态内部类 |
| 需要防序列化/反射 | 枚举 |
| Android 开发 | DCL 或静态内部类 |
