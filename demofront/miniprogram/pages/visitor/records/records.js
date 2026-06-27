const { getMyAppointments, cancelAppointment, restoreAppointment, rebookAppointment } = require('../../../utils/data-service')

Page({
  data: {
    currentTab: 'all',
    list: [],
    statusMap: {
      pending: '待审核', approved: '已通过', rejected: '已拒绝',
      cancelled: '已取消', confirmed: '已核验'
    },
    popupVisible: false,
    popupNotification: {}
  },
  onShow() {
    this.loadData()
    // 页面显示时检查是否有待弹窗通知
    this.checkPendingPopup()
  },
  checkPendingPopup() {
    const app = getApp()
    const pending = app.globalData.pendingNotifications
    if (pending && pending.length > 0) {
      this.showNotificationPopup(pending[0])
    }
  },
  showNotificationPopup(notification) {
    this.setData({ popupVisible: true, popupNotification: notification })
  },
  onPopupConfirm(e) {
    const notification = e.detail.notification
    getApp().onPopupConfirm(notification)
    this.setData({ popupVisible: false })
    // 延迟检查是否还有下一个
    setTimeout(() => this.checkPendingPopup(), 300)
  },
  onPopupClose() {
    this.setData({ popupVisible: false })
  },
  loadData() {
    const tab = this.data.currentTab
    getMyAppointments(1, 100, tab).then(res => {
      if (res.code === 200) {
        this.setData({ list: res.data.list || [] })
      }
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'error' })
    })
  },
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ currentTab: tab }, () => this.loadData())
  },
  onCancel(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定撤销该预约吗？',
      success: (res) => {
        if (res.confirm) {
          cancelAppointment(id).then(res => {
            if (res.code === 200) {
              wx.showToast({ title: '已撤销', icon: 'success' })
              this.loadData()
            } else {
              wx.showToast({ title: res.msg || '操作失败', icon: 'error' })
            }
          }).catch(() => {
            wx.showToast({ title: '操作失败', icon: 'error' })
          })
        }
      }
    })
  },
  onRestore(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定恢复该预约吗？恢复后将变为待审核状态。',
      success: (res) => {
        if (res.confirm) {
          restoreAppointment(id).then(res => {
            if (res.code === 200) {
              wx.showToast({ title: '已恢复', icon: 'success' })
              this.loadData()
            } else {
              wx.showToast({ title: res.msg || '操作失败', icon: 'error' })
            }
          }).catch(() => {
            wx.showToast({ title: '操作失败', icon: 'error' })
          })
        }
      }
    })
  },
  onRebook(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示', content: '确定重新提交该预约吗？',
      success: (res) => {
        if (res.confirm) {
          rebookAppointment(id).then(res => {
            if (res.code === 200) {
              wx.showToast({ title: '已重新提交预约', icon: 'success' })
              this.loadData()
            } else {
              wx.showToast({ title: res.msg || '操作失败', icon: 'error' })
            }
          }).catch(() => {
            wx.showToast({ title: '操作失败', icon: 'error' })
          })
        }
      }
    })
  },
  onShowQr(e) {
    const item = e.currentTarget.dataset.item
    const id = item.id

    // 如果已有 base64 缓存，直接展示
    if (item.qrBase64) {
      const list = this.data.list.map(i =>
        i.id === id ? { ...i, qrVisible: true } : i
      )
      this.setData({ list })
      return
    }

    // 请求后端生成二维码 base64
    wx.showLoading({ title: '生成中...' })
    wx.request({
      url: getApp().globalData.baseUrl + '/appointment/' + id + '/qrcode',
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + (getApp().globalData.token || wx.getStorageSync('token'))
      },
      success: (res) => {
        wx.hideLoading()
        if (res.data && res.data.code === 200 && res.data.data) {
          const base64 = res.data.data.base64
          const list = this.data.list.map(i =>
            i.id === id ? { ...i, qrVisible: true, qrBase64: base64 } : i
          )
          this.setData({ list })
        } else {
          wx.showToast({ title: '生成失败', icon: 'error' })
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '网络异常', icon: 'error' })
      }
    })
  },
  onHideQr(e) {
    const id = e.currentTarget.dataset.id
    const list = this.data.list.map(i =>
      i.id === id ? { ...i, qrVisible: false } : i
    )
    this.setData({ list })
  },

  // 保存二维码到相册
  onSaveQr(e) {
    const id = e.currentTarget.dataset.id
    const item = this.data.list.find(i => i.id === id)
    if (!item || !item.qrBase64) return

    wx.showLoading({ title: '保存中...' })
    // base64 转临时文件再保存
    const base64Data = item.qrBase64.replace(/^data:image\/\w+;base64,/, '')
    const filePath = wx.env.USER_DATA_PATH + '/qrcode_' + id + '.png'
    const fs = wx.getFileSystemManager()
    fs.writeFile({
      filePath: filePath,
      data: base64Data,
      encoding: 'base64',
      success: () => {
        wx.saveImageToPhotosAlbum({
          filePath: filePath,
          success: () => {
            wx.hideLoading()
            wx.showToast({ title: '已保存到相册', icon: 'success' })
          },
          fail: () => {
            wx.hideLoading()
            wx.showToast({ title: '请授权相册权限', icon: 'none' })
          }
        })
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '保存失败', icon: 'error' })
      }
    })
  },
})
