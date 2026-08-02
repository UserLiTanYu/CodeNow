# 通配符详解与 PECS 原则

泛型通配符是 Java 泛型系统中最灵活也最难理解的部分。掌握通配符的使用规则和 PECS 原则，对于编写灵活、类型安全的泛型代码至关重要。本文将深入探讨通配符的各种用法。

## 通配符回顾

通配符 `?` 表示未知类型，用于增加泛型的灵活性：

```java
// 无界通配符
List<?> list = new ArrayList<String>();

// 上界通配符
List<? extends Number> numbers = new ArrayList<Integer>();

// 下界通配符
List<? super Integer> integers = new ArrayList<Number>();
```

## 上界通配符 ? extends T

上界通配符表示类型是 T 或 T 的某个子类型。

### 语义与使用场景

```java
// 声明：只能读取，不能写入
List<? extends Number> numbers = new ArrayList<Integer>();

// 读取：可以安全地读取为 Number 类型
Number num = numbers.get(0);  // OK

// 写入：编译器不允许
// numbers.add(1);           // 编译错误
// numbers.add(1.0);         // 编译错误
// numbers.add((Number) 1);  // 编译错误
```

### 为什么不能写入

```java
List<? extends Number> numbers = new ArrayList<Integer>();
// numbers 实际指向 List<Integer>

numbers.add(1.0);  // 如果允许，会破坏类型安全
// 因为 numbers 可能是 List<Integer>，添加 Double 会导致类型不安全
```

### 实际应用示例

```java
public class ExtendsWildcardDemo {
    // 只读方法：计算总和
    public static double sum(List<? extends Number> numbers) {
        double total = 0;
        for (Number num : numbers) {
            total += num.doubleValue();
        }
        return total;
    }

    // 只读方法：获取最大值
    public static <T extends Comparable<T>> T max(List<? extends T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("列表为空");
        }
        T max = list.get(0);
        for (T item : list) {
            if (item.compareTo(max) > 0) {
                max = item;
            }
        }
        return max;
    }

    public static void main(String[] args) {
        List<Integer> ints = Arrays.asList(1, 2, 3);
        List<Double> doubles = Arrays.asList(1.1, 2.2, 3.3);
        List<Number> numbers = Arrays.asList(1, 2.2, 3L);

        // 都可以作为参数传入
        System.out.println(sum(ints));     // 6.0
        System.out.println(sum(doubles));  // 6.6
        System.out.println(sum(numbers)); // 6.2

        // max 方法同样适用
        System.out.println(max(ints));    // 3
        System.out.println(max(doubles)); // 3.3
    }
}
```

## 下界通配符 ? super T

下界通配符表示类型是 T 或 T 的某个父类型。

### 语义与使用场景

```java
// 声明：可以写入 T 及其子类型，读取只能得到 Object
List<? super Integer> integers = new ArrayList<Number>();

// 写入：可以安全地写入 Integer 及其子类型
integers.add(1);           // OK
integers.add(2);           // OK
// integers.add(1.0);      // 编译错误，Double 不是 Integer 的子类型

// 读取：只能得到 Object
Object obj = integers.get(0);  // OK
// Integer num = integers.get(0);  // 编译错误
```

### 实际应用示例

```java
public class SuperWildcardDemo {
    // 写入方法：添加多个元素
    public static <T> void addAll(List<? super T> list, T... elements) {
        for (T element : elements) {
            list.add(element);
        }
    }

    // 写入方法：填充默认值
    public static <T> void fill(List<? super T> list, T value, int count) {
        for (int i = 0; i < count; i++) {
            list.add(value);
        }
    }

    public static void main(String[] args) {
        List<Number> numbers = new ArrayList<>();
        List<Object> objects = new ArrayList<>();

        // Integer 可以添加到 Number 或 Object 列表
        addAll(numbers, 1, 2, 3);
        addAll(objects, 1, 2, 3);

        System.out.println(numbers); // [1, 2, 3]
        System.out.println(objects); // [1, 2, 3]

        // fill 方法
        List<Number> filled = new ArrayList<>();
        fill(filled, 0, 5);
        System.out.println(filled); // [0, 0, 0, 0, 0]
    }
}
```

## 无界通配符 ?

无界通配符 `?` 表示完全未知的类型。

### 与 Object 的区别

```java
// List<?> 和 List<Object> 的区别
List<?> unknownList = new ArrayList<String>();  // OK
// List<Object> objectList = new ArrayList<String>();  // 编译错误

// List<?> 可以接受任何类型的 List
List<?> list1 = new ArrayList<Integer>();
List<?> list2 = new ArrayList<String>();
List<?> list3 = new ArrayList<User>();

// List<Object> 只能赋值给 List<Object>
// List<Object> list4 = new ArrayList<Integer>();  // 编译错误
```

### 使用场景

