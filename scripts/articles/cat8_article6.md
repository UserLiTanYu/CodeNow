# 并发集合与遗留集合类

在多线程环境下，普通的集合类（如 `ArrayList`、`HashMap`）不是线程安全的，并发操作可能导致数据不一致、`ConcurrentModificationException` 甚至程序崩溃。Java 提供了多种并发集合来解决这些问题，同时也保留了一些遗留的同步集合类。本文将详细介绍这些并发集合的实现原理和使用场景。

## 并发集合的需求

多线程环境下的集合操作面临以下挑战：

- **竞态条件**：多个线程同时修改集合导致数据不一致
- **可见性**：一个线程的修改对其他线程不可见
- **原子性**：复合操作（如检查再执行）不是原子的

```java
// 非线程安全的复合操作
if (!map.containsKey(key)) {
    map.put(key, value); // 另一个线程可能已经插入了
}

// 非线程安全的 count++
count++; // 实际上是读-改-写三步操作
```

## ConcurrentHashMap

`ConcurrentHashMap` 是最常用的并发 Map 实现，它的实现机制在 Java 7 和 Java 8 中有重大变化。

### Java 7：分段锁（Segment）

Java 7 的 `ConcurrentHashMap` 使用分段锁技术：

```
┌─────────────────────────────────────────────────────┐
│              ConcurrentHashMap (Java 7)              │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Segment[0] ──→ HashEntry[] ──→ 链表               │
│  Segment[1] ──→ HashEntry[] ──→ 链表               │
│  Segment[2] ──→ HashEntry[] ──→ 链表               │
│  ...                                                │
│  Segment[15] ──→ HashEntry[] ──→ 链表              │
│                                                     │
│  默认 16 个 Segment，每个 Segment 是一个独立的 HashMap  │
│  锁的粒度：Segment 级别（最多支持 16 个线程并发写）     │
└─────────────────────────────────────────────────────┘
```

每个 `Segment` 继承自 `ReentrantLock`，不同 `Segment` 的操作互不影响。

### Java 8：CAS + synchronized

