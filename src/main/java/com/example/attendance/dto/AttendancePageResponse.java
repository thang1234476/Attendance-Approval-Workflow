package com.example.attendance.dto;

import java.util.List;

import com.example.attendance.entity.Attendance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendancePageResponse {
    private AttendanceSummary summary;
    private List<Attendance> details;
}