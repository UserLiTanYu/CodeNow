# 泛型数组与类型安全异构容器

Java 泛型的类型擦除机制虽然保证了向后兼容性，但也带来了一系列限制。其中最让开发者困惑的莫过于泛型数组的限制。本文将深入分析泛型数组的限制及其原因，并介绍类型安全异构容器（Typesafe Heterogeneous Container）这一高级泛型模式。

## 泛型数组的限制

Java 中以下几种泛型数组的创建方式都是非法的：

```java
// 1. 不能创建泛型类型的数组
public <T> T[] createArray(int size) {
    return new T[size]; // 编译错误：Generic array creation
}

// 2. 不能创建参数化类型的数组
List<String>[] array = new List<String>[10]; // 编译错误：Generic array creation

// 3. 不能创建类型变量的数组
public <T> void method() {
    T[] array = new T[10]; // 编译错误
}
```

这些限制看起来很不方便，但它们的存在是为了保证类型安全。

## 泛型数组限制的原因

要理解为什么不能创建泛型数组，需要从数组和泛型的根本特性入手。

### 数组的协变性（Covariance）

Java 数组是协变的——如果 `A` 是 `B` 的子类型，那么 `A[]` 也是 `B[]` 的子类型：

```java
String[] strings = new String[10];
Object[] objects = strings; // 合法：String[] 是 Object[] 的子类型
```

这种协变性在编译时是合法的，但运行时会进行类型检查：

```java
objects[0] = 42; // 编译通过，但运行时抛出 ArrayStoreException
```

### 泛型的不变性（Invariance）

泛型是不变的——`List<String>` 不是 `List<Object>` 的子类型：

```java
List<String> strings = new ArrayList<>();
List<Object> objects = strings; // 编译错误：不兼容的类型
```

这种不变性保证了编译时的类型安全。

### 协变性 + 泛型 = 类型不安全

如果允许创建泛型数组，就会出现以下问题：

```java
// 假设允许创建泛型数组
List<String>[] stringLists = new List<String>[10];

// 由于数组协变性，这行代码合法
Object[] objects = stringLists;

// 由于 List<Object> 是合法的 List，这行代码也合法
objects[0] = new ArrayList<Integer>();

// 现在 stringLists[0] 实际上是一个 List<Integer>，但我们以为它是 List<String>
String s = stringLists[0].get(0); // 运行时 ClassCastException！
```

整个过程在编译时没有任何警告，但在运行时会出错。这就是 Java 禁止创建泛型数组的根本原因——**它无法保证类型安全**。

### 对比：数组的 ArrayStoreException

非泛型数组也有类似的问题，但数组可以在运行时检查：

```java
Object[] objects = new String[10];
objects[0] = 42; // 运行时 ArrayStoreException
```

数组在运行时知道其元素类型（`String`），所以可以进行检查。但泛型的类型信息在运行时已被擦除，无法进行这种检查。

## 泛型数组的解决方案

虽然不能直接创建泛型数组，但有几种替代方案。

### 方案一：使用 List 代替数组

大多数情况下，使用 `List` 是更好的选择：

```java
// 不推荐
public <T> T[] toArray(T[] array) { ... }

// 推荐
public <T> List<T> toList(Collection<T> collection) {
    return new ArrayList<>(collection);
}
```

`List` 提供了与数组类似的功能，且完全支持泛型。

### 方案二：使用 Array.newInstance() 反射创建

如果确实需要数组，可以通过反射创建：

```java
@SuppressWarnings("unchecked")
public static <T> T[] createArray(Class<T> componentType, int size) {
    return (T[]) Array.newInstance(componentType, size);
}

// 使用
String[] strings = createArray(String.class, 10);
Integer[] integers = createArray(Integer.class, 5);
```

这种方式在运行时才知道具体类型，所以可以安全地创建数组。

### 方案三：使用 @SuppressWarnings 强制转换

在确保安全的前提下，可以使用强制转换：

