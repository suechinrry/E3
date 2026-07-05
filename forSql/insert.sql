-- ============================================================
-- 企业访客小程序 - 初始化数据脚本（合并版·BCrypt加密密码）
-- Database: visitor_system
-- 说明：包含部门、用户、预约、AI话术、核验记录、通知、节假日、权限、配置等
-- 改动：所有用户密码已替换为BCrypt加密字符串，不再存在明文密码
-- 特征：幂等脚本，可重复执行——每次先清空已有数据再插入
-- ============================================================
USE visitor_system;

-- -----------------------------------------------------------
-- 0. 清空已有数据（按子表→父表顺序避免外键冲突）
-- -----------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE app_risk_assessment;
TRUNCATE TABLE sys_user_notification;
TRUNCATE TABLE app_visit_record;
TRUNCATE TABLE app_greeting;
TRUNCATE TABLE app_appointment;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE not_notification;
TRUNCATE TABLE not_holiday;
TRUNCATE TABLE sys_role_permission;
TRUNCATE TABLE sys_setting;
TRUNCATE TABLE sys_department;
SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------
-- 1. 部门数据
-- -----------------------------------------------------------
INSERT INTO sys_department (id, name, parent_id, manager_id, status, deleted) VALUES
(1, '技术部',   0, NULL, 1, 0),
(2, '市场部',   0, NULL, 1, 0),
(3, '人事部',   0, NULL, 1, 0),
(4, '财务部',   0, NULL, 1, 0),
(5, '管理部',   0, NULL, 1, 0),
(6, '前端组',   1, NULL, 1, 0),
(7, '后端组',   1, NULL, 1, 0);

-- -----------------------------------------------------------
-- 2. 用户数据（密码全部BCrypt加密，无明文）
--    admin123 的哈希: $2a$10$4A049sGSdtfUr5hoBHaJV.iyAth4autSTDjsRotYiH5hpKadm5SXm
--    123456 的哈希:  $2a$10$SV5P6SzdD.N07vDBZ19vJOZNt1bKTjgscyqPti7VRR5XPGHZ7M9Wa
--    （已通过 BCryptPasswordEncoder.matches 验证）
-- -----------------------------------------------------------
INSERT INTO sys_user (id, username, password, name, role, phone, department_id, company, status, deleted) VALUES
-- 管理员
(1, 'admin',    '$2a$10$4A049sGSdtfUr5hoBHaJV.iyAth4autSTDjsRotYiH5hpKadm5SXm', '系统管理员', 'admin',  '13800000001', 5, NULL, 1, 0),
(2, 'admin2',   '$2a$10$SV5P6SzdD.N07vDBZ19vJOZNt1bKTjgscyqPti7VRR5XPGHZ7M9Wa', '副管理员',   'admin',  '13800000002', 5, NULL, 1, 0),
-- 被访人
(3, 'zhangsan', '$2a$10$SV5P6SzdD.N07vDBZ19vJOZNt1bKTjgscyqPti7VRR5XPGHZ7M9Wa', '张三',       'host',   '13800000011', 1, NULL, 1, 0),
(4, 'lisi',     '$2a$10$SV5P6SzdD.N07vDBZ19vJOZNt1bKTjgscyqPti7VRR5XPGHZ7M9Wa', '李四',       'host',   '13800000012', 2, NULL, 1, 0),
(5, 'wangwu',   '$2a$10$SV5P6SzdD.N07vDBZ19vJOZNt1bKTjgscyqPti7VRR5XPGHZ7M9Wa', '王五',       'host',   '13800000013', 1, NULL, 1, 0),
(6, 'zhaoliu',  '$2a$10$SV5P6SzdD.N07vDBZ19vJOZNt1bKTjgscyqPti7VRR5XPGHZ7M9Wa', '赵六',       'host',   '13800000014', 3, NULL, 1, 0),
-- 访客
(7, 'visitor1', '$2a$10$SV5P6SzdD.N07vDBZ19vJOZNt1bKTjgscyqPti7VRR5XPGHZ7M9Wa', '刘访客',     'visitor','13900000001', NULL, 'XX科技有限公司', 1, 0),
(8, 'visitor2', '$2a$10$SV5P6SzdD.N07vDBZ19vJOZNt1bKTjgscyqPti7VRR5XPGHZ7M9Wa', '陈访客',     'visitor','13900000002', NULL, 'ABC咨询', 1, 0),
(9, 'visitor3', '$2a$10$SV5P6SzdD.N07vDBZ19vJOZNt1bKTjgscyqPti7VRR5XPGHZ7M9Wa', '吴访客',     'visitor','13900000003', NULL, 'ZZ集团', 1, 0),
-- 门岗
(10,'guard',    '$2a$10$SV5P6SzdD.N07vDBZ19vJOZNt1bKTjgscyqPti7VRR5XPGHZ7M9Wa', '王门岗',     'guard',  '13800000021', 5, NULL, 1, 0);

