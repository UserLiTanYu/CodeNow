# 结构型模式与行为型模式

设计模式分为创建型、结构型和行为型三大类。本文将重点讲解结构型模式（代理、适配器、装饰器、外观、组合、桥接）和行为型模式（策略、观察者、模板方法、责任链、迭代器、命令），并通过实际案例展示它们的应用。

## 结构型模式

结构型模式关注类和对象的组合，通过描述如何将类或对象组合在一起形成更大的结构。

### 代理模式（Proxy）

代理模式为其他对象提供一种代理以控制对这个对象的访问。

```java
// 接口
public interface Image {
    void display();
}

// 真实对象
public class RealImage implements Image {
    private String filename;

    public RealImage(String filename) {
        this.filename = filename;
        loadFromDisk();
    }

    private void loadFromDisk() {
        System.out.println("加载图片: " + filename);
    }

    @Override
    public void display() {
        System.out.println("显示图片: " + filename);
    }
}

// 代理对象
public class ProxyImage implements Image {
    private RealImage realImage;
    private String filename;

    public ProxyImage(String filename) {
        this.filename = filename;
    }

    @Override
    public void display() {
        if (realImage == null) {
            realImage = new RealImage(filename);  // 延迟加载
        }
        realImage.display();
    }
}
```

**应用场景**：

- 延迟加载（虚拟代理）
- 权限控制（保护代理）
- 缓存（缓存代理）
- AOP（动态代理）

### 适配器模式（Adapter）

适配器模式将一个类的接口转换成客户端期望的另一个接口：

```java
// 客户端期望的接口
public interface Target {
    void request();
}

// 已有的类（接口不兼容）
public class Adaptee {
    public void specificRequest() {
        System.out.println("Adaptee 的特殊请求");
    }
}

// 适配器
public class Adapter implements Target {
    private Adaptee adaptee;

    public Adapter(Adaptee adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public void request() {
        adaptee.specificRequest();
    }
}
```

**实际应用**：

```java
// Java 中的 InputStreamReader 就是适配器模式
Reader reader = new InputStreamReader(new FileInputStream("file.txt"), "UTF-8");

// Spring MVC 中的 HandlerAdapter
public interface HandlerAdapter {
    boolean supports(Object handler);
    ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler);
}
```

### 装饰器模式（Decorator）

装饰器模式动态地给对象添加额外的职责：

```java
// 接口
public interface Coffee {
    String getDescription();
    double getCost();
}

// 基础实现
public class SimpleCoffee implements Coffee {
    @Override
    public String getDescription() {
        return "简单咖啡";
    }

    @Override
    public double getCost() {
        return 10.0;
    }
}

// 装饰器基类
public abstract class CoffeeDecorator implements Coffee {
    protected Coffee decoratedCoffee;

    public CoffeeDecorator(Coffee coffee) {
        this.decoratedCoffee = coffee;
    }
}

// 具体装饰器
public class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription() + " + 牛奶";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + 3.0;
    }
}

public class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription() + " + 糖";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + 1.0;
    }
}

// 使用
Coffee coffee = new SimpleCoffee();
coffee = new MilkDecorator(coffee);
coffee = new SugarDecorator(coffee);
System.out.println(coffee.getDescription());  // 简单咖啡 + 牛奶 + 糖
System.out.println(coffee.getCost());  // 14.0
```

**与代理模式的区别**：

- 代理模式：控制对象访问，通常不改变接口
- 装饰器模式：动态添加职责，通常实现同一接口

**Java IO 中的装饰器模式**：

```java
InputStream is = new BufferedInputStream(new FileInputStream("file.txt"));
```

### 外观模式（Facade）

外观模式为子系统中的一组接口提供一个一致的界面：

