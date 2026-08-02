# 设计模式实战综合案例

设计模式的价值在于实战应用。本文将通过分析 Spring、MyBatis、Vue/React 等主流框架中的设计模式，以及一个电商订单系统的综合案例，展示设计模式在真实项目中的应用。

## Spring 中的设计模式

### 工厂模式：BeanFactory

Spring 的 IoC 容器本质上就是一个大型工厂：

```java
// BeanFactory 接口
public interface BeanFactory {
    Object getBean(String name) throws BeansException;
    <T> T getBean(Class<T> requiredType) throws BeansException;
    boolean containsBean(String name);
}

// 使用
BeanFactory factory = new ClassPathXmlApplicationContext("beans.xml");
UserService userService = (UserService) factory.getBean("userService");

// 或者通过类型获取
UserService userService = factory.getBean(UserService.class);
```

### 工厂方法模式：@Bean

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        ds.setUsername("root");
        ds.setPassword("123456");
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### 代理模式：AOP

Spring AOP 的核心就是动态代理：

```java
// JDK 动态代理（基于接口）
public class JdkDynamicProxy implements InvocationHandler {
    private Object target;

    public JdkDynamicProxy(Object target) {
        this.target = target;
    }

    public Object getProxy() {
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            this
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("Before: " + method.getName());
        Object result = method.invoke(target, args);
        System.out.println("After: " + method.getName());
        return result;
    }
}

// 使用
UserService target = new UserServiceImpl();
UserService proxy = (UserService) new JdkDynamicProxy(target).getProxy();
proxy.save(user);  // 会执行前置和后置通知
```

```java
// Spring 中使用 AOP
@Aspect
@Component
public class LogAspect {

    @Pointcut("execution(* com.example.service.*.*(..))")
    public void serviceMethod() {}

    @Around("serviceMethod()")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();

        System.out.println("开始执行: " + methodName);

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            System.out.println(methodName + " 执行完成，耗时: " + cost + "ms");
            return result;
        } catch (Exception e) {
            System.out.println(methodName + " 执行异常: " + e.getMessage());
            throw e;
        }
    }
}
```

### 模板方法模式：JdbcTemplate

```java
// JdbcTemplate 的核心设计
public class JdbcTemplate {

    public <T> T execute(String sql, PreparedStatementCallback<T> action) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sql);
            return action.doInPreparedStatement(ps);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            // 模板方法负责资源管理
            JdbcUtils.closeStatement(ps);
            JdbcUtils.closeConnection(conn);
        }
    }

    public List<Map<String, Object>> queryForList(String sql) {
        return execute(sql, ps -> {
            ResultSet rs = ps.executeQuery();
            // 将 ResultSet 转换为 List<Map>
            // ...
        });
    }
}

// 使用：只需关注 SQL 和结果映射
List<User> users = jdbcTemplate.query(
    "SELECT * FROM user WHERE age > ?",
    (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        return user;
    },
    18
);
```

### 观察者模式：ApplicationEvent

```java
// 定义事件
public class UserRegisteredEvent extends ApplicationEvent {
    private final User user;

    public UserRegisteredEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}

// 发布事件
@Service
public class UserService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public void register(User user) {
        // 保存用户
        userRepository.save(user);
        // 发布事件
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user));
    }
}

// 监听事件
@Component
public class EmailService {

    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        User user = event.getUser();
        sendWelcomeEmail(user.getEmail());
    }
}

@Component
public class PointsService {

    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        User user = event.getUser();
        addWelcomePoints(user.getId(), 100);
    }
}
```

### 策略模式：Resource 接口

```java
// Spring 的 Resource 接口就是策略模式
public interface Resource {
    InputStream getInputStream() throws IOException;
    boolean exists();
    String getDescription();
}

// 不同的资源获取策略
Resource classpathResource = new ClassPathResource("application.yml");
Resource fileResource = new FileSystemResource("/path/to/file");
Resource urlResource = new UrlResource("https://example.com/config.yml");

// 统一的使用方式
try (InputStream is = classpathResource.getInputStream()) {
    // 读取资源
}
```

## MyBatis 中的设计模式

### 建造者模式：SqlSessionFactoryBuilder

```java
// MyBatis 的 SqlSessionFactory 构建过程
String resource = "mybatis-config.xml";
InputStream inputStream = Resources.getResourceAsStream(resource);

// 使用建造者模式构建 SqlSessionFactory
SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(inputStream);

// 源码中的 Builder 设计
public class SqlSessionFactoryBuilder {
    public SqlSessionFactory build(InputStream inputStream) {
        return build(inputStream, null, null);
    }

    public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
        XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
        return build(parser.parse());
    }

    public SqlSessionFactory build(Configuration config) {
        return new DefaultSqlSessionFactory(config);
    }
}
```

