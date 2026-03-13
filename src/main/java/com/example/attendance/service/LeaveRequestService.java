
package com.example.attendance.service;

import com.example.attendance.dto.LeaveRequestDto;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveStatus;
import com.example.attendance.entity.User;
import com.example.attendance.repository.LeaveRequestRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

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
}