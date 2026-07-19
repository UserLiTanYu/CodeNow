-- CodeNow V2: Community features (user registration, comments, favorites, notifications)
-- Combines migration-community-v1.sql and migration-community-v2.sql

-- 用户表新增字段
ALTER TABLE `sys_user`
    ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/BANNED）' AFTER `role`,
    ADD COLUMN `email_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证' AFTER `status`,
    ADD COLUMN `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间' AFTER `email_verified`,
    ADD COLUMN `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录 IP' AFTER `last_login_time`,
    ADD COLUMN `ban_reason` VARCHAR(255) DEFAULT NULL COMMENT '封禁原因' AFTER `last_login_ip`,
    ADD COLUMN `banned_at` DATETIME DEFAULT NULL COMMENT '封禁时间' AFTER `ban_reason`,
    ADD COLUMN `agreement_version` VARCHAR(32) DEFAULT NULL COMMENT '已接受的协议版本' AFTER `banned_at`,
    ADD COLUMN `agreement_accepted_at` DATETIME DEFAULT NULL COMMENT '接受协议时间' AFTER `agreement_version`,
    ADD UNIQUE KEY `uk_email` (`email`);

-- 更新角色枚举值
UPDATE `sys_user`
SET `role` = CASE WHEN LOWER(`role`) = 'admin' THEN 'ADMIN' ELSE 'USER' END,
    `status` = 'ACTIVE',
    `email_verified` = CASE WHEN `email` IS NULL THEN 0 ELSE 1 END;

ALTER TABLE `sys_user`
    MODIFY COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色（ADMIN/USER）';

-- 评论表新增用户外键
ALTER TABLE `blog_comment`
    ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '评论用户 ID' AFTER `parent_id`,
    ADD KEY `idx_comment_user_id` (`user_id`),
    ADD CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL;

-- 文章收藏表
CREATE TABLE IF NOT EXISTS `article_favorite` (
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

-- 评论点赞表
CREATE TABLE IF NOT EXISTS `comment_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `comment_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_like` (`comment_id`, `user_id`),
    KEY `idx_comment_like_user` (`user_id`),
    CONSTRAINT `fk_comment_like_comment` FOREIGN KEY (`comment_id`) REFERENCES `blog_comment` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_like_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

-- 站内通知表
CREATE TABLE IF NOT EXISTS `user_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(32) NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `content` VARCHAR(500) DEFAULT NULL,
    `article_id` BIGINT DEFAULT NULL,
    `comment_id` BIGINT DEFAULT NULL,
    `is_read` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_notification_user_read_time` (`user_id`, `is_read`, `create_time`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notification_article` FOREIGN KEY (`article_id`) REFERENCES `blog_article` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_notification_comment` FOREIGN KEY (`comment_id`) REFERENCES `blog_comment` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT DEFAULT NULL,
    `account` VARCHAR(100) NOT NULL,
    `ip` VARCHAR(64) DEFAULT NULL,
    `user_agent` VARCHAR(255) DEFAULT NULL,
    `success` TINYINT NOT NULL DEFAULT 0,
    `failure_reason` VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_login_log_user_time` (`user_id`, `create_time`),
    KEY `idx_login_log_account_time` (`account`, `create_time`),
    CONSTRAINT `fk_login_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
