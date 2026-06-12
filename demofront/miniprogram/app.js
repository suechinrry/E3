App({
  globalData: {
    userInfo: null,
    token: '',
    baseUrl: 'http://localhost:8080',
    useMock: false
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
