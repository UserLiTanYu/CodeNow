# 创建型模式：工厂、建造者与原型

创建型设计模式关注对象的创建机制，试图将对象的创建与使用分离。本文将深入讲解工厂模式、建造者模式、原型模式和单例模式，以及它们在 JDK 和 Spring 中的实际应用。

## 简单工厂模式

简单工厂模式通过一个工厂类，根据传入的参数决定创建哪一种产品类的实例。

```java
// 产品接口
public interface PayService {
    void pay(BigDecimal amount);
}

// 具体产品
public class AlipayService implements PayService {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("支付宝支付: " + amount);
    }
}

public class WechatPayService implements PayService {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("微信支付: " + amount);
    }
}

// 简单工厂
public class PayServiceFactory {
    public static PayService createPayService(String type) {
        switch (type) {
            case "alipay":
                return new AlipayService();
            case "wechat":
                return new WechatPayService();
            default:
                throw new IllegalArgumentException("不支持的支付方式: " + type);
        }
    }
}

// 使用
PayService payService = PayServiceFactory.createPayService("alipay");
payService.pay(new BigDecimal("100.00"));
```

**优点**：客户端不需要知道具体的产品类名，只需知道参数即可
**缺点**：每增加一种产品，都需要修改工厂类，违反开闭原则

## 工厂方法模式

工厂方法模式定义一个创建对象的接口，让子类决定实例化哪个类：

```java
// 产品接口
public interface Logger {
    void log(String message);
}

// 具体产品
public class FileLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.println("写入文件: " + message);
    }
}

public class DatabaseLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.println("写入数据库: " + message);
    }
}

// 工厂接口
public interface LoggerFactory {
    Logger createLogger();
}

// 具体工厂
public class FileLoggerFactory implements LoggerFactory {
    @Override
    public Logger createLogger() {
        return new FileLogger();
    }
}

public class DatabaseLoggerFactory implements LoggerFactory {
    @Override
    public Logger createLogger() {
        return new DatabaseLogger();
    }
}

// 使用
LoggerFactory factory = new FileLoggerFactory();
Logger logger = factory.createLogger();
logger.log("用户登录成功");
```

**优点**：符合开闭原则，新增产品只需新增工厂类
**缺点**：类的数量成倍增加

## 抽象工厂模式

抽象工厂模式提供一个接口，用于创建相关或依赖对象的家族：

```java
// 产品族接口
public interface Button {
    void render();
}

public interface TextField {
    void render();
}

// Windows 风格产品
public class WindowsButton implements Button {
    @Override
    public void render() {
        System.out.println("渲染 Windows 风格按钮");
    }
}

public class WindowsTextField implements TextField {
    @Override
    public void render() {
        System.out.println("渲染 Windows 风格文本框");
    }
}

// Mac 风格产品
public class MacButton implements Button {
    @Override
    public void render() {
        System.out.println("渲染 Mac 风格按钮");
    }
}

public class MacTextField implements TextField {
    @Override
    public void render() {
        System.out.println("渲染 Mac 风格文本框");
    }
}

// 抽象工厂
public interface GUIFactory {
    Button createButton();
    TextField createTextField();
}

// 具体工厂
public class WindowsFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new WindowsButton();
    }
    
    @Override
    public TextField createTextField() {
        return new WindowsTextField();
    }
}

public class MacFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new MacButton();
    }
    
    @Override
    public TextField createTextField() {
        return new MacTextField();
    }
}

// 使用
GUIFactory factory = new MacFactory();
Button button = factory.createButton();
TextField textField = factory.createTextField();
button.render();
textField.render();
```

## 三种工厂模式对比

| 模式 | 关注点 | 产品数量 | 扩展方式 |
|------|--------|----------|----------|
| 简单工厂 | 单一产品 | 多个具体产品 | 修改工厂类 |
| 工厂方法 | 单一产品 | 多个具体产品 | 新增工厂类 |
| 抽象工厂 | 产品族 | 多个产品族 | 新增工厂类 |

```
简单工厂：一个工厂生产多种产品
工厂方法：每种产品有专门的工厂
抽象工厂：一个工厂生产一个产品族
```

