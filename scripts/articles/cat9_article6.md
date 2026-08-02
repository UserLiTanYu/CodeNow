# 原子变量与 CAS 原理

在并发编程中，保证共享变量的线程安全是最基本的需求。除了使用 `synchronized` 和 `Lock` 这样的锁机制外，Java 还提供了一种无锁（Lock-Free）的线程安全方案——原子变量。它们基于硬件级别的 CAS（Compare-And-Swap）指令，能够在不加锁的情况下实现线程安全操作。

## 原子操作的需求

### i++ 的线程安全问题

看似简单的 `i++` 操作实际上包含三个步骤：

```
1. 读取（Read）：从内存读取 i 的当前值
2. 修改（Modify）：将值加 1
3. 写入（Write）：将结果写回内存
```

```
线程 A: 读取 i = 0  ──────── 修改 i = 1 ──────── 写入 i = 1
线程 B: ── 读取 i = 0 ──────── 修改 i = 1 ──────── 写入 i = 1
```

两个线程都执行了 `i++`，但最终结果是 1 而不是 2。这就是典型的**读-改-写**（Read-Modify-Write）非原子操作导致的数据竞争问题。

传统的解决方案是使用 `synchronized`，但锁的开销较大。在 Java 5 引入的 `java.util.concurrent.atomic` 包中，提供了一系列基于 CAS 的原子变量类，可以在无锁的情况下保证操作的原子性。

## java.util.concurrent.atomic 包

`atomic` 包提供了四大类原子变量：

| 类别 | 类名 | 说明 |
|------|------|------|
| 基本类型 | AtomicInteger, AtomicLong, AtomicBoolean | 基本数据类型的原子操作 |
| 引用类型 | AtomicReference, AtomicStampedReference, AtomicMarkableReference | 引用类型的原子操作 |
| 数组类型 | AtomicIntegerArray, AtomicLongArray, AtomicReferenceArray | 数组元素的原子操作 |
| 字段更新器 | AtomicIntegerFieldUpdater, AtomicLongFieldUpdater, AtomicReferenceFieldUpdater | 对象字段的原子操作 |

## 基本类型原子变量

### AtomicInteger

`AtomicInteger` 是最常用的原子变量，提供了丰富的原子操作方法：

```java
AtomicInteger counter = new AtomicInteger(0);

// 基本操作
counter.get();                    // 获取当前值
counter.set(10);                  // 设置值
counter.getAndSet(20);            // 设置新值，返回旧值

// 自增/自减操作
counter.incrementAndGet();        // ++i，先加后返回
counter.getAndIncrement();        // i++，先返回后加
counter.decrementAndGet();        // --i，先减后返回
counter.getAndDecrement();        // i--，先返回后减

// 加减操作
counter.addAndGet(5);             // i += 5，先加后返回
counter.getAndAdd(5);             // 先返回后加

// CAS 操作
counter.compareAndSet(25, 30);    // 如果当前值是 25，则更新为 30，返回是否成功

// 函数式更新（Java 8+）
counter.updateAndGet(x -> x * 2);       // 更新并返回新值
counter.getAndUpdate(x -> x * 2);       // 返回旧值后更新
counter.accumulateAndGet(10, Integer::sum);  // 累加并返回新值
```

### AtomicLong

`AtomicLong` 的 API 与 `AtomicInteger` 完全一致，用于 `long` 类型的原子操作。当计数器可能超过 `int` 范围（约 21 亿）时，应使用 `AtomicLong`。

### AtomicBoolean

`AtomicBoolean` 主要用于实现开关标志，其核心方法是 `compareAndSet()`：

```java
AtomicBoolean flag = new AtomicBoolean(false);

// 实现一个线程安全的开关（只执行一次）
public void doOnce() {
    if (flag.compareAndSet(false, true)) {
        // 只有一个线程能进入这里
        System.out.println("执行一次的操作");
    }
}
```

## 引用类型原子变量

### AtomicReference

`AtomicReference<V>` 提供对引用类型的原子操作：

```java
public class AtomicReferenceDemo {
    private AtomicReference<String> value = new AtomicReference<>("initial");

    public void updateValue(String expected, String newValue) {
        value.compareAndSet(expected, newValue);
    }

    public String getValue() {
        return value.get();
    }
}
```

### AtomicStampedReference：解决 ABA 问题

CAS 操作存在 ABA 问题：一个值从 A 变为 B，再变回 A，CAS 会认为值没有变化。`AtomicStampedReference` 通过引入版本号（Stamp）来解决这个问题：

