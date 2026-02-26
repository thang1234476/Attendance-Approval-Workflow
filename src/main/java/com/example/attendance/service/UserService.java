package com.example.attendance.service;

import com.example.attendance.dto.RegisterRequest;
import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (request.getTelegramId() != null && userRepository.existsByTelegramId(request.getTelegramId())) {
            throw new IllegalArgumentException("Telegram ID already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .telegramId(request.getTelegramId())
                .role(request.getRole() != null ? request.getRole() : com.example.attendance.entity.Role.EMPLOYEE)
                .status(com.example.attendance.entity.UserStatus.ACTIVE) // Mặc định ACTIVE khi tạo mới
                .build();

        return userRepository.save(user);
    }

    public User updateUser(Long id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setEmail(updatedUser.getEmail());
        user.setTelegramId(updatedUser.getTelegramId());
        user.setRole(updatedUser.getRole());
        user.setStatus(updatedUser.getStatus());

        // Only update password if provided and not empty (logic specific)
        // For now, assume password update is separate or not handled here to keep
        // simple

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
