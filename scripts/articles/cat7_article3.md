# 泛型实战与设计模式：Java 泛型的高级应用

前两篇文章介绍了泛型的基础语法和底层机制。本文将聚焦于泛型在实际项目中的应用，包括常用设计模式、项目实战技巧以及常见陷阱的解决方案。

## 泛型集合的使用

Java 集合框架是泛型最广泛的应用场景。理解集合中泛型的使用方式是每个 Java 开发者的必备技能。

### List、Set、Map 的泛型声明

```java
// List：有序、可重复
List<String> names = new ArrayList<>();
names.add("Alice");
names.add("Bob");

// Set：无序、不可重复
Set<Integer> numbers = new HashSet<>();
numbers.add(1);
numbers.add(2);

// Map：键值对
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 95);
scores.put("Bob", 87);
```

### 集合的泛型方法

集合接口中定义了许多泛型方法：

```java
// Collections 工具类中的泛型方法
List<String> list = new ArrayList<>();
Collections.sort(list);  // T extends Comparable<? super T>

List<String> unmodifiable = Collections.unmodifiableList(list);
List<String> checked = Collections.checkedList(list, String.class);
```

### 集合与数组的转换

```java
// 数组转 List
String[] array = {"a", "b", "c"};
List<String> list = Arrays.asList(array);

// List 转数组
String[] array2 = list.toArray(new String[0]);

// 使用 Stream 转换
String[] array3 = list.toArray(String[]::new);
```

## Comparable 和 Comparator 的泛型设计

`Comparable` 和 `Comparator` 是 Java 中实现对象排序的两个核心接口，它们的泛型设计非常精妙。

### Comparable 接口

```java
public interface Comparable<T> {
    int compareTo(T other);
}
```

实现 `Comparable` 接口使类具有"自然排序"能力：

```java
public class Student implements Comparable<Student> {
    private String name;
    private int age;

    @Override
    public int compareTo(Student other) {
        return this.age - other.age;  // 按年龄排序
    }
}

// 使用
List<Student> students = new ArrayList<>();
Collections.sort(students);  // 自动按年龄排序
```

### Comparator 接口

```java
public interface Comparator<T> {
    int compare(T o1, T o2);
}
```

`Comparator` 用于定义多种排序策略：

```java
public class StudentNameComparator implements Comparator<Student> {
    @Override
    public int compare(Student s1, Student s2) {
        return s1.getName().compareTo(s2.getName());
    }
}

// 使用 Lambda 表达式
Comparator<Student> byAge = (s1, s2) -> s1.getAge() - s2.getAge();
Comparator<Student> byName = Comparator.comparing(Student::getName);

// 组合排序
Comparator<Student> byAgeThenName = byAge.thenComparing(byName);
```

### Comparator 工厂方法（Java 8+）

```java
// 静态工厂方法
Comparator<String> byLength = Comparator.comparingInt(String::length);
Comparator<String> byLengthReversed = Comparator.comparingInt(String::length).reversed();

// 处理 null 值
Comparator<String> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
Comparator<String> nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
```

## 类型安全的异构容器

类型安全的异构容器（Typesafe Heterogeneous Container）是泛型的一种高级应用，允许在一个容器中存储不同类型的对象，同时保持类型安全。

### 使用 Class 对象作为键

```java
public class TypesafeMap {
    private Map<Class<?>, Object> map = new HashMap<>();

    // 存储：类型安全
    public <T> void put(Class<T> type, T value) {
        if (type == null) {
            throw new NullPointerException("Type is null");
        }
        map.put(type, type.cast(value));
    }

    // 读取：自动转型
    public <T> T get(Class<T> type) {
        return type.cast(map.get(type));
    }
}
```

使用示例：

```java
TypesafeMap favorites = new TypesafeMap();
favorites.put(String.class, "Java");
favorites.put(Integer.class, 42);
favorites.put(Class.class, TypesafeMap.class);

String str = favorites.get(String.class);     // "Java"
Integer num = favorites.get(Integer.class);    // 42
Class<?> cls = favorites.get(Class.class);     // TypesafeMap.class
```

### 进阶：支持泛型类型

