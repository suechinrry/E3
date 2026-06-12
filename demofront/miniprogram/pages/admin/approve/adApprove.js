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
    ds.getAdminPending(1, 50).then(res => {
      if (res.code === 200) {
        let list = res.data.list || []
        if (this.data.currentTab === 'pending') list = list.filter(i => i.status === 'pending')
        this.setData({ list })
      }
    })
  },
  switchTab(e) {
    this.setData({ currentTab: e.currentTarget.dataset.tab }, () => this.loadData())
  },
  onApprove(e) {
    const id = e.currentTarget.dataset.id
    ds.approveAppointment(id, 'approved').then(() => {
      wx.showToast({ title: '已通过', icon: 'success' })
      this.loadData()
    })
  },
  onReject(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '拒绝预约', content: '确定拒绝该预约吗？',
      success: (res) => {
        if (res.confirm) {
          ds.approveAppointment(id, 'rejected').then(() => {
            wx.showToast({ title: '已拒绝', icon: 'success' })
            this.loadData()
          })
        }
      }
    })
  },
  goNotifyMgr() {
    wx.navigateTo({ url: '/pages/admin/notify-mgr/notify-mgr' })
  }
})
