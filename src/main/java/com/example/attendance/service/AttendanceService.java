package com.example.attendance.service;

import com.example.attendance.dto.AttendanceSummary;
import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.AttendanceStatus;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository repository;
    private final UserRepository userRepository;
    private final SystemConfigService configService;

    public Attendance checkIn(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User ID " + userId + " không tồn tại"));

        // Kiểm tra xem nhân viên đã điểm danh hôm nay chưa
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        if (repository.findByUserIdAndCheckInTimeBetween(user.getId(), startOfDay, endOfDay).isPresent()) {
            throw new IllegalStateException("User already checked in today");
        }

        LocalDateTime now = LocalDateTime.now();
        String workStartTimeStr = configService.getConfigValue("WORK_START_TIME");
        String gracePeriodStr = configService.getConfigValue("GRACE_PERIOD_MINS");

        // Giá trị mặc định nếu không tìm thấy cấu hình
        LocalTime workStartTime = workStartTimeStr != null ? LocalTime.parse(workStartTimeStr) : LocalTime.of(9, 0);
        int gracePeriod = gracePeriodStr != null ? Integer.parseInt(gracePeriodStr) : 15;

        AttendanceStatus status;
        // So sánh thời gian hiện tại với giờ làm việc + thời gian ân hạn
        if (now.toLocalTime().isAfter(workStartTime.plusMinutes(gracePeriod))) {
            status = AttendanceStatus.LATE;
        } else {
            status = AttendanceStatus.ON_TIME;
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .checkInTime(now)
                .status(status)
                .build();

        Attendance savedAttendance = repository.save(attendance);

        sendCheckInToN8n(user.getUsername(), user.getTelegramId(), status.toString(), savedAttendance.getCheckInTime());

        return savedAttendance;
    }

    public List<Attendance> getAllUsers() {
        return repository.findAll();
    }

    public List<Attendance> getAttendanceBetween(LocalDateTime start, LocalDateTime end) {
        return repository.findAllByCheckInTimeBetween(start, end);
    }

    public List<Attendance> getMyAttendanceBetween(Long userId, LocalDateTime start, LocalDateTime end) {
        return repository.findByUserIdAndCheckInTimeBetween(userId, start, end)
                .map(List::of)
                .orElse(List.of());
    }

    public AttendanceSummary getDailySummary(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Long totalEmployees = userRepository.count();
        Long ontime = repository.countByStatusAndCheckInTimeBetween(AttendanceStatus.ON_TIME, startOfDay, endOfDay);
        Long late = repository.countByStatusAndCheckInTimeBetween(AttendanceStatus.LATE, startOfDay, endOfDay);

        long notCheckedIn = totalEmployees - ontime - late;

        if (notCheckedIn < 0)
            notCheckedIn = 0;

        return AttendanceSummary.builder()
                .totalEmployees(totalEmployees)
                .onTimeCount(ontime)
                .lateCount(late)
                .notCheckedIn(notCheckedIn)
                .build();
    }

    private void sendCheckInToN8n(String name, String telegramId, String status, LocalDateTime time) {
        try {
            String webhookUrl = "https://thawnn8n.app.n8n.cloud/webhook-test/attendance-checkin";
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("telegramId", telegramId);
            data.put("status", status);
            data.put("checkInTime", time.toString());

            restTemplate.postForEntity(webhookUrl, data, String.class);
        } catch (Exception e) {
            System.err.println("Lỗi gửi Webhook n8n: " + e.getMessage());
        }
    }
}
