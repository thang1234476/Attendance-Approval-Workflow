package com.example.attendance.controller;

import com.example.attendance.dto.LeaveRequestDto;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService service;

    @PostMapping
    public ResponseEntity<LeaveRequest> createRequest(@RequestBody LeaveRequestDto dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(service.createLeaveRequest(auth.getName(), dto));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequest>> getMyRequests() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(service.getMyRequests(auth.getName()));
    }
@GetMapping
@PreAuthorize("isAuthenticated()")
public ResponseEntity<List<LeaveRequest>> getLeaves(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String date) {
    // Gọi hàm searchRequests mới tạo ở Bước 1
    return ResponseEntity.ok(service.searchRequests(name, date)); 
}

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<LeaveRequest>> getAllRequests() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<LeaveRequest> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean approved = (boolean) payload.get("approved");
        String reason = (String) payload.get("rejectReason");
        return ResponseEntity.ok(service.approveRequest(id, auth.getName(), approved, reason));
    }
}
