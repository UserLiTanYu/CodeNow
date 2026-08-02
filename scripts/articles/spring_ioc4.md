# @Configuration 与条件装配

Spring 框架的核心是 IoC 容器，而配置类是定义 Bean 的重要方式。随着 Spring Boot 的兴起，基于注解的配置逐渐取代了 XML 配置，其中 `@Configuration` 注解成为 Java 配置类的核心标识。本文将深入探讨 `@Configuration` 的工作机制、条件装配原理以及 Spring Boot 自动配置的底层实现。

## @Configuration 注解：Full 模式与 Lite 模式

`@Configuration` 注解用于标记一个类为配置类，等价于 Spring XML 配置文件。然而，很多人不知道的是，`@Configuration` 存在两种工作模式：Full 模式和 Lite 模式。

### Full 模式（默认）

当配置类被 `@Configuration` 注解标记时，默认处于 Full 模式。在此模式下，Spring 会通过 CGLIB 动态代理对配置类进行增强，确保 `@Bean` 方法之间的调用始终保持单例语义。

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        // 每次调用都返回同一个实例
        return new HikariDataSource();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        // 调用 dataSource() 方法，返回的是同一个 DataSource 实例
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public TransactionManager transactionManager() {
        // 同样调用 dataSource()，返回的是同一个实例
        return new DataSourceTransactionManager(dataSource());
    }
}
```

在 Full 模式下，Spring 使用 CGLIB 生成 `AppConfig` 的子类（代理类），重写所有 `@Bean` 方法。当 `jdbcTemplate()` 方法内部调用 `dataSource()` 时，代理类会拦截这个调用，从容器中获取已创建的 `DataSource` Bean，而不是重新执行 `dataSource()` 方法创建新实例。

### Lite 模式

当配置类仅使用 `@Component`、`@Service` 等注解（而非 `@Configuration`）标记，或者 `@Configuration(proxyBeanMethods = false)` 时，配置类处于 Lite 模式。

```java
@Configuration(proxyBeanMethods = false)
public class LiteAppConfig {

    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        // 每次调用都会创建新的 DataSource 实例！
        return new JdbcTemplate(dataSource());
    }
}
```

### Full 与 Lite 的选择

| 特性 | Full 模式 | Lite 模式 |
|------|-----------|-----------|
| CGLIB 代理 | 是 | 否 |
| Bean 方法间调用语义 | 单例 | 每次新建 |
| 启动性能 | 较慢（需要生成代理类） | 较快 |
| 内存占用 | 较高 | 较低 |
| 适用场景 | Bean 之间有依赖关系 | Bean 之间无依赖，或使用构造器注入 |

Spring Boot 2.x 以后，推荐在组件扫描的配置类中使用 Lite 模式以提升启动速度，仅在显式需要方法调用保持单例语义时使用 Full 模式。

## @Bean 注解详解

`@Bean` 注解是配置类中定义 Bean 的核心方式，相当于 XML 配置中的 `<bean>` 元素。

### 基本用法

```java
@Bean
public UserService userService() {
    return new UserServiceImpl();
}
```

### initMethod 和 destroyMethod

`@Bean` 注解支持指定初始化和销毁方法，对应 Bean 的生命周期回调。

```java
@Bean(initMethod = "init", destroyMethod = "close")
public DataSource dataSource() {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl("jdbc:mysql://localhost:3306/test");
    ds.setUsername("root");
    return ds;
}
```

如果不显式指定 `destroyMethod`，Spring 会自动推断并调用 `close()` 或 `shutdown()` 方法。可以通过 `@Bean(destroyMethod = "")` 禁用自动推断。

### name 别名

`@Bean` 注解的 `name` 属性可以为 Bean 指定一个或多个别名。

```java
@Bean(name = {"userService", "myUserService", "userSvc"})
public UserService userService() {
    return new UserServiceImpl();
}
```

也可以使用简写形式 `@Bean("userService")`。

### 依赖注入

通过方法参数实现 Bean 之间的依赖注入，Spring 容器会自动注入所需的 Bean。

```java
@Bean
public OrderService orderService(UserService userService, 
                                  ProductService productService) {
    return new OrderService(userService, productService);
}
```

## 组件扫描：@ComponentScan 过滤器

`@ComponentScan` 注解用于配置 Spring 扫描哪些包以发现组件。通过过滤器（Filter）可以精细控制扫描行为。

### 基本扫描配置

```java
@Configuration
@ComponentScan(basePackages = "com.example",
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = Repository.class
    ),
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = InternalService.class
    )
)
public class ScanConfig {
}
```

### 过滤器类型

| FilterType | 说明 | 示例 |
|------------|------|------|
| ANNOTATION | 基于注解 | `@Controller`、`@Service` |
| ASSIGNABLE_TYPE | 基于类型 | `UserService.class` |
| ASPECTJ | 基于 AspectJ 表达式 | `com.example..*Service` |
| REGEX | 基于正则表达式 | `.*Service` |
| CUSTOM | 自定义 TypeFilter 实现 | `MyTypeFilter.class` |

### 自定义 TypeFilter

```java
public class ExcludeTestFilter implements TypeFilter {

