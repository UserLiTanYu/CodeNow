# 枚举类型详解

在 Java 中，枚举（Enum）是一种特殊的类，用于表示一组固定的常量。相比传统的 `int` 常量或 `static final` 字符串，枚举提供了类型安全、可读性更强的解决方案。本文将深入探讨枚举的各种用法和高级特性。

## 枚举的定义

使用 `enum` 关键字定义枚举类型，枚举常量之间用逗号分隔：

```java
public enum Season {
    SPRING, SUMMER, AUTUMN, WINTER
}
```

每个枚举常量都是该枚举类型的一个实例。枚举本质上是一个继承自 `java.lang.Enum` 的类，因此不能显式继承其他类，但可以实现接口。

```java
// 使用枚举
Season season = Season.SPRING;

// 在 switch 中使用
switch (season) {
    case SPRING -> System.out.println("春天来了");
    case SUMMER -> System.out.println("夏天到了");
    case AUTUMN -> System.out.println("秋高气爽");
    case WINTER -> System.out.println("冬日暖阳");
}
```

## 枚举的构造器

枚举可以有构造器，但必须是私有的（private 或 package-private）。构造器在枚举常量创建时被调用：

```java
public enum Planet {
    MERCURY(3.303e+23, 2.4397e6),
    VENUS(4.869e+24, 6.0518e6),
    EARTH(5.976e+24, 6.37814e6),
    MARS(6.421e+23, 3.3972e6);

    private final double mass;   // 质量（千克）
    private final double radius; // 半径（米）

    // 私有构造器
    Planet(double mass, double radius) {
        this.mass = mass;
        this.radius = radius;
    }

    public double getMass() { return mass; }
    public double getRadius() { return radius; }

    // 计算表面重力
    public double surfaceGravity() {
        final double G = 6.67300E-11;
        return G * mass / (radius * radius);
    }
}
```

带参数的构造器使得每个枚举常量可以拥有不同的属性值，这是枚举强大功能的基础。

## 枚举的常用方法

`java.lang.Enum` 类提供了几个重要的方法：

| 方法 | 说明 |
|------|------|
| `values()` | 返回包含所有枚举常量的数组 |
| `valueOf(String)` | 根据名称返回对应的枚举常量 |
| `ordinal()` | 返回枚举常量的序号（从 0 开始） |
| `name()` | 返回枚举常量的名称（与声明一致） |
| `toString()` | 默认返回 name()，可重写 |

```java
public class EnumMethodsDemo {
    public static void main(String[] args) {
        // values() 遍历所有常量
        for (Direction dir : Direction.values()) {
            System.out.println(dir.ordinal() + ": " + dir.name());
        }

        // valueOf() 字符串转枚举
        Direction north = Direction.valueOf("NORTH");
        System.out.println(north); // NORTH

        // ordinal() 返回声明顺序
        System.out.println(Direction.EAST.ordinal()); // 1

        try {
            Direction invalid = Direction.valueOf("UP");
        } catch (IllegalArgumentException e) {
            System.out.println("无效的枚举值: UP");
        }
    }
}

enum Direction {
    NORTH, EAST, SOUTH, WEST
}
```

输出结果：
```
0: NORTH
1: EAST
2: SOUTH
3: WEST
NORTH
1
无效的枚举值: UP
```

## 枚举中定义抽象方法

枚举可以定义抽象方法，每个枚举常量必须实现该方法：

```java
public enum Operation {
    PLUS("+") {
        @Override
        public double apply(double x, double y) {
            return x + y;
        }
    },
    MINUS("-") {
        @Override
        public double apply(double x, double y) {
            return x - y;
        }
    },
    TIMES("*") {
        @Override
        public double apply(double x, double y) {
            return x * y;
        }
    },
    DIVIDE("/") {
        @Override
        public double apply(double x, double y) {
            if (y == 0) throw new ArithmeticException("Division by zero");
            return x / y;
        }
    };

    private final String symbol;

    Operation(String symbol) {
        this.symbol = symbol;
    }

    // 抽象方法，每个常量必须实现
    public abstract double apply(double x, double y);

    @Override
    public String toString() {
        return symbol;
    }
}

// 使用
double result = Operation.PLUS.apply(3, 4); // 7.0
```

这种方式将行为与数据绑定在一起，每个枚举常量都是一个策略的实现。

## 枚举实现接口

枚举可以实现一个或多个接口：