-- -----------------------------------------------------------
-- 3. 预约数据
-- -----------------------------------------------------------
INSERT INTO app_appointment (id, visitor_id, visitor_name, visitor_phone, company, purpose, car_plate, visitor_count, remark, host_id, host_name, start_time, end_time, status, deleted) VALUES
(1, 7,  '刘访客', '13900000001', 'XX科技有限公司', '商务洽谈',  '粤B12345', 2, '需要会议室',   3, '张三', '2026-06-15 09:00', '2026-06-15 12:00', 'approved', 0),
(2, 7,  '刘访客', '13900000001', 'ABC咨询',        '项目对接',  '',         1, '',              4, '李四', '2026-06-18 14:00', '2026-06-18 16:00', 'pending', 0),
(3, 8,  '陈访客', '13900000002', 'ZZ集团',         '技术交流',  '粤C67890', 3, '需要投影设备',  3, '张三', '2026-06-10 10:00', '2026-06-10 11:00', 'rejected', 0),
(4, 7,  '刘访客', '13900000001', 'XX科技有限公司', '合同签约',  '',         1, '',              5, '王五', '2026-06-20 09:30', '2026-06-20 11:30', 'pending', 0),
(5, 9,  '吴访客', '13900000003', 'HW技术',         '产品演示',  '粤D24680', 2, '需要演示大屏',  3, '张三', '2026-06-14 09:00', '2026-06-14 12:00', 'pending', 0),
(6, 7,  '刘访客', '13900000001', 'AL云',           '合作洽谈',  '',         1, '',              4, '李四', '2026-06-13 14:00', '2026-06-13 16:00', 'approved', 0),
(7, NULL,'孙来访','13600001111', 'ABC科技',        '技术交流',  '',         2, '需要投影设备',  3, '张三', '2026-06-12 14:00', '2026-06-12 16:00', 'pending', 0),
(8, NULL,'钱七',  '13700002222', 'XX咨询',         '项目洽谈',  '',         1, '',              4, '李四', '2026-06-13 10:00', '2026-06-13 12:00', 'pending', 0),
-- 以下为历史被拒记录，用于 AI 风险预警 RAG 知识库演示
(9,  NULL,'陈访客', '13900000002', 'ZZ集团',         '技术交流',  '',         1, '非工作时间强闯', 3, '张三', '2026-06-08 22:00', '2026-06-08 23:00', 'rejected', 0),
(10, NULL,'陈访客', '13900000002', 'ZZ集团',         '推销产品',  '',         1, '多次推销骚扰',   4, '李四', '2026-06-05 14:00', '2026-06-05 15:00', 'rejected', 0),
(11, NULL,'周可疑', '13500009999', '不明机构',       '业务洽谈',  '',         2, '拒绝登记身份证', 3, '张三', '2026-06-03 09:00', '2026-06-03 10:00', 'rejected', 0);

