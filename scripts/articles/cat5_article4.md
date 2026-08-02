# 常用函数式接口与 Comparator 排序

Java 8 引入了函数式编程特性，`java.util.function` 包提供了一套通用的函数式接口，极大地简化了代码编写。本文将详细介绍这些常用函数式接口，并深入探讨 `Comparator` 的排序技巧。

## java.util.function 包概述

`java.util.function` 包包含了 JDK 内置的函数式接口，它们是 Lambda 表达式和方法引用的目标类型。主要分类如下：

| 接口 | 方法 | 用途 |
|------|------|------|
| `Predicate<T>` | `boolean test(T t)` | 条件判断 |
| `Function<T,R>` | `R apply(T t)` | 类型转换/映射 |
| `Supplier<T>` | `T get()` | 生产/提供值 |
| `Consumer<T>` | `void accept(T t)` | 消费/处理值 |
| `UnaryOperator<T>` | `T apply(T t)` | 一元操作 |
| `BinaryOperator<T>` | `T apply(T t1, T t2)` | 二元操作 |

## Predicate\<T\>：条件判断

`Predicate` 用于判断条件是否成立，返回 `boolean` 值：

```java
import java.util.function.Predicate;

public class PredicateDemo {
    public static void main(String[] args) {
        // 基本用法
        Predicate<String> isEmpty = String::isEmpty;
        Predicate<String> isNull = Objects::isNull;

        System.out.println(isEmpty.test(""));    // true
        System.out.println(isNull.test(null));    // true

        // 组合条件：and()、or()、negate()
        Predicate<Integer> isPositive = n -> n > 0;
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Predicate<Integer> isPositiveEven = isPositive.and(isEven);
        System.out.println(isPositiveEven.test(4));  // true
        System.out.println(isPositiveEven.test(3));  // false

        Predicate<Integer> isPositiveOrEven = isPositive.or(isEven);
        System.out.println(isPositiveOrEven.test(3)); // true

        Predicate<Integer> isNonPositive = isPositive.negate();
        System.out.println(isNonPositive.test(-1));   // true

        // isEqual() 创建相等判断谓词
        Predicate<String> isHello = Predicate.isEqual("Hello");
        System.out.println(isHello.test("Hello"));   // true
        System.out.println(isHello.test("World"));   // false
    }
}
```

`Predicate` 在过滤操作中广泛使用：

```java
List<String> names = Arrays.asList("Alice", "", "Bob", null, "Charlie");

// 过滤非空且非空字符串
Predicate<String> isValid = s -> s != null && !s.isEmpty();
List<String> validNames = names.stream()
    .filter(isValid)
    .collect(Collectors.toList());
// [Alice, Bob, Charlie]
```

## Function\<T,R\>：类型转换与映射

`Function` 表示接受一个参数并返回结果的函数：

```java
import java.util.function.Function;

public class FunctionDemo {
    public static void main(String[] args) {
        // 基本用法
        Function<String, Integer> strLength = String::length;
        Function<Integer, String> intToStr = Object::toString;

        System.out.println(strLength.apply("Hello")); // 5

        // andThen()：先执行当前函数，再执行参数函数
        Function<String, String> toUpperCase = String::toUpperCase;
        Function<String, String> addPrefix = s -> "PREFIX_" + s;

        Function<String, String> combined = toUpperCase.andThen(addPrefix);
        System.out.println(combined.apply("hello")); // PREFIX_HELLO

        // compose()：先执行参数函数，再执行当前函数
        Function<Integer, Integer> multiply2 = n -> n * 2;
        Function<Integer, Integer> add3 = n -> n + 3;

        Function<Integer, Integer> composed = multiply2.compose(add3);
        System.out.println(composed.apply(5)); // 16: (5+3)*2

        // identity()：返回输入参数本身
        Function<String, String> identity = Function.identity();
        System.out.println(identity.apply("Hello")); // Hello
    }
}
```

`Function` 在集合转换中非常有用：

