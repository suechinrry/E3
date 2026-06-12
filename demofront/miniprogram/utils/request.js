function request({ url, method = 'GET', data = {}, isMock = false, mockData = null }) {
  if (isMock) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ code: 200, msg: 'success', data: mockData })
      }, 300)
    })
  }

  const app = getApp()
  const token = app.globalData.token || wx.getStorageSync('token')
  const header = { 'Content-Type': 'application/json' }
  if (token) header['Authorization'] = 'Bearer ' + token

  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header,
      success(res) {
        if (res.data.code === 401) {
          wx.removeStorageSync('token')
          wx.reLaunch({ url: '/pages/login/login' })
          return
        }
        resolve(res.data)
      },
      fail(err) {
        wx.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = { request }
