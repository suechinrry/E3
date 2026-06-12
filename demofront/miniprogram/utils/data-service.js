const { request } = require('./request')
const API = require('./api')
const { login: mockLogin } = require('../mock/user')
const { mockAppointments, hostAppointments, adminPendingList, hostRecords } = require('../mock/appointment')
const { mockEmployees, filterEmployees } = require('../mock/employee')
const { mockDepartments } = require('../mock/department')
const { mockNotices, mockAdmins, mockRoles, allPermissions, mockHolidays, mockSystemSettings } = require('../mock/notification')
const { mockOverview, mockTrend, mockCompanyRank, mockDeptRank, mockHostStats } = require('../mock/stats')
const { mockGreeting, mockVerifyResult } = require('../mock/greeting')

const app = getApp()

function isMock() {
  return app.globalData.useMock === true
}

// ==================== 认证 ====================

function login(username, password) {
  if (isMock()) {
    const user = mockLogin(username, password)
    if (user) return Promise.resolve({ code: 200, data: { token: user.token, user } })
    return Promise.resolve({ code: 500, msg: '用户名或密码错误' })
  }
  return request({ url: API.LOGIN_BYPASS, method: 'POST', data: { username, password } })
}

// ==================== 员工/用户 ====================

function getEmployees(page = 1, size = 10, keyword = '', departmentId = '') {
  if (isMock()) {
    const list = keyword ? filterEmployees(keyword) : mockEmployees
    return Promise.resolve({ code: 200, data: { list, total: list.length, page, size } })
  }
  return request({ url: API.EMPLOYEE_LIST, data: { page, size, keyword, departmentId } })
}

function createEmployee(data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '新增成功' })
  return request({ url: API.EMPLOYEE_CREATE, method: 'POST', data })
}

function updateEmployee(id, data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '修改成功' })
  return request({ url: API.EMPLOYEE_UPDATE(id), method: 'PUT', data })
}

function deleteEmployee(id) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '删除成功' })
  return request({ url: API.EMPLOYEE_DELETE(id), method: 'DELETE' })
}

function batchEmployees(ids, action) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '操作成功' })
  return request({ url: API.EMPLOYEE_BATCH, method: 'POST', data: { ids, action } })
}

// ==================== 部门 ====================

function getDepartments(keyword = '') {
  if (isMock()) {
    const list = keyword ? mockDepartments.filter(d => d.name.includes(keyword)) : mockDepartments
    return Promise.resolve({ code: 200, data: { list } })
  }
  return request({ url: API.DEPT_LIST, data: { keyword } })
}

function createDepartment(data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '新增成功' })
  return request({ url: API.DEPT_CREATE, method: 'POST', data })
}

function updateDepartment(id, data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '修改成功' })
  return request({ url: API.DEPT_UPDATE(id), method: 'PUT', data })
}

function deleteDepartment(id) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '删除成功' })
  return request({ url: API.DEPT_DELETE(id), method: 'DELETE' })
}

// ==================== 预约 ====================

function getMyAppointments(page = 1, size = 10, status = 'all') {
  if (isMock()) {
    const list = status === 'all' ? mockAppointments : mockAppointments.filter(a => a.status === status)
    return Promise.resolve({ code: 200, data: { list, total: list.length } })
  }
  return request({ url: API.APPOINTMENT_MY, data: { page, size, status } })
}

function createAppointment(data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '预约提交成功', data: { appointmentId: Date.now() } })
  return request({ url: API.APPOINTMENT_CREATE, method: 'POST', data })
}

function cancelAppointment(id) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '预约已撤销' })
  return request({ url: API.APPOINTMENT_CANCEL(id), method: 'PUT' })
}

function rebookAppointment(id) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '预约提交成功', data: { appointmentId: Date.now() } })
  return request({ url: API.APPOINTMENT_REBOOK(id), method: 'POST' })
}

// ==================== 被访人 ====================

function getHostRecords(page = 1, size = 10, status = 'all', startDate = '', endDate = '') {
  if (isMock()) {
    let list = hostRecords
    if (status !== 'all') list = list.filter(r => r.status === status)
    return Promise.resolve({ code: 200, data: { list, total: list.length } })
  }
  return request({ url: API.APPOINTMENT_HOST, data: { page, size, status, startDate, endDate } })
}

function getHostStats(startDate = '', endDate = '') {
  if (isMock()) return Promise.resolve({ code: 200, data: mockHostStats })
  return request({ url: API.APPOINTMENT_HOST_STATS, data: { startDate, endDate } })
}

function getPendingApprovals() {
  if (isMock()) return Promise.resolve({ code: 200, data: hostAppointments })
  return request({ url: API.APPOINTMENT_HOST_PENDING })
}

function approveAppointment(id, status, remark = '') {
  if (isMock()) {
    const appt = hostAppointments.find(a => a.id === id)
    return Promise.resolve({
      code: 200,
      msg: '审批完成',
      data: { greeting: appt ? `欢迎${appt.company}的访客莅临` : '', seatSuggestion: '会议室A', notes: '准备访客证' }
    })
  }
  return request({ url: API.APPOINTMENT_APPROVE(id), method: 'PUT', data: { status, remark } })
}

function helperAppointment(data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '预约成功' })
  return request({ url: API.APPOINTMENT_HELPER, method: 'POST', data })
}

// ==================== 通知 ====================

function getNotifications(page = 1, size = 10) {
  if (isMock()) return Promise.resolve({ code: 200, data: { list: mockNotices, total: mockNotices.length } })
  return request({ url: API.NOTIFICATION_PUBLIC, data: { page, size } })
}

