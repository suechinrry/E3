const mockOverview = {
  todayAppointments: 12,
  pendingApprovals: 3,
  totalEmployees: 43,
  totalDepartments: 5
}

const mockTrend = [
  { date: '2026-06-05', count: 8 },
  { date: '2026-06-06', count: 15 },
  { date: '2026-06-07', count: 6 },
  { date: '2026-06-08', count: 12 },
  { date: '2026-06-09', count: 20 },
  { date: '2026-06-10', count: 14 },
  { date: '2026-06-11', count: 10 }
]

const mockCompanyRank = [
  { name: 'XX科技', count: 8 },
  { name: 'ABC咨询', count: 5 },
  { name: 'ZZ集团', count: 4 },
  { name: 'HW技术', count: 3 },
  { name: 'AL云', count: 2 }
]

const mockDeptRank = [
  { name: '技术部', count: 20 },
  { name: '市场部', count: 12 },
  { name: '人事部', count: 8 }
]

const mockHostStats = {
  totalVisits: 15,
  thisMonth: 8,
  companies: 6
}

module.exports = { mockOverview, mockTrend, mockCompanyRank, mockDeptRank, mockHostStats }
