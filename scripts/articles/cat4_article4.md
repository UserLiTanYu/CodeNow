# Class 类与反射机制

反射（Reflection）是 Java 的高级特性之一，允许程序在运行时获取类的信息并操作类的属性和方法。Spring、JUnit、Jackson 等主流框架的底层都依赖反射机制。

## Class\<T\> 类

在 Java 中，每个类被加载到 JVM 后，都会生成一个对应的 `Class` 对象。这个对象包含了该类的所有元信息：类名、包名、父类、接口、字段、方法、构造器等。

```java
public class User {
    private String name;
    private int age;
    
    public User() {}
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

对于 `User` 类，JVM 中只会有一个 `Class<User>` 对象。无论创建多少个 `User` 实例，它们共享同一个 `Class` 对象。

## 获取 Class 对象的三种方式

```java
// 方式一：通过实例的 getClass() 方法
User user = new User();
Class<?> clazz1 = user.getClass();

// 方式二：通过类的 class 属性（推荐，编译时检查）
Class<User> clazz2 = User.class;

// 方式三：通过全限定类名（运行时动态加载）
Class<?> clazz3 = Class.forName("com.example.User");

// 三种方式获取的是同一个 Class 对象
System.out.println(clazz1 == clazz2);  // true
System.out.println(clazz2 == clazz3);  // true
```

| 方式 | 语法 | 特点 |
|------|------|------|
| `getClass()` | `obj.getClass()` | 需要实例，运行时确定 |
| `.class` | `User.class` | 编译时确定，最常用 |
| `forName()` | `Class.forName("...")` | 动态加载，可能抛出 `ClassNotFoundException` |

## Class 的常用方法

```java
Class<User> clazz = User.class;

// 类名信息
clazz.getName();           // "com.example.User"（全限定名）
clazz.getSimpleName();     // "User"（简单类名）
clazz.getCanonicalName();  // "com.example.User"（规范名）
clazz.getTypeName();       // "com.example.User"

// 包信息
Package pkg = clazz.getPackage();
pkg.getName();             // "com.example"

// 父类
Class<?> superClass = clazz.getSuperclass();
superClass.getName();      // "java.lang.Object"

// 接口
Class<?>[] interfaces = clazz.getInterfaces();

// 类型判断
clazz.isInterface();       // false
clazz.isArray();           // false
clazz.isPrimitive();       // false
clazz.isEnum();            // false
clazz.isAnnotation();      // false
clazz.isAnonymousClass();  // false

// 基本类型的 Class 对象
int.class.isPrimitive();           // true
Integer.class.isPrimitive();       // false
int.class == Integer.class;        // false
int.class == Integer.TYPE;         // true
```

## 反射创建对象

### newInstance()（已废弃）

```java
// Java 9 之前的方式（已废弃）
Class<User> clazz = User.class;
User user = clazz.newInstance();  // 调用无参构造器
```

### getDeclaredConstructor().newInstance()（推荐）

```java
Class<User> clazz = User.class;

// 调用无参构造器
User user1 = clazz.getDeclaredConstructor().newInstance();

// 调用有参构造器
Constructor<User> constructor = clazz.getDeclaredConstructor(String.class, int.class);
User user2 = constructor.newInstance("张三", 25);
```

推荐使用新方式的原因：
- `newInstance()` 只能调用无参构造器
- `newInstance()` 会吞掉构造器抛出的异常，难以调试
- `getDeclaredConstructor().newInstance()` 更灵活，异常处理更清晰

## 反射调用方法

```java
Class<User> clazz = User.class;
User user = clazz.getDeclaredConstructor().newInstance();

// 获取 public 方法（包括继承的）
Method getNameMethod = clazz.getMethod("getName");
String name = (String) getNameMethod.invoke(user);

// 获取指定参数类型的方法
Method setNameMethod = clazz.getMethod("setName", String.class);
setNameMethod.invoke(user, "李四");

// 获取本类声明的方法（包括 private）
Method privateMethod = clazz.getDeclaredMethod("privateMethod");
privateMethod.setAccessible(true);  // 突破 private 限制
privateMethod.invoke(user);
```

`getMethod()` vs `getDeclaredMethod()`：

| 方法 | 范围 | 能获取 private 方法 |
|------|------|---------------------|
| `getMethod()` | 本类 + 父类的 public 方法 | 不能 |
| `getDeclaredMethod()` | 本类声明的所有方法 | 能 |

```java
// 获取所有本类声明的方法
Method[] methods = clazz.getDeclaredMethods();
for (Method method : methods) {
    System.out.println(method.getName() + " : " + method.getModifiers());
}
```

## 反射访问字段

```java
Class<User> clazz = User.class;
User user = clazz.getDeclaredConstructor("张三", 25).newInstance();

// 获取 public 字段
Field publicField = clazz.getField("publicField");

// 获取 private 字段
Field nameField = clazz.getDeclaredField("name");
nameField.setAccessible(true);  // 突破 private 限制

// 读取字段值
String name = (String) nameField.get(user);

// 设置字段值
nameField.set(user, "王五");
```

`setAccessible(true)` 的作用：

Java 的访问控制（`private`、`protected`、`public`）是编译时检查，反射可以在运行时绕过这些限制。`setAccessible(true)` 禁用访问检查，提高反射调用的性能。

```java
// 性能对比
long start = System.nanoTime();

// 普通调用
for (int i = 0; i < 1000000; i++) {
    user.getName();
}

// 反射调用（未设置 accessible）
Method method = User.class.getMethod("getName");
for (int i = 0; i < 1000000; i++) {
    method.invoke(user);
}

