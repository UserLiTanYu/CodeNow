-- 普通用户注册登录第一版迁移脚本
-- 执行前请备份数据库，并确保当前数据库为 codenow。

USE `codenow`;

ALTER TABLE `sys_user`
    ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/BANNED）' AFTER `role`,
    ADD COLUMN `email_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证' AFTER `status`,
    ADD COLUMN `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间' AFTER `email_verified`,
    ADD COLUMN `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录 IP' AFTER `last_login_time`,
    ADD COLUMN `agreement_version` VARCHAR(32) DEFAULT NULL COMMENT '已接受的协议版本' AFTER `last_login_ip`,
    ADD COLUMN `agreement_accepted_at` DATETIME DEFAULT NULL COMMENT '接受协议时间' AFTER `agreement_version`,
    ADD UNIQUE KEY `uk_email` (`email`);

UPDATE `sys_user`
SET `role` = CASE WHEN LOWER(`role`) = 'admin' THEN 'ADMIN' ELSE 'USER' END,
    `status` = 'ACTIVE',
    `email_verified` = CASE WHEN `email` IS NULL THEN 0 ELSE 1 END;

ALTER TABLE `sys_user`
    MODIFY COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色（ADMIN/USER）';

ALTER TABLE `blog_comment`
    ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '评论用户 ID' AFTER `parent_id`,
    ADD KEY `idx_comment_user_id` (`user_id`),
    ADD CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL;

CREATE TABLE `article_favorite` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL                COMMENT '用户 ID',
    `article_id`  BIGINT   NOT NULL                COMMENT '文章 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite_user_article` (`user_id`, `article_id`),
    KEY `idx_favorite_article_id` (`article_id`),
    CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_favorite_article` FOREIGN KEY (`article_id`) REFERENCES `blog_article` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章收藏表';
