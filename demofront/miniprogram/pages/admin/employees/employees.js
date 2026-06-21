const { getEmployees, createEmployee, updateEmployee, deleteEmployee, getDepartments } = require('../../../utils/data-service')

// 部门ID→名称映射缓存
let deptMap = {}

Page({
  data: {
    list: [],
    keyword: '',
    showForm: false,
    formTitle: '',
    editingId: null,
    form: { username: '', password: '', name: '', phone: '', department: '', departmentId: '', deptIndex: -1, status: 1 },
    departmentOptions: [],
    allDeptData: []    // 全部部门原始数据，供 onEdit 查找用
  },
  onShow() {
    this.loadDepartments().then(() => this.loadData())
  },

  // ===== 部门加载 =====
  loadDepartments() {
    return getDepartments().then(res => {
      if (res.code === 200) {
        const deptList = Array.isArray(res.data) ? res.data : (res.data.list || [])
        deptMap = {}
        deptList.forEach(d => { deptMap[d.id] = d.name })
        this.setData({
          departmentOptions: deptList.map(d => ({ id: d.id, name: d.name })),
          allDeptData: deptList
        })
      }
    }).catch(() => {})
  },

  // ===== 员工列表加载 =====
  loadData() {
    getEmployees(1, 100, this.data.keyword, '').then(res => {
      if (res.code === 200) {
        const list = (res.data.list || []).map(e => ({
          ...e,
          department: e.departmentName || deptMap[e.departmentId] || ''
        }))
        this.setData({ list })
      }
    }).catch(() => {})
  },

  onSearch(e) {
    const keyword = e.detail.value
    this.setData({ keyword })
    getEmployees(1, 100, keyword, '').then(res => {
      if (res.code === 200) {
        const list = (res.data.list || []).map(e => ({
          ...e,
          department: deptMap[e.departmentId] || ''
        }))
        this.setData({ list })
      }
    }).catch(() => {})
  },

  // ===== 表单操作 =====
  onAdd() {
    this.setData({
      showForm: true,
      formTitle: '新增员工',
      editingId: null,
      form: { username: '', password: '', name: '', phone: '', department: '', departmentId: '', deptIndex: -1, status: 1 }
    })
  },

  onEdit(e) {
    const item = e.currentTarget.dataset.item
    // 从部门映射中查找当前员工的部门名和 picker 索引
    const deptId = item.departmentId
    const deptName = deptMap[deptId] || ''
    const deptIndex = deptId ? this.data.departmentOptions.findIndex(d => d.id == deptId) : -1

    this.setData({
      showForm: true,
      formTitle: '编辑员工',
      editingId: item.id,
      form: {
        username: item.username || '',
        password: '',
        name: item.name || '',
        phone: item.phone || '',
        department: deptName,
        departmentId: deptId || '',
        deptIndex: deptIndex >= 0 ? deptIndex : -1,
        status: item.status != null ? item.status : 1
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

  onDepartmentChange(e) {
    const idx = parseInt(e.detail.value)
    const dept = this.data.departmentOptions[idx]
    if (dept) {
      this.setData({
        'form.department': dept.name,
        'form.departmentId': dept.id,
        'form.deptIndex': idx
      })
    }
  },

  // ===== 保存 =====
  onSave() {
    const { form, editingId } = this.data

    // 校验姓名
    if (!form.name || !form.name.trim()) {
      wx.showToast({ title: '请输入姓名', icon: 'none' })
      return
    }
    // 校验手机号
    if (!form.phone || !/^1\d{10}$/.test(form.phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    // 新增时校验用户名和密码
    if (!editingId) {
      if (!form.username || !form.username.trim()) {
        wx.showToast({ title: '请输入用户名', icon: 'none' })
        return
      }
      if (!form.password || !form.password.trim()) {
        wx.showToast({ title: '请输入密码', icon: 'none' })
        return
      }
    }

    const payload = {
      name: form.name.trim(),
      phone: form.phone,
      departmentId: form.departmentId ? Number(form.departmentId) : null,
      status: form.status != null ? form.status : 1
    }

    if (editingId) {
      // 编辑模式
      payload.username = form.username.trim() || undefined
      if (form.password && form.password.trim()) {
        payload.password = form.password.trim()
      }
    } else {
      // 新增模式：必须传 username 和 password
      payload.username = form.username.trim()
      payload.password = form.password.trim()
    }

    const apiCall = editingId
      ? updateEmployee(editingId, payload)
      : createEmployee(payload)

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

  // ===== 删除 =====
  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定删除该员工吗？',
      success: (res) => {
        if (res.confirm) {
          deleteEmployee(id).then(res => {
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