function getAdminNotifications(page = 1, size = 10) {
  if (isMock()) return Promise.resolve({ code: 200, data: { list: mockNotices, total: mockNotices.length } })
  return request({ url: API.NOTIFICATION_LIST, data: { page, size } })
}

function createNotification(data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '发布成功' })
  return request({ url: API.NOTIFICATION_CREATE, method: 'POST', data })
}

function updateNotification(id, data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '修改成功' })
  return request({ url: API.NOTIFICATION_UPDATE(id), method: 'PUT', data })
}

function deleteNotification(id) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '删除成功' })
  return request({ url: API.NOTIFICATION_DELETE(id), method: 'DELETE' })
}

// ==================== 门岗 ====================

function verifyAppointment(qrCode) {
  if (isMock()) return Promise.resolve({ code: 200, data: { ...mockVerifyResult } })
  return request({ url: API.GUARD_VERIFY, method: 'POST', data: { qrCode } })
}

function confirmAppointment(appointmentId) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '已确认放行' })
  return request({ url: API.GUARD_CONFIRM(appointmentId), method: 'PUT' })
}

// ==================== AI ====================

function getGreeting(appointmentId) {
  if (isMock()) return Promise.resolve({ code: 200, data: { ...mockGreeting } })
  return request({ url: API.APPOINTMENT_GREETING(appointmentId) })
}

function generateGreeting(appointmentId) {
  if (isMock()) return Promise.resolve({ code: 200, data: { ...mockGreeting } })
  return request({ url: API.AI_GREETING(appointmentId), method: 'POST' })
}

// ==================== 管理员/统计/系统 ====================

function getAdminPending(page = 1, size = 10) {
  if (isMock()) return Promise.resolve({ code: 200, data: { list: adminPendingList, total: adminPendingList.length } })
  return request({ url: API.ADMIN_PENDING, data: { page, size } })
}

function getOverview() {
  if (isMock()) return Promise.resolve({ code: 200, data: mockOverview })
  return request({ url: API.STATS_OVERVIEW })
}

function getTrend(days = 7) {
  if (isMock()) return Promise.resolve({ code: 200, data: mockTrend })
  return request({ url: API.STATS_TREND, data: { days } })
}

function getVisitorRecordStats(startDate = '', endDate = '') {
  if (isMock()) return Promise.resolve({ code: 200, data: { companyStats: mockCompanyRank, deptStats: mockDeptRank } })
  return request({ url: API.STATS_VISITOR_RECORD, data: { startDate, endDate } })
}

function getSettings() {
  if (isMock()) return Promise.resolve({ code: 200, data: mockSystemSettings })
  return request({ url: API.SETTINGS_GET })
}

function updateSettings(data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '保存成功' })
  return request({ url: API.SETTINGS_UPDATE, method: 'PUT', data })
}

// ==================== 角色/管理员管理/节假日 ====================

function getRoles() {
  if (isMock()) return Promise.resolve({ code: 200, data: mockRoles })
  return request({ url: '/admin/role' })
}

function updateRole(roleKey, permissions) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '更新成功' })
  return request({ url: API.ROLE_UPDATE(roleKey), method: 'PUT', data: { permissions } })
}

function getAdmins(page = 1, size = 10) {
  if (isMock()) return Promise.resolve({ code: 200, data: { list: mockAdmins, total: mockAdmins.length } })
  return request({ url: API.ADMIN_LIST, data: { page, size } })
}

function createAdmin(data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '新增成功' })
  return request({ url: API.ADMIN_CREATE, method: 'POST', data })
}

function deleteAdmin(id) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '删除成功' })
  return request({ url: API.ADMIN_DELETE(id), method: 'DELETE' })
}

function getHolidays(year = '') {
  if (isMock()) return Promise.resolve({ code: 200, data: mockHolidays })
  return request({ url: API.HOLIDAY_LIST, data: { year } })
}

function createHoliday(data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '新增成功' })
  return request({ url: API.HOLIDAY_CREATE, method: 'POST', data })
}

function deleteHoliday(id) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '删除成功' })
  return request({ url: API.HOLIDAY_DELETE(id), method: 'DELETE' })
}

// ==================== 个人资料 ====================

function getProfile() {
  if (isMock()) {
    const user = app.globalData.userInfo || {}
    return Promise.resolve({ code: 200, data: user })
  }
  return request({ url: API.USER_PROFILE_GET })
}

function updateProfile(data) {
  if (isMock()) return Promise.resolve({ code: 200, msg: '保存成功' })
  return request({ url: API.USER_PROFILE_UPDATE, method: 'PUT', data })
}

module.exports = {
  login,
  getEmployees, createEmployee, updateEmployee, deleteEmployee, batchEmployees,
  getDepartments, createDepartment, updateDepartment, deleteDepartment,
  getMyAppointments, createAppointment, cancelAppointment, rebookAppointment,
  getHostRecords, getHostStats, getPendingApprovals, approveAppointment, helperAppointment,
  getNotifications, getAdminNotifications, createNotification, updateNotification, deleteNotification,
  verifyAppointment, confirmAppointment,
  getGreeting, generateGreeting,
  getAdminPending, getOverview, getTrend, getVisitorRecordStats,
  getSettings, updateSettings,
  getRoles, updateRole,
  getAdmins, createAdmin, deleteAdmin,
  getHolidays, createHoliday, deleteHoliday,
  getProfile, updateProfile
}
