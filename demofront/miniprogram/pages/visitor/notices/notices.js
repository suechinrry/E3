const { getNotifications } = require('../../../utils/data-service')

Page({
  data: {
    list: []
  },
  onShow() {
    wx.redirectTo({ url: '/pages/visitor/notification-center/notification-center' })
  },
  loadData() {
    getNotifications(1, 100).then(res => {
      if (res.code === 200) {
        const raw = res.data.records || res.data || []
        this.setData({ list: Array.isArray(raw) ? raw : [] })
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
