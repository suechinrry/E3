const app = getApp()

function request({ url, method = 'GET', data = {}, mockData = null }) {
  const useMock = app.globalData.useMock
  if (useMock && mockData !== null) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ code: 200, msg: 'success', data: mockData })
      }, 300)
    })
  }

  const token = app.globalData.token || wx.getStorageSync('token')
  const header = { 'Content-Type': 'application/json;charset=utf-8' }
  if (token) header['Authorization'] = 'Bearer ' + token
  const body = (method === 'POST' || method === 'PUT') ? JSON.stringify(data) : data

  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data: body,
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
        wx.showToast({ title: '网络异常，请检查后端是否启动', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = { request }
