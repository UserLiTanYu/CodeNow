# 通配符与类型擦除：深入理解 Java 泛型机制

上一篇文章介绍了泛型类和泛型方法的基础知识。本文将深入探讨两个重要概念：通配符（Wildcards）和类型擦除（Type Erasure）。理解这两个概念是掌握 Java 泛型编程的关键。

## 通配符的概念

通配符使用问号 `?` 表示，代表未知类型。通配符主要用于方法参数中，增加 API 的灵活性。

```java
// 通配符声明
List<?> unknownList;  // 可以持有任意类型的 List
```

通配符与类型参数的区别：
- 类型参数 `<T>`：用于声明泛型类、接口或方法
- 通配符 `<?>`：用于使用泛型时，表示"某种类型"

## 上界通配符：? extends T

上界通配符 `? extends T` 表示"未知类型，但必须是 T 或 T 的子类"。它用于读取数据，适用于"生产者"场景。

```java
// 声明一个上界通配符参数
public double sumOfList(List<? extends Number> list) {
    double sum = 0;
    for (Number num : list) {
        sum += num.doubleValue();
    }
    return sum;
}
```

使用示例：

```java
List<Integer> integers = Arrays.asList(1, 2, 3);
List<Double> doubles = Arrays.asList(1.1, 2.2, 3.3);

// 都可以传入
double sum1 = sumOfList(integers);  // 6.0
double sum2 = sumOfList(doubles);   // 6.6
```

### 上界通配符的限制：只读

使用 `? extends T` 时，不能向集合中添加元素（除了 `null`）：

```java
public void addNumber(List<? extends Number> list) {
    list.add(1);        // 编译错误！
    list.add(1.0);      // 编译错误！
    list.add(null);     // 只能添加 null
}
```

为什么不能添加？因为编译器不知道 `?` 到底是什么类型：
- 如果传入 `List<Integer>`，添加 `Double` 会出错
- 如果传入 `List<Double>`，添加 `Integer` 会出错

编译器为了保证类型安全，禁止添加任何非 `null` 元素。

### 上界通配符的读取

虽然不能写入，但可以安全地读取为上界类型：

```java
public Number getFirst(List<? extends Number> list) {
    return list.get(0);  // 安全：无论实际类型是什么，都可以向上转型为 Number
}
```

## 下界通配符：? super T

下界通配符 `? super T` 表示"未知类型，但必须是 T 或 T 的父类"。它用于写入数据，适用于"消费者"场景。

```java
// 声明一个下界通配符参数
public void addIntegers(List<? super Integer> list) {
    list.add(1);
    list.add(2);
    list.add(3);
}
```

使用示例：

```java
List<Number> numbers = new ArrayList<>();
List<Object> objects = new ArrayList<>();

// 都可以传入
addIntegers(numbers);  // 正确：Number 是 Integer 的父类
addIntegers(objects);  // 正确：Object 是 Integer 的父类
```

### 下界通配符的限制：读取受限

使用 `? super T` 时，读取只能得到 `Object` 类型：

```java
public Object getFirst(List<? super Integer> list) {
    return list.get(0);  // 只能返回 Object，因为不知道具体类型
}
```

### 下界通配符的写入

可以安全地添加 `T` 类型或 `T` 的子类型：

```java
public void addAll(List<? super Integer> dest, List<Integer> src) {
    for (Integer item : src) {
        dest.add(item);  // 安全：dest 至少能接受 Integer
    }
}
```

## 无界通配符

无界通配符 `?` 表示"任意类型"，与使用 `Object` 不同。

```java
// 无界通配符
public void printList(List<?> list) {
    for (Object item : list) {
        System.out.println(item);
    }
}
```

### 无界通配符 vs Object

```java
// 使用 Object
public void processObject(List<Object> list) {
    // 只能接受 List<Object>
}

// 使用无界通配符
public void processAny(List<?> list) {
    // 可以接受任意类型的 List
}

List<String> strings = Arrays.asList("a", "b");
processObject(strings);  // 编译错误！
processAny(strings);     // 正确！
```

无界通配符的使用场景：
- 只关心集合的存在，不关心元素类型
- 只读取元素为 `Object`
- 实现与类型无关的通用方法

## PECS 原则详解

PECS 是 "Producer Extends, Consumer Super" 的缩写，由 Joshua Bloch 在《Effective Java》中提出。

### 原则说明

| 场景 | 通配符 | 说明 |
|------|--------|------|
| 生产者（Producer） | `? extends T` | 从集合中读取 T 类型数据 |
| 消费者（Consumer） | `? super T` | 向集合中写入 T 类型数据 |
| 既是生产者又是消费者 | 不使用通配符 | 需要具体类型 |

### 实际应用示例

```java
// 生产者：从 src 读取，写入 dest
public static <T> void copy(List<? super T> dest, List<? extends T> src) {
    for (T item : src) {
        dest.add(item);
    }
}
```

这个方法的签名完美体现了 PECS 原则：
- `src` 是生产者，使用 `extends`，从中读取元素
- `dest` 是消费者，使用 `super`，向其写入元素

### 集合框架中的 PECS

Java 标准库中大量使用了 PECS 原则：

```java
// Collections.copy 的签名
public static <T> void copy(List<? super T> dest, List<? extends T> src)

// Collections.addAll 的签名
public static <T> boolean addAll(Collection<? super T> c, T... elements)
```

## 通配符捕获

通配符捕获（Wildcard Capture）是指编译器在某些情况下可以"捕获"通配符的具体类型。

```java
public static void swap(List<?> list, int i, int j) {
    swapHelper(list, i, j);  // 通过辅助方法捕获通配符
}

private static <T> void swapHelper(List<T> list, int i, int j) {
    T temp = list.get(i);
    list.set(i, list.get(j));
    list.set(j, temp);
}
```

