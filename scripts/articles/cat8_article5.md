# TreeSet、TreeMap 与排序集合

在实际开发中，我们经常需要对集合中的元素进行排序或按特定顺序遍历。Java 集合框架提供了 `TreeSet` 和 `TreeMap` 两个基于红黑树实现的有序集合，它们能够在插入时自动维护元素的顺序，并提供丰富的导航方法。

## 排序集合的需求

无序的 `HashSet` 和 `HashMap` 虽然提供了 O(1) 的查找性能，但无法满足以下需求：

- **有序遍历**：按字母顺序、数字大小或自定义规则遍历元素
- **范围查询**：获取某个区间内的所有元素
- **最近邻查找**：找到小于或大于某个值的最近元素
- **极值获取**：快速获取最大值或最小值

`TreeSet` 和 `TreeMap` 基于红黑树实现，能够在 O(log n) 时间内完成上述操作。

## 红黑树概述

红黑树是一种自平衡的二叉搜索树，它通过节点着色和旋转规则来保持树的平衡。

### 红黑树的性质

1. 每个节点是红色或黑色
2. 根节点是黑色
3. 每个叶节点（NIL）是黑色
4. 如果一个节点是红色，则它的两个子节点都是黑色
5. 从任一节点到其每个叶节点的路径上，黑色节点数量相同

```
        [B] 8
       /     \
    [B] 4    [B] 12
    /   \    /   \
 [R]2  [R]6 [R]10 [R]14
```

### 红黑树的性能

| 操作 | 平均时间 | 最坏时间 |
|------|----------|----------|
| 查找 | O(log n) | O(log n) |
| 插入 | O(log n) | O(log n) |
| 删除 | O(log n) | O(log n) |
| 遍历 | O(n) | O(n) |

红黑树保证了最坏情况下的 O(log n) 性能，这是它相比普通二叉搜索树的优势。

## TreeSet

`TreeSet` 基于 `TreeMap` 实现，元素存储在 `TreeMap` 的键中，值使用一个固定的虚拟对象。

### 基本用法

```java
// 自然排序
TreeSet<Integer> numbers = new TreeSet<>();
numbers.add(3);
numbers.add(1);
numbers.add(4);
numbers.add(1);
numbers.add(5);

System.out.println(numbers); // [1, 3, 4, 5]（有序且去重）

// 字符串自然排序（字典序）
TreeSet<String> words = new TreeSet<>();
words.add("banana");
words.add("apple");
words.add("cherry");

System.out.println(words); // [apple, banana, cherry]
```

### 常用方法

```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));

// 获取首尾元素
set.first(); // 1
set.last();  // 9

// 子集操作
set.headSet(5);    // [1, 3]（小于 5 的元素）
set.tailSet(5);    // [5, 7, 9]（大于等于 5 的元素）
set.subSet(3, 8);  // [3, 5, 7]（>= 3 且 < 8）

// 导航方法（Java 6+）
set.ceiling(4);  // 5（大于等于 4 的最小元素）
set.floor(4);    // 3（小于等于 4 的最大元素）
set.higher(4);   // 5（大于 4 的最小元素）
set.lower(4);    // 3（小于 4 的最大元素）

// 获取并移除首尾元素
set.pollFirst(); // 1
set.pollLast();  // 9
```

### 不允许 null

`TreeSet`（以及 `TreeMap`）不允许插入 `null` 元素，因为需要调用 `compareTo()` 或 `compare()` 方法进行比较：

```java
TreeSet<String> set = new TreeSet<>();
set.add(null); // 抛出 NullPointerException
```

这是 `NavigableSet` 接口的规范要求，与 `HashSet` 不同。

## TreeMap

`TreeMap` 是按键有序的 `Map` 实现，基于红黑树。

### 基本用法

```java
TreeMap<String, Integer> scores = new TreeMap<>();
scores.put("Alice", 95);
scores.put("Bob", 87);
scores.put("Charlie", 92);
scores.put("David", 78);

// 遍历时按键排序
for (Map.Entry<String, Integer> entry : scores.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
// Alice: 95
// Bob: 87
// Charlie: 92
// David: 78
```

