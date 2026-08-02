# 类型擦除与桥接方法深入

Java 泛型是 JDK 5 引入的重要特性，它为开发者提供了编译期类型安全检查的能力。然而，Java 泛型的实现方式与 C++ 模板、C# 泛型有着本质区别——Java 采用**类型擦除**（Type Erasure）机制。理解类型擦除的原理及其带来的限制，是掌握 Java 泛型的关键。

## 泛型的本质：编译期糖

Java 泛型的核心设计理念是：**泛型信息仅在编译期存在，运行时被擦除**。这意味着泛型本质上是编译器提供的一种类型检查机制，而非运行时的类型特性。

```java
List<String> strings = new ArrayList<>();
List<Integer> integers = new ArrayList<>();

// 运行时，两者的类型完全相同
System.out.println(strings.getClass() == integers.getClass()); // true
```

编译器在编译阶段会进行类型检查，确保你不会向 `List<String>` 中添加 `Integer`。但在运行时，JVM 根本不知道这个 `List` 原本应该存储什么类型——它只是一个普通的 `List`。

这种设计的主要目的是**向后兼容**。泛型在 JDK 5 才引入，但 Java 需要保证旧代码（不使用泛型）能够与新代码（使用泛型）无缝协作。类型擦除使得泛型代码在运行时与非泛型代码完全一致，避免了 JVM 的大规模修改。

## 类型擦除的过程

编译器在处理泛型时，会将所有类型参数替换为其上界类型。如果没有指定上界，则默认替换为 `Object`。

**无界类型参数的擦除：**

```java
// 编译前
public class Box<T> {
    private T value;
    
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
}

// 编译后（等价于）
public class Box {
    private Object value;
    
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}
```

**有界类型参数的擦除：**

当类型参数有上界时，编译器会将类型参数替换为第一个上界类型。

```java
// 编译前
public <T extends Comparable<T> & Serializable> T min(T[] array) {
    T smallest = array[0];
    for (T element : array) {
        if (element.compareTo(smallest) < 0) {
            smallest = element;
        }
    }
    return smallest;
}

// 编译后（等价于）
public Comparable min(Comparable[] array) {
    Comparable smallest = array[0];
    for (Comparable element : array) {
        if (element.compareTo(smallest) < 0) {
            smallest = element;
        }
    }
    return smallest;
}
```

注意：编译器会选择第一个上界作为擦除后的类型。如果有多个上界（`<T extends A & B & C>`），擦除后类型为 `A`，但在调用 `B` 或 `C` 的方法时会插入强制类型转换。

## 擦除后的字节码

通过 `javap` 工具可以查看编译后的字节码，验证泛型信息确实被擦除。

```java
public class ErasureDemo<T> {
    private T value;
    
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
}
```

执行 `javap -c ErasureDemo.class` 后可以看到：

```
public class ErasureDemo<T> {
  private java.lang.Object value;
  
  public java.lang.Object getValue();
  public void setValue(java.lang.Object);
}
```

虽然类声明中仍保留 `<T>`（存储在 `Signature` 属性中供反射使用），但字段和方法的参数类型都被替换为 `Object`。编译器会在必要的地方插入 `checkcast` 指令进行类型转换：

```java
ErasureDemo<String> box = new ErasureDemo<>();
box.setValue("hello");
String s = box.getValue(); // 编译器插入 checkcast 指令
```

## 有界类型参数的擦除

当类型参数有上界时，擦除规则略有不同。类型参数会被替换为**第一个上界类型**。

```java
// T 擦除为 Number
public <T extends Number> double sum(List<T> list) {
    double total = 0;
    for (T num : list) {
        total += num.doubleValue(); // 可以调用 Number 的方法
    }
    return total;
}

// T 擦除为 Comparable
public static <T extends Comparable<T>> T max(List<T> list) {
    T result = list.get(0);
    for (T element : list) {
        if (element.compareTo(result) > 0) {
            result = element;
        }
    }
    return result;
}
```

多重上界的情况：

