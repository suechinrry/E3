package com.visitor.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.entity.SystemSetting;
import com.visitor.mapper.SystemSettingMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemSettingService extends ServiceImpl<SystemSettingMapper, SystemSetting> {

    public Map<String, String> getMap() {
        return list().stream()
                .collect(Collectors.toMap(SystemSetting::getConfigKey, SystemSetting::getConfigValue));
    }
}