### 常用方法

```java
TreeMap<Integer, String> map = new TreeMap<>();
map.put(1, "one");
map.put(3, "three");
map.put(5, "five");
map.put(7, "seven");
map.put(9, "nine");

// 获取首尾键
map.firstKey(); // 1
map.lastKey();  // 9

// 获取首尾Entry
map.firstEntry(); // 1=one
map.lastEntry();  // 9=nine

// 子 Map 操作
map.headMap(5);       // {1=one, 3=three}（键 < 5）
map.tailMap(5);       // {5=five, 7=seven, 9=nine}（键 >= 5）
map.subMap(3, 8);     // {3=three, 5=five, 7=seven}（>= 3 且 < 8）

// 导航方法
map.ceilingKey(4);    // 5（大于等于 4 的最小键）
map.floorKey(4);      // 3（小于等于 4 的最大键）
map.higherKey(4);     // 5（大于 4 的最小键）
map.lowerKey(4);      // 3（小于 4 的最大键）

// 获取并移除首尾
map.pollFirstEntry(); // 1=one
map.pollLastEntry();  // 9=nine
```

### NavigableMap 接口

`TreeMap` 实现了 `NavigableMap` 接口，提供了丰富的导航方法：

```java
public interface NavigableMap<K,V> extends SortedMap<K,V> {
    // 小于给定键的最大键值对
    Map.Entry<K,V> lowerEntry(K key);
    K lowerKey(K key);
    
    // 小于等于给定键的最大键值对
    Map.Entry<K,V> floorEntry(K key);
    K floorKey(K key);
    
    // 大于等于给定键的最小键值对
    Map.Entry<K,V> ceilingEntry(K key);
    K ceilingKey(K key);
    
    // 大于给定键的最小键值对
    Map.Entry<K,V> higherEntry(K key);
    K higherKey(K key);
    
    // 最小/最大键值对
    Map.Entry<K,V> firstEntry();
    Map.Entry<K,V> lastEntry();
    
    // 获取并移除
    Map.Entry<K,V> pollFirstEntry();
    Map.Entry<K,V> pollLastEntry();
    
    // 逆序视图
    NavigableMap<K,V> descendingMap();
    NavigableSet<K> descendingKeySet();
    
    // 子集视图（更精确控制）
    NavigableMap<K,V> headMap(K toKey, boolean inclusive);
    NavigableMap<K,V> tailMap(K fromKey, boolean inclusive);
    NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);
}
```

## Comparable 接口

`Comparable` 接口定义了对象的**自然排序**（natural ordering）。

### 接口定义

```java
public interface Comparable<T> {
    int compareTo(T other);
}
```

### compareTo() 的契约

实现 `compareTo()` 必须遵守以下契约：

1. **自反性**：`x.compareTo(x) == 0`
2. **对称性**：如果 `x.compareTo(y) > 0`，则 `y.compareTo(x) < 0`
3. **传递性**：如果 `x.compareTo(y) > 0` 且 `y.compareTo(z) > 0`，则 `x.compareTo(z) > 0`
4. **与 equals 一致性**：如果 `x.compareTo(y) == 0`，则 `x.equals(y)` 应该返回 `true`（建议但不强制）

### 自然排序的实现

Java 标准库中的许多类已经实现了 `Comparable`：

| 类型 | 排序方式 |
|------|----------|
| String | 字典序 |
| Integer, Long, Double | 数值大小 |
| LocalDate, LocalDateTime | 时间先后 |
| Character | Unicode 值 |

### 自定义类实现 Comparable

```java
public class Student implements Comparable<Student> {
    private String name;
    private int age;
    private double gpa;
    
    // 先按 GPA 降序，再按姓名升序
    @Override
    public int compareTo(Student other) {
        int result = Double.compare(other.gpa, this.gpa); // 降序
        if (result == 0) {
            result = this.name.compareTo(other.name); // 升序
        }
        return result;
    }
    
    // getters, setters, toString...
}
```

使用示例：

