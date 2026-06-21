const { getHolidays, createHoliday, deleteHoliday } = require('../../../utils/data-service')

Page({
  data: {
    list: [],
    showForm: false,
    form: { name: '', date: '', type: '法定' },
    typeOptions: ['法定', '公司', '调休']
  },
  onShow() {
    this.loadData()
  },
  loadData() {
    getHolidays().then(res => {
      if (res.code === 200) {
        this.setData({ list: res.data || [] })
      }
    }).catch(() => {})
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
    const idx = parseInt(e.detail.value)
    this.setData({ 'form.type': this.data.typeOptions[idx] })
  },
  onSave() {
    const { form } = this.data
    if (!form.name || !form.name.trim()) {
      wx.showToast({ title: '请输入节日名称', icon: 'none' })
      return
    }
    if (!form.date) {
      wx.showToast({ title: '请选择日期', icon: 'none' })
      return
    }
    createHoliday({ name: form.name.trim(), date: form.date, type: form.type }).then(res => {
      if (res.code === 200) {
        this.setData({ showForm: false })
        this.loadData()
        wx.showToast({ title: '已添加', icon: 'success' })
      } else {
        wx.showToast({ title: res.msg || '添加失败', icon: 'error' })
      }
    }).catch(() => {
      wx.showToast({ title: '操作失败', icon: 'error' })
    })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该节假日吗？',
      success: (res) => {
        if (res.confirm) {
          deleteHoliday(id).then(res => {
            if (res.code === 200) {
              this.loadData()
              wx.showToast({ title: '已删除', icon: 'success' })
            } else {
              wx.showToast({ title: res.msg || '删除失败', icon: 'error' })
            }
          }).catch(() => {
            wx.showToast({ title: '操作失败', icon: 'error' })
          })
        }
      }
    })
  }
})
