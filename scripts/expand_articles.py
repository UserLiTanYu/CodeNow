#!/usr/bin/env python3
"""批量插入扩充文章：先创建占位记录，再从文件读取内容更新"""
import sys
import os
import mysql.connector

sys.stdout.reconfigure(encoding='utf-8')

DB_CONFIG = {'host': 'localhost', 'port': 3306, 'user': 'root', 'password': '123456', 'database': 'codenow', 'charset': 'utf8mb4'}

# (category_id, sort, title, summary, filename)
NEW_ARTICLES = [
    # 基本程序设计结构 +2 (cat_id=69)
    (69, 4, '大数值运算与格式化输出', 'BigInteger/BigDecimal 精确运算、printf 格式化、DecimalFormat 数字格式化', 'cat2_article4.md'),
    (69, 5, 'Java 日期时间 API', 'Date/Calendar 旧 API、java.time 新 API（LocalDate/LocalDateTime/Duration/Period）、格式化与解析', 'cat2_article5.md'),
    # 对象与类 +2 (cat_id=70)
    (70, 4, '构造器详解与对象初始化顺序', '默认构造器、参数化构造器、构造器重载链、初始化块执行顺序、对象创建的完整过程', 'cat3_article4.md'),
    (70, 5, 'static 关键字与单例模式', '静态字段/方法/初始化块深入、工厂方法、单例模式的多种实现与线程安全', 'cat3_article5.md'),
    # 继承与多态 +2 (cat_id=71)
    (71, 4, 'Class 类与反射机制', 'Class<T> 获取方式、反射创建对象/调用方法/访问字段、反射的性能与安全考量', 'cat4_article4.md'),
    (71, 5, '枚举类型详解', 'enum 定义与构造器、枚举方法与字段、EnumSet/EnumMap、枚举实现策略模式', 'cat4_article5.md'),
    # 接口/Lambda/内部类 +2 (cat_id=72)
    (72, 4, '常用函数式接口与 Comparator 排序', 'Predicate/Function/Supplier/Consumer 详解、Comparator 链式比较、自定义排序实践', 'cat5_article4.md'),
    (72, 5, '代理模式与动态代理', '静态代理、JDK 动态代理（InvocationHandler）、CGLIB 代理、AOP 中的代理应用', 'cat5_article5.md'),
    # 异常处理 +1 (cat_id=73)
    (73, 4, 'try-with-resources 与异常链详解', 'AutoCloseable 接口、多资源关闭顺序、异常链（Exception Chaining）、抑制异常', 'cat6_article4.md'),
    # 泛型编程 +3 (cat_id=74)
    (74, 4, '通配符详解与 PECS 原则', '上界/下界/无界通配符对比、PECS 原则实战、通配符捕获、Collections.copy 示例', 'cat7_article4.md'),
    (74, 5, '类型擦除与桥接方法深入', '类型擦除的编译过程、原始类型替换规则、桥接方法的生成机制、泛型的运行时限制', 'cat7_article5.md'),
    (74, 6, '泛型数组与类型安全异构容器', '泛型数组的创建限制与解决方案、类型安全异构容器模式（Class<T> 作键）、泛型与反射配合', 'cat7_article6.md'),
    # 集合框架 +3 (cat_id=75)
    (75, 4, 'HashMap 源码分析与哈希冲突', 'HashMap 内部结构（数组+链表+红黑树）、put/get 流程、扩容机制、hash() 扰动函数、容量为 2 的幂的原因', 'cat8_article4.md'),
    (75, 5, 'TreeSet、TreeMap 与排序集合', '红黑树原理概述、自然排序 Comparable、定制排序 Comparator、NavigableMap/NavigableSet 接口', 'cat8_article5.md'),
    (75, 6, '并发集合与遗留集合类', 'ConcurrentHashMap 分段锁/CAS、CopyOnWriteArrayList、BlockingQueue、Properties/Vector/Hashtable', 'cat8_article6.md'),
    # 并发编程 +5 (cat_id=76)
    (76, 4, 'synchronized 与 volatile 详解', '对象监视器、同步方法/同步块、wait/notify/notifyAll、volatile 可见性与禁止重排、内存屏障', 'cat9_article4.md'),
    (76, 5, 'Lock 接口与 Condition', 'ReentrantLock 用法、tryLock 超时、ReadWriteLock 读写锁、Condition 精确唤醒、synchronized vs Lock 对比', 'cat9_article5.md'),
    (76, 6, '原子变量与 CAS 原理', 'AtomicInteger/AtomicLong/AtomicReference、CAS（Compare-And-Swap）原理、ABA 问题与 AtomicStampedReference', 'cat9_article6.md'),
    (76, 7, 'CompletableFuture 异步编程', 'supplyAsync/runAsync 创建、thenApply/thenAccept/thenRun 链式调用、thenCompose/thenCombine 组合、allOf/anyOf、异常处理', 'cat9_article7.md'),
    (76, 8, 'Fork/Join 框架与并发工具类', '分治思想、RecursiveTask/RecursiveAction、工作窃取、CountDownLatch/CyclicBarrier/Semaphore、ExecutorService 最佳实践', 'cat9_article8.md'),
    # 语法速查 +1 (cat_id=77)
    (77, 4, '正则表达式与常见异常速查', 'Pattern/Matcher 用法、正则语法速查表、Java 常见异常速查（产生原因与解决方案）', 'cat10_article4.md'),
]

AUTHOR_ID = 1

def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    articles_dir = os.path.join(os.path.dirname(__file__), 'articles')

    inserted = 0
    for cat_id, sort, title, summary, filename in NEW_ARTICLES:
        filepath = os.path.join(articles_dir, filename)
        if not os.path.exists(filepath):
            print(f"  [SKIP] {filename} 不存在")
            continue
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read().strip()
        if not content:
            print(f"  [SKIP] {filename} 为空")
            continue
        cursor.execute(
            "INSERT INTO blog_article (title, content, summary, category_id, author_id, status, sort, view_count, create_time, update_time, is_deleted) "
            "VALUES (%s, %s, %s, %s, %s, 1, %s, 0, NOW(), NOW(), 0)",
            (title, content, summary, cat_id, AUTHOR_ID, sort)
        )
        inserted += 1
        print(f"  [OK] {title}")

    conn.commit()
    print(f"\n共插入 {inserted} 篇扩充文章")

    # 验证
    cursor.execute("""
        SELECT c.name, COUNT(a.id) as cnt
        FROM blog_category c
        LEFT JOIN blog_article a ON a.category_id = c.id AND a.is_deleted = 0
        WHERE c.is_deleted = 0 AND c.parent_id = 67
        GROUP BY c.id, c.name, c.sort
        ORDER BY c.sort
    """)
    print(f"\n{'分类':<25} {'文章数':<6}")
    print("-" * 35)
    for name, cnt in cursor.fetchall():
        print(f"{name:<25} {cnt:<6}")

    cursor.close()
    conn.close()

if __name__ == '__main__':
    main()
