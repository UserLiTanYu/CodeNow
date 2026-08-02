# Java 并发编程（二）：线程安全、锁与同步机制

上一篇文章演示了多线程共享变量时的数据不一致问题。本篇将系统介绍 Java 提供的各种同步与线程安全机制，从内置锁到显式锁，从原子变量到线程安全集合。

## 为什么需要同步

当多个线程并发访问共享可变数据时，如果不加控制，就会出现**竞态条件（Race Condition）**。

**临界区（Critical Section）** 是一段访问共享资源的代码，同一时刻只能有一个线程执行。同步机制的核心目标就是保证临界区的**互斥访问**。

```java
// 两个线程同时执行 count++，可能出现：
// 线程A读取 count=5 → 线程B读取 count=5 → 线程A写入 6 → 线程B写入 6
// 结果是 6 而不是 7，一次自增被"丢失"了
```

## synchronized 关键字

`synchronized` 是 Java 最基本的内置锁机制，基于对象的监视器（Monitor）实现。

### 同步实例方法

锁住的是当前对象实例 `this`：

```java
public class SafeCounter {
    private int count = 0;

    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}
```

### 同步静态方法

锁住的是 `Class` 对象（`SafeCounter.class`）：

```java
public class StaticSync {
    private static int count = 0;

    public static synchronized void increment() {
        count++; // 锁对象是 StaticSync.class
    }
}
```

实例方法锁 `this`，静态方法锁 `Class` 对象——两者互不影响，不会互斥。

### 同步块

可以更精细地控制锁的范围，减少锁持有时间：

```java
public class FineGrainedSync {
    private final Object lock = new Object();
    private int count = 0;

    public void increment() {
        synchronized (lock) { // 只锁必要的部分
            count++;
        }
    }
}
```

同步块的锁对象可以是任意 Java 对象。常用模式：
- `synchronized(this)` — 等价于同步实例方法
- `synchronized(ClassName.class)` — 等价于同步静态方法
- `synchronized(privateObject)` — 使用私有对象作为锁，更安全

### 锁的可重入性

`synchronized` 是**可重入锁**：同一线程可以多次获取同一把锁，不会自己把自己锁死。

```java
public class ReentrantDemo {
    public synchronized void outer() {
        System.out.println("外层方法");
        inner(); // 同一线程再次获取 this 锁，不会死锁
    }

    public synchronized void inner() {
        System.out.println("内层方法");
    }
}
```

可重入性通过维护一个**锁计数器**实现：首次获取计数为 1，每次重入加 1，释放时减 1，归零后锁真正释放。

### 死锁

当两个或多个线程互相等待对方持有的锁时，就会产生死锁。

```java
public class DeadLockDemo {
    private final Object lockA = new Object();
    private final Object lockB = new Object();

    public void thread1() {
        synchronized (lockA) {
            System.out.println("线程1: 持有A，等待B");
            synchronized (lockB) {
                System.out.println("线程1: 持有A和B");
            }
        }
    }

    public void thread2() {
        synchronized (lockB) {
            System.out.println("线程2: 持有B，等待A");
            synchronized (lockA) {
                System.out.println("线程2: 持有B和A");
            }
        }
    }
}
```

死锁的四个必要条件（破坏任一即可预防）：
1. **互斥** — 资源不能共享（锁的本质，无法破坏）
2. **占有并等待** — 持有资源的同时等待其他资源
3. **不可抢占** — 已获得的资源不能被强制夺走
4. **循环等待** — 线程之间形成环形等待链

实际预防策略：**统一加锁顺序**（如按对象的 hashCode 排序），破坏循环等待条件。

## volatile 关键字

`volatile` 是轻量级的同步机制，解决变量的**可见性**和**有序性**问题。

### 保证可见性

普通变量的修改可能只在线程的工作内存（CPU 缓存）中，对其他线程不可见。`volatile` 强制每次读写都直接操作主内存。

```java
public class VolatileDemo {
    private volatile boolean running = true;

    public void stop() {
        running = false; // 修改立即对其他线程可见
    }

    public void work() {
        while (running) {
            // 如果没有 volatile，这个循环可能永远不终止
            // （JIT 可能优化为 if (!running) while(true) {}）
        }
    }
}
```

### 禁止指令重排

编译器和 CPU 会对指令进行重排序优化。`volatile` 通过插入**内存屏障（Memory Barrier）** 保证特定顺序。

经典的双重检查锁定（DCL）单例模式就依赖 `volatile`：

```java
public class Singleton {
    private static volatile Singleton instance; // 必须 volatile

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {                   // 第一次检查（无锁）
            synchronized (Singleton.class) {
                if (instance == null) {            // 第二次检查（有锁）
                    instance = new Singleton();    // 非原子操作，需要 volatile 防重排
                }
            }
        }
        return instance;
    }
}
```

为什么 `instance` 必须用 `volatile`？因为 `new Singleton()` 分为三步：
1. 分配内存
2. 调用构造函数初始化
3. 将引用指向内存地址

如果发生重排（1→3→2），另一个线程可能在步骤 3 完成后拿到一个**未初始化**的对象。

### 不保证原子性

`volatile` 不能替代锁，它不保证原子操作：

```java
private volatile int count = 0;
count++; // 仍然不是线程安全的！读取、加1、写入是三步操作
```

## Lock 接口与 ReentrantLock