### 代理模式：MapperProxy

```java
// MyBatis 的 Mapper 接口调用实际上都是代理
public interface UserMapper {
    User selectById(Long id);
    List<User> selectByName(String name);
    int insert(User user);
}

// 使用
UserMapper mapper = sqlSession.getMapper(UserMapper.class);
User user = mapper.selectById(1L);  // 实际上是代理调用

// 源码中的代理实现
public class MapperProxy<T> implements InvocationHandler {
    private final SqlSession sqlSession;
    private final Class<T> mapperInterface;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 将方法调用转换为 SQL 执行
        String statement = mapperInterface.getName() + "." + method.getName();
        return sqlSession.selectOne(statement, args[0]);
    }
}
```

### 工厂模式：MapperProxyFactory

```java
// Mapper 代理工厂
public class MapperProxyFactory<T> {
    private final Class<T> mapperInterface;

    public T newInstance(SqlSession sqlSession) {
        MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface);
        return (T) Proxy.newProxyInstance(
            mapperInterface.getClassLoader(),
            new Class[]{mapperInterface},
            mapperProxy
        );
    }
}
```

### 装饰器模式：Executor 增强

```java
// MyBatis 的 Executor 使用装饰器模式增强
public interface Executor {
    int update(MappedStatement ms, Object parameter) throws SQLException;
    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException;
}

// 基础实现
public class SimpleExecutor implements Executor {
    // 简单的 SQL 执行
}

// 缓存装饰器
public class CachingExecutor implements Executor {
    private final Executor delegate;
    private final Cache cache;

    public CachingExecutor(Executor delegate) {
        this.delegate = delegate;
        this.cache = new PerpetualCache("default");
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, ...) throws SQLException {
        CacheKey key = createCacheKey(ms, parameter, rowBounds);
        // 先查缓存
        List<E> cachedList = (List<E>) cache.getObject(key);
        if (cachedList != null) {
            return cachedList;
        }
        // 缓存未命中，委托给实际执行器
        cachedList = delegate.query(ms, parameter, rowBounds, resultHandler);
        cache.putObject(key, cachedList);
        return cachedList;
    }
}
```

## Vue/React 中的设计模式

### 观察者模式：Vue 响应式系统

```javascript
// Vue 3 的响应式系统
const { ref, reactive, watch, computed } = Vue

// 响应式数据
const count = ref(0)
const state = reactive({
  name: '张三',
  age: 25
})

// 观察者：监听数据变化
watch(count, (newValue, oldValue) => {
  console.log(`count 从 ${oldValue} 变为 ${newValue}`)
})

// 计算属性：派生状态
const doubleCount = computed(() => count.value * 2)

// 当数据变化时，相关的视图和副作用会自动更新
count.value++  // 触发 watcher，视图更新
```

### 组合模式：Vue/React 组件树

```javascript
// Vue 组件树就是组合模式
// App.vue
<template>
  <div id="app">
    <Header />
    <Main>
      <Sidebar />
      <Content>
        <ArticleList />
        <Pagination />
      </Content>
    </Main>
    <Footer />
  </div>
</template>

// 每个组件都是一个节点
// 叶子节点：Header, Sidebar, ArticleList, Pagination, Footer
// 容器节点：App, Main, Content
```

```jsx
// React 的组件组合
function App() {
  return (
    <Layout>
      <Header>
        <Logo />
        <Navigation />
      </Header>
      <Content>
        <ArticleList />
        <Sidebar>
          <UserProfile />
          <TagCloud />
        </Sidebar>
      </Content>
      <Footer />
    </Layout>
  )
}
```

### 策略模式：Vue 自定义指令

```javascript
// 不同的验证策略
const strategies = {
  required: (value) => value !== '' && value !== null && value !== undefined,
  email: (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value),
  phone: (value) => /^1[3-9]\d{9}$/.test(value),
  minLength: (value, length) => value.length >= length,
  maxLength: (value, length) => value.length <= length
}

// 表单验证器
function validate(value, rules) {
  for (const rule of rules) {
    const [strategy, ...params] = rule.split(':')
    const validator = strategies[strategy]
    if (!validator(value, ...params)) {
      return `${strategy} 验证失败`
    }
  }
  return null
}

// 使用
const error = validate('abc', ['required', 'minLength:6'])
```

