const mockEmployees = [
  { id: 1, name: '张三', phone: '13800001111', department: '技术部', departmentId: 1, position: '工程师', status: 1 },
  { id: 2, name: '李四', phone: '13800002222', department: '市场部', departmentId: 2, position: '经理', status: 1 },
  { id: 3, name: '王五', phone: '13800003333', department: '技术部', departmentId: 1, position: '高级工程师', status: 1 },
  { id: 4, name: '赵六', phone: '13800004444', department: '人事部', departmentId: 3, position: '主管', status: 0 },
  { id: 5, name: '孙七', phone: '13800005555', department: '财务部', departmentId: 4, position: '会计', status: 1 },
  { id: 6, name: '周八', phone: '13800006666', department: '技术部', departmentId: 1, position: '实习生', status: 1 }
]

function filterEmployees(keyword) {
  if (!keyword) return mockEmployees
  return mockEmployees.filter(e => e.name.includes(keyword))
}

module.exports = { mockEmployees, filterEmployees }
