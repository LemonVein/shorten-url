package com.test.shortenurl.user;

import com.test.shortenurl.common.RedisService;
import com.test.shortenurl.config.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    public ResponseEntity<?> tryLogin(String username, String password, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );


            String token = jwtTokenProvider.generateToken(authentication);

            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            redisService.saveRefreshToken("refreshToken:" + username, refreshToken);

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

    public ResponseEntity<?> tryLogout(String refreshToken, HttpServletResponse response) {
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

    public Map<String, Object> checkStatus(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String username = getCurrentUsername(request);

        boolean isAuthenticated = username != null && isValidUsername(username);

        response.put("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            response.put("username", username);
        }

        return response;
    }

    public String getCurrentUsername(HttpServletRequest request) {
        String token = extractTokenFromHeader(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            return jwtTokenProvider.getUsernameFromToken(token);
        }

        HttpSession session = request.getSession();
        String anonId = (String) session.getAttribute("ANON_ID");

        if (anonId == null) {
            anonId = "ANON_" + UUID.randomUUID();
            session.setAttribute("ANON_ID", anonId);
        }

        return anonId;
    }

    public ResponseEntity<?> refreshWithToken(String refreshToken) {
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


    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean isValidUsername(String username) {
        return username != null && !username.startsWith("ANON_");
    }
}
