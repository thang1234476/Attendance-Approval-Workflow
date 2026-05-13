package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePerformanceDto {
    private Long userId;
    private String username;
    
    // Chỉ số điểm danh
    private long lateCount;          // Số lần đi trễ
    private long earlyCount;         // Số lần về sớm
    private long totalWorkingDays;   // Tổng ngày làm việc
    private double totalHours;       // Tổng giờ làm
    
    // Chỉ số nghỉ phép
    private long totalLeave;          // Tổng số lần nghỉ
    private long rejectedLeave;       // Nghỉ bị từ chối
    private long suddenLeave;         // Nghỉ đột xuất (ốm)
    
    // Chỉ số đánh giá
    private double latenessRate;      // Tỷ lệ đi trễ
    private double earlyLeaveRate;    // Tỷ lệ về sớm
    private double leaveRate;         // Tỷ lệ nghỉ
    private double rejectionRate;     // Tỷ lệ bị từ chối
    
    private String attitudeScore;     // Điểm thái độ (Tốt/Khá/Trung bình/Kém)
    private String productivityScore; // Điểm năng suất
    private String overallRating;     // Đánh giá tổng quan
    private String recommendation;    // Khuyến nghị
}