## Builder 模式

Builder 模式将复杂对象的构建与表示分离，允许使用相同的构建过程创建不同的表示。

### 经典 Builder 模式

```java
public class HttpRequest {
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final String body;
    private final int timeout;
    
    private HttpRequest(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers;
        this.body = builder.body;
        this.timeout = builder.timeout;
    }
    
    public static class Builder {
        private String url;
        private String method = "GET";
        private Map<String, String> headers = new HashMap<>();
        private String body;
        private int timeout = 30000;
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder method(String method) {
            this.method = method;
            return this;
        }
        
        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }
        
        public Builder body(String body) {
            this.body = body;
            return this;
        }
        
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public HttpRequest build() {
            if (url == null) {
                throw new IllegalStateException("URL 不能为空");
            }
            return new HttpRequest(this);
        }
    }
    
    @Override
    public String toString() {
        return "HttpRequest{" +
            "url='" + url + '\'' +
            ", method='" + method + '\'' +
            ", headers=" + headers +
            ", body='" + body + '\'' +
            ", timeout=" + timeout +
            '}';
    }
}

// 使用
HttpRequest request = new HttpRequest.Builder()
    .url("https://api.example.com/users")
    .method("POST")
    .header("Content-Type", "application/json")
    .header("Authorization", "Bearer token123")
    .body("{\"name\":\"张三\"}")
    .timeout(5000)
    .build();
```

### Lombok @Builder

使用 Lombok 可以简化 Builder 模式的实现：

```java
@Builder
@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private Integer age;
}

// 使用
User user = User.builder()
    .id(1L)
    .username("张三")
    .email("zhangsan@example.com")
    .age(25)
    .build();
```

### Builder 模式的应用场景

1. **对象有很多可选参数**：避免构造函数参数过多（伸缩构造函数问题）
2. **对象创建过程复杂**：需要多个步骤才能创建一个完整的对象
3. **需要创建不可变对象**：Builder 模式天然支持创建不可变对象

### JDK 中的 Builder 模式

```java
// StringBuilder
StringBuilder sb = new StringBuilder();
sb.append("Hello").append(" ").append("World");
String result = sb.toString();

// Stream.Builder
Stream.Builder<String> builder = Stream.builder();
builder.add("a").add("b").add("c");
Stream<String> stream = builder.build();

// Locale.Builder
Locale locale = new Locale.Builder()
    .setLanguage("zh")
    .setRegion("CN")
    .build();
```

## Prototype 模式

原型模式通过复制现有的实例来创建新的实例，而不是通过 new 关键字。

### 浅拷贝

```java
public class Address implements Cloneable {
    private String city;
    private String street;
    
    // 构造函数、getter/setter 省略
    
    @Override
    public Address clone() {
        try {
            return (Address) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

public class User implements Cloneable {
    private String name;
    private Address address;
    private List<String> hobbies;
    
    // 构造函数、getter/setter 省略
    
    @Override
    public User clone() {
        try {
            return (User) super.clone();  // 浅拷贝
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

// 浅拷贝的问题
User user1 = new User("张三", new Address("北京", "朝阳"), Arrays.asList("读书", "coding"));
User user2 = user1.clone();

user2.getAddress().setCity("上海");
System.out.println(user1.getAddress().getCity());  // 输出：上海（被修改了！）

user2.getHobbies().add("游泳");
System.out.println(user1.getHobbies());  // 输出：[读书, coding, 游泳]（被修改了！）
```

### 深拷贝

```java
public class User implements Cloneable {
    private String name;
    private Address address;
    private List<String> hobbies;
    
    @Override
    public User clone() {
        try {
            User cloned = (User) super.clone();
            cloned.address = this.address.clone();  // 深拷贝 Address
            cloned.hobbies = new ArrayList<>(this.hobbies);  // 深拷贝 List
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

// 深拷贝：修改克隆对象不影响原对象
User user1 = new User("张三", new Address("北京", "朝阳"), new ArrayList<>(Arrays.asList("读书")));
User user2 = user1.clone();

user2.getAddress().setCity("上海");
System.out.println(user1.getAddress().getCity());  // 输出：北京（不受影响）

user2.getHobbies().add("游泳");
System.out.println(user1.getHobbies());  // 输出：[读书]（不受影响）
```

