const roleMap = { admin: '管理员', host: '被访人', visitor: '访客', guard: '门岗' }

Page({
  data: {
    user: {},
    avatarText: '',
    roleText: ''
  },
  onShow() {
    const app = getApp()
    const user = app.globalData.userInfo || {}
    this.setData({
      user: { ...user },
      avatarText: user.name ? user.name.charAt(0) : '?',
      roleText: roleMap[user.role] || ''
    })
  },
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`user.${field}`]: e.detail.value })
  },
  onSave() {
    const { user } = this.data
    if (!user.name || !user.name.trim()) {
      wx.showToast({ title: '姓名不能为空', icon: 'none' })
      return
    }
    if (user.phone && !/^1\d{10}$/.test(user.phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    const app = getApp()
    app.globalData.userInfo = { ...app.globalData.userInfo, ...user }
    this.setData({
      user: { ...app.globalData.userInfo },
      avatarText: user.name ? user.name.charAt(0) : '?'
    })
    wx.showToast({ title: '保存成功', icon: 'success' })
  },
  onLogout() {
    wx.showModal({
      title: '提示', content: '确定退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          getApp().logout()
        }
      }
    })
  }
})
