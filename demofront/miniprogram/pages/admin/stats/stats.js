const ds = require('../../../utils/data-service')

Page({
  data: {
    stats: { totalAppointments: 0, pendingCount: 0, approvedCount: 0, todayVisits: 0, uniqueCompanies: 0 },
    dailyStats: [],
    loading: true
  },
  onShow() {
    this.loadData()
  },
  loadData() {
    this.setData({ loading: true })
    ds.getSystemStats().then(res => {
      if (res.code === 200) {
        const data = res.data
        this.setData({
          stats: {
            totalAppointments: data.totalAppointments || data.total || 0,
            pendingCount: data.pendingCount || 0,
            approvedCount: data.approvedCount || 0,
            todayVisits: data.todayVisits || data.todayCount || 0,
            uniqueCompanies: data.uniqueCompanies || data.companyCount || 0
          },
          loading: false
        })
      } else {
        this.setData({ loading: false })
      }
    })
    ds.getDailyStats('week').then(res => {
      if (res.code === 200) this.setData({ dailyStats: res.data || [] })
    })
  }
})
