const ds = require('../../../utils/data-service')

Page({
  data: {
    currentTab: 'all',
    list: [],
    statusMap: { pending: '待审核', approved: '已通过', rejected: '已拒绝', cancelled: '已取消', confirmed: '已核验' }
  },
  onShow() {
    this.loadData()
  },
  loadData() {
    ds.getMyAppointments(1, 50, this.data.currentTab).then(res => {
      if (res.code === 200) this.setData({ list: res.data.list || [] })
    })
  },
  switchTab(e) {
    this.setData({ currentTab: e.currentTarget.dataset.tab }, () => this.loadData())
  },
  onCancel(e) {
    wx.showModal({
      title: '提示', content: '确定撤销该预约吗？',
      success: (res) => {
        if (res.confirm) {
          ds.cancelAppointment(e.currentTarget.dataset.id).then(() => {
            wx.showToast({ title: '已撤销', icon: 'success' })
            this.loadData()
          })
        }
      }
    })
  },
  onRebook() {
    ds.rebookAppointment(0).then(() => {
      wx.showToast({ title: '已重新提交预约', icon: 'success' })
      this.loadData()
    })
  },
  onShowQr(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({
      title: '预约二维码',
      content: `扫码内容: ${JSON.stringify({ id: item.id, name: item.visitorName, company: item.company })}\n（对接后端后显示二维码图片）`,
      confirmText: '关闭'
    })
  },
  onViewGreeting(e) {
    const item = e.currentTarget.dataset.item
    if (item.greeting) {
      wx.showModal({ title: '迎接话术', content: item.greeting, confirmText: '知道了' })
    } else {
      ds.getGreeting(item.id).then(res => {
        if (res.code === 200) {
          const g = res.data
          wx.showModal({ title: '迎接话术', content: g.greeting || g.greetingText, confirmText: '知道了' })
        }
      })
    }
  },
  goNotices() {
    wx.navigateTo({ url: '/pages/visitor/notices/notices' })
  }
})
