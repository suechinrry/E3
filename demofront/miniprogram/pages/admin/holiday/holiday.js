const ds = require('../../../utils/data-service')

Page({
  data: { list: [] },
  onShow() {
    ds.getHolidays().then(res => {
      if (res.code === 200) this.setData({ list: res.data || [] })
    })
  },
  onAdd() {
    wx.showModal({
      title: '新增节假日', content: '（对接后端后显示新增表单）\n名称、日期、类型',
      confirmText: '确定'
    })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该节假日吗？',
      success: (res) => {
        if (res.confirm) {
          ds.deleteHoliday(id).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' })
            this.onShow()
          })
        }
      }
    })
  }
})
