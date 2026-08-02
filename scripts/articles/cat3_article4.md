# 构造器详解与对象初始化顺序

构造器（Constructor）是 Java 中创建对象的关键机制。理解构造器的工作原理和对象初始化顺序，对于编写健壮的 Java 程序至关重要。

## 构造器的基本语法

构造器是一种特殊的方法，用于创建并初始化对象。它有两个显著特征：

1. **与类同名**
2. **没有返回类型**（连 `void` 都没有）

```java
public class User {
    private String name;
    private int age;
    
    // 构造器
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

// 使用
User user = new User("张三", 25);
```

`new User("张三", 25)` 的执行过程：
1. 在堆内存中分配空间
2. 初始化字段为默认值（`null`、`0`、`false`）
3. 执行构造器体中的代码
4. 返回对象的引用

## 默认构造器

### 编译器何时自动生成

如果一个类**没有定义任何构造器**，编译器会自动提供一个无参构造器（默认构造器）：

```java
public class Animal {
    // 没有定义构造器
    // 编译器自动生成：public Animal() {}
}

Animal a = new Animal();  // 可以调用
```

### 何时不生成

一旦定义了**任何一个构造器**，编译器就**不再自动生成**无参构造器：

```java
public class Animal {
    private String name;
    
    // 定义了有参构造器
    public Animal(String name) {
        this.name = name;
    }
}

// Animal a = new Animal();  // 编译错误！无参构造器不存在
Animal a = new Animal("Tom");  // 正确
```

**最佳实践**：如果定义了有参构造器，通常应该手动提供一个无参构造器，尤其是需要被框架（如 Hibernate、MyBatis）使用的实体类。

## 构造器重载

一个类可以有多个构造器，通过参数列表区分：

```java
public class Student {
    private String name;
    private int age;
    private String school;
    
    // 无参构造器
    public Student() {
        this("未知", 0, "未知");
    }
    
    // 一个参数
    public Student(String name) {
        this(name, 0, "未知");
    }
    
    // 两个参数
    public Student(String name, int age) {
        this(name, age, "未知");
    }
    
    // 三个参数（主构造器）
    public Student(String name, int age, String school) {
        this.name = name;
        this.age = age;
        this.school = school;
    }
}
```

### this() 语法

构造器之间可以通过 `this(...)` 相互调用，避免代码重复：

```java
public Student() {
    this("未知", 0, "未知");  // 调用三参构造器
}

public Student(String name) {
    this(name, 0, "未知");    // 调用三参构造器
}
```

规则：
- `this(...)` 必须是构造器中的**第一条语句**
- 不能形成循环调用（如 A 调 B，B 调 A）
- 一个构造器最多只能调用一次 `this(...)`

## 构造器链：super()

子类构造器必须调用父类构造器，形成构造器链：

```java
public class Person {
    private String name;
    
    public Person() {
        this("未知");
    }
    
    public Person(String name) {
        this.name = name;
    }
}

public class Employee extends Person {
    private String company;
    
    public Employee() {
        super();  // 调用父类无参构造器
        this.company = "未知";
    }
    
    public Employee(String name, String company) {
        super(name);  // 调用父类有参构造器
        this.company = company;
    }
}
```

规则：
- 子类构造器的第一条语句必须是 `super(...)` 或 `this(...)`
- 如果没有显式调用 `super(...)`，编译器会自动插入 `super()`（调用父类无参构造器）
- 如果父类没有无参构造器，子类必须显式调用 `super(参数)`

```java
public class Animal {
    private String name;
    
    public Animal(String name) {  // 没有无参构造器
        this.name = name;
    }
}

public class Dog extends Animal {
    public Dog(String name) {
        super(name);  // 必须显式调用，否则编译错误
    }
}
```

## 参数化构造器：构造器注入

构造器注入是依赖注入的一种方式，Spring 框架推荐的方式：

```java
@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final UserService userService;
    
    // 构造器注入（推荐）
    public ArticleService(ArticleRepository articleRepository, UserService userService) {
        this.articleRepository = articleRepository;
        this.userService = userService;
    }
}
```

优势：
- 依赖明确，代码可读性高
- 字段可以声明为 `final`，保证不可变
- 不依赖框架注解，便于单元测试

