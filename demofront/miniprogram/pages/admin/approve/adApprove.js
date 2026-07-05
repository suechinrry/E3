const { getAllAppointments, getAdminPending, approveAppointment: apiApprove, getRiskAssessment, reassessRisk } = require('../../../utils/data-service')

Page({
  data: {
    currentTab: 'all',
    list: [],
    statusMap: { pending: '待审核', approved: '已通过', rejected: '已拒绝', cancelled: '已取消', confirmed: '已核验' },
    riskMap: { low: '低风险', medium: '中风险', high: '高风险' }
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
          expanded: !!this._expandedCache[i.id],
          riskAssessed: false,
          riskLevel: '',
          riskScore: 0,
          riskReason: ''
        }))
        this.setData({ list })
        // 异步加载待审核预约的风险评估
        this.loadRiskInfo(list)
      }
    }).catch(() => {})
  },

  // 批量加载风险评估（仅待审核的预约）
  loadRiskInfo(list) {
    const pendingItems = list.filter(i => i.status === 'pending')
    if (pendingItems.length === 0) return
    pendingItems.forEach(item => {
      getRiskAssessment(item.id).then(res => {
        if (res.code === 200 && res.data && res.data.assessed) {
          const idx = this.data.list.findIndex(i => i.id === item.id)
          if (idx >= 0) {
            this.setData({
              ['list[' + idx + '].riskAssessed']: true,
              ['list[' + idx + '].riskLevel']: res.data.riskLevel,
              ['list[' + idx + '].riskScore']: res.data.riskScore,
              ['list[' + idx + '].riskReason']: res.data.reason || ''
            })
          }
        }
      }).catch(() => {})
    })
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

  onReassessRisk(e) {
    const id = e.currentTarget.dataset.id
    const idx = this.data.list.findIndex(i => i.id === id)
    const that = this
    wx.showLoading({ title: '重新评估中...' })
    if (idx >= 0) { this.setData({ ['list[' + idx + '].riskAssessed']: false, ['list[' + idx + '].riskLevel']: '', ['list[' + idx + '].riskScore']: 0, ['list[' + idx + '].riskReason']: '正在重新评估...' }) }
    reassessRisk(id).then(res => {
      wx.hideLoading()
      if (res.code === 200) {
        // 轮询刷新，最多等 10 秒
        var retry = 0
        var timer = setInterval(function() {
          retry++
          getRiskAssessment(id).then(function(r) {
            if (r.code === 200 && r.data && r.data.assessed) {
              clearInterval(timer)
              if (idx >= 0) {
                that.setData({ ['list[' + idx + '].riskAssessed']: true, ['list[' + idx + '].riskLevel']: r.data.riskLevel, ['list[' + idx + '].riskScore']: r.data.riskScore, ['list[' + idx + '].riskReason']: r.data.reason || '' })
              }
            } else if (retry >= 10) {
              clearInterval(timer)
              if (idx >= 0) { that.setData({ ['list[' + idx + '].riskReason']: '评估超时，请稍后刷新' }) }
            }
          }).catch(function() {
            if (retry >= 10) clearInterval(timer)
          })
        }, 1000)
      } else { wx.showToast({ title: '评估失败', icon: 'error' }) }
    }).catch(() => { wx.hideLoading(); wx.showToast({ title: '评估失败', icon: 'error' }) })
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
    const that = this
    wx.showModal({
      title: '拒绝预约',
      content: '请输入拒绝原因（可选）',
      editable: true,
      placeholderText: '如：时间冲突、信息不实等',
      success: (res) => {
        if (res.confirm) {
          const remark = res.content || ''
          wx.showLoading({ title: '处理中...' })
          apiApprove(id, 'rejected', remark).then(res => {
            wx.hideLoading()
            if (res.code === 200) {
              const list = that.data.list.map(i =>
                i.id === id ? { ...i, status: 'rejected', remark: remark } : i
              )
              that.setData({ list })
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
