# synchronized 与 volatile 详解

在 Java 并发编程中，`synchronized` 和 `volatile` 是两个最基础的关键字，它们构成了 Java 内存模型的基石。理解这两个关键字的原理和适用场景，是掌握并发编程的第一步。

## synchronized 的本质

### 对象监视器（Monitor）

`synchronized` 的底层实现依赖于对象监视器（Monitor）机制。每个 Java 对象都有一个与之关联的监视器锁（也称为内置锁或 Intrinsic Lock）。当一个线程进入 `synchronized` 代码块时，它必须先获取对象的监视器锁；当线程退出 `synchronized` 代码块时，会自动释放锁。

```
┌─────────────────────────────────────────────────────────┐
│                    Java 对象内存布局                       │
├─────────────────────────────────────────────────────────┤
│  对象头（Mark Word）                                       │
│  ├── 哈希码（HashCode）                                     │
│  ├── GC 分代年龄                                           │
│  ├── 锁标志位（01=无锁, 00=轻量级锁, 10=重量级锁）            │
│  └── 指向监视器的指针                                       │
├─────────────────────────────────────────────────────────┤
│  实例数据（Instance Data）                                  │
├─────────────────────────────────────────────────────────┤
│  对齐填充（Padding）                                        │
└─────────────────────────────────────────────────────────┘
```

在 HotSpot 虚拟机中，对象头的 Mark Word 存储了锁状态信息。当使用 `synchronized` 时，JVM 会在对象头中记录持有锁的线程 ID，实现锁的获取与释放。

### synchronized 方法

`synchronized` 可以修饰实例方法和静态方法，但两者的锁对象不同：

```java
public class SynchronizedDemo {

    // 实例方法：锁对象是当前实例 this
    public synchronized void instanceMethod() {
        // 临界区代码
    }

    // 静态方法：锁对象是 Class 对象（SynchronizedDemo.class）
    public static synchronized void staticMethod() {
        // 临界区代码
    }

    // 等价写法
    public void instanceMethodEquivalent() {
        synchronized (this) {
            // 临界区代码
        }
    }

    public static void staticMethodEquivalent() {
        synchronized (SynchronizedDemo.class) {
            // 临界区代码
        }
    }
}
```

| 方法类型 | 锁对象 | 影响范围 |
|---------|--------|---------|
| 实例方法 | `this`（当前实例） | 同一实例的所有 synchronized 实例方法 |
| 静态方法 | `Class` 对象 | 该类的所有 synchronized 静态方法 |

需要注意的是，`synchronized(this)` 和 `synchronized(ClassName.class)` 是两把不同的锁，它们之间互不影响。

### synchronized 块

相比修饰整个方法，`synchronized` 块可以提供更细粒度的锁控制，只同步真正需要保护的代码段，从而减少锁的持有时间，提高并发性能。

```java
public class FineGrainedLock {
    private final List<String> list = new ArrayList<>();
    private final Object lock = new Object();

    public void addAndProcess(String item) {
        // 非临界区代码（不需要同步）
        String processed = item.trim().toLowerCase();

        // 只同步必要的部分
        synchronized (lock) {
            list.add(processed);
        }

        // 非临界区代码
        System.out.println("Added: " + processed);
    }
}
```

**最佳实践**：锁的范围越小越好，但不能遗漏需要保护的代码。锁对象建议使用 `private final` 修饰，防止外部干扰。

## synchronized 的特性

### 可重入性

`synchronized` 是可重入锁（Reentrant Lock），同一线程可以多次获取同一把锁，而不会造成死锁。每获取一次锁，计数器加 1；释放锁时计数器减 1，计数器为 0 时才真正释放锁。

```java
public class ReentrantDemo {
    public synchronized void methodA() {
        System.out.println("methodA");
        methodB(); // 同一线程再次获取锁，不会死锁
    }

    public synchronized void methodB() {
        System.out.println("methodB");
    }
}
```

可重入性对于继承场景尤为重要——子类重写父类的 `synchronized` 方法后调用 `super.method()`，如果锁不可重入，就会产生死锁。

### 互斥性

`synchronized` 保证同一时刻只有一个线程能进入临界区，其他线程必须等待。这是通过对象监视器的排他性实现的。

### 可见性

`synchronized` 不仅保证互斥性，还保证可见性。当线程释放锁时，会将工作内存中的变量修改刷新到主内存；当线程获取锁时，会从主内存重新读取变量的最新值。

## wait() / notify() / notifyAll()

### 基本用法

`wait()`、`notify()` 和 `notifyAll()` 是 `Object` 类的方法，用于线程间的通信。它们**必须在 `synchronized` 块内调用**，否则会抛出 `IllegalMonitorStateException`。

```java
synchronized (lock) {
    while (!condition) {
        lock.wait();    // 释放锁，进入等待状态
    }
    // 条件满足，执行操作
}

synchronized (lock) {
    lock.notify();      // 唤醒一个等待线程
    // 或 lock.notifyAll(); 唤醒所有等待线程
}
```

