const { mockSystemSettings } = require('../../../mock/notification')

let systemSettings = { ...mockSystemSettings }

Page({
  data: {
    settings: { ...systemSettings },
    saved: false
  },
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [`settings.${field}`]: e.detail.value,
      saved: false
    })
  },
  onSave() {
    systemSettings = { ...this.data.settings }
    this.setData({ saved: true })
    wx.showToast({ title: '设置已保存', icon: 'success' })
  },
  goPage(e) {
    wx.navigateTo({ url: e.currentTarget.dataset.url })
  }
})
