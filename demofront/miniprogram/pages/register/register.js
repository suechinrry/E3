const { register } = require('../../utils/data-service')

Page({
  data: {
    username: '',
    password: '',
    confirmPassword: '',
    name: '',
    phone: '',
    company: '',
    loading: false
  },
  onUsernameInput(e) {
    this.setData({ username: e.detail.value })
  },
  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },
  onConfirmPasswordInput(e) {
    this.setData({ confirmPassword: e.detail.value })
  },
  onNameInput(e) {
    this.setData({ name: e.detail.value })
  },
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },
  onCompanyInput(e) {
    this.setData({ company: e.detail.value })
  },
  onRegister() {
    const { username, password, confirmPassword, name, phone, company } = this.data

    if (!username || !password || !name || !phone) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' })
      return
    }

    if (password !== confirmPassword) {
      wx.showToast({ title: '两次密码输入不一致', icon: 'none' })
      return
    }

    if (password.length < 6) {
      wx.showToast({ title: '密码长度至少6位', icon: 'none' })
      return
    }

    const phoneReg = /^1[3-9]\d{9}$/
    if (!phoneReg.test(phone)) {
      wx.showToast({ title: '手机号格式不正确', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    register({ username, password, name, phone, company }).then(res => {
      this.setData({ loading: false })
      if (res.code === 200) {
        wx.showToast({ title: '注册成功，请登录', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      } else {
        wx.showToast({ title: res.msg || '注册失败', icon: 'error' })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '注册失败，请检查网络', icon: 'error' })
    })
  },
  onGoLogin() {
    wx.navigateBack()
  }
})