```java
public class UnboundedWildcardDemo {
    // 只调用与类型参数无关的方法
    public static void printList(List<?> list) {
        for (Object item : list) {
            System.out.println(item);
        }
    }

    // 获取列表大小
    public static int size(List<?> list) {
        return list.size();
    }

    // 判断是否为空
    public static boolean isEmpty(List<?> list) {
        return list.isEmpty();
    }

    // 清空列表
    public static void clear(List<?> list) {
        list.clear();  // 可以调用不依赖类型参数的方法
    }

    // 复制列表（使用 Collections.copy 的简化版）
    public static void copy(List<? super Object> dest, List<?> src) {
        dest.clear();
        for (Object item : src) {
            dest.add(item);
        }
    }

    public static void main(String[] args) {
        List<Integer> ints = Arrays.asList(1, 2, 3);
        List<String> strings = Arrays.asList("a", "b", "c");

        printList(ints);
        printList(strings);

        System.out.println(size(ints));     // 3
        System.out.println(isEmpty(strings)); // false
    }
}
```

## PECS 原则详解

PECS 是 "Producer Extends, Consumer Super" 的缩写，是使用通配符的核心原则：

- **Producer Extends**：如果集合是数据的生产者（从中读取），使用 `? extends T`
- **Consumer Super**：如果集合是数据的消费者（向其写入），使用 `? super T`

```java
public class PECSPrinciple {
    // Producer Extends：从集合中读取
    public static <T> void copy(
            List<? extends T> src,    // src 是生产者，从中读取
            List<? super T> dest) {   // dest 是消费者，向其写入
        for (T item : src) {
            dest.add(item);
        }
    }

    // 示例：灵活的 API 设计
    public static <T> void process(
            List<? extends T> input,   // 输入：生产者
            List<? super T> output,    // 输出：消费者
            Function<T, T> transformer) {
        for (T item : input) {
            output.add(transformer.apply(item));
        }
    }

    public static void main(String[] args) {
        List<Integer> src = Arrays.asList(1, 2, 3);
        List<Number> dest = new ArrayList<>();

        // Integer 是 Number 的子类型，符合 PECS
        copy(src, dest);
        System.out.println(dest); // [1, 2, 3]

        // 也可以从 List<Number> 复制到 List<Object>
        List<Object> objects = new ArrayList<>();
        copy(dest, objects);
        System.out.println(objects); // [1, 2, 3]
    }
}
```

## Collections.copy() 示例

`Collections.copy()` 是 PECS 原则的经典应用：

```java
public class CollectionsCopyDemo {
    public static void main(String[] args) {
        List<Integer> src = Arrays.asList(1, 2, 3);
        List<Number> dest = Arrays.asList(null, null, null);

        // Collections.copy 的签名：
        // public static <T> void copy(List<? super T> dest, List<? extends T> src)

        Collections.copy(dest, src);
        System.out.println(dest); // [1, 2, 3]

        // 更多示例
        List<Object> objects = new ArrayList<>(Arrays.asList(null, null, null));
        Collections.copy(objects, src);  // Integer -> Object
        System.out.println(objects); // [1, 2, 3]
    }
}

// 分析 Collections.copy 的签名
// List<? super T> dest  - dest 是消费者，接受 T 或 T 的父类型
// List<? extends T> src - src 是生产者，提供 T 或 T 的子类型

// 为什么 dest 需要预填充元素？
// 因为 copy 是按索引复制，dest 必须有足够的元素
```

## 通配符捕获

通配符捕获是指编译器推断通配符的具体类型：

```java
public class WildcardCaptureDemo {
    // 通配符捕获的方法
    public static void swap(List<?> list, int i, int j) {
        // 直接调用会编译错误
        // list.set(i, list.set(j, list.get(i)));  // 错误！

        // 使用辅助方法实现通配符捕获
        swapHelper(list, i, j);
    }

    // 辅助方法：捕获通配符类型
    private static <T> void swapHelper(List<T> list, int i, int j) {
        list.set(i, list.set(j, list.get(i)));
    }

    public static void main(String[] args) {
        List<String> names = new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie"));
        swap(names, 0, 2);
        System.out.println(names); // [Charlie, Bob, Alice]

        List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3));
        swap(numbers, 0, 1);
        System.out.println(numbers); // [2, 1, 3]
    }
}
```

通配符捕获的工作原理：

```java
// swapHelper 方法中，T 捕获了通配符 ? 的具体类型
// 当调用 swap(names, 0, 2) 时：
// 1. names 的类型是 List<String>
// 2. swap 的参数是 List<?>，即 List<String>
// 3. swapHelper 被调用时，T 被推断为 String
// 4. 因此 list.set(i, list.set(j, list.get(i))) 是类型安全的
```

## 通配符的使用限制

通配符有一些使用限制需要注意：

```java
public class WildcardLimitations {
    // 限制1：不能用于泛型方法的类型参数声明
    // public static <?> void process(List<?> list) {}  // 编译错误！

    // 正确做法：使用类型参数
    public static <T> void process(List<T> list) {
        // ...
    }

    // 限制2：不能创建泛型数组
    // List<?>[] array = new List<?>[10];  // 可以创建
    // List<? extends Number>[] array = new List<? extends Number>[10];  // 可以
    // 但不推荐，因为数组的协变性会带来问题

    // 限制3：不能实例化类型参数
    // public static <T> T create() {
    //     return new T();  // 编译错误！
    // }

    // 限制4：通配符不能用于 throws
    // public void process() throws <? extends Exception> {}  // 编译错误！
}
```

