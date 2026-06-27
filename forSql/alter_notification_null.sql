-- 修复用户通知表 appointment_id 非空约束，允许公告类通知存储 NULL
ALTER TABLE sys_user_notification MODIFY COLUMN appointment_id INT NULL COMMENT '关联预约ID（公告类通知可为空）';
