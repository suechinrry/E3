const { lookupHost, createAppointment: apiCreateAppointment } = require('../../../utils/data-service')

Page({
  data: {
    form: {
      visitorName: '',
      visitorPhone: '',
      company: '',
      purpose: '',
      hostName: '',
      hostPhone: '',
      hostId: '',
      visitorCount: 1,
      carPlate: '',
      date: '',
      remark: ''
    },
    hostChecked: '',      // '' | 'ok' | 'fail'
    hostCheckedName: '',
    lookingUp: false,
    submitting: false
  },

  onShow() {
    this.loadUserInfo()
  },

  // ===== 自动填入访客信息 =====
  loadUserInfo() {
    const app = getApp()
    const user = app.globalData.userInfo
    if (user) {
      // 只在字段为空时自动填入（避免覆盖用户手动修改）
      this.setData({
        'form.visitorName': user.name || '',
        'form.visitorPhone': user.phone || '',
        'form.company': user.company || ''
      })
    }
  },

  // ===== 查询被访人 =====
  onLookupHost() {
    const { form } = this.data
    const name = (form.hostName || '').trim()
    const phone = (form.hostPhone || '').trim()

    if (!name) {
      wx.showToast({ title: '请输入被访人姓名', icon: 'none' })
      return
    }
    if (!phone || !/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的被访人手机号', icon: 'none' })
      return
    }

    this.setData({ lookingUp: true })
    lookupHost(name, phone).then(res => {
      this.setData({ lookingUp: false })
      if (res.code === 200 && res.data) {
        this.setData({
          'form.hostId': res.data.id,
          'form.hostName': res.data.name,
          hostChecked: 'ok',
          hostCheckedName: res.data.name
        })
        wx.showToast({ title: '已确认被访人：' + res.data.name, icon: 'success', duration: 1500 })
      } else {
        this.setData({ 'form.hostId': '', hostChecked: 'fail' })
        wx.showToast({ title: res.msg || '未找到该被访人', icon: 'error' })
      }
    }).catch(() => {
      this.setData({ lookingUp: false })
      wx.showToast({ title: '查询失败，请检查网络', icon: 'error' })
    })
  },

  // ===== 表单输入 =====
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    // 修改被访人信息时清除校验状态
    if (field === 'hostName' || field === 'hostPhone') {
      this.setData({ hostChecked: '', hostCheckedName: '', 'form.hostId': '' })
    }
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  onCountChange(e) {
    this.setData({ 'form.visitorCount': parseInt(e.detail.value) || 1 })
  },

  onDateChange(e) {
    this.setData({ 'form.date': e.detail.value })
  },

  // ===== 提交 =====
  onSubmit() {
    const { form, hostChecked } = this.data

    if (!form.visitorName || !form.visitorPhone || !form.company) {
      wx.showToast({ title: '请填写来访人信息', icon: 'none' })
      return
    }
    if (!/^1\d{10}$/.test(form.visitorPhone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (!form.hostName || !form.hostPhone) {
      wx.showToast({ title: '请填写被访人信息', icon: 'none' })
      return
    }
    if (hostChecked !== 'ok') {
      wx.showToast({ title: '请先点击「查询被访人」确认被访人存在', icon: 'none' })
      return
    }
    if (!form.purpose || !form.date) {
      wx.showToast({ title: '请填写预约详情', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    apiCreateAppointment({
      visitorName: form.visitorName,
      visitorPhone: form.visitorPhone,
      company: form.company,
      purpose: form.purpose,
      hostId: form.hostId,
      hostName: form.hostName,
      visitorCount: form.visitorCount,
      carPlate: form.carPlate || '',
      startTime: form.date + 'T09:00:00',
      endTime: form.date + 'T18:00:00',
      remark: form.remark || ''
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 200) {
        wx.showToast({ title: '预约提交成功', icon: 'success' })
        setTimeout(() => wx.redirectTo({ url: '/pages/visitor/records/records' }), 800)
      } else {
        wx.showToast({ title: res.msg || '提交失败', icon: 'error' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '提交失败，请检查网络', icon: 'error' })
    })
  }
})