Java 8 对 `ConcurrentHashMap` 进行了重大重构，放弃了分段锁：

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        
        // 1. 表为空则初始化
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
            
        // 2. 桶为空，CAS 插入（无锁）
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                break;
        }
        
        // 3. 正在扩容，帮助扩容
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
            
        // 4. 桶不为空，synchronized 锁住头节点
        else {
            V oldVal = null;
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        // 链表操作
                        for (Node<K,V> e = f;; ++binCount) {
                            if (e.hash == hash && 
                                ((ek = e.key) == key || ...)) {
                                oldVal = e.val;
                                if (!onlyIfAbsent) e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<>(hash, key, value, null);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        // 红黑树操作
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

**Java 8 的改进：**
- 锁的粒度更细：从 Segment（包含多个桶）细化到单个桶节点
- 使用 CAS 操作进行无锁插入（桶为空时）
- 使用 synchronized 锁住桶的头节点（桶不为空时）
- 支持更高程度的并发

### 不能存 null 键和 null 值

`ConcurrentHashMap` 不允许 `null` 键和 `null` 值：

```java
ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
map.put(null, "value"); // NullPointerException
map.put("key", null);   // NullPointerException
```

**原因：** 在并发环境下，`get()` 返回 `null` 无法区分是"键不存在"还是"值为 null"。而 `HashMap` 可以用 `containsKey()` 来区分，但在并发环境下这两个操作不是原子的。

### 常用方法

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// putIfAbsent：不存在才插入
map.putIfAbsent("key", 1);

// computeIfAbsent：不存在则计算并插入（常用于缓存）
map.computeIfAbsent("key", k -> expensiveComputation(k));

// computeIfPresent：存在则计算并更新
map.computeIfPresent("key", (k, v) -> v + 1);

// compute：无论如何都计算
map.compute("key", (k, v) -> v == null ? 1 : v + 1);

// merge：合并值
map.merge("key", 1, Integer::sum); // 等价于累加

// replace：替换
map.replace("key", 1, 2); // 旧值为 1 时才替换为 2

// forEach：遍历（支持并行）
map.forEach((k, v) -> System.out.println(k + "=" + v));

// 批量操作（并行）
map.forEach(4, (k, v) -> System.out.println(k + "=" + v)); // 并行阈值 4
```

### size() 的近似性

在并发环境下，`size()` 的结果是近似的：

```java
// 推荐使用 mappingCount()（Java 8+）
long size = map.mappingCount(); // 返回 long
```

**原因：** `size()` 使用 `baseCount` + `CounterCell[]` 计算，在并发修改时无法得到精确值。如果需要精确计数，需要额外的同步措施。

## CopyOnWriteArrayList

`CopyOnWriteArrayList` 是一个线程安全的 `List` 实现，采用**写时复制**（Copy-On-Write）策略。

### 写时复制原理

```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        // 复制一个新数组
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        newElements[len] = e;
        // 替换引用
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
```

每次写操作（add、set、remove）都会创建一个新的底层数组，然后替换原来的数组。读操作直接在当前数组上进行，无需加锁。

### 特点与适用场景

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// 读操作：无锁，性能高
for (String s : list) {
    System.out.println(s);
}

// 写操作：需要复制数组，性能低
list.add("new item");
```

**适用场景：** 读多写少的场景，如事件监听器列表、配置列表。

**不适用场景：** 写操作频繁的场景，因为每次写都要复制整个数组。

### 迭代器特性

`CopyOnWriteArrayList` 的迭代器是**弱一致性**的，不会抛出 `ConcurrentModificationException`：

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(Arrays.asList("A", "B", "C"));

Iterator<String> it = list.iterator();
list.add("D"); // 修改集合

// 迭代器仍然遍历旧数组，不会看到 "D"
while (it.hasNext()) {
    System.out.println(it.next()); // A, B, C
}
```

## BlockingQueue

`BlockingQueue` 是一个阻塞式队列，当队列满时 `put` 会阻塞，当队列空时 `take` 会阻塞。它是实现生产者-消费者模式的理想选择。

### 接口定义

```java
public interface BlockingQueue<E> extends Queue<E> {
    // 阻塞式操作
    void put(E e) throws InterruptedException;      // 队列满时阻塞
    E take() throws InterruptedException;            // 队列空时阻塞
    boolean offer(E e, long timeout, TimeUnit unit); // 超时返回 false
    E poll(long timeout, TimeUnit unit);             // 超时返回 null
    
    // 非阻塞式操作
    boolean offer(E e);  // 队列满时返回 false
    E poll();            // 队列空时返回 null
}
```

### 常见实现

| 实现类 | 特点 | 适用场景 |
|--------|------|----------|
| ArrayBlockingQueue | 有界数组实现，公平锁可选 | 固定大小的缓冲区 |
| LinkedBlockingQueue | 可选有界，链表实现 | 不确定大小的缓冲区 |
| PriorityBlockingQueue | 无界，优先级排序 | 需要优先级的任务队列 |
| SynchronousQueue | 不存储元素，直接传递 | 直接交付的场景 |
| DelayQueue | 延迟获取，元素需实现 Delayed | 定时任务、缓存过期 |

### 生产者-消费者模式

```java
public class ProducerConsumerDemo {
    private static final BlockingQueue<Task> queue = new ArrayBlockingQueue<>(10);
    
    // 生产者
    static class Producer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Task task = createTask();
                    queue.put(task); // 队列满时阻塞
                    System.out.println("Produced: " + task);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // 消费者
    static class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Task task = queue.take(); // 队列空时阻塞
                    processTask(task);
                    System.out.println("Consumed: " + task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) {
        // 启动多个生产者和消费者
        ExecutorService executor = Executors.newFixedThreadPool(4);
        executor.submit(new Producer());
        executor.submit(new Producer());
        executor.submit(new Consumer());
        executor.submit(new Consumer());
    }
}
```

### 有界队列的优势

```java
// 有界队列可以防止生产者过快导致内存溢出
BlockingQueue<Task> boundedQueue = new ArrayBlockingQueue<>(100);

// 无界队列可能耗尽内存
BlockingQueue<Task> unboundedQueue = new LinkedBlockingQueue<>(); // 默认 Integer.MAX_VALUE
```

## ConcurrentLinkedQueue

`ConcurrentLinkedQueue` 是一个无锁的非阻塞队列，基于 CAS（Compare-And-Swap）实现。

```java
ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

// 入队
queue.offer("item1");
queue.offer("item2");

// 出队
String item = queue.poll(); // "item1"

// 查看队首
String peek = queue.peek(); // "item2"
```

**特点：**
- 无锁实现，高并发下性能好
- 非阻塞：`poll()` 在队列空时返回 `null`
- 弱一致性迭代器
- 适合高并发、无界的队列场景

**与 BlockingQueue 的区别：**

| 特性 | ConcurrentLinkedQueue | BlockingQueue |
|------|----------------------|---------------|
| 阻塞 | 否 | 是 |
| 锁 | 无锁（CAS） | 有锁 |
| 有界 | 无界 | 可选有界 |
| 适用场景 | 高并发、非阻塞 | 生产者-消费者 |

## Collections 工具类的并发包装

`Collections` 工具类提供了将普通集合包装为同步集合的方法。

### synchronized 包装

```java
// 创建同步集合
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
Set<String> syncSet = Collections.synchronizedSet(new HashSet<>());
Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());
```

**注意事项：**

```java
List<String> list = Collections.synchronizedList(new ArrayList<>());

// 单个操作是线程安全的
list.add("item");
list.size();

// 复合操作需要手动同步
synchronized (list) {
    if (!list.contains("item")) {
        list.add("item");
    }
}

// 迭代时也需要手动同步
synchronized (list) {
    for (String s : list) {
        System.out.println(s);
    }
}
```

**性能问题：** `synchronized` 包装使用全局锁，所有操作（包括读）都要竞争同一把锁，性能较差。在高并发场景下，应该使用 `ConcurrentHashMap`、`CopyOnWriteArrayList` 等专门的并发集合。

### 不可变包装

```java
// 创建不可变集合
List<String> unmodifiableList = Collections.unmodifiableList(list);
Set<String> unmodifiableSet = Collections.unmodifiableSet(set);
Map<String, String> unmodifiableMap = Collections.unmodifiableMap(map);

// 修改会抛出 UnsupportedOperationException
unmodifiableList.add("item"); // UnsupportedOperationException
```

**注意：** 不可变包装只是创建了一个**视图**，底层集合仍然可以修改：

```java
List<String> list = new ArrayList<>();
list.add("A");
List<String> unmodifiable = Collections.unmodifiableList(list);

list.add("B"); // 底层集合可以修改
System.out.println(unmodifiable); // [A, B]，unmodifiable 也变了
```

Java 9+ 提供了真正的不可变集合工厂方法：

```java
List<String> immutable = List.of("A", "B", "C"); // 真正不可变
Set<String> immutableSet = Set.of("A", "B", "C");
Map<String, String> immutableMap = Map.of("key1", "val1", "key2", "val2");
```

## 遗留集合类

Java 早期版本（JDK 1.0/1.1）提供了一些同步集合类，现在已不推荐使用。

### Vector

`Vector` 是同步的 `ArrayList`，所有方法都用 `synchronized` 修饰：

```java
// 不推荐使用
Vector<String> vector = new Vector<>();
vector.add("item");

// 推荐替代方案
List<String> list = Collections.synchronizedList(new ArrayList<>());
// 或使用 CopyOnWriteArrayList（读多写少时）
```

**缺点：**
- 粒度太粗：即使单线程也要获取锁
- 某些复合操作仍需手动同步
- 性能差

### Hashtable

`Hashtable` 是同步的 `HashMap`，不允许 `null` 键和 `null` 值：

```java
// 不推荐使用
Hashtable<String, String> hashtable = new Hashtable<>();
hashtable.put("key", "value");

// 推荐替代方案
Map<String, String> map = new ConcurrentHashMap<>();
```

**缺点：**
- 全表锁，性能差
- 不允许 null
- 迭代器是 fail-fast 的

### Stack

`Stack` 继承自 `Vector`，实现了后进先出（LIFO）的栈：

```java
// 不推荐使用
Stack<String> stack = new Stack<>();
stack.push("A");
stack.push("B");
String top = stack.pop(); // "B"

// 推荐替代方案
Deque<String> stack = new ArrayDeque<>();
stack.push("A");
stack.push("B");
String top = stack.pop(); // "B"
```

`ArrayDeque` 作为栈使用时性能优于 `Stack`，因为它没有同步开销。

### Enumeration

`Enumeration` 是 `Iterator` 的前身，功能较弱：

```java
// 旧式遍历
Enumeration<String> elements = vector.elements();
while (elements.hasMoreElements()) {
    System.out.println(elements.nextElement());
}

// 现代遍历
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    System.out.println(it.next());
}

// 或 for-each
for (String s : list) {
    System.out.println(s);
}
```

### Properties

`Properties` 是 `Hashtable` 的子类，用于处理配置文件：

```java
Properties props = new Properties();

// 从文件加载
try (InputStream in = new FileInputStream("config.properties")) {
    props.load(in);
}

// 获取属性
String value = props.getProperty("key");
String defaultValue = props.getProperty("key", "default");

// 设置属性
props.setProperty("newKey", "newValue");

// 保存到文件
try (OutputStream out = new FileOutputStream("config.properties")) {
    props.store(out, "Comments");
}
```

`Properties` 是少数仍有使用场景的遗留类，但建议只在处理 `.properties` 文件时使用。

## 集合选型总结

### 线程安全场景的选择指南

| 场景 | 推荐集合 | 说明 |
|------|----------|------|
| 通用并发 Map | ConcurrentHashMap | 高并发、高性能 |
| 读多写少的 List | CopyOnWriteArrayList | 事件监听器、配置 |
| 生产者-消费者队列 | ArrayBlockingQueue / LinkedBlockingQueue | 阻塞式、有界/无界 |
| 高并发无阻塞队列 | ConcurrentLinkedQueue | 无锁实现 |
| 优先级任务队列 | PriorityBlockingQueue | 按优先级处理 |
| 定时任务 | DelayQueue | 延迟执行 |
| 简单同步包装 | Collections.synchronizedXxx | 低并发场景 |

### 性能对比

| 操作 | HashMap | Hashtable | ConcurrentHashMap |
|------|---------|-----------|-------------------|
| get | O(1) | O(1) | O(1) |
| put | O(1) | O(1) | O(1) |
| 并发读 | 不安全 | 安全（慢） | 安全（快） |
| 并发写 | 不安全 | 安全（慢） | 安全（快） |
| null 支持 | 是 | 否 | 否 |

### 选型决策树

```
需要线程安全？
├── 否 → ArrayList / HashMap / HashSet
└── 是 → 
    ├── Map → ConcurrentHashMap
    ├── List →
    │   ├── 读多写少 → CopyOnWriteArrayList
    │   └── 读写均衡 → Collections.synchronizedList
    ├── Queue →
    │   ├── 需要阻塞 → BlockingQueue 实现类
    │   └── 不需要阻塞 → ConcurrentLinkedQueue
    └── Set →
        ├── 读多写少 → CopyOnWriteArraySet
        └── 通用 → ConcurrentHashMap.newKeySet()
```

在现代 Java 开发中，应该优先使用 `java.util.concurrent` 包下的并发集合，而不是遗留的同步包装类。这些专门设计的并发集合在保证线程安全的同时，提供了更好的并发性能。
