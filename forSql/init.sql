-- ============================================================
-- 企业访客小程序 - 数据库初始化脚本
-- Database: visitor_system
-- ============================================================

CREATE DATABASE IF NOT EXISTS visitor_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE visitor_system;

-- 对于已存在的数据库，执行以下语句添加 position 列
-- ALTER TABLE sys_user ADD COLUMN position VARCHAR(50) NULL COMMENT '职位' AFTER department_id;

-- -----------------------------------------------------------
-- 1. 部门表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS sys_department;
CREATE TABLE sys_department (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(50)  NOT NULL COMMENT '部门名称',
  parent_id   INT          NOT NULL DEFAULT 0 COMMENT '上级部门ID',
  manager_id  INT          NULL     COMMENT '负责人用户ID',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- -----------------------------------------------------------
-- 2. 用户表（管理员/被访人/访客/门岗 共用）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录名',
  password      VARCHAR(100) NOT NULL COMMENT '密码（建议BCrypt加密）',
  name          VARCHAR(50)  NOT NULL COMMENT '姓名',
  role          ENUM('admin','host','visitor','guard') NOT NULL COMMENT '角色',
  phone         VARCHAR(20)  NULL     COMMENT '手机号',
  department_id INT          NULL     COMMENT '部门ID',
  position      VARCHAR(50)  NULL     COMMENT '职位',
  company       VARCHAR(100) NULL     COMMENT '所属公司（访客使用）',
  avatar        VARCHAR(255) NULL     COMMENT '头像URL',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_role (role),
  INDEX idx_dept (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -----------------------------------------------------------
-- 3. 预约表（核心业务表）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS app_appointment;
CREATE TABLE app_appointment (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  visitor_id    INT          NULL     COMMENT '访客用户ID（游客可为NULL）',
  visitor_name  VARCHAR(50)  NOT NULL COMMENT '访客姓名',
  visitor_phone VARCHAR(20)  NOT NULL COMMENT '访客手机号',
  company       VARCHAR(100) NOT NULL COMMENT '来访单位',
  purpose       VARCHAR(200) NOT NULL COMMENT '来访事由',
  car_plate     VARCHAR(20)  NULL     COMMENT '车牌号',
  visitor_count INT          NOT NULL DEFAULT 1 COMMENT '来访人数',
  remark        VARCHAR(500) NULL     COMMENT '备注',
  host_id       INT          NOT NULL COMMENT '被访人用户ID',
  host_name     VARCHAR(50)  NOT NULL COMMENT '被访人姓名',
  start_time    DATETIME     NOT NULL COMMENT '预计到访时间',
  end_time      DATETIME     NULL     COMMENT '预计离开时间',
  status        ENUM('pending','approved','rejected','cancelled','confirmed') NOT NULL DEFAULT 'pending' COMMENT '状态',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NULL     ON UPDATE CURRENT_TIMESTAMP COMMENT '最后操作时间',
  INDEX idx_visitor (visitor_id),
  INDEX idx_host (host_id),
  INDEX idx_status (status),
  INDEX idx_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- -----------------------------------------------------------
-- 4. AI迎接话术记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS app_greeting;
CREATE TABLE app_greeting (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  appointment_id  INT          NOT NULL COMMENT '预约ID',
  greeting_text   TEXT         NULL     COMMENT '迎接语',
  seat_suggestion VARCHAR(500) NULL     COMMENT '座位安排建议',
  notes           VARCHAR(500) NULL     COMMENT '注意事项',
  status          ENUM('pending','completed','failed') NOT NULL DEFAULT 'pending' COMMENT '生成状态',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_appointment (appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI话术记录表';

-- -----------------------------------------------------------
-- 5. 门岗核验记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS app_visit_record;
CREATE TABLE app_visit_record (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  appointment_id  INT          NOT NULL COMMENT '预约ID',
  guard_id        INT          NULL     COMMENT '门岗用户ID',
  confirm_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '核验时间',
  remark          VARCHAR(200) NULL     COMMENT '备注',
  INDEX idx_appointment (appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门岗核验记录表';

-- -----------------------------------------------------------
-- 6. 通知公告表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS not_notification;
CREATE TABLE not_notification (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(100) NOT NULL COMMENT '标题',
  content     TEXT         NOT NULL COMMENT '内容',
  target_role ENUM('all','visitor','host','admin','guard') NOT NULL DEFAULT 'all' COMMENT '发布范围',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1发布 0草稿',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_target (target_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';

-- -----------------------------------------------------------
-- 7. 节假日设置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS not_holiday;
CREATE TABLE not_holiday (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(50)  NOT NULL COMMENT '节假日名称',
  date        DATE         NOT NULL COMMENT '日期',
  type        VARCHAR(20)  NOT NULL DEFAULT '法定' COMMENT '类型（法定/公司）',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节假日设置表';

-- -----------------------------------------------------------
-- 8. 角色权限配置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  role_key        VARCHAR(50)  NOT NULL COMMENT '角色标识（super/admin/host/visitor/guard）',
  permission_key  VARCHAR(50)  NOT NULL COMMENT '权限标识',
  permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_role (role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限表';

-- -----------------------------------------------------------
-- 9. 系统配置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS sys_setting;
CREATE TABLE sys_setting (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  config_key  VARCHAR(50)  NOT NULL UNIQUE COMMENT '配置键',
  config_value TEXT        NULL     COMMENT '配置值',
  description VARCHAR(200) NULL     COMMENT '说明',
  update_time DATETIME     NULL     ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';