```java
public class CPU {
    public void start() { System.out.println("CPU 启动"); }
    public void execute() { System.out.println("CPU 执行指令"); }
}

public class Memory {
    public void load() { System.out.println("内存加载数据"); }
}

public class Disk {
    public void read() { System.out.println("磁盘读取数据"); }
}

public class ComputerFacade {
    private CPU cpu = new CPU();
    private Memory memory = new Memory();
    private Disk disk = new Disk();

    public void start() {
        System.out.println("=== 电脑启动 ===");
        cpu.start();
        memory.load();
        disk.read();
        cpu.execute();
        System.out.println("=== 启动完成 ===");
    }
}
```

### 组合模式（Composite）

组合模式将对象组合成树形结构以表示"部分-整体"的层次结构：

```java
// 组件接口
public abstract class FileSystemComponent {
    protected String name;

    public FileSystemComponent(String name) {
        this.name = name;
    }

    public abstract long getSize();
    public abstract void display(int depth);
}

// 叶子节点
public class File extends FileSystemComponent {
    private long size;

    public File(String name, long size) {
        super(name);
        this.size = size;
    }

    @Override
    public long getSize() { return size; }

    @Override
    public void display(int depth) {
        System.out.println("  ".repeat(depth) + "- " + name + " (" + size + " bytes)");
    }
}

// 容器节点
public class Directory extends FileSystemComponent {
    private List<FileSystemComponent> children = new ArrayList<>();

    public Directory(String name) {
        super(name);
    }

    public void add(FileSystemComponent component) {
        children.add(component);
    }

    @Override
    public long getSize() {
        return children.stream().mapToLong(FileSystemComponent::getSize).sum();
    }

    @Override
    public void display(int depth) {
        System.out.println("  ".repeat(depth) + "+ " + name + "/");
        for (FileSystemComponent child : children) {
            child.display(depth + 1);
        }
    }
}
```

### 桥接模式（Bridge）

桥接模式将抽象部分与实现部分分离，使它们都可以独立变化：

```java
// 实现接口
public interface DrawAPI {
    void drawCircle(int x, int y, int radius);
}

public class RedDrawAPI implements DrawAPI {
    @Override
    public void drawCircle(int x, int y, int radius) {
        System.out.println("画红色圆: (" + x + "," + y + "), 半径=" + radius);
    }
}

public class GreenDrawAPI implements DrawAPI {
    @Override
    public void drawCircle(int x, int y, int radius) {
        System.out.println("画绿色圆: (" + x + "," + y + "), 半径=" + radius);
    }
}

// 抽象类
public abstract class Shape {
    protected DrawAPI drawAPI;
    protected Shape(DrawAPI drawAPI) { this.drawAPI = drawAPI; }
    public abstract void draw();
}

public class Circle extends Shape {
    private int x, y, radius;
    public Circle(int x, int y, int radius, DrawAPI drawAPI) {
        super(drawAPI);
        this.x = x; this.y = y; this.radius = radius;
    }

    @Override
    public void draw() { drawAPI.drawCircle(x, y, radius); }
}

// 形状和颜色可以独立变化
Shape redCircle = new Circle(10, 10, 5, new RedDrawAPI());
Shape greenCircle = new Circle(20, 20, 10, new GreenDrawAPI());
```

### 结构型模式对比

| 模式 | 意图 | 关键特征 |
|------|------|----------|
| 代理 | 控制对象访问 | 持有被代理对象的引用 |
| 适配器 | 接口转换 | 包装不兼容的接口 |
| 装饰器 | 动态添加职责 | 实现同一接口，层层包装 |
| 外观 | 简化复杂系统 | 提供统一的高层接口 |
| 组合 | 树形结构 | 统一叶子和容器的接口 |
| 桥接 | 分离抽象和实现 | 两个独立的维度 |

## 行为型模式

行为型模式关注对象之间的职责分配和算法封装。

### 策略模式（Strategy）

策略模式定义一系列算法，将每个算法封装起来，使它们可以互相替换：

