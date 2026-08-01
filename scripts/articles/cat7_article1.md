# 泛型类与泛型方法：Java 泛型编程入门

泛型（Generics）是 Java 5 引入的重要特性，它允许在定义类、接口和方法时使用类型参数，从而实现代码的类型安全和复用。本文将从基础概念出发，系统讲解泛型类、泛型接口和泛型方法的定义与使用。

## 为什么需要泛型

在泛型出现之前，Java 集合类只能存储 `Object` 类型的对象，这带来了两个严重问题：

**类型不安全**

```java
// 没有泛型的时代
List list = new ArrayList();
list.add("hello");
list.add(123);  // 编译器不会报错，但运行时可能出问题

String str = (String) list.get(1);  // ClassCastException!
```

**需要强制类型转换**

每次从集合中取出元素，都需要手动进行类型转换，代码冗长且容易出错。

泛型的引入完美解决了这些问题：

```java
// 使用泛型
List<String> list = new ArrayList<>();
list.add("hello");
list.add(123);  // 编译错误！类型不匹配

String str = list.get(0);  // 无需强制转换
```

泛型的核心优势可以总结为三点：

| 优势 | 说明 |
|------|------|
| 类型安全 | 编译期检查类型，避免 `ClassCastException` |
| 消除强制转换 | 无需手动 `(Type)` 转换 |
| 代码复用 | 一份代码适用于多种类型 |

## 泛型类的定义

泛型类是在类名后面添加类型参数的类。类型参数使用尖括号 `<>` 声明，放在类名之后。

```java
public class Box<T> {
    private T content;

    public Box(T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }
}
```

`T` 是类型参数（Type Parameter），在使用时会被具体的类型替换。

### 类型参数命名惯例

Java 社区对类型参数的命名有一些约定俗成的惯例：

| 符号 | 含义 | 典型场景 |
|------|------|----------|
| `T` | Type（类型） | 通用类型参数 |
| `E` | Element（元素） | 集合中的元素类型 |
| `K` | Key（键） | Map 的键类型 |
| `V` | Value（值） | Map 的值类型 |
| `N` | Number（数字） | 数值类型 |
| `S`, `U`, `V` | 第二、三、四个类型参数 | 多参数场景 |

这些命名只是惯例，并非强制要求。使用有意义的名称可以提高代码可读性，例如 `Element` 可以缩写为 `E`，但如果上下文清晰，使用 `T` 也完全没问题。

### 多个类型参数

泛型类可以有多个类型参数：

```java
public class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }
}
```

## 泛型类的使用

实例化泛型类时，需要在尖括号中指定具体的类型：

```java
// 创建一个存放 String 的 Box
Box<String> stringBox = new Box<>("Hello");
String content = stringBox.getContent();  // 返回 String 类型

// 创建一个存放 Integer 的 Box
Box<Integer> intBox = new Box<>(42);
Integer number = intBox.getContent();  // 返回 Integer 类型
```

Java 7 引入了菱形操作符（Diamond Operator）`<>`，可以在构造器中省略类型参数：

```java
Box<String> stringBox = new Box<>("Hello");  // 编译器推断类型为 String
```

## 泛型接口的定义与实现

泛型接口的定义方式与泛型类类似：

```java
public interface Repository<T> {
    void save(T entity);
    T findById(int id);
    List<T> findAll();
}
```

实现泛型接口时，可以指定具体的类型参数：

```java
public class UserRepository implements Repository<User> {
    @Override
    public void save(User entity) {
        // 保存用户
    }

    @Override
    public User findById(int id) {
        // 查找用户
        return null;
    }

    @Override
    public List<User> findAll() {
        // 返回所有用户
        return new ArrayList<>();
    }
}
```

也可以保留类型参数，让实现类继续是泛型的：

```java
public class ArrayListRepository<T> implements Repository<T> {
    private List<T> storage = new ArrayList<>();

    @Override
    public void save(T entity) {
        storage.add(entity);
    }

    @Override
    public T findById(int id) {
        return storage.get(id);
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage);
    }
}
```

## 泛型方法的定义

泛型方法是在方法返回类型之前声明类型参数的方法。泛型方法可以定义在普通类中，也可以定义在泛型类中。

```java
public class ArrayUtils {
    // 泛型方法：将数组转换为 List
    public static <T> List<T> arrayToList(T[] array) {
        return new ArrayList<>(Arrays.asList(array));
    }

    // 泛型方法：在数组中查找元素
    public static <T> int indexOf(T[] array, T target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(target)) {
                return i;
            }
        }
        return -1;
    }
}
```

关键语法：`<T>` 放在方法返回类型之前，表示这是一个泛型方法，`T` 是类型参数。

### 泛型方法与泛型类的区别

```java
public class Box<T> {  // T 是类的类型参数
    private T content;

    // 这不是泛型方法，它使用的是类的类型参数 T
    public T getContent() {
        return content;
    }

    // 这是泛型方法，它定义了自己的类型参数 U
    public <U> void inspect(U item) {
        System.out.println("Item: " + item);
    }
}
```

