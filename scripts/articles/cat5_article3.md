# 内部类与匿名类

## 内部类概述

内部类是定义在另一个类内部的类，Java 中有四种类型的内部类：

1. **成员内部类**（非静态内部类）：定义在类中，方法外，没有 `static` 修饰
2. **静态嵌套类**：定义在类中，方法外，使用 `static` 修饰
3. **局部内部类**：定义在方法或代码块中
4. **匿名内部类**：没有类名的局部内部类

内部类的主要用途：
- 实现更好的封装
- 实现多重继承效果
- 实现回调机制
- 简化代码编写

## 成员内部类

成员内部类是定义在类中、方法外的非静态内部类。

### 基本语法

```java
public class Outer {
    private String name = "Outer";
    
    // 成员内部类
    public class Inner {
        public void display() {
            // 可以访问外部类的私有成员
            System.out.println("Name: " + name);
        }
    }
}
```

### 访问外部类成员

成员内部类可以访问外部类的所有成员，包括私有成员：

```java
public class Outer {
    private int x = 10;
    private static int y = 20;
    
    public class Inner {
        public void show() {
            System.out.println(x);   // 访问实例变量
            System.out.println(y);   // 访问静态变量
            System.out.println(Outer.this.x); // 显式访问外部类成员
        }
    }
}
```

### 持有外部类引用

每个成员内部类实例都隐式持有一个外部类实例的引用。这意味着：
- 创建内部类实例前，必须先有外部类实例
- 内部类实例不能脱离外部类实例独立存在

```java
public class Outer {
    private String name = "Outer";
    
    public class Inner {
        public void show() {
            // this 指向内部类实例
            System.out.println(this);
            // Outer.this 指向外部类实例
            System.out.println(Outer.this);
        }
    }
}
```

### 创建方式

```java
// 方式一：在外部类内部创建
public class Outer {
    public void createInner() {
        Inner inner = new Inner();
        inner.display();
    }
    
    public class Inner {
        public void display() {
            System.out.println("Inner");
        }
    }
}

// 方式二：在外部类外部创建
Outer outer = new Outer();
Outer.Inner inner = outer.new Inner();
inner.display();
```

### 内部类与外部类的同名变量

```java
public class Outer {
    private String name = "Outer";
    
    public class Inner {
        private String name = "Inner";
        
        public void show() {
            System.out.println(name);           // 访问内部类的 name
            System.out.println(this.name);      // 访问内部类的 name
            System.out.println(Outer.this.name); // 访问外部类的 name
        }
    }
}
```

## 静态嵌套类

静态嵌套类使用 `static` 修饰，不持有外部类的引用。

### 基本语法

```java
public class Outer {
    private static String staticVar = "Static";
    private String instanceVar = "Instance";
    
    // 静态嵌套类
    public static class StaticNested {
        public void show() {
            // 只能访问外部类的静态成员
            System.out.println(staticVar);
            // System.out.println(instanceVar); // 编译错误
        }
    }
}
```

### 与成员内部类的区别

| 特性 | 成员内部类 | 静态嵌套类 |
|------|-----------|-----------|
| 外部类引用 | 持有外部类实例引用 | 不持有外部类实例引用 |
| 访问实例成员 | 可以访问外部类实例成员 | 不能访问外部类实例成员 |
| 创建方式 | 需要外部类实例 | 不需要外部类实例 |
| 静态成员 | 不能有静态成员（除非是编译时常量） | 可以有静态成员 |

### 使用场景

静态嵌套类适合用于：
- 与外部类关系不紧密的辅助类
- 不需要访问外部类实例成员的类
- 工厂方法模式中的 Builder 类

```java
public class Computer {
    private String cpu;
    private String memory;
    
    // Builder 模式使用静态嵌套类
    public static class Builder {
        private String cpu;
        private String memory;
        
        public Builder cpu(String cpu) {
            this.cpu = cpu;
            return this;
        }
        
        public Builder memory(String memory) {
            this.memory = memory;
            return this;
        }
        
        public Computer build() {
            return new Computer(this);
        }
    }
    
    private Computer(Builder builder) {
        this.cpu = builder.cpu;
        this.memory = builder.memory;
    }
}
```

## 局部内部类

局部内部类定义在方法或代码块中，作用域仅限于该方法或代码块。

### 基本语法

