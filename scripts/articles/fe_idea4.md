# IDEA 高级调试与性能分析

## 条件断点

条件断点允许程序只在满足特定条件时暂停执行，这在调试循环或处理大量数据时非常有用。

### 设置条件断点

```java
// 假设我们要调试这个循环
for (int i = 0; i < 10000; i++) {
    processItem(items.get(i));  // 在这行设置断点
}
```

设置步骤：
1. 在代码行号左侧点击设置普通断点
2. 右键点击断点
3. 在 Condition 输入框中输入条件表达式
4. 点击 Done

```
# 条件表达式示例
i == 9999                    # 当 i 等于 9999 时暂停
items.get(i).getName().equals("test")  # 当 item 名称为 "test" 时暂停
amount > 1000 && status == Status.PENDING  # 复合条件
```

### 条件断点的高级用法

```java
// 使用方法调用作为条件
public class OrderService {
    public void processOrders(List<Order> orders) {
        for (Order order : orders) {
            // 条件: isHighValueOrder(order)
            validateOrder(order);
        }
    }
    
    private boolean isHighValueOrder(Order order) {
        return order.getAmount().compareTo(new BigDecimal("10000")) > 0;
    }
}
```

## 表达式求值

在调试过程中，可以实时计算表达式的值，甚至修改变量的值。

### Evaluate Expression 快捷键

- Windows/Linux: `Alt + F8`
- macOS: `Option + F8`

### 表达式求值示例

```java
// 在断点处可以计算任意表达式
public class UserService {
    public User findUser(String username) {
        User user = userRepository.findByUsername(username);
        // 断点在这里
        
        // 可以计算的表达式：
        // user != null
        // user.getRoles().size()
        // user.getEmail().contains("@")
        // userRepository.count()
        // "Admin".equals(user.getRole())
        
        return user;
    }
}
```

### 修改变量值

```java
public void processOrder(Order order) {
    BigDecimal discount = calculateDiscount(order);
    // 断点在这里
    
    // 可以在 Evaluate 窗口中修改变量值：
    // discount = new BigDecimal("0.1")
    
    BigDecimal finalPrice = order.getPrice().multiply(
        BigDecimal.ONE.subtract(discount)
    );
}
```

## 异常断点

异常断点可以在程序抛出指定异常时自动暂停，无需在代码中手动设置断点。

### 设置异常断点

1. 打开断点对话框（`Ctrl + Shift + F8`）
2. 点击 `+` 号，选择 `Java Exception Breakpoints`
3. 输入异常类名
4. 配置选项：
   - Caught exception: 捕获的异常
   - Uncaught exception: 未捕获的异常
   - Both: 两者都暂停

### 常用异常断点

```
# 常用异常类型
java.lang.NullPointerException
java.lang.ArrayIndexOutOfBoundsException
java.lang.ClassCastException
java.io.FileNotFoundException
java.sql.SQLException
org.springframework.beans.factory.BeanCreationException
```

### 异常断点过滤

```java
// 可以设置过滤条件，只在特定类中暂停
// 在断点属性中设置 Class filters:
com.example.service.*      # 只在 service 包中的异常暂停
com.example.MyClass        # 只在特定类中暂停
```

## 多线程调试

### 线程视图

在调试面板中可以看到所有线程的状态：
- Running: 运行中
- Sleeping: 休眠中
- Waiting: 等待中
- Blocked: 阻塞中

### 线程切换

```java
// 多线程代码示例
public class ThreadDemo {
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    
    public void processTasks(List<Task> tasks) {
        for (Task task : tasks) {
            executor.submit(() -> {
                // 在这里设置断点
                processTask(task);
            });
        }
    }
    
    private synchronized void processTask(Task task) {
        // 断点可以设置在这里
        // 可以切换到不同线程查看各自的执行状态
    }
}
```

### 线程调试技巧

1. **查看所有线程**: 在调试面板的线程下拉框中切换
2. **线程过滤**: 右键线程可以设置过滤
3. **挂起策略**: 设置是挂起所有线程还是只挂当前线程
4. **线程断点**: 可以在特定线程中暂停

### 并发调试配置

```
# 设置断点挂起策略
1. 右键点击断点
2. 选择 Suspend: All / Thread
3. All: 暂停所有线程
4. Thread: 只暂停当前线程（推荐用于并发调试）
```

## 远程调试配置

### JVM 远程调试参数