### 使用序列化实现深拷贝

```java
public class DeepCloneUtil {
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deepClone(T obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

## 单例模式

单例模式确保一个类只有一个实例，并提供一个全局访问点。

### 饿汉式

```java
public class Singleton {
    private static final Singleton INSTANCE = new Singleton();
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        return INSTANCE;
    }
}
```

**优点**：实现简单，线程安全
**缺点**：类加载时就创建实例，可能浪费资源

### 懒汉式（双重检查锁）

```java
public class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

**注意**：必须使用 `volatile` 关键字，防止指令重排导致的问题。

### 静态内部类

```java
public class Singleton {
    private Singleton() {}
    
    private static class SingletonHolder {
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
```

**优点**：懒加载、线程安全、无锁

### 枚举实现

```java
public enum Singleton {
    INSTANCE;
    
    public void doSomething() {
        System.out.println("单例方法");
    }
}

// 使用
Singleton.INSTANCE.doSomething();
```

**优点**：最简洁、防止反射和序列化攻击
**缺点**：不够直观

### 单例模式对比

| 实现方式 | 懒加载 | 线程安全 | 防止反射 | 防止序列化 |
|----------|--------|----------|----------|------------|
| 饿汉式 | 否 | 是 | 否 | 否 |
| 双重检查锁 | 是 | 是 | 否 | 否 |
| 静态内部类 | 是 | 是 | 否 | 否 |
| 枚举 | 否 | 是 | 是 | 是 |

## Spring 中的创建型模式

### BeanFactory（工厂模式）

Spring 的 BeanFactory 就是工厂模式的典型应用：

```java
// Spring 容器就是一个大工厂
BeanFactory factory = new ClassPathXmlApplicationContext("beans.xml");

// 从工厂获取 Bean
UserService userService = (UserService) factory.getBean("userService");
```

### @Bean 方法（工厂方法模式）

```java
@Configuration
public class AppConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("root");
        dataSource.setPassword("123456");
        return dataSource;
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### BeanDefinitionBuilder（Builder 模式）

```java
BeanDefinition definition = BeanDefinitionBuilder
    .genericBeanDefinition(UserService.class)
    .setScope("prototype")
    .addPropertyValue("name", "张三")
    .addConstructorArgValue(userRepository)
    .getBeanDefinition();
```

### @Scope("prototype")（原型模式）

```java
@Component
@Scope("prototype")
public class PrototypeBean {
    private int count;
    
    public void increment() {
        count++;
    }
    
    public int getCount() {
        return count;
    }
}

// 每次获取都是新实例
PrototypeBean bean1 = context.getBean(PrototypeBean.class);
PrototypeBean bean2 = context.getBean(PrototypeBean.class);
System.out.println(bean1 == bean2);  // false
```

## JDK 中的创建型模式

```java
// 工厂方法
List<String> list = new ArrayList<>();  // 构造函数
ExecutorService executor = Executors.newFixedThreadPool(10);  // 静态工厂

// Builder 模式
StringBuilder sb = new StringBuilder();
Stream.Builder<String> streamBuilder = Stream.builder();

// 原型模式
int[] arr1 = {1, 2, 3};
int[] arr2 = arr1.clone();

// 单例模式
Runtime.getRuntime();  // 饿汉式
Desktop.getDesktop();  // 懒汉式
```

## 模式选型指南

```
对象创建简单，参数少 → 直接 new
对象创建简单，参数多且有默认值 → Builder 模式
需要根据类型创建不同对象 → 工厂模式
需要创建对象的完整副本 → 原型模式
全局只需要一个实例 → 单例模式
需要创建一组相关对象 → 抽象工厂模式
```

创建型模式的核心目标是解耦对象的创建和使用。选择哪种模式取决于你的具体需求：参数数量、创建复杂度、是否需要多态、是否需要单例等。理解这些模式的适用场景，能帮助你写出更灵活、更可维护的代码。
