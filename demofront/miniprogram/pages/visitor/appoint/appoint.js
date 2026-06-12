const ds = require('../../../utils/data-service')

Page({
  data: {
    form: {
      visitorName: '', visitorPhone: '', company: '', purpose: '',
      hostName: '', hostId: '', visitorCount: 1,
      carPlate: '', date: '', remark: ''
    },
    hostList: [],
    submitting: false
  },
  onShow() {
    ds.getEmployees(1, 100).then(res => {
      if (res.code === 200) {
        const list = (res.data.list || []).map(e => ({ id: e.id, name: e.name, department: e.department }))
        this.setData({ hostList: list })
      }
    })
  },
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },
  onHostChange(e) {
    const idx = e.detail.value
    const host = this.data.hostList[idx]
    if (host) this.setData({ 'form.hostName': host.name, 'form.hostId': host.id })
  },
  onCountChange(e) {
    this.setData({ 'form.visitorCount': e.detail.value })
  },
  onDateChange(e) {
    this.setData({ 'form.date': e.detail.value })
  },
  onSubmit() {
    const { form } = this.data
    if (!form.visitorName || !form.visitorPhone || !form.company || !form.purpose || !form.hostName || !form.date) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    const data = { ...form, startTime: form.date + ' 09:00', endTime: form.date + ' 17:00' }
    ds.createAppointment(data).then(res => {
      this.setData({ submitting: false })
      if (res.code === 200) {
        wx.showToast({ title: '预约提交成功', icon: 'success' })
        wx.redirectTo({ url: '/pages/visitor/records/records' })
      } else {
        wx.showToast({ title: res.msg || '提交失败', icon: 'none' })
      }
    })
  }
})
