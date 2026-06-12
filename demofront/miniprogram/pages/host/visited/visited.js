const { getHostRecords, getHostStats, getGreeting, regenerateGreeting } = require('../../../utils/data-service')

Page({
  data: {
    list: [],
    stats: {},
    statusMap: {
      pending: '待审核', approved: '已通过', rejected: '已拒绝',
      cancelled: '已取消', confirmed: '已核验'
    }
  },
  onShow() {
    this.loadData()
  },
  loadData() {
    getHostRecords(1, 100).then(res => {
      if (res.code === 200) {
        const list = (res.data.list || []).map(item => ({
          ...item,
          greetingText: '',
          seatSuggestion: '',
          notes: '',
          greetingSource: ''
        }))
        this.setData({ list })
        // 异步拉取已通过预约的 AI 话术
        list.forEach((item, idx) => {
          if (item.status === 'approved') {
            getGreeting(item.id).then(gRes => {
              if (gRes.code === 200 && gRes.data) {
                this.setData({
                  [`list[${idx}].greetingText`]: gRes.data.greeting || gRes.data.greetingText || '',
                  [`list[${idx}].seatSuggestion`]: gRes.data.seatSuggestion || '',
                  [`list[${idx}].notes`]: gRes.data.notes || '',
                  [`list[${idx}].greetingSource`]: gRes.data.source || 'ai'
                })
              }
            }).catch(() => {})
          }
        })
      }
    }).catch(() => {})
    getHostStats().then(res => {
      if (res.code === 200) {
        const raw = res.data || {}
        const companies = raw.companyStats || []
        this.setData({
          stats: {
            total: raw.totalVisits || 0,
            thisMonth: 0,
            companies: companies.length
          }
        })
      }
    }).catch(() => {})
  },
  onRegenerate(e) {
    const id = e.currentTarget.dataset.id
    wx.showLoading({ title: 'AI生成中...' })
    regenerateGreeting(id).then(res => {
      wx.hideLoading()
      if (res.code === 200 && res.data) {
        const source = res.data.source
        const msg = source === 'ai' ? '✅ AI调用成功！' : '⚠️ 使用模板生成'
        wx.showModal({
          title: source === 'ai' ? '生成成功' : '提示',
          content: msg,
          showCancel: false
        })
        // 刷新当前项的话术
        const list = this.data.list.map(item => {
          if (item.id === id) {
            return {
              ...item,
              greetingText: res.data.greeting || '',
              seatSuggestion: res.data.seatSuggestion || '',
              notes: res.data.notes || '',
              greetingSource: source || ''
            }
          }
          return item
        })
        this.setData({ list })
      } else {
        wx.showToast({ title: res.msg || '生成失败', icon: 'error' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '网络异常', icon: 'error' })
    })
  },

  goHelper() {
    wx.navigateTo({ url: '/pages/host/helper/helper' })
  }
})
