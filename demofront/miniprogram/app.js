App({
  globalData: {
    userInfo: null,
    token: '',
    baseUrl: 'http://localhost:8080',
    pendingNotifications: []
  },
  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
      this.checkPopupNotifications()
    }
  },
  setUserInfo(user) {
    this.globalData.userInfo = user
    this.globalData.token = user.token || ''
    wx.setStorageSync('token', user.token || '')
    // 登录后检查未读弹窗通知
    this.checkPopupNotifications()
  },
  logout() {
    this.globalData.userInfo = null
    this.globalData.token = ''
    this.globalData.pendingNotifications = []
    wx.removeStorageSync('token')
    wx.reLaunch({ url: '/pages/login/login' })
  },
  checkPopupNotifications() {
    if (!this.globalData.token) return
    const { request } = require('./utils/request')
    const API = require('./utils/api')
    request({ url: API.USER_NOTIFICATION_POPUP }).then(res => {
      if (res.code === 200 && res.data && res.data.length > 0) {
        this.globalData.pendingNotifications = res.data
        this.triggerNotificationPopup()
      }
    }).catch(function() {})
  },
  triggerNotificationPopup() {
    const notifications = this.globalData.pendingNotifications
    if (!notifications || notifications.length === 0) return

    const n = notifications[0]
    // 获取当前页面来显示弹窗
    const pages = getCurrentPages()
    if (pages.length === 0) return
    const currentPage = pages[pages.length - 1]

    if (currentPage.showNotificationPopup) {
      currentPage.showNotificationPopup(n)
    } else {
      // 如果当前页面没有弹窗方法，跳转到通知中心
      wx.navigateTo({ url: '/pages/visitor/notification-center/notification-center' })
    }
  },
  onPopupConfirm(notification) {
    // 从待处理列表中移除已确认的通知
    const list = this.globalData.pendingNotifications.filter(n => n.id !== notification.id)
    this.globalData.pendingNotifications = list
    // 如果还有未读通知，继续弹下一个
    if (list.length > 0) {
      this.triggerNotificationPopup()
    }
  }
})
