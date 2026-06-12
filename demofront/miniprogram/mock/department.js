const mockDepartments = [
  { id: 1, name: '技术部', manager: '张三', managerId: 1, count: 15, parentId: 0 },
  { id: 2, name: '市场部', manager: '李四', managerId: 2, count: 10, parentId: 0 },
  { id: 3, name: '人事部', manager: '王五', managerId: 5, count: 8, parentId: 0 },
  { id: 4, name: '财务部', manager: '赵六', managerId: 6, count: 6, parentId: 0 },
  { id: 5, name: '管理部', manager: '孙七', managerId: 7, count: 4, parentId: 0 },
  { id: 6, name: '前端组', manager: '张三', managerId: 1, count: 5, parentId: 1 },
  { id: 7, name: '后端组', manager: '王五', managerId: 3, count: 8, parentId: 1 }
]

module.exports = { mockDepartments }
