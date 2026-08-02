# SQL 查询进阶与子查询

在日常开发中，简单的 `SELECT`、`WHERE`、`GROUP BY` 已经能满足大部分需求，但面对复杂业务逻辑——比如取每组最新记录、计算同比环比、层级递归查询——就需要更高级的 SQL 技巧。本文深入讲解窗口函数、公用表表达式（CTE）、各类子查询及其性能特征。

## 窗口函数基础

窗口函数（Window Function）在不改变结果集行数的前提下，对"窗口"内的数据进行聚合或排名计算。与 `GROUP BY` 不同，窗口函数保留每一行的细节。

### 基本语法

```sql
函数名() OVER (
    [PARTITION BY 分区列]
    [ORDER BY 排序列 [ASC|DESC]]
    [ROWS BETWEEN ... AND ...]
)
```

`PARTITION BY` 类似 `GROUP BY`，但不合并行。`ORDER BY` 定义窗口内的排序。帧子句（`ROWS BETWEEN`）进一步限定参与计算的行范围。

### ROW_NUMBER / RANK / DENSE_RANK

三个排名函数的区别是处理并列值的方式不同：

```sql
SELECT
    student_name,
    score,
    ROW_NUMBER() OVER (ORDER BY score DESC) AS row_num,
    RANK()       OVER (ORDER BY score DESC) AS rank_num,
    DENSE_RANK() OVER (ORDER BY score DESC) AS dense_rank_num
FROM exam_scores;
```

假设分数为 100、98、98、95：

| student_name | score | ROW_NUMBER | RANK | DENSE_RANK |
|---|---|---|---|---|
| 张三 | 100 | 1 | 1 | 1 |
| 李四 | 98 | 2 | 2 | 2 |
| 王五 | 98 | 3 | 2 | 2 |
| 赵六 | 95 | 4 | 4 | 3 |

`ROW_NUMBER` 始终递增，`RANK` 并列后跳号，`DENSE_RANK` 并列不跳号。实际业务中取每组 Top-N 通常用 `ROW_NUMBER()`：

```sql
SELECT * FROM (
    SELECT
        department_id,
        employee_name,
        salary,
        ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC) AS rn
    FROM employees
) t
WHERE rn <= 3;
```

### LEAD 与 LAG

`LAG(col, n)` 取前第 n 行的值，`LEAD(col, n)` 取后第 n 行的值，常用于计算环比或相邻行差值：

```sql
SELECT
    month,
    revenue,
    LAG(revenue, 1)  OVER (ORDER BY month) AS prev_month,
    revenue - LAG(revenue, 1) OVER (ORDER BY month) AS mom_growth,
    ROUND(
        (revenue - LAG(revenue, 1) OVER (ORDER BY month))
        / LAG(revenue, 1) OVER (ORDER BY month) * 100, 2
    ) AS mom_pct
FROM monthly_sales;
```

### 聚合窗口函数

`SUM`、`AVG`、`COUNT` 也能作窗口函数，配合帧子句实现滑动平均、累计求和：

```sql
SELECT
    order_date,
    amount,
    SUM(amount) OVER (ORDER BY order_date ROWS UNBOUNDED PRECEDING) AS running_total,
    AVG(amount) OVER (ORDER BY order_date ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS moving_avg_7d
FROM daily_orders;
```

`UNBOUNDED PRECEDING` 表示从分区第一行开始，`CURRENT ROW` 是当前行。常用帧定义：

| 帧子句 | 含义 |
|---|---|
| `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` | 从头到当前行 |
| `ROWS BETWEEN 6 PRECEDING AND CURRENT ROW` | 当前行及前 6 行 |
| `ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING` | 当前行到末尾 |
| `ROWS BETWEEN 3 PRECEDING AND 3 FOLLOWING` | 前 3 行到后 3 行 |

## 公用表表达式（CTE）

CTE 用 `WITH` 关键字定义临时命名结果集，让复杂查询变得可读。

### 非递归 CTE

```sql
WITH high_value_customers AS (
    SELECT customer_id, SUM(amount) AS total_spent
    FROM orders
    WHERE order_date >= '2025-01-01'
    GROUP BY customer_id
    HAVING SUM(amount) > 10000
),
customer_details AS (
    SELECT c.customer_id, c.name, c.email, h.total_spent
    FROM customers c
    JOIN high_value_customers h ON c.customer_id = h.customer_id
)
SELECT * FROM customer_details ORDER BY total_spent DESC;
```

CTE 的优势：逻辑分层清晰、可复用（同一查询中多次引用）、便于调试（可单独执行某个 CTE 检查结果）。

MySQL 8.0+、PostgreSQL、SQL Server 均支持 CTE。MySQL 5.7 不支持，需要改写为子查询。

### CTE 与子查询的性能差异

