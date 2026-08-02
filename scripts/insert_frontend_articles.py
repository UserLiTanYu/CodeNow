#!/usr/bin/env python3
"""批量插入前端开发扩充文章"""
import sys
import os
import mysql.connector

sys.stdout.reconfigure(encoding='utf-8')

DB_CONFIG = {'host': 'localhost', 'port': 3306, 'user': 'root', 'password': '123456', 'database': 'codenow', 'charset': 'utf8mb4'}

# (category_id, sort, title, summary, filename)
NEW_ARTICLES = [
    # HTML 与 CSS (id=26) +2
    (26, 4, 'CSS 预处理器与工程化样式', 'Sass/SCSS、Less、CSS Modules、PostCSS、Tailwind CSS、BEM 命名、CSS 变量与样式方案选择', 'fe_html4.md'),
    (26, 5, 'CSS 动画、过渡与性能优化', 'transition/animation、transform 变换、GPU 加速、回流重绘优化、content-visibility', 'fe_html5.md'),
    # JavaScript 基础 (id=27) +2
    (27, 4, '闭包、作用域与执行上下文', '执行上下文与执行栈、作用域链、闭包原理与应用、this 绑定规则、箭头函数 this', 'fe_js4.md'),
    (27, 5, '事件循环、宏任务与微任务', '事件循环机制、宏任务微任务执行顺序、requestAnimationFrame、Node.js 事件循环差异', 'fe_js5.md'),
    # TypeScript (id=28) +2
    (28, 4, 'TypeScript 工具类型与高级模式', '内置工具类型、自定义工具类型、条件类型、infer、映射类型、模板字面量类型', 'fe_ts4.md'),
    (28, 5, '装饰器与 TypeScript 编译配置', '类/方法/属性/参数装饰器、reflect-metadata、tsconfig 核心配置、项目引用', 'fe_ts5.md'),
    # Vue 3 (id=29) +3
    (29, 4, 'Vue 3 组合式函数（Composables）与高级模式', 'Composables 概念与 Mixins 对比、自定义组合式函数、provide/inject 封装、异步组件与 Teleport', 'fe_vue4.md'),
    (29, 5, 'Vue Router 与 Pinia 状态管理深入', '路由守卫、动态路由、懒加载、Pinia 核心概念与插件、SSR 状态管理', 'fe_vue5.md'),
    # Element Plus (id=30) +2
    (30, 4, 'Element Plus 表格进阶与大数据处理', '多级表头、合并行列、虚拟滚动表格、表格内编辑、导出 Excel、通用表格封装', 'fe_ep4.md'),
    (30, 5, '组件封装、主题定制与按需加载', '二次封装原则、主题定制、暗色模式、按需导入、国际化、组件文档', 'fe_ep5.md'),
    # React (id=31) +2
    (31, 4, 'React 表单处理与数据请求', '受控/非受控组件、React Hook Form、Zod 校验、React Query、乐观更新、错误边界', 'fe_react4.md'),
    (31, 5, 'React 状态管理与性能优化', 'Context/Redux/Zustand/Jotai 对比、React.memo、useMemo/useCallback、虚拟列表、代码分割', 'fe_react5.md'),
    # 前端工程化 (id=32) +3
    (32, 4, 'Monorepo、包管理与构建优化', 'pnpm workspace、Turborepo、Vite 构建优化、资源压缩、Bundle 分析、CDN 加速', 'fe_eng4.md'),
    (32, 5, '前端监控、错误追踪与性能度量', 'Web Vitals 指标、错误捕获与 Source Map、Sentry 集成、自定义埋点、A/B 测试', 'fe_eng5.md'),
    (32, 6, '前端 CI/CD、Docker 部署与 Nginx 配置', 'GitHub Actions 流水线、Docker 多阶段构建、Nginx SPA 配置、安全头、蓝绿部署', 'fe_eng6.md'),
    (32, 7, '前端安全与浏览器存储', 'XSS/CSRF/点击劫持防护、localStorage/Cookie/IndexedDB 对比、Token 存储、依赖安全', 'fe_eng7.md'),
]

AUTHOR_ID = 1

def main():
    articles_dir = os.path.join(os.path.dirname(__file__), 'articles')
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()

    inserted = 0
    skipped = []
    for cat_id, sort, title, summary, filename in NEW_ARTICLES:
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
            (title, content, summary, cat_id, AUTHOR_ID, sort)
        )
        inserted += 1
        print(f"  [OK] {title}")

    conn.commit()
    print(f"\n共插入 {inserted} 篇文章")
    if skipped:
        print(f"跳过: {skipped}")

    # 验证
    cursor.execute("""
        SELECT c.name, COUNT(a.id) as cnt
        FROM blog_category c
        LEFT JOIN blog_article a ON a.category_id = c.id AND a.is_deleted = 0
        WHERE c.is_deleted = 0 AND c.parent_id = 3
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