```java
public class ABADemo {
    private AtomicStampedReference<Integer> ref = new AtomicStampedReference<>(1, 0);

    public void update() {
        int stamp = ref.getStamp();  // 获取当前版本号
        int value = ref.getReference();

        // 只有当引用和版本号都匹配时才更新
        boolean success = ref.compareAndSet(1, 2, stamp, stamp + 1);
        System.out.println("Update success: " + success);
        System.out.println("New stamp: " + ref.getStamp());
    }
}
```

```
CAS 只比较值：
  线程 1: 读取 A ──────────────────────────── CAS(A, C) → 成功（但中间已被改为 B 再改回 A）
  线程 2: ── 读取 A → 改为 B → 改为 A ────────

AtomicStampedReference 比较值 + 版本号：
  线程 1: 读取 (A, 1) ──────────────────────── CAS((A,1), (C,2)) → 失败（版本号已变为 3）
  线程 2: ── 读取 (A,1) → 改为 (B,2) → 改为 (A,3) ─────────────
```

### AtomicMarkableReference

`AtomicMarkableReference` 将引用与一个布尔标记关联，适合需要标记引用是否被修改过的场景：

```java
AtomicMarkableReference<String> ref = new AtomicMarkableReference<>("value", false);

boolean[] markHolder = new boolean[1];
String value = ref.get(markHolder);  // 获取值和标记
boolean isMarked = markHolder[0];

// 更新引用和标记
ref.compareAndSet("value", "new value", false, true);
```

## 数组原子变量

数组原子变量可以对数组中的单个元素进行原子操作：

```java
public class AtomicIntegerArrayDemo {
    private AtomicIntegerArray array = new AtomicIntegerArray(10);

    public void increment(int index) {
        array.incrementAndGet(index);  // 原子递增指定索引的元素
    }

    public int get(int index) {
        return array.get(index);
    }

    public void compareAndSet(int index, int expected, int update) {
        array.compareAndSet(index, expected, update);
    }
}
```

| 类名 | 说明 |
|------|------|
| `AtomicIntegerArray` | `int[]` 数组元素的原子操作 |
| `AtomicLongArray` | `long[]` 数组元素的原子操作 |
| `AtomicReferenceArray<V>` | `V[]` 数组元素的原子操作 |

## 字段更新器

字段更新器允许对对象的 `volatile` 字段进行原子操作，而不需要为每个对象创建一个原子变量实例，从而减少内存占用。

### AtomicIntegerFieldUpdater

```java
public class User {
    // 必须是 volatile 且非 private
    volatile int score;

    private static final AtomicIntegerFieldUpdater<User> SCORE_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(User.class, "score");

    public void addScore(int delta) {
        SCORE_UPDATER.addAndGet(this, delta);
    }

    public int getScore() {
        return score;
    }
}

// 使用
User user1 = new User();
User user2 = new User();
user1.addScore(10);  // 只更新 user1 的 score
user2.addScore(20);  // 只更新 user2 的 score
```

**字段要求**：
- 字段必须是 `volatile` 修饰
- 不能是 `static` 字段（除非使用 `AtomicReferenceFieldUpdater` 的静态版本）
- 不能是 `private` 字段（或者通过反射设置可访问）

**优势**：如果有一个包含大量对象的集合，每个对象都需要一个计数器，使用字段更新器只需要一个静态的 Updater 实例，而不是每个对象都持有一个 `AtomicInteger`。

## CAS（Compare-And-Swap）原理

### 硬件支持

CAS 是一种硬件级别的原子操作，由 CPU 直接支持。在 x86 架构上，CAS 通过 `CMPXCHG` 指令实现；在 ARM 架构上，通过 `LDREX`/`STREX` 指令对实现。

### 三个操作数

CAS 操作包含三个操作数：

- **V**（Value）：要更新的内存位置
- **A**（Expected）：期望的旧值
- **B**（New）：要设置的新值

执行逻辑：

```
if (V == A) {
    V = B;      // 更新成功
    return true;
} else {
    return false; // 更新失败，说明其他线程已修改
}
```

这个比较并更新的操作是原子的，由硬件保证。

### 乐观锁 vs 悲观锁

| 锁类型 | 思想 | 实现 | 适用场景 |
|--------|------|------|---------|
| 悲观锁 | 假设一定会冲突，先加锁再操作 | synchronized, ReentrantLock | 写多读少，竞争激烈 |
| 乐观锁 | 假设不会冲突，失败则重试 | CAS, Atomic* | 读多写少，竞争较少 |

CAS 是乐观锁的核心实现方式——不加锁，直接尝试更新；如果失败（说明被其他线程修改了），就重试。

