# Java 并发编程（一）：线程基础与生命周期

并发编程是 Java 的核心能力之一。从操作系统底层的进程与线程模型，到 Java 提供的丰富线程 API，理解线程的基本概念和生命周期是掌握并发编程的第一步。

## 进程与线程

在深入 Java 线程之前，先厘清两个最基本的概念：

| 对比维度 | 进程（Process） | 线程（Thread） |
|---------|---------------|---------------|
| 定义 | 程序的一次执行实例 | 进程内的一个执行单元 |
| 资源分配 | 操作系统分配资源的基本单位（内存、文件句柄等） | 共享所属进程的资源，自身仅拥有栈、程序计数器 |
| 开销 | 创建/切换开销大（需切换地址空间） | 创建/切换开销小（共享地址空间） |
| 通信 | 需要 IPC（管道、Socket 等） | 可直接读写共享变量 |
| 典型比喻 | 工厂 | 工厂里的工人 |

一个进程可以包含多个线程，这些线程共享进程的堆内存和方法区，但每个线程有自己的虚拟机栈、本地方法栈和程序计数器。这就是为什么多线程可以高效共享数据，但也因此容易产生线程安全问题。

## Java 线程的创建方式

Java 提供了三种创建线程的方式，适用于不同场景。

### 方式一：继承 Thread 类

```java
public class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("当前线程: " + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        MyThread t = new MyThread();
        t.start(); // 启动线程，JVM 会调用 run()
    }
}
```

缺点：Java 单继承限制，继承 `Thread` 后无法再继承其他类。

### 方式二：实现 Runnable 接口（推荐）

```java
public class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("当前线程: " + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        Thread t = new Thread(new MyRunnable(), "worker-1");
        t.start();
    }
}
```

**推荐使用这种方式**，原因：
- 接口可以多实现，不受单继承限制
- 任务（`Runnable`）与线程（`Thread`）解耦，便于线程池复用
- 多个线程可以共享同一个 `Runnable` 实例

### 方式三：实现 Callable 接口 + FutureTask（有返回值）

`Runnable.run()` 没有返回值，如果需要获取执行结果，可以使用 `Callable`：

```java
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class MyCallable implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        int sum = 0;
        for (int i = 1; i <= 100; i++) {
            sum += i;
        }
        return sum;
    }

    public static void main(String[] args) throws Exception {
        FutureTask<Integer> futureTask = new FutureTask<>(new MyCallable());
        new Thread(futureTask).start();

        // get() 会阻塞当前线程，直到 call() 执行完毕
        Integer result = futureTask.get();
        System.out.println("计算结果: " + result); // 5050
    }
}
```

三种方式的对比：

| 特性 | Thread | Runnable | Callable + FutureTask |
|------|--------|----------|----------------------|
| 返回值 | 无 | 无 | 有（泛型指定） |
| 异常 | 不能抛受检异常 | 不能抛受检异常 | 可以抛受检异常 |
| 灵活性 | 低（单继承） | 高（接口） | 高（接口） |
| 适用场景 | 简单测试 | 通用场景 | 需要返回值或异步结果 |

## 线程的生命周期与状态

Java 线程在其生命周期中会经历 6 种状态，定义在 `Thread.State` 枚举中：

```
NEW ──start()──→ RUNNABLE ──→ BLOCKED（等待获取 synchronized 锁）
                   │
                   ├──→ WAITING（wait()、join()、LockSupport.park()）
                   │
                   ├──→ TIMED_WAITING（sleep(ms)、wait(ms)、join(ms)）
                   │
                   └──→ TERMINATED（run() 正常结束或抛出未捕获异常）
```

各状态说明：

| 状态 | 触发条件 | 说明 |
|------|---------|------|
| `NEW` | `new Thread()` 创建后、尚未 `start()` | 线程对象已存在，但操作系统尚未分配资源 |
| `RUNNABLE` | 调用 `start()` 后 | 包含就绪（Ready）和运行中（Running）两种子状态 |
| `BLOCKED` | 等待获取 `synchronized` 内置锁 | 锁被其他线程持有时进入此状态 |
| `WAITING` | `wait()`、`join()`、`LockSupport.park()` | 无限期等待，需要被显式唤醒 |
| `TIMED_WAITING` | `sleep(ms)`、`wait(ms)`、`join(ms)` | 有限期等待，超时后自动返回 |
| `TERMINATED` | `run()` 执行完毕或抛出未捕获异常 | 线程结束，不可再次 `start()` |

```java
Thread t = new Thread(() -> {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
});

System.out.println(t.getState()); // NEW
t.start();
System.out.println(t.getState()); // RUNNABLE（或 TIMED_WAITING，取决于调度）
```

## Thread 类常用方法

