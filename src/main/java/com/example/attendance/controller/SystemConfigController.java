package com.example.attendance.controller;

import com.example.attendance.entity.SystemConfig;
import com.example.attendance.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigController {

    private final SystemConfigService service;

    @GetMapping
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        return ResponseEntity.ok(service.getAllConfigs());
    }

    @PutMapping
    public ResponseEntity<SystemConfig> updateConfig(@RequestBody Map<String, String> payload) {
        String key = payload.get("key");
        String value = payload.get("value");
        if (key == null || value == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.updateConfig(key, value));
    }





        // Cập nhật nhiều cấu hình cùng lúc
    @PutMapping("/batch")
    public ResponseEntity<Map<String, String>> updateMultipleConfigs(@RequestBody Map<String, String> configs) {
        Map<String, String> result = service.updateMultipleConfigs(configs);
        return ResponseEntity.ok(result);
    }

    // Lấy cấu hình dưới dạng Map
    @GetMapping("/map")
    public ResponseEntity<Map<String, String>> getAllConfigsAsMap() {
        Map<String, String> configs = service.getAllConfigsAsMap();
        return ResponseEntity.ok(configs);
    }

    // Lấy giá trị cấu hình cụ thể
    @GetMapping("/{key}")
    public ResponseEntity<String> getConfigValue(@PathVariable String key) {
        String value = service.getConfigValue(key);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(value);
    }
}
