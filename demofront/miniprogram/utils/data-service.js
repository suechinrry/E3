const { request } = require('./request')
const API = require('./api')

// ==================== 认证 ====================

function login(username, password) {
  return request({ url: API.LOGIN_BYPASS, method: 'POST', data: { username, password } })
}

function register(data) {
  return request({ url: API.REGISTER, method: 'POST', data })
}

// ==================== 员工/用户 ====================

function getEmployees(page = 1, size = 10, keyword = '', departmentId = '') {
  return request({ url: API.EMPLOYEE_LIST, data: { page, size, keyword, departmentId } })
}

function createEmployee(data) {
  return request({ url: API.EMPLOYEE_CREATE, method: 'POST', data })
}

function updateEmployee(id, data) {
  return request({ url: API.EMPLOYEE_UPDATE(id), method: 'PUT', data })
}

function deleteEmployee(id) {
  return request({ url: API.EMPLOYEE_DELETE(id), method: 'DELETE' })
}

function batchEmployees(ids, action) {
  return request({ url: API.EMPLOYEE_BATCH, method: 'POST', data: { ids, action } })
}

// ==================== 部门 ====================

function getDepartments(keyword = '') {
  return request({ url: API.DEPT_LIST, data: { keyword } })
}

function createDepartment(data) {
  return request({ url: API.DEPT_CREATE, method: 'POST', data })
}

function updateDepartment(id, data) {
  return request({ url: API.DEPT_UPDATE(id), method: 'PUT', data })
}

function deleteDepartment(id) {
  return request({ url: API.DEPT_DELETE(id), method: 'DELETE' })
}

// ==================== 预约 ====================

function lookupHost(name, phone) {
  return request({ url: API.HOST_LOOKUP, method: 'POST', data: { name, phone } })
}

function getMyAppointments(page = 1, size = 10, status = 'all') {
  return request({ url: API.APPOINTMENT_MY, data: { page, size, status } })
}

function createAppointment(data) {
  return request({ url: API.APPOINTMENT_CREATE, method: 'POST', data })
}

function cancelAppointment(id) {
  return request({ url: API.APPOINTMENT_CANCEL(id), method: 'PUT' })
}

function restoreAppointment(id) {
  return request({ url: API.APPOINTMENT_RESTORE(id), method: 'PUT' })
}

function rebookAppointment(id) {
  return request({ url: API.APPOINTMENT_REBOOK(id), method: 'POST' })
}

// ==================== 被访人 ====================

function getHostRecords(page = 1, size = 10, status = 'all', startDate = '', endDate = '') {
  return request({ url: API.APPOINTMENT_HOST, data: { page, size, status, startDate, endDate } })
}

function getHostStats(startDate = '', endDate = '') {
  return request({ url: API.APPOINTMENT_HOST_STATS, data: { startDate, endDate } })
}

function getPendingApprovals() {
  return request({ url: API.APPOINTMENT_HOST_PENDING })
}

function approveAppointment(id, status, remark = '') {
  return request({ url: API.APPOINTMENT_APPROVE(id), method: 'PUT', data: { status, remark } })
}

function helperAppointment(data) {
  return request({ url: API.APPOINTMENT_HELPER, method: 'POST', data })
}

// ==================== 通知 ====================

function getNotifications(page = 1, size = 10) {
  return request({ url: API.NOTIFICATION_PUBLIC, data: { page, size } })
}

function getAdminNotifications(page = 1, size = 10) {
  return request({ url: API.NOTIFICATION_LIST, data: { page, size } })
}

function createNotification(data) {
  return request({ url: API.NOTIFICATION_CREATE, method: 'POST', data })
}

function updateNotification(id, data) {
  return request({ url: API.NOTIFICATION_UPDATE(id), method: 'PUT', data })
}

function deleteNotification(id) {
  return request({ url: API.NOTIFICATION_DELETE(id), method: 'DELETE' })
}

function getNotificationRecipients(id) {
  return request({ url: API.NOTIFICATION_RECIPIENTS(id) })
}

// ==================== 用户弹窗通知 ====================

function getUserNotifications() {
  return request({ url: API.USER_NOTIFICATION_LIST })
}

function getPopupNotifications() {
  return request({ url: API.USER_NOTIFICATION_POPUP })
}

function markNotificationRead(id) {
  return request({ url: API.USER_NOTIFICATION_READ(id), method: 'PUT' })
}

