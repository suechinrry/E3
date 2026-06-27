const { login } = require('../../utils/data-service')

const roleHomeMap = {
  visitor: '/pages/visitor/appoint/appoint',
  host: '/pages/host/visited/visited',
  admin: '/pages/admin/employees/employees',
  guard: '/pages/guard/scan/scan'
}

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
    wx.nextTick(() => {
      this.onLogin()
    })
  },
  onGoRegister() {
    wx.navigateTo({ url: '/pages/register/register' })
  },
  onLogin() {
    const { username, password } = this.data
    if (!username || !password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    login(username, password).then(res => {
      this.setData({ loading: false })
      if (res.code === 200) {
        const { token, user } = res.data
        const userInfo = { ...user, token }
        getApp().setUserInfo(userInfo)
        wx.showToast({ title: '登录成功', icon: 'success' })
        wx.reLaunch({ url: roleHomeMap[user.role] })
      } else {
        wx.showToast({ title: res.msg || '用户名或密码错误', icon: 'error' })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '登录失败，请检查网络', icon: 'error' })
    })
  }
})