## CAS 的问题

### ABA 问题

值从 A 变为 B 再变回 A，CAS 误认为值没有变化。解决方法是使用 `AtomicStampedReference`（版本号）或 `AtomicMarkableReference`（布尔标记）。

### 自旋开销

CAS 失败后通常会重试（自旋），在高竞争场景下，大量线程同时自旋会导致 CPU 资源浪费。

```java
// 模拟 CAS 自旋
public int incrementWithCAS(AtomicInteger value) {
    int oldValue;
    int newValue;
    do {
        oldValue = value.get();
        newValue = oldValue + 1;
    } while (!value.compareAndSet(oldValue, newValue));  // 失败则重试
    return newValue;
}
```

### 只能保证单个变量的原子性

CAS 一次只能操作一个变量。如果需要同时更新多个变量，可以将它们封装到一个对象中，使用 `AtomicReference` 进行原子更新。

```java
public class Pair {
    final int x;
    final int y;
    // 构造器、getter...
}

AtomicReference<Pair> ref = new AtomicReference<>(new Pair(0, 0));

// 同时更新两个值
Pair oldPair, newPair;
do {
    oldPair = ref.get();
    newPair = new Pair(oldPair.x + 1, oldPair.y + 1);
} while (!ref.compareAndSet(oldPair, newPair));
```

## LongAdder / LongAccumulator（Java 8+）

在高竞争场景下，`AtomicLong` 的 CAS 操作会频繁失败，导致大量自旋。Java 8 引入了 `LongAdder` 和 `LongAccumulator` 来解决这个问题。

### LongAdder：分段累加

`LongAdder` 将一个 `long` 值拆分为多个 `Cell`，每个线程更新自己的 `Cell`，最后汇总结果。这种分段累加的思想大幅减少了 CAS 竞争。

```java
public class LongAdderDemo {
    private LongAdder counter = new LongAdder();

    public void increment() {
        counter.increment();  // 内部可能更新不同的 Cell
    }

    public long get() {
        return counter.sum();  // 汇总所有 Cell 的值
    }

    public void add(long value) {
        counter.add(value);
    }

    public void reset() {
        counter.reset();
    }
}
```

```
AtomicLong：所有线程竞争同一个变量
┌─────────────────────────────────────────────┐
│              AtomicLong = 100               │
│    ↑      ↑      ↑      ↑      ↑          │
│  线程1  线程2  线程3  线程4  线程5          │
└─────────────────────────────────────────────┘

LongAdder：分散到多个 Cell
┌─────────────────────────────────────────────┐
│  Cell[0]=20  Cell[1]=25  Cell[2]=30  ...   │
│    ↑            ↑            ↑              │
│  线程1,4      线程2,5       线程3           │
│                                             │
│  sum() = Cell[0] + Cell[1] + Cell[2] + ... │
└─────────────────────────────────────────────┘
```

### LongAccumulator：自定义累加规则

`LongAccumulator` 是 `LongAdder` 的增强版，支持自定义累加函数：

```java
// 求最大值
LongAccumulator maxAccumulator = new LongAccumulator(Long::max, Long.MIN_VALUE);
maxAccumulator.accumulate(10);
maxAccumulator.accumulate(20);
maxAccumulator.accumulate(15);
System.out.println(maxAccumulator.get());  // 输出 20

// 求乘积
LongAccumulator productAccumulator = new LongAccumulator((a, b) -> a * b, 1);
productAccumulator.accumulate(2);
productAccumulator.accumulate(3);
productAccumulator.accumulate(4);
System.out.println(productAccumulator.get());  // 输出 24
```

### 性能对比

| 操作场景 | AtomicLong | LongAdder |
|---------|------------|-----------|
| 低竞争（单线程） | 更快 | 略慢（有额外开销） |
| 高竞争（多线程写） | CAS 频繁失败，性能差 | 分段累加，性能优秀 |
| 读取频率 | 实时准确 | `sum()` 需要遍历 Cell |
| 内存占用 | 一个 long | 一个 long + Cell 数组 |

**选择建议**：
- 统计计数器、指标收集等场景：使用 `LongAdder`
- 需要精确读取、低竞争场景：使用 `AtomicLong`
- 自定义累加逻辑：使用 `LongAccumulator`

---

原子变量和 CAS 是 Java 并发包的基石。它们提供了无锁的线程安全方案，在竞争不激烈的场景下性能优于传统锁机制。`LongAdder` 的分段累加思想更是将无锁并发的优势发挥到了极致。理解这些底层原理，有助于我们在实际开发中做出正确的技术选型。