| 方法 | 作用 | 锁行为 |
|------|------|--------|
| `wait()` | 当前线程进入等待状态 | 释放锁 |
| `wait(long timeout)` | 超时等待，超时后自动唤醒 | 释放锁 |
| `notify()` | 唤醒一个在此对象上等待的线程 | 不释放锁 |
| `notifyAll()` | 唤醒所有在此对象上等待的线程 | 不释放锁 |

### 经典应用：生产者-消费者模式

```java
public class ProducerConsumerDemo {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 10;
    private final Object lock = new Object();

    public void produce(int item) throws InterruptedException {
        synchronized (lock) {
            // 必须用 while 检查条件，防止虚假唤醒
            while (queue.size() == capacity) {
                lock.wait();
            }
            queue.offer(item);
            System.out.println("Produced: " + item + ", size: " + queue.size());
            lock.notifyAll();
        }
    }

    public int consume() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty()) {
                lock.wait();
            }
            int item = queue.poll();
            System.out.println("Consumed: " + item + ", size: " + queue.size());
            lock.notifyAll();
            return item;
        }
    }
}
```

### 虚假唤醒（Spurious Wakeup）

线程可能在没有被 `notify()`/`notifyAll()` 调用的情况下被唤醒，这是操作系统层面的行为，称为虚假唤醒。因此，**必须使用 `while` 循环检查条件，而不是 `if` 语句**。

```java
// 错误写法：使用 if
synchronized (lock) {
    if (!condition) {
        lock.wait();
    }
    // 被虚假唤醒后，条件可能仍不满足，但会继续执行
}

// 正确写法：使用 while
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }
    // 被虚假唤醒后，会重新检查条件
}
```

## volatile 关键字

### 保证可见性

`volatile` 最核心的作用是保证变量的可见性。当一个线程修改 `volatile` 变量时，修改会立即刷新到主内存；其他线程读取该变量时，会从主内存重新获取最新值。

```java
public class VolatileDemo {
    private volatile boolean running = true;

    public void stop() {
        running = false; // 修改立即对其他线程可见
    }

    public void run() {
        while (running) {
            // 如果没有 volatile，其他线程可能永远看不到 running 变为 false
            doSomething();
        }
    }
}
```

### 禁止指令重排

编译器和处理器会对指令进行重排序优化，但在多线程环境下可能导致问题。`volatile` 通过插入内存屏障（Memory Barrier）来禁止重排序。

```
┌─────────────────────────────────────────────────────────┐
│                  内存屏障类型                               │
├──────────────┬──────────────────────────────────────────┤
│ LoadLoad     │ 确保 Load1 在 Load2 之前执行               │
│ StoreStore   │ 确保 Store1 在 Store2 之前执行              │
│ LoadStore    │ 确保 Load1 在 Store2 之前执行               │
│ StoreLoad    │ 确保 Store1 在 Load2 之前执行               │
└──────────────┴──────────────────────────────────────────┘
```

`volatile` 写操作前插入 StoreStore 屏障，后插入 StoreLoad 屏障；`volatile` 读操作后插入 LoadLoad 和 LoadStore 屏障。

### 不保证原子性

`volatile` 只保证可见性和有序性，**不保证原子性**。经典的反例是 `i++` 操作：

```java
// 非线程安全！i++ 实际包含三个操作：读取、加 1、写回
volatile int i = 0;
i++; // 不是原子操作
```

`i++` 的字节码如下：

```
1. 读取 i 的当前值（getfield）
2. 将值加 1（iadd）
3. 将结果写回 i（putfield）
```

在多线程环境下，两个线程可能同时读取到相同的值，各自加 1 后写回，导致结果丢失。要保证原子性，应使用 `synchronized` 或 `AtomicInteger`。

## volatile 的适用场景

### 状态标志

最简单的 `volatile` 使用场景——用作开关标志：

```java
private volatile boolean shutdownRequested = false;

public void shutdown() {
    shutdownRequested = true;
}

public void doWork() {
    while (!shutdownRequested) {
        // 处理任务
    }
}
```

### 双重检查锁定（DCL）单例

```java
public class Singleton {
    // 必须用 volatile 修饰，防止指令重排导致获取到未初始化的对象
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {                    // 第一次检查（无锁）
            synchronized (Singleton.class) {
                if (instance == null) {            // 第二次检查（有锁）
                    instance = new Singleton();    // 非原子操作，需要 volatile
                }
            }
        }
        return instance;
    }
}
```

`new Singleton()` 实际包含三步：① 分配内存 ② 初始化对象 ③ 将引用指向内存地址。如果没有 `volatile`，步骤 ② 和 ③ 可能重排，导致其他线程获取到未初始化的对象。

### 独立观察

当变量的值不依赖于其当前值时，可以安全使用 `volatile`：

```java
// 温度传感器示例：每次写入都是独立的值
volatile double temperature;

public void updateTemperature(double newTemp) {
    temperature = newTemp; // 不依赖之前的值
}
```

