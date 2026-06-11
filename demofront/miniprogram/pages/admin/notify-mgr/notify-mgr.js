const { mockNotices } = require('../../../mock/notification')

let noticeData = [...mockNotices]

Page({
  data: {
    list: noticeData,
    showForm: false,
    showDetail: false,
    detailItem: null,
    formTitle: '',
    editingId: null,
    form: { title: '', content: '', targetRole: 'all' },
    roleOptions: ['全部人员', '访客', '被访人', '管理员', '门岗'],
    roleValues: ['all', 'visitor', 'host', 'admin', 'guard']
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
    const idx = e.detail.value
    this.setData({ 'form.targetRole': this.data.roleValues[idx] })
  },
  onSave() {
    const { form, editingId } = this.data
    if (!form.title.trim()) {
      wx.showToast({ title: '请输入通知标题', icon: 'none' })
      return
    }
    if (!form.content.trim()) {
      wx.showToast({ title: '请输入通知内容', icon: 'none' })
      return
    }
    if (editingId) {
      const idx = noticeData.findIndex(n => n.id === editingId)
      if (idx >= 0) {
        noticeData[idx] = {
          ...noticeData[idx],
          title: form.title.trim(),
          content: form.content.trim(),
          targetRole: form.targetRole
        }
      }
    } else {
      const newId = Math.max(...noticeData.map(n => n.id), 0) + 1
      noticeData.push({
        id: newId,
        title: form.title.trim(),
        content: form.content.trim(),
        targetRole: form.targetRole,
        createTime: new Date().toISOString().slice(0, 10)
      })
    }
    this.setData({ list: noticeData, showForm: false })
    wx.showToast({ title: editingId ? '已更新' : '已发布', icon: 'success' })
  },
  onDetail(e) {
    const item = e.currentTarget.dataset.item
    this.setData({ showDetail: true, detailItem: item })
  },
  hideDetail() {
    this.setData({ showDetail: false, detailItem: null })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该通知吗？',
      success: (res) => {
        if (res.confirm) {
          noticeData = noticeData.filter(i => i.id !== id)
          this.setData({ list: noticeData })
          wx.showToast({ title: '已删除', icon: 'success' })
        }
      }
    })
  }
})
