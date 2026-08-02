# Java 并发编程（三）：线程池与高级并发工具

手动创建线程存在两个问题：每次请求都创建/销毁线程的开销不可忽视，且无限制地创建线程会耗尽系统资源。线程池通过线程复用和数量控制解决了这些问题，是 Java 并发编程中最重要的基础设施。

## 为什么需要线程池

| 场景 | 无线程池 | 使用线程池 |
|------|---------|-----------|
| 线程创建 | 每次任务都 `new Thread()`，涉及 OS 线程分配 | 复用已有线程，避免重复创建 |
| 线程销毁 | 任务完成后线程废弃 | 线程归还池中等待下一个任务 |
| 并发控制 | 1000 个请求 = 1000 个线程，可能 OOM | 固定大小，任务排队等待 |
| 资源管理 | 线程数不可控 | 统一管理、监控、调优 |

## Executor 框架

Java 的线程池基于 `Executor` 框架，采用三级接口设计：

```
Executor（最顶层，仅定义 execute(Runnable)）
    └── ExecutorService（扩展生命周期管理、任务提交）
            └── ScheduledExecutorService（扩展定时/周期任务）
```

```java
// Executor — 最简接口
Executor executor = task -> new Thread(task).start();

// ExecutorService — 生产环境使用
ExecutorService service = Executors.newFixedThreadPool(4);
service.submit(() -> System.out.println("任务执行中"));
service.shutdown();
```

## ThreadPoolExecutor 核心参数

`ThreadPoolExecutor` 是线程池的核心实现类，构造函数有 7 个参数：

```java
public ThreadPoolExecutor(
    int corePoolSize,       // 核心线程数
    int maximumPoolSize,    // 最大线程数
    long keepAliveTime,     // 非核心线程的空闲存活时间
    TimeUnit unit,          // keepAliveTime 的时间单位
    BlockingQueue<Runnable> workQueue,  // 任务等待队列
    ThreadFactory threadFactory,        // 线程创建工厂（可自定义线程名）
    RejectedExecutionHandler handler    // 拒绝策略
)
```

| 参数 | 说明 | 示例 |
|------|------|------|
| `corePoolSize` | 常驻线程数，即使空闲也不会回收（除非设置 `allowCoreThreadTimeOut`） | CPU 密集型：N+1；IO 密集型：2N |
| `maximumPoolSize` | 池允许的最大线程数 | 根据系统承载能力设定 |
| `keepAliveTime` | 非核心线程空闲超过此时间后被回收 | 60 秒 |
| `workQueue` | 核心线程都在忙时，新任务在此排队 | `ArrayBlockingQueue`、`LinkedBlockingQueue` |
| `threadFactory` | 自定义线程的创建方式 | 设置线程名前缀便于日志排查 |
| `handler` | 队列满且线程数达到最大值时的拒绝策略 | `AbortPolicy`（默认） |

## 线程池的执行流程

当提交一个新任务时，线程池按以下顺序处理：

```
提交任务
  │
  ├─ 当前线程数 < corePoolSize？
  │   └─ YES → 创建核心线程执行任务
  │
  ├─ 核心线程都在忙 → 尝试放入 workQueue
  │   └─ 队列未满 → 任务排队等待
  │
  ├─ 队列已满 → 当前线程数 < maximumPoolSize？
  │   └─ YES → 创建非核心线程执行任务
  │
  └─ 线程数已达 maximumPoolSize → 执行拒绝策略
```

核心要点：**先填满核心线程 → 再放入队列 → 再扩展到最大线程 → 最后拒绝**。

## 四种拒绝策略

当线程池无法接受新任务时，由拒绝策略决定如何处理：

| 策略 | 行为 | 适用场景 |
|------|------|---------|
| `AbortPolicy`（默认） | 抛出 `RejectedExecutionException` | 需要感知任务被拒绝 |
| `CallerRunsPolicy` | 由提交任务的线程（调用者）自己执行 | 不想丢失任务，可接受延迟 |
| `DiscardPolicy` | 静默丢弃任务 | 可容忍丢失的任务（如日志） |
| `DiscardOldestPolicy` | 丢弃队列中最老的任务，然后重试 | 优先处理最新任务 |

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2, 4, 60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10),
    new ThreadPoolExecutor.CallerRunsPolicy() // 任务不会丢失
);
```

## Executors 工厂方法

`Executors` 提供了快速创建线程池的静态方法：

```java
// 固定大小线程池，核心线程 = 最大线程，无超时
ExecutorService fixed = Executors.newFixedThreadPool(4);