```java
@SuppressWarnings("unchecked")
public static <T> T[] toArray(Collection<T> collection, Class<T> componentType) {
    T[] array = (T[]) Array.newInstance(componentType, collection.size());
    return collection.toArray(array);
}
```

关键是要确保数组的实际运行时类型与声明的类型一致，否则在后续使用中可能出现 `ArrayStoreException`。

## 类型安全异构容器（Typesafe Heterogeneous Container）

类型安全异构容器是泛型的一种高级应用模式，它允许在一个容器中存储不同类型的对象，同时保持类型安全。

### 问题：Map<String, Object> 的类型不安全

考虑一个需要存储不同类型配置项的场景：

```java
public class Config {
    private Map<String, Object> settings = new HashMap<>();
    
    public void put(String key, Object value) {
        settings.put(key, value);
    }
    
    public Object get(String key) {
        return settings.get(key);
    }
}

Config config = new Config();
config.put("maxRetry", 3);
config.put("timeout", 1000L);

// 需要强制转换，容易出错
int retry = (Integer) config.get("maxRetry");
long timeout = (Long) config.get("timeout");

// 没有编译时类型检查
String name = (String) config.get("maxRetry"); // 运行时 ClassCastException
```

这种方式有几个问题：
1. 需要强制转换
2. 容易混淆键和值的类型对应关系
3. 错误只能在运行时发现

### 解决方案：Class<T> 作为键

使用 `Class<T>` 作为键，可以将值的类型与键关联起来：

```java
public class Favorites {
    private Map<Class<?>, Object> favorites = new HashMap<>();
    
    public <T> void putFavorite(Class<T> type, T instance) {
        favorites.put(Objects.requireNonNull(type), instance);
    }
    
    public <T> T getFavorite(Class<T> type) {
        return type.cast(favorites.get(type));
    }
}
```

使用示例：

```java
Favorites f = new Favorites();
f.putFavorite(String.class, "Hello");
f.putFavorite(Integer.class, 42);
f.putFavorite(Class.class, Favorites.class);

String s = f.getFavorite(String.class);   // "Hello"
Integer i = f.getFavorite(Integer.class); // 42
Class<?> c = f.getFavorite(Class.class);  // Favorites.class
```

**核心思想：** `Class<T>` 对象被称为**类型令牌**（Type Token），它在运行时携带了类型信息。`getFavorite` 方法的返回类型由传入的 `Class<T>` 参数决定，编译器可以保证类型安全。

### Favorites 类的完整实现

一个更健壮的实现应该包含类型检查和一些便利方法：

```java
public class Favorites {
    private Map<Class<?>, Object> favorites = new ConcurrentHashMap<>();
    
    public <T> void putFavorite(Class<T> type, T instance) {
        Objects.requireNonNull(type, "Type cannot be null");
        // 确保值确实是声明的类型
        favorites.put(type, type.cast(instance));
    }
    
    public <T> T getFavorite(Class<T> type) {
        return type.cast(favorites.get(type));
    }
    
    public boolean containsFavorite(Class<?> type) {
        return favorites.containsKey(type);
    }
    
    public boolean removeFavorite(Class<?> type) {
        return favorites.remove(type) != null;
    }
}
```

**使用 `type.cast(instance)` 而非直接存储的原因：** 这可以在 `putFavorite` 时就捕获类型错误，而不是等到 `getFavorite` 时才发现。

## Class<T> 作为类型令牌

`Class<T>` 是最简单的类型令牌，它在运行时表示一个具体的类型。

### 类型令牌的基本用法

```java
public static <T> T createInstance(Class<T> clazz) throws Exception {
    return clazz.getDeclaredConstructor().newInstance();
}

String s = createInstance(String.class);
Integer i = createInstance(Integer.class);
```

### 类型令牌的局限性

`Class<T>` 只能表示**原始类型**，无法表示**泛型类型**：