在大多数现代数据库中，CTE 是"优化器可穿透的"，即优化器会将 CTE 内联到主查询中，与直接写子查询的执行计划相同。但 PostgreSQL 的 CTE 默认是"优化围栏"（Optimization Fence），CTE 内部的结果会物化，不会被外层条件下推。PostgreSQL 12+ 可用 `AS MATERIALIZED` / `AS NOT MATERIALIZED` 手动控制。

## 递归 CTE

递归 CTE 是处理层级数据（组织架构、分类目录、评论回复树）的利器。

### 基本结构

```sql
WITH RECURSIVE cte_name AS (
    -- 锚点成员（Anchor）：初始查询
    SELECT ... FROM table WHERE 初始条件
    UNION ALL
    -- 递归成员（Recursive）：引用自身
    SELECT ... FROM table JOIN cte_name ON 递归条件
)
SELECT * FROM cte_name;
```

### 组织架构递归查询

假设有一张 `employees` 表，`manager_id` 指向直属上级：

```sql
WITH RECURSIVE org_tree AS (
    -- 锚点：顶级管理者（无上级）
    SELECT
        employee_id,
        employee_name,
        manager_id,
        1 AS level,
        CAST(employee_name AS CHAR(1000)) AS path
    FROM employees
    WHERE manager_id IS NULL

    UNION ALL

    -- 递归：逐层向下
    SELECT
        e.employee_id,
        e.employee_name,
        e.manager_id,
        t.level + 1,
        CONCAT(t.path, ' > ', e.employee_name)
    FROM employees e
    JOIN org_tree t ON e.manager_id = t.employee_id
)
SELECT
    employee_id,
    employee_name,
    level,
    path
FROM org_tree
ORDER BY path;
```

### 递归深度限制

MySQL 默认递归深度为 1000，可通过 `cte_max_recursion_depth` 调整：

```sql
SET cte_max_recursion_depth = 5000;
```

SQL Server 用 `OPTION (MAXRECURSION n)`，默认 100，设为 0 表示无限制。

### 递归 CTE 生成序列

递归 CTE 还能生成日期序列或数字序列：

```sql
WITH RECURSIVE date_series AS (
    SELECT CAST('2025-01-01' AS DATE) AS dt
    UNION ALL
    SELECT DATE_ADD(dt, INTERVAL 1 DAY)
    FROM date_series
    WHERE dt < '2025-12-31'
)
SELECT dt FROM date_series;
```

## 标量子查询

标量子查询返回单行单列，可出现在 `SELECT`、`WHERE`、`HAVING` 等位置。

```sql
SELECT
    d.department_name,
    (SELECT COUNT(*) FROM employees e WHERE e.department_id = d.department_id) AS emp_count,
    (SELECT AVG(salary) FROM employees e WHERE e.department_id = d.department_id) AS avg_salary
FROM departments d;
```

标量子查询每执行一次外层行就要执行一次子查询，数据量大时性能很差。更好的写法是用 `JOIN`：

```sql
SELECT
    d.department_name,
    COUNT(e.employee_id) AS emp_count,
    AVG(e.salary) AS avg_salary
FROM departments d
LEFT JOIN employees e ON e.department_id = d.department_id
GROUP BY d.department_id, d.department_name;
```

## 关联子查询

关联子查询（Correlated Subquery）引用了外层查询的列，无法独立执行。

```sql
SELECT e.*
FROM employees e
WHERE e.salary > (
    SELECT AVG(e2.salary)
    FROM employees e2
    WHERE e2.department_id = e.department_id
);
```

这条查询找出每个部门中薪资高于部门平均值的员工。执行过程：外层每取一行，内层用该行的 `department_id` 计算一次平均薪资。

关联子查询还可以用在 `EXISTS` 和 `NOT EXISTS` 中，这是处理"存在性判断"的标准写法。

## EXISTS vs IN 性能对比

`IN` 和 `EXISTS` 是两种常用的子查询过滤方式，选择不当可能导致性能差异。

### 写法对比

```sql
-- 使用 IN
SELECT * FROM orders
WHERE customer_id IN (SELECT customer_id FROM vip_customers);

-- 使用 EXISTS
SELECT o.* FROM orders o
WHERE EXISTS (
    SELECT 1 FROM vip_customers v WHERE v.customer_id = o.customer_id
);
```

### 执行逻辑差异

- **IN**：先执行子查询，将结果集加载到临时表或内存中，再对外层表做全表扫描逐行匹配。适合子查询结果集小的场景。
- **EXISTS**：对外层表每行执行一次关联子查询，找到一行即返回 `TRUE`。适合外层表小、子查询表大且有索引的场景。

### 优化器改写

现代优化器（MySQL 8.0+、PostgreSQL）通常会自动将 `IN` 改写为半连接（Semi-Join），性能与 `EXISTS` 接近。但以下情况仍有差异：

