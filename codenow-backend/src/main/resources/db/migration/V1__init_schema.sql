-- CodeNow V1: Base schema
-- Tables: sys_user, blog_category, blog_tag, blog_article, blog_article_tag, sys_operation_log, blog_comment

CREATE DATABASE IF NOT EXISTS `codenow` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `codenow`;

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL                COMMENT '用户名（登录账号）',
    `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密）',
    `nickname`    VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL            COMMENT '头像 URL',
    `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色（ADMIN/USER）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0               COMMENT '逻辑删除（0=正常, 1=已删）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 默认管理员账号：admin / 123456（BCrypt 加密）
INSERT IGNORE INTO `sys_user` (`username`, `password`, `nickname`, `role`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'ADMIN');

CREATE TABLE IF NOT EXISTS `blog_category` (
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

CREATE TABLE IF NOT EXISTS `blog_tag` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '标签名称',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`  TINYINT      DEFAULT 0               COMMENT '逻辑删除（0=正常, 1=已删）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

CREATE TABLE IF NOT EXISTS `blog_article` (
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

CREATE TABLE IF NOT EXISTS `blog_article_tag` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id` BIGINT NOT NULL               COMMENT '文章 ID',
    `tag_id`     BIGINT NOT NULL               COMMENT '标签 ID',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_tag_id` (`tag_id`),
    CONSTRAINT `fk_artag_article` FOREIGN KEY (`article_id`) REFERENCES `blog_article` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_artag_tag` FOREIGN KEY (`tag_id`) REFERENCES `blog_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章-标签关联表';

CREATE TABLE IF NOT EXISTS `sys_operation_log` (
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

CREATE TABLE IF NOT EXISTS `blog_comment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id`  BIGINT       NOT NULL                COMMENT '文章 ID',
    `parent_id`   BIGINT       DEFAULT 0                COMMENT '父评论 ID（顶级评论为 0）',
    `content`     TEXT         NOT NULL                COMMENT '评论内容',
    `nickname`    VARCHAR(50)  NOT NULL                COMMENT '昵称',
    `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱（不公开显示）',
    `ip`          VARCHAR(50)  DEFAULT NULL            COMMENT '评论者 IP',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态（0=待审核, 1=已通过）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_parent_id` (`parent_id`),
    CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `blog_article` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';