## 泛型方法的类型推断

Java 编译器可以根据方法参数和赋值目标自动推断泛型方法的类型参数，调用时可以省略类型参数：

```java
// 显式指定类型参数
List<String> list1 = ArrayUtils.<String>arrayToList(new String[]{"a", "b", "c"});

// 编译器自动推断类型参数（推荐写法）
List<String> list2 = ArrayUtils.arrayToList(new String[]{"a", "b", "c"});

// 类型推断的更多例子
int index = ArrayUtils.indexOf(new Integer[]{1, 2, 3}, 2);  // 推断 T 为 Integer
```

类型推断的规则：

1. 根据方法参数推断
2. 根据赋值目标类型推断
3. 结合多种信息综合推断

## 泛型构造器

构造器也可以声明自己的类型参数，虽然这种情况不太常见：

```java
public class Entity {
    private String data;

    // 泛型构造器
    public <T> Entity(T item) {
        this.data = item.toString();
    }

    // 普通构造器
    public Entity(String data) {
        this.data = data;
    }
}
```

调用泛型构造器：

```java
Entity e1 = new Entity(123);        // T 推断为 Integer
Entity e2 = new Entity("hello");    // 调用普通构造器
Entity e3 = new <String>Entity("hello");  // 显式指定（不常见）
```

## 泛型的类型限制：有界类型参数

有时我们需要对类型参数进行限制，使其必须是某个类的子类或实现了某个接口。这就是有界类型参数（Bounded Type Parameters）。

```java
// T 必须实现 Comparable 接口
public class SortUtil {
    public static <T extends Comparable<T>> T min(T[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }
        T min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i].compareTo(min) < 0) {
                min = array[i];
            }
        }
        return min;
    }
}
```

`<T extends Comparable<T>>` 的含义：
- `T` 必须实现 `Comparable<T>` 接口
- 只有满足这个条件的类型才能作为类型参数传入

使用：

```java
Integer[] nums = {3, 1, 4, 1, 5};
Integer min = SortUtil.min(nums);  // 正确，Integer 实现了 Comparable<Integer>

// String[] strs = {"banana", "apple", "cherry"};
// String minStr = SortUtil.min(strs);  // 正确，String 实现了 Comparable<String>
```

### 多重限制

类型参数可以有多个限制，使用 `&` 连接：

```java
public class DataProcessor<T extends Comparable<T> & Serializable> {
    private T data;

    public DataProcessor(T data) {
        this.data = data;
    }

    public boolean isGreaterThan(T other) {
        return data.compareTo(other) > 0;
    }
}
```

多重限制的规则：
- 类必须放在第一位，接口放在后面
- 只能有一个类限制，但可以有多个接口限制
- 例如：`<T extends ArrayList & Comparable<T> & Serializable>`

```java
// 正确：类在前，接口在后
<T extends Number & Comparable<T> & Serializable>

// 错误：不能有多个类限制
// <T extends Number & String>  // 编译错误
```

## 泛型与继承

这是一个常见的误区：`Pair<Employee>` 不是 `Pair<Person>` 的子类型，即使 `Employee` 是 `Person` 的子类。

```java
class Person { }
class Employee extends Person { }

// 这行代码无法编译！
// Pair<Employee> employeePair = new Pair<Person>(...);

// 也不能这样赋值
// List<Person> persons = new ArrayList<Employee>();  // 编译错误
```

原因在于泛型的类型安全保证。假设允许这种赋值：

```java
List<Employee> employees = new ArrayList<>();
employees.add(new Employee());

List<Person> persons = employees;  // 假设允许
persons.add(new Person());  // 向 employees 列表中添加了 Person！

Employee emp = employees.get(1);  // ClassCastException!
```

为了保证类型安全，Java 泛型是**不协变**的（invariant）。如果需要协变行为，需要使用通配符，这将在下一篇文章中详细讨论。

### 泛型类的继承

泛型类本身可以被继承：

```java
// 继承时指定具体类型
public class IntBox extends Box<Integer> {
    public IntBox(Integer content) {
        super(content);
    }
}

// 继承时保留类型参数
public class PersistentBox<T> extends Box<T> {
    private String storageId;

    public PersistentBox(T content, String storageId) {
        super(content);
        this.storageId = storageId;
    }
}
```

## 小结

本文介绍了 Java 泛型编程的基础知识，包括泛型类、泛型接口和泛型方法的定义与使用。核心要点：

- 泛型提供编译期类型安全检查，避免运行时类型转换异常
- 类型参数命名有惯例：`T`（类型）、`E`（元素）、`K`/`V`（键值）
- 泛型方法的类型参数声明在返回类型之前
- 有界类型参数 `<T extends ...>` 限制类型参数的范围
- 泛型不支持协变：`List<Employee>` 不是 `List<Person>` 的子类型

掌握这些基础知识后，下一篇文章将深入讲解通配符和类型擦除，这是理解 Java 泛型底层机制的关键。
