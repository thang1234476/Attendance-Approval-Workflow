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
}
