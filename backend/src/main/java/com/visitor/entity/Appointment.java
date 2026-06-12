package com.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_appointment")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer visitorId;
    private String visitorName;
    private String visitorPhone;
    private String company;
    private String purpose;
    private String carPlate;
    private Integer visitorCount;
    private String remark;
    private Integer hostId;
    private String hostName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