`java.util.concurrent.locks.Lock` 提供了比 `synchronized` 更灵活的锁机制。

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockCounter {
    private final Lock lock = new ReentrantLock();
    private int count = 0;

    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock(); // 必须在 finally 中释放
        }
    }
}
```

`ReentrantLock` 的高级功能：

```java
Lock lock = new ReentrantLock();

// 非阻塞尝试获取锁
if (lock.tryLock()) {
    try {
        // 获取成功
    } finally {
        lock.unlock();
    }
} else {
    // 获取失败，执行其他逻辑
}

// 带超时的尝试获取
if (lock.tryLock(2, TimeUnit.SECONDS)) {
    try { /* ... */ } finally { lock.unlock(); }
}

// 可中断的获取锁
try {
    lock.lockInterruptibly();
    try { /* ... */ } finally { lock.unlock(); }
} catch (InterruptedException e) {
    // 等待锁期间被中断
}
```

### synchronized vs ReentrantLock

| 特性 | synchronized | ReentrantLock |
|------|-------------|---------------|
| 释放锁 | 自动（退出同步块/方法） | 手动（必须在 finally 中 unlock） |
| 可中断 | 不可中断获取 | `lockInterruptibly()` |
| 超时获取 | 不支持 | `tryLock(timeout)` |
| 公平锁 | 非公平 | 可选公平/非公平 |
| 条件变量 | 单一（wait/notify） | 多个 Condition |
| 性能 | JDK 6+ 优化后差异不大 | 略高（竞争激烈时） |

选择建议：**优先使用 `synchronized`**，简单可靠且不易遗漏释放。需要高级功能（超时、中断、多条件）时使用 `ReentrantLock`。

## ReadWriteLock 读写锁

`ReentrantReadWriteLock` 将锁分为读锁和写锁，**读读不互斥**，适用于读多写少的场景。

```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache<K, V> {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final java.util.Map<K, V> map = new java.util.HashMap<>();

    public V get(K key) {
        rwLock.readLock().lock();
        try {
            return map.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void put(K key, V value) {
        rwLock.writeLock().lock();
        try {
            map.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```

| 锁组合 | 是否互斥 |
|-------|---------|
| 读-读 | 不互斥（可并发） |
| 读-写 | 互斥 |
| 写-写 | 互斥 |

## 原子变量与 CAS

`java.util.concurrent.atomic` 包提供了一组基于 **CAS（Compare-And-Swap）** 的无锁线程安全类。

```java
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet(); // 无锁自增
    }

    public int getCount() {
        return count.get();
    }
}
```

常用原子类：

| 类 | 用途 |
|----|------|
| `AtomicInteger` | 原子 int 操作 |
| `AtomicLong` | 原子 long 操作 |
| `AtomicBoolean` | 原子 boolean 操作 |
| `AtomicReference<V>` | 原子对象引用操作 |
| `AtomicIntegerArray` | 原子 int 数组操作 |

**CAS 原理**：比较当前值与期望值，若相等则更新为新值，否则重试。这是一个 CPU 级别的原子指令（如 x86 的 `CMPXCHG`）。

```
CAS(内存地址V, 期望值A, 新值B)：
  if V == A:
      V = B
      return true
  else:
      return false  // 其他线程已修改，重试
```

CAS 的问题：
- **ABA 问题**：值从 A 改为 B 又改回 A，CAS 认为没变。`AtomicStampedReference` 通过版本号解决。
- **自旋开销**：高竞争下大量重试。此时使用锁可能更高效。

## 线程安全的集合

### ConcurrentHashMap

`HashMap` 非线程安全，`Hashtable` 整体加锁性能差。`ConcurrentHashMap` 使用分段锁（JDK 7）或 CAS + synchronized（JDK 8+），实现了高并发下的线程安全。

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("key", 1);
map.computeIfAbsent("key", k -> 0); // 原子操作
```

### CopyOnWriteArrayList

写时复制——每次修改都会创建底层数组的新副本。适用于**读多写少**的场景（如事件监听器列表）。

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("item"); // 复制数组，写入新元素
// 迭代时不会抛 ConcurrentModificationException
```

### BlockingQueue

阻塞队列是生产者-消费者模式的标准工具：

```java
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(100);

// 生产者
queue.put(task); // 队列满时阻塞

// 消费者
Task task = queue.take(); // 队列空时阻塞
```

## ThreadLocal 线程局部变量

`ThreadLocal` 为每个线程提供独立的变量副本，实现线程隔离。

```java
public class UserContext {
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    public static void setUser(String user) {
        currentUser.set(user);
    }

    public static String getUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove(); // 必须清理，防止内存泄漏
    }
}

// 在 Web 请求拦截器中设置
UserContextHolder.setUser("admin");
try {
    // 业务代码中任意位置可获取
    String user = UserContextHolder.getUser(); // "admin"
} finally {
    UserContextHolder.clear(); // 请求结束时清理
}
```

### 使用场景

- **数据库连接**：每个线程绑定独立的 Connection，避免频繁创建/关闭
- **用户会话**：Web 应用中在拦截器设置、Controller 中读取
- **日期格式化**：`SimpleDateFormat` 非线程安全，用 ThreadLocal 为每个线程创建实例

### 内存泄漏风险

`ThreadLocal` 的值存储在线程对象的 `ThreadLocalMap` 中，Key 是 `ThreadLocal` 的弱引用。如果 `ThreadLocal` 对象被 GC 回收，Key 变为 `null`，但 Value 仍然被 Entry 强引用，无法回收。

**防范措施**：使用完毕后**必须调用 `remove()`**，尤其在线程池场景下（线程会被复用，Value 持续累积）。