```java
// 这不是 List<String> 的类型令牌，只是 List 的类型令牌
Class<List<String>> clazz = List.class; // 实际类型是 Class<List>

// 无法区分 List<String> 和 List<Integer>
Class<List<String>> stringListClass = List.class;
Class<List<Integer>> integerListClass = List.class;
System.out.println(stringListClass == integerListClass); // true
```

这是因为类型擦除——运行时 `List<String>` 和 `List<Integer>` 是同一个类。

### 复杂泛型类型的表示

对于需要表示复杂泛型类型的场景，可以使用 Guava 的 `TypeToken` 或 Jackson 的 `TypeReference`：

```java
// Guava TypeToken
TypeToken<List<String>> listToken = new TypeToken<List<String>>() {};
Type type = listToken.getType(); // java.util.List<java.lang.String>

// Jackson TypeReference
TypeReference<Map<String, List<Integer>>> typeRef = 
    new TypeReference<Map<String, List<Integer>>>() {};
```

这些工具通过匿名内部类保留了父类的泛型参数信息。

## 实际应用场景

### Spring 的 BeanFactory

Spring 框架广泛使用类型令牌来提供类型安全的 Bean 获取：

```java
// 通过 Class<T> 获取类型安全的 Bean
UserService userService = applicationContext.getBean(UserService.class);
```

`getBean(Class<T>)` 方法返回 `T` 类型，无需强制转换。

### Guava 的 TypeToInstanceMap

Guava 提供了 `TypeToInstanceMap`，它是类型安全异构容器的标准实现：

```java
TypeToInstanceMap<Object> map = MutableTypeToInstanceMap.create();

map.putInstance(String.class, "Hello");
map.putInstance(Integer.class, 42);

String s = map.getInstance(String.class);   // "Hello"
Integer i = map.getInstance(Integer.class); // 42
```

### JPA/Hibernate 的类型安全查询

```java
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<User> query = cb.createQuery(User.class); // 类型令牌
Root<User> root = query.from(User.class);
```

## 泛型与反射的配合

反射 API 中有几个与泛型配合使用的关键方法。

### Class.cast()

`cast()` 方法在运行时进行类型转换，比强制转换更安全：

```java
public <T> T safeCast(Object obj, Class<T> type) {
    if (type.isInstance(obj)) {
        return type.cast(obj);
    }
    return null;
}
```

### Class.isInstance()

`isInstance()` 是 `instanceof` 的动态版本：

```java
public <T> boolean isType(Object obj, Class<T> type) {
    return type.isInstance(obj);
}

// 等价于
isType("hello", String.class) // true
```

### 反射创建泛型实例的综合示例

```java
public class GenericFactory {
    public static <T> T create(Class<T> clazz, Object... args) throws Exception {
        // 找到匹配的构造函数
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }
        
        Constructor<T> constructor = clazz.getDeclaredConstructor(paramTypes);
        return constructor.newInstance(args);
    }
}

// 使用
String s = create(String.class, "hello");
ArrayList<String> list = create(ArrayList.class); // 需要 unchecked 警告
```

### 获取父类泛型参数

```java
public class UserDao extends GenericDao<User, Long> {
    // ...
}

// 获取 UserDao 的父类泛型参数
Type superclass = UserDao.class.getGenericSuperclass();
if (superclass instanceof ParameterizedType) {
    ParameterizedType pt = (ParameterizedType) superclass;
    Type[] typeArgs = pt.getActualTypeArguments();
    System.out.println("Entity type: " + typeArgs[0]); // User
    System.out.println("ID type: " + typeArgs[1]);     // Long
}
```

这种技术在 ORM 框架中很常见，用于自动推断实体类型。

泛型数组的限制和类型安全异构容器是 Java 泛型系统中两个重要的主题。理解这些概念有助于编写更安全、更灵活的泛型代码。在大多数情况下，优先使用 `List` 代替数组；当需要存储不同类型的对象时，考虑使用类型安全异构容器模式。
