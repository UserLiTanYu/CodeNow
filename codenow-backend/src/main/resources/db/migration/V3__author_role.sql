-- CodeNow V3: Author role and author tables
-- From migration-author-role.sql

ALTER TABLE `sys_user`
    MODIFY COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色（ADMIN/AUTHOR/USER）';

CREATE TABLE IF NOT EXISTS `author_profile` (
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
    CONSTRAINT `fk_author_profile_user`
        FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作者公开资料表';

CREATE TABLE IF NOT EXISTS `author_application` (
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
    `pending_user_id` BIGINT GENERATED ALWAYS AS (
        CASE WHEN `status` = 'PENDING' THEN `user_id` ELSE NULL END
    ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_author_application_pending_user` (`pending_user_id`),
    KEY `idx_author_application_user_status` (`user_id`, `status`),
    KEY `idx_author_application_status_time` (`status`, `submitted_at`),
    KEY `idx_author_application_reviewer` (`reviewer_id`),
    CONSTRAINT `fk_author_application_user`
        FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_author_application_reviewer`
        FOREIGN KEY (`reviewer_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作者入驻申请表';
