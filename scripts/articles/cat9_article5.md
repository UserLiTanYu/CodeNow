# Lock 接口与 Condition

在 Java 5 之前，`synchronized` 和 `wait()`/`notify()` 是实现线程同步与通信的唯一选择。`java.util.concurrent.locks` 包提供了更灵活、更强大的锁机制，包括 `Lock` 接口、`ReadWriteLock` 接口以及 `Condition` 条件对象。

## Lock 接口

`Lock` 接口定义了比 `synchronized` 更广泛的锁操作，提供了非阻塞获取锁、可中断获取锁、超时获取锁等高级特性。

```java
public interface Lock {
    void lock();                    // 获取锁（阻塞）
    void lockInterruptibly() throws InterruptedException;  // 可中断获取
    boolean tryLock();              // 非阻塞尝试获取
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;  // 超时尝试
    void unlock();                  // 释放锁
    Condition newCondition();       // 创建条件对象
}
```

## ReentrantLock

`ReentrantLock` 是 `Lock` 接口最常用的实现类，功能与 `synchronized` 类似，但提供了更多控制能力。

### 基本用法：lock() / unlock()

```java
public class ReentrantLockDemo {
    private final Lock lock = new ReentrantLock();
    private int count = 0;

    public void increment() {
        lock.lock();  // 获取锁
        try {
            count++;
        } finally {
            lock.unlock();  // 必须在 finally 中释放锁，防止异常导致锁泄漏
        }
    }
}
```

**关键点**：`unlock()` 必须放在 `finally` 块中。与 `synchronized` 不同，`Lock` 不会自动释放锁，如果忘记释放，会导致死锁。

### tryLock()：非阻塞尝试获取

`tryLock()` 立即返回，不阻塞当前线程。获取成功返回 `true`，失败返回 `false`。

```java
public void tryDoSomething() {
    if (lock.tryLock()) {
        try {
            // 获取锁成功，执行操作
            doSomething();
        } finally {
            lock.unlock();
        }
    } else {
        // 获取锁失败，执行其他逻辑
        doAlternative();
    }
}
```

### tryLock(timeout, unit)：超时尝试

在指定时间内尝试获取锁，超时后返回 `false`。可以响应中断。

```java
public void doWithTimeout() throws InterruptedException {
    if (lock.tryLock(5, TimeUnit.SECONDS)) {
        try {
            doSomething();
        } finally {
            lock.unlock();
        }
    } else {
        System.out.println("获取锁超时");
    }
}
```

### lockInterruptibly()：可中断的锁获取

与 `lock()` 不同，`lockInterruptibly()` 在等待锁的过程中可以响应中断，适合需要取消等待的场景。

```java
public void interruptibleOperation() throws InterruptedException {
    lock.lockInterruptibly();  // 等待时可被中断
    try {
        doSomething();
    } finally {
        lock.unlock();
    }
}

// 使用示例
Thread t = new Thread(() -> {
    try {
        demo.interruptibleOperation();
    } catch (InterruptedException e) {
        System.out.println("等待锁时被中断");
    }
});
t.start();
t.interrupt();  // 中断线程，使其从 lockInterruptibly() 中退出
```

### 锁状态查询方法

`ReentrantLock` 提供了丰富的锁状态查询方法，用于监控和调试：

```java
ReentrantLock lock = new ReentrantLock();

lock.isLocked()              // 锁是否被任意线程持有
lock.isHeldByCurrentThread() // 锁是否被当前线程持有
lock.getHoldCount()          // 当前线程持有锁的次数（可重入计数）
lock.getQueueLength()        // 等待获取锁的线程数
lock.hasQueuedThreads()      // 是否有线程在等待获取锁
```

## synchronized vs ReentrantLock

