const { getOverview, getTrend, getVisitorRecordStats } = require('../../../utils/data-service')

Page({
  data: {
    overview: {},
    trend: [],
    companyRank: [],
    deptRank: [],
    maxBar: 1,
    maxCompany: 1,
    maxDept: 1,
    startDate: '',
    endDate: '',
    quickRange: 7
  },

  onShow() {
    // 默认近7天
    const today = new Date()
    const sevenAgo = new Date(today.getTime() - 7 * 86400000)
    this.setData({
      quickRange: 7,
      startDate: this.formatDate(sevenAgo),
      endDate: this.formatDate(today)
    })
    this.loadData()
  },

  formatDate(d) {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return y + '-' + m + '-' + day
  },

  // ===== 加载全部数据 =====
  loadData() {
    // 概览
    getOverview().then(res => {
      if (res.code === 200) {
        this.setData({ overview: res.data || {} })
      }
    }).catch(() => {})

    // 趋势（按选中天数）
    const days = this.data.quickRange
    getTrend(days).then(res => {
      if (res.code === 200) {
        const trend = res.data || []
        const maxVal = Math.max(...trend.map(i => i.count || 0), 1)
        this.setData({ trend, maxBar: maxVal })
      }
    }).catch(() => {})

    // 公司/部门排名（按日期区间）
    this.loadRanking()
  },

  loadRanking() {
    const { startDate, endDate } = this.data
    getVisitorRecordStats(startDate, endDate).then(res => {
      if (res.code === 200) {
        const data = res.data || {}
        const companyRank = data.companyStats || data.companyRank || []
        const deptRank = data.deptStats || data.deptRank || []
        this.setData({
          companyRank,
          deptRank,
          maxCompany: Math.max(...companyRank.map(i => i.count || 0), 1),
          maxDept: Math.max(...deptRank.map(i => i.count || 0), 1)
        })
      }
    }).catch(() => {})
  },

  // ===== 日期选择 =====
  onStartChange(e) {
    this.setData({ startDate: e.detail.value, quickRange: 0 })
  },
  onEndChange(e) {
    this.setData({ endDate: e.detail.value, quickRange: 0 })
  },
  onQuery() {
    this.loadRanking()
    wx.showToast({ title: '已更新', icon: 'success', duration: 1000 })
  },
  onQuickDate(e) {
    const days = parseInt(e.currentTarget.dataset.days)
    const today = new Date()
    const start = new Date(today.getTime() - days * 86400000)
    this.setData({
      quickRange: days,
      startDate: this.formatDate(start),
      endDate: this.formatDate(today)
    })
    // 刷新趋势和排名
    getTrend(days).then(res => {
      if (res.code === 200) {
        const trend = res.data || []
        this.setData({ trend, maxBar: Math.max(...trend.map(i => i.count || 0), 1) })
      }
    }).catch(() => {})
    this.loadRanking()
  }
})
