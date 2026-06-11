const { mockAdmins } = require('../../../mock/notification')

let adminData = [...mockAdmins]

Page({
  data: {
    list: adminData,
    showForm: false,
    formTitle: '',
    editingId: null,
    form: { username: '', password: '', name: '', phone: '', role: 'admin' }
  },
  onAdd() {
    this.setData({
      showForm: true,
      formTitle: '新增管理员',
      editingId: null,
      form: { username: '', password: '', name: '', phone: '', role: 'admin' }
    })
  },
  onEdit(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showForm: true,
      formTitle: '编辑管理员',
      editingId: item.id,
      form: {
        username: item.username,
        password: '',
        name: item.name,
        phone: item.phone,
        role: item.role || 'admin'
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
  onSave() {
    const { form, editingId } = this.data
    if (!form.username.trim() || !form.name.trim()) {
      wx.showToast({ title: '请填写用户名和姓名', icon: 'none' })
      return
    }
    if (!editingId && !form.password.trim()) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
    if (!form.phone.trim() || !/^1\d{10}$/.test(form.phone.trim())) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    // 检查用户名唯一性
    const dup = adminData.find(a => a.username === form.username.trim() && a.id !== editingId)
    if (dup) {
      wx.showToast({ title: '用户名已存在', icon: 'none' })
      return
    }
    if (editingId) {
      const idx = adminData.findIndex(a => a.id === editingId)
      if (idx >= 0) {
        const update = { ...form }
        if (!update.password) delete update.password
        adminData[idx] = { ...adminData[idx], ...update }
      }
    } else {
      const newId = Math.max(...adminData.map(a => a.id), 0) + 1
      adminData.push({
        id: newId,
        ...form,
        createTime: new Date().toISOString().slice(0, 10),
        status: 1
      })
    }
    this.setData({ list: adminData, showForm: false })
    wx.showToast({ title: editingId ? '已更新' : '已添加', icon: 'success' })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该管理员吗？',
      success: (res) => {
        if (res.confirm) {
          adminData = adminData.filter(i => i.id !== id)
          this.setData({ list: adminData })
          wx.showToast({ title: '已删除', icon: 'success' })
        }
      }
    })
  }
})
