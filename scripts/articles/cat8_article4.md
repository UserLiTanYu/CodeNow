# HashMap 源码分析与哈希冲突

HashMap 是 Java 集合框架中最常用的容器之一，它提供了基于键值对的高效存取操作。理解 HashMap 的内部实现原理，特别是哈希冲突的处理机制，对于编写高效、正确的 Java 程序至关重要。本文将深入分析 JDK 8 中 HashMap 的源码实现。

## HashMap 的内部结构

JDK 8 中 HashMap 采用**数组 + 链表 + 红黑树**的复合结构：

```
┌─────────────────────────────────────────────────────┐
│                    HashMap 内部结构                    │
├─────────────────────────────────────────────────────┤
│                                                     │
│  [0] ──→ null                                       │
│  [1] ──→ Entry ──→ Entry ──→ null                   │
│  [2] ──→ null                                       │
│  [3] ──→ Entry ──→ Entry ──→ Entry ──→ ... (链表)   │
│  [4] ──→ TreeNode ──→ TreeNode (红黑树)              │
│  [5] ──→ null                                       │
│  ...                                                │
│  [n] ──→ Entry ──→ null                             │
│                                                     │
└─────────────────────────────────────────────────────┘
```

- **数组（桶数组）**：`Node<K,V>[] table`，每个位置称为一个"桶"（bucket）
- **链表**：当多个键映射到同一个桶时，使用链表存储
- **红黑树**：当链表长度超过阈值时，转换为红黑树以提高查找效率

Node 节点的定义：

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;    // 键的哈希值
    final K key;       // 键
    V value;           // 值
    Node<K,V> next;    // 下一个节点（链表结构）
}
```

## 重要参数

```java
// 默认初始容量：16，必须是 2 的幂
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

// 最大容量：2^30
static final int MAXIMUM_CAPACITY = 1 << 30;

// 默认负载因子：0.75
static final float DEFAULT_LOAD_FACTOR = 0.75f;

// 链表转红黑树的阈值：当链表长度 >= 8 且数组长度 >= 64 时树化
static final int TREEIFY_THRESHOLD = 8;

// 红黑树退化为链表的阈值：当节点数 <= 6 时退化
static final int UNTREEIFY_THRESHOLD = 6;

// 最小树化容量：数组长度必须 >= 64 才会树化
static final int MIN_TREEIFY_CAPACITY = 64;
```

**为什么选择 0.75 作为默认负载因子？**

这是时间和空间的折中：
- 负载因子越小，空间浪费越多，但哈希冲突概率低，查找快
- 负载因子越大，空间利用率高，但哈希冲突概率高，查找慢
- 0.75 在大多数场景下能提供较好的平衡

**为什么树化阈值是 8？**

根据泊松分布，在负载因子为 0.75 的情况下，一个桶中链表长度达到 8 的概率约为 0.00000006，非常罕见。这保证了正常使用时红黑树几乎不会被触发，只有在哈希冲突严重（如恶意攻击）时才会启用。

## hash() 方法：扰动函数

HashMap 的 `hash()` 方法是整个哈希表的核心，它决定了键值对在数组中的分布。

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

**原理：** 将 hashCode 的高 16 位与低 16 位进行异或运算。

**为什么需要这个操作？**

数组定位使用 `hash & (n-1)`，当数组长度较小时（如 16），只有低 4 位参与运算，高位信息完全丢失。通过将高位信息混入低位，可以显著减少哈希冲突：

```
原始 hashCode:  1111 1111 1111 1111 0000 0000 0000 0101
                                    ↑ 高16位    ↑ 低16位
                                    
h >>> 16:       0000 0000 0000 0000 1111 1111 1111 1111

h ^ (h >>> 16): 1111 1111 1111 1111 1111 1111 1111 1010
                                    ↑ 混合后      ↑ 低位包含了高位信息
```

## put 流程详解

`put` 方法是 HashMap 最核心的操作，完整流程如下：

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}
```

### putVal 源码分析