```java
TreeSet<Student> students = new TreeSet<>();
students.add(new Student("Alice", 20, 3.8));
students.add(new Student("Bob", 21, 3.9));
students.add(new Student("Charlie", 19, 3.8));

// 按 GPA 降序排列
for (Student s : students) {
    System.out.println(s);
}
```

**实现 `compareTo` 的技巧：**

```java
// 对于基本类型，使用包装类的 compare 方法
int result = Integer.compare(this.age, other.age);

// 对于可能为 null 的字段
int result = Comparator.nullsFirst(Comparator.naturalOrder())
                      .compare(this.name, other.name);

// 链式比较（推荐使用 Comparator）
int result = Comparator.comparing(Student::getGpa, Comparator.reverseOrder())
                       .thenComparing(Student::getName)
                       .compare(this, other);
```

## Comparator 接口

`Comparator` 接口提供了**定制排序**（custom ordering），无需修改类本身。

### 接口定义

```java
public interface Comparator<T> {
    int compare(T o1, T o2);
    
    // Java 8+ 默认方法
    default Comparator<T> thenComparing(Comparator<? super T> other) { ... }
    default <U> Comparator<T> thenComparing(Function<? super T, ? extends U> keyExtractor,
                                            Comparator<? super U> keyComparator) { ... }
    // ...
}
```

### 基本用法

```java
// 按年龄排序
Comparator<Student> byAge = Comparator.comparingInt(Student::getAge);

// 按姓名排序
Comparator<Student> byName = Comparator.comparing(Student::getName);

// 按 GPA 降序
Comparator<Student> byGpaDesc = Comparator.comparingDouble(Student::getGpa).reversed();

// 使用
TreeSet<Student> students = new TreeSet<>(byAge);
// 或
students.sort(byGpaDesc);
```

### Comparator 工厂方法（Java 8+）

```java
// 自然排序
Comparator<String> natural = Comparator.naturalOrder();

// 逆序
Comparator<String> reverse = Comparator.reverseOrder();

// null 处理
Comparator<String> nullFirst = Comparator.nullsFirst(Comparator.naturalOrder());
Comparator<String> nullLast = Comparator.nullsLast(Comparator.naturalOrder());
```

### 链式比较器

```java
// 先按部门排序，再按薪资降序，最后按姓名排序
Comparator<Employee> comparator = 
    Comparator.comparing(Employee::getDepartment)
              .thenComparing(Employee::getSalary, Comparator.reverseOrder())
              .thenComparing(Employee::getName);

employees.sort(comparator);
```

### 自定义 Comparator

```java
// 按字符串长度排序
Comparator<String> byLength = new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return Integer.compare(s1.length(), s2.length());
    }
};

// Java 8+ Lambda 写法
Comparator<String> byLength = (s1, s2) -> Integer.compare(s1.length(), s2.length());

// 方法引用写法
Comparator<String> byLength = Comparator.comparingInt(String::length);
```

## SortedSet/SortedMap 接口

`SortedSet` 和 `SortedMap` 是排序集合的基础接口，`TreeSet` 和 `TreeMap` 都实现了它们的子接口 `NavigableSet` 和 `NavigableMap`。

### SortedSet 接口

```java
public interface SortedSet<E> extends Set<E> {
    Comparator<? super E> comparator(); // 获取比较器，null 表示自然排序
    SortedSet<E> subSet(E fromElement, E toElement);
    SortedSet<E> headSet(E toElement);
    SortedSet<E> tailSet(E fromElement);
    E first();
    E last();
}
```

### 子集视图

子集视图是**活的**（live），对子集的修改会反映到原始集合：

```java
TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));

SortedSet<Integer> sub = set.subSet(3, 8); // [3, 4, 5, 6, 7]

// 修改子集会影响原集合
sub.remove(5);
System.out.println(set); // [1, 2, 3, 4, 6, 7, 8, 9]

// 向子集添加元素（必须在范围内）
sub.add(5);
// sub.add(10); // IllegalArgumentException：超出范围
```

## 实战场景

### 排行榜：TreeMap 实现 Top N

使用 `TreeMap` 维护一个排行榜，自动按分数排序：