```java
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

// 转换为大写
List<String> upperNames = names.stream()
    .map(String::toUpperCase)
    .collect(Collectors.toList());

// 转换为长度
List<Integer> lengths = names.stream()
    .map(String::length)
    .collect(Collectors.toList());

// 使用 andThen 链式转换
Function<String, User> toUser = User::new;
Function<User, UserDTO> toDTO = user -> new UserDTO(user.getName(), user.getAge());
Function<String, UserDTO> pipeline = toUser.andThen(toDTO);

List<UserDTO> dtos = names.stream()
    .map(pipeline)
    .collect(Collectors.toList());
```

## Supplier\<T\>：延迟求值与工厂模式

`Supplier` 不接受参数，返回一个值，常用于延迟求值和工厂模式：

```java
import java.util.function.Supplier;

public class SupplierDemo {
    public static void main(String[] args) {
        // 基本用法
        Supplier<Double> randomSupplier = Math::random;
        System.out.println(randomSupplier.get()); // 随机数

        // 延迟求值
        Supplier<String> lazyValue = () -> {
            System.out.println("计算中...");
            return "计算结果";
        };

        // 只有调用 get() 时才执行
        if (Math.random() > 0.5) {
            System.out.println(lazyValue.get());
        }

        // 工厂模式
        Supplier<List<String>> listFactory = ArrayList::new;
        List<String> list1 = listFactory.get();
        List<String> list2 = listFactory.get();

        // 创建默认值
        Supplier<User> defaultUser = () -> new User("Guest", 0);
        User user = getUserFromCache().orElseGet(defaultUser);
    }

    static Optional<User> getUserFromCache() {
        // 模拟从缓存获取用户
        return Optional.empty();
    }
}
```

`Supplier` 在 `Optional` 中的应用：

```java
public class UserService {
    private final Supplier<User> defaultUserSupplier;

    public UserService() {
        this.defaultUserSupplier = () -> new User("Anonymous", -1);
    }

    public User getUser(String id) {
        return Optional.ofNullable(findById(id))
            .orElseGet(defaultUserSupplier);
    }

    // 使用 orElseThrow
    public User getUserOrThrow(String id) {
        return Optional.ofNullable(findById(id))
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

## Consumer\<T\>：消费与遍历

`Consumer` 接受一个参数，不返回结果，用于消费数据：

```java
import java.util.function.Consumer;

public class ConsumerDemo {
    public static void main(String[] args) {
        // 基本用法
        Consumer<String> print = System.out::println;
        print.accept("Hello"); // 输出: Hello

        // andThen()：链式执行
        Consumer<String> log = s -> System.out.println("Log: " + s);
        Consumer<String> save = s -> System.out.println("Saved: " + s);
        Consumer<String> process = log.andThen(save);
        process.accept("data"); // 先 log，再 save

        // 遍历操作
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        names.forEach(print);

        // 自定义 forEach
        forEach(names, name -> {
            System.out.println("Processing: " + name);
        });
    }

    static <T> void forEach(Iterable<T> iterable, Consumer<T> action) {
        for (T item : iterable) {
            action.accept(item);
        }
    }
}
```

`Consumer` 在集合操作中的应用：

```java
public class OrderService {
    private final List<Consumer<Order>> validators = new ArrayList<>();

    public void addValidator(Consumer<Order> validator) {
        validators.add(validator);
    }

    public void processOrder(Order order) {
        // 执行所有验证器
        validators.forEach(v -> v.accept(order));
        // 处理订单
        System.out.println("订单处理完成: " + order.getId());
    }
}

// 使用
OrderService service = new OrderService();
service.addValidator(order -> {
    if (order.getAmount() <= 0) {
        throw new IllegalArgumentException("金额必须大于0");
    }
});
service.addValidator(order -> {
    if (order.getItems().isEmpty()) {
        throw new IllegalArgumentException("订单项不能为空");
    }
});
```

## UnaryOperator\<T\> 和 BinaryOperator\<T\>

这两个是 `Function` 的特化版本，输入输出类型相同：

```java
import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;

