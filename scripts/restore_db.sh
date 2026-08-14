#!/bin/bash
# 完整数据库恢复脚本
set -e
cd /opt/codenow

echo "=== 1. 备份当前生产数据库 ==="
BACKUP_DIR="/opt/codenow-backups/full-before-restore-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
docker compose exec -T mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" --single-transaction --routines --triggers --default-character-set=utf8mb4' \
  > "$BACKUP_DIR/codenow_before_restore.sql"
echo "备份完成: $(du -h "$BACKUP_DIR/codenow_before_restore.sql" | cut -f1)"

echo ""
echo "=== 2. 查看恢复前状态 ==="
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT COUNT(*) as articles FROM blog_article WHERE is_deleted=0; SELECT COUNT(*) as categories FROM blog_category WHERE is_deleted=0; SELECT COUNT(*) as users FROM sys_user WHERE is_deleted=0;"'

echo ""
echo "=== 3. 导入本地数据库 ==="
# 停止后端避免写入冲突
docker compose stop backend
sleep 3

# 导入（init.sql 包含 DROP TABLE + CREATE TABLE + INSERT，会重建整个数据库）
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < /opt/codenow/codenow-backend/sql/init.sql
echo "init.sql 导入完成"

# 执行 Flyway 迁移（补充 init.sql 之后的增量变更）
docker compose start backend
sleep 20
echo "后端已重启，等待 Flyway 迁移..."

# 检查 Flyway 版本
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'

echo ""
echo "=== 4. 查看恢复后状态 ==="
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT COUNT(*) as articles FROM blog_article WHERE is_deleted=0; SELECT COUNT(*) as categories FROM blog_category WHERE is_deleted=0; SELECT COUNT(*) as users FROM sys_user WHERE is_deleted=0;"'

echo ""
echo "=== 5. 验证后端接口 ==="
curl --fail --silent --show-error 'http://127.0.0.1:8080/api/blog/articles?pageNum=1&pageSize=3' 2>&1 | head -c 500
echo ""
