# Object 类与通用方法

在 Java 中，所有类都直接或间接继承自 `java.lang.Object`。Object 类是整个类层次结构的根，它定义了每个 Java 对象都具有的基本行为。理解 Object 类的核心方法，是编写高质量 Java 代码的基础。

## Object 类的地位：所有类的根类

当你定义一个类时，如果没有显式指定父类，它默认继承 Object：

```java
// 这两个定义完全等价
public class MyClass { }
public class MyClass extends Object { }
```

这意味着所有对象都拥有 Object 类的方法。即使是数组，也是 Object 的子类：

```java
int[] arr = {1, 2, 3};
System.out.println(arr instanceof Object);  // true
```

## equals() 与 hashCode()

### equals()：对象相等性判断

`equals()` 方法用于判断两个对象是否"相等"。Object 类的默认实现使用 `==` 比较引用地址：

```java
public class User {
    private String name;
    private int age;
}

User u1 = new User("张三", 25);
User u2 = new User("张三", 25);

System.out.println(u1.equals(u2));  // false，因为是两个不同的对象
```

通常我们需要重写 `equals()`，根据业务逻辑判断对象相等：

```java
public class User {
    private String name;
    private int age;
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return age == user.age && Objects.equals(name, user.name);
    }
}
```

### hashCode()：哈希码契约

`hashCode()` 返回对象的哈希码值，主要用于哈希表（HashMap、HashSet）。

**equals() 和 hashCode() 的契约**：
1. 如果 `a.equals(b)` 为 true，那么 `a.hashCode() == b.hashCode()` 必须为 true
2. 如果 `a.hashCode() == b.hashCode()`，`a.equals(b)` 不一定为 true（哈希冲突）
3. 重写 `equals()` 必须同时重写 `hashCode()`

违反这个契约会导致 HashMap、HashSet 等集合类行为异常：

```java
// 错误示例：只重写 equals 不重写 hashCode
Set<User> set = new HashSet<>();
User u1 = new User("张三", 25);
set.add(u1);

User u2 = new User("张三", 25);
System.out.println(set.contains(u2));  // 可能是 false！因为 hashCode 不同
```

正确的重写方式：

```java
@Override
public int hashCode() {
    return Objects.hash(name, age);
}
```

### Objects.equals() 工具方法

`Objects.equals()` 是 Java 7 引入的静态方法，可以安全地比较两个可能为 null 的对象：

```java
String s1 = null;
String s2 = null;

// 危险：s1 可能为 null
s1.equals(s2);

// 安全：null-safety
Objects.equals(s1, s2);  // true
```

## toString()：对象的字符串表示

`toString()` 返回对象的字符串描述。Object 类的默认实现返回类名 + 哈希码：

```java
public class User {
    private String name = "张三";
    private int age = 25;
}

User user = new User();
System.out.println(user);  // User@15db9742（无意义的默认值）
```

重写 `toString()` 可以提供有意义的调试信息：

```java
@Override
public String toString() {
    return "User{name='" + name + "', age=" + age + "}";
}

// 现在输出：User{name='张三', age=25}
```

**隐式调用场景**：当对象与字符串拼接时，`toString()` 会被自动调用：

```java
User user = new User();
String msg = "用户信息：" + user;  // 等价于 "用户信息：" + user.toString()
```

## getClass()：运行时类型信息

`getClass()` 返回对象的运行时类（Class 对象），是 `final` 方法，不能被覆盖：

```java
Object obj = "Hello";
Class<?> clazz = obj.getClass();
System.out.println(clazz.getName());    // java.lang.String
System.out.println(clazz.getSimpleName()); // String
```

### getClass() vs instanceof

`getClass()` 和 `instanceof` 都可以用于类型检查，但行为不同：

| 特性 | getClass() | instanceof |
|------|------------|------------|
| 比较方式 | 精确匹配运行时类 | 包含子类实例 |
| null 处理 | 抛出 NullPointerException | 返回 false |

```java
public class Animal { }
public class Dog extends Animal { }

Animal animal = new Dog();

// getClass()：精确匹配
System.out.println(animal.getClass() == Animal.class);  // false
System.out.println(animal.getClass() == Dog.class);      // true

// instanceof：包含子类
System.out.println(animal instanceof Animal);  // true
System.out.println(animal instanceof Dog);     // true
```

