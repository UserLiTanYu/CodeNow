CREATE TABLE IF NOT EXISTS `site_profile` (
    `id` TINYINT NOT NULL,
    `bio` VARCHAR(500) NOT NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点管理员公开简介';

INSERT INTO `site_profile` (`id`, `bio`)
VALUES (1, '一个支持 Markdown 写作的个人技术博客，帮助开发者记录和分享学习笔记。')
ON DUPLICATE KEY UPDATE `id` = `id`;