为什么需要通配符捕获？

```java
// 直接这样写会编译错误
public static void swap(List<?> list, int i, int j) {
    ? temp = list.get(i);  // 编译错误！不能声明 ? 类型的变量
    list.set(i, list.get(j));
    list.set(j, temp);
}
```

通配符捕获的原理：
1. `swapHelper` 方法将 `?` 捕获为类型参数 `T`
2. 在 `swapHelper` 内部，可以正常操作 `T` 类型
3. 编译器保证类型安全

## 类型擦除

Java 泛型是一个**编译期特性**，在运行时会进行类型擦除（Type Erasure）。这意味着泛型类型信息在运行时是不可用的。

### 类型擦除的过程

编译器在编译时会：
1. 将泛型类型参数替换为其上界（如果没有指定上界，则替换为 `Object`）
2. 插入必要的类型转换
3. 生成桥接方法（如果有需要）

```java
// 编译前
public class Box<T> {
    private T content;

    public T getContent() {
        return content;
    }
}

// 编译后（类型擦除后）
public class Box {
    private Object content;

    public Object getContent() {
        return content;
    }
}
```

### 有界类型参数的擦除

```java
// 编译前
public class NumberBox<T extends Number> {
    private T number;

    public T getNumber() {
        return number;
    }
}

// 编译后
public class NumberBox {
    private Number number;

    public Number getNumber() {
        return number;
    }
}
```

类型参数被替换为其上界 `Number`，而不是 `Object`。

### 类型擦除的影响

由于类型擦除的存在，以下操作是不允许的：

**1. 不能实例化类型参数**

```java
public class Box<T> {
    private T content;

    public Box() {
        this.content = new T();  // 编译错误！运行时不知道 T 是什么
    }
}
```

替代方案：使用工厂模式或 `Supplier`

```java
public class Box<T> {
    private T content;

    public Box(Supplier<T> supplier) {
        this.content = supplier.get();
    }
}

Box<String> box = new Box<>(String::new);
```

**2. 不能使用 instanceof 检查泛型类型**

```java
public <T> boolean isType(Object obj) {
    return obj instanceof T;  // 编译错误！
}

// 替代方案：传递 Class 对象
public <T> boolean isType(Object obj, Class<T> type) {
    return type.isInstance(obj);
}
```

**3. 不能创建泛型数组**

```java
public <T> T[] createArray(int size) {
    return new T[size];  // 编译错误！
}

// 替代方案：使用 Array.newInstance
@SuppressWarnings("unchecked")
public <T> T[] createArray(Class<T> componentType, int size) {
    return (T[]) Array.newInstance(componentType, size);
}
```

## 桥接方法

桥接方法（Bridge Methods）是编译器为保证多态性而生成的合成方法。

### 问题场景

```java
public class Node<T> {
    private T data;

    public void setData(T data) {
        this.data = data;
    }
}

public class IntegerNode extends Node<Integer> {
    @Override
    public void setData(Integer data) {
        super.setData(data);
    }
}
```

类型擦除后：

```java
public class Node {
    private Object data;

    public void setData(Object data) {
        this.data = data;
    }
}

public class IntegerNode extends Node {
    // 这个方法的签名是 setData(Integer)
    // 但父类擦除后是 setData(Object)
    // 问题：多态性被破坏了！
    public void setData(Integer data) {
        super.setData(data);
    }
}
```

### 编译器的解决方案

编译器会生成一个桥接方法：

```java
public class IntegerNode extends Node {
    // 程序员写的方法
    public void setData(Integer data) {
        super.setData(data);
    }

    // 编译器生成的桥接方法
    @Override
    public void setData(Object data) {
        setData((Integer) data);  // 调用上面的方法
    }
}
```

桥接方法的特征：
- 编译器自动生成
- 返回类型和参数类型与父类擦除后一致
- 内部进行类型转换并调用实际方法

## 泛型的局限性总结

| 局限性 | 原因 | 替代方案 |
|--------|------|----------|
| 不能使用基本类型 | 泛型参数必须是引用类型 | 使用包装类：`List<Integer>` 而非 `List<int>` |
| 不能实例化类型参数 | 运行时类型擦除 | 使用 `Supplier<T>` 或反射 |
| 不能创建泛型数组 | 数组需要知道元素类型 | 使用 `ArrayList<T>` 或 `Array.newInstance` |
| 不能在静态上下文使用类型参数 | 静态成员属于类，类型参数属于实例 | 使用泛型方法 |
| 不能 instanceof 泛型类型 | 运行时类型信息已擦除 | 传递 `Class<T>` 对象 |
| 不能抛出或捕获泛型异常 | 异常处理需要确切类型 | 使用泛型包装异常 |

### 不能在静态上下文使用类型参数

```java
public class Box<T> {
    private T content;

    // 编译错误！静态方法不能使用类的类型参数
    public static T getContent(Box<T> box) {
        return box.content;
    }

    // 正确：泛型方法有自己的类型参数
    public static <U> U getFirst(List<U> list) {
        return list.get(0);
    }
}
```

## 小结

本文深入探讨了 Java 泛型的两个核心概念：

**通配符**
- `? extends T`：上界通配符，只读，适用于生产者
- `? super T`：下界通配符，只写，适用于消费者
- PECS 原则是使用通配符的指导方针

**类型擦除**
- Java 泛型是编译期特性，运行时类型信息被擦除
- 类型擦除导致的限制：不能实例化类型参数、不能创建泛型数组等
- 编译器通过桥接方法保证多态性

理解这些底层机制，有助于编写正确的泛型代码，避免常见的陷阱。下一篇文章将介绍泛型在实际项目中的应用和设计模式。
