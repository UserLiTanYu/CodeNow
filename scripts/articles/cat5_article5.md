# 代理模式与动态代理

代理模式是 Java 中最常用的设计模式之一，它通过引入代理对象来控制对目标对象的访问。动态代理则将这一模式提升到了新的高度，允许在运行时动态创建代理类。本文将深入探讨代理模式的原理、实现和实际应用。

## 代理模式的概念

代理模式（Proxy Pattern）为目标对象提供一个代理，通过代理来控制对目标对象的访问。代理对象与目标对象实现相同的接口，客户端通过代理来间接访问目标对象。

代理模式的主要用途：

- **访问控制**：控制对目标对象的访问权限
- **功能增强**：在不修改目标对象的情况下添加额外功能
- **延迟初始化**：延迟创建开销大的对象
- **远程代理**：为远程对象提供本地代理

## 静态代理

静态代理是最简单的代理实现方式，需要手动编写代理类：

```java
// 接口定义
public interface UserService {
    void addUser(String name);
    String getUser(int id);
}

// 目标类
public class UserServiceImpl implements UserService {
    @Override
    public void addUser(String name) {
        System.out.println("添加用户: " + name);
    }

    @Override
    public String getUser(int id) {
        return "User_" + id;
    }
}

// 代理类
public class UserServiceProxy implements UserService {
    private final UserService target;

    public UserServiceProxy(UserService target) {
        this.target = target;
    }

    @Override
    public void addUser(String name) {
        System.out.println("[日志] 开始执行 addUser");
        long start = System.currentTimeMillis();

        target.addUser(name);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[日志] addUser 执行完成，耗时: " + elapsed + "ms");
    }

    @Override
    public String getUser(int id) {
        System.out.println("[日志] 开始执行 getUser");
        String result = target.getUser(id);
        System.out.println("[日志] getUser 返回: " + result);
        return result;
    }
}

// 使用
UserService target = new UserServiceImpl();
UserService proxy = new UserServiceProxy(target);
proxy.addUser("Alice");
```

静态代理的结构清晰，但存在明显局限：

- 每个接口都需要编写一个代理类
- 代理类的代码重复度高
- 接口方法变更时，代理类也需要同步修改

## JDK 动态代理

JDK 动态代理通过 `java.lang.reflect.Proxy` 在运行时动态生成代理类，解决了静态代理的局限性。

### Proxy.newProxyInstance()

```java
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JdkProxyDemo {
    public static void main(String[] args) {
        // 目标对象
        UserService target = new UserServiceImpl();

        // 创建代理
        UserService proxy = (UserService) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),    // 类加载器
            target.getClass().getInterfaces(),      // 代理接口
            new LogInvocationHandler(target)        // 调用处理器
        );

        // 通过代理调用方法
        proxy.addUser("Alice");
        String user = proxy.getUser(1);
        System.out.println("获取到用户: " + user);
    }
}

// 调用处理器
class LogInvocationHandler implements InvocationHandler {
    private final Object target;

    public LogInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("[日志] 调用方法: " + method.getName());

        long start = System.currentTimeMillis();
        Object result = method.invoke(target, args);  // 反射调用目标方法
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("[日志] 方法执行完成，耗时: " + elapsed + "ms");
        return result;
    }
}
```

### InvocationHandler 接口

`InvocationHandler` 是动态代理的核心接口，定义了 `invoke` 方法：

```java
public interface InvocationHandler {
    /**
     * @param proxy   代理对象本身
     * @param method  被调用的方法
     * @param args    方法参数
     * @return 方法返回值
     */
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable;
}
```

注意事项：
- 在 `invoke` 中调用 `proxy` 的方法会导致无限递归
- `method.invoke(target, args)` 调用的是目标对象的方法

### 日志记录代理示例