// 缓存线程池，核心线程为 0，按需创建，空闲 60s 回收
ExecutorService cached = Executors.newCachedThreadPool();

// 单线程池，保证任务按提交顺序执行
ExecutorService single = Executors.newSingleThreadExecutor();

// 定时线程池，支持延迟和周期任务
ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);
scheduled.schedule(() -> System.out.println("延迟执行"), 3, TimeUnit.SECONDS);
scheduled.scheduleAtFixedRate(() -> System.out.println("周期执行"),
    0, 1, TimeUnit.SECONDS);
```

### 为什么阿里规约禁止使用 Executors 创建线程池

阿里巴巴 Java 开发手册明确规定：**线程池不允许使用 `Executors` 创建，而是通过 `ThreadPoolExecutor` 构造函数创建**。

原因是 `Executors` 部分方法存在隐患：

| 方法 | 队列类型 | 风险 |
|------|---------|------|
| `newFixedThreadPool` | `LinkedBlockingQueue`（无界） | 队列可无限增长，导致 OOM |
| `newSingleThreadExecutor` | `LinkedBlockingQueue`（无界） | 同上 |
| `newCachedThreadPool` | `SynchronousQueue` | 最大线程数为 `Integer.MAX_VALUE`，可能创建大量线程导致 OOM |

正确做法是手动创建 `ThreadPoolExecutor`，明确指定有界队列和最大线程数：

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10, 20, 60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(200), // 有界队列
    new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "biz-pool-" + counter.getAndIncrement());
        }
    },
    new ThreadPoolExecutor.AbortPolicy()
);
```

## Callable 与 Future

`Runnable.run()` 没有返回值，而 `Callable.call()` 可以返回结果并抛出异常。通过 `Future` 获取异步执行结果：

```java
ExecutorService executor = Executors.newFixedThreadPool(2);

Future<String> future = executor.submit(() -> {
    Thread.sleep(2000);
    return "任务完成";
});

// 做其他事情...

try {
    String result = future.get(5, TimeUnit.SECONDS); // 阻塞等待，最多 5 秒
    System.out.println(result);
} catch (TimeoutException e) {
    future.cancel(true); // 超时取消
} catch (ExecutionException e) {
    // 任务内部抛出的异常包装在 ExecutionException 中
    e.getCause().printStackTrace();
}
```

`Future` 的局限性：
- `get()` 只能阻塞等待，不能回调
- 多个 `Future` 无法组合（如 "A 完成后执行 B"）
- 异常处理不够优雅

## CompletableFuture（Java 8+）

`CompletableFuture` 解决了 `Future` 的所有痛点，支持链式调用、组合、异常处理，是现代 Java 异步编程的标准工具。

### 创建异步任务

```java
// 无返回值
CompletableFuture<Void> cf1 = CompletableFuture.runAsync(() -> {
    System.out.println("异步执行，无返回值");
});

// 有返回值
CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> {
    return "异步结果";
});

// 指定线程池（不指定则使用 ForkJoinPool.commonPool）
ExecutorService pool = Executors.newFixedThreadPool(4);
CompletableFuture<String> cf3 = CompletableFuture.supplyAsync(() -> {
    return "自定义线程池";
}, pool);
```

### 链式调用

```java
CompletableFuture.supplyAsync(() -> {
    return "Hello";
})
.thenApply(s -> s + " World")       // 转换结果，返回 CompletableFuture<String>
.thenAccept(System.out::println);    // 消费结果，无返回值

// thenRun — 不关心前一步结果，只执行动作
CompletableFuture.supplyAsync(() -> "result")
    .thenRun(() -> System.out.println("前一步已完成"));
```

方法对比：

| 方法 | 参数 | 返回值 | 用途 |
|------|------|--------|------|
| `thenApply` | `Function<T,U>` | `CompletableFuture<U>` | 转换结果 |
| `thenAccept` | `Consumer<T>` | `CompletableFuture<Void>` | 消费结果 |
| `thenRun` | `Runnable` | `CompletableFuture<Void>` | 执行动作，不关心结果 |

