-- ============================================
-- 码上记（CodeNow）数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS `codenow` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `codenow`;

-- -------------------------------------------
-- 1. 用户表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL                COMMENT '用户名（登录账号）',
    `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密）',
    `nickname`    VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL            COMMENT '头像 URL',
    `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色（ADMIN/AUTHOR/USER）',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/BANNED）',
    `email_verified` TINYINT   NOT NULL DEFAULT 0      COMMENT '邮箱是否已验证',
    `last_login_time` DATETIME DEFAULT NULL            COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(64) DEFAULT NULL            COMMENT '最后登录 IP',
    `ban_reason` VARCHAR(255) DEFAULT NULL               COMMENT '封禁原因',
    `banned_at` DATETIME DEFAULT NULL                    COMMENT '封禁时间',
    `agreement_version` VARCHAR(32) DEFAULT NULL        COMMENT '已接受的协议版本',
    `agreement_accepted_at` DATETIME DEFAULT NULL       COMMENT '接受协议时间',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0               COMMENT '逻辑删除（0=正常, 1=已删）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 默认管理员账号：admin / 123456（BCrypt 加密）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`, `status`, `email_verified`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'ADMIN', 'ACTIVE', 1);

-- -------------------------------------------
-- 2. 分类表
-- -------------------------------------------
DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '分类名称',
    `description` VARCHAR(200) DEFAULT NULL            COMMENT '分类描述',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父分类 ID（0=一级分类）',
    `sort`        INT          DEFAULT 0               COMMENT '排序号（数字越小越靠前）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0               COMMENT '逻辑删除（0=正常, 1=已删）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_parent_sort` (`parent_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- -------------------------------------------
-- 3. 标签表
-- -------------------------------------------
DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '标签名称',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`  TINYINT      DEFAULT 0               COMMENT '逻辑删除（0=正常, 1=已删）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- -------------------------------------------
-- 4. 文章表
-- -------------------------------------------
DROP TABLE IF EXISTS `blog_article`;
CREATE TABLE `blog_article` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(200) NOT NULL                COMMENT '文章标题',
    `content`     TEXT         NOT NULL                COMMENT '文章内容（Markdown 原文）',
    `content_html` TEXT        DEFAULT NULL            COMMENT '渲染后的 HTML',
    `summary`     VARCHAR(500) DEFAULT NULL            COMMENT '文章摘要',
    `cover_image` VARCHAR(255) DEFAULT NULL            COMMENT '封面图 URL',
    `category_id` BIGINT       DEFAULT NULL            COMMENT '所属分类 ID',
    `author_id`   BIGINT       DEFAULT NULL            COMMENT '作者 ID',
    `status`      TINYINT      DEFAULT 0               COMMENT '状态（0=草稿, 1=已发布）',
    `is_top`      TINYINT      DEFAULT 0               COMMENT '是否置顶（0=否, 1=是）',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '学习顺序（数字越小越靠前）',
    `view_count`  INT          DEFAULT 0               COMMENT '浏览量',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0               COMMENT '逻辑删除（0=正常, 1=已删）',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_author_id` (`author_id`),
    CONSTRAINT `fk_article_category` FOREIGN KEY (`category_id`) REFERENCES `blog_category` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- -------------------------------------------
-- 5. 文章-标签关联表
-- -------------------------------------------
DROP TABLE IF EXISTS `blog_article_tag`;
CREATE TABLE `blog_article_tag` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id` BIGINT NOT NULL               COMMENT '文章 ID',
    `tag_id`     BIGINT NOT NULL               COMMENT '标签 ID',
    `is_deleted` TINYINT DEFAULT 0              COMMENT '逻辑删除（0=正常, 1=已删）',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_tag_id` (`tag_id`),
    CONSTRAINT `fk_artag_article` FOREIGN KEY (`article_id`) REFERENCES `blog_article` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_artag_tag` FOREIGN KEY (`tag_id`) REFERENCES `blog_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章-标签关联表';

-- -------------------------------------------
-- 6. 操作日志表
-- -------------------------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       DEFAULT NULL            COMMENT '操作人 ID',
    `username`    VARCHAR(50)  DEFAULT NULL            COMMENT '操作人用户名',
    `operation`   VARCHAR(100) DEFAULT NULL            COMMENT '操作描述',
    `method`      VARCHAR(200) DEFAULT NULL            COMMENT '请求方法（类名.方法名）',
    `params`      TEXT         DEFAULT NULL            COMMENT '请求参数 JSON',
    `ip`          VARCHAR(50)  DEFAULT NULL            COMMENT '请求 IP',
    `duration`    INT          DEFAULT NULL            COMMENT '耗时（毫秒）',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态（0=失败, 1=成功）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- -------------------------------------------
-- 7. 评论表
-- -------------------------------------------
DROP TABLE IF EXISTS `blog_comment`;
CREATE TABLE `blog_comment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id`  BIGINT       NOT NULL                COMMENT '文章 ID',
    `parent_id`   BIGINT       DEFAULT 0                COMMENT '父评论 ID（顶级评论为 0）',
    `user_id`     BIGINT       DEFAULT NULL             COMMENT '评论用户 ID',
    `content`     TEXT         NOT NULL                COMMENT '评论内容',
    `nickname`    VARCHAR(50)  NOT NULL                COMMENT '昵称',
    `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱（不公开显示）',
    `ip`          VARCHAR(50)  DEFAULT NULL            COMMENT '评论者 IP',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态（0=待审核, 1=已通过）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_comment_user_id` (`user_id`),
    CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `blog_article` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- -------------------------------------------
-- 8. 文章收藏表
-- -------------------------------------------
DROP TABLE IF EXISTS `article_favorite`;
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

-- -------------------------------------------
-- 9. 评论点赞表
-- -------------------------------------------
CREATE TABLE `comment_like` (
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

-- -------------------------------------------
-- 10. 站内通知表
-- -------------------------------------------
CREATE TABLE `user_notification` (
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

-- -------------------------------------------
-- 11. 登录日志表
-- -------------------------------------------
CREATE TABLE `sys_login_log` (
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

-- -------------------------------------------
-- 12. 作者公开资料表
-- -------------------------------------------
CREATE TABLE `author_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `bio` VARCHAR(500) NOT NULL,
    `expertise` VARCHAR(500) NOT NULL,
    `website_url` VARCHAR(500) DEFAULT NULL,
    `portfolio_url` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_author_profile_user` (`user_id`),
    CONSTRAINT `fk_author_profile_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作者公开资料表';

-- -------------------------------------------
-- 13. 作者入驻申请表
-- -------------------------------------------
CREATE TABLE `author_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `reason` VARCHAR(1000) NOT NULL,
    `expertise` VARCHAR(500) NOT NULL,
    `bio` VARCHAR(500) NOT NULL,
    `portfolio_url` VARCHAR(500) DEFAULT NULL,
    `website_url` VARCHAR(500) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `reviewer_id` BIGINT DEFAULT NULL,
    `review_remark` VARCHAR(500) DEFAULT NULL,
    `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `reviewed_at` DATETIME DEFAULT NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `pending_user_id` BIGINT GENERATED ALWAYS AS (CASE WHEN `status` = 'PENDING' THEN `user_id` ELSE NULL END) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_author_application_pending_user` (`pending_user_id`),
    KEY `idx_author_application_user_status` (`user_id`, `status`),
    KEY `idx_author_application_status_time` (`status`, `submitted_at`),
    KEY `idx_author_application_reviewer` (`reviewer_id`),
    CONSTRAINT `fk_author_application_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_author_application_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作者入驻申请表';
