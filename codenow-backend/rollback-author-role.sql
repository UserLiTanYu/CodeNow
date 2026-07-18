-- CodeNow 作者角色 P1 回滚脚本
-- 回滚会将现有 AUTHOR 用户降级为 USER，并删除作者申请及作者资料。

UPDATE `sys_user` SET `role` = 'USER' WHERE `role` = 'AUTHOR';

DROP TABLE IF EXISTS `author_application`;
DROP TABLE IF EXISTS `author_profile`;

ALTER TABLE `sys_user`
    MODIFY COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色（ADMIN/USER）';
