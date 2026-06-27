package com.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_notification")
public class UserNotification {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String type;
    private String title;
    private String content;
    private Integer appointmentId;
    private Integer isRead;
    private LocalDateTime createTime;
}
