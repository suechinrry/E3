const ds = require('../../../utils/data-service')

Page({
  data: { list: [] },
  onShow() {
    ds.getAdmins().then(res => {
      if (res.code === 200) this.setData({ list: res.data || [] })
    })
  },
  onAdd() {
    wx.showModal({ title: '新增管理员', content: '（对接后端后显示新增表单）\n用户名、密码、姓名、手机号', confirmText: '确定' })
  },
  onEdit(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({ title: '编辑管理员', content: `编辑 ${item.name}\n（对接后端后显示编辑表单）`, confirmText: '确定' })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该管理员吗？',
      success: (res) => {
        if (res.confirm) {
          ds.deleteAdmin(id).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' })
            this.onShow()
          })
        }
      }
    })
  }
})