-- -----------------------------------------------------------
-- 4. AI话术记录
-- -----------------------------------------------------------
INSERT INTO app_greeting (id, appointment_id, greeting_text, notes, status) VALUES
(1, 1,
 '欢迎XX科技有限公司的刘访客先生莅临我司洽谈合作，张三经理已在二楼会议室A等候，请前台引导至二楼。',
 '来访人员共2位，请准备2份访客证和停车券。',
 'completed'),
(2, 6,
 '欢迎AL云的代表莅临我司洽谈合作，李四经理已在三楼贵宾室等候。',
 '来访1人，准备1份访客证。',
 'completed');

-- -----------------------------------------------------------
-- 5. 门岗核验记录
-- -----------------------------------------------------------
INSERT INTO app_visit_record (id, appointment_id, guard_id, confirm_time, remark) VALUES
(1, 1, 10, '2026-06-15 08:55:00', '已核验放行'),
(2, 6, 10, '2026-06-13 13:50:00', '已核验放行');

-- -----------------------------------------------------------
-- 6. 通知公告
-- -----------------------------------------------------------
INSERT INTO not_notification (id, title, content, target_role, status, deleted) VALUES
(1, '访客进入须知',
 '来访人员请提前10分钟到达前台登记，携带有效身份证件。请配合安保人员进行安全检查，访客证请妥善保管，离司时归还。',
 'all', 1, 0),
(2, '园区停车管理通知',
 '来访车辆请从南门进入，地下车库B区可供访客使用。请勿占用员工专用车位，临时停车不超过2小时。超时需缴纳停车费。',
 'all', 1, 0),
(3, '端午节放假安排',
 '端午节期间（6月22日-24日）来访需提前一天预约。紧急联系安保部电话：0755-88888888。',
 'all', 1, 0);

-- -----------------------------------------------------------
-- 7. 节假日数据
-- -----------------------------------------------------------
INSERT INTO not_holiday (id, name, date, type) VALUES
(1, '元旦',     '2026-01-01', '法定'),
(2, '春节',     '2026-02-10', '法定'),
(3, '清明节',   '2026-04-05', '法定'),
(4, '劳动节',   '2026-05-01', '法定'),
(5, '端午节',   '2026-06-22', '法定'),
(6, '中秋节',   '2026-09-17', '法定'),
(7, '国庆节',   '2026-10-01', '法定'),
(8, '公司年会', '2026-07-15', '公司');

-- -----------------------------------------------------------
-- 8. 角色权限数据
-- -----------------------------------------------------------
INSERT INTO sys_role_permission (role_key, permission_key, permission_name) VALUES
('super', 'all', '全部权限'),
('admin', 'employee:view', '查看员工'),
('admin', 'employee:edit', '编辑员工'),
('admin', 'dept:view',    '查看部门'),
('admin', 'dept:edit',    '编辑部门'),
('admin', 'appointment:approve', '审核预约'),
('admin', 'stats:view',   '查看统计'),
('admin', 'notice:manage','管理通知'),
('admin', 'admin:manage', '管理员管理'),
('admin', 'system:config', '系统配置'),
('host',  'appointment:view',    '查看预约'),
('host',  'appointment:approve', '审核预约'),
('host',  'appointment:helper',  '辅助预约'),
('visitor', 'appointment:create', '提交预约'),
('visitor', 'appointment:my',     '我的预约'),
('guard',   'guard:verify',       '扫码核验');

-- -----------------------------------------------------------
-- 9. 系统配置
-- -----------------------------------------------------------
INSERT INTO sys_setting (config_key, config_value, description) VALUES
('company_name',   'XX科技有限公司', '公司名称'),
('contact_phone',  '0755-88888888', '联系电话'),
('company_address','深圳市南山区科技园南区XX大厦', '公司地址'),
('working_hours',  '09:00-18:00',   '工作时间'),
('visitor_notice', '来访请提前预约，携带有效身份证件，配合安保检查。', '访客须知'),
('logo_url',       '',              '公司Logo地址');