```java
public class LoggingProxyFactory {
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target) {
        return (T) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            (proxy, method, args) -> {
                String className = target.getClass().getSimpleName();
                String methodName = method.getName();

                System.out.println("[LOG] " + className + "." + methodName
                    + " 开始执行，参数: " + Arrays.toString(args));

                try {
                    Object result = method.invoke(target, args);
                    System.out.println("[LOG] " + className + "." + methodName
                        + " 执行完成，返回值: " + result);
                    return result;
                } catch (InvocationTargetException e) {
                    System.out.println("[LOG] " + className + "." + methodName
                        + " 抛出异常: " + e.getTargetException().getMessage());
                    throw e.getTargetException();
                }
            }
        );
    }
}

// 使用
UserService proxy = LoggingProxyFactory.createProxy(new UserServiceImpl());
proxy.addUser("Bob");
```

### 性能监控代理示例

```java
public class PerformanceProxyFactory {
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target) {
        return (T) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            (proxy, method, args) -> {
                long start = System.nanoTime();
                try {
                    return method.invoke(target, args);
                } finally {
                    long elapsed = System.nanoTime() - start;
                    System.out.printf("[性能] %s.%s 耗时: %.2fms%n",
                        target.getClass().getSimpleName(),
                        method.getName(),
                        elapsed / 1_000_000.0);
                }
            }
        );
    }
}
```

## CGLIB 动态代理

CGLIB（Code Generation Library）通过生成目标类的子类来实现代理，不需要目标类实现接口。

### 原理

CGLIB 使用字节码技术在运行时生成目标类的子类，并重写目标类的方法：

```java
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class CglibProxyDemo {
    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(UserServiceImpl.class);  // 设置父类
        enhancer.setCallback(new LogMethodInterceptor()); // 设置回调

        UserServiceImpl proxy = (UserServiceImpl) enhancer.create();
        proxy.addUser("Alice");
    }
}

class LogMethodInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args,
                            MethodProxy methodProxy) throws Throwable {
        System.out.println("[CGLIB 日志] 调用方法: " + method.getName());

        long start = System.currentTimeMillis();
        Object result = methodProxy.invokeSuper(obj, args);  // 调用父类方法
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("[CGLIB 日志] 耗时: " + elapsed + "ms");
        return result;
    }
}
```

### JDK 代理与 CGLIB 的区别

| 特性 | JDK 动态代理 | CGLIB 动态代理 |
|------|-------------|----------------|
| 实现方式 | 基于接口 | 基于继承 |
| 要求 | 目标类必须实现接口 | 目标类不能是 final |
| 性能 | 创建快，调用稍慢 | 创建慢，调用快 |
| 依赖 | JDK 自带 | 需要额外引入 cglib |
| final 方法 | 可以代理 | 无法代理 |

## Spring AOP 中的代理选择

Spring AOP 默认的代理选择策略：

- 如果目标类实现了接口，默认使用 JDK 动态代理
- 如果目标类没有实现接口，使用 CGLIB
- Spring Boot 2.x 默认使用 CGLIB（`spring.aop.proxy-target-class=true`）

```java
// Spring 配置
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)  // 强制使用 CGLIB
public class AopConfig {
}

// 或在 application.yml 中配置
// spring:
//   aop:
//     proxy-target-class: true
```

Spring 中查看代理类型：

```java
@Service
public class UserServiceImpl implements UserService {
    // ...
}

@RestController
public class TestController {
    @Autowired
    private UserService userService;

    @GetMapping("/test")
    public String test() {
        // 判断是否为代理对象
        System.out.println("是否为代理: " + AopUtils.isAopProxy(userService));
        System.out.println("是否为 JDK 代理: " + AopUtils.isJdkDynamicProxy(userService));
        System.out.println("是否为 CGLIB 代理: " + AopUtils.isCglibProxy(userService));
        return "ok";
    }
}
```

## 动态代理的实际应用

### RPC 调用

在 RPC 框架中，动态代理用于将本地接口调用转换为远程调用：

```java
// RPC 代理工厂
public class RpcProxyFactory {
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceClass, String serverAddress) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class[]{interfaceClass},
            (proxy, method, args) -> {
                // 构建 RPC 请求
                RpcRequest request = new RpcRequest();
                request.setInterfaceName(interfaceClass.getName());
                request.setMethodName(method.getName());
                request.setParameterTypes(method.getParameterTypes());
                request.setArguments(args);

                // 发送远程调用
                RpcResponse response = RpcClient.send(serverAddress, request);

                if (response.hasException()) {
                    throw response.getException();
                }
                return response.getResult();
            }
        );
    }
}

// 使用
UserService userService = RpcProxyFactory.createProxy(
    UserService.class, "192.168.1.100:8080");
userService.addUser("Alice");  // 远程调用
```