```java
// T 擦除为第一个上界 Serializable，但可以调用两个接口的方法
public <T extends Serializable & Comparable<T>> void process(T item) {
    // 可以调用 Serializable 的方法
    // 可以调用 Comparable 的方法
}
```

## 桥接方法（Bridge Methods）

桥接方法是编译器为了维护类型安全和多态性而自动生成的合成方法（synthetic method）。

### 为什么需要桥接方法

考虑以下继承关系：

```java
public class Node<T> {
    private T data;
    
    public void setData(T data) {
        this.data = data;
    }
    
    public T getData() {
        return data;
    }
}

public class IntegerNode extends Node<Integer> {
    @Override
    public void setData(Integer data) {
        super.setData(data);
    }
}
```

类型擦除后，`Node<T>` 变为：

```java
public class Node {
    private Object data;
    
    public void setData(Object data) { this.data = data; }
    public Object getData() { return data; }
}
```

而 `IntegerNode.setData(Integer)` 的参数类型是 `Integer`，这与父类擦除后的 `setData(Object)` 签名不同。如果直接调用多态，就会出现问题：

```java
Node node = new IntegerNode();
node.setData(42); // 期望调用 IntegerNode.setData(Integer)
```

编译器看到的是 `Node.setData(Object)`，但由于 `IntegerNode` 没有重写 `setData(Object)`，多态调用会调用到父类的 `setData(Object)`，这破坏了类型安全。

### 桥接方法的生成机制

为了解决这个问题，编译器会在 `IntegerNode` 中自动生成一个桥接方法：

```java
public class IntegerNode extends Node<Integer> {
    @Override
    public void setData(Integer data) {
        super.setData(data);
    }
    
    // 编译器生成的桥接方法
    @Override
    public void setData(Object data) {
        setData((Integer) data); // 委托给真正的方法
    }
}
```

桥接方法的签名与父类擦除后的方法签名一致（`setData(Object)`），内部则将参数转换为正确的类型后委托给子类的实际方法。这样就保证了多态调用的正确性。

### 通过反射查看桥接方法

可以使用 `Method.isBridge()` 方法判断一个方法是否为桥接方法：

```java
public class BridgeMethodDemo {
    public static void main(String[] args) {
        for (Method method : IntegerNode.class.getDeclaredMethods()) {
            System.out.println(method.getName() + 
                " | Bridge: " + method.isBridge() +
                " | ParameterType: " + method.getParameterTypes()[0].getName());
        }
    }
}
```

输出：

```
setData | Bridge: false | ParameterType: java.lang.Integer
setData | Bridge: true  | ParameterType: java.lang.Object
```

### 桥接方法的另一个场景：返回类型协变

桥接方法也出现在返回类型协变（covariant return type）的情况下：

```java
public class Base {
    public Object clone() { return new Base(); }
}

public class Derived extends Base {
    @Override
    public Derived clone() { return new Derived(); } // 返回类型更具体
}
```

编译器会在 `Derived` 中生成桥接方法：

```java
// 编译器生成
public Object clone() {
    return this.clone(); // 委托给 Derived.clone()
}
```

## 类型擦除的限制

类型擦除带来了一系列限制，理解这些限制有助于避免常见的泛型陷阱。

### 不能创建泛型实例

```java
public <T> T createInstance() {
    return new T(); // 编译错误：类型擦除后 T 变为 Object，无法实例化
}
```

**解决方案：** 通过 `Class<T>` 参数创建实例。

```java
public <T> T createInstance(Class<T> clazz) throws InstantiationException, IllegalAccessException {
    return clazz.newInstance();
}
```

### 不能 instanceof 泛型类型

```java
public <T> boolean check(Object obj) {
    return obj instanceof List<String>; // 编译错误
}
```

**原因：** 运行时 `List<String>` 和 `List<Integer>` 是同一个类型，`instanceof` 无法区分。

**解决方案：** 只能检查原始类型。

```java
public boolean check(Object obj) {
    return obj instanceof List<?>; // 可以，使用通配符
}
```