-- -----------------------------------------------------------
-- 10. 用户通知示例数据
-- -----------------------------------------------------------
INSERT INTO sys_user_notification (id, user_id, type, title, content, appointment_id, is_read) VALUES
-- 刘访客(7) 预约1已审批通过的通知
(1, 7, 'approved', '预约审批通过', '您预约的【商务洽谈】已被张三审核通过，预计到访时间：2026-06-15 09:00。', 1, 1),
-- 刘访客(7) 预约1门岗核验后的欢迎莅临通知（对应visit_record 1）
(2, 7, 'greeting', '欢迎莅临', '{"greetingText":"欢迎XX科技有限公司的刘访客先生莅临我司洽谈合作，张三经理已在二楼会议室A等候，请前台引导至二楼。","notes":"来访人员共2位，请准备2份访客证和停车券。"}', 1, 1),
-- 陈访客(8) 预约3被拒绝的通知
(3, 8, 'rejected', '预约被拒绝', '您预约的【技术交流】已被张三拒绝，原因：时间冲突，请重新预约。', 3, 0),
-- 刘访客(7) 预约6已审批通过的通知
(4, 7, 'approved', '预约审批通过', '您预约的【合作洽谈】已被李四审核通过，预计到访时间：2026-06-13 14:00。', 6, 1),
-- 刘访客(7) 预约6门岗核验后的欢迎莅临通知（对应visit_record 2）
(5, 7, 'greeting', '欢迎莅临', '{"greetingText":"欢迎AL云的代表莅临我司洽谈合作，李四经理已在三楼贵宾室等候。","notes":"来访1人，准备1份访客证。"}', 6, 0);

-- -----------------------------------------------------------
-- 11. AI 风险预警记录（为待审核预约预填，模拟异步评估）
--     SQL 插入的预约不触发后端 API，因此手动补入
-- -----------------------------------------------------------
INSERT INTO app_risk_assessment (id, appointment_id, risk_level, risk_score, reason, similar_cases) VALUES
-- 预约#7 孙来访/ABC科技/技术交流 → 事由"技术交流"出现3次，高风险
(1, 7, 'high', 78, '规则兜底：3条被拒记录高度相似（均含"技术交流"事由），强烈建议人工审核。',
 '[{"appointmentId":3,"visitorName":"陈访客","company":"ZZ集团","purpose":"技术交流","similarity":0.62},{"appointmentId":9,"visitorName":"陈访客","company":"ZZ集团","purpose":"技术交流","similarity":0.62}]'),
-- 预约#2 刘访客/ABC咨询/项目对接 → 中风险
(2, 2, 'medium', 45, '规则兜底：1条相似被拒记录，建议关注。',
 '[{"appointmentId":3,"visitorName":"陈访客","company":"ZZ集团","purpose":"技术交流","similarity":0.08}]'),
-- 预约#5 吴访客/HW技术/产品演示 → 中风险
(3, 5, 'medium', 42, '规则兜底：1条相似被拒记录（事由含"技术"），风险中等。',
 '[{"appointmentId":3,"visitorName":"陈访客","company":"ZZ集团","purpose":"技术交流","similarity":0.15}]'),
-- 预约#8 钱七/XX咨询/项目洽谈 → 低风险
(4, 8, 'low', 10, '规则兜底：未检索到相似被拒记录，风险较低。', '[]'),
-- 预约#4 刘访客/XX科技/合同签约 → 低风险
(5, 4, 'low', 12, '规则兜底：未检索到高度相似被拒记录，风险较低。', '[]');

-- -----------------------------------------------------------
-- 12. 通知公告自动分发到用户通知表
--     根据 not_notification.target_role 分发到目标用户
--     （等价于 Java 端 POST /admin/notification 时分发逻辑）
-- -----------------------------------------------------------
INSERT INTO sys_user_notification (user_id, type, title, content, appointment_id, is_read)
SELECT u.id, 'announcement', n.title, n.content, NULL, 0
FROM sys_user u
JOIN not_notification n ON (n.target_role = 'all' OR n.target_role = u.role)
WHERE n.status = 1 AND n.deleted = 0 AND u.status = 1 AND u.deleted = 0;