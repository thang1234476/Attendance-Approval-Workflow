package com.example.attendance.controller;

import com.example.attendance.dto.AuthRequest;
import com.example.attendance.dto.AuthResponse;
import com.example.attendance.dto.RegisterRequest;
import com.example.attendance.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import com.example.attendance.entity.User;
import com.example.attendance.service.UserService;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService service;
    private final UserService userService;  // Thêm dòng này vào constructor

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(
            @RequestBody AuthRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }
    @GetMapping("/me")
public ResponseEntity<?> getCurrentUser(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    String username = authentication.getName();
    // Cần inject UserService hoặc AuthenticationService để lấy user
    User user = userService.findByUsername(username);
    
    if (user == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }
    
    return ResponseEntity.ok(Map.of(
        "id", user.getId(),
        "username", user.getUsername(),
        "role", user.getRole(),
        "email", user.getEmail()
    ));
}
}
