package com.test.shortenurl.user;

import com.test.shortenurl.common.DuplicateUserException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password, HttpServletResponse response) {
        Map<String, String> tokens = authService.tryLogin(username, password, response);
        String token = tokens.get("token");
        String refreshToken = tokens.get("refreshToken");
        try {
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(Duration.ofDays(1))
                    .build();

            response.addHeader("Set-Cookie", cookie.toString());

            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
        boolean isLoggedOut = authService.tryLogout(refreshToken, response);

        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", expiredCookie.toString());

        return isLoggedOut ? ResponseEntity.ok("Logged out successfully")
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Logout failed");

    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username, String password) {
        try {
            userService.registerUser(username, password);
            return ResponseEntity.ok(Map.of("message", "Registered successfully"));
        } catch (DuplicateUserException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Register failed"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        return authService.refreshWithToken(refreshToken)
                .map(tokens -> {
                    ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.get("refreshToken"))
                            .httpOnly(true)
                            .secure(true)
                            .sameSite("Strict")
                            .path("/")
                            .build();

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(Map.of("token", tokens.get("token")));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

}
