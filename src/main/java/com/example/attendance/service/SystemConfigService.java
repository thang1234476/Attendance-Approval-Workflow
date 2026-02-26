package com.example.attendance.service;

import com.example.attendance.entity.SystemConfig;
import com.example.attendance.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository repository;

    public List<SystemConfig> getAllConfigs() {
        return repository.findAll();
    }

    public SystemConfig updateConfig(String key, String value) {
        SystemConfig config = repository.findByConfigKey(key)
                .orElse(SystemConfig.builder()
                        .configKey(key)
                        .build());
        config.setConfigValue(value);
        return repository.save(config);
    }

    public String getConfigValue(String key) {
        return repository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(null);
    }
}