### MyBatis Mapper 接口

MyBatis 使用动态代理将 Mapper 接口方法转换为 SQL 执行：

```java
// Mapper 代理工厂（简化版）
public class MapperProxyFactory<T> {
    private final Class<T> mapperInterface;

    public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @SuppressWarnings("unchecked")
    public T newInstance(SqlSession sqlSession) {
        return (T) Proxy.newProxyInstance(
            mapperInterface.getClassLoader(),
            new Class[]{mapperInterface},
            new MapperProxy(sqlSession, mapperInterface)
        );
    }
}

class MapperProxy implements InvocationHandler {
    private final SqlSession sqlSession;
    private final Class<?> mapperInterface;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 解析方法对应的 SQL
        String statementId = mapperInterface.getName() + "." + method.getName();

        // 根据方法返回类型执行不同操作
        if (method.getReturnType().isAssignableFrom(List.class)) {
            return sqlSession.selectList(statementId, args);
        } else {
            return sqlSession.selectOne(statementId, args);
        }
    }
}

// MyBatis 中的使用
SqlSession session = sqlSessionFactory.openSession();
UserMapper mapper = session.getMapper(UserMapper.class);
User user = mapper.selectById(1);  // 实际执行 SQL
```

### 事务管理

Spring 的事务管理也是基于动态代理：

```java
// 事务代理（简化版）
public class TransactionProxyFactory {
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target, PlatformTransactionManager txManager) {
        return (T) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            (proxy, method, args) -> {
                TransactionStatus status = txManager.getTransaction(
                    new DefaultTransactionDefinition());

                try {
                    Object result = method.invoke(target, args);
                    txManager.commit(status);
                    return result;
                } catch (InvocationTargetException e) {
                    txManager.rollback(status);
                    throw e.getTargetException();
                }
            }
        );
    }
}

// Spring 中的 @Transactional 注解就是通过这种方式实现的
@Service
public class OrderService {
    @Transactional
    public void createOrder(Order order) {
        // 事务内的操作
        orderRepository.save(order);
        inventoryService.deduct(order.getProductId(), order.getQuantity());
    }
}
```

## 深入理解 InvocationHandler

`InvocationHandler` 的 `invoke` 方法参数含义：

```java
public class DetailedInvocationHandler implements InvocationHandler {
    private final Object target;

    public DetailedInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // proxy: 动态生成的代理对象，注意不要在 invoke 中调用 proxy 的方法
        // method: 被调用的方法的 Method 对象
        // args: 方法参数

        // 获取方法信息
        String methodName = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();

        System.out.println("方法名: " + methodName);
        System.out.println("参数类型: " + Arrays.toString(paramTypes));
        System.out.println("返回类型: " + returnType.getName());

        // 处理 Object 类的方法
        if ("toString".equals(methodName)) {
            return "Proxy for " + target.getClass().getSimpleName();
        }

        if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
        }

        if ("equals".equals(methodName)) {
            return proxy == args[0];
        }

        // 调用目标方法
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            // 提取目标方法抛出的实际异常
            throw e.getTargetException();
        }
    }
}
```

注意事项：
- `proxy` 参数是代理对象本身，在 `invoke` 中调用 `proxy` 的方法会导致无限递归
- 通过 `method.invoke(target, args)` 调用目标对象的方法
- 异常处理时需要解包 `InvocationTargetException`

```java
// 错误示例：会导致 StackOverflowError
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    return method.invoke(proxy, args);  // 错误！proxy 是代理对象
}

// 正确示例
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    return method.invoke(target, args);  // 正确！target 是目标对象
}
```

动态代理是 Java 反射机制的重要应用，也是 Spring AOP、MyBatis、Dubbo 等框架的核心技术。理解动态代理的原理和用法，对于深入学习这些框架至关重要。