    @Override
    public boolean match(MetadataReader metadataReader,
                         MetadataReaderFactory metadataReaderFactory)
            throws IOException {
        // 获取类名
        String className = metadataReader.getClassMetadata().getClassName();
        // 排除名称包含 "Test" 的类
        return className.contains("Test");
    }
}
```

使用自定义过滤器：

```java
@ComponentScan(
    basePackages = "com.example",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.CUSTOM,
        classes = ExcludeTestFilter.class
    )
)
```

## 条件装配注解

条件装配是 Spring 4.x 引入的特性，允许根据特定条件决定是否创建 Bean。这是 Spring Boot 自动配置的基石。

### @Conditional

`@Conditional` 是所有条件注解的元注解，需要配合 `Condition` 接口的实现使用。

```java
public class LinuxCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context,
                           AnnotatedTypeMetadata metadata) {
        String os = context.getEnvironment().getProperty("os.name");
        return os != null && os.toLowerCase().contains("linux");
    }
}

public class WindowsCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context,
                           AnnotatedTypeMetadata metadata) {
        String os = context.getEnvironment().getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows");
    }
}
```

使用 `@Conditional`：

```java
@Configuration
public class OSConfig {

    @Bean
    @Conditional(LinuxCondition.class)
    public CommandLineRunner linuxRunner() {
        return args -> System.out.println("Running on Linux");
    }

    @Bean
    @Conditional(WindowsCondition.class)
    public CommandLineRunner windowsRunner() {
        return args -> System.out.println("Running on Windows");
    }
}
```

### @ConditionalOnClass

当类路径上存在指定类时，才会创建 Bean。

```java
@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
    public CacheManager caffeineCacheManager() {
        return new CaffeineCacheManager();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        return new RedisCacheManager.RedisCacheManagerBuilder(connectionFactory).build();
    }
}
```

### @ConditionalOnMissingBean

当容器中不存在指定类型的 Bean 时，才会创建。

```java
@Configuration
public class DefaultServiceConfig {

    @Bean
    @ConditionalOnMissingBean(UserService.class)
    public UserService defaultUserService() {
        return new DefaultUserService();
    }
}
```

这个注解常用于提供默认实现，允许用户覆盖自动配置的 Bean。

### @ConditionalOnProperty

基于配置属性决定是否创建 Bean。

```java
@Configuration
public class FeatureConfig {

    @Bean
    @ConditionalOnProperty(
        name = "app.feature.cache.enabled",
        havingValue = "true",
        matchIfMissing = false
    )
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    @ConditionalOnProperty(
        name = "app.feature.async.enabled",
        havingValue = "true",
        matchIfMissing = true  // 默认启用
    )
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        return executor;
    }
}
```

### 组合条件注解

Spring Boot 提供了丰富的组合条件注解：

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnProductionEnvironmentCondition.class)
public @interface ConditionalOnProduction {
}
```

常用组合条件注解：