以下是 `Thread` 类中最常用的方法：

**启动与运行**

- `start()` — 启动线程，由 JVM 调用 `run()` 方法。**不能对同一线程对象重复调用**。
- `run()` — 线程执行的逻辑。直接调用 `run()` 不会创建新线程，只是普通方法调用。

**休眠与等待**

- `sleep(long millis)` — 当前执行线程休眠指定毫秒数，**不会释放锁**。休眠期间响应 `interrupt()`。
- `join()` / `join(long millis)` — 等待目标线程执行完毕。例如主线程等待子线程完成后再继续：

```java
Thread worker = new Thread(() -> {
    System.out.println("子线程工作中...");
});
worker.start();
worker.join(); // 主线程阻塞，直到 worker 结束
System.out.println("子线程已结束");
```

**中断**

- `interrupt()` — 设置目标线程的中断标志位。
- `isInterrupted()` — 检查中断标志（不清除）。
- `interrupted()` — 检查**当前线程**的中断标志并清除（静态方法）。

**获取与设置**

- `currentThread()` — 返回当前正在执行的线程对象（静态方法）。
- `getName()` / `setName(String)` — 获取/设置线程名称。
- `setDaemon(boolean)` — 设为守护线程（必须在 `start()` 前调用）。
- `setPriority(int)` — 设置优先级（1~10），默认为 5。

## 线程中断机制

Java 的中断是一种**协作式**机制——调用 `interrupt()` 只是设置中断标志，不会强制停止线程。线程需要自己检查并响应中断。

```java
// 方式一：检查中断标志
Thread t = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // 执行任务
    }
    System.out.println("线程被中断，退出循环");
});

// 方式二：捕获 InterruptedException
Thread t2 = new Thread(() -> {
    try {
        Thread.sleep(5000);
    } catch (InterruptedException e) {
        // sleep 被中断时会抛出此异常，同时清除中断标志
        System.out.println("休眠被中断");
    }
});
```

中断响应策略：

1. **立即退出** — 检测到中断后直接 `return`，终止任务。
2. **向上抛出** — 将 `InterruptedException` 传递给调用者，由上层决定如何处理。
3. **恢复中断** — 捕获异常后重新设置中断标志：`Thread.currentThread().interrupt()`，让上层感知。

## 守护线程（Daemon Thread）

守护线程是为其他线程提供服务的后台线程。当所有非守护线程（用户线程）结束时，JVM 会自动退出，**不会等待守护线程执行完毕**。

最典型的守护线程是垃圾回收（GC）线程。

```java
Thread gcThread = new Thread(() -> {
    while (true) {
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            break;
        }
    }
});
gcThread.setDaemon(true); // 必须在 start() 之前设置
gcThread.start();
```

注意事项：
- `setDaemon(true)` 必须在 `start()` 之前调用，否则抛出 `IllegalThreadStateException`。
- 守护线程中创建的线程默认也是守护线程。
- 不要在守护线程中执行需要保证完成的关键逻辑（如写文件、发邮件），因为它可能随时被终止。

## 线程优先级

Java 线程优先级范围为 1（`MIN_PRIORITY`）到 10（`MAX_PRIORITY`），默认为 5（`NORM_PRIORITY`）。

```java
Thread high = new Thread(() -> doImportantWork());
high.setPriority(Thread.MAX_PRIORITY); // 优先级 10

Thread low = new Thread(() -> doBackgroundWork());
low.setPriority(Thread.MIN_PRIORITY); // 优先级 1
```

优先级的作用：
- 优先级只是给操作系统的**调度建议**，**不保证**高优先级线程一定先执行。
- 不同操作系统对优先级的支持程度不同（Windows 有 7 级，Linux 可能忽略）。
- **不应依赖优先级来控制程序逻辑**，应使用同步机制。

## 线程安全问题演示

当多个线程同时访问和修改共享变量时，如果没有适当的同步措施，就会出现**数据不一致**问题。

```java
public class UnsafeCounter {
    private int count = 0;

    public void increment() {
        count++; // 非原子操作：读取 → 加1 → 写回
    }

    public int getCount() {
        return count;
    }

    public static void main(String[] args) throws InterruptedException {
        UnsafeCounter counter = new UnsafeCounter();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) counter.increment();
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) counter.increment();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // 期望 20000，实际结果不确定（可能是 15832、18976 等）
        System.out.println("count = " + counter.getCount());
    }
}
```

问题根源在于 `count++` 不是原子操作，它包含三个步骤：
1. 读取 `count` 的当前值
2. 将值加 1
3. 将新值写回 `count`

当两个线程在这三步之间交替执行时，就会出现丢失更新（Lost Update）的问题。解决这个问题需要用到同步机制，将在下一篇文章中详细讨论。
