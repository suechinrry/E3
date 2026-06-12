const mockAppointments = [
  { id: 1001, visitorName: '李四', visitorPhone: '13800000003', company: 'XX科技有限公司', purpose: '商务洽谈', carPlate: '粤B12345', visitorCount: 2, remark: '需要会议室', hostId: 2, hostName: '张三', startTime: '2026-06-15 09:00', endTime: '2026-06-15 12:00', status: 'approved', createTime: '2026-06-10 14:30', qrCode: '' },
  { id: 1002, visitorName: '李四', visitorPhone: '13800000003', company: 'ABC咨询', purpose: '项目对接', carPlate: '', visitorCount: 1, remark: '', hostId: 1, hostName: '系统管理员', startTime: '2026-06-18 14:00', endTime: '2026-06-18 16:00', status: 'pending', createTime: '2026-06-12 09:00', qrCode: '' },
  { id: 1003, visitorName: '李四', visitorPhone: '13800000003', company: 'ZZ集团', purpose: '技术交流', carPlate: '粤C67890', visitorCount: 3, remark: '需要投影', hostId: 2, hostName: '张三', startTime: '2026-06-10 10:00', endTime: '2026-06-10 11:00', status: 'rejected', createTime: '2026-06-08 16:00', qrCode: '' }
]

const hostAppointments = [
  { id: 1, visitorName: '赵六', visitorPhone: '13600001111', company: 'ABC科技', purpose: '技术交流', startTime: '2026-06-12 14:00', endTime: '2026-06-12 16:00', visitorCount: 2, remark: '需要投影设备', status: 'pending', greeting: '' },
  { id: 2, visitorName: '钱七', visitorPhone: '13700002222', company: 'XX咨询', purpose: '项目洽谈', startTime: '2026-06-13 10:00', endTime: '2026-06-13 12:00', visitorCount: 1, remark: '', status: 'pending', greeting: '' }
]

const adminPendingList = [
  { id: 101, visitorName: '周八', company: 'HW技术', hostName: '张三', purpose: '产品演示', startTime: '2026-06-14 09:00', status: 'pending' },
  { id: 102, visitorName: '吴九', company: 'AL云', hostName: '李四', purpose: '合作洽谈', startTime: '2026-06-15 14:00', status: 'pending' },
  { id: 103, visitorName: '郑十', company: 'TX科技', hostName: '王五', purpose: '技术交流', startTime: '2026-06-10 10:00', status: 'approved' }
]

const hostRecords = [
  { id: 1, visitorName: '赵六', company: 'ABC科技', purpose: '技术交流', startTime: '2026-06-11 14:00', status: 'pending', phone: '13600001111' },
  { id: 2, visitorName: '钱七', company: 'XX咨询', purpose: '项目洽谈', startTime: '2026-06-10 10:00', status: 'approved', phone: '13700002222' },
  { id: 3, visitorName: '孙八', company: 'ZZ集团', purpose: '商务拜访', startTime: '2026-06-09 15:30', status: 'confirmed', phone: '13800003333' }
]

module.exports = { mockAppointments, hostAppointments, adminPendingList, hostRecords }
