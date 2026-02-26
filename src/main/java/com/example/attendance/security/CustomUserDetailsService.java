package com.example.attendance.security;

import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tìm user trong DB theo username, ném exception nếu không tìm thấy
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));

        // User entity đã implement UserDetails, trả về trực tiếp
        // với authorities = ROLE_ADMIN / ROLE_MANAGER / ROLE_EMPLOYEE
        return user;
    }
}