### 不能创建泛型数组

```java
public <T> T[] createArray(int size) {
    return new T[size]; // 编译错误
}
```

**原因：** 数组在运行时知道其元素类型（用于数组存储检查），但泛型类型信息已被擦除。

**解决方案：** 使用反射创建数组。

```java
@SuppressWarnings("unchecked")
public <T> T[] createArray(Class<T> componentType, int size) {
    return (T[]) Array.newInstance(componentType, size);
}
```

### 不能在静态上下文使用类型参数

```java
public class Box<T> {
    private static T value; // 编译错误
    
    public static T getValue() { // 编译错误
        return value;
    }
    
    public static <U> void method(U param) { // 合法：这是泛型方法，不是泛型类的类型参数
        // ...
    }
}
```

**原因：** 泛型类的类型参数属于实例，而静态成员属于类。静态成员在类加载时初始化，此时还没有任何实例，无法确定类型参数的具体类型。

## 泛型与数组的交互

数组和泛型在类型系统上有根本性差异：

| 特性 | 数组 | 泛型 |
|------|------|------|
| 类型检查 | 运行时（ArrayStoreException） | 编译时 |
| 协变性 | 协变（`String[]` 是 `Object[]` 的子类型） | 不变（`List<String>` 不是 `List<Object>` 的子类型） |

```java
// 数组的协变性
Object[] objects = new String[10];
objects[0] = 42; // 编译通过，运行时 ArrayStoreException

// 泛型的不变性
List<Object> objectList = new ArrayList<String>(); // 编译错误
```

数组的协变性导致运行时类型检查的开销，而泛型通过不变性在编译时就捕获了类型错误。这也是为什么不能创建泛型数组的根本原因——如果允许 `new List<String>[10]`，由于数组协变性，它可以赋值给 `List<Object>[]`，进而添加 `List<Integer>`，破坏类型安全。

## 运行时泛型信息的获取：TypeToken 模式

虽然类型擦除会移除大多数泛型信息，但某些泛型信息仍然保留在字节码中：

1. **类声明的泛型参数**：`class MyClass<T>` 中的 `T`
2. **字段、方法签名中的泛型类型**：存储在 `Signature` 属性中
3. **父类/父接口的泛型参数**：`extends Comparable<String>` 中的 `String`

可以通过反射获取父类的泛型参数：

```java
public class StringList extends ArrayList<String> {
    // ...
}

// 获取泛型信息
Type superclass = StringList.class.getGenericSuperclass();
if (superclass instanceof ParameterizedType) {
    ParameterizedType pt = (ParameterizedType) superclass;
    Type[] typeArgs = pt.getActualTypeArguments();
    System.out.println(typeArgs[0]); // class java.lang.String
}
```

### TypeToken 模式

TypeToken（类型令牌）是 Guava 引入的经典模式，用于在运行时捕获泛型类型：

```java
public abstract class TypeToken<T> {
    private final Type type;
    
    protected TypeToken() {
        // 获取匿名子类的父类泛型参数
        Type superclass = getClass().getGenericSuperclass();
        ParameterizedType pt = (ParameterizedType) superclass;
        this.type = pt.getActualTypeArguments()[0];
    }
    
    public Type getType() { return type; }
}

// 使用方式
TypeToken<List<String>> token = new TypeToken<List<String>>() {};
System.out.println(token.getType()); // java.util.List<java.lang.String>
```

**原理：** 匿名子类 `new TypeToken<List<String>>() {}` 会保留父类的泛型参数信息。通过反射可以获取到 `List<String>` 这个完整的泛型类型。

这种模式广泛应用于需要运行时泛型信息的场景，如 JSON 序列化、依赖注入框架等。

```java
// Jackson 中的用法
ObjectMapper mapper = new ObjectMapper();
List<String> list = mapper.readValue(json, new TypeReference<List<String>>() {});
```

理解类型擦除及其限制，能够帮助开发者更好地使用泛型，在遇到编译错误时快速定位原因，并在需要运行时类型信息时采用正确的模式来获取。
