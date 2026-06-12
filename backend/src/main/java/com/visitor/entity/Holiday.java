package com.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("not_holiday")
public class Holiday {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private LocalDate date;
    private String type;
    private LocalDateTime createTime;
}
