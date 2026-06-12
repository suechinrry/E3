const { getPendingApprovals, approveAppointment: apiApprove } = require('../../../utils/data-service')

Page({
  data: {
    list: [],
    statusMap: { pending: '待审核', approved: '已通过', rejected: '已拒绝', cancelled: '已取消', confirmed: '已核验' }
  },
  onShow() {
    this.loadData()
  },
  loadData() {
    getPendingApprovals().then(res => {
      if (res.code === 200) {
        const list = (res.data || []).map(i => ({ ...i, showGreeting: false, greeting: '' }))
        this.setData({ list })
      }
    }).catch(() => {})
  },
  onApprove(e) {
    const id = e.currentTarget.dataset.id
    wx.showLoading({ title: '审批中...' })
    apiApprove(id, 'approved').then(res => {
      wx.hideLoading()
      if (res.code === 200) {
        const list = this.data.list.map(i =>
          i.id === id ? { ...i, status: 'approved', showGreeting: true, greeting: res.data?.greeting || res.data?.greetingText || '欢迎来访' } : i
        )
        this.setData({ list })
        wx.showToast({ title: '已通过', icon: 'success' })
      } else {
        wx.showToast({ title: res.msg || '操作失败', icon: 'error' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '操作失败', icon: 'error' })
    })
  },
  onReject(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '拒绝预约', content: '确定拒绝该预约申请吗？',
      success: (res) => {
        if (res.confirm) {
          apiApprove(id, 'rejected').then(res => {
            if (res.code === 200) {
              const list = this.data.list.map(i => i.id === id ? { ...i, status: 'rejected' } : i)
              this.setData({ list })
              wx.showToast({ title: '已拒绝', icon: 'success' })
            } else {
              wx.showToast({ title: res.msg || '操作失败', icon: 'error' })
            }
          }).catch(() => {
            wx.showToast({ title: '操作失败', icon: 'error' })
          })
        }
      }
    })
  }
})
