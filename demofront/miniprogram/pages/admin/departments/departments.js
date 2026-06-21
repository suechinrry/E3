const { getDepartments, createDepartment, updateDepartment, deleteDepartment } = require('../../../utils/data-service')

Page({
  data: {
    treeList: [],
    flatList: [],
    showForm: false,
    formTitle: '',
    editingId: null,
    form: { name: '', parentName: '', parentId: 0, parentIndex: 0, managerId: '' },
    parentOptions: []
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    getDepartments().then(res => {
      if (res.code === 200) {
        const deptList = Array.isArray(res.data) ? res.data : (res.data.list || [])
        this.setData({ flatList: deptList })
        this.buildView(deptList)
      }
    }).catch(() => {})
  },

  buildView(deptList) {
    const parentOptions = [{ id: 0, name: '无（顶级部门）' }].concat(
      deptList.map(d => ({ id: d.id, name: d.name }))
    )
    const topList = deptList
      .filter(d => !d.parentId || d.parentId === 0)
      .map(d => this.buildNode(d, deptList))
    this.setData({ treeList: topList, parentOptions })
  },

  buildNode(dept, allDepts) {
    const children = allDepts.filter(d => d.parentId === dept.id)
    return {
      ...dept,
      expanded: !!this._expandedCache[dept.id],
      children: children.map(c => this.buildNode(c, allDepts))
    }
  },
  _expandedCache: {},

  onToggle(e) {
    const id = e.currentTarget.dataset.id
    this._expandedCache[id] = !this._expandedCache[id]
    this.buildView(this.data.flatList)
  },

  onAdd() {
    this.setData({
      showForm: true,
      formTitle: '新增部门',
      editingId: null,
      form: { name: '', parentName: '', parentId: 0, parentIndex: 0, managerId: '' }
    })
  },

  onEdit(e) {
    const item = e.currentTarget.dataset.item
    const parentId = item.parentId || 0
    const parentIndex = this.data.parentOptions.findIndex(p => p.id == parentId)
    const parentName = parentIndex > 0 ? this.data.parentOptions[parentIndex].name : ''

    this.setData({
      showForm: true,
      formTitle: '编辑部门',
      editingId: item.id,
      form: {
        name: item.name || '',
        parentName: parentName,
        parentId: parentId,
        parentIndex: parentIndex >= 0 ? parentIndex : 0,
        managerId: item.managerId ? String(item.managerId) : ''
      }
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
    const idx = parseInt(e.detail.value)
    const opt = this.data.parentOptions[idx]
    if (opt) {
      this.setData({
        'form.parentId': opt.id,
        'form.parentName': opt.name === '无（顶级部门）' ? '' : opt.name,
        'form.parentIndex': idx
      })
    }
  },

  onSave() {
    const { form, editingId } = this.data
    if (!form.name || !form.name.trim()) {
      wx.showToast({ title: '请输入部门名称', icon: 'none' })
      return
    }
    const payload = {
      name: form.name.trim(),
      parentId: form.parentId || 0,
      managerId: form.managerId ? Number(form.managerId) : null
    }
    const apiCall = editingId
      ? updateDepartment(editingId, payload)
      : createDepartment(payload)

    apiCall.then(res => {
      if (res.code === 200) {
        this.setData({ showForm: false })
        this.loadData()
        wx.showToast({ title: editingId ? '已更新' : '已添加', icon: 'success' })
      } else {
        wx.showToast({ title: res.msg || '操作失败', icon: 'error' })
      }
    }).catch(() => {
      wx.showToast({ title: '操作失败', icon: 'error' })
    })
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id
    const hasChildren = this.data.flatList.some(d => d.parentId === id)
    const dept = this.data.flatList.find(d => d.id === id)
    const hasEmployees = dept && dept.employeeCount > 0

    let msg = '确定删除该部门吗？'
    if (hasChildren) msg = '该部门下存在子部门，确定删除吗？'
    if (hasEmployees) msg += '\n（当前有 ' + dept.employeeCount + ' 名员工）'

    wx.showModal({
      title: '提示', content: msg,
      success: (res) => {
        if (res.confirm) {
          deleteDepartment(id).then(res => {
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