## 实战：设计灵活的 API

使用通配符设计更灵活的 API：

```java
public class FlexibleApiDesign {
    // 场景1：数据转换管道
    public static <I, O> void transform(
            List<? extends I> input,
            List<? super O> output,
            Function<I, O> transformer) {
        for (I item : input) {
            output.add(transformer.apply(item));
        }
    }

    // 场景2：合并多个列表
    public static <T> List<T> merge(List<? extends T>... lists) {
        List<T> result = new ArrayList<>();
        for (List<? extends T> list : lists) {
            result.addAll(list);
        }
        return result;
    }

    // 场景3：过滤并收集
    public static <T> void filterTo(
            List<? extends T> source,
            List<? super T> dest,
            Predicate<T> predicate) {
        for (T item : source) {
            if (predicate.test(item)) {
                dest.add(item);
            }
        }
    }

    // 场景4：累加器
    public static <T, R> R accumulate(
            List<? extends T> list,
            R identity,
            BiFunction<R, T, R> accumulator) {
        R result = identity;
        for (T item : list) {
            result = accumulator.apply(result, item);
        }
        return result;
    }

    // 场景5：分组
    public static <T, K> Map<K, List<T>> groupBy(
            List<? extends T> list,
            Function<T, K> keyExtractor) {
        Map<K, List<T>> map = new HashMap<>();
        for (T item : list) {
            K key = keyExtractor.apply(item);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    public static void main(String[] args) {
        // transform 示例
        List<Integer> ints = Arrays.asList(1, 2, 3);
        List<String> strings = new ArrayList<>();
        transform(ints, strings, Object::toString);
        System.out.println(strings); // [1, 2, 3]

        // merge 示例
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        List<Number> merged = merge(list1, list2);
        System.out.println(merged); // [1, 2, 3, 4]

        // filterTo 示例
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<Number> evens = new ArrayList<>();
        filterTo(numbers, evens, n -> n % 2 == 0);
        System.out.println(evens); // [2, 4, 6]

        // accumulate 示例
        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5);
        int sum = accumulate(values, 0, Integer::sum);
        System.out.println(sum); // 15

        // groupBy 示例
        List<String> words = Arrays.asList("apple", "banana", "avocado", "blueberry");
        Map<Character, List<String>> grouped = groupBy(words, s -> s.charAt(0));
        System.out.println(grouped);
        // {a=[apple, avocado], b=[banana, blueberry]}
    }
}
```

综合示例 - 通配符在实际项目中的应用：

```java
public class PracticalWildcardExample {
    // 数据导出服务
    public static class ExportService {
        // 导出任意类型的数据到 CSV
        public static <T> void exportToCsv(
                List<? extends T> data,
                Function<T, String> toCsvLine,
                Writer writer) throws IOException {
            for (T item : data) {
                writer.write(toCsvLine.apply(item));
                writer.write("\n");
            }
        }

        // 从多种数据源汇总
        public static <T> List<T> collectFromSources(
                List<? extends List<? extends T>> sources) {
            List<T> result = new ArrayList<>();
            for (List<? extends T> source : sources) {
                result.addAll(source);
            }
            return result;
        }
    }

    // 事件处理系统
    public static class EventProcessor {
        private final List<Consumer<? super Event>> handlers = new ArrayList<>();

        // 注册处理器：接受任何能处理 Event 或其父类型的处理器
        public void registerHandler(Consumer<? super Event> handler) {
            handlers.add(handler);
        }

        // 触发事件
        public void fireEvent(Event event) {
            for (Consumer<? super Event> handler : handlers) {
                handler.accept(event);
            }
        }
    }

    // 测试
    public static void main(String[] args) {
        // 导出示例
        List<User> users = Arrays.asList(
            new User("Alice", 25),
            new User("Bob", 30)
        );

        StringWriter writer = new StringWriter();
        ExportService.exportToCsv(
            users,
            user -> user.getName() + "," + user.getAge(),
            writer
        );
        System.out.println(writer);

        // 事件处理示例
        EventProcessor processor = new EventProcessor();

        // Consumer<Event> 可以作为 Consumer<? super Event>
        processor.registerHandler(event ->
            System.out.println("处理事件: " + event.getType()));

        // Consumer<Object> 也可以作为 Consumer<? super Event>
        processor.registerHandler(obj ->
            System.out.println("日志: " + obj));

        processor.fireEvent(new Event("click"));
    }

    static class Event {
        private final String type;
        Event(String type) { this.type = type; }
        String getType() { return type; }
    }

    static class User {
        private final String name;
        private final int age;
        User(String name, int age) { this.name = name; this.age = age; }
        String getName() { return name; }
        int getAge() { return age; }
    }
}
```

通配符和 PECS 原则是 Java 泛型系统的精髓。掌握它们可以让你设计出既灵活又类型安全的 API。记住：生产者用 extends，消费者用 super，既是生产者又是消费者时，不使用通配符。