```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    
    // 1. 如果 table 为空或长度为 0，先进行扩容
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    
    // 2. 计算桶位置：(n-1) & hash
    //    如果桶为空，直接创建新节点
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        
        // 3. 桶不为空，检查第一个节点
        if (p.hash == hash && 
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p; // key 相同，后面会覆盖 value
            
        // 4. 如果是红黑树节点，调用红黑树的插入方法
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            
        // 5. 链表遍历
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    // 链表长度 >= 8，尝试树化
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash && 
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break; // 找到相同 key
                p = e;
            }
        }
        
        // 6. 如果 key 已存在，覆盖旧值
        if (e != null) {
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    
    ++modCount;
    
    // 7. 超过阈值，扩容
    if (++size > threshold)
        resize();
    afterNodeInsertion(evict);
    return null;
}
```

### put 流程图解

```
                    ┌─────────────────┐
                    │   put(key, val) │
                    └────────┬────────┘
                             ▼
                    ┌─────────────────┐
                    │ 计算 hash(key)  │
                    └────────┬────────┘
                             ▼
                    ┌─────────────────┐
                    │ table 是否为空？ │
                    └────────┬────────┘
                        是 ↙     ↘ 否
                    ┌───────┐   ┌──────────────────┐
                    │resize │   │ 定位桶 (n-1)&hash│
                    └───────┘   └────────┬─────────┘
                                        ▼
                              ┌─────────────────────┐
                              │  桶是否为空？        │
                              └────────┬────────────┘
                                  是 ↙     ↘ 否
                            ┌─────────┐   ┌──────────────────┐
                            │直接插入 │   │ 检查第一个节点   │
                            └─────────┘   └────────┬─────────┘
                                                   ▼
                                        ┌─────────────────────┐
                                        │ key 是否相同？      │
                                        └────────┬────────────┘
                                            是 ↙     ↘ 否
                                      ┌───────┐   ┌───────────────┐
                                      │覆盖   │   │ 是红黑树？    │
                                      └───────┘   └───────┬───────┘
                                                     是 ↙     ↘ 否
                                              ┌──────────┐  ┌──────────────┐
                                              │树插入    │  │ 链表遍历     │
                                              └──────────┘  └──────┬───────┘
                                                                   ▼
                                                        ┌──────────────────────┐
                                                        │ 链表>=8 且数组>=64？ │
                                                        └──────────┬───────────┘
                                                              是 ↙     ↘ 否
                                                       ┌──────────┐ ┌──────────┐
                                                       │ 树化     │ │ 继续链表 │
                                                       └──────────┘ └──────────┘
```

## get 流程

```java
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    
    // 1. 定位桶
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        
        // 2. 检查第一个节点
        if (first.hash == hash && 
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
            
        // 3. 遍历链表或红黑树
        if ((e = first.next) != null) {
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            do {
                if (e.hash == hash && 
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

**时间复杂度：**
- 无冲突：O(1)
- 链表：O(n)，n 为链表长度
- 红黑树：O(log n)

## 扩容机制

### 触发条件

当 `size > capacity * loadFactor` 时触发扩容。

### resize() 源码核心逻辑

```java
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    
    if (oldCap > 0) {
        // 已达到最大容量，不再扩容
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        // 新容量 = 旧容量 * 2
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                 oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1; // 阈值也翻倍
    }
    // ... 其他情况处理
    
    threshold = newThr;
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    
    // 重新分配元素
    if (oldTab != null) {
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null; // 帮助 GC
                
                if (e.next == null)
                    // 只有一个节点，直接放到新位置
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)
                    // 红黑树节点，拆分红黑树
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else {
                    // 链表拆分：分成两个链表
                    Node<K,V> loHead = null, loTail = null; // 原位链表
                    Node<K,V> hiHead = null, hiTail = null; // 新位链表
                    Node<K,V> next;
                    do {
                        next = e.next;
                        // 关键判断：hash & oldCap
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        } else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead; // 原位
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead; // 原位 + 旧容量
                    }
                }
            }
        }
    }
    return newTab;
}
```

### Java 8 的扩容优化

Java 8 使用 `hash & oldCap` 判断元素在新数组中的位置，这个技巧非常巧妙：

假设旧容量为 16（`10000`），新容量为 32（`100000`）：

```
元素 hash:     ...x xxxx (二进制)
旧位置:        hash & 01111 = ?xxx
新位置:        hash & 11111 = x?xxx

