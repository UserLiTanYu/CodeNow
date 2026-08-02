#!/usr/bin/env python3
"""Java基础模块重构脚本：清理旧分类/文章 → 创建新分类 → 批量插入文章"""
import sys
import os
import glob
import mysql.connector

sys.stdout.reconfigure(encoding='utf-8')

DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'codenow',
    'charset': 'utf8mb4',
}

AUTHOR_ID = 1  # admin

# 新分类定义: (sort, name, description)
NEW_CATEGORIES = [
    (1,  'Java 概述与环境',          'Java 语言的历史、核心特性、JDK 安装配置与第一个程序'),
    (2,  '基本程序设计结构',          '数据类型、变量、运算符、字符串、控制流与数组'),
    (3,  '对象与类',                 '面向对象基础：类、构造器、方法、包与模块'),
    (4,  '继承与多态',               '继承机制、Object 类、抽象类、多态与类型转换'),
    (5,  '接口、Lambda 与内部类',     '接口设计、函数式接口、Lambda 表达式、方法引用与内部类'),
    (6,  '异常处理',                 '异常体系、try-catch-finally、自定义异常与断言'),
    (7,  '泛型编程',                 '泛型类、泛型方法、通配符与类型擦除'),
    (8,  '集合框架',                 'Collection 体系、List、Set、Map、Queue 与工具类'),
    (9,  '并发编程',                 '线程基础、线程安全、锁、线程池与高级并发工具'),
    (10, 'Java 语法速查',            '基本数据类型、运算符优先级、常用 String/Math/Arrays 方法参考'),
]

# 文章文件映射: (cat_sort_idx, article_sort, filename)
ARTICLE_FILES = []
for cat_idx in range(10):
    for art_idx in range(1, 4):
        ARTICLE_FILES.append((cat_idx, art_idx, f'cat{cat_idx+1}_article{art_idx}.md'))

