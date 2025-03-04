package com.test.shortenurl.url;

import com.test.shortenurl.config.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password, HttpServletResponse response) {
        return authService.tryLogin(username, password, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)  // 즉시 만료
                .build();

        response.addHeader("Set-Cookie", expiredCookie.toString());

        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username, String password) {
        return userService.registerUser(username, password);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        String storedRefreshToken = redisService.getRefreshToken("refreshToken:" + username);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 기존 리프레시 토큰 무효화 (재사용 방지)
        redisService.deleteSingleData("refreshToken:" + username);

        // 새 리프레시 토큰 발급
        String newAccessToken = jwtTokenProvider.generateTokenByUsername(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshTokenByUsername(username);

        // 새 리프레시 토큰을 Redis에 저장
        redisService.saveRefreshToken("refreshToken:" + username, newRefreshToken);

        // 새 리프레시 토큰을 쿠키에 저장
        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("token", newAccessToken));
    }

}
