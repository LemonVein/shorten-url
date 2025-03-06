package com.test.shortenurl.user;

import com.test.shortenurl.domain.user.User;
import com.test.shortenurl.domain.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ResponseEntity<?> registerUser(String username, String password) {
        if (userRepository.existsByUserName(username)){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }

        User user = User.builder()
                .userName(username)
                .password(passwordEncoder.encode(password))
                .role("ROLE_USER")
                .build();
        try {
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "registered Ok"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Can't registered");
        }

    }
}
