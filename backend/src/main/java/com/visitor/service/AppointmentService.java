package com.visitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.common.PageResult;
import com.visitor.common.exception.BusinessException;
import com.visitor.entity.Appointment;
import com.visitor.mapper.AppointmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AppointmentService extends ServiceImpl<AppointmentMapper, Appointment> {

    public PageResult<Appointment> pageMy(int userId, int page, int size, String status) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getVisitorId, userId)
                .eq(!"all".equals(status), Appointment::getStatus, status)
                .orderByDesc(Appointment::getId);
        IPage<Appointment> p = page(new Page<>(page, size), qw);
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public PageResult<Appointment> pageHost(int hostId, int page, int size, String status,
                                             LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getHostId, hostId)
                .eq(!"all".equals(status), Appointment::getStatus, status)
                .orderByDesc(Appointment::getId);
        if (startDate != null) qw.ge(Appointment::getStartTime, startDate.atStartOfDay());
        if (endDate != null) qw.le(Appointment::getStartTime, endDate.plusDays(1).atStartOfDay());
        IPage<Appointment> p = page(new Page<>(page, size), qw);
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public List<Appointment> pendingList(int hostId) {
        return lambdaQuery()
                .eq(Appointment::getHostId, hostId)
                .eq(Appointment::getStatus, "pending")
                .list();
    }

    @Transactional
    public void approve(Integer id, String status, String remark) {
        Appointment a = getById(id);
        if (a == null) throw new BusinessException("预约不存在");
        if (!"pending".equals(a.getStatus())) throw new BusinessException("当前状态不可审批");
        a.setStatus(status);
        a.setRemark(remark);
        updateById(a);
    }

    public PageResult<Appointment> pageAll(int page, int size, String status) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(!"all".equals(status), Appointment::getStatus, status)
                .orderByDesc(Appointment::getId);
        IPage<Appointment> p = page(new Page<>(page, size), qw);
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public List<Appointment> allPendingList() {
        return lambdaQuery()
                .eq(Appointment::getStatus, "pending")
                .orderByDesc(Appointment::getId)
                .list();
    }

    public void cancel(Integer id) {
        Appointment a = getById(id);
        if (a == null) throw new BusinessException("预约不存在");
        if (!"pending".equals(a.getStatus())) throw new BusinessException("只能撤销待审核预约");
        a.setStatus("cancelled");
        updateById(a);
    }

    // --- 统计 ---

    public Map<String, Object> hostStats(int hostId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getHostId, hostId);
        if (startDate != null) qw.ge(Appointment::getStartTime, startDate.atStartOfDay());
        if (endDate != null) qw.le(Appointment::getStartTime, endDate.plusDays(1).atStartOfDay());
        List<Appointment> list = baseMapper.selectList(qw);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalVisits", list.size());
        Map<String, Long> companyStats = new LinkedHashMap<>();
        list.forEach(a -> companyStats.merge(a.getCompany(), 1L, Long::sum));
        result.put("companyStats", companyStats.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "count", e.getValue().intValue())).toList());
        return result;
    }

    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        long todayCount = lambdaQuery()
                .ge(Appointment::getCreateTime, today.atStartOfDay())
                .lt(Appointment::getCreateTime, today.plusDays(1).atStartOfDay())
                .count();
        long pendingCount = lambdaQuery()
                .eq(Appointment::getStatus, "pending").count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todayAppointments", todayCount);
        m.put("pendingApprovals", pendingCount);
        return m;
    }

    public List<Map<String, Object>> trend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        List<Map<String, Object>> list = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LocalDate finalD = d;
            long count = lambdaQuery()
                    .ge(Appointment::getCreateTime, finalD.atStartOfDay())
                    .lt(Appointment::getCreateTime, finalD.plusDays(1).atStartOfDay())
                    .count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.toString());
            m.put("count", (int) count);
            list.add(m);
        }
        return list;
    }

    public Map<String, Object> visitorRecordStats(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>();
        if (startDate != null) qw.ge(Appointment::getCreateTime, startDate.atStartOfDay());
        if (endDate != null) qw.le(Appointment::getCreateTime, endDate.plusDays(1).atStartOfDay());
        List<Appointment> list = baseMapper.selectList(qw);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Long> companyStats = new LinkedHashMap<>();
        Map<String, Long> deptStats = new LinkedHashMap<>();
        list.forEach(a -> {
            companyStats.merge(a.getCompany(), 1L, Long::sum);
        });
        result.put("companyStats", companyStats.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "count", e.getValue().intValue())).toList());
        result.put("deptStats", deptStats.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "count", e.getValue().intValue())).toList());
        return result;
    }
}
