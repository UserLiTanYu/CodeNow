-- Idempotent upgrade for existing databases (2026-07-15).
-- Adds the logical-delete flag only when it is absent.

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'blog_article_tag'
      AND COLUMN_NAME = 'is_deleted'
);
SET @migration_sql = IF(
    @column_exists = 0,
    'ALTER TABLE `blog_article_tag` ADD COLUMN `is_deleted` TINYINT DEFAULT 0 AFTER `tag_id`',
    'SELECT ''migration already applied'' AS message'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
