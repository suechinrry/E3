package com.visitor.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.entity.VisitRecord;
import com.visitor.mapper.VisitRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class VisitRecordService extends ServiceImpl<VisitRecordMapper, VisitRecord> {
}
