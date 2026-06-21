const { getAdmins, createAdmin, updateAdmin, deleteAdmin } = require('../../../utils/data-service')

Page({
  data: {
    list: [],
    showForm: false,
    formTitle: '',
    editingId: null,
    form: { username: '', password: '', name: '', phone: '', role: 'admin' }
  },
  onShow() {
    this.loadData()
  },
  loadData() {
    getAdmins(1, 100).then(res => {
      if (res.code === 200) {
        this.setData({ list: res.data.list || [] })
      }
    }).catch(() => {})
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
    if (!form.username || !form.username.trim() || !form.name || !form.name.trim()) {
      wx.showToast({ title: '请填写用户名和姓名', icon: 'none' })
      return
    }
    if (!editingId && (!form.password || !form.password.trim())) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
    if (!form.phone || !/^1\d{10}$/.test(form.phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    const apiCall = editingId
      ? updateAdmin(editingId, form)
      : createAdmin(form)
    apiCall.then(res => {
      if (res.code === 200) {
        this.setData({ showForm: false })
        this.loadData()
        wx.showToast({ title: editingId ? '已更新' : '已添加', icon: 'success' })
      } else {
        wx.showToast({ title: res.msg || '操作失败', icon: 'error' })
      }
    }).catch(() => {
      wx.showToast({ title: '操作失败', icon: 'error' })
    })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该管理员吗？',
      success: (res) => {
        if (res.confirm) {
          deleteAdmin(id).then(res => {
            if (res.code === 200) {
              this.loadData()
              wx.showToast({ title: '已删除', icon: 'success' })
            } else {
              wx.showToast({ title: res.msg || '删除失败', icon: 'error' })
            }
          }).catch(() => {
            wx.showToast({ title: '操作失败', icon: 'error' })
          })
        }
      }
    })
  }
})
