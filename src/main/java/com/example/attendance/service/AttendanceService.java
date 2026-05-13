package com.example.attendance.service;

import com.example.attendance.dto.AttendanceSummary;
import com.example.attendance.dto.EmployeePerformanceDto;
import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.AttendanceStatus;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.example.attendance.entity.CheckoutStatus;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.attendance.repository.LeaveRequestRepository;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository repository;
    private final UserRepository userRepository;
    private final SystemConfigService configService;
    private final LeaveRequestRepository leaveRequestRepository;

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
            String webhookUrl = "https://n8n.thangnguyen.id.vn/webhook/attendance-checkin";
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

    public List<Attendance> getCheckedInNotCheckoutToday(LocalDateTime start, LocalDateTime end) {
        return repository.findCheckedInNotCheckout(start, end);
    }

    // Checkout
    public Attendance checkout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User ID " + userId + " không tồn tại"));

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // Tìm bản ghi attendance hôm nay chưa checkout
        Attendance attendance = repository.findByUserIdAndCheckInTimeBetween(user.getId(), startOfDay, endOfDay)
                .orElseThrow(() -> new IllegalStateException("User has not checked in today"));

        if (attendance.getCheckOutTime() != null) {
            throw new IllegalStateException("User already checked out today");
        }

        // Lấy cấu hình giờ tan làm
        String workEndTimeStr = configService.getConfigValue("WORK_END_TIME");
        String earlyLeaveThresholdStr = configService.getConfigValue("EARLY_LEAVE_THRESHOLD");
        String otThresholdStr = configService.getConfigValue("OT_THRESHOLD");

        LocalTime workEndTime = workEndTimeStr != null ? LocalTime.parse(workEndTimeStr) : LocalTime.of(17, 0);
        int earlyLeaveThreshold = earlyLeaveThresholdStr != null ? Integer.parseInt(earlyLeaveThresholdStr) : 5;
        int otThreshold = otThresholdStr != null ? Integer.parseInt(otThresholdStr) : 20;

        // Tính trạng thái checkout
        LocalTime checkoutTime = now.toLocalTime();
        String checkoutStatus;
        if (checkoutTime.isBefore(workEndTime.minusMinutes(earlyLeaveThreshold))) {
            checkoutStatus = "EARLY";
        } else if (checkoutTime.isAfter(workEndTime.plusMinutes(otThreshold))) {
            checkoutStatus = "LATE";
        } else {
            checkoutStatus = "ON_TIME";
        }

        // Tính tổng giờ làm
        long minutes = java.time.Duration.between(attendance.getCheckInTime(), now).toMinutes();
        double totalHours = minutes / 60.0;

        // Tính giờ OT (nếu làm sau giờ tan làm)
        double overtimeHours = 0;
        LocalDateTime endOfWorkTime = attendance.getCheckInTime().toLocalDate().atTime(workEndTime);
        if (now.isAfter(endOfWorkTime)) {
            long otMinutes = java.time.Duration.between(endOfWorkTime, now).toMinutes();
            overtimeHours = otMinutes / 60.0;
        }

        attendance.setCheckOutTime(now);
        attendance.setTotalHours(totalHours);
        attendance.setOvertimeHours(overtimeHours);
        attendance.setCheckoutStatus(com.example.attendance.entity.CheckoutStatus.valueOf(checkoutStatus));

        return repository.save(attendance);
    }

    // Export Excel tổng giờ làm theo nhân viên (rút gọn)
    public byte[] exportToExcel(int year, int month) throws Exception {
        List<Attendance> attendances = repository.findByMonthAndYear(year, month);

        // Nhóm theo nhân viên và tính tổng giờ
        Map<String, Double> workingHours = new HashMap<>();
        Map<String, String> employeeInfo = new HashMap<>();

        for (Attendance att : attendances) {
            String username = att.getUser().getUsername();
            double hours = att.getTotalHours() != null ? att.getTotalHours() : 0;
            workingHours.put(username, workingHours.getOrDefault(username, 0.0) + hours);
            if (!employeeInfo.containsKey(username)) {
                employeeInfo.put(username, att.getUser().getEmail());
            }
        }

        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Tong_gio_lam" + month + "_" + year);

        // Header: 4 cột
        String[] headers = { "STT", "Nhân Viên", "Email", "Tổng giờ làm (giờ)" };
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Đổ dữ liệu
        int rowNum = 1;
        int stt = 1;
        for (Map.Entry<String, Double> entry : workingHours.entrySet()) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(stt++);
            row.createCell(1).setCellValue(entry.getKey());
            row.createCell(2).setCellValue(employeeInfo.get(entry.getKey()));
            row.createCell(3).setCellValue(Math.round(entry.getValue() * 100) / 100.0);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    // Lấy tổng giờ làm theo tháng
    public Map<String, Double> getWorkingHoursByMonth(int year, int month) {
        List<Attendance> attendances = repository.findByMonthAndYear(year, month);
        Map<String, Double> result = new HashMap<>();

        for (Attendance att : attendances) {
            String username = att.getUser().getUsername();
            double hours = att.getTotalHours() != null ? att.getTotalHours() : 0;
            result.put(username, result.getOrDefault(username, 0.0) + hours);
        }
        return result;
    }
    // Phân tích thái độ và năng suất nhân viên
public List<EmployeePerformanceDto> analyzeEmployeePerformance(int year, Integer quarter, Integer month) {
    List<Object[]> perfData = repository.getEmployeePerformance(year, quarter, month);
    List<Object[]> leaveData = leaveRequestRepository.getLeaveStatsByEmployee(year, quarter, month);
    
    // Chuyển đổi dữ liệu nghỉ phép thành Map
    Map<Long, Object[]> leaveMap = new HashMap<>();
    for (Object[] row : leaveData) {
        leaveMap.put((Long) row[0], row);
    }
    
    List<EmployeePerformanceDto> results = new ArrayList<>();
    
    for (Object[] row : perfData) {
        Long userId = (Long) row[0];
        String username = (String) row[1];
        long lateCount = (Long) row[2];
        long earlyCount = (Long) row[3];
        long totalWorkingDays = (Long) row[4];
        double totalHours = (Double) row[5];
        
        Object[] leaveRow = leaveMap.getOrDefault(userId, new Object[]{userId, username, 0L, 0L, 0L});
        long totalLeave = (Long) leaveRow[2];
        long rejectedLeave = (Long) leaveRow[3];
        long suddenLeave = (Long) leaveRow[4];
        
        // Tính tỷ lệ
        double latenessRate = totalWorkingDays > 0 ? (double) lateCount / totalWorkingDays * 100 : 0;
        double earlyLeaveRate = totalWorkingDays > 0 ? (double) earlyCount / totalWorkingDays * 100 : 0;
        double leaveRate = totalWorkingDays > 0 ? (double) totalLeave / totalWorkingDays * 100 : 0;
        double rejectionRate = totalLeave > 0 ? (double) rejectedLeave / totalLeave * 100 : 0;
        
        // Đánh giá thái độ
        String attitudeScore;
        if (latenessRate <= 5 && earlyLeaveRate <= 5 && rejectionRate <= 10) {
            attitudeScore = "🟢 Tốt";
        } else if (latenessRate <= 15 && earlyLeaveRate <= 15 && rejectionRate <= 25) {
            attitudeScore = "🟡 Khá";
        } else if (latenessRate <= 30 && earlyLeaveRate <= 30) {
            attitudeScore = "🟠 Trung bình";
        } else {
            attitudeScore = "🔴 Kém";
        }
        
        // Đánh giá năng suất (dựa trên tổng giờ làm so với chuẩn 8h/ngày)
        double standardHours = totalWorkingDays * 8;
        double productivity = standardHours > 0 ? (totalHours / standardHours) * 100 : 0;
        String productivityScore;
        if (productivity >= 95) {
            productivityScore = "🟢 Xuất sắc";
        } else if (productivity >= 85) {
            productivityScore = "🟡 Tốt";
        } else if (productivity >= 70) {
            productivityScore = "🟠 Trung bình";
        } else {
            productivityScore = "🔴 Yếu";
        }
        
        // Đánh giá tổng quan
        String overallRating;
        String recommendation;
        if (latenessRate <= 5 && earlyLeaveRate <= 5 && productivity >= 90) {
            overallRating = "🌟 Nhân viên xuất sắc";
            recommendation = "Cân nhắc khen thưởng, tạo động lực";
        } else if (latenessRate > 30 || earlyLeaveRate > 30 || productivity < 60) {
            overallRating = "⚠️ Cần cải thiện ngay";
            recommendation = "Họp trao đổi, cảnh cáo, theo dõi sát sao";
        } else if (latenessRate > 15 || earlyLeaveRate > 15 || productivity < 75) {
            overallRating = "📌 Cần cải thiện";
            recommendation = "Nhắc nhở, đào tạo thêm kỹ năng";
        } else {
            overallRating = "✅ Đạt yêu cầu";
            recommendation = "Duy trì và phát huy";
        }
        
        EmployeePerformanceDto dto = EmployeePerformanceDto.builder()
                .userId(userId).username(username)
                .lateCount(lateCount).earlyCount(earlyCount)
                .totalWorkingDays(totalWorkingDays).totalHours(totalHours)
                .totalLeave(totalLeave).rejectedLeave(rejectedLeave).suddenLeave(suddenLeave)
                .latenessRate(Math.round(latenessRate * 100) / 100.0)
                .earlyLeaveRate(Math.round(earlyLeaveRate * 100) / 100.0)
                .leaveRate(Math.round(leaveRate * 100) / 100.0)
                .rejectionRate(Math.round(rejectionRate * 100) / 100.0)
                .attitudeScore(attitudeScore).productivityScore(productivityScore)
                .overallRating(overallRating).recommendation(recommendation)
                .build();
        
        results.add(dto);
    }
    
    // Sắp xếp theo điểm thái độ (Tốt → Kém)
    results.sort((a, b) -> {
        String order = "🟢 Tốt,🟡 Khá,🟠 Trung bình,🔴 Kém";
        return order.indexOf(a.getAttitudeScore()) - order.indexOf(b.getAttitudeScore());
    });
    
    return results;
}
// Export Excel phân tích thái độ & năng suất
public byte[] exportPerformanceToExcel(int year, Integer quarter, Integer month) throws Exception {
    List<EmployeePerformanceDto> data = analyzeEmployeePerformance(year, quarter, month);
    
    org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
    org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Phan_tich_thai_do_" + year);
    
    // Header: 9 cột
    String[] headers = {"STT", "Nhân Viên", "Đi trễ (lần/%)", "Về sớm (lần/%)", "Số lần nghỉ", "Thái độ", "Năng suất", "Đánh giá", "Khuyến nghị"};
    org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
    org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
    org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    
    for (int i = 0; i < headers.length; i++) {
        org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
    }
    
    // Đổ dữ liệu
    int rowNum = 1;
    int stt = 1;
    for (EmployeePerformanceDto emp : data) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(stt++);
        row.createCell(1).setCellValue(emp.getUsername());
        row.createCell(2).setCellValue(emp.getLateCount() + " lần (" + emp.getLatenessRate() + "%)");
        row.createCell(3).setCellValue(emp.getEarlyCount() + " lần (" + emp.getEarlyLeaveRate() + "%)");
        row.createCell(4).setCellValue(emp.getTotalLeave() + " lần (" + emp.getLeaveRate() + "%)");
        row.createCell(5).setCellValue(emp.getAttitudeScore());
        row.createCell(6).setCellValue(emp.getProductivityScore());
        row.createCell(7).setCellValue(emp.getOverallRating());
        row.createCell(8).setCellValue(emp.getRecommendation());
    }
    
    for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
    }
    
    java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
    workbook.write(outputStream);
    workbook.close();
    
    return outputStream.toByteArray();
}

}