function markAllNotificationsRead() {
  return request({ url: API.USER_NOTIFICATION_READ_ALL, method: 'PUT' })
}

// ==================== 门岗 ====================

function verifyAppointmentByName(visitorName, visitorPhone) {
  return request({ url: API.GUARD_VERIFY_BY_NAME, method: 'POST', data: { visitorName, visitorPhone } })
}

function verifyAppointment(qrCode) {
  return request({ url: API.GUARD_VERIFY, method: 'POST', data: { qrCode } })
}

function confirmAppointment(appointmentId) {
  return request({ url: API.GUARD_CONFIRM(appointmentId), method: 'PUT' })
}

// ==================== AI ====================

function getGreeting(appointmentId) {
  return request({ url: API.APPOINTMENT_GREETING(appointmentId) })
}

function regenerateGreeting(appointmentId) {
  return request({ url: API.AI_REGENERATE(appointmentId), method: 'POST' })
}

function generateGreeting(appointmentId) {
  return request({ url: API.AI_GREETING(appointmentId), method: 'POST' })
}

// ==================== 管理员/统计/系统 ====================

function getAdminPending(page = 1, size = 10) {
  return request({ url: API.ADMIN_PENDING, data: { page, size } })
}

function getAllAppointments(page = 1, size = 100, status = 'all') {
  return request({ url: API.ADMIN_APPOINTMENT, data: { page, size, status } })
}

function getOverview() {
  return request({ url: API.STATS_OVERVIEW })
}

function getTrend(days = 7) {
  return request({ url: API.STATS_TREND, data: { days } })
}

function getVisitorRecordStats(startDate = '', endDate = '') {
  return request({ url: API.STATS_VISITOR_RECORD, data: { startDate, endDate } })
}

function getSettings() {
  return request({ url: API.SETTINGS_GET })
}

function updateSettings(data) {
  return request({ url: API.SETTINGS_UPDATE, method: 'PUT', data })
}

// ==================== 角色/管理员管理/节假日 ====================

function getRoles() {
  return request({ url: '/admin/role' })
}

function updateRole(roleKey, permissions) {
  return request({ url: API.ROLE_UPDATE(roleKey), method: 'PUT', data: { permissions } })
}

function getAdmins(page = 1, size = 10) {
  return request({ url: API.ADMIN_LIST, data: { page, size } })
}

function createAdmin(data) {
  return request({ url: API.ADMIN_CREATE, method: 'POST', data })
}

function updateAdmin(id, data) {
  return request({ url: API.ADMIN_UPDATE(id), method: 'PUT', data })
}

function deleteAdmin(id) {
  return request({ url: API.ADMIN_DELETE(id), method: 'DELETE' })
}

function getHolidays(year = '') {
  return request({ url: API.HOLIDAY_LIST, data: { year } })
}

function createHoliday(data) {
  return request({ url: API.HOLIDAY_CREATE, method: 'POST', data })
}

function deleteHoliday(id) {
  return request({ url: API.HOLIDAY_DELETE(id), method: 'DELETE' })
}

// ==================== 个人资料 ====================

function getProfile() {
  return request({ url: API.USER_PROFILE_GET })
}

function updateProfile(data) {
  return request({ url: API.USER_PROFILE_UPDATE, method: 'PUT', data })
}

module.exports = {
  login,
  register,
  getEmployees, createEmployee, updateEmployee, deleteEmployee, batchEmployees,
  getDepartments, createDepartment, updateDepartment, deleteDepartment,
  lookupHost, getMyAppointments, createAppointment, cancelAppointment, restoreAppointment, rebookAppointment,
  getHostRecords, getHostStats, getPendingApprovals, approveAppointment, helperAppointment,
  getNotifications, getAdminNotifications, createNotification, updateNotification, deleteNotification, getNotificationRecipients,
  getUserNotifications, getPopupNotifications, markNotificationRead, markAllNotificationsRead,
  verifyAppointment, verifyAppointmentByName, confirmAppointment,
  getGreeting, generateGreeting, regenerateGreeting,
  getAdminPending, getAllAppointments, getOverview, getTrend, getVisitorRecordStats,
  getSettings, updateSettings,
  getRoles, updateRole,
  getAdmins, createAdmin, updateAdmin, deleteAdmin,
  getHolidays, createHoliday, deleteHoliday,
  getProfile, updateProfile
}