```java
public class Leaderboard {
    // 分数 -> 玩家列表（同分可能多人）
    private final TreeMap<Integer, List<String>> scoreMap = new TreeMap<>(Comparator.reverseOrder());
    private final Map<String, Integer> playerScores = new HashMap<>();
    
    public void addScore(String player, int score) {
        // 移除旧分数
        Integer oldScore = playerScores.get(player);
        if (oldScore != null) {
            List<String> players = scoreMap.get(oldScore);
            players.remove(player);
            if (players.isEmpty()) {
                scoreMap.remove(oldScore);
            }
        }
        
        // 添加新分数
        playerScores.put(player, score);
        scoreMap.computeIfAbsent(score, k -> new ArrayList<>()).add(player);
    }
    
    public List<String> getTopN(int n) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : scoreMap.entrySet()) {
            for (String player : entry.getValue()) {
                result.add(player);
                if (result.size() >= n) return result;
            }
        }
        return result;
    }
    
    public int getRank(String player) {
        Integer score = playerScores.get(player);
        if (score == null) return -1;
        
        int rank = 1;
        for (Map.Entry<Integer, List<String>> entry : scoreMap.entrySet()) {
            if (entry.getKey() == score) {
                return rank + entry.getValue().indexOf(player);
            }
            rank += entry.getValue().size();
        }
        return -1;
    }
}
```

### 时间线：TreeMap 按日期排序

```java
public class Timeline {
    private final TreeMap<LocalDate, List<String>> events = new TreeMap<>();
    
    public void addEvent(LocalDate date, String event) {
        events.computeIfAbsent(date, k -> new ArrayList<>()).add(event);
    }
    
    // 获取某一天的事件
    public List<String> getEvents(LocalDate date) {
        return events.getOrDefault(date, Collections.emptyList());
    }
    
    // 获取日期范围内的事件
    public Map<LocalDate, List<String>> getEventsInRange(LocalDate from, LocalDate to) {
        return events.subMap(from, true, to, true);
    }
    
    // 获取最近的事件
    public Map.Entry<LocalDate, List<String>> getNearestEvent(LocalDate date) {
        Map.Entry<LocalDate, List<String>> floor = events.floorEntry(date);
        Map.Entry<LocalDate, List<String>> ceiling = events.ceilingEntry(date);
        
        if (floor == null) return ceiling;
        if (ceiling == null) return floor;
        
        long floorDiff = ChronoUnit.DAYS.between(floor.getKey(), date);
        long ceilingDiff = ChronoUnit.DAYS.between(date, ceiling.getKey());
        
        return floorDiff <= ceilingDiff ? floor : ceiling;
    }
    
    // 获取今天的事件
    public List<String> getTodayEvents() {
        return getEvents(LocalDate.now());
    }
    
    // 获取本周的事件
    public Map<LocalDate, List<String>> getThisWeekEvents() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        return getEventsInRange(weekStart, weekEnd);
    }
}
```

### 使用示例

```java
Timeline timeline = new Timeline();
timeline.addEvent(LocalDate.of(2024, 1, 1), "新年");
timeline.addEvent(LocalDate.of(2024, 2, 10), "春节");
timeline.addEvent(LocalDate.of(2024, 5, 1), "劳动节");
timeline.addEvent(LocalDate.of(2024, 10, 1), "国庆节");

// 查找距离今天最近的节日
Map.Entry<LocalDate, List<String>> nearest = 
    timeline.getNearestEvent(LocalDate.now());
System.out.println("最近的节日: " + nearest);

// 获取某个范围内的事件
Map<LocalDate, List<String>> range = 
    timeline.getEventsInRange(
        LocalDate.of(2024, 1, 1), 
        LocalDate.of(2024, 6, 30)
    );
```

`TreeSet` 和 `TreeMap` 是处理有序数据的强大工具。在需要排序、范围查询或导航操作的场景下，它们比 `HashSet` 和 `HashMap` 更合适。选择使用 `Comparable` 还是 `Comparator` 取决于排序逻辑是否与类本身相关：如果排序是类的本质属性（如按日期排序的时间戳），实现 `Comparable`；如果排序是外部需求（如按不同字段排序），使用 `Comparator`。
