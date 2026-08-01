# CompletableFuture 异步编程

在现代应用中，异步编程是提升系统吞吐量的关键手段。Java 5 引入的 `Future` 接口虽然提供了基本的异步能力，但存在诸多局限。Java 8 引入的 `CompletableFuture` 大幅增强了异步编程的表达力，支持链式调用、组合、异常处理等高级特性。

## Future 接口的局限

```java
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
```

`Future` 的主要问题：

| 问题 | 说明 |
|------|------|
| `get()` 阻塞 | 调用 `get()` 会阻塞当前线程，违背异步初衷 |
| 不能链式组合 | 无法表达"完成后执行下一步"的逻辑 |
| 不能手动完成 | 无法从外部设置结果 |
| 异常处理不便 | 只能通过 `try-catch` 包裹 `get()` |
| 不能合并多个 Future | 无法表达"等待所有完成"或"任意一个完成" |

```java
// Future 的典型问题：阻塞等待
Future<String> future = executor.submit(() -> fetchData());
String result = future.get();  // 阻塞！线程在这里等待
process(result);
```

## CompletableFuture 基础

`CompletableFuture` 同时实现了 `Future` 和 `CompletionStage` 接口，后者提供了丰富的链式操作。

### 创建 CompletableFuture

```java
// 1. supplyAsync：有返回值的异步任务
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    return fetchDataFromDB();
});

// 2. runAsync：无返回值的异步任务
CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
    sendNotification();
});

// 3. 指定自定义线程池（推荐）
ExecutorService executor = Executors.newFixedThreadPool(10);
CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
    return fetchDataFromDB();
}, executor);

// 4. completedFuture：创建已完成的 Future（常用于测试或默认值）
CompletableFuture<String> completed = CompletableFuture.completedFuture("result");
```

**重要建议**：生产环境应使用自定义线程池，而不是默认的 `ForkJoinPool.commonPool()`，避免任务相互影响。

## 链式转换

`CompletableFuture` 最强大的特性是链式调用，将多个异步操作串联起来。

### thenApply：转换结果

`thenApply` 接收一个 `Function`，将前一步的结果转换为新的值：

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")           // 同步转换
    .thenApply(String::toUpperCase);        // 再次转换

System.out.println(future.join());  // HELLO WORLD
```

### thenApplyAsync：异步转换

`thenApplyAsync` 在默认线程池（或指定线程池）中执行转换操作：

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenApplyAsync(s -> {
        // 在另一个线程中执行
        return s + " World";
    });
```

### thenAccept：消费结果

`thenAccept` 接收一个 `Consumer`，消费结果但不返回新值：

```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenAccept(s -> System.out.println("Received: " + s));
```

### thenRun：执行后续操作

`thenRun` 不依赖前一步的结果，只执行一个 `Runnable`：

```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> fetchData())
    .thenRun(() -> System.out.println("Task completed"));
```

### 操作对比

| 方法 | 参数 | 是否使用结果 | 是否有返回值 |
|------|------|-------------|-------------|
| `thenApply` | `Function<T, U>` | 是 | `CompletableFuture<U>` |
| `thenAccept` | `Consumer<T>` | 是 | `CompletableFuture<Void>` |
| `thenRun` | `Runnable` | 否 | `CompletableFuture<Void>` |

每个方法都有对应的 `*Async` 版本，区别在于执行线程：

- `thenXxx()`：在前一个任务的线程中执行（或主线程，如果前一个已完成）
- `thenXxxAsync()`：在默认线程池中执行
- `thenXxxAsync(executor)`：在指定线程池中执行

## 组合多个 Future

### thenCompose：扁平化嵌套 Future

当一个异步操作的结果需要作为另一个异步操作的输入时，`thenCompose` 可以避免嵌套的 `CompletableFuture<CompletableFuture<T>>`：

```java
// 错误示范：产生嵌套
CompletableFuture<CompletableFuture<String>> nested = future
    .thenApply(s -> CompletableFuture.supplyAsync(() -> s + "!"));

// 正确做法：使用 thenCompose（类似 flatMap）
CompletableFuture<String> flat = future
    .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + "!"));
```

```
thenApply:     Future<A> → (A → B) → Future<B>
thenCompose:   Future<A> → (A → Future<B>) → Future<B>
```

### thenCombine：合并两个独立 Future

当两个 Future 相互独立，需要合并它们的结果时：

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "World");

CompletableFuture<String> combined = future1.thenCombine(future2,
    (s1, s2) -> s1 + " " + s2);

System.out.println(combined.join());  // Hello World
```

类似的方法还有：
- `thenAcceptBoth`：消费两个 Future 的结果，无返回值
- `runAfterBoth`：两个都完成后执行 Runnable

### allOf：等待所有完成

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> fetchFromAPI1());
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> fetchFromAPI2());
CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> fetchFromAPI3());

// 等待所有完成
CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3);
all.join();  // 阻塞直到所有完成

// 获取所有结果
List<String> results = Stream.of(f1, f2, f3)
    .map(CompletableFuture::join)
    .collect(Collectors.toList());
```

### anyOf：任意一个完成

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> slowAPI());
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> fastAPI());
CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> mediumAPI());

// 任意一个完成就返回
CompletableFuture<Object> any = CompletableFuture.anyOf(f1, f2, f3);
Object result = any.join();  // 返回最先完成的结果
```

## 异常处理

### exceptionally：异常时的 fallback

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        if (Math.random() > 0.5) {
            throw new RuntimeException("Something went wrong");
        }
        return "Success";
    })
    .exceptionally(ex -> {
        System.out.println("Error: " + ex.getMessage());
        return "Default Value";  // 返回默认值
    });
```

### handle：统一处理成功和失败

