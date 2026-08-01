# 队列、工具类与集合选型

## Queue 接口与 Deque 接口

`Queue` 表示队列——先进先出（FIFO）的容器。`Queue` 定义了两套 API：

| 操作 | 抛异常 | 返回特殊值 |
|------|--------|-----------|
| 入队 | `add(e)` | `offer(e)` |
| 出队 | `remove()` | `poll()` |
| 查看队首 | `element()` | `peek()` |

推荐使用返回特殊值的版本（`offer`/`poll`/`peek`），避免在队列为空或满时抛出异常。

`Deque`（Double-Ended Queue）是双端队列，支持在两端进行插入和删除操作，同时可以作为栈使用。

### ArrayDeque

`ArrayDeque` 基于**循环数组**实现，是 Java 中最推荐的双端队列和栈的实现。

```java
// 作为栈使用（替代 Stack）
Deque<String> stack = new ArrayDeque<>();
stack.push("first");
stack.push("second");
String top = stack.pop();    // "second"
String peek = stack.peek();  // "first"

// 作为队列使用
Deque<String> queue = new ArrayDeque<>();
queue.offer("a");
queue.offer("b");
String head = queue.poll();  // "a"

// 作为双端队列使用
Deque<String> deque = new ArrayDeque<>();
deque.offerFirst("head");
deque.offerLast("tail");
```

`ArrayDeque` 比 `LinkedList` 更适合做队列和栈——它的内存连续，缓存命中率高，且没有节点分配的开销。也比 `Stack`（基于 Vector）快得多。

### PriorityQueue

`PriorityQueue` 是优先队列，元素按**优先级**出队而非按插入顺序。底层基于**小顶堆**实现。

```java
// 默认自然排序（小顶堆）
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.offer(3);
pq.offer(1);
pq.offer(2);
pq.poll();  // 1（最小值先出）
pq.poll();  // 2
pq.poll();  // 3

// 自定义排序（大顶堆）
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(
    Comparator.reverseOrder()
);

// 实际应用：Top-K 问题
PriorityQueue<Integer> topK = new PriorityQueue<>(k);  // 小顶堆，容量 k
for (int num : numbers) {
    if (topK.size() < k) {
        topK.offer(num);
    } else if (num > topK.peek()) {
        topK.poll();
        topK.offer(num);
    }
}
// topK 中就是最大的 k 个数
```

`PriorityQueue` 的 `offer()` 和 `poll()` 时间复杂度为 O(log n)，`peek()` 为 O(1)。

## Collections 工具类

`java.util.Collections` 提供了大量静态方法来操作集合。

### 排序与变换

```java
List<Integer> list = new ArrayList<>(List.of(3, 1, 4, 1, 5));

Collections.sort(list);           // 排序：[1, 1, 3, 4, 5]
Collections.reverse(list);        // 反转：[5, 4, 3, 1, 1]
Collections.shuffle(list);        // 随机打乱
Collections.swap(list, 0, 1);     // 交换位置 0 和 1 的元素
```

### 查找

```java
List<Integer> sorted = List.of(1, 2, 3, 4, 5);

// 二分查找（前提：列表已排序）
int index = Collections.binarySearch(sorted, 3);  // 2

// 最小值和最大值
int min = Collections.min(sorted);  // 1
int max = Collections.max(sorted);  // 5
```

`binarySearch()` 要求列表已经排好序，否则结果未定义。如果元素不存在，返回 `(-(插入点) - 1)`。

### 不可变包装

```java
List<String> mutable = new ArrayList<>(List.of("a", "b", "c"));

List<String> unmodifiable = Collections.unmodifiableList(mutable);
Map<String, Integer> unmodifiableMap = Collections.unmodifiableMap(map);
Set<String> unmodifiableSet = Collections.unmodifiableSet(set);

// unmodifiable 只是视图，修改原集合仍会影响它
mutable.add("d");
System.out.println(unmodifiable.size());  // 4
```

### 线程安全包装

```java
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
```

这些包装器通过在每个方法上加 `synchronized` 实现线程安全，性能一般。对于高并发场景，优先使用 `java.util.concurrent` 包中的类（如 `ConcurrentHashMap`、`CopyOnWriteArrayList`）。

### 类型安全包装

```java
// 运行时检查元素类型，防止混入错误类型
List raw = new ArrayList();
raw.add("hello");
raw.add(42);  // 编译不报错，但运行时可能出问题

List<String> checked = Collections.checkedList(raw, String.class);
checked.add(42);  // 立即抛出 ClassCastException
```

### 空集合与单元素集合

```java
List<String> empty = Collections.emptyList();
Set<Integer> single = Collections.singleton(42);
List<String> copies = Collections.nCopies(5, "hello");  // 5 个 "hello"
```

## 集合与数组的转换

### 数组转集合

