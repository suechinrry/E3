const mockUsers = [
  { id: 1, username: 'admin', password: 'admin123', name: '系统管理员', role: 'admin', phone: '13800000001', department: '管理部', company: '企业管理部' },
  { id: 2, username: 'zhangsan', password: '123456', name: '张三', role: 'host', phone: '13800000002', department: '技术部', company: 'XX科技有限公司' },
  { id: 3, username: 'lisi', password: '123456', name: '李四', role: 'visitor', phone: '13800000003', company: 'XX科技' },
  { id: 4, username: 'guard', password: '123456', name: '王门岗', role: 'guard', phone: '13800000004', department: '安保部', company: 'XX科技有限公司' }
]

const roleHomeMap = {
  visitor: '/pages/visitor/appoint/appoint',
  host: '/pages/host/visited/visited',
  admin: '/pages/admin/employees/employees',
  guard: '/pages/guard/scan/scan'
}

function login(username, password) {
  const user = mockUsers.find(u => u.username === username && u.password === password)
  if (!user) return null
  const token = 'mock_token_' + user.id + '_' + Date.now()
  const info = { ...user, token }
  delete info.password
  return info
}

module.exports = { mockUsers, roleHomeMap, login }
