package com.example.attendance.controller;

import com.example.attendance.dto.AttendancePageResponse;
import com.example.attendance.dto.AttendanceSummary;
import com.example.attendance.entity.Attendance;
import com.example.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;

    @GetMapping
    public ResponseEntity<AttendancePageResponse> getAllByDate(@RequestParam("date") String dateStr) {
        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(java.time.LocalTime.MAX);

        AttendanceSummary summary = service.getDailySummary(dateStr);

        List<Attendance> details = service.getAttendanceBetween(start, end);

        return ResponseEntity.ok(AttendancePageResponse.builder()
                .summary(summary)
                .details(details)
                .build());
    }

    @PostMapping("/check-in/qr")
    public ResponseEntity<Attendance> checkInQr(@RequestBody Map<String, String> body) {
        String employeeId = body.get("employeeId");
        Long userId = Long.parseLong(employeeId);
        return ResponseEntity.ok(service.checkIn(userId));
    }
}
