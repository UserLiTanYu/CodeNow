# Set、Map 与哈希原理

## Set 接口

`Set` 是不允许重复元素的集合。判断两个元素是否"重复"依据的是 `equals()` 方法——如果 `set.add(e)` 时集合中已存在一个元素 `e2` 满足 `e.equals(e2)`，则添加失败。

```java
Set<String> set = new HashSet<>();
set.add("Java");
set.add("Python");
set.add("Java");  // 重复，不会被添加
System.out.println(set.size());  // 2
```

## Set 的三种实现

### HashSet

`HashSet` 底层基于 `HashMap` 实现，元素作为 Map 的 Key 存储，Value 统一使用一个静态的占位对象。

- **无序**：不保证元素的遍历顺序
- **性能**：添加、删除、查找均为 O(1)
- **要求**：元素必须正确实现 `hashCode()` 和 `equals()`

```java
Set<String> languages = new HashSet<>();
languages.add("Java");
languages.add("Go");
languages.add("Rust");
```

### LinkedHashSet

`LinkedHashSet` 继承自 `HashSet`，内部使用一个双向链表维护元素的**插入顺序**。

```java
Set<String> ordered = new LinkedHashSet<>();
ordered.add("Java");
ordered.add("Go");
ordered.add("Rust");
// 遍历顺序：Java → Go → Rust
```

### TreeSet

`TreeSet` 基于红黑树实现，元素按**自然排序**或自定义 `Comparator` 排序。

- **有序**：元素按排序规则排列
- **性能**：添加、删除、查找均为 O(log n)
- **要求**：元素必须实现 `Comparable` 接口，或者在构造时传入 `Comparator`

```java
// 自然排序
TreeSet<Integer> numbers = new TreeSet<>();
numbers.add(3);
numbers.add(1);
numbers.add(2);
// 遍历顺序：1, 2, 3

// 自定义排序
TreeSet<String> byLength = new TreeSet<>(
    Comparator.comparingInt(String::length)
);
byLength.add("Go");
byLength.add("JavaScript");
byLength.add("Java");
// 遍历顺序：Go → Java → JavaScript
```

## Set 的常用操作

Set 天然支持数学上的集合运算：

```java
Set<String> a = new HashSet<>(Set.of("Java", "Python", "Go"));
Set<String> b = new HashSet<>(Set.of("Python", "Rust", "C++"));

// 并集
Set<String> union = new HashSet<>(a);
union.addAll(b);  // [Java, Python, Go, Rust, C++]

// 交集
Set<String> intersection = new HashSet<>(a);
intersection.retainAll(b);  // [Python]

// 差集
Set<String> difference = new HashSet<>(a);
difference.removeAll(b);  // [Java, Go]
```

## Map 接口

`Map` 存储键值对（Key-Value）映射，**键不允许重复**。如果对同一个 Key 调用两次 `put()`，后一次的值会覆盖前一次。

## Map 的主要实现

### HashMap

`HashMap` 是最常用的 Map 实现。Java 8 之后，其底层结构为**数组 + 链表 + 红黑树**。

- 数组中的每个位置称为一个**桶（Bucket）**
- 当哈希冲突导致同一个桶中的元素超过 8 个时，链表会转换为红黑树，查找效率从 O(n) 提升到 O(log n)
- 当红黑树节点少于 6 个时，退化回链表

**性能**：在哈希分布均匀的情况下，`put`、`get`、`remove` 均为 O(1)。

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 95);
scores.put("Bob", 87);

int aliceScore = scores.get("Alice");  // 95
```

### LinkedHashMap

`LinkedHashMap` 在 HashMap 的基础上，用一个双向链表维护键值对的**插入顺序**或**访问顺序**。

```java
// 插入顺序（默认）
Map<String, Integer> insertion = new LinkedHashMap<>();

