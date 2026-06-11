const { mockDepartments } = require('../../../mock/department')

let departmentData = [...mockDepartments]

Page({
  data: {
    list: departmentData.filter(d => d.parentId === 0),
    showForm: false,
    formTitle: '',
    editingId: null,
    form: { name: '', manager: '', managerId: 1, parentId: 0 },
    parentOptions: ['无（顶级部门）'].concat(departmentData.filter(d => d.parentId === 0).map(d => d.name))
  },
  onAdd() {
    this.setData({
      showForm: true,
      formTitle: '新增部门',
      editingId: null,
      form: { name: '', manager: '', managerId: 1, parentId: 0 },
      parentOptions: ['无（顶级部门）'].concat(departmentData.filter(d => d.parentId === 0).map(d => d.name))
    })
  },
  onEdit(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showForm: true,
      formTitle: '编辑部门',
      editingId: item.id,
      form: {
        name: item.name,
        manager: item.manager,
        managerId: item.managerId || 1,
        parentId: item.parentId || 0
      },
      parentOptions: ['无（顶级部门）'].concat(departmentData.filter(d => d.parentId === 0 && d.id !== item.id).map(d => d.name))
    })
  },
  hideForm() {
    this.setData({ showForm: false })
  },
  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },
  onParentChange(e) {
    const idx = e.detail.value
    const parentName = this.data.parentOptions[idx]
    this.setData({
      'form.parentId': idx === 0 ? 0 : departmentData.find(d => d.name === parentName && d.parentId === 0)?.id || 0
    })
  },
  onSave() {
    const { form, editingId } = this.data
    if (!form.name.trim()) {
      wx.showToast({ title: '请输入部门名称', icon: 'none' })
      return
    }
    if (editingId) {
      const idx = departmentData.findIndex(d => d.id === editingId)
      if (idx >= 0) {
        departmentData[idx] = { ...departmentData[idx], ...form, count: departmentData[idx].count || 0 }
      }
    } else {
      const newId = Math.max(...departmentData.map(d => d.id), 0) + 1
      departmentData.push({ id: newId, ...form, count: 0 })
    }
    this.setData({
      list: departmentData.filter(d => d.parentId === 0),
      showForm: false
    })
    wx.showToast({ title: editingId ? '已更新' : '已添加', icon: 'success' })
  },
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    const hasChildren = departmentData.some(d => d.parentId === id)
    wx.showModal({
      title: '提示',
      content: hasChildren ? '该部门下存在子部门，确定删除吗？' : '确定删除该部门吗？',
      success: (res) => {
        if (res.confirm) {
          departmentData = departmentData.filter(d => d.id !== id && d.parentId !== id)
          this.setData({ list: departmentData.filter(d => d.parentId === 0) })
          wx.showToast({ title: '已删除', icon: 'success' })
        }
      }
    })
  }
})
