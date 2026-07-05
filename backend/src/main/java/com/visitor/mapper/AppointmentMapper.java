package com.visitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.entity.Appointment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AppointmentMapper extends BaseMapper<Appointment> {

    /**
     * 按来访单位聚合统计（SQL 聚合，避免全表查出后内存分组）
     */
    @Select("SELECT company AS name, COUNT(*) AS count " +
            "FROM app_appointment " +
            "WHERE deleted = 0 " +
            "  AND host_id = #{hostId} " +
            "  AND #{start} IS NULL OR create_time >= #{start} " +
            "  AND #{end} IS NULL OR create_time < #{end} " +
            "GROUP BY company ORDER BY count DESC")
    List<Map<String, Object>> companyStatsByHost(@Param("hostId") Integer hostId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /**
     * 全局来访单位 + 部门维度统计（关联 sys_user / sys_department）
     */
    @Select("SELECT a.company AS name, COUNT(*) AS count " +
            "FROM app_appointment a " +
            "WHERE a.deleted = 0 " +
            "  AND (#{start} IS NULL OR a.create_time >= #{start}) " +
            "  AND (#{end} IS NULL OR a.create_time < #{end}) " +
            "GROUP BY a.company ORDER BY count DESC")
    List<Map<String, Object>> companyStatsAll(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    /**
     * 被访部门维度统计（关联 sys_user.department_id → sys_department.name）
     */
    @Select("SELECT d.name AS name, COUNT(*) AS count " +
            "FROM app_appointment a " +
            "LEFT JOIN sys_user u ON a.host_id = u.id AND u.deleted = 0 " +
            "LEFT JOIN sys_department d ON u.department_id = d.id AND d.deleted = 0 " +
            "WHERE a.deleted = 0 " +
            "  AND (#{start} IS NULL OR a.create_time >= #{start}) " +
            "  AND (#{end} IS NULL OR a.create_time < #{end}) " +
            "GROUP BY d.name ORDER BY count DESC")
    List<Map<String, Object>> deptStatsAll(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    /**
     * 预约趋势统计（一条SQL按天聚合，避免循环查库）
     */
    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS count " +
            "FROM app_appointment " +
            "WHERE deleted = 0 AND create_time >= #{start} AND create_time < #{end} " +
            "GROUP BY DATE(create_time)")
    List<Map<String, Object>> trendByDate(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);
}
