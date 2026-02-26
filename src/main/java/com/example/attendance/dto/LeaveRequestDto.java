package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