# 文章元数据: (cat_sort_idx, article_sort, title, summary)
ARTICLE_META = [
    # 0: Java 概述与环境
    (0, 1, 'Java 语言概述与发展历程',
     'Java 的诞生背景、核心设计目标、平台版本（SE/EE/ME）、JDK/JRE/JVM 关系、从 JDK 1.0 到 Java 21 的版本演进'),
    (0, 2, '开发环境搭建与 JDK 安装',
     'Oracle JDK vs OpenJDK 选择、Windows/macOS/Linux 安装 JDK 21、环境变量配置、IDE 选择与 Maven/Gradle 简介'),
    (0, 3, '第一个 Java 程序与编译运行原理',
     'Hello World 编写编译运行全流程、main 方法详解、源码到字节码到 JVM 的执行过程、常见错误排查与编码规范'),
    # 1: 基本程序设计结构
    (1, 1, '数据类型、变量与运算符',
     '8 种基本数据类型、引用类型、变量声明、类型转换规则、字面量、算术/逻辑/位运算符与优先级'),
    (1, 2, '字符串（String）详解',
     'String 不可变性、字符串常量池、常用方法、== vs equals 比较陷阱、StringBuilder/StringBuffer、文本块、字符编码'),
    (1, 3, '控制流与数组',
     'if-else/switch/for/while/for-each 控制流语句、break/continue、数组声明创建遍历、多维数组、Arrays 工具类'),
    # 2: 对象与类
    (2, 1, '类与对象：从现实模型到 Java 代码',
     'OOP 思想、类定义（字段/方法/构造器）、对象创建与引用、this 关键字、方法重载、静态成员、访问控制、final'),
    (2, 2, '包与模块系统',
     '包的概念与命名、import 语句、包访问权限、类路径、Java 9 模块系统（module-info.java）、标准库包'),
    (2, 3, '对象的生命周期与设计原则',
     '对象创建过程与内存分配、GC 概述、栈与堆、equals/hashCode/toString 重写、对象克隆、不可变对象设计'),
    # 3: 继承与多态
    (3, 1, '继承机制与 super 关键字',
     '继承概念、extends/super 用法、构造器调用链、方法覆盖规则、@Override、protected、单继承限制'),
    (3, 2, 'Object 类与通用方法',
     'Object 根类地位、equals/hashCode/toString/getClass/clone/finalize 详解、Objects 工具类'),
    (3, 3, '抽象类、多态与类型转换',
     '抽象类与抽象方法、多态动态绑定、向上/向下转型、instanceof 模式匹配、密封类、组合优于继承'),
    # 4: 接口、Lambda 与内部类
    (4, 1, '接口设计与默认方法',
     '接口定义与实现、默认方法与冲突解决、接口静态/私有方法、函数式接口、标准库常见函数式接口'),
    (4, 2, 'Lambda 表达式与方法引用',
     'Lambda 语法与类型、变量捕获、四种方法引用、Lambda vs 匿名内部类、Comparator 链式比较'),
    (4, 3, '内部类与匿名类',
     '四种内部类类型、成员/静态/局部/匿名内部类、内部类与 Lambda 对比、Builder 模式、枚举类型'),
    # 5: 异常处理
    (5, 1, '异常体系与 try-catch-finally',
     'Throwable 体系、受检 vs 非受检异常、try-catch-finally、try-with-resources、常见标准异常'),
    (5, 2, '自定义异常与异常设计',
     '自定义异常创建、throw vs throws、异常链、异常处理最佳实践（早抛出晚捕获）'),
    (5, 3, '断言与日志',
     'assert 断言、JUL 日志、SLF4J/Logback 体系、日志级别与格式、日志最佳实践'),
    # 6: 泛型编程
    (6, 1, '泛型类与泛型方法',
     '泛型的意义、泛型类/接口/方法定义、类型参数命名、有界类型参数、泛型与继承'),
    (6, 2, '通配符与类型擦除',
     '上界/下界/无界通配符、PECS 原则、类型擦除机制与影响、桥接方法、泛型局限性'),
    (6, 3, '泛型实战与设计模式',
     '泛型集合使用、Comparable/Comparator 泛型、类型安全异构容器、通用 R<T>/Page<T> 封装'),
    # 7: 集合框架
    (7, 1, 'Collection 体系与 List',
     '集合框架概览、接口层次、ArrayList vs LinkedList、常用方法、不可变 List、迭代器模式'),
    (7, 2, 'Set、Map 与哈希原理',
     'HashSet/LinkedHashSet/TreeSet、HashMap/LinkedHashMap/TreeMap、哈希原理与 HashMap put 流程'),
    (7, 3, '队列、工具类与集合选型',
     'ArrayDeque/PriorityQueue、Collections 工具类、集合选型决策表、Java 9+ 不可变集合 API'),
    # 8: 并发编程
    (8, 1, '线程基础与生命周期',
     '进程 vs 线程、三种线程创建方式、线程生命周期与状态、Thread 常用方法、中断机制、守护线程'),
    (8, 2, '线程安全、锁与同步机制',
     '竞态条件、synchronized（方法/块）、死锁、volatile、Lock/ReentrantLock、原子变量、ThreadLocal'),
    (8, 3, '线程池与高级并发工具',
     'Executor 框架、ThreadPoolExecutor 参数与执行流程、拒绝策略、CompletableFuture 链式调用、Fork/Join'),
    # 9: Java 语法速查
    (9, 1, '基本数据类型与类型转换速查',
     '8 种基本类型一览表、浮点精度问题、自动装箱缓存池、类型转换规则表、var 推断'),
    (9, 2, '运算符优先级与控制流速查',
     '运算符优先级完整表、控制流语句速查（if/switch/for/while）、常见运算符陷阱'),
    (9, 3, 'String、Math、Arrays 常用方法速查',
     'String 全方法分类表、StringBuilder、Math 工具类、Arrays 工具类、Objects 工具类、正则表达式基础'),
]


def get_connection():
    return mysql.connector.connect(**DB_CONFIG)