| 注解 | 说明 |
|------|------|
| `@ConditionalOnClass` | 类路径上存在指定类 |
| `@ConditionalOnMissingClass` | 类路径上不存在指定类 |
| `@ConditionalOnBean` | 容器中存在指定 Bean |
| `@ConditionalOnMissingBean` | 容器中不存在指定 Bean |
| `@ConditionalOnProperty` | 指定配置属性为特定值 |
| `@ConditionalOnResource` | 类路径上存在指定资源 |
| `@ConditionalOnWebApplication` | 当前是 Web 应用 |
| `@ConditionalOnNotWebApplication` | 当前不是 Web 应用 |
| `@ConditionalOnExpression` | SpEL 表达式结果为 true |

## @Profile：环境特定 Bean

`@Profile` 注解用于根据激活的 Profile 有条件地注册 Bean，适用于开发、测试、生产等不同环境。

### 定义 Profile Bean

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:h2:mem:devdb");
        ds.setUsername("sa");
        return ds;
    }

    @Bean
    @Profile("test")
    public DataSource testDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/testdb");
        ds.setUsername("test");
        return ds;
    }

    @Bean
    @Profile("prod")
    public DataSource prodDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://prod-server:3306/proddb");
        ds.setUsername("prod");
        return ds;
    }
}
```

### 激活 Profile

有多种方式激活 Profile：

1. **配置文件**：`application.properties`
   ```properties
   spring.profiles.active=dev
   ```

2. **命令行参数**：
   ```bash
   java -jar app.jar --spring.profiles.active=dev,ssl
   ```

3. **环境变量**：
   ```bash
   export SPRING_PROFILES_ACTIVE=dev
   ```

4. **编程方式**：
   ```java
   SpringApplication application = new SpringApplication(MyApp.class);
   application.setAdditionalProfiles("dev");
   application.run(args);
   ```

### Profile 表达式

支持逻辑运算符：`!`（非）、`&`（与）、`|`（或）。

```java
@Bean
@Profile("dev & !test")  // dev 环境且不是 test
public DataSource specialDataSource() {
    // ...
}
```

## @Import 与 @ImportResource

### @Import

`@Import` 注解用于导入其他配置类，类似于 XML 中的 `<import>` 元素。

```java
@Configuration
@Import({SecurityConfig.class, DatabaseConfig.class, WebConfig.class})
public class MainConfig {
    // 导入的配置类中的 Bean 会被自动注册
}
```

`@Import` 还支持导入 `ImportSelector` 和 `ImportBeanDefinitionRegistrar` 实现，实现更灵活的条件导入。

```java
public class MyImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        // 根据条件返回需要导入的配置类全限定名
        if (isProduction()) {
            return new String[]{"com.example.ProductionConfig"};
        }
        return new String[]{"com.example.DevelopmentConfig"};
    }
}

@Configuration
@Import(MyImportSelector.class)
public class AppConfig {
}
```

### @ImportResource

`@ImportResource` 用于导入 XML 配置文件，实现 Java 配置与 XML 配置的混合使用。

```java
@Configuration
@ImportResource("classpath:spring/applicationContext.xml")
public class JavaConfig {
    // XML 中定义的 Bean 也会被加载
}
```

## 属性绑定：@ConfigurationProperties

`@ConfigurationProperties` 注解用于将配置文件中的属性绑定到 Java 对象，是 Spring Boot 类型安全配置的核心。

### 基本用法

```java
@ConfigurationProperties(prefix = "app.datasource")
public class DataSourceProperties {
    private String url;
    private String username;
    private String password;
    private int maxPoolSize = 10;
    
    // getters and setters
}
```

对应 `application.yml`：

```yaml
app:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
    max-pool-size: 20
```

### 启用配置绑定

使用 `@EnableConfigurationProperties` 启用配置类：

```java
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(properties.getUrl());
        ds.setUsername(properties.getUsername());
        ds.setPassword(properties.getPassword());
        ds.setMaximumPoolSize(properties.getMaxPoolSize());
        return ds;
    }
}
```

或者使用 `@ConfigurationPropertiesScan` 自动扫描：

```java
@SpringBootApplication
@ConfigurationPropertiesScan("com.example.properties")
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

### 松散绑定

Spring Boot 支持松散绑定（Relaxed Binding），配置属性名可以使用多种格式：

