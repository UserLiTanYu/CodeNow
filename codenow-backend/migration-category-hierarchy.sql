-- CodeNow 分类树与文章学习顺序迁移
-- 执行前请先备份数据库。本脚本只修改结构，不删除现有数据。

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog_category' AND COLUMN_NAME = 'parent_id') = 0,
    'ALTER TABLE blog_category ADD COLUMN parent_id BIGINT NOT NULL DEFAULT 0 COMMENT ''父分类 ID（0=一级分类）'' AFTER description',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog_category' AND INDEX_NAME = 'idx_parent_sort') = 0,
    'ALTER TABLE blog_category ADD INDEX idx_parent_sort (parent_id, sort)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog_article' AND COLUMN_NAME = 'sort') = 0,
    'ALTER TABLE blog_article ADD COLUMN sort INT NOT NULL DEFAULT 0 COMMENT ''学习顺序（数字越小越靠前）'' AFTER is_top',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