```java
String[] array = {"Java", "Python", "Go"};

// Arrays.asList：返回固定大小的 List，不能 add/remove，但可以 set
List<String> list = Arrays.asList(array);
list.set(0, "Kotlin");  // OK
list.add("Rust");       // UnsupportedOperationException

// 想要可变 List，用 ArrayList 包装
List<String> mutable = new ArrayList<>(Arrays.asList(array));

// Java 10+
List<String> copy = List.copyOf(List.of(array));
```

`Arrays.asList()` 返回的 List 背后仍然是原数组——修改 List 会影响数组，修改数组也会影响 List。

### 集合转数组

```java
List<String> list = List.of("Java", "Python", "Go");

// 方式一：toArray() 返回 Object[]
Object[] objArray = list.toArray();

// 方式二：toArray(T[]) 返回正确类型的数组
String[] strArray = list.toArray(new String[0]);

// 方式三（Java 11+）
String[] strArray2 = list.toArray(String[]::new);
```

传入的数组如果长度不够，`toArray` 会创建一个新数组；如果长度足够，会复用传入的数组并在末尾填充 `null`。所以一般传 `new String[0]` 最简洁高效。

## 集合选型指南

面对众多的集合实现，如何选择？以下是一份决策表：

| 需求 | 推荐选择 | 说明 |
|------|----------|------|
| 快速随机访问 | `ArrayList` | O(1) 下标访问，最通用的 List |
| 频繁头部插入/删除 | `ArrayDeque`（两端操作）或 `LinkedList` | 但大多数场景 ArrayList 仍更快 |
| 不重复元素、无序 | `HashSet` | O(1) 查找，最常用的 Set |
| 不重复元素、保持插入顺序 | `LinkedHashSet` | 遍历顺序 = 插入顺序 |
| 不重复元素、需要排序 | `TreeSet` | O(log n)，自然排序或 Comparator |
| 键值映射、无序 | `HashMap` | O(1) 查找，最常用的 Map |
| 键值映射、保持插入顺序 | `LinkedHashMap` | 也可用于实现 LRU 缓存 |
| 键值映射、按键排序 | `TreeMap` | 红黑树实现，O(log n) |
| 队列/栈 | `ArrayDeque` | 循环数组实现，性能最好 |
| 优先队列 | `PriorityQueue` | 堆实现，O(log n) 入队出队 |
| 线程安全 Map | `ConcurrentHashMap` | 分段锁/CAS，高并发首选 |
| 线程安全 List | `CopyOnWriteArrayList` | 读多写少场景 |

**经验法则：**

1. 不确定用什么 List？选 `ArrayList`
2. 不确定用什么 Map？选 `HashMap`
3. 需要排序？用 `Tree` 系列
4. 需要保持顺序？用 `Linked` 系列
5. 需要线程安全？用 `java.util.concurrent` 包

## 遗留集合类

JDK 1.0 时代的集合类已被更好的替代品取代：

| 遗留类 | 替代方案 | 原因 |
|--------|----------|------|
| `Vector` | `ArrayList` | synchronized 开销不必要 |
| `Stack` | `ArrayDeque` | 基于 Vector，性能差 |
| `Hashtable` | `HashMap` / `ConcurrentHashMap` | 全局锁，性能差 |
| `Enumeration` | `Iterator` | 功能更完善，支持 remove |
| `Properties` | 仍在使用 | 配置文件读取场景仍有效 |

`Properties` 是个例外——它仍然广泛用于读取 `.properties` 配置文件，没有直接替代品。

## 不可变集合的现代 API

Java 9 引入了一系列工厂方法，让创建不可变集合变得简洁：

```java
// List
List<String> list = List.of("Java", "Python", "Go");

// Set
Set<Integer> set = Set.of(1, 2, 3);

// Map（最多 10 个键值对）
Map<String, Integer> map = Map.of(
    "Java", 1995,
    "Python", 1991,
    "Go", 2009
);

// Map（超过 10 个键值对）
Map<String, Integer> bigMap = Map.ofEntries(
    Map.entry("Java", 1995),
    Map.entry("Python", 1991),
    Map.entry("Go", 2009),
    Map.entry("Rust", 2010)
);
```

这些方法创建的集合具有以下特点：

- **不可变**：不能添加、删除或修改元素
- **不允许 null**：传入 null 会抛出 `NullPointerException`
- **值对象语义**：元素为基本类型或不可变对象时，整个集合是线程安全的

Java 10 进一步提供了从已有集合创建不可变副本的方法：

```java
List<String> copy = List.copyOf(existingList);
Set<String> setCopy = Set.copyOf(existingSet);
```

这些现代 API 应该成为创建小型不可变集合的首选方式。相比 `Collections.unmodifiableXxx()`，它们更简洁，且不存在"原集合被修改导致不可变视图跟着变"的隐患。
