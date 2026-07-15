-- 中等优先级数据库变更回滚脚本。
-- 警告：仅用于回滚到不识别 is_deleted 字段的旧应用版本。

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'blog_article_tag'
      AND COLUMN_NAME = 'is_deleted'
);
SET @rollback_sql = IF(
    @column_exists = 1,
    'ALTER TABLE `blog_article_tag` DROP COLUMN `is_deleted`',
    'SELECT ''rollback already applied'' AS message'
);
PREPARE rollback_statement FROM @rollback_sql;
EXECUTE rollback_statement;
DEALLOCATE PREPARE rollback_statement;
