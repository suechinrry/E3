const ds = require('../../utils/data-service')
const { roleHomeMap } = require('../../mock/user')

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
  onLogin() {
    const { username, password } = this.data
    if (!username || !password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    ds.login(username, password).then(res => {
      this.setData({ loading: false })
      if (res.code === 200) {
        const userInfo = res.data.user || res.data
        userInfo.token = res.data.token
        getApp().setUserInfo(userInfo)
        wx.showToast({ title: '登录成功', icon: 'success' })
        wx.reLaunch({ url: roleHomeMap[userInfo.role] })
      } else {
        wx.showToast({ title: res.msg || '用户名或密码错误', icon: 'error' })
      }
    })
  }
})
