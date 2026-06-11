const { mockRoles, allPermissions } = require('../../../mock/notification')

let rolePermissions = [...mockRoles]

Page({
  data: {
    roles: rolePermissions,
    allPermissions,
    expandedId: null
  },
  toggleExpand(e) {
    const id = e.currentTarget.dataset.id
    this.setData({
      expandedId: this.data.expandedId === id ? null : id
    })
  },
  onPermChange(e) {
    const roleId = e.currentTarget.dataset.role
    const permKey = e.currentTarget.dataset.perm
    const checked = e.detail.value.length > 0

    const roleIdx = rolePermissions.findIndex(r => r.id === roleId)
    if (roleIdx < 0) return

    const perms = [...rolePermissions[roleIdx].permissions]

    if (permKey === 'all') {
      // 全选/取消全选
      if (checked) {
        rolePermissions[roleIdx].permissions = ['all']
      } else {
        rolePermissions[roleIdx].permissions = []
      }
    } else {
      // 去掉 all，单独操作
      const allIdx = perms.indexOf('all')
      if (allIdx >= 0) perms.splice(allIdx, 1)

      const idx = perms.indexOf(permKey)
      if (checked && idx < 0) {
        perms.push(permKey)
      } else if (!checked && idx >= 0) {
        perms.splice(idx, 1)
      }

      // 如果全选了所有非all权限，自动勾选all
      const nonAllPerms = allPermissions.filter(p => p.key !== 'all').map(p => p.key)
      if (nonAllPerms.every(p => perms.includes(p))) {
        rolePermissions[roleIdx].permissions = ['all']
      } else {
        rolePermissions[roleIdx].permissions = perms
      }
    }

    this.setData({ roles: rolePermissions })
    wx.showToast({ title: '权限已更新', icon: 'success' })
  },
  onSaveAll() {
    wx.showToast({ title: '所有角色权限已保存', icon: 'success' })
  }
})
