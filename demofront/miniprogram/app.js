App({
  globalData: {
    userInfo: null,
    token: '',
    baseUrl: 'http://localhost:8080',
    // useMock: true  → 使用本地模拟数据（不连后端）
    // useMock: false → 连接真实后端 API
    useMock: false
  },
  onLaunch() {
    // 启动时尝试从本地恢复登录状态
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
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
