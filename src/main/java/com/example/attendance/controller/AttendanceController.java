package com.example.attendance.controller;

import com.example.attendance.entity.Attendance;
import com.example.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;

    @GetMapping
    public ResponseEntity<List<Attendance>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    // @PostMapping("/check-in")
    // public ResponseEntity<Attendance> checkIn() {
    // Authentication authentication =
    // SecurityContextHolder.getContext().getAuthentication();
    // String username = authentication.getName();
    // return ResponseEntity.ok(service.checkIn(username));
    // }

    @PostMapping("/check-in/qr")
    public ResponseEntity<Attendance> checkInQr(@RequestBody Map<String, String> body) {
        String employeeId = body.get("employeeId");
        Long userId = Long.parseLong(employeeId);
        return ResponseEntity.ok(service.checkIn(userId));
    }
}
