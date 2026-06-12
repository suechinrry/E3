package com.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_greeting")
public class Greeting {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer appointmentId;
    private String greetingText;
    private String seatSuggestion;
    private String notes;
    private String status;
    private LocalDateTime createTime;
}
