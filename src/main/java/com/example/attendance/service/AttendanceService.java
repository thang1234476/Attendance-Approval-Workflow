package com.example.attendance.service;

import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.AttendanceStatus;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository repository;
    private final UserRepository userRepository;
    private final SystemConfigService configService;

    public Attendance checkIn(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

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

        return repository.save(attendance);
    }

    public List<Attendance> getAllUsers() {
        return repository.findAll();
    }
}
