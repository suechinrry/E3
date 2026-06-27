const { getAdminNotifications, createNotification, updateNotification, deleteNotification, getNotificationRecipients } = require('../../../utils/data-service')

Page({
  data: {
    list: [],
    showForm: false,
    showDetail: false,
    detailItem: null,
    showRecipients: false,
    recipients: [],
    formTitle: '',
    editingId: null,
    form: { title: '', content: '', targetRole: 'all' },
    roleOptions: ['全部人员', '访客', '被访人', '管理员', '门岗'],
    roleValues: ['all', 'visitor', 'host', 'admin', 'guard']
  },
  onShow() {
    this.loadData()
  },
  loadData() {
    getAdminNotifications(1, 100).then(res => {
      if (res.code === 200) {
        const raw = res.data.records || res.data || []
        this.setData({ list: Array.isArray(raw) ? raw : [] })
      }
    }).catch(() => {})
  },
  onAdd() {
    this.setData({
      showForm: true,
      formTitle: '发布通知',
      editingId: null,
      form: { title: '', content: '', targetRole: 'all' }
    })
  },
  onEdit(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showForm: true,
      formTitle: '编辑通知',
      editingId: item.id,
      form: {
        title: item.title,
        content: item.content,
        targetRole: item.targetRole || 'all'
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
  onRoleChange(e) {
    const idx = parseInt(e.detail.value)
    this.setData({ 'form.targetRole': this.data.roleValues[idx] })
  },
  onSave() {
    const { form, editingId } = this.data
    if (!form.title || !form.title.trim()) {
      wx.showToast({ title: '请输入通知标题', icon: 'none' })
      return
    }
    if (!form.content || !form.content.trim()) {
      wx.showToast({ title: '请输入通知内容', icon: 'none' })
      return
    }
    const apiCall = editingId
      ? updateNotification(editingId, form)
      : createNotification(form)
    apiCall.then(res => {
      if (res.code === 200) {
        this.setData({ showForm: false })
        this.loadData()
        wx.showToast({ title: editingId ? '已更新' : '已发布', icon: 'success' })
      } else {
        wx.showToast({ title: res.msg || '操作失败', icon: 'error' })
      }
    }).catch(() => {
      wx.showToast({ title: '操作失败', icon: 'error' })
    })
  },
  onDetail(e) {
    const item = e.currentTarget.dataset.item
    this.setData({ showDetail: true, detailItem: item })
  },
  hideDetail() {
    this.setData({ showDetail: false, detailItem: null })
  },
  onViewRecipients(e) {
    const id = e.currentTarget.dataset.id
    getNotificationRecipients(id).then(res => {
      if (res.code === 200) {
        this.setData({ showRecipients: true, recipients: res.data || [] })
      } else {
        wx.showToast({ title: res.msg || '加载失败', icon: 'error' })
      }
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'error' })
    })
  },
  hideRecipients() {
    this.setData({ showRecipients: false, recipients: [] })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该通知吗？',
      success: (res) => {
        if (res.confirm) {
          deleteNotification(id).then(res => {
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