// 访问顺序（第三个参数为 true）
Map<String, Integer> access = new LinkedHashMap<>(16, 0.75f, true);
```

访问顺序模式下，每次 `get()` 都会将该元素移到链表末尾。利用这一特性，可以轻松实现 **LRU 缓存**：

```java
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public LRUCache(int maxSize) {
        super(16, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
```

### TreeMap

`TreeMap` 基于红黑树实现，按键的排序顺序维护键值对。

```java
TreeMap<String, Integer> sorted = new TreeMap<>();
sorted.put("Banana", 2);
sorted.put("Apple", 5);
sorted.put("Cherry", 1);

// 按键排序遍历：Apple → Banana → Cherry
sorted.forEach((k, v) -> System.out.println(k + ": " + v));

// 获取键的子范围
Map<String, Integer> sub = sorted.subMap("A", "C");
```

### Hashtable 与 ConcurrentHashMap

`Hashtable` 是 JDK 1.0 的遗留类，所有方法都用 `synchronized` 修饰，性能差，不推荐使用。

`ConcurrentHashMap` 是 Java 5 引入的线程安全 HashMap，采用分段锁（Java 7）或 CAS + synchronized（Java 8）实现高并发访问，是多线程场景下的首选。

## 哈希原理详解

理解哈希原理是用好 HashSet 和 HashMap 的前提。

### hashCode() 的契约

Java 对 `hashCode()` 有明确的契约：

1. 同一个对象在程序运行期间，`hashCode()` 必须返回相同的值（前提是 equals 比较用到的字段没有被修改）
2. 如果 `a.equals(b)` 为 true，则 `a.hashCode()` 必须等于 `b.hashCode()`
3. 如果 `a.equals(b)` 为 false，不要求 `hashCode()` 一定不同，但不同能提升哈希表性能

违反第 2 条会导致 HashMap 无法正确查找元素——这在实际开发中是常见的 bug 来源。

### 哈希冲突

不同的对象可能产生相同的哈希码，这就是**哈希冲突**。解决冲突的常见方式：

- **链地址法**（Java HashMap 采用）：每个桶维护一个链表或红黑树，冲突的元素挂在同一个桶上
- **开放寻址法**：冲突时按某种规则探测下一个空位

### HashMap 的 put 流程

```java
map.put(key, value);
```

大致流程如下：

1. **计算哈希**：`(key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)`——高 16 位与低 16 位异或，减少冲突
2. **定位桶**：`hash & (n - 1)`，其中 `n` 是数组长度（必须是 2 的幂）
3. **处理冲突**：
   - 桶为空 → 直接放入
   - 桶不为空 → 遍历链表/红黑树，找到相同 Key 则覆盖 Value，否则追加到末尾
4. **判断是否扩容**：如果元素总数超过 `容量 × 负载因子`（默认 0.75），则扩容为原来的 2 倍

### 为什么容量必须是 2 的幂

HashMap 的容量（数组长度）始终是 2 的幂，例如 16、32、64。这样做的好处是：

- **取模运算优化**：`hash % n` 等价于 `hash & (n - 1)`，位运算比取模快得多
- **分布均匀**：2 的幂减 1 后，二进制全为 1（如 15 = 0b1111），与 hash 做 & 运算时，每一位都参与了桶的选择，分布更均匀

```java
// n = 16, n - 1 = 15 = 0b1111
// hash = 37 = 0b100101
// hash & (n - 1) = 0b0101 = 5 → 放入 5 号桶
```

## Map 的常用方法

```java
Map<String, Integer> map = new HashMap<>();

// 基本操作
map.put("Java", 1995);
map.put("Python", 1991);
int year = map.get("Java");             // 1995
int fallback = map.getOrDefault("Go", 0);  // 0（键不存在时返回默认值）

// 判断与遍历
boolean hasJava = map.containsKey("Java");
map.forEach((lang, age) -> 
    System.out.println(lang + " was created in " + age));

// 计算方法
map.putIfAbsent("Go", 2009);  // 仅当键不存在时放入
map.computeIfAbsent("Rust", k -> 2010);  // 键不存在时，通过函数计算值并放入

// merge：合并值
map.merge("Java", 1, Integer::sum);  // Java 的值变为 1996
```

`computeIfAbsent` 在实现缓存时非常有用——先查缓存，没有就计算并存入：

```java
Map<String, List<String>> cache = new HashMap<>();
cache.computeIfAbsent("user:1", k -> loadFromDB(k));
```

`merge` 适合做计数器：

```java
Map<String, Integer> wordCount = new HashMap<>();
words.forEach(word -> wordCount.merge(word, 1, Integer::sum));
```
