ALTER TABLE `site_profile`
    ADD COLUMN `site_name` VARCHAR(50) NOT NULL DEFAULT '码上记' AFTER `id`,
    ADD COLUMN `slogan` VARCHAR(100) NOT NULL DEFAULT '记录实践，分享知识' AFTER `site_name`,
    ADD COLUMN `description` VARCHAR(500) NOT NULL
        DEFAULT '码上记是一个面向开发者的技术学习与知识分享平台，专注于系统化教程、开发实践和项目经验沉淀。'
        AFTER `slogan`,
    ADD COLUMN `about_content` VARCHAR(5000) NOT NULL
        DEFAULT '我们整理可靠的技术知识、真实的开发经验和可复用的解决方案，帮助学习过程更清晰、更连贯。'
        AFTER `bio`,
    ADD COLUMN `contact_email` VARCHAR(100) NULL AFTER `about_content`,
    ADD COLUMN `github_url` VARCHAR(255) NULL AFTER `contact_email`,
    ADD COLUMN `founded_at` DATE NULL AFTER `github_url`;

ALTER TABLE `site_profile` COMMENT = '站点公开资料';
