# 抽象类、多态与类型转换

多态是面向对象编程最强大的特性之一，它让代码能够"一个接口，多种实现"。抽象类为多态提供了基础，而类型转换则是使用多态时必须掌握的技能。本文将深入探讨这些核心概念。

## 抽象类（abstract class）

### 抽象方法

用 `abstract` 关键字修饰的方法称为抽象方法。抽象方法只有方法签名，没有方法体：

```java
public abstract class Shape {
    // 抽象方法：只有签名，没有实现
    public abstract double area();
    public abstract double perimeter();
}
```

### 抽象类的规则

1. **不能实例化**：抽象类不能创建对象
2. **子类必须实现抽象方法**：除非子类也是抽象类
3. **可以有构造器**：供子类调用初始化
4. **可以有非抽象方法**：提供默认实现

```java
public abstract class Shape {
    private String color;
    
    // 构造器
    public Shape(String color) {
        this.color = color;
    }
    
    // 抽象方法
    public abstract double area();
    
    // 非抽象方法：提供默认实现
    public String getColor() {
        return color;
    }
    
    public void display() {
        System.out.println(color + "的面积是：" + area());
    }
}

public class Circle extends Shape {
    private double radius;
    
    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }
    
    @Override
    public double area() {
        return Math.PI * radius * radius;
    }
}

// Shape shape = new Shape("红色");  // 编译错误！不能实例化抽象类
Shape shape = new Circle("红色", 5.0);  // 正确：通过子类实例化
```

### 抽象类 vs 普通类 vs 接口

| 特性 | 普通类 | 抽象类 | 接口 |
|------|--------|--------|------|
| 实例化 | 可以 | 不能 | 不能 |
| 抽象方法 | 不能有 | 可以有 | 默认就是抽象的 |
| 构造器 | 有 | 有 | 无 |
| 字段 | 任意 | 任意 | 只能是常量 |
| 多继承 | 不支持 | 不支持 | 支持多实现 |
| 设计意图 | 具体实现 | 部分实现 + 模板 | 行为契约 |

**选择场景**：

- **普通类**：有完整的实现，可以直接使用
- **抽象类**：有共同的状态和行为，但子类需要自定义部分实现（模板模式）
- **接口**：定义能力契约，与实现无关（可插拔、可组合）

## 多态（Polymorphism）

多态的字面意思是"多种形态"。在 Java 中，多态体现在：**同一个引用类型，使用不同的实例而执行不同的操作**。

### 运行时动态绑定

多态的核心机制是**动态绑定**（也叫晚期绑定）。编译时，编译器只检查引用类型是否有该方法；运行时，JVM 根据对象的实际类型决定调用哪个方法：

```java
public abstract class Animal {
    public abstract void makeSound();
}

public class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("汪汪！");
    }
}

public class Cat extends Animal {
    @Override
    public void makeSound() {
        System.out.println("喵喵！");
    }
}

// 编译时类型是 Animal，运行时类型是 Dog 或 Cat
Animal animal = new Dog();
animal.makeSound();  // 输出：汪汪！

animal = new Cat();
animal.makeSound();  // 输出：喵喵！
```

**方法调用的绑定过程**：

1. 编译器检查引用类型 `Animal` 是否有 `makeSound()` 方法
2. 运行时，JVM 查找对象实际类型（Dog 或 Cat）的方法实现
3. 调用实际类型的方法

## 向上转型（Upcasting）

向上转型是自动的、安全的——子类对象赋值给父类引用：

```java
Dog dog = new Dog();
Animal animal = dog;  // 自动向上转型

// 也可以直接写
Animal animal = new Dog();
```

向上转型的特点：

| 特性 | 说明 |
|------|------|
| 安全性 | 完全安全，无需显式转换 |
| 自动性 | 编译器自动完成 |
| 方法访问 | 只能调用引用类型（父类）中声明的方法 |

```java
Animal animal = new Dog();

animal.makeSound();  // ✓ 可以调用 Animal 中声明的方法
// animal.fetch();   // 编译错误！Animal 类没有 fetch() 方法
```

向上转型会"丢失"子类特有的方法，但这正是多态的价值——**统一接口，隐藏实现细节**。

## 向下转型（Downcasting）

向下转型需要显式转换，将父类引用转回子类类型：

```java
Animal animal = new Dog();

// Dog dog = animal;  // 编译错误！需要显式转换
Dog dog = (Dog) animal;  // 正确：向下转型
dog.fetch();  // 现在可以调用 Dog 特有的方法
```

向下转型是**不安全的**，如果转换类型不匹配，会抛出 `ClassCastException`：

```java
Animal animal = new Cat();

try {
    Dog dog = (Dog) animal;  // 运行时异常！Cat 不能转为 Dog
} catch (ClassCastException e) {
    System.out.println("类型转换失败！");
}
```

## instanceof 运算符

在向下转型前，应该使用 `instanceof` 检查对象的实际类型：

```java
Animal animal = getAnimal();  // 可能返回任意类型的 Animal

if (animal instanceof Dog) {
    Dog dog = (Dog) animal;
    dog.fetch();
} else if (animal instanceof Cat) {
    Cat cat = (Cat) animal;
    cat.purr();
}
```

