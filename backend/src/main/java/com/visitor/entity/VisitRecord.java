package com.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_visit_record")
public class VisitRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer appointmentId;
    private Integer guardId;
    private LocalDateTime confirmTime;
    private String remark;
}