```java
public interface Printable {
    void print();
}

public interface Describable {
    String getDescription();
}

public enum Color implements Printable, Describable {
    RED("红色", "#FF0000"),
    GREEN("绿色", "#00FF00"),
    BLUE("蓝色", "#0000FF");

    private final String name;
    private final String hexCode;

    Color(String name, String hexCode) {
        this.name = name;
        this.hexCode = hexCode;
    }

    @Override
    public void print() {
        System.out.println(name + " (" + hexCode + ")");
    }

    @Override
    public String getDescription() {
        return "颜色: " + name + ", 十六进制: " + hexCode;
    }
}

// 使用
Color.RED.print();           // 红色 (#FF0000)
Color.BLUE.getDescription(); // 颜色: 蓝色, 十六进制: #0000FF
```

## EnumSet：高效枚举集合

`EnumSet` 是专门为枚举类型设计的高性能集合，内部使用位向量实现：

```java
import java.util.EnumSet;

public class EnumSetDemo {
    public static void main(String[] args) {
        // 创建 EnumSet
        EnumSet<Day> weekend = EnumSet.of(Day.SATURDAY, Day.SUNDAY);
        EnumSet<Day> weekdays = EnumSet.range(Day.MONDAY, Day.FRIDAY);
        EnumSet<Day> allDays = EnumSet.allOf(Day.class);
        EnumSet<Day> none = EnumSet.noneOf(Day.class);

        // complementOf: 获取补集
        EnumSet<Day> notWeekend = EnumSet.complementOf(weekend);
        System.out.println(notWeekend); // [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]

        // 集合运算
        EnumSet<Day> set1 = EnumSet.of(Day.MONDAY, Day.WEDNESDAY, Day.FRIDAY);
        EnumSet<Day> set2 = EnumSet.of(Day.TUESDAY, Day.WEDNESDAY, Day.THURSDAY);

        EnumSet<Day> union = EnumSet.copyOf(set1);
        union.addAll(set2);
        System.out.println("并集: " + union);

        EnumSet<Day> intersection = EnumSet.copyOf(set1);
        intersection.retainAll(set2);
        System.out.println("交集: " + intersection);
    }
}

enum Day {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}
```

`EnumSet` 的方法列表：

| 方法 | 说明 |
|------|------|
| `of(E...)` | 创建包含指定元素的集合 |
| `range(E, E)` | 创建包含范围内所有元素的集合 |
| `allOf(Class)` | 创建包含所有枚举常量的集合 |
| `noneOf(Class)` | 创建空集合 |
| `complementOf(EnumSet)` | 创建补集 |
| `copyOf(Collection)` | 从已有集合复制 |

## EnumMap：以枚举为键的高效映射

`EnumMap` 是键类型为枚举的专用 Map 实现，性能优于 HashMap：

```java
import java.util.EnumMap;
import java.util.Map;

public class EnumMapDemo {
    public static void main(String[] args) {
        // 创建 EnumMap
        Map<WeekDay, String> schedule = new EnumMap<>(WeekDay.class);

        schedule.put(WeekDay.MONDAY, "项目会议");
        schedule.put(WeekDay.WEDNESDAY, "代码评审");
        schedule.put(WeekDay.FRIDAY, "周报提交");

        // 遍历（按枚举声明顺序）
        for (Map.Entry<WeekDay, String> entry : schedule.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        // 使用 merge 统计字符出现次数
        String text = "hello world";
        Map<Character, Integer> charCount = new EnumMap<>(Character.class); // 注意：Character 不是枚举
        // 这里仅为演示 merge 用法，实际应使用 HashMap
    }
}

enum WeekDay {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}
```

`EnumMap` 的优势：
- 内部使用数组实现，查找和插入为 O(1)
- 按枚举常量的声明顺序迭代
- 不允许 null 键

## 枚举实现策略模式

枚举是实现策略模式的理想方式：

```java
public enum PayType {
    ALIPAY {
        @Override
        public double calculateFee(double amount) {
            return amount * 0.006; // 支付宝费率 0.6%
        }

        @Override
        public void pay(double amount) {
            System.out.println("支付宝支付: ¥" + amount);
        }
    },
    WECHAT {
        @Override
        public double calculateFee(double amount) {
            return amount * 0.006; // 微信费率 0.6%
        }

        @Override
        public void pay(double amount) {
            System.out.println("微信支付: ¥" + amount);
        }
    },
    CREDIT_CARD {
        @Override
        public double calculateFee(double amount) {
            return amount * 0.01; // 信用卡费率 1%
        }

        @Override
        public void pay(double amount) {
            System.out.println("信用卡支付: ¥" + amount);
        }
    };

    // 抽象方法定义策略接口
    public abstract double calculateFee(double amount);
    public abstract void pay(double amount);

    // 模板方法
    public void processPayment(double amount) {
        double fee = calculateFee(amount);
        System.out.println("手续费: ¥" + fee);
        pay(amount + fee);
    }
}

// 使用
public class PaymentService {
    public void checkout(PayType payType, double amount) {
        payType.processPayment(amount);
    }
}

// 调用
new PaymentService().checkout(PayType.ALIPAY, 100.0);
```