public class OperatorDemo {
    public static void main(String[] args) {
        // UnaryOperator：一元操作
        UnaryOperator<String> toUpper = String::toUpperCase;
        UnaryOperator<String> trim = String::trim;
        UnaryOperator<String> process = trim.andThen(toUpper);

        System.out.println(process.apply("  hello  ")); // HELLO

        // 列表元素转换
        List<String> words = new ArrayList<>(Arrays.asList("hello", "world"));
        words.replaceAll(toUpper); // [HELLO, WORLD]

        // BinaryOperator：二元操作
        BinaryOperator<Integer> sum = Integer::sum;
        BinaryOperator<Integer> max = BinaryOperator.maxBy(Comparator.naturalOrder());

        System.out.println(sum.apply(3, 4)); // 7
        System.out.println(max.apply(3, 4)); // 4

        // reduce 操作
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        int total = numbers.stream().reduce(0, sum);
        System.out.println(total); // 15
    }
}
```

## BiFunction、BiPredicate、BiConsumer：双参数变体

这些是对应接口的双参数版本：

```java
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BiConsumer;

public class BiVariantDemo {
    public static void main(String[] args) {
        // BiFunction<T, U, R>
        BiFunction<String, Integer, User> createUser = User::new;
        User user = createUser.apply("Alice", 25);

        // BiPredicate<T, U>
        BiPredicate<String, String> startsWith = String::startsWith;
        System.out.println(startsWith.test("Hello", "He")); // true

        // BiConsumer<T, U>
        BiConsumer<String, Integer> printPair = (k, v) ->
            System.out.println(k + " = " + v);
        printPair.accept("age", 25);

        // 在 Map 操作中使用
        Map<String, Integer> map = new HashMap<>();
        map.put("Alice", 25);
        map.put("Bob", 30);

        map.forEach(printPair);
    }
}
```

## Comparator 接口详解

`Comparator` 是 Java 中最常用的函数式接口之一，用于定义对象的排序规则。

### comparing() 创建比较器

```java
import java.util.Comparator;

public class ComparatorDemo {
    public static void main(String[] args) {
        List<Employee> employees = Arrays.asList(
            new Employee("Alice", "IT", 8000),
            new Employee("Bob", "HR", 6000),
            new Employee("Charlie", "IT", 9000),
            new Employee("David", "HR", 7000)
        );

        // 使用 comparing() 创建比较器
        Comparator<Employee> byName = Comparator.comparing(Employee::getName);
        Comparator<Employee> bySalary = Comparator.comparing(Employee::getSalary);

        employees.sort(byName);
        System.out.println("按姓名排序: " + employees);

        employees.sort(bySalary);
        System.out.println("按薪资排序: " + employees);
    }
}
```

### thenComparing() 链式比较

```java
// 多字段排序
Comparator<Employee> comparator = Comparator
    .comparing(Employee::getDepartment)  // 先按部门
    .thenComparing(Employee::getSalary)  // 再按薪资
    .thenComparing(Employee::getName);   // 最后按姓名

employees.sort(comparator);

// 逆序排序
Comparator<Employee> bySalaryDesc = Comparator
    .comparing(Employee::getSalary)
    .reversed();

employees.sort(bySalaryDesc);
```

### reversed() 逆序

```java
// 逆序排序
Comparator<Integer> natural = Comparator.naturalOrder();
Comparator<Integer> reversed = natural.reversed();

List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9);
numbers.sort(reversed);
System.out.println(numbers); // [9, 5, 4, 3, 1, 1]
```

### nullsFirst() 和 nullsLast() 处理 null

```java
List<String> names = Arrays.asList("Charlie", null, "Alice", "Bob", null);

// null 值排在前面
Comparator<String> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
names.sort(nullsFirst);
System.out.println(names); // [null, null, Alice, Bob, Charlie]

// null 值排在后面
Comparator<String> nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
names.sort(nullsLast);
System.out.println(names); // [Alice, Bob, Charlie, null, null]

// 在对象排序中处理 null
Comparator<Employee> byNameNullSafe = Comparator
    .comparing(Employee::getName, Comparator.nullsLast(Comparator.naturalOrder()));
```

### 基本类型特化

```java
// comparingInt() 避免装箱
Comparator<Employee> bySalaryInt = Comparator.comparingInt(Employee::getSalaryInt);

// comparingLong()
Comparator<Transaction> byAmount = Comparator.comparingLong(Transaction::getAmount);

