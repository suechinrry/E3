const mockNotices = [
  { id: 1, title: '访客进入须知', content: '来访人员请提前10分钟到达前台登记，携带有效身份证件。请配合安保人员进行安全检查，访客证请妥善保管，离司时归还。', createTime: '2026-06-01' },
  { id: 2, title: '园区停车管理通知', content: '来访车辆请从南门进入，地下车库B区可供访客使用。请勿占用员工专用车位，临时停车不超过2小时。', createTime: '2026-05-28' },
  { id: 3, title: '节假日来访安排', content: '端午节期间（6月22日-24日）来访需提前一天预约，紧急联系安保部电话0755-88888888。', createTime: '2026-05-20' }
]

const mockAdmins = [
  { id: 1, username: 'admin', name: '系统管理员', phone: '13800000001', role: 'super', createTime: '2026-01-01', status: 1 },
  { id: 2, username: 'admin2', name: '副管理员', phone: '13800000002', role: 'admin', createTime: '2026-03-15', status: 1 }
]

const mockRoles = [
  { id: 1, name: '超级管理员', key: 'super', permissions: ['all'] },
  { id: 2, name: '普通管理员', key: 'admin', permissions: ['employee:view', 'employee:edit', 'dept:view', 'dept:edit', 'appointment:approve', 'stats:view', 'notice:manage'] },
  { id: 3, name: '被访人', key: 'host', permissions: ['appointment:view', 'appointment:approve', 'appointment:helper'] },
  { id: 4, name: '访客', key: 'visitor', permissions: ['appointment:create', 'appointment:my'] },
  { id: 5, name: '门岗', key: 'guard', permissions: ['guard:verify'] }
]

const allPermissions = [
  { key: 'employee:view', name: '查看员工' },
  { key: 'employee:edit', name: '编辑员工' },
  { key: 'dept:view', name: '查看部门' },
  { key: 'dept:edit', name: '编辑部门' },
  { key: 'appointment:approve', name: '审核预约' },
  { key: 'appointment:view', name: '查看预约' },
  { key: 'appointment:helper', name: '辅助预约' },
  { key: 'appointment:create', name: '提交预约' },
  { key: 'appointment:my', name: '我的预约' },
  { key: 'stats:view', name: '查看统计' },
  { key: 'notice:manage', name: '管理通知' },
  { key: 'guard:verify', name: '扫码核验' },
  { key: 'admin:manage', name: '管理员管理' },
  { key: 'system:config', name: '系统配置' },
  { key: 'all', name: '全部权限' }
]

const mockHolidays = [
  { id: 1, name: '元旦', date: '2026-01-01', type: '法定' },
  { id: 2, name: '春节', date: '2026-02-10', type: '法定' },
  { id: 3, name: '清明节', date: '2026-04-05', type: '法定' },
  { id: 4, name: '劳动节', date: '2026-05-01', type: '法定' },
  { id: 5, name: '端午节', date: '2026-06-22', type: '法定' },
  { id: 6, name: '公司年会', date: '2026-07-15', type: '公司' }
]

const mockSystemSettings = {
  companyName: 'XX科技有限公司',
  logo: '',
  contactPhone: '0755-88888888',
  address: '深圳市南山区科技园',
  workingHours: '09:00-18:00',
  visitorNotice: '来访请提前预约，携带有效证件'
}

module.exports = { mockNotices, mockAdmins, mockRoles, allPermissions, mockHolidays, mockSystemSettings }
