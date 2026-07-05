package com.visitor.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.entity.RiskAssessment;
import com.visitor.mapper.RiskAssessmentMapper;
import org.springframework.stereotype.Service;

@Service
public class RiskAssessmentService extends ServiceImpl<RiskAssessmentMapper, RiskAssessment> {
}
