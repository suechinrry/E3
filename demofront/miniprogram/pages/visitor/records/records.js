const { getMyAppointments, cancelAppointment, restoreAppointment } = require('../../../utils/data-service')

Page({
  data: {
    currentTab: 'all',
    list: [],
    statusMap: {
      pending: '待审核', approved: '已通过', rejected: '已拒绝',
      cancelled: '已取消', confirmed: '已核验'
    }
  },
  onShow() {
    this.loadData()
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

    const that = this
    // 先检查并申请相册写入权限
    wx.getSetting({
      success(res) {
        if (!res.authSetting['scope.writePhotosAlbum']) {
          wx.authorize({
            scope: 'scope.writePhotosAlbum',
            success() {
              that.doSaveQr(item, id)
            },
            fail() {
              wx.showModal({
                title: '需要相册权限',
                content: '请在设置中开启相册权限后重试',
                confirmText: '去设置',
                success(m) {
                  if (m.confirm) wx.openSetting()
                }
              })
            }
          })
        } else {
          that.doSaveQr(item, id)
        }
      }
    })
  },

  doSaveQr(item, id) {
    wx.showLoading({ title: '保存中...' })
    // 优先用离屏Canvas渲染（兼容性更好），失败降级 writeFile
    this.saveViaCanvas(item, () => this.saveViaFile(item, id))
  },

  // 方式1：离屏Canvas→temp文件→保存相册（最稳定）
  saveViaCanvas(item, fallback) {
    try {
      const canvas = wx.createOffscreenCanvas({ type: '2d', width: 300, height: 300 })
      const img = canvas.createImage()
      img.onload = () => {
        const ctx = canvas.getContext('2d')
        ctx.clearRect(0, 0, 300, 300)
        ctx.drawImage(img, 0, 0, 300, 300)
        wx.canvasToTempFilePath({
          canvas: canvas,
          fileType: 'png',
          success: (res) => {
            wx.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => {
                wx.hideLoading()
                wx.showToast({ title: '已保存到相册', icon: 'success' })
              },
              fail: (e) => {
                console.warn('canvas→保存相册失败，降级writeFile', e)
                fallback()
              }
            })
          },
          fail: () => { console.warn('canvasToTempFilePath失败，降级writeFile'); fallback() }
        })
      }
      img.onerror = () => { console.warn('canvas图片加载失败，降级writeFile'); fallback() }
      img.src = item.qrBase64
    } catch (e) {
      console.warn('离屏Canvas不可用，降级writeFile', e)
      fallback()
    }
  },

  // 方式2：writeFile→保存相册（兜底）
  saveViaFile(item, id) {
    const base64Data = item.qrBase64.replace(/^data:image\/\w+;base64,/, '')
    // base64→ArrayBuffer，比字符串写入更稳定
    let buffer
    try {
      buffer = wx.base64ToArrayBuffer(base64Data)
    } catch (e) {
      buffer = this._base64ToBuffer(base64Data)
    }
    const fs = wx.getFileSystemManager()
    const filePath = wx.env.USER_DATA_PATH + '/qrcode_' + id + '.png'
    fs.writeFile({
      filePath: filePath,
      data: buffer,
      success: () => {
        wx.saveImageToPhotosAlbum({
          filePath: filePath,
          success: () => { wx.hideLoading(); wx.showToast({ title: '已保存到相册', icon: 'success' }) },
          fail: (e) => { wx.hideLoading(); this._showSaveFailed(e) }
        })
      },
      fail: (e) => { wx.hideLoading(); console.error('writeFile失败', e); this._showSaveFailed() }
    })
  },

  _showSaveFailed(e) {
    if (e && e.errMsg && e.errMsg.indexOf('auth') > -1) {
      wx.showModal({
        title: '需要相册权限',
        content: '请在设置中开启相册权限，或直接长按二维码图片保存',
        confirmText: '去设置',
        success(m) { if (m.confirm) wx.openSetting() }
      })
    } else {
      wx.showModal({
        title: '保存失败',
        content: '自动保存异常，请长按二维码图片→选择「保存图片」',
        showCancel: false
      })
    }
  },

  _base64ToBuffer(base64) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
    let output = new Uint8Array(base64.length)
    let p = 0
    for (let i = 0; i < base64.length; i += 4) {
      const a = chars.indexOf(base64[i] || 'A')
      const b = chars.indexOf(base64[i + 1] || 'A')
      const c = chars.indexOf(base64[i + 2] || 'A')
      const d = chars.indexOf(base64[i + 3] || 'A')
      if (a < 0 || b < 0) break
      output[p++] = (a << 2) | (b >> 4)
      if (c >= 0) { output[p++] = ((b & 15) << 4) | (c >> 2) }
      if (d >= 0) { output[p++] = ((c & 3) << 6) | d }
    }
    return output.buffer.slice(0, p)
  },
})
