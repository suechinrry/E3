const { getNotifications } = require('../../../utils/data-service')

Page({
  data: {
    list: []
  },
  onShow() {
    this.loadData()
  },
  loadData() {
    getNotifications(1, 100).then(res => {
      if (res.code === 200) {
        this.setData({ list: res.data.list || [] })
      }
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'error' })
    })
  },
  onDetail(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({
      title: item.title,
      content: (item.content || item.summary || '暂无详细内容'),
      confirmText: '关闭'
    })
  }
})