```java
public class Outer {
    public void method() {
        // 局部内部类
        class LocalInner {
            public void show() {
                System.out.println("Local Inner");
            }
        }
        
        // 在方法内使用
        LocalInner inner = new LocalInner();
        inner.show();
    }
}
```

### 访问 effectively final 局部变量

局部内部类可以访问外部方法的局部变量，但要求变量是 effectively final：

```java
public class Outer {
    public void method() {
        int x = 10; // effectively final
        
        class LocalInner {
            public void show() {
                System.out.println(x); // 可以访问
                // x = 20; // 编译错误
            }
        }
        
        LocalInner inner = new LocalInner();
        inner.show();
    }
}
```

### 局部内部类的特点

- 只在定义它的方法中可见
- 不能使用 `public`、`private`、`protected`、`static` 修饰
- 可以访问外部类的成员
- 可以访问 effectively final 的局部变量
- 在编译后会生成 `Outer$1LocalInner.class` 这样的文件

## 匿名内部类

匿名内部类是没有类名的局部内部类，常用于创建只使用一次的类实例。

### 基本语法

```java
// 接口实现
接口类型 变量名 = new 接口类型() {
    // 实现方法
};

// 抽象类实例化
抽象类 变量名 = new 抽象类() {
    // 实现抽象方法
};
```

### 常见用法

#### 1. 接口实现

```java
// 实现 Runnable 接口
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("Running");
    }
};

// 实现 Comparator 接口
Comparator<String> comp = new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return s1.length() - s2.length();
    }
};
```

#### 2. 抽象类实例化

```java
// 创建抽象类的实例
AbstractList<String> list = new AbstractList<String>() {
    private String[] data = {"A", "B", "C"};
    
    @Override
    public String get(int index) {
        return data[index];
    }
    
    @Override
    public int size() {
        return data.length;
    }
};
```

#### 3. 事件监听器

```java
// Swing 事件处理
button.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Button clicked");
    }
});

// JavaFX 事件处理
button.setOnAction(new EventHandler<ActionEvent>() {
    @Override
    public void handle(ActionEvent event) {
        System.out.println("Button clicked");
    }
});
```

### 匿名内部类的特点

- 没有类名，不能定义构造方法
- 只能创建一个实例
- 不能是抽象类，必须实现所有抽象方法
- 可以访问外部类的成员
- 可以访问 effectively final 的局部变量
- 编译后会生成 `Outer$1.class` 这样的文件

## 匿名内部类与 Lambda 表达式的对比

| 特性 | 匿名内部类 | Lambda 表达式 |
|------|-----------|--------------|
| 适用接口 | 任何接口或抽象类 | 只能是函数式接口 |
| this 指向 | 匿名内部类实例 | 外部类实例 |
| 编译结果 | 单独的 `.class` 文件 | 使用 `invokedynamic` 指令 |
| 性能 | 每次创建新实例 | 首次调用有开销，后续调用快 |
| 变量捕获 | 访问 effectively final 变量 | 访问 effectively final 变量 |
| 代码简洁性 | 相对冗长 | 更简洁 |

### 选择建议

- 如果是函数式接口，优先使用 Lambda 表达式
- 如果需要访问 `this` 指向当前内部类，使用匿名内部类
- 如果是抽象类或非函数式接口，必须使用匿名内部类
- 如果需要多次实例化，考虑使用命名类

```java
// Lambda 表达式（推荐）
Runnable r1 = () -> System.out.println("Hello");

// 匿名内部类
Runnable r2 = new Runnable() {
    @Override
    public void run() {
        System.out.println("Hello");
    }
};
```

## 内部类的编译机制

内部类在编译后会生成单独的 `.class` 文件，命名规则如下：

- 成员内部类：`Outer$Inner.class`
- 静态嵌套类：`Outer$StaticNested.class`
- 局部内部类：`Outer$1LocalInner.class`
- 匿名内部类：`Outer$1.class`、`Outer$2.class` 等

```java
public class Outer {
    public class Inner {}
    
    public void method() {
        class Local {}
        
        Runnable r = new Runnable() {
            public void run() {}
        };
    }
}
```

编译后生成：
- `Outer.class`
- `Outer$Inner.class`
- `Outer$1Local.class`
- `Outer$1.class`
- `Outer$2.class`

## 内部类的实际应用场景

### 1. Builder 模式

