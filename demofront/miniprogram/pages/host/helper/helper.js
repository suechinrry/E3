const { helperAppointment } = require('../../../utils/data-service')

Page({
  data: {
    form: {
      visitorName: '', visitorPhone: '', company: '', purpose: '', date: ''
    },
    submitting: false
  },
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },
  onDateChange(e) {
    this.setData({ 'form.date': e.detail.value })
  },
  onSubmit() {
    const { form } = this.data
    if (!form.visitorName || !form.visitorPhone || !form.company || !form.purpose || !form.date) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' })
      return
    }
    if (!/^1\d{10}$/.test(form.visitorPhone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    helperAppointment({
      visitorName: form.visitorName,
      visitorPhone: form.visitorPhone,
      company: form.company,
      purpose: form.purpose,
      startTime: form.date + 'T09:00:00',
      endTime: form.date + 'T18:00:00'
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 200) {
        wx.showToast({ title: '代填预约成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack()
        }, 1000)
      } else {
        wx.showToast({ title: res.msg || '提交失败', icon: 'error' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '提交失败，请检查网络', icon: 'error' })
    })
  }
})
