const { getAdminNotifications, createNotification, updateNotification, deleteNotification, getNotificationRecipients, getUserNotifications, markNotificationRead, markAllNotificationsRead } = require('../../../utils/data-service')

Page({
  data: {
    list: [], showForm: false, showDetail: false, detailItem: null, showRecipients: false, recipients: [],
    formTitle: '', editingId: null, form: { title: '', content: '', targetRole: 'all' },
    roleOptions: ['全部人员', '访客', '被访人', '管理员', '门岗'], roleValues: ['all','visitor','host','admin','guard'],
    currentTab: 'manage', inboxFilter: 'unread', myList: [], myListDisplay: [], unreadCount: 0,
    inboxTypeIcon: { approved:'✅', rejected:'❌', greeting:'🤖', announcement:'📢', risk_warning:'⚠️' }
  },
  onShow() { this.loadCurrentTab() },
  switchMyTab(e) { this.setData({ currentTab: e.currentTarget.dataset.tab }, () => this.loadCurrentTab()) },
  loadCurrentTab() { this.data.currentTab === 'manage' ? this.loadData() : this.loadInbox() },

  onBack() { wx.redirectTo({ url: '/pages/admin/employees/employees' }) },

  // === 公告管理 ===
  loadData() { getAdminNotifications(1, 100).then(res => { if (res.code === 200) { const raw = res.data.records || res.data || []; this.setData({ list: Array.isArray(raw) ? raw : [] }) } }).catch(() => {}) },
  onAdd() { this.setData({ showForm: true, formTitle: '发布通知', editingId: null, form: { title: '', content: '', targetRole: 'all' } }) },
  onEdit(e) { const item = e.currentTarget.dataset.item; this.setData({ showForm: true, formTitle: '编辑通知', editingId: item.id, form: { title: item.title, content: item.content, targetRole: item.targetRole || 'all' } }) },
  hideForm() { this.setData({ showForm: false }) },
  onInputChange(e) { this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value }) },
  onRoleChange(e) { this.setData({ 'form.targetRole': this.data.roleValues[parseInt(e.detail.value)] }) },
  onSave() { const { form, editingId } = this.data; if (!form.title || !form.title.trim()) { wx.showToast({ title: '请输入标题', icon: 'none' }); return } if (!form.content || !form.content.trim()) { wx.showToast({ title: '请输入内容', icon: 'none' }); return }; (editingId ? updateNotification(editingId, form) : createNotification(form)).then(res => { if (res.code === 200) { this.setData({ showForm: false }); this.loadData(); wx.showToast({ title: editingId ? '已更新' : '已发布', icon: 'success' }) } else { wx.showToast({ title: res.msg || '操作失败', icon: 'error' }) } }).catch(() => { wx.showToast({ title: '操作失败', icon: 'error' }) }) },
  onDetail(e) { const id = e.currentTarget.dataset.id; var item = null; for (var i = 0; i < this.data.list.length; i++) { if (this.data.list[i].id === id) { item = this.data.list[i]; break } } if (item) this.setData({ showDetail: true, detailItem: item }) },
  hideDetail() { this.setData({ showDetail: false }) },
  onViewRecipients(e) { getNotificationRecipients(e.currentTarget.dataset.id).then(res => { if (res.code === 200) this.setData({ showRecipients: true, recipients: res.data || [] }); else wx.showToast({ title: res.msg || '加载失败', icon: 'error' }) }).catch(() => { wx.showToast({ title: '加载失败', icon: 'error' }) }) },
  hideRecipients() { this.setData({ showRecipients: false, recipients: [] }) },
  onDelete(e) { const id = e.currentTarget.dataset.id; wx.showModal({ title: '提示', content: '确定删除？', success: (res) => { if (res.confirm) { deleteNotification(id).then(r => { if (r.code === 200) { this.loadData(); wx.showToast({ title: '已删除', icon: 'success' }) } }).catch(() => {}) } } }) },

  // === 我的通知 ===
  filterInbox(e) { this.setData({ inboxFilter: e.currentTarget.dataset.filter }); this.applyInboxFilter() },
  loadInbox() { getUserNotifications().then(res => { if (res.code === 200) { const all = Array.isArray(res.data) ? res.data : []; this.setData({ myList: all, unreadCount: all.filter(function(n) { return !n.isRead }).length }); this.applyInboxFilter() } }).catch(() => {}) },
  applyInboxFilter() { const { myList, inboxFilter } = this.data; var d; if (inboxFilter === 'unread') d = myList.filter(function(n) { return !n.isRead }); else if (inboxFilter === 'read') d = myList.filter(function(n) { return n.isRead }); else d = myList; this.setData({ myListDisplay: d }) },
  onTapInboxItem(e) { var id = e.currentTarget.dataset.id; var item = null; for (var i = 0; i < this.data.myListDisplay.length; i++) { if (this.data.myListDisplay[i].id === id) { item = this.data.myListDisplay[i]; break } } if (!item) return; if (!item.isRead) { markNotificationRead(item.id).catch(function(){}); var ml = this.data.myList.map(function(n) { if (n.id === item.id) n.isRead = 1; return n }); this.setData({ myList: ml, unreadCount: ml.filter(function(n) { return !n.isRead }).length }); this.applyInboxFilter() } if (item.type === 'risk_warning') { wx.navigateTo({ url: '/pages/admin/approve/adApprove' }); return } this.setData({ showDetail: true, detailItem: item }) },
  onMarkAllRead() { markAllNotificationsRead().then(res => { if (res.code === 200) { var ml = this.data.myList.map(function(n) { n.isRead = 1; return n }); this.setData({ myList: ml, unreadCount: 0, myListDisplay: ml }); wx.showToast({ title: '已全部标记已读', icon: 'success' }) } }).catch(() => {}) }
})
