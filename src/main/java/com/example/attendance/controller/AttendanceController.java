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

    @GetMapping("/checkedin/today")
    public ResponseEntity<List<Attendance>> getCheckedInToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        List<Attendance> attendances = service.getCheckedInNotCheckoutToday(start, end);

        return ResponseEntity.ok(attendances);
    }

    @PostMapping("/check-in/qr")
    public ResponseEntity<Attendance> checkInQr(@RequestBody Map<String, String> body) {
        String employeeId = body.get("employeeId");
        Long userId = Long.parseLong(employeeId);
        return ResponseEntity.ok(service.checkIn(userId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, String> body) {
        try {
            Long userId = Long.parseLong(body.get("userId"));
            Attendance attendance = service.checkout(userId);
            return ResponseEntity.ok(attendance);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAttendance(@RequestParam int month, @RequestParam int year) {
        try {
            byte[] excel = service.exportToExcel(year, month);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "attachment; filename=attendance_" + year + "_" + month + ".xlsx")
                    .body(excel);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