| 特性 | synchronized | ReentrantLock |
|------|-------------|---------------|
| 语法 | 关键字，JVM 内置 | 类，API 层面 |
| 释放锁 | 自动（退出同步块） | 手动（必须在 finally 中 unlock） |
| 可中断 | 不支持 | `lockInterruptibly()` |
| 非阻塞尝试 | 不支持 | `tryLock()` |
| 超时尝试 | 不支持 | `tryLock(time, unit)` |
| 公平锁 | 非公平 | 可选公平/非公平 |
| 条件变量 | 单一（wait/notify） | 多个 Condition |
| 锁状态查询 | 有限 | 丰富的方法 |
| 性能 | Java 6 后优化良好 | 高竞争下略优 |

**选择建议**：

- **简单同步**：优先使用 `synchronized`，代码更简洁，不易出错
- **需要高级特性**：使用 `ReentrantLock`，如可中断、超时、公平锁、多条件等
- **读多写少**：使用 `ReadWriteLock`

## ReadWriteLock 接口

`ReadWriteLock` 维护一对锁：读锁和写锁。读锁是共享锁，允许多个线程同时读取；写锁是排他锁，独占访问。

```java
public interface ReadWriteLock {
    Lock readLock();   // 获取读锁
    Lock writeLock();  // 获取写锁
}
```

### ReentrantReadWriteLock

```java
public class CachedData {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private Map<String, Object> cache = new HashMap<>();

    // 读操作：多个线程可以同时读
    public Object get(String key) {
        readLock.lock();
        try {
            return cache.get(key);
        } finally {
            readLock.unlock();
        }
    }

    // 写操作：独占访问
    public void put(String key, Object value) {
        writeLock.lock();
        try {
            cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }
}
```

### 读写锁的特性

| 操作组合 | 是否兼容 |
|---------|---------|
| 读 - 读 | 兼容（可同时进行） |
| 读 - 写 | 互斥 |
| 写 - 写 | 互斥 |

### 锁降级与锁升级

**锁降级**（允许）：持有写锁的情况下，可以获取读锁，然后释放写锁。这样可以在保证数据可见性的同时，让其他读线程访问。

```java
writeLock.lock();
try {
    // 修改数据
    readLock.lock();   // 在持有写锁时获取读锁（锁降级）
} finally {
    writeLock.unlock(); // 释放写锁，降级为读锁
}
try {
    // 使用数据（仍持有读锁）
} finally {
    readLock.unlock();
}
```

**锁升级**（不允许）：持有读锁的情况下，不能直接获取写锁。必须先释放读锁，再获取写锁。这是为了防止死锁——如果两个线程都持有读锁并尝试升级为写锁，就会互相等待。

## Condition 接口

`Condition` 接口提供了比 `wait()`/`notify()` 更灵活的线程通信机制。一个 `Lock` 可以创建多个 `Condition`，每个条件对象都有自己的等待队列，可以实现精确唤醒。

### 基本用法

```java
Lock lock = new ReentrantLock();
Condition condition = lock.newCondition();

// 等待
lock.lock();
try {
    while (!conditionMet) {
        condition.await();  // 对应 wait()
    }
    // 执行操作
} finally {
    lock.unlock();
}

// 唤醒
lock.lock();
try {
    condition.signal();      // 对应 notify()
    // 或 condition.signalAll(); 对应 notifyAll()
} finally {
    lock.unlock();
}
```

### Condition vs Object 监视器方法

| Object 方法 | Condition 方法 | 说明 |
|------------|---------------|------|
| `wait()` | `await()` | 释放锁并等待 |
| `wait(long timeout)` | `await(long time, TimeUnit unit)` | 超时等待 |
| `notify()` | `signal()` | 唤醒一个等待线程 |
| `notifyAll()` | `signalAll()` | 唤醒所有等待线程 |

### 多条件精确唤醒

`Condition` 最大的优势是支持多个条件队列。以生产者-消费者为例，可以分别设置"不满"和"不空"两个条件：

