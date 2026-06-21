const { verifyAppointment, verifyAppointmentByName, confirmAppointment } = require('../../../utils/data-service')

Page({
  data: {
    showResult: false,
    showManual: false,
    result: null,
    manualName: '',
    manualPhone: ''
  },

  // ===== 扫码核验 =====
  onScan() {
    wx.scanCode({
      scanType: ['qrCode', 'barCode'],
      success: (res) => {
        wx.showLoading({ title: '核验中...' })
        this.doVerifyCode(res.result)
      },
      fail: () => {
        wx.showToast({ title: '扫码失败，请重试', icon: 'none' })
      }
    })
  },

  doVerifyCode(code) {
    verifyAppointment(code).then(res => {
      wx.hideLoading()
      if (res.code === 200) {
        this.showResultData({ ...res.data, confirmed: false })
      } else {
        this.showResultData({ valid: false, message: res.msg || '未找到对应预约', confirmed: false })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '核验失败', icon: 'error' })
    })
  },

  // ===== 手动核验表单 =====
  onShowManual() {
    this.setData({ showManual: true, manualName: '', manualPhone: '' })
  },
  onHideManual() {
    this.setData({ showManual: false })
  },
  onNameInput(e) {
    this.setData({ manualName: e.detail.value })
  },
  onPhoneInput(e) {
    this.setData({ manualPhone: e.detail.value })
  },
  onManualVerify() {
    const name = (this.data.manualName || '').trim()
    const phone = (this.data.manualPhone || '').trim()
    if (!name) {
      wx.showToast({ title: '请输入访客姓名', icon: 'none' })
      return
    }
    if (!phone || !/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    wx.showLoading({ title: '核验中...' })
    verifyAppointmentByName(name, phone).then(res => {
      wx.hideLoading()
      if (res.code === 200 && res.data && res.data.valid) {
        this.setData({ showManual: false })
        this.showResultData({ ...res.data, confirmed: false })
      } else {
        this.showResultData({ valid: false, message: res.msg || (res.data && res.data.message) || '未找到有效预约', confirmed: false })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '核验失败', icon: 'error' })
    })
  },

  showResultData(data) {
    this.setData({ showResult: true, result: data })
  },

  // ===== 确认放行 =====
  onConfirm() {
    if (!this.data.result || !this.data.result.appointmentId) return
    wx.showLoading({ title: '确认中...' })
    confirmAppointment(this.data.result.appointmentId).then(res => {
      wx.hideLoading()
      if (res.code === 200) {
        this.setData({ 'result.confirmed': true })
        wx.showToast({ title: '已确认放行', icon: 'success' })
      } else {
        wx.showToast({ title: res.msg || '操作失败', icon: 'error' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '操作失败', icon: 'error' })
    })
  },

  onReset() {
    this.setData({ showResult: false, showManual: false, result: null })
  }
})
