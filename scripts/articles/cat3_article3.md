# 对象的生命周期与设计原则

在 Java 中，对象从诞生到消亡经历一个完整的生命周期。理解这个过程以及相关的内存模型，能帮助你写出更高效、更健壮的代码。此外，掌握 `equals()`、`hashCode()`、`toString()` 这些基础方法的正确用法，是每个 Java 开发者的基本功。

## 对象的创建过程

当你写下 `new User("张三", 25)` 时，JVM 内部经历了三个步骤：

### 第一步：内存分配

JVM 在堆内存中为新对象分配一块空间。所有实例变量都会被分配存储空间，但此时字段会被设置为**默认值**。

### 第二步：字段默认值初始化

在构造器执行之前，所有字段先被赋予默认值：

| 类型 | 默认值 |
|------|--------|
| `byte`、`short`、`int`、`long` | `0` |
| `float`、`double` | `0.0` |
| `boolean` | `false` |
| `char` | `'\u0000'`（空字符） |
| 引用类型（对象、数组） | `null` |

### 第三步：构造器执行

按照代码中的赋值语句和构造器逻辑，用实际值覆盖默认值。

```java
public class User {
    private String name;
    private int age;
    private boolean active;

    public User(String name, int age) {
        // 此时 name=null, age=0, active=false（默认值已就绪）
        this.name = name;  // 覆盖为"张三"
        this.age = age;    // 覆盖为 25
        this.active = true; // 覆盖为 true
    }
}
```

如果存在继承链，执行顺序是：父类构造器 → 子类的实例初始化块 → 子类构造器。

## 栈内存与堆内存

Java 的内存管理分为两个核心区域，理解它们对排查性能问题至关重要：

**栈内存（Stack）**：每个线程有自己的调用栈。局部变量（基本类型和对象引用）存储在栈上，方法结束时自动弹出释放。栈的分配和回收速度极快。

**堆内存（Heap）**：所有线程共享。对象实例（通过 `new` 创建的）存储在堆上，需要垃圾回收器来回收。

```java
public void createUser() {
    int age = 25;           // age 是局部变量，存在栈上
    User user = new User("张三", age); 
    // "张三" 对象存在堆上
    // user 引用变量存在栈上，值是堆中对象的地址
}
```

可以用一张表来对比：

| 特性 | 栈内存 | 堆内存 |
|------|--------|--------|
| 存储内容 | 局部变量、方法调用帧 | 对象实例、数组 |
| 生命周期 | 方法结束自动释放 | GC 回收时释放 |
| 速度 | 快 | 相对慢 |
| 线程安全 | 每个线程独立，天然安全 | 共享，需要同步控制 |
| 大小 | 较小（默认 256KB~1MB） | 较大（默认物理内存的 1/4） |

## 对象的销毁与垃圾回收

Java 不需要像 C/C++ 那样手动释放内存。当一个对象不再被任何引用指向时，它就变成了垃圾回收器（Garbage Collector，GC）的回收候选。

```java
User user = new User("张三", 25);
user = null; // "张三" 对象不再有引用指向它，等待 GC 回收
```

GC 的工作原理简化来说就是：周期性地扫描堆内存，找到所有从"根对象"（线程栈中的引用、静态变量等）不可达的对象，然后回收它们占用的内存。

现代 JVM 的 GC 算法非常成熟（如 G1、ZGC），绝大多数情况下你不需要关心垃圾回收的细节。但有两条原则值得注意：

1. **不要创建不必要的对象**。比如在循环中反复创建 `String` 对象或 `SimpleDateFormat` 实例，会给 GC 带来压力。
2. **不要手动调用 `System.gc()`**。这只是一个"建议"，JVM 不保证立即执行，而且可能打乱 GC 的自适应调度。

## equals() 方法：自定义相等性

`Object` 类的默认 `equals()` 方法比较的是引用是否相同（即是否为同一个对象），这通常不是你想要的。两个不同的 `User` 对象，如果 `id` 相同，业务上应该被视为"相等"。

### 重写 equals() 的契约

Java 对 `equals()` 方法有严格的契约要求，违反这些契约会导致集合框架行为异常：

| 契约性质 | 含义 |
|---------|------|
| 自反性 | `x.equals(x)` 必须返回 `true` |
| 对称性 | `x.equals(y)` 为 `true`，则 `y.equals(x)` 也必须为 `true` |
| 传递性 | `x.equals(y)` 且 `y.equals(z)`，则 `x.equals(z)` 必须为 `true` |
| 一致性 | 多次调用 `x.equals(y)` 结果不变（前提是对象未被修改） |
| 非空性 | `x.equals(null)` 必须返回 `false` |

### 正确的重写模板

```java
public class User {
    private Long id;
    private String name;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        User user = (User) other;
        return Objects.equals(id, user.id);
    }
}
```

几个要点：
- 参数类型必须是 `Object`，否则是重载而非重写。
- 用 `getClass()` 而不是 `instanceof` 来判断类型，确保对称性（`User` 和它的子类不应该相等）。
- 使用 `Objects.equals()` 来安全地比较可能为 `null` 的字段。

## hashCode() 与 equals() 的一致性

这是 Java 面试中的经典问题，也是实际开发中容易踩坑的地方。

**契约**：如果两个对象 `equals()` 返回 `true`，它们的 `hashCode()` 必须相等。反过来不一定成立——`hashCode()` 相等不代表对象相等（哈希冲突）。

