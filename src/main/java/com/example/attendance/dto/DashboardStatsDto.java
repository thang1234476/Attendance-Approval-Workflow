package com.example.attendance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDto {
    private long totalEmployees;
    private long todayCheckIns;
    private long lateCount;
    private long pendingLeaveRequests;
}
