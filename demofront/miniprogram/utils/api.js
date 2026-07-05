// 参考 项目接口文档.md
const API = {
  // 认证
  LOGIN: '/auth/login',
  LOGIN_BYPASS: '/auth/login/bypass',
  REGISTER: '/auth/register',

  // 访客
  APPOINTMENT_CREATE: '/appointment',
  APPOINTMENT_MY: '/appointment/my',
  APPOINTMENT_CANCEL: (id) => `/appointment/${id}/cancel`,
  APPOINTMENT_RESTORE: (id) => `/appointment/${id}/restore`,
  HOST_LOOKUP: '/appointment/host/lookup',
  APPOINTMENT_GREETING: (id) => `/ai/appointment/${id}/greeting`,

  // 被访人
  APPOINTMENT_HOST: '/appointment/host',
  APPOINTMENT_HOST_STATS: '/appointment/host/stats',
  APPOINTMENT_HOST_PENDING: '/appointment/host/pending',
  APPOINTMENT_APPROVE: (id) => `/appointment/${id}/approve`,
  APPOINTMENT_HELPER: '/appointment/helper',

  // 管理员 - 员工
  EMPLOYEE_LIST: '/admin/employee',
  EMPLOYEE_CREATE: '/admin/employee',
  EMPLOYEE_UPDATE: (id) => `/admin/employee/${id}`,
  EMPLOYEE_DELETE: (id) => `/admin/employee/${id}`,
  EMPLOYEE_BATCH: '/admin/employee/batch',

  // 管理员 - 部门
  DEPT_LIST: '/admin/department',
  DEPT_CREATE: '/admin/department',
  DEPT_UPDATE: (id) => `/admin/department/${id}`,
  DEPT_DELETE: (id) => `/admin/department/${id}`,

  // 管理员 - 管理员管理
  ADMIN_LIST: '/admin/admin',
  ADMIN_CREATE: '/admin/admin',
  ADMIN_UPDATE: (id) => `/admin/admin/${id}`,
  ADMIN_DELETE: (id) => `/admin/admin/${id}`,

  // 管理员 - 角色权限
  ROLE_UPDATE: (id) => `/admin/role/${id}`,

  // 管理员 - 系统设置
  SETTINGS_GET: '/admin/settings',
  SETTINGS_UPDATE: '/admin/settings',

  // 管理员 - 节假日
  HOLIDAY_LIST: '/admin/holiday',
  HOLIDAY_CREATE: '/admin/holiday',
  HOLIDAY_DELETE: (id) => `/admin/holiday/${id}`,

  // 管理员 - 审核
  ADMIN_APPOINTMENT: '/admin/appointment',
  ADMIN_PENDING: '/admin/appointment/pending',
  ADMIN_APPROVE: (id) => `/admin/appointment/${id}/approve`,

  // 管理员 - 统计
  STATS_OVERVIEW: '/admin/stats/overview',
  STATS_VISITOR_RECORD: '/admin/stats/visitor-record',
  STATS_TREND: '/admin/stats/trend',

  // 管理员 - 通知
  NOTIFICATION_LIST: '/admin/notification',
  NOTIFICATION_CREATE: '/admin/notification',
  NOTIFICATION_UPDATE: (id) => `/admin/notification/${id}`,
  NOTIFICATION_DELETE: (id) => `/admin/notification/${id}`,
  NOTIFICATION_RECIPIENTS: (id) => `/admin/notification/${id}/recipients`,

  // 访客通知
  NOTIFICATION_PUBLIC: '/notification',

  // 用户弹窗通知
  USER_NOTIFICATION_POPUP: '/user-notification/popup',
  USER_NOTIFICATION_LIST: '/user-notification/list',
  USER_NOTIFICATION_READ: (id) => `/user-notification/${id}/read`,
  USER_NOTIFICATION_READ_ALL: '/user-notification/read-all',

  // 门岗
  GUARD_VERIFY: '/guard/verify',
  GUARD_VERIFY_BY_NAME: '/guard/verify-by-name',
  GUARD_CONFIRM: (id) => `/guard/confirm/${id}`,

  // 个人资料
  USER_PROFILE_GET: '/user/profile',
  USER_PROFILE_UPDATE: '/user/profile',

  // AI
  AI_GREETING: (id) => `/ai/greeting/${id}`,
  AI_REGENERATE: (id) => `/ai/greeting/${id}/regenerate`,

  // AI 风险预警（管理员）
  RISK_BY_APPOINTMENT: (id) => `/admin/risk/appointment/${id}`,
  RISK_HIGH: '/admin/risk/high',
  RISK_ALL: '/admin/risk/all',
  RISK_REASSESS: (id) => `/admin/risk/appointment/${id}/reassess`,
  RISK_STORE_STATUS: '/admin/risk/store/status',
  RISK_STORE_REBUILD: '/admin/risk/store/rebuild'
}

module.exports = API
