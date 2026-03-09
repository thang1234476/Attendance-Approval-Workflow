package com.example.attendance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceSummary {
    private long totalEmployees;
    private long onTimeCount;
    private long lateCount;
    private long notCheckedIn;
}