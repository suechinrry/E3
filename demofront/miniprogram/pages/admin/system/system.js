const ds = require('../../../utils/data-service')

Page({
  data: { settings: {} },
  onShow() {
    ds.getSystemSettings().then(res => {
      if (res.code === 200) this.setData({ settings: res.data })
    })
  },
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`settings.${field}`]: e.detail.value })
  },
  onSave() {
    ds.updateSystemSettings(this.data.settings).then(res => {
      wx.showToast({ title: res.code === 200 ? '设置已保存' : '保存失败', icon: res.code === 200 ? 'success' : 'none' })
    })
  },
  goPage(e) {
    wx.navigateTo({ url: e.currentTarget.dataset.url })
  }
})
