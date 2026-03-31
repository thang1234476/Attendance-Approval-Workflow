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






        // Lấy nhiều cấu hình cùng lúc
    public java.util.Map<String, String> getMultipleConfigs(List<String> keys) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        for (String key : keys) {
            result.put(key, getConfigValue(key));
        }
        return result;
    }

    // Cập nhật nhiều cấu hình cùng lúc
    public java.util.Map<String, String> updateMultipleConfigs(java.util.Map<String, String> configs) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, String> entry : configs.entrySet()) {
            SystemConfig updated = updateConfig(entry.getKey(), entry.getValue());
            result.put(updated.getConfigKey(), updated.getConfigValue());
        }
        return result;
    }

    // Lấy tất cả cấu hình dưới dạng Map
    public java.util.Map<String, String> getAllConfigsAsMap() {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        for (SystemConfig config : repository.findAll()) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }
}
