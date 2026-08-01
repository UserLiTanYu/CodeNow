# Fork/Join 框架与并发工具类

Java 并发包（`java.util.concurrent`）提供了丰富的并发工具，从底层的锁和原子变量，到高层的线程池和并发集合。本章将介绍 Fork/Join 框架的分治并发思想，以及 `CountDownLatch`、`CyclicBarrier`、`Semaphore` 等实用的并发工具类。

## Fork/Join 框架

### 分治思想

分治（Divide and Conquer）是算法设计中的经典思想：将一个大问题分解为多个小问题，分别求解，最后合并结果。Fork/Join 框架将这种思想应用到并发编程中。

```
                        ┌─────────────┐
                        │  大任务      │
                        └──────┬──────┘
                               │ fork
                 ┌─────────────┼─────────────┐
                 │             │             │
           ┌─────┴─────┐ ┌────┴─────┐ ┌─────┴─────┐
           │  子任务 1  │ │ 子任务 2  │ │ 子任务 3  │
           └─────┬─────┘ └────┬─────┘ └─────┬─────┘
                 │             │             │
                 └─────────────┼─────────────┘
                               │ join
                        ┌──────┴──────┐
                        │  合并结果    │
                        └─────────────┘
```

### 核心类

#### ForkJoinPool

`ForkJoinPool` 是 Fork/Join 框架的线程池实现，采用**工作窃取**（Work Stealing）算法。

```java
// 创建 ForkJoinPool
ForkJoinPool pool = new ForkJoinPool();  // 使用默认线程数（CPU 核心数）
ForkJoinPool pool = new ForkJoinPool(4); // 指定并行度

// 提交任务
pool.invoke(task);           // 同步执行，等待结果
pool.execute(task);          // 异步执行
Future<T> future = pool.submit(task);  // 提交并获取 Future
```

#### RecursiveTask\<V\>：有返回值的递归任务

```java
public abstract class RecursiveTask<V> extends ForkJoinTask<V> {
    protected abstract V compute();  // 需要实现的核心方法
}
```

#### RecursiveAction：无返回值的递归任务

```java
public abstract class RecursiveAction extends ForkJoinTask<Void> {
    protected abstract void compute();
}
```

### 工作窃取算法

传统的线程池采用共享队列，所有线程从同一个队列中取任务。ForkJoinPool 采用工作窃取算法：每个线程有自己的双端队列（Deque），当自己的任务处理完毕后，会从其他忙碌线程的队列尾部"窃取"任务。

```
┌─────────────────────────────────────────────────────────────────┐
│                        ForkJoinPool                              │
│                                                                  │
│  ┌─────────────────┐      ┌─────────────────┐                  │
│  │   线程 1         │      │   线程 2         │                  │
│  │  ┌───────────┐  │      │  ┌───────────┐  │                  │
│  │  │ 队列       │  │      │  │ 队列       │  │                  │
│  │  │ [T1][T2]  │  │      │  │ [T3]       │  │                  │
│  │  └───────────┘  │      │  └───────────┘  │                  │
│  └────────┬────────┘      └────────┬────────┘                  │
│           │                        │                            │
│           │    ┌───────────────────┘                            │
│           │    │  窃取                                           │
│           │    ↓                                                │
│  ┌────────┴────────┐                                            │
│  │   线程 2 从线程 1 队列尾部窃取任务 T2                          │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

**优势**：
- 减少线程竞争：每个线程操作自己的队列
- 负载均衡：空闲线程自动帮助忙碌线程
- 高效的 fork 操作：子任务放入当前线程的队列头部

### 使用步骤

1. 继承 `RecursiveTask<V>` 或 `RecursiveAction`
2. 实现 `compute()` 方法
3. 在 `compute()` 中判断任务是否足够小（阈值）
4. 如果足够小，直接计算
5. 如果不够小，`fork()` 子任务，`join()` 合并结果

### 实战：大数组求和

```java
public class ArraySumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 10000;  // 分割阈值
    private final long[] array;
    private final int start;
    private final int end;

    public ArraySumTask(long[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        // 任务足够小，直接计算
        if (end - start <= THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        }

        // 任务较大，分割为两个子任务
        int mid = (start + end) / 2;
        ArraySumTask leftTask = new ArraySumTask(array, start, mid);
        ArraySumTask rightTask = new ArraySumTask(array, mid, end);

        // fork 左子任务（异步执行）
        leftTask.fork();

        // 右子任务在当前线程执行（避免不必要的 fork 开销）
        long rightResult = rightTask.compute();

        // join 左子任务结果
        long leftResult = leftTask.join();

        // 合并结果
        return leftResult + rightResult;
    }
}

// 使用
long[] array = new long[100_000_000];
Arrays.fill(array, 1);

ForkJoinPool pool = new ForkJoinPool();
ArraySumTask task = new ArraySumTask(array, 0, array.length);
long sum = pool.invoke(task);
System.out.println("Sum: " + sum);
```

### 实战：并行排序

```java
public class ParallelSortTask extends RecursiveAction {
    private static final int THRESHOLD = 1000;
    private final int[] array;
    private final int start;
    private final int end;

