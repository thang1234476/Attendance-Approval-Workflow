package com.example.attendance.service;

import com.example.attendance.dto.DashboardStatsDto;
import com.example.attendance.entity.LeaveStatus;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.LeaveRequestRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public DashboardStatsDto getStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return DashboardStatsDto.builder()
                .totalEmployees(userRepository.count())
                .todayCheckIns(attendanceRepository.countTodayCheckIns(startOfDay, endOfDay))
                .lateCount(attendanceRepository.countByStatusAndCheckInTimeBetween(
                        com.example.attendance.entity.AttendanceStatus.LATE, startOfDay, endOfDay))
                .pendingLeaveRequests(leaveRequestRepository.countByStatus(LeaveStatus.PENDING))
                .build();
    }
}