通常在 `equals()` 方法中使用 `getClass()` 进行精确比较，而使用 `instanceof` 进行类型兼容性检查。

## clone()：对象克隆

`clone()` 方法创建并返回对象的一个副本。要使用 `clone()`，类必须实现 `Cloneable` 接口：

```java
public class User implements Cloneable {
    private String name;
    private int age;
    
    @Override
    public User clone() throws CloneNotSupportedException {
        return (User) super.clone();
    }
}
```

### 浅拷贝问题

`Object.clone()` 执行的是**浅拷贝**——对于引用类型字段，只复制引用，不复制对象：

```java
public class Address {
    private String city;
}

public class User implements Cloneable {
    private String name;
    private Address address;  // 引用类型
    
    @Override
    public User clone() throws CloneNotSupportedException {
        return (User) super.clone();
    }
}

User u1 = new User("张三", new Address("北京"));
User u2 = u1.clone();

u2.getAddress().setCity("上海");
System.out.println(u1.getAddress().getCity());  // "上海"！u1 也被修改了
```

要实现深拷贝，需要手动复制引用类型的字段：

```java
@Override
public User clone() throws CloneNotSupportedException {
    User cloned = (User) super.clone();
    cloned.address = new Address(this.address.getCity());  // 手动深拷贝
    return cloned;
}
```

### 推荐的替代方案

`clone()` 方法设计存在缺陷（Joshua Bloerg 在《Effective Java》中详细分析过），推荐使用以下替代方案：

**拷贝构造器**：
```java
public class User {
    private String name;
    private Address address;
    
    // 拷贝构造器
    public User(User other) {
        this.name = other.name;
        this.address = new Address(other.address.getCity());
    }
}

User u2 = new User(u1);
```

**拷贝工厂方法**：
```java
public static User copyOf(User other) {
    return new User(other.name, new Address(other.address.getCity()));
}
```

## finalize()：已废弃

`finalize()` 方法在垃圾收集器回收对象前被调用。Java 9 已将其标记为废弃（deprecated）。

**不要使用 finalize()** 的原因：
1. **执行时机不确定**：可能永远不会被调用
2. **性能问题**：会显著降低垃圾回收效率
3. **线程安全问题**：finalize() 在单独的线程中执行
4. **更好的替代方案**：使用 `try-with-resources` 或 `Cleaner` 类

```java
// 不推荐：使用 finalize
public class MyResource {
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
}

// 推荐：使用 AutoCloseable
public class MyResource implements AutoCloseable {
    @Override
    public void close() {
        // 释放资源
    }
}

// 使用 try-with-resources
try (MyResource resource = new MyResource()) {
    // 使用资源
}  // 自动调用 close()
```

## Objects 工具类

`java.util.Objects` 是 Java 7 引入的工具类，提供了许多静态方法来安全地操作对象。

### requireNonNull()

用于参数校验，如果参数为 null 则抛出 NullPointerException：

```java
public void setName(String name) {
    this.name = Objects.requireNonNull(name, "name 不能为 null");
}

// 可以使用方法引用简化
public void setName(String name) {
    this.name = Objects.requireNonNull(name);
}
```

### isNull() 和 nonNull()

用于判空，返回 boolean：

```java
String str = null;

// 传统方式
if (str == null) { }

// 使用 Objects
if (Objects.isNull(str)) { }
if (Objects.nonNull(str)) { }
```

### hash()

生成对象的哈希码，null 安全：

```java
@Override
public int hashCode() {
    return Objects.hash(name, age, email);
}
```

`Objects.hash()` 内部处理了 null 值，不需要手动判空。

### toString()

null 安全的 toString()：

```java
String str = null;

// 危险
str.toString();  // NullPointerException

// 安全
Objects.toString(str);           // "null"
Objects.toString(str, "默认值");  // "默认值"
```

### compare()

null 安全的比较方法：

```java
String s1 = "apple";
String s2 = "banana";

int result = Objects.compare(s1, s2, Comparator.naturalOrder());
```

Object 类虽然是所有类的根，但它的方法设计体现了 Java 的核心理念。正确理解和使用这些方法，能够帮助你编写更健壮、更高效的 Java 代码。