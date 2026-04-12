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
import com.example.attendance.entity.CheckoutStatus;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

    // Export Excel theo tháng
    public byte[] exportToExcel(int year, int month) throws Exception {
        List<Attendance> attendances = repository.findByMonthAndYear(year, month);

        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Attendance " + month + "/" + year);

        String[] headers = {"ID", "Nhân viên", "Ngày", "Check-in", "Check-out", "Trạng thái check-in", "Trạng thái check-out", "Tổng giờ", "OT", "Ghi chú"};
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

        int rowNum = 1;
        for (Attendance att : attendances) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(att.getId());
            row.createCell(1).setCellValue(att.getUser().getUsername());
            row.createCell(2).setCellValue(att.getCheckInTime().toLocalDate().toString());
            row.createCell(3).setCellValue(att.getCheckInTime().toLocalTime().toString());
            row.createCell(4).setCellValue(att.getCheckOutTime() != null ? att.getCheckOutTime().toLocalTime().toString() : "");
            row.createCell(5).setCellValue(att.getStatus().toString());
            row.createCell(6).setCellValue(att.getCheckoutStatus() != null ? att.getCheckoutStatus().toString() : "");
            row.createCell(7).setCellValue(att.getTotalHours() != null ? att.getTotalHours() : 0);
            row.createCell(8).setCellValue(att.getOvertimeHours() != null ? att.getOvertimeHours() : 0);
            row.createCell(9).setCellValue(att.getNote() != null ? att.getNote() : "");
        }

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
}