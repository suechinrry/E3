const { getUserNotifications, markNotificationRead, markAllNotificationsRead } = require('../../../utils/data-service')

Page({
  data: {
    currentTab: 'unread',
    list: [],
    hasUnread: false,
    popupVisible: false,
    popupNotification: {},
    activeTab: 2
  },
  onShow() {
    this.setActiveTab()
    this.loadData()
  },
  setActiveTab() {
    const app = getApp()
    const role = app.globalData.userInfo ? app.globalData.userInfo.role : 'visitor'
    const tabMap = { visitor: 2, host: 2, guard: 1, admin: 3 }
    this.setData({ activeTab: tabMap[role] || 2 })
  },
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ currentTab: tab }, () => this.loadData())
  },
  loadData() {
    getUserNotifications().then(res => {
      if (res.code === 200) {
        const allList = Array.isArray(res.data) ? res.data : []
        const tab = this.data.currentTab
        const filteredList = tab === 'unread'
          ? allList.filter(function(n) { return !n.isRead })
          : tab === 'read'
            ? allList.filter(function(n) { return n.isRead })
            : allList

        this.setData({
          list: filteredList,
          hasUnread: allList.some(function(n) { return !n.isRead })
        })
      }
    }).catch(function() {
      wx.showToast({ title: '加载失败', icon: 'error' })
    })
  },
  onTapItem(e) {
    const item = e.currentTarget.dataset.item
    const idx = e.currentTarget.dataset.index

    // 未读通知：点击后标记已读
    if (!item.isRead) {
      markNotificationRead(item.id).catch(() => {})
      // 立即更新本地状态
      if (idx >= 0) {
        const key = 'list[' + idx + '].isRead'
        this.setData({ [key]: 1 })
        this.setData({ hasUnread: this.data.list.some(function(n) { return !n.isRead }) })
      }
    }

    // 根据通知类型决定行为
    if (item.type === 'approved') {
      const app = getApp()
      const role = app.globalData.userInfo ? app.globalData.userInfo.role : 'visitor'
      if (role === 'host') {
        wx.navigateTo({ url: '/pages/host/visited/visited' })
      } else {
        wx.navigateTo({ url: '/pages/visitor/records/records' })
      }
    } else if (item.type === 'risk_warning') {
      // 风险预警通知：跳转到管理员审批页
      wx.navigateTo({ url: '/pages/admin/approve/adApprove' })
    } else {
      // 显示详情弹窗
      this.setData({
        popupVisible: true,
        popupNotification: item
      })
    }
  },
  onPopupConfirm(e) {
    this.setData({ popupVisible: false })
  },
  onPopupClose() {
    this.setData({ popupVisible: false })
  },
  onMarkAllRead() {
    markAllNotificationsRead().then(res => {
      if (res.code === 200) {
        const list = this.data.list.map(function(n) { return Object.assign({}, n, { isRead: 1 }) })
        this.setData({ list: list, hasUnread: false })
        wx.showToast({ title: '已全部标记为已读', icon: 'success' })
      }
    }).catch(function() {})
  }
})
