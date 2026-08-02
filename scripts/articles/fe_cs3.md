# 操作系统：进程、线程与内存管理

操作系统是计算机科学的核心基础，理解进程、线程和内存管理对后端开发者至关重要。这些概念不仅是面试的高频考点，更是理解并发编程、性能优化的基石。

## 进程与线程

### 基本概念

**进程**是程序的一次执行实例，是系统资源分配的基本单位。每个进程拥有独立的地址空间、文件描述符、环境变量等。

**线程**是进程中的执行单元，是 CPU 调度的基本单位。同一进程内的线程共享进程的地址空间和资源。

### 进程 vs 线程

| 特性 | 进程 | 线程 |
|------|------|------|
| 资源分配 | 独立地址空间 | 共享进程地址空间 |
| 创建开销 | 大（需要分配资源） | 小（共享资源） |
| 切换开销 | 大（需要切换页表等） | 小（只需切换寄存器） |
| 通信方式 | IPC（管道、消息队列等） | 共享内存 |
| 崩溃影响 | 不影响其他进程 | 可能导致整个进程崩溃 |
| 典型实现 | fork() | pthread / Java Thread |

### 进程的状态转换

```
            创建
             |
             v
         +--------+
         | 就绪态  |<-------+
         +--------+        |
             |              |
        调度/分派           |
             v              |
         +--------+    时间片用完
  -----> | 运行态  | --------+
  |      +--------+
  |           |
  |     I/O请求或等待事件
  |           v
  |      +--------+
  |      | 阻塞态  |
  |      +--------+
  |           |
  |     I/O完成或事件发生
  |           |
  +-----------+
         终止
```

### 进程调度算法

**1. 先来先服务（FCFS）**

```java
// 按到达顺序执行，非抢占式
// 优点：简单公平
// 缺点：短作业可能长时间等待（护航效应）
public class FCFSScheduler {
    public double[] schedule(int[] arrivalTime, int[] burstTime) {
        int n = arrivalTime.length;
        double[] waitingTime = new double[n];
        int currentTime = 0;

        for (int i = 0; i < n; i++) {
            if (currentTime < arrivalTime[i]) {
                currentTime = arrivalTime[i];
            }
            waitingTime[i] = currentTime - arrivalTime[i];
            currentTime += burstTime[i];
        }
        return waitingTime;
    }
}
```

**2. 短作业优先（SJF）**

```java
// 选择执行时间最短的作业
// 优点：平均等待时间最短
// 缺点：可能导致长作业饥饿
public class SJFScheduler {
    public double[] schedule(int[] arrivalTime, int[] burstTime) {
        int n = arrivalTime.length;
        boolean[] done = new boolean[n];
        double[] waitingTime = new double[n];
        int currentTime = 0;
        int completed = 0;

        while (completed < n) {
            int shortest = -1;
            int minBurst = Integer.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                if (!done[i] && arrivalTime[i] <= currentTime && burstTime[i] < minBurst) {
                    minBurst = burstTime[i];
                    shortest = i;
                }
            }

            if (shortest == -1) {
                currentTime++;
                continue;
            }

            waitingTime[shortest] = currentTime - arrivalTime[shortest];
            currentTime += burstTime[shortest];
            done[shortest] = true;
            completed++;
        }
        return waitingTime;
    }
}
```

**3. 时间片轮转（RR）**

```java
// 每个进程执行一个时间片后切换
// 优点：响应时间短，适合交互式系统
// 缺点：上下文切换开销
// 时间片太大 → 退化为 FCFS
// 时间片太小 → 上下文切换过于频繁
```

**4. 多级反馈队列**

```
优先级队列1（时间片=1）→ 优先级队列2（时间片=2）→ 优先级队列3（时间片=4）→ FCFS

新进程进入队列1
如果在时间片内未完成，降级到队列2
以此类推
高优先级队列中的进程优先执行
```

### 进程间通信（IPC）

**1. 管道（Pipe）**

```c
// 匿名管道：只能用于父子进程之间
int pipefd[2];
pipe(pipefd);

// pipefd[0] 是读端
// pipefd[1] 是写端

if (fork() == 0) {
    // 子进程：写入数据
    close(pipefd[0]);
    write(pipefd[1], "hello", 5);
    close(pipefd[1]);
} else {
    // 父进程：读取数据
    close(pipefd[1]);
    char buf[10];
    read(pipefd[0], buf, 5);
    close(pipefd[0]);
}
```

**2. 消息队列**

```java
// Java 中的 BlockingQueue 就是消息队列
BlockingQueue<Message> queue = new LinkedBlockingQueue<>(100);

// 生产者
queue.put(new Message("data"));

// 消费者
Message msg = queue.take();
```

**3. 共享内存**

```java
// Java 中的 MappedByteBuffer（内存映射文件）
RandomAccessFile file = new RandomAccessFile("shared.dat", "rw");
MappedByteBuffer buffer = file.getChannel()
    .map(FileChannel.MapMode.READ_WRITE, 0, 1024);

// 写入
buffer.putInt(42);

// 读取
buffer.flip();
int value = buffer.getInt();
```

**4. 信号量（Semaphore）**

```java
// Java 中的 Semaphore
Semaphore semaphore = new Semaphore(3);  // 最多3个线程同时访问

public void accessResource() throws InterruptedException {
    semaphore.acquire();
    try {
        // 访问共享资源
    } finally {
        semaphore.release();
    }
}
```

## 内存管理

### 虚拟内存

虚拟内存是操作系统提供的一种抽象，让每个进程都以为自己拥有连续的、独立的内存空间。