```sql
-- 当子查询结果集包含 NULL 时，IN 的行为可能出乎意料
SELECT * FROM t1 WHERE col IN (SELECT col FROM t2);
-- 如果 t2.col 包含 NULL，IN 不会返回错误，但 NOT IN 会排除所有行
-- NOT IN (1, 2, NULL) 等价于 col != 1 AND col != 2 AND col != NULL
-- 最后一个条件永远为 FALSE，导致整个表达式为 FALSE
```

**最佳实践**：
- 优先用 `EXISTS` 处理存在性判断
- `NOT IN` 要确保子查询无 NULL，或改用 `NOT EXISTS`
- 数据量大时用 `EXPLAIN` 确认执行计划

## UNION vs UNION ALL

`UNION` 和 `UNION ALL` 都能合并多个查询结果，但行为不同。

### 核心区别

```sql
-- UNION：去重（隐含 DISTINCT 操作）
SELECT city FROM customers
UNION
SELECT city FROM suppliers;

-- UNION ALL：保留所有行，包含重复
SELECT city FROM customers
UNION ALL
SELECT city FROM suppliers;
```

| 特性 | UNION | UNION ALL |
|---|---|---|
| 去重 | 是 | 否 |
| 排序 | 通常需要额外排序 | 无额外排序 |
| 性能 | 较慢（去重需要临时表或排序） | 较快 |
| 使用场景 | 需要唯一结果 | 确定无重复或允许重复 |

### 性能分析

`UNION` 的去重操作会使用临时表（Using temporary）和文件排序（Using filesort），当结果集大时开销显著。如果业务上确定不会有重复数据，或允许重复存在，应优先使用 `UNION ALL`。

### 多表合并的实际应用

```sql
-- 将不同类型的通知合并到统一的时间线
SELECT created_at, '系统通知' AS type, content
FROM system_notifications
WHERE user_id = 1001
UNION ALL
SELECT created_at, '评论回复' AS type, content
FROM comment_replies
WHERE target_user_id = 1001
UNION ALL
SELECT created_at, '私信' AS type, content
FROM private_messages
WHERE receiver_id = 1001
ORDER BY created_at DESC
LIMIT 20;
```

## 子查询优化建议

在编写复杂查询时，子查询的写法直接影响执行效率。

### 避免在 WHERE 中使用函数包裹的子查询

```sql
-- 不推荐：子查询在循环中反复执行
SELECT * FROM orders
WHERE order_date = (SELECT MAX(order_date) FROM orders);

-- 推荐：先计算再 JOIN
SELECT o.* FROM orders o
JOIN (SELECT MAX(order_date) AS max_date FROM orders) m
ON o.order_date = m.max_date;
```

### 子查询物化

MySQL 优化器可能将子查询物化（Materialization）为临时表，这对大结果集的 `IN` 子查询有优化效果。可通过 `EXPLAIN` 查看是否触发物化：

```sql
EXPLAIN SELECT * FROM orders
WHERE customer_id IN (SELECT customer_id FROM vip_customers);
```

在执行计划中，如果看到 `subquery` 表的 `type` 为 `ALL` 且 `Extra` 包含 `Using materialized`，说明优化器选择了物化策略。

### 派生表合并（Derived Table Merge）

MySQL 8.0 支持将简单派生表合并到外层查询，避免创建临时表：

```sql
-- MySQL 8.0 会将这个派生表合并
SELECT * FROM (
    SELECT employee_id, salary FROM employees WHERE department_id = 5
) AS dept5
WHERE salary > 10000;

-- 等价于合并后的执行计划
SELECT employee_id, salary
FROM employees
WHERE department_id = 5 AND salary > 10000;
```

但包含 `GROUP BY`、`DISTINCT`、`UNION`、聚合函数的派生表无法合并，会被物化为临时表。

## 实战：综合案例

综合运用窗口函数、CTE 和子查询解决一个复杂业务问题：找出每个月销售额排名前 3 的产品，并计算该产品当月占总销售额的百分比。

```sql
WITH monthly_product_sales AS (
    SELECT
        DATE_FORMAT(order_date, '%Y-%m') AS month,
        product_id,
        SUM(amount) AS product_sales
    FROM order_items oi
    JOIN orders o ON oi.order_id = o.order_id
    WHERE order_date >= '2025-01-01'
    GROUP BY DATE_FORMAT(order_date, '%Y-%m'), product_id
),
ranked_sales AS (
    SELECT
        month,
        product_id,
        product_sales,
        SUM(product_sales) OVER (PARTITION BY month) AS month_total,
        ROW_NUMBER() OVER (PARTITION BY month ORDER BY product_sales DESC) AS rn
    FROM monthly_product_sales
)
SELECT
    rs.month,
    p.product_name,
    rs.product_sales,
    ROUND(rs.product_sales / rs.month_total * 100, 2) AS pct_of_month
FROM ranked_sales rs
JOIN products p ON rs.product_id = p.product_id
WHERE rs.rn <= 3
ORDER BY rs.month, rs.rn;
```

这个查询展示了 CTE 分层数据准备、窗口函数排名与聚合、最终 `JOIN` 取产品名称的典型模式。