// comparingDouble()
Comparator<Product> byPrice = Comparator.comparingDouble(Product::getPrice);
```

## List.sort()、Arrays.sort()、TreeSet/TreeMap 使用 Comparator

```java
public class SortExamples {
    public static void main(String[] args) {
        List<String> names = new ArrayList<>(Arrays.asList("Charlie", "Alice", "Bob"));

        // List.sort()
        names.sort(Comparator.naturalOrder());
        names.sort(Comparator.reverseOrder());

        // Arrays.sort()
        String[] array = {"Charlie", "Alice", "Bob"};
        Arrays.sort(array);
        Arrays.sort(array, Comparator.reverseOrder());

        // TreeSet
        TreeSet<String> treeSet = new TreeSet<>(Comparator.reverseOrder());
        treeSet.addAll(Arrays.asList("Charlie", "Alice", "Bob"));

        // TreeMap
        TreeMap<String, Integer> treeMap = new TreeMap<>(Comparator.reverseOrder());
        treeMap.put("Charlie", 3);
        treeMap.put("Alice", 1);
        treeMap.put("Bob", 2);

        // 自定义对象排序
        TreeSet<Employee> employeeSet = new TreeSet<>(
            Comparator.comparing(Employee::getSalary)
                      .thenComparing(Employee::getName)
        );
    }
}
```

## 实战：多字段排序

以下是一个完整的多字段排序示例：

```java
public class MultiFieldSortDemo {
    public static void main(String[] args) {
        List<Employee> employees = Arrays.asList(
            new Employee("张三", "技术部", 15000, LocalDate.of(2020, 1, 15)),
            new Employee("李四", "市场部", 12000, LocalDate.of(2019, 6, 1)),
            new Employee("王五", "技术部", 18000, LocalDate.of(2018, 3, 20)),
            new Employee("赵六", "市场部", 12000, LocalDate.of(2021, 9, 10)),
            new Employee("钱七", "技术部", 15000, LocalDate.of(2020, 1, 15))
        );

        // 需求：先按部门升序，再按薪资降序，再按姓名升序
        Comparator<Employee> comparator = Comparator
            .comparing(Employee::getDepartment)
            .thenComparing(Employee::getSalary, Comparator.reverseOrder())
            .thenComparing(Employee::getName);

        employees.sort(comparator);

        employees.forEach(System.out::println);
    }

    static class Employee {
        private String name;
        private String department;
        private int salary;
        private LocalDate hireDate;

        // 构造器、getter、setter、toString 省略

        public String getName() { return name; }
        public String getDepartment() { return department; }
        public int getSalary() { return salary; }
        public LocalDate getHireDate() { return hireDate; }

        @Override
        public String toString() {
            return String.format("%-6s %-8s %6d %s", name, department, salary, hireDate);
        }
    }
}
```

输出结果：
```
王五   技术部    18000 2018-03-20
张三   技术部    15000 2020-01-15
钱七   技术部    15000 2020-01-15
李四   市场部    12000 2019-06-01
赵六   市场部    12000 2021-09-10
```

更复杂的排序场景：

```java
public class AdvancedSortDemo {
    // 自定义比较器：按薪资范围分组
    static Comparator<Employee> bySalaryRange() {
        return Comparator.comparingInt(emp -> {
            int salary = emp.getSalary();
            if (salary < 10000) return 1;
            if (salary < 20000) return 2;
            return 3;
        });
    }

    // 按入职年限排序
    static Comparator<Employee> byYearsOfService() {
        return Comparator.comparingInt(emp ->
            Period.between(emp.getHireDate(), LocalDate.now()).getYears()
        ).reversed();
    }

    // 组合多个复杂排序
    static Comparator<Employee> complexSort() {
        return Comparator
            .comparing(Employee::getDepartment)
            .thenComparing(bySalaryRange())
            .thenComparing(byYearsOfService())
            .thenComparing(Employee::getName);
    }

    public static void main(String[] args) {
        List<Employee> employees = getEmployees();
        employees.sort(complexSort());
        employees.forEach(System.out::println);
    }
}
```

函数式接口和 `Comparator` 的结合使用，让 Java 的集合操作变得更加简洁和表达力强。掌握这些接口的用法，是编写现代 Java 代码的基础。