// 反射调用（设置 accessible）
method.setAccessible(true);
for (int i = 0; i < 1000000; i++) {
    method.invoke(user);
}
```

## 反射操作构造器

```java
Class<User> clazz = User.class;

// 获取所有构造器
Constructor<?>[] constructors = clazz.getDeclaredConstructors();
for (Constructor<?> c : constructors) {
    System.out.println(c);
}

// 获取指定参数类型的构造器
Constructor<User> constructor = clazz.getDeclaredConstructor(String.class, int.class);

// 获取构造器参数信息
Parameter[] parameters = constructor.getParameters();
for (Parameter p : parameters) {
    System.out.println(p.getType() + " : " + p.getName());
}

// 创建对象
User user = constructor.newInstance("张三", 25);
```

## 反射的典型应用

### Spring IoC 容器

Spring 通过反射创建 Bean 实例：

```java
// Spring 的简化实现逻辑
public class SimpleIoC {
    public Object createBean(Class<?> clazz) {
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        return constructor.newInstance();
    }
}

// 配置文件指定类名，运行时动态加载
// <bean class="com.example.UserService" />
Class<?> clazz = Class.forName("com.example.UserService");
Object bean = clazz.getDeclaredConstructor().newInstance();
```

### JUnit 测试框架

JUnit 通过反射发现和执行测试方法：

```java
public class TestRunner {
    public void run(Class<?> testClass) {
        Object instance = testClass.getDeclaredConstructor().newInstance();
        
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Test.class)) {
                method.invoke(instance);
            }
        }
    }
}
```

### 序列化框架（Jackson）

Jackson 通过反射读取字段和调用 setter：

```java
// JSON 反序列化的简化逻辑
public <T> T deserialize(String json, Class<T> clazz) {
    T obj = clazz.getDeclaredConstructor().newInstance();
    
    // 解析 JSON，通过反射设置字段
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
        field.setAccessible(true);
        Object value = extractValue(json, field.getName());
        field.set(obj, value);
    }
    
    return obj;
}
```

### ORM 框架（MyBatis）

MyBatis 通过反射将数据库结果映射到对象：

```java
// 简化的 ORM 映射逻辑
public <T> T mapRow(ResultSet rs, Class<T> clazz) {
    T obj = clazz.getDeclaredConstructor().newInstance();
    
    ResultSetMetaData meta = rs.getMetaData();
    for (int i = 1; i <= meta.getColumnCount(); i++) {
        String columnName = meta.getColumnName(i);
        Field field = clazz.getDeclaredField(columnName);
        field.setAccessible(true);
        field.set(obj, rs.getObject(i));
    }
    
    return obj;
}
```

## 反射的性能开销与安全考量

### 性能开销

反射比直接调用慢，原因包括：

| 开销来源 | 说明 |
|----------|------|
| 类型检查 | 运行时需要验证参数类型 |
| 访问控制 | 需要检查权限（`setAccessible` 可优化） |
| 方法查找 | `getMethod()` 需要遍历方法表 |
| 装箱拆箱 | 基本类型需要包装为对象 |

优化建议：

```java
// 1. 缓存 Method/Field 对象，避免重复查找
private static final Method GET_NAME_METHOD = 
    User.class.getMethod("getName");

// 2. 使用 setAccessible 提高性能
method.setAccessible(true);

// 3. 考虑使用 MethodHandle（Java 7+）或 VarHandle（Java 9+）
MethodHandles.Lookup lookup = MethodHandles.lookup();
MethodHandle handle = lookup.findVirtual(User.class, "getName", 
    MethodType.methodType(String.class));
String name = (String) handle.invoke(user);
```

### 安全考量

反射可以绕过访问控制，使用时需注意：

```java
// 1. 不要反射修改 final 字段（行为未定义）
// 2. 不要反射访问内部 API（sun.*、com.sun.*）
// 3. 在安全管理器环境下可能受限

// 安全的做法：只反射访问自己类的成员
public class ReflectionUtils {
    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("反射访问字段失败", e);
        }
    }
}
```

## java.lang.reflect 包核心类

| 类 | 说明 | 常用方法 |
|----|------|----------|
| `Method` | 方法信息 | `invoke()`、`getParameterTypes()`、`getReturnType()` |
| `Field` | 字段信息 | `get()`、`set()`、`getType()`、`getModifiers()` |
| `Constructor` | 构造器信息 | `newInstance()`、`getParameterTypes()` |
| `Modifier` | 修饰符工具 | `isPublic()`、`isPrivate()`、`isStatic()`、`isFinal()` |

```java
// 使用 Modifier 判断修饰符
Method method = User.class.getMethod("getName");
int mod = method.getModifiers();

Modifier.isPublic(mod);     // true
Modifier.isStatic(mod);     // false
Modifier.isFinal(mod);      // false
Modifier.toString(mod);     // "public"

// 综合示例：打印类的完整信息
public static void printClassInfo(Class<?> clazz) {
    System.out.println("类名：" + clazz.getName());
    System.out.println("父类：" + clazz.getSuperclass().getName());
    
    System.out.println("\n字段：");
    for (Field field : clazz.getDeclaredFields()) {
        System.out.printf("  %s %s %s%n", 
            Modifier.toString(field.getModifiers()),
            field.getType().getSimpleName(),
            field.getName());
    }
    
    System.out.println("\n方法：");
    for (Method method : clazz.getDeclaredMethods()) {
        System.out.printf("  %s %s %s%n",
            Modifier.toString(method.getModifiers()),
            method.getReturnType().getSimpleName(),
            method.getName());
    }
}
```

反射是 Java 元编程的基础，虽然日常业务代码中较少直接使用，但理解反射机制有助于深入理解框架原理，排查框架相关的问题。
