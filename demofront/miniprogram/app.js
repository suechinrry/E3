App({
  globalData: {
    userInfo: null,
    token: '',
    baseUrl: 'http://localhost:8080'
  },
  onLaunch() {
    // 不恢复 token：每次冷启动强制重新登录，避免残留 token 导致角色错乱
    wx.removeStorageSync('token')
  },
  setUserInfo(user) {
    this.globalData.userInfo = user
    this.globalData.token = user.token || ''
    wx.setStorageSync('token', user.token || '')
  },
  logout() {
    this.globalData.userInfo = null
    this.globalData.token = ''
    wx.removeStorageSync('token')
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
