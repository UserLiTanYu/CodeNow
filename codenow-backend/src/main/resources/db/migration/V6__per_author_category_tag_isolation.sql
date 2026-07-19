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

-- 5. Drop old global unique constraint on tag name (too restrictive for per-author isolation)
SET @old_tag_idx = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog_tag' AND INDEX_NAME = 'uk_name'
);
SET @sql_tag_drop = IF(@old_tag_idx = 0,
    "SELECT 'uk_name already removed'",
    "ALTER TABLE `blog_tag` DROP INDEX `uk_name`"
);
PREPARE s_tag_drop FROM @sql_tag_drop; EXECUTE s_tag_drop; DEALLOCATE PREPARE s_tag_drop;

-- 6. Backfill existing tags' created_by to admin
UPDATE `blog_tag` SET `created_by` = (SELECT MIN(id) FROM sys_user WHERE role = 'ADMIN') WHERE `created_by` IS NULL AND EXISTS (SELECT 1 FROM sys_user WHERE role = 'ADMIN');

-- 7. Add unique constraint for tag name per author
SET @tag_creator_idx = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog_tag' AND INDEX_NAME = 'uk_tag_creator_name'
);
SET @sql_tag_creator = IF(@tag_creator_idx = 0,
    "ALTER TABLE `blog_tag` ADD UNIQUE KEY `uk_tag_creator_name` (`created_by`, `name`)",
    "SELECT 'uk_tag_creator_name already exists'"
);
PREPARE s_tag_creator FROM @sql_tag_creator; EXECUTE s_tag_creator; DEALLOCATE PREPARE s_tag_creator;
