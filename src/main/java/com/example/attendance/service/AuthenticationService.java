package com.example.attendance.service;

import com.example.attendance.dto.AuthRequest;
import com.example.attendance.dto.AuthResponse;
import com.example.attendance.dto.RegisterRequest;
import com.example.attendance.entity.Role;
import com.example.attendance.entity.User;
import com.example.attendance.entity.UserStatus;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        /**
         * Đăng ký tài khoản mới và trả về JWT token.
         */
        public AuthResponse register(RegisterRequest request) {
                // Kiểm tra username đã tồn tại chưa
                if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                        throw new IllegalStateException("Tên đăng nhập đã tồn tại");
                }
                if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                        throw new IllegalStateException("Email đã được sử dụng");
                }

                // Tạo user mới, mặc định role EMPLOYEE nếu không chỉ định
                User user = User.builder()
                                .username(request.getUsername())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .telegramId(request.getTelegramId())
                                .role(request.getRole() != null ? request.getRole() : Role.EMPLOYEE)
                                .status(UserStatus.ACTIVE) // Mặc định ACTIVE khi đăng ký
                                .build();

                userRepository.save(user);

                // Dùng User entity trực tiếp vì nó đã implement UserDetails
                String jwtToken = jwtService.generateToken(user);
                return AuthResponse.builder()
                                .token(jwtToken)
                                .role(user.getRole().name())
                                .build();
        }

        /**
         * Xác thực username/password và trả về JWT token.
         * AuthenticationManager sẽ ném exception nếu sai thông tin.
         */
        public AuthResponse authenticate(AuthRequest request) {
                // Spring Security sẽ kiểm tra username/password và ném exception nếu sai
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getUsername(),
                                                request.getPassword()));

                // Lấy user từ DB (đã xác thực thành công ở trên)
                User user = userRepository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

                // Dùng User entity trực tiếp (implements UserDetails) để tạo token
                String jwtToken = jwtService.generateToken(user);
                return AuthResponse.builder()
                                .token(jwtToken)
                                .role(user.getRole().name())
                                .build();
        }
}