`handle` 无论成功还是失败都会执行，类似于 `try-catch-finally`：

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> fetchData())
    .handle((result, ex) -> {
        if (ex != null) {
            System.out.println("Failed: " + ex.getMessage());
            return "Fallback";
        } else {
            return result.toUpperCase();
        }
    });
```

### whenComplete：完成时的回调

`whenComplete` 与 `handle` 类似，但不改变结果（只做观察）：

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> fetchData())
    .whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("Task failed", ex);
        } else {
            log.info("Task succeeded: {}", result);
        }
    });
    // 结果不受 whenComplete 的影响
```

### 异常处理对比

| 方法 | 用途 | 是否改变结果 | 参数 |
|------|------|-------------|------|
| `exceptionally` | 异常 fallback | 是 | `Function<Throwable, T>` |
| `handle` | 统一处理 | 是 | `BiFunction<T, Throwable, U>` |
| `whenComplete` | 观察/记录 | 否 | `BiConsumer<T, Throwable>` |

## 超时控制（Java 9+）

Java 9 为 `CompletableFuture` 添加了超时控制方法：

```java
// orTimeout：超时后抛出 TimeoutException
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> slowOperation())
    .orTimeout(5, TimeUnit.SECONDS);

// completeOnTimeout：超时后返回默认值
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> slowOperation())
    .completeOnTimeout("Default Value", 5, TimeUnit.SECONDS);
```

Java 8 中的替代方案：

```java
// 使用 ExecutorService 的 schedule 方法
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> slowOperation());

scheduler.schedule(() -> {
    future.complete("Timeout Default");
}, 5, TimeUnit.SECONDS);
```

## 实战：多个 API 并行调用

一个典型的微服务场景：并行调用多个 API，聚合结果，超时降级。

```java
public class ApiAggregator {
    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    public UserDetail getUserDetail(String userId) {
        // 并行调用三个 API
        CompletableFuture<UserProfile> profileFuture = CompletableFuture
            .supplyAsync(() -> fetchProfile(userId), executor)
            .orTimeout(3, TimeUnit.SECONDS)
            .exceptionally(ex -> getDefaultProfile());

        CompletableFuture<List<Order>> ordersFuture = CompletableFuture
            .supplyAsync(() -> fetchOrders(userId), executor)
            .orTimeout(5, TimeUnit.SECONDS)
            .exceptionally(ex -> Collections.emptyList());

        CompletableFuture<Integer> creditFuture = CompletableFuture
            .supplyAsync(() -> fetchCredit(userId), executor)
            .orTimeout(2, TimeUnit.SECONDS)
            .exceptionally(ex -> 0);

        // 等待所有完成并聚合结果
        return CompletableFuture.allOf(profileFuture, ordersFuture, creditFuture)
            .thenApply(v -> new UserDetail(
                profileFuture.join(),
                ordersFuture.join(),
                creditFuture.join()
            ))
            .join();
    }

    private UserProfile fetchProfile(String userId) {
        // 调用用户服务 API
        return userService.getProfile(userId);
    }

    private List<Order> fetchOrders(String userId) {
        // 调用订单服务 API
        return orderService.getOrders(userId);
    }

    private Integer fetchCredit(String userId) {
        // 调用信用服务 API
        return creditService.getCredit(userId);
    }

    private UserProfile getDefaultProfile() {
        return new UserProfile("Unknown", "default.png");
    }
}
```

```
执行流程：
┌────────────────────────────────────────────────────────────────┐
│                         主线程                                   │
│  supplyAsync ───────────────────────────────────────────────────│
│       │                                                        │
│       ├── fetchProfile()    ── 3s 超时 ──┐                     │
│       ├── fetchOrders()     ── 5s 超时 ──┼── allOf ── 聚合结果  │
│       └── fetchCredit()     ── 2s 超时 ──┘                     │
│                                                                │
│  总耗时 ≈ max(各 API 耗时)，而不是 sum                          │
└────────────────────────────────────────────────────────────────┘
```

## 默认线程池与自定义线程池

### 默认线程池

`CompletableFuture` 的异步方法（不指定 Executor）默认使用 `ForkJoinPool.commonPool()`：

```java
// 这些方法使用 commonPool
CompletableFuture.supplyAsync(() -> task());
CompletableFuture.runAsync(() -> task());
future.thenApplyAsync(x -> transform(x));
```

`commonPool` 的特点：
- 线程数 = CPU 核心数 - 1
- 全局共享，所有未指定线程池的异步任务共用
- 不适合 I/O 密集型任务

### 自定义线程池建议

```java
// CPU 密集型任务
ExecutorService cpuExecutor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
);

// I/O 密集型任务
ExecutorService ioExecutor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() * 2
);

// 带名称的线程池（便于调试）
ExecutorService namedExecutor = new ThreadPoolExecutor(
    10, 20, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("async-pool-" + counter.incrementAndGet());
            return t;
        }
    }
);
```

### 注意事项

1. **始终指定线程池**：避免阻塞 `commonPool`，影响其他使用它的代码
2. **合理设置线程数**：CPU 密集型任务线程数 ≈ CPU 核心数；I/O 密集型可以更多
3. **优雅关闭线程池**：应用关闭时调用 `executor.shutdown()`
4. **避免在 CompletableFuture 中执行阻塞操作**：如果必须阻塞，使用独立的线程池

---

`CompletableFuture` 是 Java 异步编程的核心工具。它通过链式调用将多个异步操作优雅地串联起来，通过 `allOf`/`anyOf` 实现并行执行，通过 `exceptionally`/`handle` 提供完善的异常处理。在微服务架构中，合理使用 `CompletableFuture` 可以显著提升系统的响应速度和吞吐量。