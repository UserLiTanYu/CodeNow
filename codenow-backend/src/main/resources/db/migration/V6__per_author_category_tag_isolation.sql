-- CodeNow V6: Per-author category and tag isolation

-- 1. Add author_id to blog_category
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog_category' AND COLUMN_NAME = 'author_id'
);
SET @sql = IF(@column_exists = 0,
    "ALTER TABLE `blog_category` ADD COLUMN `author_id` BIGINT DEFAULT NULL COMMENT '作者ID（属于哪个作者）' AFTER `description`, ADD KEY `idx_category_author` (`author_id`)",
    "SELECT 'blog_category.author_id already exists'"
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. Backfill existing categories to admin (earliest admin user)
UPDATE `blog_category` SET `author_id` = (SELECT MIN(id) FROM sys_user WHERE role = 'ADMIN') WHERE `author_id` IS NULL AND EXISTS (SELECT 1 FROM sys_user WHERE role = 'ADMIN');

-- 3. Drop old global unique constraint on name (too restrictive for per-author isolation)
SET @old_idx = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog_category' AND INDEX_NAME = 'uk_name'
);
SET @sql_drop = IF(@old_idx = 0,
    "SELECT 'uk_name already removed'",
    "ALTER TABLE `blog_category` DROP INDEX `uk_name`"
);
PREPARE s_drop FROM @sql_drop; EXECUTE s_drop; DEALLOCATE PREPARE s_drop;

-- 4. Add unique constraint for category name per author
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog_category' AND INDEX_NAME = 'uk_category_author_name'
);
SET @sql2 = IF(@idx_exists = 0,
    "ALTER TABLE `blog_category` ADD UNIQUE KEY `uk_category_author_name` (`author_id`, `name`)",
    "SELECT 'uk_category_author_name already exists'"
);
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;
