const ds = require('../../../utils/data-service')

Page({
  data: {
    showResult: false,
    result: null
  },
  onScan() {
    wx.showToast({ title: '模拟扫码中...', icon: 'none' })
    setTimeout(() => this.showMockResult(), 500)
  },
  onManualInput() {
    wx.showModal({
      title: '手动输入', content: '（对接后端后输入预约ID查询）\n开发阶段直接显示模拟结果',
      success: () => this.showMockResult()
    })
  },
  showMockResult() {
    this.setData({
      showResult: true,
      result: { visitorName: '模拟访客', company: '示例科技', hostName: '张三', status: 'approved' }
    })
  },
  onConfirm() {
    const result = this.data.result
    if (!result || !result.id) {
      wx.showToast({ title: '请输入预约编号', icon: 'none' })
      return
    }
    ds.verifyAppointment(result.id, 'confirmed').then(res => {
      if (res.code === 200) {
        this.setData({ result: { ...this.data.result, confirmed: true } })
        wx.showToast({ title: '已确认放行', icon: 'success' })
      } else {
        wx.showToast({ title: res.msg || '核验失败', icon: 'none' })
      }
    })
  }
})
