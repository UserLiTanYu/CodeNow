# 类与对象：从现实模型到 Java 代码

面向对象编程（Object-Oriented Programming，OOP）是 Java 语言的核心范式。它不把程序看作一系列依次执行的指令，而是围绕"对象"来组织代码——每个对象既有自己的数据（状态），也有自己的行为（方法）。这种思维方式更贴近人类对现实世界的理解，因此被广泛应用于大型软件系统的开发。

## 面向对象编程的三大基本思想

### 封装

封装是将数据和操作数据的方法捆绑在一起，并对外部隐藏内部实现细节。就像一台电视机：用户只需要知道遥控器上的按钮（公开的接口），而不需要了解内部电路的运作方式。

在 Java 中，封装通过访问控制修饰符（`private`、`protected`、`public`）来实现。把字段声明为 `private`，只通过 `public` 方法暴露必要的操作，这就是封装的核心做法。

```java
public class BankAccount {
    private double balance; // 外部不能直接访问

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }
}
```

### 继承

继承允许一个类基于另一个类来构建，自动获得父类的字段和方法，并在此基础上扩展自己的特性。比如"猫"是一种"动物"，那么 `Cat` 类可以继承 `Animal` 类，自动拥有动物的基本属性，再添加猫特有的行为。

```java
public class Animal {
    protected String name;

    public void eat() {
        System.out.println(name + " is eating");
    }
}

public class Cat extends Animal {
    public void meow() {
        System.out.println("Meow!");
    }
}
```

### 多态

多态指的是同一个方法调用在不同对象上会产生不同的行为。父类引用可以指向子类对象，运行时会根据对象的实际类型来调用对应的方法。

```java
Animal animal = new Cat(); // 父类引用指向子类对象
animal.eat(); // 实际调用的是 Cat 继承的 eat()，若 Cat 重写了则调用 Cat 的版本
```

## 类的定义：字段、方法、构造器

类是创建对象的蓝图。一个类通常包含三个核心部分：

**字段（实例变量）**：描述对象的状态。每个对象都有自己的一份副本。

**方法**：描述对象的行为，可以读取和修改字段。

**构造器**：一种特殊的方法，在创建对象时被调用，用于初始化字段。构造器的名称必须与类名相同，且没有返回类型。

```java
public class Student {
    // 字段
    private String name;
    private int age;

    // 构造器
    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // 方法
    public String getName() {
        return name;
    }

    public void setAge(int age) {
        if (age > 0) {
            this.age = age;
        }
    }
}
```

## 对象的创建与使用

### new 关键字与构造器调用

创建对象使用 `new` 关键字，后跟构造器调用。`new` 做了两件事：在堆内存中分配空间，然后调用构造器进行初始化。

```java
Student stu = new Student("张三", 20);
```

上面这行代码可以拆解为：

| 步骤 | 说明 |
|------|------|
| `Student stu` | 声明一个 `Student` 类型的引用变量，存储在栈上 |
| `new Student("张三", 20)` | 在堆上分配内存，调用构造器创建对象 |
| `=` | 将堆中对象的地址赋给引用变量 `stu` |

### 对象引用

对象变量存储的不是对象本身，而是指向对象的引用（地址）。两个引用可以指向同一个对象：

```java
Student s1 = new Student("张三", 20);
Student s2 = s1; // s1 和 s2 指向同一个对象
s2.setName("李四");
System.out.println(s1.getName()); // 输出"李四"，因为是同一个对象
```

## this 关键字的用法

`this` 是一个指向当前对象的引用，有两种主要用途。

### 引用当前对象

当方法参数名与字段名相同时，用 `this` 区分二者：

```java
public void setName(String name) {
    this.name = name; // this.name 是字段，name 是参数
}
```

### 构造器间调用（this()）

一个构造器可以调用同类的另一个构造器，避免重复代码。注意 `this()` 必须是构造器中的第一条语句。

```java
public class Employee {
    private String name;
    private int age;
    private String department;

    public Employee(String name, int age) {
        this(name, age, "未分配");
    }

    public Employee(String name, int age, String department) {
        this.name = name;
        this.age = age;
        this.department = department;
    }
}
```

## 方法签名与方法重载

**方法签名**由方法名和参数列表（参数的类型和顺序）组成，不包括返回类型。Java 根据方法签名来区分不同的方法。

**方法重载（Overloading）** 是指在同一个类中定义多个同名方法，但参数列表不同。编译器会根据调用时传入的实参类型和数量，自动匹配最合适的版本。

```java
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public double add(double a, double b) {
        return a + b;
    }

    public int add(int a, int b, int c) {
        return a + b + c;
    }
}
```

```java
Calculator calc = new Calculator();
calc.add(1, 2);       // 调用第一个：3
calc.add(1.5, 2.5);   // 调用第二个：4.0
calc.add(1, 2, 3);    // 调用第三个：6
```

重载的关键是**参数列表必须不同**（类型、个数或顺序不同）。仅返回类型不同不构成重载，编译器会报错。