    public ParallelSortTask(int[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected void compute() {
        if (end - start <= THRESHOLD) {
            // 小数组直接排序
            Arrays.sort(array, start, end);
            return;
        }

        // 快速排序的分区操作
        int pivot = partition(array, start, end);

        // 并行排序左右两部分
        ParallelSortTask leftTask = new ParallelSortTask(array, start, pivot);
        ParallelSortTask rightTask = new ParallelSortTask(array, pivot + 1, end);

        invokeAll(leftTask, rightTask);  // 并行执行两个任务
    }

    private int partition(int[] arr, int lo, int hi) {
        int pivot = arr[hi - 1];
        int i = lo - 1;
        for (int j = lo; j < hi - 1; j++) {
            if (arr[j] <= pivot) {
                i++;
                swap(arr, i, j);
            }
        }
        swap(arr, i + 1, hi - 1);
        return i + 1;
    }

    private void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
```

## 并发工具类

### CountDownLatch：倒计数门闩

`CountDownLatch` 允许一个或多个线程等待其他线程完成操作。它维护一个计数器，每调用 `countDown()` 一次计数器减 1，当计数器为 0 时，所有等待的线程被释放。

```java
public class CountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
        int workerCount = 5;
        CountDownLatch latch = new CountDownLatch(workerCount);

        for (int i = 0; i < workerCount; i++) {
            final int workerId = i;
            new Thread(() -> {
                try {
                    System.out.println("Worker " + workerId + " starting...");
                    Thread.sleep((long) (Math.random() * 3000));
                    System.out.println("Worker " + workerId + " finished");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();  // 完成工作，计数器减 1
                }
            }).start();
        }

        System.out.println("Main thread waiting for all workers...");
        latch.await();  // 阻塞直到计数器为 0
        System.out.println("All workers finished, main thread continues");
    }
}
```

**特性**：
- 计数器只能使用一次，不能重置
- `await(timeout, unit)` 支持超时等待
- 适合"等待 N 个任务完成"的场景

### CyclicBarrier：循环栅栏

`CyclicBarrier` 让一组线程互相等待，直到所有线程都到达栅栏点，然后一起继续执行。与 `CountDownLatch` 不同，`CyclicBarrier` 可以重复使用。

```java
public class CyclicBarrierDemo {
    public static void main(String[] args) {
        int partyCount = 3;
        // 所有线程到达栅栏后，执行 barrierAction
        CyclicBarrier barrier = new CyclicBarrier(partyCount, () -> {
            System.out.println("所有线程已到达栅栏，继续执行");
        });

        for (int i = 0; i < partyCount; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    System.out.println("线程 " + id + " 正在执行第一阶段...");
                    Thread.sleep((long) (Math.random() * 3000));
                    System.out.println("线程 " + id + " 到达栅栏");
                    barrier.await();  // 等待其他线程

                    System.out.println("线程 " + id + " 正在执行第二阶段...");
                    Thread.sleep((long) (Math.random() * 2000));
                    barrier.await();  // 可以再次使用

                    System.out.println("线程 " + id + " 完成所有阶段");
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
```

### CountDownLatch vs CyclicBarrier

| 特性 | CountDownLatch | CyclicBarrier |
|------|---------------|---------------|
| 计数方向 | 递减到 0 | 递增到 N |
| 可重用 | 否（一次性） | 是（自动重置） |
| 等待对象 | 等待其他线程完成 | 所有线程互相等待 |
| 触发条件 | 计数器为 0 | 所有线程到达栅栏 |
| 额外动作 | 无 | 支持 barrierAction |
| 典型场景 | 主线程等待多个子任务 | 多线程分阶段并行计算 |

### Semaphore：信号量

`Semaphore` 用于控制同时访问某个资源的线程数量，常用于限流和资源池管理。

```java
public class SemaphoreDemo {
    // 同时最多 3 个线程访问
    private final Semaphore semaphore = new Semaphore(3);

    public void accessResource() {
        try {
            semaphore.acquire();  // 获取许可（阻塞）
            System.out.println(Thread.currentThread().getName() + " 获取许可");
            Thread.sleep(2000);  // 模拟使用资源
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release();  // 释放许可
            System.out.println(Thread.currentThread().getName() + " 释放许可");
        }
    }

    public void tryAccessResource() {
        if (semaphore.tryAcquire()) {  // 非阻塞尝试获取
            try {
                System.out.println("获取许可成功");
            } finally {
                semaphore.release();
            }
        } else {
            System.out.println("获取许可失败，当前无可用资源");
        }
    }
}
```

**常用场景**：
- 数据库连接池限流
- 接口限流
- 文件读取并发控制

### Exchanger：两个线程交换数据

`Exchanger` 提供一个同步点，两个线程在此交换数据。

```java
public class ExchangerDemo {
    public static void main(String[] args) {
        Exchanger<String> exchanger = new Exchanger<>();

        // 生产者线程
        new Thread(() -> {
            try {
                String data = "Hello from Producer";
                System.out.println("Producer 准备交换: " + data);
                String received = exchanger.exchange(data);  // 阻塞等待交换
                System.out.println("Producer 收到: " + received);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // 消费者线程
        new Thread(() -> {
            try {
                String data = "Hello from Consumer";
                System.out.println("Consumer 准备交换: " + data);
                String received = exchanger.exchange(data);  // 阻塞等待交换
                System.out.println("Consumer 收到: " + received);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```

## ExecutorService 最佳实践

### 正确关闭线程池

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

// 提交任务
for (int i = 0; i < 100; i++) {
    executor.submit(() -> doWork());
}

// 正确关闭方式：三步走
executor.shutdown();  // 1. 停止接受新任务，等待已提交任务完成

try {
    // 2. 等待一段时间让任务完成
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        // 3. 超时后强制关闭
        List<Runnable> droppedTasks = executor.shutdownNow();
        System.out.println("还有 " + droppedTasks.size() + " 个任务未完成");
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

| 方法 | 作用 |
|------|------|
| `shutdown()` | 停止接受新任务，已提交的任务继续执行 |
| `shutdownNow()` | 尝试停止所有正在执行的任务，返回未执行的任务列表 |
| `isShutdown()` | 是否已调用 shutdown |
| `isTerminated()` | 所有任务是否已完成 |
| `awaitTermination()` | 阻塞等待所有任务完成或超时 |

### 提交任务：submit vs execute

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

// execute：无返回值，异常直接抛出
executor.execute(() -> {
    doSomething();
});

// submit：返回 Future，异常封装在 Future 中
Future<String> future = executor.submit(() -> {
    return doSomethingAndReturn();
});

try {
    String result = future.get();  // 获取结果（可能抛出 ExecutionException）
} catch (ExecutionException e) {
    Throwable cause = e.getCause();  // 获取原始异常
}
```

### 批量提交：invokeAll / invokeAny

```java
List<Callable<String>> tasks = Arrays.asList(
    () -> fetchFromAPI1(),
    () -> fetchFromAPI2(),
    () -> fetchFromAPI3()
);

// invokeAll：等待所有任务完成，返回所有结果
List<Future<String>> futures = executor.invokeAll(tasks);
for (Future<String> f : futures) {
    System.out.println(f.get());
}

// 带超时的 invokeAll
List<Future<String>> futures = executor.invokeAll(tasks, 5, TimeUnit.SECONDS);

// invokeAny：返回第一个完成的结果，取消其他任务
String result = executor.invokeAny(tasks);
```

## 并发编程最佳实践总结

### 线程安全的单例

```java
// 方式 1：静态内部类（推荐）
public class Singleton {
    private Singleton() {}

    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;  // 类加载时初始化，线程安全
    }
}

// 方式 2：枚举（最简洁）
public enum Singleton {
    INSTANCE;

    public void doSomething() { }
}
```

### 不可变对象

不可变对象天然是线程安全的，因为它们的状态在创建后不能被修改：

```java
public final class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    // 返回新对象，而不是修改自身
    public Point translate(int dx, int dy) {
        return new Point(x + dx, y + dy);
    }
}
```

**不可变对象的要求**：
- 类声明为 `final`，不能被继承
- 所有字段声明为 `final`
- 不提供修改状态的方法
- 如果包含可变对象，防御性拷贝

### ThreadLocal 最佳实践

`ThreadLocal` 为每个线程提供独立的变量副本，避免线程间的数据竞争。

```java
public class UserContext {
    // 使用 ThreadLocal 存储当前线程的用户信息
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    public static void setUser(User user) {
        CURRENT_USER.set(user);
    }

    public static User getUser() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();  // 必须清理，防止内存泄漏
    }
}

// 使用示例：Web 请求处理
public class RequestFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
            User user = extractUser(request);
            UserContext.setUser(user);
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();  // 请求结束必须清理
        }
    }
}
```

**ThreadLocal 注意事项**：
- 使用后必须调用 `remove()`，防止线程池场景下的内存泄漏和数据污染
- 不要在线程池中存储生命周期长的对象
- `InheritableThreadLocal` 可以让子线程继承父线程的值，但在线程池中不可靠

### 并发工具选择指南

| 场景 | 推荐工具 |
|------|---------|
| 简单的互斥同步 | `synchronized` |
| 需要高级锁特性 | `ReentrantLock` |
| 读多写少 | `ReadWriteLock` / `StampedLock` |
| 简单计数器 | `AtomicLong` / `LongAdder` |
| 异步编程 | `CompletableFuture` |
| 分治计算 | `ForkJoinPool` |
| 等待多个任务完成 | `CountDownLatch` |
| 多线程分阶段执行 | `CyclicBarrier` |
| 资源限流 | `Semaphore` |
| 线程间数据交换 | `Exchanger` |
| 线程局部变量 | `ThreadLocal` |

---

Fork/Join 框架通过工作窃取算法实现了高效的分治并发，适合可分解的计算密集型任务。`CountDownLatch`、`CyclicBarrier`、`Semaphore` 等并发工具类则提供了灵活的线程协调机制。在实际开发中，应根据具体场景选择合适的并发工具，遵循最佳实践，编写安全、高效的并发代码。