const ds = require('../../../utils/data-service')

Page({
  data: {
    list: [],
    stats: { total: 0, thisMonth: 0, companies: 0 },
    statusMap: { pending: '待审核', approved: '已通过', rejected: '已拒绝', cancelled: '已取消', confirmed: '已核验' }
  },
  onShow() {
    ds.getHostRecords(1, 50).then(res => {
      if (res.code === 200) this.setData({ list: res.data.list || [] })
    })
    ds.getHostStats().then(res => {
      if (res.code === 200) this.setData({ stats: res.data })
    })
  },
  goHelper() {
    wx.navigateTo({ url: '/pages/host/helper/helper' })
  }
})