```
进程A的虚拟地址空间          物理内存
+-------------+           +-------------+
|  0x00000000 |    →      |  物理页 3   |
+-------------+           +-------------+
|  虚拟页 0   |    →      |  物理页 7   |
+-------------+           +-------------+
|  虚拟页 1   |    →      |  物理页 1   |
+-------------+           +-------------+
|  虚拟页 2   |           |  (磁盘)     |
+-------------+           +-------------+
|  ...        |           |  ...        |
+-------------+           +-------------+
```

**虚拟内存的好处**：

1. **进程隔离**：每个进程有独立的地址空间
2. **内存保护**：防止进程访问其他进程的内存
3. **共享内存**：多个进程可以映射相同的物理页
4. **按需加载**：只加载需要的页面到物理内存

### 分页与分段

**分页**：

```
虚拟地址 = 虚拟页号 + 页内偏移

页表：虚拟页号 → 物理页号

例：虚拟地址 0x00001234
假设页面大小 4KB（2^12）
虚拟页号 = 0x00001234 >> 12 = 0x00001 = 1
页内偏移 = 0x00001234 & 0xFFF = 0x234
```

**分段**：

```
虚拟地址 = 段号 + 段内偏移

段表：段号 → (段基址, 段长)

分段更符合程序的逻辑结构（代码段、数据段、栈段）
```

**分页 vs 分段**：

| 特性 | 分页 | 分段 |
|------|------|------|
| 大小 | 固定 | 可变 |
| 透明性 | 对程序员透明 | 程序员可见 |
| 碎片 | 内碎片 | 外碎片 |
| 逻辑关系 | 不反映 | 反映程序逻辑 |

### 页面置换算法

当物理内存不足时，需要选择一个页面换出到磁盘。

**1. 最优算法（Optimal）**

```java
// 替换将来最长时间不会被访问的页面
// 优点：缺页率最低
// 缺点：无法实现（需要预知未来）
```

**2. 先进先出（FIFO）**

```java
public class FIFOPageReplacement {
    public int simulate(int[] pages, int capacity) {
        Queue<Integer> frames = new LinkedList<>();
        int pageFaults = 0;

        for (int page : pages) {
            if (!frames.contains(page)) {
                pageFaults++;
                if (frames.size() >= capacity) {
                    frames.poll();  // 移除最早进入的页面
                }
                frames.offer(page);
            }
        }
        return pageFaults;
    }
}
```

**3. 最近最少使用（LRU）**

```java
public class LRUPageReplacement {
    public int simulate(int[] pages, int capacity) {
        // 使用 LinkedHashMap 实现 LRU
        LinkedHashMap<Integer, Integer> cache = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > capacity;
            }
        };

        int pageFaults = 0;
        for (int page : pages) {
            if (!cache.containsKey(page)) {
                pageFaults++;
            }
            cache.put(page, 1);
        }
        return pageFaults;
    }
}
```

**4. 时钟算法（Clock）**

```
FIFO 的改进版，使用一个访问位：

1. 指针指向下一个候选页面
2. 检查访问位：
   - 为 0：替换该页面
   - 为 1：清除访问位，指针前移
3. 重复步骤 2 直到找到可替换的页面

比 LRU 实现简单，效果接近
```

### 页面置换算法对比

| 算法 | 缺页率 | 实现复杂度 | 是否可实现 |
|------|--------|------------|------------|
| Optimal | 最低 | - | 否 |
| FIFO | 较高 | 简单 | 是 |
| LRU | 较低 | 复杂 | 是 |
| Clock | 接近LRU | 中等 | 是 |

## 死锁

### 死锁的四个必要条件

1. **互斥条件**：资源只能被一个进程使用
2. **占有并等待**：进程持有资源的同时等待其他资源
3. **不可剥夺**：已获得的资源不能被强制剥夺
4. **循环等待**：存在一个进程等待的循环链

### 死锁的预防

```java
// 破坏"占有并等待"：一次性申请所有资源
public class DeadlockPrevention {
    private final Object lock = new Object();

    public void transfer(Account from, Account to, int amount) {
        // 按照固定顺序获取锁，破坏"循环等待"
        Account first = from.getId() < to.getId() ? from : to;
        Account second = from.getId() < to.getId() ? to : from;

        synchronized (first) {
            synchronized (second) {
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}
```

### 死锁的检测与恢复

```java
// 使用资源分配图检测死锁
// 如果图中存在环，且每个资源只有一个实例，则存在死锁

// Java 中可以使用 jstack 检测死锁
// 或者使用 ThreadMXBean
ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
long[] threadIds = tmx.findDeadlockedThreads();
if (threadIds != null) {
    ThreadInfo[] infos = tmx.getThreadInfo(threadIds);
    for (ThreadInfo info : infos) {
        System.out.println("死锁线程: " + info.getThreadName());
        System.out.println("等待锁: " + info.getLockName());
        System.out.println("持有锁: " + info.getLockOwnerName());
    }
}
```

### 死锁、饥饿与活锁

| 现象 | 描述 | 解决方案 |
|------|------|----------|
| 死锁 | 互相等待对方释放资源 | 银行家算法、锁排序 |
| 饥饿 | 某进程长期得不到资源 | 公平锁、老化技术 |
| 活锁 | 不断重试但都无法前进 | 随机退避、优先级 |

理解操作系统的这些核心概念，能帮助你写出更高效的并发程序，更好地理解 JVM 的内存模型和垃圾回收机制。这些知识是后端开发者的重要基础。
