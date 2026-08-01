# 继承机制与 super 关键字

继承是面向对象编程的三大支柱之一，它让代码复用变得简单而优雅。在 Java 中，继承通过 `extends` 关键字实现，而 `super` 关键字则用于访问父类的成员。掌握继承机制是理解 Java 类体系的基础。

## 继承的概念：代码复用与 is-a 关系

继承的核心思想是**代码复用**。假设我们要设计一个员工管理系统，有普通员工和经理两种角色。如果不使用继承，两个类中会有大量重复的字段和方法：

```java
public class Employee {
    private String name;
    private double salary;
    
    public String getName() { return name; }
    public double getSalary() { return salary; }
    public void work() { System.out.println("工作中..."); }
}

public class Manager {
    private String name;
    private double salary;
    private double bonus;  // 经理特有：奖金
    
    public String getName() { return name; }
    public double getSalary() { return salary; }
    public void work() { System.out.println("管理团队..."); }
}
```

使用继承后，子类自动拥有父类的字段和方法，只需定义自己特有的部分：

```java
public class Employee {
    private String name;
    private double salary;
    
    public String getName() { return name; }
    public double getSalary() { return salary; }
    public void work() { System.out.println("工作中..."); }
}

public class Manager extends Employee {
    private double bonus;  // 只需定义特有字段
    
    public double getBonus() { return bonus; }
    public double getTotalSalary() { return getSalary() + bonus; }
}
```

继承体现了 **is-a**（是一个）关系：Manager is an Employee。这种关系是单向的——经理是员工，但员工不一定是经理。

## extends 关键字

`extends` 关键字用于声明继承关系：

```java
public class 子类名 extends 父类名 {
    // 子类特有的字段和方法
}
```

子类会继承父类中所有非私有的字段和方法。私有字段虽然被继承（存在于对象内存中），但子类无法直接访问，需要通过公共的 getter/setter 方法。

```java
public class Animal {
    private String name;
    protected int age;
    
    public String getName() { return name; }
    public void eat() { System.out.println(name + "在吃东西"); }
}

public class Dog extends Animal {
    private String breed;
    
    public void bark() {
        System.out.println(getName() + "汪汪叫");  // 通过 getter 访问私有字段
        System.out.println(age + "岁了");           // protected 字段可直接访问
    }
}
```

**继承的传递性**：如果 A extends B，B extends C，那么 A 也继承了 C 的成员（C 是 A 的间接父类）。

## super 关键字

`super` 关键字有两个主要用途：调用父类构造器和访问父类成员。

### 调用父类构造器

```java
public class Employee {
    private String name;
    private double salary;
    
    public Employee(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }
}

public class Manager extends Employee {
    private double bonus;
    
    public Manager(String name, double salary, double bonus) {
        super(name, salary);  // 必须放在第一行
        this.bonus = bonus;
    }
}
```

**重要规则**：`super(...)` 调用必须是构造器的第一条语句。如果子类构造器没有显式调用 `super(...)`，编译器会自动插入 `super()`（无参构造器调用）。

### 访问父类方法和字段

当子类覆盖了父类的方法时，可以用 `super` 调用父类版本：

```java
public class Animal {
    public void makeSound() {
        System.out.println("动物发出声音");
    }
}

public class Dog extends Animal {
    @Override
    public void makeSound() {
        super.makeSound();  // 先调用父类方法
        System.out.println("汪汪汪！");
    }
}
```

也可以用 `super` 访问被子类隐藏的父类字段（虽然这种情况较少见）：

```java
public class Parent {
    protected int value = 10;
}

public class Child extends Parent {
    private int value = 20;
    
    public void printValues() {
        System.out.println("子类 value: " + value);          // 20
        System.out.println("父类 value: " + super.value);    // 10
    }
}
```

## 子类构造器的调用链

创建子类对象时，构造器的调用链是从最顶层的父类开始，逐级向下执行：

```java
public class GrandParent {
    public GrandParent() {
        System.out.println("GrandParent 构造器");
    }
}

public class Parent extends GrandParent {
    public Parent() {
        // 隐式调用 super()
        System.out.println("Parent 构造器");
    }
}

public class Child extends Parent {
    public Child() {
        // 隐式调用 super()
        System.out.println("Child 构造器");
    }
}

// new Child() 的输出：
// GrandParent 构造器
// Parent 构造器
// Child 构造器
```

这个设计确保了：**在子类构造器执行前，父类已经完成初始化**。子类可能依赖父类的字段，所以父类必须先构造完成。

## 方法覆盖（Override）

方法覆盖是指子类重新实现父类中已有的方法。覆盖需要遵循严格的规则：

