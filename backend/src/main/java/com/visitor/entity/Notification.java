package com.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("not_notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String content;
    private String targetRole;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
