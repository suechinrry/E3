const { getProfile, updateProfile } = require('../../../utils/data-service')

const roleMap = { admin: '管理员', host: '被访人', visitor: '访客', guard: '门岗' }

Page({
  data: {
    user: {},
    avatarText: '',
    roleText: ''
  },
  onShow() {
    this.loadData()
  },
  loadData() {
    const app = getApp()
    const cachedUser = app.globalData.userInfo
    if (cachedUser) {
      this.renderUser(cachedUser)
    }
    getProfile().then(res => {
      if (res.code === 200 && res.data) {
        const user = { ...cachedUser, ...res.data }
        // 同步到 globalData
        app.globalData.userInfo = user
        this.renderUser(user)
      }
    }).catch(() => {
      // 网络错误时使用缓存
      if (cachedUser) this.renderUser(cachedUser)
    })
  },
  renderUser(user) {
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
    updateProfile(user).then(res => {
      if (res.code === 200) {
        const app = getApp()
        app.globalData.userInfo = { ...app.globalData.userInfo, ...user }
        this.renderUser(user)
        wx.showToast({ title: '保存成功', icon: 'success' })
      } else {
        wx.showToast({ title: res.msg || '保存失败', icon: 'error' })
      }
    }).catch(() => {
      wx.showToast({ title: '保存失败', icon: 'error' })
    })
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