关键：第 5 位（oldCap 对应的位）是 0 还是 1？
- 如果是 0：新位置 = 旧位置
- 如果是 1：新位置 = 旧位置 + oldCap
```

`hash & oldCap` 恰好检查的就是这一位，结果非 0 则元素移到新位置，为 0 则留在原位。这比重新计算 `hash & (newCap - 1)` 更高效。

## 为什么容量必须是 2 的幂

HashMap 的容量始终是 2 的幂（16, 32, 64...），这是因为：

**桶定位公式：** `index = hash & (n - 1)`

当 `n` 是 2 的幂时，`n - 1` 的二进制全是 1：

```
n = 16:    10000
n - 1:     01111  (低 4 位全为 1)

hash & 01111 = 取 hash 的低 4 位作为索引
```

这等价于 `hash % n`（取模运算），但位运算比取模快得多。

如果容量不是 2 的幂，`n - 1` 的二进制会有 0 位，导致某些桶永远不会被使用：

```
n = 10:    1010
n - 1:     1001  (第 2 位是 0)

hash & 1001 → 第 2 位永远是 0 → 某些桶无法映射到
```

## 线程不安全

HashMap 不是线程安全的，并发使用会导致以下问题：

### 数据丢失

两个线程同时 `put` 到同一个桶，可能导致一个覆盖另一个：

```java
// 线程 A 和线程 B 同时执行
map.put(keyA, valueA); // 线程 A
map.put(keyB, valueB); // 线程 B
// 可能丢失其中一个
```

### 死循环（Java 7）

Java 7 的 HashMap 在扩容时使用**头插法**，在并发场景下可能导致链表成环：

```
线程 1 扩容中：A -> B -> null
线程 2 扩容中：B -> A -> null (头插法反转)

最终可能形成：A -> B -> A (环形链表)
get() 时陷入死循环
```

Java 8 改用**尾插法**解决了环形链表问题，但并发下仍可能丢数据。

### fail-fast 机制

HashMap 维护 `modCount` 计数器，迭代时如果发现 `modCount` 变化，抛出 `ConcurrentModificationException`：

```java
for (Map.Entry<K, V> entry : map.entrySet()) {
    if (someCondition) {
        map.remove(entry.getKey()); // 抛出 ConcurrentModificationException
    }
}
```

正确做法：

```java
Iterator<Map.Entry<K, V>> it = map.entrySet().iterator();
while (it.hasNext()) {
    Map.Entry<K, V> entry = it.next();
    if (someCondition) {
        it.remove(); // 使用迭代器的 remove
    }
}
```

## HashMap vs Hashtable vs ConcurrentHashMap

| 特性 | HashMap | Hashtable | ConcurrentHashMap |
|------|---------|-----------|-------------------|
| 线程安全 | 否 | 是（全表锁） | 是（分段锁/CAS） |
| null 键/值 | 允许 1 个 null 键，多个 null 值 | 不允许 | 不允许 |
| 性能 | 最高（单线程） | 差 | 高（并发） |
| 迭代器 | fail-fast | fail-fast | 弱一致性 |
| 推荐使用 | 单线程 | 不推荐 | 多线程 |

**使用建议：**
- 单线程环境：`HashMap`
- 多线程环境：`ConcurrentHashMap`
- 不推荐使用 `Hashtable`（遗留类，性能差）

## 哈希冲突的处理策略

HashMap 使用**链地址法**（Separate Chaining）处理哈希冲突：

```java
// 哈希冲突示例
map.put("Aa", 1); // "Aa".hashCode() = 2112
map.put("BB", 2); // "BB".hashCode() = 2112

// 两个键映射到同一个桶，形成链表
// 桶[x] -> Node("Aa",1) -> Node("BB",2) -> null
```

### 影响哈希冲突的因素

1. **hashCode() 的实现**：好的 hashCode 应该均匀分布
2. **容量大小**：容量越大，冲突概率越低
3. **负载因子**：负载因子越小，冲突概率越低

### 自定义类的 hashCode 实现

```java
public class User {
    private String name;
    private int age;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return age == user.age && Objects.equals(name, user.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, age); // 推荐使用 Objects.hash
    }
}
```

**equals 和 hashCode 的契约：**
- 相等的对象必须有相同的 hashCode
- 相同 hashCode 的对象不一定相等
- 重写 equals 必须同时重写 hashCode

理解 HashMap 的内部实现，有助于在实际开发中正确使用它，避免并发问题和性能陷阱。在需要线程安全的场景下，始终选择 `ConcurrentHashMap` 而非 `Hashtable`。
