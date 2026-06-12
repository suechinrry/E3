const ds = require('../../../utils/data-service')

Page({
  data: { list: [] },
  onShow() {
    ds.getNotifications().then(res => {
      if (res.code === 200) this.setData({ list: res.data.list || [] })
    })
  },
  onDetail(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({ title: item.title, content: item.content, confirmText: '关闭' })
  }
})