## 综合案例：电商订单系统

### 系统设计

```
需求分析：
- 用户可以浏览商品、添加购物车、下单购买
- 订单支持多种支付方式
- 订单状态流转：待支付 → 已支付 → 已发货 → 已完成
- 支持订单取消和退款
```

### 工厂模式：支付方式创建

```java
// 支付接口
public interface PaymentService {
    PayResult pay(Order order, BigDecimal amount);
    RefundResult refund(Order order, BigDecimal amount);
}

// 支付宝实现
@Service("alipay")
public class AlipayService implements PaymentService {
    @Override
    public PayResult pay(Order order, BigDecimal amount) {
        // 调用支付宝 API
        return new PayResult(true, "支付宝支付成功");
    }

    @Override
    public RefundResult refund(Order order, BigDecimal amount) {
        return new RefundResult(true, "支付宝退款成功");
    }
}

// 微信支付实现
@Service("wechat")
public class WechatPayService implements PaymentService {
    @Override
    public PayResult pay(Order order, BigDecimal amount) {
        return new PayResult(true, "微信支付成功");
    }

    @Override
    public RefundResult refund(Order order, BigDecimal amount) {
        return new RefundResult(true, "微信退款成功");
    }
}

// 支付工厂
@Component
public class PaymentFactory {
    @Autowired
    private Map<String, PaymentService> paymentServiceMap;

    public PaymentService getPaymentService(String paymentType) {
        PaymentService service = paymentServiceMap.get(paymentType);
        if (service == null) {
            throw new IllegalArgumentException("不支持的支付方式: " + paymentType);
        }
        return service;
    }
}
```

### 状态模式：订单状态管理

```java
// 订单状态接口
public interface OrderState {
    void pay(Order order);
    void ship(Order order);
    void complete(Order order);
    void cancel(Order order);
    String getStateName();
}

// 待支付状态
@Component
public class PendingPaymentState implements OrderState {
    @Override
    public void pay(Order order) {
        order.setState(OrderStateEnum.PAID);
        System.out.println("订单 " + order.getId() + " 支付成功");
    }

    @Override
    public void ship(Order order) {
        throw new IllegalStateException("待支付订单不能发货");
    }

    @Override
    public void complete(Order order) {
        throw new IllegalStateException("待支付订单不能完成");
    }

    @Override
    public void cancel(Order order) {
        order.setState(OrderStateEnum.CANCELLED);
        // 恢复库存
        restoreStock(order);
        System.out.println("订单 " + order.getId() + " 已取消");
    }

    @Override
    public String getStateName() {
        return "待支付";
    }
}

// 已支付状态
@Component
public class PaidState implements OrderState {
    @Override
    public void pay(Order order) {
        throw new IllegalStateException("订单已支付");
    }

    @Override
    public void ship(Order order) {
        order.setState(OrderStateEnum.SHIPPED);
        System.out.println("订单 " + order.getId() + " 已发货");
    }

    @Override
    public void complete(Order order) {
        throw new IllegalStateException("订单未发货，不能完成");
    }

    @Override
    public void cancel(Order order) {
        order.setState(OrderStateEnum.CANCELLED);
        // 退款
        refund(order);
        restoreStock(order);
        System.out.println("订单 " + order.getId() + " 已取消并退款");
    }

    @Override
    public String getStateName() {
        return "已支付";
    }
}

// 订单类
@Data
public class Order {
    private Long id;
    private OrderStateEnum state;
    private String paymentType;
    private BigDecimal totalAmount;
    private List<OrderItem> items;

    public void pay() {
        OrderState stateHandler = getStateHandler();
        stateHandler.pay(this);
    }

    public void ship() {
        OrderState stateHandler = getStateHandler();
        stateHandler.ship(this);
    }

    public void complete() {
        OrderState stateHandler = getStateHandler();
        stateHandler.complete(this);
    }

    public void cancel() {
        OrderState stateHandler = getStateHandler();
        stateHandler.cancel(this);
    }

    @Autowired
    private OrderStateFactory stateFactory;

    private OrderState getStateHandler() {
        return stateFactory.getState(state);
    }
}
```

### 观察者模式：订单事件

