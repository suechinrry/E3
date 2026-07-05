package com.visitor.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.entity.Notification;
import com.visitor.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

@Service
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {
}