```java
public class TypesafeMap {
    private Map<TypeLiteral<?>, Object> map = new HashMap<>();

    public <T> void put(TypeLiteral<T> key, T value) {
        map.put(key, value);
    }

    public <T> T get(TypeLiteral<T> key) {
        return key.getType().cast(map.get(key));
    }
}

// 类型字面量
public class TypeLiteral<T> {
    private final Type type;

    protected TypeLiteral() {
        Type superClass = getClass().getGenericSuperclass();
        this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked")
    public Class<T> getType() {
        return (Class<T>) type;
    }
}

// 使用
TypeLiteral<List<String>> stringListKey = new TypeLiteral<>() {};
TypesafeMap map = new TypesafeMap();
map.put(stringListKey, Arrays.asList("a", "b"));
List<String> result = map.get(stringListKey);
```

## 泛型与反射

泛型与反射的结合使用在框架开发中非常常见。

### Class 的泛型

`Class<T>` 本身就是泛型类：

```java
// 获取 Class 对象
Class<String> stringClass = String.class;
Class<Integer> intClass = Integer.class;

// 泛型方法：创建实例
public <T> T createInstance(Class<T> clazz) throws Exception {
    return clazz.getDeclaredConstructor().newInstance();
}

String str = createInstance(String.class);
```

### cast 方法

`Class.cast()` 方法用于安全的类型转换：

```java
public <T> T safeCast(Object obj, Class<T> type) {
    if (type.isInstance(obj)) {
        return type.cast(obj);
    }
    return null;
}

String str = safeCast("hello", String.class);  // "hello"
String num = safeCast(42, String.class);        // null
```

### 获取泛型类型信息

```java
public class GenericReflection {
    // 获取父类的泛型参数
    public static Type[] getGenericSuperclassTypeArgs(Class<?> clazz) {
        Type superClass = clazz.getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            return ((ParameterizedType) superClass).getActualTypeArguments();
        }
        return new Type[0];
    }

    // 获取接口的泛型参数
    public static Type[] getGenericInterfaceTypeArgs(Class<?> clazz, Class<?> iface) {
        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType() == iface) {
                    return pt.getActualTypeArguments();
                }
            }
        }
        return new Type[0];
    }
}
```

## 泛型在实际项目中的应用

### 通用响应封装

在 Web 开发中，通常需要一个通用的响应封装类：

```java
public class R<T> {
    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> error(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    // getter/setter 省略
}

// 使用
@GetMapping("/user/{id}")
public R<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return R.ok(user);
}
```

### 分页封装

```java
public class Page<T> {
    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> Page<T> of(List<T> records, long total, long page, long size) {
        Page<T> p = new Page<>();
        p.setRecords(records);
        p.setTotal(total);
        p.setPage(page);
        p.setSize(size);
        return p;
    }

    // getter/setter 省略
}

// 使用
@GetMapping("/users")
public R<Page<User>> getUsers(@RequestParam int page, @RequestParam int size) {
    Page<User> userPage = userService.getUsers(page, size);
    return R.ok(userPage);
}
```

### 通用 Repository 模式

```java
public interface BaseRepository<T, ID> {
    T findById(ID id);
    List<T> findAll();
    T save(T entity);
    void deleteById(ID id);
    long count();
}

// 实现
public abstract class AbstractRepository<T, ID> implements BaseRepository<T, ID> {
    protected final Class<T> entityClass;

    @SuppressWarnings("unchecked")
    protected AbstractRepository() {
        Type superClass = getClass().getGenericSuperclass();
        Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        this.entityClass = (Class<T>) type;
    }

    @Override
    public T findById(ID id) {
        // 使用 entityClass 进行查询
        return null;
    }
}

// 具体实现
public class UserRepository extends AbstractRepository<User, Long> {
    // 只需实现特定的查询方法
}
```

### 泛型工具方法

Java 标准库提供了许多实用的泛型工具方法：

```java
// 类型安全的集合包装
List<String> safeList = Collections.checkedList(new ArrayList<>(), String.class);
Set<Integer> safeSet = Collections.checkedSet(new HashSet<>(), Integer.class);

// 不可修改的集合
List<String> unmodifiable = Collections.unmodifiableList(list);
Map<String, Integer> unmodifiableMap = Collections.unmodifiableMap(map);

// 空集合
List<String> emptyList = Collections.emptyList();
Map<String, Integer> emptyMap = Collections.emptyMap();

// 单元素集合
List<String> singleton = Collections.singletonList("only");
Set<Integer> singletonSet = Collections.singleton(42);
```

## 使用泛型的常见陷阱与解决方案

### 陷阱1：泛型数组创建

```java
// 错误：不能创建泛型数组
// List<String>[] array = new List<String>[10];

// 解决方案1：使用 ArrayList 数组
List<String>[] array = new ArrayList[10];  // 未检查的警告

// 解决方案2：使用 List<List<String>>
List<List<String>> listOfLists = new ArrayList<>();
```

