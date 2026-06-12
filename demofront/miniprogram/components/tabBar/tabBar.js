const tabsConfig = {
  visitor: [
    { page: '/pages/visitor/appoint/appoint', text: '预约申请', icon: '📋' },
    { page: '/pages/visitor/records/records', text: '预约记录', icon: '📄' },
    { page: '/pages/personal/profile/profile', text: '我的', icon: '👤' }
  ],
  host: [
    { page: '/pages/host/visited/visited', text: '被访记录', icon: '📊' },
    { page: '/pages/host/approve/approve', text: '审批', icon: '✅' },
    { page: '/pages/personal/profile/profile', text: '我的', icon: '👤' }
  ],
  admin: [
    { page: '/pages/admin/employees/employees', text: '员工', icon: '👥' },
    { page: '/pages/admin/departments/departments', text: '部门', icon: '🏢' },
    { page: '/pages/admin/approve/adApprove', text: '审核', icon: '📝' },
    { page: '/pages/admin/stats/stats', text: '统计', icon: '📈' },
    { page: '/pages/admin/system/system', text: '设置', icon: '⚙️' }
  ],
  guard: [
    { page: '/pages/guard/scan/scan', text: '扫码核验', icon: '📷' },
    { page: '/pages/personal/profile/profile', text: '我的', icon: '👤' }
  ]
}

Component({
  properties: {
    activeTab: { type: Number, value: 0 }
  },
  data: {
    tabs: []
  },
  lifetimes: {
    attached() {
      const app = getApp()
      const role = app.globalData.userInfo ? app.globalData.userInfo.role : 'visitor'
      this.setData({ tabs: tabsConfig[role] || [] })
    }
  },
  methods: {
    onTabTap(e) {
      const page = e.currentTarget.dataset.page
      const idx = e.currentTarget.dataset.index
      if (idx === this.data.activeTab) return
      wx.redirectTo({ url: page })
    }
  }
})