### 组合多个异步任务

```java
// thenCompose — 串行依赖（类似 flatMap）
CompletableFuture<String> result = getUserId()
    .thenCompose(id -> getUserInfo(id));  // 第二步依赖第一步的结果

// thenCombine — 并行执行，合并结果
CompletableFuture<String> combined = getPriceAsync()
    .thenCombine(getRateAsync(), (price, rate) -> price * rate);

// allOf — 等待所有任务完成
CompletableFuture<Void> all = CompletableFuture.allOf(
    queryFromDB(),
    queryFromRedis(),
    queryFromAPI()
);
all.join(); // 全部完成后继续

// anyOf — 任意一个完成即继续
CompletableFuture<Object> any = CompletableFuture.anyOf(
    fastSource(),
    slowSource()
);
```

### 异常处理

```java
CompletableFuture.supplyAsync(() -> {
    if (true) throw new RuntimeException("出错了");
    return "ok";
})
.exceptionally(ex -> {
    System.out.println("异常: " + ex.getMessage());
    return "默认值"; // 异常时的降级结果
});

// handle — 同时处理正常结果和异常
CompletableFuture.supplyAsync(() -> "result")
    .handle((result, ex) -> {
        if (ex != null) return "异常降级";
        return result;
    });
```

### 实际应用：多个 API 并行调用

```java
public CompletableFuture<UserProfile> getUserProfile(Long userId) {
    CompletableFuture<User> userFuture = CompletableFuture
        .supplyAsync(() -> userService.getUser(userId));

    CompletableFuture<List<Order>> ordersFuture = CompletableFuture
        .supplyAsync(() -> orderService.getOrders(userId));

    CompletableFuture<Integer> scoreFuture = CompletableFuture
        .supplyAsync(() -> scoreService.getScore(userId));

    return CompletableFuture.allOf(userFuture, ordersFuture, scoreFuture)
        .thenApply(v -> new UserProfile(
            userFuture.join(),
            ordersFuture.join(),
            scoreFuture.join()
        ));
}
```

三个查询并行执行，总耗时约等于最慢的那个请求，而非三者之和。

## Fork/Join 框架

Fork/Join 是 JDK 7 引入的并行计算框架，基于**分治（Divide and Conquer）** 思想：将大任务拆分为小任务（Fork），小任务结果合并（Join）。

### 核心类

- `ForkJoinPool` — 线程池实现，使用**工作窃取（Work Stealing）** 算法
- `RecursiveTask<V>` — 有返回值的递归任务
- `RecursiveAction` — 无返回值的递归任务

### 工作窃取算法

每个工作线程有自己的双端队列（Deque）。当自己的任务执行完毕时，从其他线程队列的**尾部**窃取任务，减少竞争。

```
线程A队列: [T1, T2, T3, T4]  ← A 从头部取任务
线程B队列: [T5, T6]          ← B 空闲时从 A 的尾部窃取 T4
```

### 示例：并行求和

```java
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class SumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 10000;
    private final long[] array;
    private final int start, end;

    public SumTask(long[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            // 足够小，直接计算
            long sum = 0;
            for (int i = start; i < end; i++) sum += array[i];
            return sum;
        }

        // 拆分
        int mid = (start + end) / 2;
        SumTask left = new SumTask(array, start, mid);
        SumTask right = new SumTask(array, mid, end);

        left.fork(); // 异步执行左半部分
        long rightResult = right.compute(); // 当前线程执行右半部分
        long leftResult = left.join(); // 等待左半部分完成

        return leftResult + rightResult;
    }

    public static void main(String[] args) {
        long[] array = new long[100_000];
        for (int i = 0; i < array.length; i++) array[i] = i + 1;

        ForkJoinPool pool = new ForkJoinPool();
        Long result = pool.invoke(new SumTask(array, 0, array.length));
        System.out.println("总和: " + result);
    }
}
```

`parallelStream()` 底层就是基于 `ForkJoinPool` 实现的：

```java
long sum = LongStream.rangeClosed(1, 100_000)
    .parallel()
    .sum();
```

Fork/Join 适合可以递归拆分的计算密集型任务（如排序、矩阵运算、大数据聚合），不适合 IO 密集型任务。
