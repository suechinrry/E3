const ds = require('../../../utils/data-service')
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
    ds.updateProfile(this.data.user).then(res => {
      wx.showToast({ title: res.code === 200 ? '保存成功' : '保存失败', icon: res.code === 200 ? 'success' : 'none' })
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
