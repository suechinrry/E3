package com.visitor.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.entity.Holiday;
import com.visitor.mapper.HolidayMapper;
import org.springframework.stereotype.Service;

@Service
public class HolidayService extends ServiceImpl<HolidayMapper, Holiday> {
}
