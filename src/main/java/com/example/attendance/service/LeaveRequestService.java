package com.example.attendance.service;

import com.example.attendance.dto.LeaveRequestDto;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveStatus;
import com.example.attendance.entity.User;
import com.example.attendance.repository.LeaveRequestRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository repository;
    private final UserRepository userRepository;

    public LeaveRequest createLeaveRequest(String username, LeaveRequestDto dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

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

    // List all for Manager/Admin, List own for Employee
    // For simplicity, we implement getAll and getMyRequests
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

        // Cập nhật trạng thái duyệt/từ chối
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
