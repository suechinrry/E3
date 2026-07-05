package com.visitor.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.entity.Greeting;
import com.visitor.mapper.GreetingMapper;
import org.springframework.stereotype.Service;

@Service
public class GreetingService extends ServiceImpl<GreetingMapper, Greeting> {
}