```bash
# Java 9+ 远程调试参数
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar

# Java 8 远程调试参数
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar app.jar

# 参数说明：
# transport=dt_socket: 使用 Socket 传输
# server=y: 作为调试服务端
# suspend=n: 不等待调试器连接
# address=*:5005: 监听所有网络接口的 5005 端口
```

### Docker 容器远程调试

```yaml
# docker-compose.yml
services:
  api:
    build: ./api
    ports:
      - "8080:8080"
      - "5005:5005"  # 调试端口
    environment:
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jdk-alpine

# 暴露调试端口
EXPOSE 5005

# 设置调试参数
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

COPY target/app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### IDEA 远程调试配置

1. 打开 Run/Debug Configurations
2. 点击 `+` 号，选择 `Remote JVM Debug`
3. 配置参数：
   - Host: 远程服务器地址
   - Port: 5005
   - Use module classpath: 选择对应模块
4. 点击 Debug 开始调试

### 远程调试注意事项

```
# 安全建议
1. 不要在生产环境暴露调试端口
2. 使用 SSH 隧道进行安全调试
3. 调试完成后及时关闭调试端口

# SSH 隧道命令
ssh -L 5005:localhost:5005 user@remote-server
# 然后在 IDEA 中连接 localhost:5005
```

## 内存视图

### 查看对象内存

1. 在调试时，右键点击变量
2. 选择 `Evaluate Expression`
3. 使用 `System.identityHashCode(obj)` 查看对象哈希码

### 内存分析

```java
// 可以查看对象的引用关系
public class MemoryDemo {
    private List<byte[]> data = new ArrayList<>();
    
    public void allocateMemory() {
        for (int i = 0; i < 100; i++) {
            data.add(new byte[1024 * 1024]); // 每次分配 1MB
        }
        // 断点在这里，可以查看 data 列表的大小
    }
}
```

### 查看集合内容

```java
// 调试时可以查看集合的详细信息
Map<String, List<User>> userGroups = new HashMap<>();
// 在 Variables 窗口中：
// - 展开 userGroups 可以看到所有键值对
// - 展开每个 List 可以看到用户详情
// - 右键选择 View as 可以改变显示方式
```

## 性能分析（Async Profiler）

### 安装 Async Profiler

```bash
# 下载 Async Profiler
wget https://github.com/async-profiler/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz

# 解压
tar -xzf async-profiler-2.9-linux-x64.tar.gz

# 添加到系统路径
export PATH=$PATH:async-profiler-2.9-linux-x64/bin
```

### IDEA 集成 Async Profiler

1. 安插件：Settings → Plugins → 搜索 "Async Profiler"
2. 配置 Async Profiler 路径
3. 在代码中右键选择 "Profile with Async Profiler"

### 命令行使用

```bash
# CPU 分析
./profiler.sh -d 30 -f cpu.html <pid>

# 内存分配分析
./profiler.sh -d 30 -e alloc -f alloc.html <pid>

# 锁竞争分析
./profiler.sh -d 30 -e lock -f lock.html <pid>

# 查看支持的事件
./profiler.sh list <pid>
```

### 分析结果解读

```
# 火焰图解读
1. X 轴：采样比例（越宽表示占用越多 CPU 时间）
2. Y 轴：调用栈深度（越深表示调用层级越多）
3. 颜色：随机分配，用于区分不同函数

# 常见性能问题
1. 某个函数特别宽：CPU 密集型热点
2. 调用栈特别深：可能存在递归或过深调用
3. GC 相关函数占比高：内存分配过多
```

## 数据库工具集成

### 数据库连接配置

1. 打开 Database 工具窗口（View → Tool Windows → Database）
2. 点击 `+` 号，选择 Data Source
3. 选择数据库类型（MySQL、PostgreSQL 等）
4. 填写连接信息：
   - Host: localhost
   - Port: 3306
   - User: root
   - Password: ****
   - Database: mydb

### 数据库操作

```sql
-- 在 IDEA 中可以直接执行 SQL
SELECT * FROM users WHERE status = 'ACTIVE';

-- 可以查看执行计划
EXPLAIN SELECT * FROM orders WHERE user_id = 123;

-- 可以查看表结构
DESCRIBE users;
```

### 数据库调试集成

```java
// 在代码中设置断点，可以同时查看数据库内容
public class UserRepository {
    public User findById(Long id) {
        // 断点在这里
        // 可以在 Database 工具窗口中执行：
        // SELECT * FROM users WHERE id = 123
        // 来验证数据是否正确
        return entityManager.find(User.class, id);
    }
}
```

## HTTP Client

### 创建 HTTP 请求文件

```http
### 创建 .http 或 .rest 文件

