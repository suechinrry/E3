const { login, roleHomeMap } = require('../../mock/user')

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },
  onUsernameInput(e) {
    this.setData({ username: e.detail.value })
  },
  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },
  onQuickLogin(e) {
    const { user, pwd } = e.currentTarget.dataset
    this.setData({ username: user, password: pwd })
    // 自动触发登录
    wx.nextTick(() => {
      this.onLogin()
    })
  },
  onLogin() {
    const { username, password } = this.data
    if (!username || !password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    setTimeout(() => {
      this.setData({ loading: false })
      const userInfo = login(username, password)
      if (userInfo) {
        getApp().setUserInfo(userInfo)
        wx.showToast({ title: '登录成功', icon: 'success' })
        wx.reLaunch({ url: roleHomeMap[userInfo.role] })
      } else {
        wx.showToast({ title: '用户名或密码错误', icon: 'error' })
      }
    }, 800)
  }
})
