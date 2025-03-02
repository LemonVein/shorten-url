package com.test.shortenurl.url;

import com.test.shortenurl.config.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, String password) {
        return authService.tryLogin(username, password);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username, String password) {
        return userService.registerUser(username, password);
    }
}