| 规则 | 说明 |
|------|------|
| 方法签名相同 | 方法名、参数列表必须完全一致 |
| 返回类型协变 | 返回类型可以是父类返回类型的子类型 |
| 访问权限不能更严格 | 父类 protected → 子类可以 public，但不能 private |
| 不能抛出更多受检异常 | 可以抛出更少或更具体的异常 |

```java
public class Animal {
    protected Animal reproduce() {
        return new Animal();
    }
    
    protected void eat() throws Exception {
        // ...
    }
}

public class Dog extends Animal {
    @Override
    public Dog reproduce() {  // 协变返回类型：Dog 是 Animal 的子类
        return new Dog();
    }
    
    @Override
    public void eat() throws RuntimeException {  // RuntimeException 是非受检异常，可以抛出
        // ...
    }
}
```

## @Override 注解

`@Override` 注解不是必须的，但强烈建议使用。它让编译器帮你检查覆盖是否正确：

```java
public class Dog extends Animal {
    @Override  // 告诉编译器：这个方法是覆盖父类的
    public void makeSound() {
        System.out.println("汪汪！");
    }
    
    // 如果拼写错误，编译器会报错
    @Override
    public void MakeSound() {  // 编译错误：父类没有这个方法
        // ...
    }
}
```

没有 `@Override`，如果你拼错方法名，编译器会认为你定义了一个新方法，而不是覆盖父类方法。这是一个常见的 bug 来源。

## final 方法不能被覆盖

用 `final` 修饰的方法不能被子类覆盖：

```java
public class Account {
    public final double getInterestRate() {
        return 0.05;
    }
}

public class SavingsAccount extends Account {
    @Override  // 编译错误！final 方法不能被覆盖
    public double getInterestRate() {
        return 0.08;
    }
}
```

`final` 方法的意义在于：父类确定某个方法的实现不应该被改变，确保行为的一致性。`private` 方法隐式地是 `final` 的，也不能被覆盖（子类无法看到 private 方法）。

## protected 访问级别在继承中的意义

Java 的四种访问级别中，`protected` 专门为继承设计：

| 修饰符 | 同类 | 同包 | 子类 | 其他包 |
|--------|------|------|------|--------|
| private | ✓ | ✗ | ✗ | ✗ |
| 默认(包私有) | ✓ | ✓ | ✗ | ✗ |
| protected | ✓ | ✓ | ✓ | ✗ |
| public | ✓ | ✓ | ✓ | ✓ |

`protected` 成员可以被子类访问，即使子类在不同的包中：

```java
// 包 com.example.base
package com.example.base;

public class Base {
    protected int value = 10;
    protected void doSomething() { /* ... */ }
}

// 包 com.example.sub
package com.example.sub;
import com.example.base.Base;

public class Sub extends Base {
    public void test() {
        System.out.println(value);    // ✓ 子类可以访问 protected 成员
        doSomething();                // ✓ 子类可以调用 protected 方法
    }
}
```

但要注意：**protected 不意味着完全公开**。在非子类中，即使在同一个包内，也不能通过子类实例访问父类的 protected 成员（除非是同包访问的权限）。

## Java 单继承的限制与解决方案

Java 只支持单继承——一个类只能有一个直接父类。这个限制是为了避免"菱形继承"问题（C++ 的多继承会产生二义性）。

单继承的限制有时会带来不便。例如，一个类既需要序列化，又需要克隆能力：

```java
// 错误！Java 不支持多继承
public class MyClass extends Serializable, Cloneable {
    // ...
}
```

Java 的解决方案是**接口多实现**：

```java
public class MyClass implements Serializable, Cloneable, Comparable<MyClass> {
    @Override
    public MyClass clone() throws CloneNotSupportedException {
        return (MyClass) super.clone();
    }
    
    @Override
    public int compareTo(MyClass other) {
        // ...
    }
}
```

接口允许多实现，因为接口只定义方法签名，不包含状态（Java 8 之后接口可以有 default 方法，但仍然没有实例字段）。这既获得了多继承的灵活性，又避免了菱形继承的复杂性。

**继承 vs 接口的选择**：

- **继承**：表达"是一个"关系，共享实现代码
- **接口**：表达"能做什么"能力，定义行为契约

```java
// 继承：Dog is an Animal
public class Dog extends Animal { }

// 接口：Dog can swim
public class Dog extends Animal implements Swimmable {
    @Override
    public void swim() { /* ... */ }
}
```

理解继承机制是掌握 Java 面向对象编程的关键一步。合理使用继承可以让代码结构清晰、易于维护，但过度使用继承会导致类层次过深、耦合过紧。在设计时，始终牢记"优先使用组合而非继承"的原则。