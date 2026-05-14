
package com.example.attendance.service;

import com.example.attendance.dto.LeaveRequestDto;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveStatus;
import com.example.attendance.entity.User;
import com.example.attendance.repository.LeaveRequestRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.Comparator;

import org.apache.poi.ss.usermodel.Row;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository repository;
    private final UserRepository userRepository;

    public LeaveRequest createLeaveRequest(LeaveRequestDto dto) {
        User user = userRepository.findByTelegramId(dto.getTelegramId())
                .orElseThrow(
                        () -> new RuntimeException("Không tìm thấy nhân viên với Telegram ID: " + dto.getTelegramId()));

        String employeeName = user.getUsername();
        System.out.println("Đang xử lý đơn nghỉ phép cho: " + employeeName);
        // Tạo yêu cầu nghỉ phép mới với trạng thái PENDING
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .user(user)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        return repository.save(leaveRequest);
    }

    public List<LeaveRequest> searchRequests(String name, String dateStr) {
        log.info("Searching leaves with name: {}, date: {}", name, dateStr);
        
        LocalDate date = null;
        try {
            if (dateStr != null && !dateStr.isEmpty() && !dateStr.equals("undefined")) {
                date = LocalDate.parse(dateStr);
                log.info("Parsed date: {}", date);
            }
        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", dateStr, e);
            date = null;
        }
        
        // Xử lý tên: nếu null hoặc rỗng thì chuyển thành null để query hoạt động đúng
        String searchName = (name == null || name.trim().isEmpty()) ? null : name.trim();
        
        List<LeaveRequest> results;
        if (searchName == null && date == null) {
            results = repository.findAll();
        } else {
            results = repository.searchLeaves(searchName, date);
        }
        
        log.info("Found {} results", results.size());
        return results;
    }

    public List<LeaveRequest> getAllRequests() {
        return repository.findAll();
    }

    public List<LeaveRequest> getMyRequests(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow();
        return repository.findByUserId(user.getId());
    }

    public LeaveRequest approveRequest(Long id, String managerUsername, boolean isApproved, String rejectReason) {
        LeaveRequest request = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        User manager = userRepository.findByUsername(managerUsername)
                .orElseThrow();

        if (isApproved) {
            request.setStatus(LeaveStatus.APPROVED);
        } else {
            request.setStatus(LeaveStatus.REJECTED);
            request.setRejectReason(rejectReason);
        }
        request.setApprovedBy(manager);

        return repository.save(request);
    }
//     * Lấy thống kê nghỉ phép theo tháng/quý
//  */
public Map<String, Object> getLeaveStatistics(int year, Integer quarter, Integer month) {
    Map<String, Object> result = new HashMap<>();
    
    // 1. Thống kê số lần nghỉ theo nhân viên và lý do
    List<Object[]> leaveCounts = repository.countLeaveByUserAndReason(year, quarter, month);
    
    Map<String, List<Map<String, Object>>> employeeStats = new LinkedHashMap<>();
    for (Object[] row : leaveCounts) {
        String username = (String) row[0];
        String reason = (String) row[1];
        Long count = (Long) row[2];
        
        employeeStats.computeIfAbsent(username, k -> new ArrayList<>())
                     .add(Map.of("reason", reason, "count", count));
    }
    
    // 2. Thống kê tổng số ngày nghỉ
    List<Object[]> dayCounts = repository.countTotalLeaveDaysByUser(year, quarter, month);
    Map<String, Long> totalDaysMap = new HashMap<>();
    for (Object[] row : dayCounts) {
        totalDaysMap.put((String) row[0], (Long) row[1]);
    }
    
    // 3. Xây dựng danh sách nhân viên
    List<Map<String, Object>> employeeList = new ArrayList<>();
    for (Map.Entry<String, List<Map<String, Object>>> entry : employeeStats.entrySet()) {
        String username = entry.getKey();
        List<Map<String, Object>> reasons = entry.getValue();
        
        // Tìm lý do chính (nghỉ nhiều nhất)
        String mainReason = reasons.stream()
            .max(Comparator.comparingLong(r -> (Long) r.get("count")))
            .map(r -> (String) r.get("reason"))
            .orElse("Khác");
        
        long totalTimes = reasons.stream().mapToLong(r -> (Long) r.get("count")).sum();
        long totalDays = totalDaysMap.getOrDefault(username, 0L);
        
        Map<String, Object> empMap = new HashMap<>();
        empMap.put("username", username);
        empMap.put("totalTimes", totalTimes);
        empMap.put("totalDays", totalDays);
        empMap.put("mainReason", mainReason);
        empMap.put("reasons", reasons);
        
        employeeList.add(empMap);
    }
    
    // Sắp xếp theo số lần nghỉ giảm dần
    employeeList.sort((a, b) -> Long.compare((Long) b.get("totalTimes"), (Long) a.get("totalTimes")));
    
    // 4. Thống kê theo tháng (biểu đồ)
    List<Object[]> monthlyStats = repository.countLeaveByMonth(year);
    List<Map<String, Object>> monthlyData = new ArrayList<>();
    for (Object[] row : monthlyStats) {
        Map<String, Object> monthData = new HashMap<>();
        monthData.put("month", row[0]);
        monthData.put("count", row[1]);
        monthlyData.add(monthData);
    }
    result.put("employees", employeeList);
    result.put("monthlyTrend", monthlyData);
    result.put("summary", Map.of(
        "totalEmployees", employeeList.size(),
        "totalLeaveRequests", employeeList.stream().mapToLong(e -> (Long) e.get("totalTimes")).sum(),
        "totalLeaveDays", employeeList.stream().mapToLong(e -> (Long) e.get("totalDays")).sum()
    ));
    return result;
}
// Export Excel thống kê nghỉ phép
public byte[] exportStatisticsToExcel(int year, Integer quarter, Integer month) throws Exception {
    Map<String, Object> stats = getLeaveStatistics(year, quarter, month);
    List<Map<String, Object>> employees = (List<Map<String, Object>>) stats.get("employees");
    
    org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
    org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Thong_ke_nghi_phep_" + year);
    
    // Header: 6 cột
    String[] headers = {"STT", "Nhân Viên", "Số lần nghỉ", "Tổng ngày nghỉ", "Lý do chính", "Chi tiết lý do"};
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
    for (Map<String, Object> emp : employees) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(stt++);
        row.createCell(1).setCellValue((String) emp.get("username"));
        row.createCell(2).setCellValue((Long) emp.get("totalTimes"));
        row.createCell(3).setCellValue((Long) emp.get("totalDays"));
        row.createCell(4).setCellValue((String) emp.get("mainReason"));
        
        List<Map<String, Object>> reasons = (List<Map<String, Object>>) emp.get("reasons");
        String reasonDetail = reasons.stream()
                .map(r -> r.get("reason") + "(" + r.get("count") + ")")
                .collect(java.util.stream.Collectors.joining(", "));
        row.createCell(5).setCellValue(reasonDetail);
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