| 配置文件格式 | Java 属性名 |
|-------------|-------------|
| `app.datasource.max-pool-size` | `maxPoolSize` |
| `app.datasource.maxPoolSize` | `maxPoolSize` |
| `app.datasource.max_pool_size` | `maxPoolSize` |
| `APP_DATASOURCE_MAXPOOLSIZE` | `maxPoolSize` |

### 数据校验

结合 JSR 380 Bean Validation 进行配置校验：

```java
@ConfigurationProperties(prefix = "app.server")
@Validated
public class ServerProperties {

    @NotNull
    private String host;

    @Min(1024)
    @Max(65535)
    private int port = 8080;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeout = Duration.ofSeconds(30);
}
```

## Spring Boot 自动配置的核心原理

Spring Boot 的自动配置是其最强大的特性之一，它能根据类路径、配置属性等条件自动配置 Bean。

### 自动配置的触发机制

Spring Boot 通过 `@SpringBootApplication` 注解触发自动配置，该注解包含 `@EnableAutoConfiguration`。

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = { ... })
public @interface SpringBootApplication {
}
```

### spring.factories 机制（Spring Boot 2.x）

在 Spring Boot 2.x 中，自动配置类通过 `META-INF/spring.factories` 文件注册：

```properties
# META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

### AutoConfiguration.imports（Spring Boot 3.x）

Spring Boot 3.x 改用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件：

```
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

### 自动配置类的典型结构

```java
@AutoConfiguration
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
@ConditionalOnMissingBean(type = "DataSource")
@EnableConfigurationProperties(DataSourceProperties.class)
@Import({DataSourcePoolMetadataProvidersConfiguration.class,
         DataSourceCheckpointRestoreConfiguration.class})
public class DataSourceAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @Conditional(EmbeddedDatabaseCondition.class)
    @ConditionalOnMissingBean({DataSource.class, XADataSource.class})
    @Import(EmbeddedDataSourceConfiguration.class)
    protected static class EmbeddedDatabaseConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @Conditional(PooledDataSourceCondition.class)
    @ConditionalOnMissingBean({DataSource.class, XADataSource.class})
    @Import(PooledDataSourceConfiguration.class)
    protected static class PooledDataSourceConfiguration {
    }
}
```

### 自动配置的执行流程

1. **收集阶段**：扫描所有 `spring.factories` 或 `AutoConfiguration.imports` 中的配置类
2. **过滤阶段**：通过 `@Conditional` 系列注解过滤不满足条件的配置类
3. **排序阶段**：使用 `@AutoConfigureBefore`、`@AutoConfigureAfter` 和 `@AutoConfigureOrder` 排序
4. **注册阶段**：将满足条件的配置类注册到 IoC 容器
5. **覆盖阶段**：用户自定义的 Bean 优先于自动配置的 Bean（`@ConditionalOnMissingBean`）

### 查看自动配置报告

通过以下方式查看自动配置报告：

```properties
# application.properties
logging.level.org.springframework.boot.autoconfigure=DEBUG
```

或使用 `--debug` 参数启动：

```bash
java -jar app.jar --debug
```

启动后访问 `/actuator/conditions`（需引入 Actuator 依赖）可查看详细的条件评估报告。

### 自定义自动配置

创建自定义自动配置的步骤：

1. **创建配置类**：
   ```java
   @AutoConfiguration
   @ConditionalOnClass(MyService.class)
   @EnableConfigurationProperties(MyServiceProperties.class)
   public class MyServiceAutoConfiguration {

       @Bean
       @ConditionalOnMissingBean
       public MyService myService(MyServiceProperties properties) {
           return new MyService(properties.getEndpoint(), properties.getTimeout());
       }
   }
   ```

2. **注册配置类**：
   ```properties
   # META-INF/spring.factories
   org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
   com.example.autoconfigure.MyServiceAutoConfiguration
   ```

3. **发布到 Maven 仓库**，供其他项目使用。

通过深入理解 `@Configuration`、条件装配和自动配置原理，开发者可以更好地掌控 Spring 应用的初始化过程，编写更灵活、更易于维护的配置代码。