### Java 16+ 模式匹配

Java 16 引入了 `instanceof` 的模式匹配，简化了类型检查和转换的代码：

```java
// 传统写法
if (animal instanceof Dog) {
    Dog dog = (Dog) animal;
    dog.fetch();
}

// Java 16+ 模式匹配
if (animal instanceof Dog dog) {
    dog.fetch();  // 直接使用 dog 变量
}

// 还可以在同一表达式中使用
if (animal instanceof Dog dog && dog.isTrained()) {
    dog.fetch();
}
```

## 多态的经典应用

### 集合中存储不同子类对象

多态最常见的应用是在集合中存储不同类型的对象：

```java
List<Animal> animals = new ArrayList<>();
animals.add(new Dog());
animals.add(new Cat());
animals.add(new Bird());

// 统一接口调用
for (Animal animal : animals) {
    animal.makeSound();  // 每个动物发出自己的声音
}
```

### 统一接口调用

```java
public class AnimalDoctor {
    // 接收任何 Animal 类型
    public void examine(Animal animal) {
        System.out.println("检查中...");
        animal.makeSound();  // 多态调用
    }
    
    // 返回父类类型，实际返回子类对象
    public Animal createPet(String type) {
        if ("dog".equals(type)) {
            return new Dog();
        } else if ("cat".equals(type)) {
            return new Cat();
        }
        throw new IllegalArgumentException("未知类型：" + type);
    }
}
```

### 策略模式

```java
public interface SortStrategy {
    void sort(int[] array);
}

public class BubbleSort implements SortStrategy {
    @Override
    public void sort(int[] array) { /* 冒泡排序实现 */ }
}

public class QuickSort implements SortStrategy {
    @Override
    public void sort(int[] array) { /* 快速排序实现 */ }
}

public class Sorter {
    private SortStrategy strategy;
    
    public void setStrategy(SortStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void sort(int[] array) {
        strategy.sort(array);  // 多态调用
    }
}

// 使用
Sorter sorter = new Sorter();
sorter.setStrategy(new QuickSort());
sorter.sort(data);
```

## 继承的设计原则

### 优先使用组合而非继承

继承会创建强耦合关系。在很多情况下，组合是更好的选择：

```java
// 继承方式：Dog is an Animal
public class Dog extends Animal {
    // 继承了 Animal 的所有方法
}

// 组合方式：Dog has an AnimalBehavior
public class Dog {
    private AnimalBehavior behavior;  // 组合
    
    public Dog(AnimalBehavior behavior) {
        this.behavior = behavior;
    }
    
    public void makeSound() {
        behavior.makeSound();
    }
}
```

**组合的优势**：
1. **松耦合**：不依赖父类的具体实现
2. **灵活性**：可以在运行时更换行为
3. **可测试性**：更容易 mock 测试

**使用继承的场景**：
- 子类是父类的真正特化（is-a 关系）
- 需要覆盖父类的方法
- 子类与父类在同一个包中

### Liskov 替换原则（LSP）

LSP 是面向对象设计的核心原则之一：**子类必须能够替换父类，而程序的行为不变**。

违反 LSP 的经典例子——正方形继承矩形：

```java
public class Rectangle {
    protected int width;
    protected int height;
    
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int width) {
        this.width = width;
        this.height = width;  // 正方形宽高相等
    }
    
    @Override
    public void setHeight(int height) {
        this.width = height;
        this.height = height;
    }
}

// 使用
Rectangle rect = new Square();
rect.setWidth(5);
rect.setHeight(3);
System.out.println(rect.area());  // 期望 15，实际 9！违反了 LSP
```

LSP 的要求：
1. 子类不能加强前置条件
2. 子类不能削弱后置条件
3. 子类必须保持父类的不变量

## 密封类（Sealed Classes，Java 17）

密封类限制了哪些类可以继承它，提供了更精确的类型控制：

```java
public sealed class Shape permits Circle, Rectangle, Triangle {
    // ...
}

// 被允许的子类必须是 final、sealed 或 non-sealed
public final class Circle extends Shape {
    // final：不能再被继承
}

public sealed class Rectangle extends Shape permits Square {
    // sealed：只能被指定的类继承
}

public non-sealed class Triangle extends Shape {
    // non-sealed：开放继承
}
```

密封类的价值：

1. **模式匹配的完整性检查**：编译器可以检查 switch 是否覆盖了所有子类

```java
String describe(Shape shape) {
    return switch (shape) {
        case Circle c -> "圆形：" + c.radius();
        case Rectangle r -> "矩形：" + r.width() + "x" + r.height();
        case Triangle t -> "三角形";
        // 如果漏掉某个子类，编译器会报错
    };
}
```

2. **设计意图明确**：明确表达类层次结构是封闭的

3. **与记录类（Record）配合**：

```java
public sealed interface Shape permits Circle, Rectangle {
    record Circle(double radius) implements Shape { }
    record Rectangle(double width, double height) implements Shape { }
}
```

多态、抽象类和类型转换是 Java 面向对象编程的核心。掌握这些概念，你就能设计出灵活、可扩展的代码结构。记住：多态让代码更通用，抽象让设计更清晰，而类型安全让程序更健壮。