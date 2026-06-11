const { mockEmployees, filterEmployees } = require('../../../mock/employee')

let employeeData = [...mockEmployees]

Page({
  data: {
    list: employeeData,
    keyword: '',
    showForm: false,
    formTitle: '',
    editingId: null,
    form: { name: '', phone: '', department: '', departmentId: 1, position: '', status: 1 },
    departmentOptions: ['技术部', '市场部', '人事部', '财务部', '管理部']
  },
  onSearch(e) {
    const keyword = e.detail.value
    this.setData({
      keyword,
      list: keyword ? filterEmployees(keyword) : employeeData
    })
  },
  onAdd() {
    this.setData({
      showForm: true,
      formTitle: '新增员工',
      editingId: null,
      form: { name: '', phone: '', department: '技术部', departmentId: 1, position: '', status: 1 }
    })
  },
  onEdit(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showForm: true,
      formTitle: '编辑员工',
      editingId: item.id,
      form: {
        name: item.name,
        phone: item.phone,
        department: item.department,
        departmentId: item.departmentId || 1,
        position: item.position,
        status: item.status
      }
    })
  },
  hideForm() {
    this.setData({ showForm: false })
  },
  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },
  onDepartmentChange(e) {
    const idx = e.detail.value
    const deptName = this.data.departmentOptions[idx]
    this.setData({
      'form.department': deptName,
      'form.departmentId': idx + 1
    })
  },
  onSave() {
    const { form, editingId } = this.data
    if (!form.name.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' })
      return
    }
    if (!form.phone.trim() || !/^1\d{10}$/.test(form.phone.trim())) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (editingId) {
      const idx = employeeData.findIndex(e => e.id === editingId)
      if (idx >= 0) {
        employeeData[idx] = { ...employeeData[idx], ...form }
      }
    } else {
      const newId = Math.max(...employeeData.map(e => e.id), 0) + 1
      employeeData.push({ id: newId, ...form })
    }
    this.setData({
      list: this.data.keyword ? filterEmployees(this.data.keyword) : employeeData,
      showForm: false
    })
    wx.showToast({ title: editingId ? '已更新' : '已添加', icon: 'success' })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该员工吗？',
      success: (res) => {
        if (res.confirm) {
          employeeData = employeeData.filter(i => i.id !== id)
          this.setData({ list: this.data.keyword ? filterEmployees(this.data.keyword) : employeeData })
          wx.showToast({ title: '已删除', icon: 'success' })
        }
      }
    })
  }
})
