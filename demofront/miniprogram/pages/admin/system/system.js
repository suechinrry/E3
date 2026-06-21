const { getSettings, updateSettings } = require('../../../utils/data-service')

Page({
  data: {
    settings: {},
    saved: false
  },
  onShow() {
    this.loadData()
  },
  loadData() {
    getSettings().then(res => {
      if (res.code === 200) {
        const raw = res.data || {}
        // 后端返回 snake_case 键名，前端用 camelCase 展示
        this.setData({
          settings: {
            companyName: raw.company_name || '',
            contactPhone: raw.contact_phone || '',
            address: raw.company_address || '',
            workingHours: raw.working_hours || '',
            visitorNotice: raw.visitor_notice || '',
            logoUrl: raw.logo_url || ''
          }
        })
      }
    }).catch(() => {})
  },
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [`settings.${field}`]: e.detail.value,
      saved: false
    })
  },
  onSave() {
    const s = this.data.settings
    // 前端 camelCase 转后端 snake_case
    updateSettings({
      company_name: s.companyName,
      contact_phone: s.contactPhone,
      company_address: s.address,
      working_hours: s.workingHours,
      visitor_notice: s.visitorNotice
    }).then(res => {
      if (res.code === 200) {
        this.setData({ saved: true })
        wx.showToast({ title: '设置已保存', icon: 'success' })
      } else {
        wx.showToast({ title: res.msg || '保存失败', icon: 'error' })
      }
    }).catch(() => {
      wx.showToast({ title: '保存失败', icon: 'error' })
    })
  },
  goPage(e) {
    wx.navigateTo({ url: e.currentTarget.dataset.url })
  }
})