## 对象初始化的完整顺序

这是 Java 中最复杂的初始化机制之一：

```java
public class InitializationOrder {
    // 1. 静态字段
    private static int staticField = initStaticField();
    
    // 2. 静态初始化块
    static {
        System.out.println("2. 静态初始化块");
    }
    
    // 3. 实例字段
    private int instanceField = initInstanceField();
    
    // 4. 实例初始化块
    {
        System.out.println("4. 实例初始化块");
    }
    
    // 5. 构造器
    public InitializationOrder() {
        System.out.println("5. 构造器体");
    }
    
    private static int initStaticField() {
        System.out.println("1. 静态字段初始化");
        return 0;
    }
    
    private int initInstanceField() {
        System.out.println("3. 实例字段初始化");
        return 0;
    }
}
```

输出顺序：
```
1. 静态字段初始化
2. 静态初始化块
3. 实例字段初始化
4. 实例初始化块
5. 构造器体
```

### 完整的初始化顺序（含继承）

```java
public class Parent {
    static { System.out.println("1. 父类静态块"); }
    { System.out.println("3. 父类实例块"); }
    public Parent() { System.out.println("4. 父类构造器"); }
}

public class Child extends Parent {
    static { System.out.println("2. 子类静态块"); }
    { System.out.println("5. 子类实例块"); }
    public Child() { System.out.println("6. 子类构造器"); }
}

// new Child() 的输出：
// 1. 父类静态块
// 2. 子类静态块
// 3. 父类实例块
// 4. 父类构造器
// 5. 子类实例块
// 6. 子类构造器
```

| 阶段 | 执行内容 | 执行次数 |
|------|----------|----------|
| 类加载 | 父类静态字段和静态块 | 一次 |
| 类加载 | 子类静态字段和静态块 | 一次 |
| 对象创建 | 父类实例字段和实例块 | 每次 new |
| 对象创建 | 父类构造器 | 每次 new |
| 对象创建 | 子类实例字段和实例块 | 每次 new |
| 对象创建 | 子类构造器 | 每次 new |

## 静态初始化块 vs 实例初始化块

### 静态初始化块

```java
public class DatabaseConfig {
    private static Map<String, String> config;
    
    static {
        config = new HashMap<>();
        config.put("url", "jdbc:mysql://localhost:3306/codenow");
        config.put("username", "root");
        config.put("password", "123456");
    }
}
```

特点：
- 在类加载时执行，只执行一次
- 可以访问静态成员，不能访问实例成员
- 适合初始化静态资源（配置、常量表等）

### 实例初始化块

```java
public class User {
    private List<String> permissions;
    
    {
        permissions = new ArrayList<>();
        permissions.add("read");
        permissions.add("write");
    }
    
    public User() {}
    public User(String role) {
        if ("admin".equals(role)) {
            permissions.add("delete");
        }
    }
}
```

特点：
- 每次创建对象时执行，在构造器之前执行
- 可以访问实例成员和静态成员
- 适合多个构造器共享的初始化逻辑

## 不可变对象的构造器设计

不可变对象（Immutable Object）一旦创建就不能修改，`String`、`Integer`、`LocalDate` 都是不可变类。

设计要点：

```java
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money(BigDecimal amount, Currency currency) {
        // 验证参数
        if (amount == null || currency == null) {
            throw new IllegalArgumentException("金额和货币不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负");
        }
        
        // 防御性复制（防止外部修改）
        this.amount = amount.stripTrailingZeros();
        this.currency = currency;
    }
    
    // 只提供 getter，不提供 setter
    public BigDecimal getAmount() {
        return amount;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    // 返回新对象而非修改自身
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("货币类型不同");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

不可变对象设计清单：

| 要点 | 说明 |
|------|------|
| 类声明为 `final` | 防止子类破坏不可变性 |
| 字段声明为 `private final` | 禁止外部访问和重新赋值 |
| 不提供 setter 方法 | 禁止修改状态 |
| 深拷贝可变参数 | 防御性复制 |
| 深拷贝返回值 | 防止内部状态泄露 |
| 修改操作返回新对象 | 不改变原对象 |

这种设计使得不可变对象天然线程安全，可以放心地在多线程环境中共享。