### 获取用户列表
GET http://localhost:8080/api/users
Authorization: Bearer {{token}}
Accept: application/json

### 创建用户
POST http://localhost:8080/api/users
Content-Type: application/json
Authorization: Bearer {{token}}

{
  "name": "John Doe",
  "email": "john@example.com",
  "role": "USER"
}

### 更新用户
PUT http://localhost:8080/api/users/123
Content-Type: application/json
Authorization: Bearer {{token}}

{
  "name": "John Updated",
  "email": "john.updated@example.com"
}

### 删除用户
DELETE http://localhost:8080/api/users/123
Authorization: Bearer {{token}}
```

### 环境变量配置

```http
### http-client.env.json 文件
{
  "development": {
    "host": "localhost:8080",
    "token": "dev-token-123"
  },
  "production": {
    "host": "api.example.com",
    "token": "prod-token-456"
  }
}

### 使用环境变量
GET http://{{host}}/api/users
Authorization: Bearer {{token}}
```

### 测试脚本

```http
### 带测试脚本的请求
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "name": "Test User",
  "email": "test@example.com"
}

> {%
client.test("创建用户成功", function() {
    client.assert(response.status === 201, "状态码应为 201");
    client.assert(response.body.name === "Test User", "用户名不匹配");
    client.assert(response.body.id !== null, "ID 不应为空");
    
    // 保存用户 ID 供后续请求使用
    client.global.set("userId", response.body.id);
});
%}
```

### GraphQL 请求

```http
### GraphQL 查询
POST http://localhost:8080/graphql
Content-Type: application/json

{
  "query": "query GetUser($id: ID!) { user(id: $id) { id name email posts { title } } }",
  "variables": {
    "id": "123"
  }
}
```

## 调试快捷键汇总

| 功能 | Windows/Linux | macOS |
|-----|---------------|-------|
| 调试运行 | Shift + F9 | Control + D |
| Step Over | F8 | F8 |
| Step Into | F7 | F7 |
| Step Out | Shift + F8 | Shift + F8 |
| 运行到光标处 | Alt + F9 | Option + F9 |
| Evaluate Expression | Alt + F8 | Option + F8 |
| 断点对话框 | Ctrl + Shift + F8 | Command + Shift + F8 |
| 恢复程序 | F9 | F9 |
| 停止 | Ctrl + F2 | Command + F2 |
| 查看所有断点 | Ctrl + Shift + F8 | Command + Shift + F8 |

## 调试模板配置

### 日志断点

```java
// 不暂停程序，只输出日志
// 设置断点后，右键取消 "Suspend" 选项
// 勾选 "Evaluate and log"，输入表达式
// 示例表达式：
"Processing user: " + user.getName() + ", ID: " + user.getId()
String.format("Order %s: amount=%.2f, status=%s", order.getId(), order.getAmount(), order.getStatus())
```

### 异常断点过滤

```
# 在断点对话框中设置
1. 点击 Java Exception Breakpoints
2. 添加 NullPointerException
3. 在 Class filters 中添加：
   com.example.service.*
   com.example.controller.*
4. 在 Instance filters 中可以过滤特定实例
```

## 高级调试技巧

### 强制返回

```java
// 在调试时可以强制方法返回特定值
public User findUser(String username) {
    User user = userRepository.findByUsername(username);
    // 断点在这里
    // 右键选择 "Force Return"
    // 输入返回值：new User("admin", "admin@example.com")
    return user;
}
```

### 强制抛出异常

```java
public void processOrder(Order order) {
    validateOrder(order);
    // 断点在这里
    // 右键选择 "Throw Exception"
    // 输入：new RuntimeException("模拟异常")
    saveOrder(order);
}
```

### 标记对象

```java
// 在调试时可以标记对象，方便在多个断点之间跟踪
List<Order> orders = getOrders();
// 断点在这里
// 右键点击 orders 变量
// 选择 "Mark Object"，输入标签
// 在后续断点中，被标记的对象会显示标签
```

### 调试多模块项目

```
# 配置源代码路径映射
1. 打开断点对话框
2. 选择断点
3. 在 "Search by" 中配置源代码路径
4. 添加依赖库的源代码路径
```
