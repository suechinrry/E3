const { mockVerifyResult } = require('../../../mock/greeting')

const verifyResults = [
  { ...mockVerifyResult },
  {
    valid: true,
    visitorName: '王小明',
    company: 'ABC科技有限公司',
    hostName: '张三',
    time: '2026-06-12 14:00-15:00',
    message: '预约有效，请在约定时间到访'
  },
  {
    valid: false,
    visitorName: '',
    company: '',
    hostName: '',
    time: '',
    message: '未找到对应的预约记录，请核实'
  }
]
let verifyIdx = 0

Page({
  data: {
    showResult: false,
    result: null
  },
  onScan() {
    wx.scanCode({
      scanType: ['qrCode', 'barCode'],
      success: (res) => {
        wx.showLoading({ title: '核验中...' })
        setTimeout(() => {
          wx.hideLoading()
          // 通过扫描结果进行核验
          this.onVerifyCode(res.result)
        }, 800)
      },
      fail: (err) => {
        console.log('扫码失败', err)
        // 使用模拟扫码
        wx.showToast({ title: '模拟扫码中...', icon: 'none', duration: 1200 })
        setTimeout(() => this.showMockResult(), 800)
      }
    })
  },
  onManualInput() {
    wx.showModal({
      title: '手动输入预约ID',
      editable: true,
      placeholderText: '请输入预约ID',
      success: (res) => {
        if (res.confirm && res.content) {
          wx.showLoading({ title: '核验中...' })
          setTimeout(() => {
            wx.hideLoading()
            this.onVerifyCode(res.content)
          }, 600)
        } else if (res.confirm) {
          // 未输入时使用模拟
          this.showMockResult()
        }
      }
    })
  },
  onVerifyCode(code) {
    // 对接后端：调用核验API
    // 当前使用模拟数据循环展示不同结果
    const result = verifyResults[verifyIdx % verifyResults.length]
    verifyIdx++
    this.setData({
      showResult: true,
      result: {
        ...result,
        confirmed: false,
        code: code
      }
    })
  },
  showMockResult() {
    const result = verifyResults[verifyIdx % verifyResults.length]
    verifyIdx++
    this.setData({
      showResult: true,
      result: { ...result, confirmed: false }
    })
  },
  onConfirm() {
    wx.showLoading({ title: '确认中...' })
    setTimeout(() => {
      this.setData({ 'result.confirmed': true })
      wx.hideLoading()
      wx.showToast({ title: '已确认放行', icon: 'success' })
    }, 600)
  },
  onReset() {
    this.setData({ showResult: false, result: null })
  }
})
