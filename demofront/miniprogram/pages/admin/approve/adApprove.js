const { getAllAppointments, getAdminPending, approveAppointment: apiApprove } = require('../../../utils/data-service')

Page({
  data: {
    currentTab: 'all',
    list: [],
    statusMap: { pending: '待审核', approved: '已通过', rejected: '已拒绝', cancelled: '已取消', confirmed: '已核验' }
  },
  _expandedCache: {},

  onShow() {
    this.loadData()
  },

  loadData() {
    const tab = this.data.currentTab
    let apiCall
    if (tab === 'pending') {
      apiCall = getAdminPending(1, 100)
    } else {
      apiCall = getAllAppointments(1, 100, tab)
    }

    apiCall.then(res => {
      if (res.code === 200) {
        let list = res.data.list || res.data || []
        if (tab === 'pending') {
          list = list.filter(i => i.status === 'pending')
        }
        list = list.map(i => ({
          ...i,
          expanded: !!this._expandedCache[i.id]
        }))
        this.setData({ list })
      }
    }).catch(() => {})
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ currentTab: tab }, () => this.loadData())
  },

  onToggleDetail(e) {
    const id = e.currentTarget.dataset.id
    this._expandedCache[id] = !this._expandedCache[id]
    const list = this.data.list.map(i =>
      i.id === id ? { ...i, expanded: !!this._expandedCache[id] } : i
    )
    this.setData({ list })
  },

  onApprove(e) {
    const id = e.currentTarget.dataset.id
    wx.showLoading({ title: '审批中...' })
    apiApprove(id, 'approved').then(res => {
      wx.hideLoading()
      if (res.code === 200) {
        const list = this.data.list.map(i =>
          i.id === id ? { ...i, status: 'approved' } : i
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
          wx.showLoading({ title: '处理中...' })
          apiApprove(id, 'rejected').then(res => {
            wx.hideLoading()
            if (res.code === 200) {
              const list = this.data.list.map(i =>
                i.id === id ? { ...i, status: 'rejected' } : i
              )
              this.setData({ list })
              wx.showToast({ title: '已拒绝', icon: 'success' })
            } else {
              wx.showToast({ title: res.msg || '操作失败', icon: 'error' })
            }
          }).catch(() => {
            wx.hideLoading()
            wx.showToast({ title: '操作失败', icon: 'error' })
          })
        }
      }
    })
  },

  goNotifyMgr() {
    wx.navigateTo({ url: '/pages/admin/notify-mgr/notify-mgr' })
  },
  goStats() {
    wx.redirectTo({ url: '/pages/admin/stats/stats' })
  }
})