## 构造器重载与默认构造器

和方法一样，构造器也可以重载。当你定义了多个构造器时，调用者可以选择最合适的初始化方式。

**默认构造器**是一个特殊的无参构造器。如果你没有为类编写任何构造器，Java 编译器会自动提供一个默认的无参构造器（所有字段取默认值）。但只要你自己写了任何一个构造器，编译器就不再自动生成默认构造器。

```java
public class Point {
    private int x;
    private int y;

    // 自己写的无参构造器（此时仍可手动定义）
    public Point() {
        this(0, 0);
    }

    // 带参数的构造器
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
```

> 最佳实践：即使你暂时只需要带参数的构造器，也建议显式写一个无参构造器，因为很多框架（如 Spring、Hibernate）在反射创建对象时依赖无参构造器。

## 静态字段与静态方法

### 静态字段（类变量）

用 `static` 修饰的字段属于类本身，而非某个具体的对象。所有实例共享同一个静态字段。

```java
public class Student {
    private String name;
    private static int totalCount = 0;

    public Student(String name) {
        this.name = name;
        totalCount++;
    }

    public static int getTotalCount() {
        return totalCount;
    }
}
```

```java
new Student("张三");
new Student("李四");
System.out.println(Student.getTotalCount()); // 2
```

### 静态方法

静态方法不依赖于任何对象，通过类名直接调用。静态方法中不能使用 `this`，也不能直接访问实例字段。

```java
public class MathUtil {
    public static int max(int a, int b) {
        return a > b ? a : b;
    }
}

int result = MathUtil.max(10, 20); // 20
```

### 工厂方法模式

静态方法一个典型的应用是工厂方法，用静态方法替代构造器来创建对象。好处是方法可以有有意义的名称，并且可以控制返回的对象（如缓存、单例）。

```java
public class Boolean {
    public static Boolean valueOf(boolean b) {
        return b ? Boolean.TRUE : Boolean.FALSE;
    }
}
```

## 静态初始化块与实例初始化块

### 静态初始化块

用 `static { }` 包裹的代码块，在类第一次被加载时执行一次，通常用于初始化静态字段。

```java
public class Config {
    private static Map<String, String> settings;

    static {
        settings = new HashMap<>();
        settings.put("version", "1.0");
        settings.put("author", "码上记");
    }
}
```

### 实例初始化块

没有 `static` 关键字的 `{ }` 块，在每次创建对象时、构造器体执行之前执行。当有多个构造器需要共享初始化逻辑时，实例初始化块可以避免代码重复。

```java
public class Demo {
    private int value;

    {
        value = 42; // 每次创建对象都会执行
        System.out.println("实例初始化块执行");
    }

    public Demo() {
        System.out.println("无参构造器执行");
    }
}
```

执行顺序：静态初始化块（仅首次）→ 实例初始化块 → 构造器体。

## 访问控制修饰符

Java 提供四个访问级别，从宽到严排列如下：

| 修饰符 | 同一个类 | 同一个包 | 子类（不同包） | 任意位置 |
|--------|---------|---------|---------------|---------|
| `public` | ✔ | ✔ | ✔ | ✔ |
| `protected` | ✔ | ✔ | ✔ | ✘ |
| （默认，无修饰符） | ✔ | ✔ | ✘ | ✘ |
| `private` | ✔ | ✘ | ✘ | ✘ |

- `private`：只有本类内部可以访问，用于隐藏实现细节。
- 默认（包访问）：同一个包内的类可以访问，不加任何修饰符即可。
- `protected`：子类可以访问（即使不在同一个包），但要注意限制在继承关系中使用。
- `public`：公开的，任何地方都可以访问。

> 设计原则：尽量使用最严格的访问级别。字段应该声明为 `private`，只在需要时才逐步放宽。这就是"最小权限原则"。

## final 修饰符

`final` 关键字的含义是"不可改变"，可以修饰字段、方法和类。

### final 字段（常量）

`final` 字段在赋值后就不能再修改。通常与 `static` 一起使用来定义常量。

```java
public class MathConstants {
    public static final double PI = 3.14159265358979;
    public static final String APP_NAME = "码上记";
}
```

对于引用类型，`final` 指的是引用本身不可变，但对象的内容仍然可以修改：

```java
final List<String> list = new ArrayList<>();
list.add("hello"); // 允许，修改的是对象内容
list = new ArrayList<>(); // 编译错误，不能修改引用
```

### final 方法

`final` 方法不能被子类重写。这在需要确保某些关键行为不被改变时很有用。

```java
public class Account {
    public final double getInterestRate() {
        return 0.05;
    }
}
```

### final 类

`final` 类不能被继承。Java 标准库中的 `String`、`Integer` 等类就是 `final` 的。

```java
public final class StringUtil {
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}

// 编译错误：不能继承 final 类
public class MyStringUtil extends StringUtil { }
```

使用 `final` 的核心理由是安全性——防止子类无意中改变父类的行为，同时也让编译器有机会进行优化。
