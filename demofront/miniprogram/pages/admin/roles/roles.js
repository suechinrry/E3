const ds = require('../../../utils/data-service')

Page({
  data: { list: [] },
  onShow() {
    ds.getRoles().then(res => {
      if (res.code === 200) this.setData({ list: res.data || [] })
    })
  },
  onAdd() {
    wx.showModal({ title: '新增角色', content: '（对接后端后显示新增表单）\n角色名称、权限配置', confirmText: '确定' })
  },
  onEdit(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({ title: '编辑角色', content: `编辑 ${item.name || item.roleName}\n（对接后端后显示编辑表单）`, confirmText: '确定' })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该角色吗？',
      success: (res) => {
        if (res.confirm) {
          ds.deleteRole(id).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' })
            this.onShow()
          })
        }
      }
    })
  }
})