## 枚举与 switch 的配合

枚举与 switch 语句配合使用非常优雅：

```java
public enum TrafficLight {
    RED, YELLOW, GREEN
}

public class TrafficController {
    public String getNextLight(TrafficLight current) {
        return switch (current) {
            case RED -> "绿灯";
            case YELLOW -> "红灯";
            case GREEN -> "黄灯";
        };
    }

    public int getWaitTime(TrafficLight light) {
        return switch (light) {
            case RED -> 60;
            case YELLOW -> 3;
            case GREEN -> 45;
        };
    }
}
```

Java 14+ 的 switch 表达式与枚举结合更加简洁，且编译器会检查是否覆盖了所有枚举常量。

## 枚举的单例特性

枚举是实现单例模式的最佳方式，它天然具备以下特性：

```java
public enum Singleton {
    INSTANCE;

    private int count = 0;

    public void doSomething() {
        count++;
        System.out.println("处理请求 #" + count);
    }

    public int getCount() {
        return count;
    }
}

// 使用
Singleton.INSTANCE.doSomething();
```

枚举单例的优势：

| 特性 | 说明 |
|------|------|
| 线程安全 | JVM 保证枚举实例的创建是线程安全的 |
| 防序列化 | 枚举的序列化机制不会创建新实例 |
| 防反射攻击 | 无法通过反射创建枚举实例 |

```java
// 测试防序列化
public class SingletonTest {
    public static void main(String[] args) throws Exception {
        Singleton s1 = Singleton.INSTANCE;

        // 序列化
        ObjectOutputStream oos = new ObjectOutputStream(
            new FileOutputStream("singleton.ser"));
        oos.writeObject(s1);
        oos.close();

        // 反序列化
        ObjectInputStream ois = new ObjectInputStream(
            new FileInputStream("singleton.ser"));
        Singleton s2 = (Singleton) ois.readObject();
        ois.close();

        System.out.println(s1 == s2); // true，同一个实例

        // 测试防反射
        try {
            Constructor<Singleton> constructor = Singleton.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Singleton s3 = constructor.newInstance(); // 抛出异常
        } catch (IllegalArgumentException e) {
            System.out.println("无法通过反射创建枚举实例");
        }
    }
}
```

## Effective Java：用枚举代替 int 常量

《Effective Java》第 34 条建议：用枚举类型代替 int 常量。对比两种方式：

```java
// 不推荐：int 常量模式
public class Apple {
    public static final int FUJI = 0;
    public static final int PIPPIN = 1;
    public static final int GRANNY_SMITH = 2;
}

public class Orange {
    public static final int NAVEL = 0;
    public static final int TEMPLE = 1;
    public static final int BLOOD = 2;
}

// 问题：类型不安全
int apple = Apple.FUJI;
int orange = Orange.NAVEL;
// apple == orange 为 true，语义错误但编译通过
```

```java
// 推荐：枚举类型
public enum Apple { FUJI, PIPPIN, GRANNY_SMITH }
public enum Orange { NAVEL, TEMPLE, BLOOD }

// 类型安全
Apple apple = Apple.FUJI;
Orange orange = Orange.NAVEL;
// apple == orange 编译错误
```

枚举相比 int 常量的优势：

1. **类型安全**：编译器阻止非法值
2. **可读性**：`Apple.FUJI` 比 `0` 更清晰
3. **命名空间**：不同枚举类型的常量互不干扰
4. **可扩展**：可以添加方法和字段
5. **可用于集合**：EnumSet 和 EnumMap 提供高性能操作
6. **单例保证**：每个枚举常量在 JVM 中只有一个实例

```java
// 枚举的高级用法：实现责任链
public enum Logger {
    DEBUG(1) {
        @Override
        public void log(String message) {
            System.out.println("[DEBUG] " + message);
        }
    },
    INFO(2) {
        @Override
        public void log(String message) {
            System.out.println("[INFO] " + message);
        }
    },
    WARN(3) {
        @Override
        public void log(String message) {
            System.out.println("[WARN] " + message);
        }
    },
    ERROR(4) {
        @Override
        public void log(String message) {
            System.out.println("[ERROR] " + message);
        }
    };

    private final int level;

    Logger(int level) {
        this.level = level;
    }

    public abstract void log(String message);

    public boolean isEnabled(Logger other) {
        return this.level <= other.level;
    }
}
```

枚举是 Java 中一个强大但常被低估的特性。合理使用枚举可以让代码更安全、更清晰、更易维护。在设计固定常量集合时，优先考虑使用枚举而非 int 常量或字符串常量。