```java
public class Pizza {
    private String size;
    private boolean cheese;
    private boolean pepperoni;
    private boolean mushrooms;
    
    private Pizza(Builder builder) {
        this.size = builder.size;
        this.cheese = builder.cheese;
        this.pepperoni = builder.pepperoni;
        this.mushrooms = builder.mushrooms;
    }
    
    public static class Builder {
        private String size;
        private boolean cheese;
        private boolean pepperoni;
        private boolean mushrooms;
        
        public Builder(String size) {
            this.size = size;
        }
        
        public Builder cheese(boolean cheese) {
            this.cheese = cheese;
            return this;
        }
        
        public Builder pepperoni(boolean pepperoni) {
            this.pepperoni = pepperoni;
            return this;
        }
        
        public Builder mushrooms(boolean mushrooms) {
            this.mushrooms = mushrooms;
            return this;
        }
        
        public Pizza build() {
            return new Pizza(this);
        }
    }
}

// 使用
Pizza pizza = new Pizza.Builder("large")
    .cheese(true)
    .pepperoni(true)
    .build();
```

### 2. 回调机制

```java
public class AsyncExecutor {
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
    
    public <T> void execute(Callable<T> task, Callback<T> callback) {
        new Thread(() -> {
            try {
                T result = task.call();
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}

// 使用
AsyncExecutor executor = new AsyncExecutor();
executor.execute(
    () -> "Result",
    new AsyncExecutor.Callback<String>() {
        @Override
        public void onSuccess(String result) {
            System.out.println("Success: " + result);
        }
        
        @Override
        public void onError(Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
);
```

### 3. 迭代器实现

```java
public class MyCollection<T> {
    private T[] items;
    private int size;
    
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index = 0;
            
            @Override
            public boolean hasNext() {
                return index < size;
            }
            
            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return items[index++];
            }
        };
    }
}
```

## 枚举类型

枚举（enum）是 Java 中用于定义一组常量的特殊类。

### 基本定义

```java
public enum Day {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}
```

### 带构造器和方法的枚举

```java
public enum Planet {
    MERCURY(3.303e+23, 2.4397e6),
    VENUS(4.869e+24, 6.0518e6),
    EARTH(5.976e+24, 6.37814e6);
    
    private final double mass;
    private final double radius;
    
    Planet(double mass, double radius) {
        this.mass = mass;
        this.radius = radius;
    }
    
    public double getMass() {
        return mass;
    }
    
    public double getRadius() {
        return radius;
    }
    
    public double surfaceGravity() {
        final double G = 6.67300E-11;
        return G * mass / (radius * radius);
    }
}
```

### 枚举的常用方法

```java
public enum Color {
    RED, GREEN, BLUE;
}

// values() 返回所有枚举常量
Color[] colors = Color.values();

// valueOf(String) 根据名称获取枚举常量
Color red = Color.valueOf("RED");

// name() 返回枚举常量的名称
String name = Color.RED.name();

// ordinal() 返回枚举常量的序号
int ordinal = Color.RED.ordinal();
```

### EnumSet 和 EnumMap

```java
// EnumSet - 高效的枚举集合
EnumSet<Day> weekdays = EnumSet.range(Day.MONDAY, Day.FRIDAY);
EnumSet<Day> weekend = EnumSet.of(Day.SATURDAY, Day.SUNDAY);

// EnumMap - 以枚举为键的 Map
EnumMap<Day, String> schedule = new EnumMap<>(Day.class);
schedule.put(Day.MONDAY, "Meeting");
schedule.put(Day.FRIDAY, "Party");
```

### 在 switch 中使用枚举

```java
public String getDayType(Day day) {
    switch (day) {
        case MONDAY:
        case TUESDAY:
        case WEDNESDAY:
        case THURSDAY:
        case FRIDAY:
            return "Weekday";
        case SATURDAY:
        case SUNDAY:
            return "Weekend";
        default:
            throw new IllegalArgumentException("Unknown day: " + day);
    }
}
```

### 枚举实现接口

```java
public interface Printable {
    void print();
}

public enum Status implements Printable {
    ACTIVE {
        @Override
        public void print() {
            System.out.println("Active");
        }
    },
    INACTIVE {
        @Override
        public void print() {
            System.out.println("Inactive");
        }
    };
}
```

枚举类型是 Java 中定义常量的最佳方式，它比常量类更安全、更简洁，并且支持方法、字段和接口实现。在实际开发中，应优先使用枚举来定义有限的常量集合。