const { mockHolidays } = require('../../../mock/notification')

let holidayData = [...mockHolidays]

Page({
  data: {
    list: holidayData,
    showForm: false,
    form: { name: '', date: '', type: '法定' },
    typeOptions: ['法定', '公司', '调休']
  },
  onAdd() {
    this.setData({
      showForm: true,
      form: { name: '', date: '', type: '法定' }
    })
  },
  hideForm() {
    this.setData({ showForm: false })
  },
  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },
  onDateChange(e) {
    this.setData({ 'form.date': e.detail.value })
  },
  onTypeChange(e) {
    const idx = e.detail.value
    this.setData({ 'form.type': this.data.typeOptions[idx] })
  },
  onSave() {
    const { form } = this.data
    if (!form.name.trim()) {
      wx.showToast({ title: '请输入节日名称', icon: 'none' })
      return
    }
    if (!form.date) {
      wx.showToast({ title: '请选择日期', icon: 'none' })
      return
    }
    // 检查重复
    const dup = holidayData.find(h => h.date === form.date && h.name === form.name.trim())
    if (dup) {
      wx.showToast({ title: '该节假日已存在', icon: 'none' })
      return
    }
    const newId = Math.max(...holidayData.map(h => h.id), 0) + 1
    holidayData.push({ id: newId, name: form.name.trim(), date: form.date, type: form.type })
    // 按日期排序
    holidayData.sort((a, b) => new Date(a.date) - new Date(b.date))
    this.setData({ list: holidayData, showForm: false })
    wx.showToast({ title: '已添加', icon: 'success' })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该节假日吗？',
      success: (res) => {
        if (res.confirm) {
          holidayData = holidayData.filter(i => i.id !== id)
          this.setData({ list: holidayData })
          wx.showToast({ title: '已删除', icon: 'success' })
        }
      }
    })
  }
})
