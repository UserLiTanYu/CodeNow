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
    `role`        VARCHAR(20)  DEFAULT 'admin'         COMMENT '角色（admin/user）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0               COMMENT '逻辑删除（0=正常, 1=已删）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 默认管理员账号：admin / 123456（BCrypt 加密）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin');

-- -------------------------------------------
-- 2. 分类表
-- -------------------------------------------
DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '分类名称',
    `description` VARCHAR(200) DEFAULT NULL            COMMENT '分类描述',
    `sort`        INT          DEFAULT 0               COMMENT '排序号（数字越小越靠前）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0               COMMENT '逻辑删除（0=正常, 1=已删）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
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
    `view_count`  INT          DEFAULT 0               COMMENT '浏览量',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0               COMMENT '逻辑删除（0=正常, 1=已删）',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- -------------------------------------------
-- 5. 文章-标签关联表
-- -------------------------------------------
DROP TABLE IF EXISTS `blog_article_tag`;
CREATE TABLE `blog_article_tag` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id` BIGINT NOT NULL               COMMENT '文章 ID',
    `tag_id`     BIGINT NOT NULL               COMMENT '标签 ID',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_tag_id` (`tag_id`)
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
