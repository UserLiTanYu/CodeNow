-- CodeNow V5: Tag ownership, historical backfill, and composite index
-- Idempotent where possible

-- 1. blog_tag.created_by
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog_tag' AND COLUMN_NAME = 'created_by'
);
SET @sql = IF(@column_exists = 0,
    "ALTER TABLE `blog_tag` ADD COLUMN `created_by` BIGINT DEFAULT NULL COMMENT '标签创建人 ID' AFTER `create_time`, ADD KEY `idx_tag_created_by` (`created_by`)",
    "SELECT 'blog_tag.created_by already exists'"
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. Backfill blog_article.author_id NULL -> earliest admin
UPDATE `blog_article` SET `author_id` = (SELECT MIN(id) FROM sys_user WHERE role = 'ADMIN') WHERE `author_id` IS NULL AND EXISTS (SELECT 1 FROM sys_user WHERE role = 'ADMIN');

-- 3. Composite index on blog_article
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog_article' AND INDEX_NAME = 'idx_author_status_time'
);
SET @sql2 = IF(@idx_exists = 0,
    "ALTER TABLE `blog_article` ADD KEY `idx_author_status_time` (`author_id`, `status`, `create_time`)",
    "SELECT 'idx_author_status_time already exists'"
);
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;