```java
public class BoundedBuffer<T> {
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();   // 缓冲区不满
    private final Condition notEmpty = lock.newCondition();  // 缓冲区不空
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();  // 缓冲区满，等待"不满"条件
            }
            queue.offer(item);
            notEmpty.signal();  // 唤醒一个等待"不空"条件的消费者
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();  // 缓冲区空，等待"不空"条件
            }
            T item = queue.poll();
            notFull.signal();  // 唤醒一个等待"不满"条件的生产者
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

相比 `notifyAll()` 会唤醒所有等待线程（包括生产者和消费者），`signal()` 可以精确唤醒特定类型的线程，减少不必要的上下文切换。

## StampedLock（Java 8+）

`StampedLock` 是 Java 8 引入的新锁，相比 `ReadWriteLock` 提供了更高的并发性能。它支持三种模式：

1. **写锁**（独占）：与 `ReadWriteLock` 的写锁类似
2. **悲观读锁**（共享）：与 `ReadWriteLock` 的读锁类似
3. **乐观读**（无锁）：不加锁，通过校验判断数据是否被修改

```java
public class Point {
    private final StampedLock sl = new StampedLock();
    private double x, y;

    // 写操作
    public void move(double deltaX, double deltaY) {
        long stamp = sl.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    // 乐观读
    public double distanceFromOrigin() {
        long stamp = sl.tryOptimisticRead();  // 获取乐观读戳记
        double currentX = x, currentY = y;    // 读取数据（无锁）
        if (!sl.validate(stamp)) {
            // 数据被修改，升级为悲观读锁
            stamp = sl.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                sl.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    // 悲观读锁
    public void moveIfAtOrigin(double newX, double newY) {
        long stamp = sl.readLock();
        try {
            while (x == 0.0 && y == 0.0) {
                // 尝试升级为写锁
                long ws = sl.tryConvertToWriteLock(stamp);
                if (ws != 0L) {
                    stamp = ws;
                    x = newX;
                    y = newY;
                    break;
                } else {
                    // 升级失败，释放读锁，获取写锁
                    sl.unlockRead(stamp);
                    stamp = sl.writeLock();
                }
            }
        } finally {
            sl.unlock(stamp);
        }
    }
}
```

### StampedLock 注意事项

- `StampedLock` 不可重入，同一线程重复获取会导致死锁
- 不支持 `Condition` 条件变量
- 乐观读适合读多写少、读操作非常短的场景
- 需要手动管理 stamp（戳记），使用不当容易出错

## 实战：使用 ReentrantLock + Condition 实现有界缓冲区

下面是一个完整的有界缓冲区实现，支持超时等待：

```java
public class TimedBoundedBuffer<E> {
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private final Object[] items;
    private int putIndex, takeIndex, count;

    public TimedBoundedBuffer(int capacity) {
        items = new Object[capacity];
    }

    public boolean offer(E item, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (count == items.length) {
                if (nanos <= 0) {
                    return false;  // 超时，返回失败
                }
                nanos = notFull.awaitNanos(nanos);  // 超时等待
            }
            enqueue(item);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                if (nanos <= 0) {
                    return null;  // 超时，返回 null
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private E dequeue() {
        Object item = items[takeIndex];
        items[takeIndex] = null;
        takeIndex = (takeIndex + 1) % items.length;
        count--;
        notFull.signal();
        return (E) item;
    }

    private void enqueue(E item) {
        items[putIndex] = item;
        putIndex = (putIndex + 1) % items.length;
        count++;
        notEmpty.signal();
    }
}
```

---

`Lock` 接口和 `Condition` 为 Java 并发编程提供了比 `synchronized` 更灵活的锁控制能力。在简单场景下，`synchronized` 仍然是首选（代码更简洁、不易出错）；但在需要可中断锁、超时锁、公平锁、多条件精确唤醒等高级特性时，`ReentrantLock` 是更好的选择。对于读多写少的场景，`ReadWriteLock` 或 `StampedLock` 可以显著提升并发性能。