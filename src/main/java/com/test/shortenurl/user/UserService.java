package com.test.shortenurl.user;

import com.test.shortenurl.exception.DuplicateUserException;
import com.test.shortenurl.domain.user.User;
import com.test.shortenurl.domain.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerUser(String username, String password) {
        if (userRepository.existsByUserName(username)) {
            throw new DuplicateUserException("Username already exists");
        }

        User user = User.builder()
                .userName(username)
                .password(passwordEncoder.encode(password))
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
    }
}