```java
// 策略接口
public interface SortStrategy {
    void sort(int[] arr);
}

// 具体策略
public class BubbleSortStrategy implements SortStrategy {
    @Override
    public void sort(int[] arr) {
        System.out.println("冒泡排序");
        // 冒泡排序实现...
    }
}

public class QuickSortStrategy implements SortStrategy {
    @Override
    public void sort(int[] arr) {
        System.out.println("快速排序");
        // 快速排序实现...
    }
}

// 上下文
public class Sorter {
    private SortStrategy strategy;

    public Sorter(SortStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(SortStrategy strategy) {
        this.strategy = strategy;
    }

    public void sort(int[] arr) {
        strategy.sort(arr);
    }
}

// 使用
Sorter sorter = new Sorter(new BubbleSortStrategy());
sorter.sort(arr);
sorter.setStrategy(new QuickSortStrategy());
sorter.sort(arr);
```

**Spring 中的策略模式**：

```java
// Resource 接口就是策略模式
Resource resource = new ClassPathResource("application.yml");
Resource resource2 = new FileSystemResource("/path/to/file");
Resource resource3 = new UrlResource("https://example.com/file");
```

### 观察者模式（Observer）

观察者模式定义了一种一对多的依赖关系，当对象状态改变时，所有依赖它的对象都会得到通知：

```java
// 事件对象
public class UserEvent {
    private String username;
    private String action;

    public UserEvent(String username, String action) {
        this.username = username;
        this.action = action;
    }
    // getter 省略
}

// 观察者接口
public interface UserEventListener {
    void onUserEvent(UserEvent event);
}

// 具体观察者
public class EmailService implements UserEventListener {
    @Override
    public void onUserEvent(UserEvent event) {
        System.out.println("发送邮件通知: " + event.getUsername() + " " + event.getAction());
    }
}

public class LogService implements UserEventListener {
    @Override
    public void onUserEvent(UserEvent event) {
        System.out.println("记录日志: " + event.getUsername() + " " + event.getAction());
    }
}

// 被观察者（事件发布者）
public class UserService {
    private List<UserEventListener> listeners = new ArrayList<>();

    public void addListener(UserEventListener listener) {
        listeners.add(listener);
    }

    public void register(String username) {
        // 注册逻辑...
        UserEvent event = new UserEvent(username, "注册成功");
        notifyListeners(event);
    }

    private void notifyListeners(UserEvent event) {
        for (UserEventListener listener : listeners) {
            listener.onUserEvent(event);
        }
    }
}
```

### 模板方法模式（Template Method）

模板方法模式定义一个操作中的算法骨架，将某些步骤延迟到子类：

```java
// 抽象类定义模板方法
public abstract class AbstractExportService {

    // 模板方法
    public final void export(String query) {
        List<?> data = fetchData(query);
        List<?> processed = processData(data);
        byte[] bytes = formatData(processed);
        saveFile(bytes);
    }

    // 抽象方法，由子类实现
    protected abstract List<?> fetchData(String query);
    protected abstract byte[] formatData(List<?> data);

    // 钩子方法，子类可以覆盖
    protected List<?> processData(List<?> data) {
        return data;  // 默认不做处理
    }

    // 具体方法
    protected void saveFile(byte[] bytes) {
        System.out.println("保存文件，大小: " + bytes.length);
    }
}

// 具体实现
public class CsvExportService extends AbstractExportService {
    @Override
    protected List<?> fetchData(String query) {
        System.out.println("从数据库查询数据: " + query);
        return new ArrayList<>();
    }

    @Override
    protected byte[] formatData(List<?> data) {
        System.out.println("格式化为 CSV");
        return "col1,col2".getBytes();
    }
}

public class PdfExportService extends AbstractExportService {
    @Override
    protected List<?> fetchData(String query) {
        System.out.println("从数据库查询数据: " + query);
        return new ArrayList<>();
    }

    @Override
    protected byte[] formatData(List<?> data) {
        System.out.println("格式化为 PDF");
        return new byte[0];
    }
}
```

**Spring 中的模板方法**：

```java
// JdbcTemplate 就是模板方法模式
jdbcTemplate.query("SELECT * FROM user", (rs, rowNum) -> {
    User user = new User();
    user.setId(rs.getLong("id"));
    user.setName(rs.getString("name"));
    return user;
});
```

