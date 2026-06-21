// 角色配置 + 权限范围描述
const ROLE_CONFIG = {
  admin: {
    name: '管理员', desc: '管理系统全部资源与数据',
    icon: '🛡️', color: '#1a7cee',
    routes: [
      { icon: '👥', label: '员工管理（增删改查）' },
      { icon: '🏢', label: '部门管理（含子部门）' },
      { icon: '📝', label: '审核所有预约' },
      { icon: '📈', label: '数据统计与趋势分析' },
      { icon: '⚙️', label: '系统设置 / 节假日 / 通知' },
      { icon: '🔑', label: '角色权限配置' },
      { icon: '📢', label: '发布通知公告' }
    ]
  },
  host: {
    name: '被访人', desc: '管理预约与接待访客',
    icon: '👨‍💼', color: '#52c41a',
    routes: [
      { icon: '📋', label: '查看被访记录' },
      { icon: '✅', label: '审批预约（通过/拒绝）' },
      { icon: '➕', label: '辅助预约（代访客填写）' },
      { icon: '🤖', label: '查看AI迎接话术' },
      { icon: '📢', label: '查看通知公告' },
      { icon: '👤', label: '个人资料管理' }
    ]
  },
  visitor: {
    name: '访客', desc: '提交预约与查看记录',
    icon: '🧑', color: '#fa8c16',
    routes: [
      { icon: '📝', label: '提交预约申请' },
      { icon: '📄', label: '查看预约记录与状态' },
      { icon: '↩️', label: '撤销 / 再次预约' },
      { icon: '💬', label: '查看AI话术' },
      { icon: '📢', label: '查看通知公告' },
      { icon: '👤', label: '个人资料管理' }
    ]
  },
  guard: {
    name: '门岗', desc: '核验访客并放行',
    icon: '🔒', color: '#722ed1',
    routes: [
      { icon: '📷', label: '扫码核验预约信息' },
      { icon: '✅', label: '确认放行操作' },
      { icon: '🔍', label: '手动输入预约ID核验' },
      { icon: '👤', label: '个人资料管理' }
    ]
  }
}

Page({
  data: {
    roles: [],
    expandedId: null
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    const roles = Object.keys(ROLE_CONFIG).map(key => ({
      id: key,
      roleKey: key,
      ...ROLE_CONFIG[key]
    }))
    this.setData({ roles })
  },

  toggleExpand(e) {
    const id = e.currentTarget.dataset.id
    this.setData({ expandedId: this.data.expandedId === id ? null : id })
  }
})
