#!/bin/bash
# 只同步 blog_category 和 blog_article 表到生产环境
set -e
cd /opt/codenow

echo "=== 1. 备份当前生产数据 ==="
BACKUP_DIR="/opt/codenow-backups/table-sync-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
docker compose exec -T mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" blog_category blog_article --single-transaction --default-character-set=utf8mb4' \
  > "$BACKUP_DIR/blog_tables_backup.sql"
echo "生产表备份完成: $BACKUP_DIR/blog_tables_backup.sql ($(du -h "$BACKUP_DIR/blog_tables_backup.sql" | cut -f1))"

echo ""
echo "=== 2. 查看导入前状态 ==="
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT COUNT(*) as articles FROM blog_article WHERE is_deleted=0; SELECT COUNT(*) as categories FROM blog_category WHERE is_deleted=0;"'

echo ""
echo "=== 3. 导入新数据 ==="
# 禁用外键检查，清空旧数据，导入新数据
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SET FOREIGN_KEY_CHECKS=0; TRUNCATE TABLE blog_article_tag; TRUNCATE TABLE blog_article; TRUNCATE TABLE blog_category; SET FOREIGN_KEY_CHECKS=1;"'
cat /opt/codenow/codenow-backend/sql/init.sql | docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
echo "init.sql 导入完成"

echo ""
echo "=== 4. 查看导入后状态 ==="
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT COUNT(*) as articles FROM blog_article WHERE is_deleted=0; SELECT COUNT(*) as categories FROM blog_category WHERE is_deleted=0;"'

echo ""
echo "=== 5. 验证分类结构 ==="
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT id, name, parent_id, sort FROM blog_category WHERE is_deleted=0 AND parent_id=0 ORDER BY sort;"'
