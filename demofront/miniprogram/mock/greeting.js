const mockGreeting = {
  greeting: '欢迎XX科技的李四先生莅临我司洽谈合作，张三经理已在会议室A等候，请前台引导至二楼。',
  seatSuggestion: '建议安排在会议室A，配有投影仪和白板，已准备茶水。',
  notes: '来访人员共2位，请准备2份访客证和停车券。',
  status: 'completed'
}

const mockVerifyResult = {
  valid: true,
  confirmed: false,
  message: '核验通过，欢迎来访',
  appointmentId: 1001,
  visitorName: '李四',
  company: 'XX科技有限公司',
  hostName: '张三',
  time: '2026-06-15 09:00 - 12:00',
  status: 'approved'
}

module.exports = { mockGreeting, mockVerifyResult }
