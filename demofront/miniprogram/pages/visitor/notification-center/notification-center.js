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
    this.checkPendingPopup()
  },
  setActiveTab() {
    const app = getApp()
    const role = app.globalData.userInfo ? app.globalData.userInfo.role : 'visitor'
    const tabMap = { visitor: 2, host: 2, guard: 1, admin: 3 }
    this.setData({ activeTab: tabMap[role] || 2 })
  },
  checkPendingPopup() {
    const app = getApp()
    const pending = app.globalData.pendingNotifications
    if (pending && pending.length > 0) {
      this.showNotificationPopup(pending[0])
    }
  },
  showNotificationPopup(notification) {
    this.setData({ popupVisible: true, popupNotification: notification })
  },
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ currentTab: tab }, () => this.loadData())
  },
  loadData() {
    // 后端 /user-notification/list 已合并个人通知+公共公告，只需一次请求
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
    if (item.type === 'approved') {
      // 预约审批通过通知，跳转到预约记录页面
      wx.navigateTo({ url: '/pages/visitor/records/records' })
    } else if (item.type === 'greeting' || item.type === 'announcement' || !item.isRead) {
      this.setData({
        popupVisible: true,
        popupNotification: item
      })
    }
  },
  onPopupConfirm(e) {
    const notification = e.detail.notification
    getApp().onPopupConfirm(notification)
    const idx = this.data.list.findIndex(n => n.id === notification.id)
    if (idx >= 0) {
      this.setData({
        ['list[' + idx + '].isRead']: 1,
        popupVisible: false
      })
      this.setData({ hasUnread: this.data.list.some(function(n) { return !n.isRead }) })
    } else {
      this.setData({ popupVisible: false })
    }
    setTimeout(function() { this.checkPendingPopup() }.bind(this), 300)
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
