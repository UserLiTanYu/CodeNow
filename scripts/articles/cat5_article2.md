# Lambda 表达式与方法引用

## Lambda 表达式简介

Lambda 表达式是 Java 8 引入的重要特性，它提供了一种简洁的方式来表示可传递的匿名函数。Lambda 表达式使得 Java 能够支持函数式编程，大大简化了代码编写，特别是在处理集合、事件处理和并发编程等场景。

## Lambda 表达式的语法

Lambda 表达式的基本语法由三部分组成：参数列表、箭头符号和方法体。

### 完整形式

```java
(参数类型 参数名, 参数类型 参数名) -> {
    方法体;
    return 返回值;
}
```

### 省略形式

Java 编译器可以根据上下文推断类型，因此可以省略部分语法：

```java
// 1. 省略参数类型
(a, b) -> { return a + b; }

// 2. 单参数省略括号
x -> x * x

// 3. 单语句省略大括号和 return
(a, b) -> a + b

// 4. 无参数
() -> System.out.println("Hello")
```

### 各种形式示例

```java
// 无参数，无返回值
Runnable r1 = () -> System.out.println("Hello");

// 单参数，无返回值
Consumer<String> c1 = s -> System.out.println(s);

// 多参数，有返回值
Comparator<Integer> comp = (a, b) -> a - b;

// 多语句，有返回值
Function<String, Integer> f1 = s -> {
    int length = s.length();
    return length * 2;
};
```

## Lambda 表达式的类型

Lambda 表达式的类型是函数式接口（只有一个抽象方法的接口）。编译器会根据上下文推断 Lambda 表达式对应的函数式接口类型：

```java
// 编译器推断类型为 Runnable
Runnable r = () -> System.out.println("Hello");

// 编译器推断类型为 Comparator<Integer>
Comparator<Integer> comp = (a, b) -> a - b;

// 编译器推断类型为 Predicate<String>
Predicate<String> pred = s -> s.length() > 5;
```

Lambda 表达式可以赋值给以下类型的变量：
- 函数式接口类型
- 使用 `@FunctionalInterface` 注解的接口
- 隐式满足函数式接口条件的接口（只有一个抽象方法）

## Lambda 表达式中的变量捕获

Lambda 表达式可以访问外部作用域的变量，但有一些限制：

### Effectively Final

Lambda 表达式只能访问 **effectually final** 的局部变量，即变量在初始化后不再被修改：

```java
public void example() {
    int x = 10; // effectively final
    
    Runnable r = () -> {
        System.out.println(x); // 可以访问
        // x = 20; // 编译错误，不能修改
    };
    
    // 如果修改了变量，就不是 effectively final
    // x = 20; // 如果取消注释，上面的 Lambda 会编译错误
}
```

### 为什么有这个限制？

局部变量存储在栈上，Lambda 表达式可能在另一个线程中执行。为了确保线程安全，Java 要求 Lambda 捕获的变量不能被修改。实际上，Lambda 捕获的是变量的副本，而不是引用。

### 可以访问的变量类型

- **局部变量**：必须是 effectively final
- **实例变量**：可以访问和修改（存储在堆上）
- **静态变量**：可以访问和修改

```java
public class LambdaScope {
    private int instanceVar = 10;
    private static int staticVar = 20;
    
    public void test() {
        int localVar = 30; // effectively final
        
        Runnable r = () -> {
            // localVar = 40; // 编译错误
            System.out.println(localVar); // 可以访问
            
            instanceVar = 15; // 可以修改实例变量
            staticVar = 25;   // 可以修改静态变量
        };
    }
}
```

## 方法引用

方法引用是 Lambda 表达式的简写形式，当 Lambda 表达式只是调用一个已有方法时，可以使用方法引用。方法引用通过 `::` 操作符实现。

### 四种方法引用形式

#### 1. 类::静态方法

```java
// Lambda 表达式
Function<Double, Double> sqrt1 = x -> Math.sqrt(x);

// 方法引用
Function<Double, Double> sqrt2 = Math::sqrt;

// 另一个例子
Comparator<Integer> comp1 = (a, b) -> Integer.compare(a, b);
Comparator<Integer> comp2 = Integer::compare;
```

#### 2. 对象::实例方法

```java
// Lambda 表达式
String str = "Hello";
Supplier<Integer> len1 = () -> str.length();

// 方法引用
Supplier<Integer> len2 = str::length;

// 另一个例子
Consumer<String> printer1 = s -> System.out.println(s);
Consumer<String> printer2 = System.out::println;
```

#### 3. 类::实例方法

```java
// Lambda 表达式
Predicate<String> isEmpty1 = s -> s.isEmpty();

// 方法引用
Predicate<String> isEmpty2 = String::isEmpty;

// 另一个例子
BiPredicate<String, String> startsWith1 = (s, prefix) -> s.startsWith(prefix);
BiPredicate<String, String> startsWith2 = String::startsWith;
```

#### 4. 类::new（构造器引用）

```java
// Lambda 表达式
Supplier<ArrayList> list1 = () -> new ArrayList();

// 方法引用
Supplier<ArrayList> list2 = ArrayList::new;

// 另一个例子
Function<String, StringBuilder> sb1 = s -> new StringBuilder(s);
Function<String, StringBuilder> sb2 = StringBuilder::new;

// 数组构造器引用
Function<Integer, int[]> array1 = len -> new int[len];
Function<Integer, int[]> array2 = int[]::new;
```