### 陷阱2：类型擦除导致的 instanceof 失败

```java
// 错误：不能 instanceof 泛型类型
// if (obj instanceof List<String>) { }

// 解决方案：只检查原始类型
if (obj instanceof List) {
    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) obj;
    // 安全使用
}
```

### 陷阱3：泛型类型不能用作异常

```java
// 错误：不能抛出泛型异常
// public <T extends Exception> void throwIt() throws T { }

// 解决方案：使用包装类
public class TypedException<T> extends Exception {
    private T detail;

    public TypedException(T detail) {
        this.detail = detail;
    }
}
```

### 陷阱4：静态上下文中的类型参数

```java
// 错误：静态方法不能使用类的类型参数
// public class Box<T> {
//     public static T getValue() { return null; }
// }

// 解决方案：使用泛型方法
public class Box<T> {
    public static <U> U getValue(U defaultValue) {
        return defaultValue;
    }
}
```

## 泛型与 Stream API 的配合

Stream API 与泛型配合使用，可以实现类型安全的函数式编程。

### 基本用法

```java
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

// filter：类型不变
List<String> filtered = names.stream()
    .filter(name -> name.startsWith("A"))
    .collect(Collectors.toList());

// map：类型可以改变
List<Integer> lengths = names.stream()
    .map(String::length)
    .collect(Collectors.toList());

// flatMap：展平嵌套
List<List<Integer>> nested = Arrays.asList(
    Arrays.asList(1, 2),
    Arrays.asList(3, 4)
);
List<Integer> flat = nested.stream()
    .flatMap(Collection::stream)
    .collect(Collectors.toList());
```

### 自定义泛型 Stream 操作

```java
public class StreamUtils {
    // 安全的类型转换
    public static <T, R> Stream<R> mapType(Stream<T> stream, Class<R> type) {
        return stream.filter(type::isInstance).map(type::cast);
    }

    // 分组并统计
    public static <T, K> Map<K, Long> groupAndCount(
            Stream<T> stream,
            Function<T, K> classifier) {
        return stream.collect(Collectors.groupingBy(classifier, Collectors.counting()));
    }
}
```

### 收集器的泛型

```java
// 自定义收集器
public class GenericCollectors {
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return Collectors.collectingAndThen(
            Collectors.toList(),
            Collections::unmodifiableList
        );
    }
}

// 使用
List<String> result = names.stream()
    .collect(GenericCollectors.toUnmodifiableList());
```

## Java 泛型与 Kotlin 泛型的对比

如果你同时使用 Java 和 Kotlin，了解两者泛型的差异很重要。

### 型变差异

| 特性 | Java | Kotlin |
|------|------|--------|
| 声明处型变 | 不支持 | 支持（`out`/`in`） |
| 使用处型变 | 支持（`? extends`/`? super`） | 不需要（声明处处理） |
| 不变性 | 默认不变 | 默认不变 |

### Kotlin 的 out 和 in

```kotlin
// Kotlin：声明处型变
class Producer<out T> {  // T 只能作为返回类型（生产者）
    fun produce(): T { ... }
}

class Consumer<in T> {  // T 只能作为参数类型（消费者）
    fun consume(item: T) { ... }
}

// 等价于 Java 的
// Producer<? extends T>  // 生产者
// Consumer<? super T>    // 消费者
```

### 类型投影

```kotlin
// 使用处型变（类型投影）
fun copy(from: Array<out Any>, to: Array<Any>) { ... }  // 等价于 Java 的 ? extends
fun fill(dest: Array<in String>, value: String) { ... }  // 等价于 Java 的 ? super
```

### 星号投影

```kotlin
// Kotlin 的星号投影
fun printAll(list: List<*>) {  // 等价于 Java 的 List<?>
    list.forEach { println(it) }
}
```

## 小结

本文介绍了 Java 泛型在实际项目中的高级应用：

- **集合框架**：泛型是集合类型安全的基础
- **Comparable/Comparator**：泛型设计使排序接口灵活且类型安全
- **异构容器**：使用 `Class<T>` 作为键实现类型安全的异构存储
- **项目实践**：通用响应封装 `R<T>`、分页 `Page<T>`、Repository 模式
- **Stream API**：泛型与函数式编程的完美结合
- **Kotlin 对比**：了解 `out`/`in` 关键字与 Java 通配符的关系

掌握泛型不仅能写出更安全的代码，还能设计出更优雅的 API。建议在实际项目中多加练习，逐步深入理解泛型的强大能力。