### 责任链模式（Chain of Responsibility）

责任链模式为请求创建了一个处理者链，每个处理者决定自己处理请求或传给下一个：

```java
// 处理者接口
public abstract class Handler {
    protected Handler next;

    public Handler setNext(Handler next) {
        this.next = next;
        return next;
    }

    public abstract boolean handle(int amount);
}

// 具体处理者
public class TeamLeader extends Handler {
    @Override
    public boolean handle(int amount) {
        if (amount <= 1000) {
            System.out.println("组长审批: 批准 " + amount + " 元");
            return true;
        }
        return next != null && next.handle(amount);
    }
}

public class DepartmentManager extends Handler {
    @Override
    public boolean handle(int amount) {
        if (amount <= 10000) {
            System.out.println("部门经理审批: 批准 " + amount + " 元");
            return true;
        }
        return next != null && next.handle(amount);
    }
}

public class GeneralManager extends Handler {
    @Override
    public boolean handle(int amount) {
        if (amount <= 100000) {
            System.out.println("总经理审批: 批准 " + amount + " 元");
            return true;
        }
        System.out.println("金额过大，需要董事会审批");
        return false;
    }
}

// 构建责任链
Handler chain = new TeamLeader();
chain.setNext(new DepartmentManager()).setNext(new GeneralManager());

chain.handle(500);    // 组长审批
chain.handle(5000);   // 部门经理审批
chain.handle(50000);  // 总经理审批
```

### 迭代器模式（Iterator）

迭代器模式提供一种方法顺序访问一个聚合对象中的各个元素，而不暴露其内部表示：

```java
// 自定义集合
public class BookShelf implements Iterable<String> {
    private List<String> books = new ArrayList<>();

    public void addBook(String book) {
        books.add(book);
    }

    @Override
    public Iterator<String> iterator() {
        return books.iterator();
    }
}

// 使用
BookShelf shelf = new BookShelf();
shelf.addBook("Java 编程思想");
shelf.addBook("设计模式");
shelf.addBook("算法导论");

for (String book : shelf) {
    System.out.println(book);
}
```

### 命令模式（Command）

命令模式将请求封装为对象，支持撤销、排队等操作：

```java
// 命令接口
public interface Command {
    void execute();
    void undo();
}

// 接收者
public class TextEditor {
    private StringBuilder content = new StringBuilder();

    public void insert(String text, int position) {
        content.insert(position, text);
    }

    public String delete(int start, int end) {
        String deleted = content.substring(start, end);
        content.delete(start, end);
        return deleted;
    }

    public String getContent() {
        return content.toString();
    }
}

// 具体命令
public class InsertCommand implements Command {
    private TextEditor editor;
    private String text;
    private int position;

    public InsertCommand(TextEditor editor, String text, int position) {
        this.editor = editor;
        this.text = text;
        this.position = position;
    }

    @Override
    public void execute() {
        editor.insert(text, position);
    }

    @Override
    public void undo() {
        editor.delete(position, position + text.length());
    }
}

// 命令管理器（支持撤销/重做）
public class CommandManager {
    private Stack<Command> undoStack = new Stack<>();
    private Stack<Command> redoStack = new Stack<>();

    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
        }
    }
}
```

### 模式选型决策树

```
需要封装算法族并可互换 → 策略模式
需要一对多通知机制 → 观察者模式
有固定流程但步骤可变 → 模板方法模式
请求需要经过多级处理 → 责任链模式
需要遍历集合而不暴露内部 → 迭代器模式
需要支持撤销/重做/队列 → 命令模式
需要控制对象访问 → 代理模式
接口不兼容需要转换 → 适配器模式
需要动态添加职责 → 装饰器模式
需要简化复杂子系统 → 外观模式
```

设计模式不是银弹，不要为了使用模式而使用模式。先理解问题的本质，再选择合适的模式。很多时候，简单直接的代码比过度设计的代码更好维护。
