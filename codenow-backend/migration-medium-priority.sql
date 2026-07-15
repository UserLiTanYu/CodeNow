-- 已有数据库升级脚本（2026-07-15）
-- 新部署由 init.sql 自动包含该字段；已有实例仅需执行一次本脚本。

ALTER TABLE `blog_article_tag`
    ADD COLUMN `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0=正常, 1=已删）' AFTER `tag_id`;