def cleanup(conn):
    """软删除旧的 Java 基础相关分类和文章"""
    cursor = conn.cursor()
    old_cat_ids = [8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 63, 64]
    root_cat_ids = [1, 58]

    placeholders = ','.join(['%s'] * len(old_cat_ids))
    cursor.execute(f"UPDATE blog_article SET is_deleted=1 WHERE category_id IN ({placeholders})", old_cat_ids)
    print(f"  软删除文章: {cursor.rowcount} 条")

    cursor.execute(f"UPDATE blog_category SET is_deleted=1 WHERE id IN ({placeholders})", old_cat_ids)
    print(f"  软删除子分类: {cursor.rowcount} 条")

    placeholders2 = ','.join(['%s'] * len(root_cat_ids))
    cursor.execute(f"UPDATE blog_category SET is_deleted=1 WHERE id IN ({placeholders2})", root_cat_ids)
    print(f"  软删除根分类: {cursor.rowcount} 条")

    conn.commit()
    cursor.close()


def create_categories(conn):
    """创建新的分类结构，返回 {cat_sort_idx: category_id} 映射"""
    cursor = conn.cursor()
    cat_map = {}

    cursor.execute(
        "INSERT INTO blog_category (name, parent_id, sort, create_time, update_time, is_deleted) "
        "VALUES (%s, 0, 1, NOW(), NOW(), 0)",
        ('Java 基础',)
    )
    root_id = cursor.lastrowid
    print(f"  根分类 'Java 基础' id={root_id}")

    for idx, (sort, name, desc) in enumerate(NEW_CATEGORIES):
        cursor.execute(
            "INSERT INTO blog_category (name, parent_id, sort, create_time, update_time, is_deleted) "
            "VALUES (%s, %s, %s, NOW(), NOW(), 0)",
            (name, root_id, sort)
        )
        cat_id = cursor.lastrowid
        cat_map[idx] = cat_id
        print(f"  子分类 '{name}' id={cat_id}")

    conn.commit()
    cursor.close()
    return cat_map


def insert_articles(conn, cat_map, articles_dir):
    """从 Markdown 文件读取内容并批量插入文章"""
    cursor = conn.cursor()
    inserted = 0
    skipped = []

    for cat_idx, art_idx, title, summary in ARTICLE_META:
        cat_id = cat_map[cat_idx]
        filename = f'cat{cat_idx+1}_article{art_idx}.md'
        filepath = os.path.join(articles_dir, filename)

        if not os.path.exists(filepath):
            skipped.append(filename)
            continue

        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read().strip()

        if not content:
            skipped.append(filename)
            continue

        cursor.execute(
            "INSERT INTO blog_article (title, content, summary, category_id, author_id, status, sort, view_count, create_time, update_time, is_deleted) "
            "VALUES (%s, %s, %s, %s, %s, 1, %s, 0, NOW(), NOW(), 0)",
            (title, content, summary, cat_id, AUTHOR_ID, art_idx)
        )
        inserted += 1

    conn.commit()
    cursor.close()
    print(f"  插入文章: {inserted} 篇")
    if skipped:
        print(f"  跳过（文件不存在或为空）: {skipped}")


def verify(conn):
    """验证插入结果"""
    cursor = conn.cursor()
    cursor.execute("""
        SELECT c.id, c.name, COUNT(a.id) as article_count
        FROM blog_category c
        LEFT JOIN blog_article a ON a.category_id = c.id AND a.is_deleted = 0
        WHERE c.is_deleted = 0 AND c.parent_id != 0
        GROUP BY c.id, c.name
        ORDER BY c.sort
    """)
    print("\n  验证结果:")
    print(f"  {'ID':<5} {'分类名':<25} {'文章数':<6}")
    print(f"  {'-'*40}")
    for row in cursor.fetchall():
        print(f"  {row[0]:<5} {row[1]:<25} {row[2]:<6}")
    cursor.close()


def main():
    articles_dir = os.path.join(os.path.dirname(__file__), 'articles')

    print("=" * 50)
    print("Java 基础模块重构")
    print("=" * 50)

    conn = get_connection()

    print("\n[1/4] 清理旧数据...")
    cleanup(conn)

    print("\n[2/4] 创建新分类...")
    cat_map = create_categories(conn)

    print("\n[3/4] 插入文章...")
    insert_articles(conn, cat_map, articles_dir)

    print("\n[4/4] 验证结果...")
    verify(conn)

    conn.close()
    print("\n完成！")


if __name__ == '__main__':
    main()
