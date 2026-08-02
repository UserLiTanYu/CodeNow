# Collection 体系与 List

## Java 集合框架概览

在日常开发中，我们几乎离不开集合——存储一组用户、管理一批订单、缓存临时数据……Java 集合框架（Java Collections Framework, JCF）正是为了解决"如何高效地组织和操作一组对象"而设计的。

整个集合框架分为两大体系：

| 体系 | 根接口 | 特点 |
|------|--------|------|
| Collection | `java.util.Collection` | 存储单个元素的序列 |
| Map | `java.util.Map` | 存储键值对映射 |

两者没有继承关系，但在框架设计上遵循相同的理念：面向接口编程、提供多种实现、通过工具类简化操作。

## 接口层次结构

Collection 体系的接口层次如下：

```
Iterable<E>
  └── Collection<E>
        ├── List<E>       有序、可重复
        ├── Set<E>        不可重复
        └── Queue<E>      队列语义
              └── Deque<E> 双端队列
```

`Iterable` 是最顶层的接口，它只定义了一个方法 `iterator()`，正是这个接口让 `for-each` 循环成为可能。`Collection` 在其基础上增加了 `add()`、`remove()`、`size()` 等通用操作。

## List 接口

`List` 是最常用的集合类型，特点是**有序**（元素按插入顺序排列）且**允许重复**。Java 提供了三个主要实现。

### ArrayList

`ArrayList` 是日常开发中使用频率最高的 List 实现，底层基于**Object 数组**。

**核心特性：**

- **随机访问**：通过下标直接定位元素，时间复杂度 O(1)
- **插入/删除**：需要移动后续元素，时间复杂度 O(n)
- **扩容机制**：当元素数量超过当前容量时，创建一个 1.5 倍大小的新数组，将旧数组内容拷贝过去

```java
// 默认初始容量为 10
List<String> list = new ArrayList<>();

// 指定初始容量，避免频繁扩容
List<Order> orders = new ArrayList<>(1000);
```

扩容系数为什么是 1.5 而不是 2？这是一个空间与时间的折中——1.5 倍扩容在摊还分析下仍然是 O(1)，但比 2 倍更节省内存，且在 64 位 JVM 中更容易被内存分配器高效处理。

### LinkedList

`LinkedList` 基于**双向链表**实现，同时实现了 `List` 和 `Deque` 接口。

**核心特性：**

- **插入/删除**：只需修改指针，时间复杂度 O(1)（前提是已有节点引用）
- **随机访问**：需要从头或尾遍历到目标位置，时间复杂度 O(n)
- **额外开销**：每个节点需要存储前后指针，内存占用比 ArrayList 大

```java
// 作为 List 使用
List<String> linkedList = new LinkedList<>();

// 作为双端队列使用
Deque<String> deque = new LinkedList<>();
deque.addFirst("head");
deque.addLast("tail");
```

### Vector 与 Stack

`Vector` 是 JDK 1.0 就存在的遗留类，功能与 ArrayList 类似，但所有方法都用 `synchronized` 修饰，性能较差。`Stack` 继承自 Vector，提供了 LIFO 栈操作。

**不推荐使用**。如果需要栈语义，用 `ArrayDeque`；如果需要线程安全集合，用 `java.util.concurrent` 包中的类。

## ArrayList vs LinkedList

| 比较项 | ArrayList | LinkedList |
|--------|-----------|------------|
| 底层结构 | 动态数组 | 双向链表 |
| 随机访问 | O(1) | O(n) |
| 尾部添加 | 均摊 O(1) | O(1) |
| 中间插入/删除 | O(n) | O(1)（已定位时） |
| 内存占用 | 紧凑 | 每个元素额外 24 字节指针开销 |
| 缓存友好性 | 高（连续内存） | 低（节点分散） |

**选择指南：** 绝大多数场景优先选择 `ArrayList`。现代 CPU 的缓存机制对连续内存非常友好，即使频繁的中间插入，ArrayList 因为内存紧凑，实际性能往往也优于 LinkedList。只有在确实需要大量头部插入/删除，且已持有节点引用时，LinkedList 才有优势。

## List 的常用方法

```java
List<String> list = new ArrayList<>();

// 添加元素
list.add("Java");
list.add(0, "Python");  // 在指定位置插入

// 访问元素
String first = list.get(0);

// 修改元素
list.set(0, "Kotlin");

// 删除元素
list.remove(0);          // 按索引删除
list.remove("Java");     // 按对象删除

// 查找
int index = list.indexOf("Kotlin");    // 首次出现的位置
boolean has = list.contains("Java");   // 是否包含

// 子列表（视图，非拷贝）
List<String> sub = list.subList(0, 2);

// 排序
list.sort(Comparator.naturalOrder());
```

`subList()` 返回的是原列表的一个**视图**，对子列表的修改会反映到原列表上。需要注意：在子列表存在期间，如果结构性地修改原列表（添加或删除元素），会抛出 `ConcurrentModificationException`。

## 不可变 List

Java 9 引入了工厂方法，可以方便地创建不可变列表：

```java
// Java 9+
List<String> languages = List.of("Java", "Python", "Go");

// 会抛出 UnsupportedOperationException
languages.add("Rust");
```

在 Java 9 之前，通常使用 `Collections.unmodifiableList()`：

```java
List<String> mutable = new ArrayList<>();
mutable.add("Java");
List<String> immutable = Collections.unmodifiableList(mutable);
```

两者的区别在于：`List.of()` 创建的是真正的不可变对象，内部不持有可变引用；而 `unmodifiableList()` 只是包装了一个视图，如果原列表被修改，不可变视图也会跟着变。

## 迭代器模式

`Iterator` 是集合框架中遍历元素的标准方式，`for-each` 循环本质上就是迭代器的语法糖。

```java
// 手动使用迭代器
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String item = it.next();
    if (item.equals("Java")) {
        it.remove();  // 安全删除
    }
}

// 等价的 for-each 写法
for (String item : list) {
    // 注意：for-each 循环中不能安全地删除元素
    // 必须用迭代器的 remove() 或 break 后再删除
}
```

**fail-fast 机制**：当迭代器被创建后，如果通过非迭代器的方式（如直接调用 `list.remove()`）修改集合的结构，迭代器会检测到并发修改并立即抛出 `ConcurrentModificationException`。这是通过一个内部的修改计数器 `modCount` 实现的——每次结构性修改都会递增 `modCount`，迭代器在 `next()` 和 `hasNext()` 中会检查该值是否与预期一致。

## Iterable 接口与 for-each

任何实现了 `Iterable` 接口的类都可以使用 `for-each` 循环：

```java
public class NumberRange implements Iterable<Integer> {
    private final int start;
    private final int end;

    public NumberRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            private int current = start;

            @Override
            public boolean hasNext() {
                return current < end;
            }

            @Override
            public Integer next() {
                return current++;
            }
        };
    }
}

// 使用 for-each 遍历自定义可迭代对象
for (int n : new NumberRange(1, 10)) {
    System.out.println(n);
}
```

`for-each` 循环在编译时会被转换为对 `iterator()`、`hasNext()` 和 `next()` 的调用，所以理解迭代器模式有助于我们编写自定义的可迭代类型，以及在需要安全删除元素时正确使用迭代器。