```java
// 订单事件
public abstract class OrderEvent extends ApplicationEvent {
    private final Order order;

    public OrderEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order getOrder() { return order; }
}

public class OrderCreatedEvent extends OrderEvent {
    public OrderCreatedEvent(Object source, Order order) {
        super(source, order);
    }
}

public class OrderPaidEvent extends OrderEvent {
    public OrderPaidEvent(Object source, Order order) {
        super(source, order);
    }
}

public class OrderCancelledEvent extends OrderEvent {
    public OrderCancelledEvent(Object source, Order order) {
        super(source, order);
    }
}

// 事件监听器
@Component
public class OrderEventListener {

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 发送订单确认邮件
        emailService.sendOrderConfirmation(event.getOrder());
    }

    @EventListener
    public void handleOrderPaid(OrderPaidEvent event) {
        // 发送支付成功通知
        smsService.sendPaymentNotification(event.getOrder());
        // 更新销售统计
        statisticsService.updateSales(event.getOrder());
    }

    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        // 发送取消通知
        emailService.sendCancellationNotice(event.getOrder());
    }
}
```

### 策略模式：优惠计算

```java
// 优惠策略接口
public interface DiscountStrategy {
    BigDecimal calculate(BigDecimal originalPrice, Order order);
    String getStrategyName();
}

// 满减策略
@Component
public class FullReductionStrategy implements DiscountStrategy {
    @Override
    public BigDecimal calculate(BigDecimal originalPrice, Order order) {
        // 满 200 减 20
        if (originalPrice.compareTo(new BigDecimal("200")) >= 0) {
            return originalPrice.subtract(new BigDecimal("20"));
        }
        return originalPrice;
    }

    @Override
    public String getStrategyName() {
        return "满减";
    }
}

// 折扣策略
@Component
public class PercentageDiscountStrategy implements DiscountStrategy {
    private BigDecimal discount = new BigDecimal("0.9");  // 9 折

    @Override
    public BigDecimal calculate(BigDecimal originalPrice, Order order) {
        return originalPrice.multiply(discount);
    }

    @Override
    public String getStrategyName() {
        return "打折";
    }
}

// 优惠计算上下文
@Component
public class DiscountContext {
    @Autowired
    private Map<String, DiscountStrategy> strategyMap;

    public BigDecimal calculatePrice(BigDecimal originalPrice, Order order, String discountType) {
        DiscountStrategy strategy = strategyMap.get(discountType);
        if (strategy == null) {
            return originalPrice;  // 无优惠
        }
        return strategy.calculate(originalPrice, order);
    }
}
```

### 建造者模式：订单构建

```java
// 订单构建器
public class OrderBuilder {
    private Long userId;
    private List<OrderItem> items = new ArrayList<>();
    private String paymentType;
    private String address;
    private String discountType;
    private String couponCode;

    public OrderBuilder userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public OrderBuilder addItem(Long productId, int quantity, BigDecimal price) {
        items.add(new OrderItem(productId, quantity, price));
        return this;
    }

    public OrderBuilder paymentType(String paymentType) {
        this.paymentType = paymentType;
        return this;
    }

    public OrderBuilder address(String address) {
        this.address = address;
        return this;
    }

    public OrderBuilder discountType(String discountType) {
        this.discountType = discountType;
        return this;
    }

    public OrderBuilder couponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    public Order build() {
        // 验证
        if (userId == null) throw new IllegalStateException("用户ID不能为空");
        if (items.isEmpty()) throw new IllegalStateException("订单项不能为空");
        if (paymentType == null) throw new IllegalStateException("支付方式不能为空");

        // 计算总价
        BigDecimal totalPrice = items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUserId(userId);
        order.setItems(items);
        order.setPaymentType(paymentType);
        order.setAddress(address);
        order.setTotalAmount(totalPrice);
        order.setState(OrderStateEnum.PENDING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now());

        return order;
    }
}

// 使用
Order order = new OrderBuilder()
    .userId(1L)
    .addItem(1001L, 2, new BigDecimal("99.99"))
    .addItem(1002L, 1, new BigDecimal("199.99"))
    .paymentType("alipay")
    .address("北京市朝阳区xxx")
    .discountType("fullReduction")
    .build();
```

### 模式应用总结

| 模式 | 应用场景 | 解决的问题 |
|------|----------|------------|
| 工厂模式 | 支付方式创建 | 解耦对象创建和使用 |
| 状态模式 | 订单状态管理 | 管理复杂的状态转换 |
| 观察者模式 | 订单事件通知 | 解耦事件发布和处理 |
| 策略模式 | 优惠计算 | 支持多种优惠策略 |
| 建造者模式 | 订单构建 | 复杂对象的构建过程 |

设计模式不是为了使用而使用，而是为了解决实际问题。在设计系统时，先分析问题的特征，再选择合适的模式。好的设计应该是自然的、简洁的，而不是刻意堆砌模式。