### 方法引用总结表

| 形式 | 语法 | 示例 | 说明 |
|------|------|------|------|
| 静态方法引用 | `类名::静态方法` | `Math::sqrt` | 参数作为方法的参数 |
| 实例方法引用 | `对象::实例方法` | `System.out::println` | 参数作为方法的参数 |
| 特定类的实例方法引用 | `类名::实例方法` | `String::isEmpty` | 第一个参数作为调用对象 |
| 构造器引用 | `类名::new` | `ArrayList::new` | 参数作为构造器参数 |

## Lambda 与匿名内部类的区别

Lambda 表达式和匿名内部类都可以用来实现函数式接口，但有重要区别：

| 特性 | Lambda 表达式 | 匿名内部类 |
|------|--------------|-----------|
| **编译方式** | 编译为私有方法，使用 `invokedynamic` 指令 | 编译为单独的 `.class` 文件 |
| **this 指向** | 指向外部类 | 指向匿名内部类实例 |
| **作用域** | 独立作用域 | 有自己独立的作用域 |
| **类型** | 必须是函数式接口 | 可以是任何接口或抽象类 |
| **性能** | 首次调用有开销，后续调用快 | 每次创建新实例 |

```java
public class LambdaVsAnonymous {
    private String name = "Outer";
    
    public void test() {
        // 匿名内部类
        Runnable r1 = new Runnable() {
            private String name = "Inner";
            
            @Override
            public void run() {
                System.out.println(this.name); // 输出 "Inner"
            }
        };
        
        // Lambda 表达式
        Runnable r2 = () -> {
            System.out.println(this.name); // 输出 "Outer"
        };
        
        r1.run();
        r2.run();
    }
}
```

## Lambda 的使用场景

### 1. 集合排序

```java
List<String> names = Arrays.asList("Charlie", "Alice", "Bob");

// 使用 Lambda
names.sort((a, b) -> a.compareTo(b));

// 使用方法引用
names.sort(String::compareTo);

// 使用 Comparator 工厂方法
names.sort(Comparator.naturalOrder());
```

### 2. 事件处理

```java
// 传统方式
button.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Button clicked");
    }
});

// Lambda 方式
button.addActionListener(e -> System.out.println("Button clicked"));
```

### 3. 线程创建

```java
// 传统方式
Thread t1 = new Thread(new Runnable() {
    @Override
    public void run() {
        System.out.println("Running");
    }
});

// Lambda 方式
Thread t2 = new Thread(() -> System.out.println("Running"));
```

### 4. Stream 操作

```java
List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David");

// 过滤、转换、收集
List<String> result = names.stream()
    .filter(s -> s.length() > 3)
    .map(String::toUpperCase)
    .sorted()
    .collect(Collectors.toList());

// 遍历
names.forEach(System.out::println);
```

## Comparator 的链式比较

Java 8 为 `Comparator` 接口添加了许多实用的默认方法和静态方法，支持链式比较：

### comparing() 方法

```java
// 按单个字段排序
Comparator<Person> byName = Comparator.comparing(Person::getName);
Comparator<Person> byAge = Comparator.comparing(Person::getAge);

// 使用
people.sort(byName);
people.sort(byAge);
```

### thenComparing() 方法

```java
// 先按姓名排序，姓名相同再按年龄排序
Comparator<Person> comp = Comparator
    .comparing(Person::getName)
    .thenComparing(Person::getAge);

// 按多个字段排序
Comparator<Person> multiComp = Comparator
    .comparing(Person::getLastName)
    .thenComparing(Person::getFirstName)
    .thenComparing(Person::getAge);
```

### reversed() 方法

```java
// 升序
Comparator<Person> asc = Comparator.comparing(Person::getAge);

// 降序
Comparator<Person> desc = Comparator.comparing(Person::getAge).reversed();

// 先按年龄降序，再按姓名升序
Comparator<Person> comp = Comparator
    .comparing(Person::getAge).reversed()
    .thenComparing(Person::getName);
```

### 处理 null 值

```java
// null 值排在前面
Comparator<Person> nullsFirst = Comparator
    .comparing(Person::getName, Comparator.nullsFirst(String::compareTo));

// null 值排在后面
Comparator<Person> nullsLast = Comparator
    .comparing(Person::getName, Comparator.nullsLast(String::compareTo));
```

### 完整示例

```java
public class ComparatorDemo {
    public static void main(String[] args) {
        List<Person> people = Arrays.asList(
            new Person("Alice", 25, "New York"),
            new Person("Bob", 30, "Boston"),
            new Person("Charlie", 25, "Chicago"),
            new Person("Alice", 20, "Denver")
        );
        
        // 先按姓名升序，再按年龄降序，最后按城市升序
        Comparator<Person> comp = Comparator
            .comparing(Person::getName)
            .thenComparing(Person::getAge, Comparator.reverseOrder())
            .thenComparing(Person::getCity);
        
        people.sort(comp);
        people.forEach(System.out::println);
    }
}
```

Lambda 表达式和方法引用是 Java 函数式编程的基础，它们与 Stream API 结合使用，能够写出简洁、高效的代码。在实际开发中，应根据具体情况选择使用 Lambda 表达式还是方法引用，当只是简单调用已有方法时，优先使用方法引用。