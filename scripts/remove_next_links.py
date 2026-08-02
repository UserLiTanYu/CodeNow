#!/usr/bin/env python3
"""移除所有文章末尾的内嵌'下一篇'导航链接"""
import sys
import re
import mysql.connector

sys.stdout.reconfigure(encoding='utf-8')

DB_CONFIG = {'host': 'localhost', 'port': 3306, 'user': 'root', 'password': '123456', 'database': 'codenow', 'charset': 'utf8mb4'}

# 匹配末尾的导航块（两种格式）：
# 1. \n\n---\n\n**下一篇：[...](/blog/article/...)**
# 2. \n\n---\n\n下一篇：[...](/blog/article/...)
PATTERN = re.compile(r'\n\n---\n\n\*?\*?下一篇：\[.*?\]\(/blog/article/\d+\)\*?\*?\s*$')

def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()

    cursor.execute("SELECT id, content FROM blog_article WHERE is_deleted=0 AND content LIKE '%%下一篇%%' AND content LIKE '%%article/%%'")
    articles = cursor.fetchall()
    print(f"检查 {len(articles)} 篇包含'下一篇'的文章")

    updated = 0
    skipped = 0
    for article_id, content in articles:
        new_content = PATTERN.sub('', content)
        if new_content != content:
            cursor.execute("UPDATE blog_article SET content=%s WHERE id=%s", (new_content, article_id))
            updated += 1
            print(f"  [清理] id={article_id}")
        else:
            skipped += 1

    conn.commit()
    print(f"\n完成：清理 {updated} 篇，跳过 {skipped} 篇（无匹配的导航块）")
    cursor.close()
    conn.close()

if __name__ == '__main__':
    main()
