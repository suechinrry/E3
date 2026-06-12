const ds = require('../../../utils/data-service')

Page({
  data: {
    list: [],
    statusMap: { pending: '待审核', approved: '已通过', rejected: '已拒绝', cancelled: '已取消', confirmed: '已核验' }
  },
  onShow() {
    ds.getPendingApprovals().then(res => {
      if (res.code === 200) this.setData({ list: res.data || [] })
    })
  },
  onApprove(e) {
    const id = e.currentTarget.dataset.id
    wx.showLoading({ title: '审批中...' })
    ds.approveAppointment(id, 'approved').then(res => {
      wx.hideLoading()
      if (res.code === 200) {
        const greeting = res.data && (res.data.greeting || res.data.greetingText)
        const list = this.data.list.map(i =>
          i.id === id ? { ...i, status: 'approved', showGreeting: true, greeting: greeting || '话术已生成' } : i
        )
        this.setData({ list })
        wx.showToast({ title: '已通过 · AI话术已生成', icon: 'success' })
      }
    })
  },
  onReject(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '拒绝预约', content: '确定拒绝该预约申请吗？',
      success: (res) => {
        if (res.confirm) {
          ds.approveAppointment(id, 'rejected').then(() => {
            const list = this.data.list.map(i => i.id === id ? { ...i, status: 'rejected' } : i)
            this.setData({ list })
            wx.showToast({ title: '已拒绝', icon: 'success' })
          })
        }
      }
    })
  }
})