## synchronized vs volatile 对比表

| 特性 | synchronized | volatile |
|------|-------------|----------|
| 原子性 | 保证 | 不保证 |
| 可见性 | 保证 | 保证 |
| 有序性 | 保证 | 保证 |
| 阻塞 | 会阻塞其他线程 | 不会阻塞 |
| 性能 | 较重（涉及上下文切换） | 较轻（无锁） |
| 适用场景 | 复合操作、临界区保护 | 简单标志、独立观察 |
| 修饰范围 | 方法、代码块 | 只能修饰变量 |
| 可重入 | 是 | 不适用 |

## 死锁

### 四个必要条件

死锁的发生必须同时满足以下四个条件：

1. **互斥条件**：资源一次只能被一个线程占用
2. **持有并等待**：线程持有资源的同时，等待获取其他资源
3. **不可剥夺**：线程已获得的资源不能被强制剥夺
4. **循环等待**：线程之间形成环形的资源等待关系

### 死锁示例

```java
public class DeadlockDemo {
    private final Object lockA = new Object();
    private final Object lockB = new Object();

    public void method1() {
        synchronized (lockA) {
            System.out.println("Thread 1: Holding lockA...");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            synchronized (lockB) {
                System.out.println("Thread 1: Holding lockA & lockB");
            }
        }
    }

    public void method2() {
        synchronized (lockB) {
            System.out.println("Thread 2: Holding lockB...");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            synchronized (lockA) {
                System.out.println("Thread 2: Holding lockB & lockA");
            }
        }
    }
}
```

### 检测与预防

**检测**：使用 `jstack` 命令或 `ThreadMXBean` 检测死锁：

```java
ThreadMXBean bean = ManagementFactory.getThreadMXBean();
long[] threadIds = bean.findDeadlockedThreads();
if (threadIds != null) {
    ThreadInfo[] infos = bean.getThreadInfo(threadIds);
    for (ThreadInfo info : infos) {
        System.out.println("Deadlocked thread: " + info.getThreadName());
    }
}
```

**预防策略**：

- **固定加锁顺序**：所有线程按相同顺序获取锁（如按对象 hashCode 排序）
- **使用 `tryLock()`**：设置超时，超时后释放已持有的锁
- **避免嵌套锁**：尽量减少同时持有多个锁的情况
- **使用 `java.util.concurrent` 包**：高级并发工具内部已处理死锁问题

## Java 内存模型（JMM）简述

### 主内存与工作内存

Java 内存模型定义了多线程环境下变量的访问规则。每个线程有自己的工作内存（Working Memory），其中保存了主内存（Main Memory）中共享变量的副本。

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   线程 1     │    │   线程 2     │    │   线程 3     │
│  工作内存    │    │  工作内存    │    │  工作内存    │
│  ┌───────┐  │    │  ┌───────┐  │    │  ┌───────┐  │
│  │ x = 1 │  │    │  │ x = 0 │  │    │  │ x = 0 │  │
│  └───────┘  │    │  └───────┘  │    │  └───────┘  │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       │                  │                  │
       └──────────────────┼──────────────────┘
                          │
                   ┌──────┴──────┐
                   │    主内存     │
                   │  ┌───────┐  │
                   │  │ x = 0 │  │
                   │  └───────┘  │
                   └─────────────┘
```

线程对变量的所有操作（读取、赋值）都必须在工作内存中进行，不能直接操作主内存。这就是为什么在多线程环境下，一个线程修改了变量，其他线程可能看不到——因为修改可能只在工作内存中，没有刷新到主内存。

### happens-before 关系

happens-before 是 JMM 中判断数据是否存在竞争、是否安全的核心概念。如果操作 A happens-before 操作 B，则 A 的结果对 B 可见。

主要的 happens-before 规则：

| 规则 | 说明 |
|------|------|
| 程序次序规则 | 同一线程内，前面的操作 happens-before 后面的操作 |
| 锁规则 | unlock 操作 happens-before 后续的 lock 操作 |
| volatile 规则 | volatile 写 happens-before 后续的 volatile 读 |
| 线程启动规则 | `Thread.start()` happens-before 该线程的每个操作 |
| 线程终止规则 | 线程的所有操作 happens-before `Thread.join()` 返回 |
| 传递性 | 如果 A happens-before B，B happens-before C，则 A happens-before C |

理解 happens-before 关系，可以帮助我们判断在特定场景下是否需要加锁，以及加锁是否能保证正确的可见性。

---

`synchronized` 和 `volatile` 是 Java 并发编程的基石。`synchronized` 提供了互斥和可见性的完整保证，适合保护复杂的临界区代码；`volatile` 则是一种轻量级的同步机制，适合简单的标志位和独立观察场景。在实际开发中，应根据具体需求选择合适的同步机制，必要时结合 `java.util.concurrent.atomic` 包中的原子类来保证原子性。