package com.test.shortenurl.user;

import jakarta.servlet.http.HttpServletResponse;
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
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password, HttpServletResponse response) {
        return authService.tryLogin(username, password, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
        ResponseEntity<?> result = authService.tryLogout(refreshToken, response);

        return result;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username, String password) {
        return userService.registerUser(username, password);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        ResponseEntity<?> response = authService.refreshWithToken(refreshToken);

        return response;
    }

}