**为什么必须遵守这个契约？** 因为 `HashMap`、`HashSet` 等哈希集合先用 `hashCode()` 定位桶，再用 `equals()` 判断是否为同一个元素。如果两个 `equals` 的对象 `hashCode` 不同，它们可能被放进不同的桶，导致集合中出现"重复"元素。

```java
@Override
public int hashCode() {
    return Objects.hash(id);
}
```

> 黄金法则：**重写 `equals()` 必须同时重写 `hashCode()`**。现代 IDE 都提供了一键生成这两个方法的功能，建议直接使用。

用 `HashMap` 来演示问题：

```java
User u1 = new User(1L, "张三");
User u2 = new User(1L, "张三");

Map<User, String> map = new HashMap<>();
map.put(u1, "value");

// 如果只重写了 equals 没重写 hashCode：
map.get(u2); // 返回 null！因为 u2 落在了不同的桶

// 正确重写两个方法后：
map.get(u2); // 返回 "value"
```

## toString() 方法

`Object` 的默认 `toString()` 返回类似 `User@1a2b3c4d` 这样的格式，基本没有可读性。重写 `toString()` 能让对象在日志、调试、字符串拼接时表现得更友好。

```java
@Override
public String toString() {
    return "User{id=" + id + ", name='" + name + "'}";
}
```

```java
User user = new User(1L, "张三");
System.out.println(user);         // User{id=1, name='张三'}
System.out.println("用户: " + user); // 用户: User{id=1, name='张三'}
```

JDK 15+ 提供了 `record` 类型，自动生成 `toString()`、`equals()` 和 `hashCode()`，可以省去手写这些样板代码的麻烦：

```java
public record User(Long id, String name) {}
// 自动生成了 toString(): User[id=1, name=张三]
```

## 对象克隆：浅拷贝与深拷贝

有时你需要复制一个对象。Java 提供了 `Cloneable` 接口和 `clone()` 方法。

### 浅拷贝

默认的 `clone()` 是浅拷贝：对于基本类型字段，复制值；对于引用类型字段，只复制引用（两个对象指向同一个子对象）。

```java
public class Team implements Cloneable {
    private String name;
    private List<String> members;

    @Override
    public Team clone() {
        try {
            return (Team) super.clone(); // 浅拷贝
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
```

```java
Team t1 = new Team("A队", new ArrayList<>(List.of("张三")));
Team t2 = t1.clone();
t2.getMembers().add("李四");
System.out.println(t1.getMembers()); // [张三, 李四] —— t1 也被改了！
```

### 深拷贝

深拷贝需要手动递归复制所有引用类型的字段：

```java
@Override
public Team clone() {
    try {
        Team cloned = (Team) super.clone();
        cloned.members = new ArrayList<>(this.members); // 复制集合
        return cloned;
    } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
    }
}
```

深拷贝后的 `t2` 修改 `members` 不会再影响 `t1`。

> 实际开发中，`clone()` 的使用并不广泛。更常见的做法是通过拷贝构造器、拷贝工厂方法或序列化/反序列化来实现对象复制。

## Java 编码最佳实践

### 不可变对象设计

不可变对象是指创建后状态不能被修改的对象。`String`、`Integer`、`LocalDate` 等类都是不可变的。

设计不可变类的规则：
1. 所有字段声明为 `private final`
2. 不提供修改字段的 setter 方法
3. 类声明为 `final`（防止子类破坏不可变性）
4. 如果字段是可变对象（如 `List`），在构造时做深拷贝

```java
public final class Money {
    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

不可变对象的优势：
- **线程安全**：不需要同步，天然可以在多线程间共享
- **安全可靠**：不会被意外修改，可以放心作为 Map 的 key 或 Set 的元素
- **易于推理**：代码中看到一个不可变对象的引用，就知道它的值不会变

### 优先使用组合而非继承

继承虽然方便，但它创建了一种强耦合关系。子类和父类紧密绑定，父类的任何改动都可能影响子类的行为。Joshua Bloch 在《Effective Java》中给出了明确的建议：**优先使用组合（composition）而非继承（inheritance）**。

```java
// 继承方式（耦合强）
public class EnhancedArrayList extends ArrayList<String> {
    public void addTwice(String s) {
        add(s);
        add(s);
    }
}

// 组合方式（耦合弱，推荐）
public class StringList {
    private final List<String> list = new ArrayList<>();

    public void add(String s) {
        list.add(s);
    }

    public void addTwice(String s) {
        list.add(s);
        list.add(s);
    }

    public List<String> asUnmodifiable() {
        return Collections.unmodifiableList(list);
    }
}
```

组合的好处是你可以精确控制对外暴露哪些行为，内部实现也可以随时替换。只有当两个类之间确实存在"is-a"关系（猫 is-a 动物）且父类是为继承而设计的时，才考虑使用继承。

### 最小化可变性

可变状态是 bug 的温床。在设计类时，应该遵循"最小化可变性"的原则：

- 能用 `final` 的字段就用 `final`
- 能用不可变对象就用不可变对象
- 能用局部变量就不要用实例字段
- 集合字段暴露时返回不可修改视图：`Collections.unmodifiableList(list)`
- 减少对象中可以被外部修改的状态数量

这些原则并非要求你把所有类都设计成不可变的，而是提醒你在做设计决策时，优先选择更安全、更简单的方案。可变状态越少，代码中的意外就越少。
