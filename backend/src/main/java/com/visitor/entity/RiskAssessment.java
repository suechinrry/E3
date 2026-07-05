package com.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 风险预警记录：新预约提交后，后端检索相似历史被拒记录，
 * 由大模型评估风险等级（低/中/高）并给出预警理由。
 */
@Data
@TableName("app_risk_assessment")
public class RiskAssessment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer appointmentId;
    /** 风险等级：low / medium / high */
    private String riskLevel;
    /** 风险分数 0-100 */
    private Integer riskScore;
    /** 预警理由 */
    private String reason;
    /** 相似历史案例（JSON 数组） */
    private String similarCases;
    private LocalDateTime createTime;
}
