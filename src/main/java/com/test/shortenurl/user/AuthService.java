package com.test.shortenurl.user;

import com.test.shortenurl.domain.common.RedisService;
import com.test.shortenurl.config.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    public Map<String, String> tryLogin(String username, String password, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        String token = jwtTokenProvider.generateToken(authentication);

        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        redisService.saveRefreshToken("refreshToken:" + username, refreshToken);

        return Map.of("token", token, "refreshToken", refreshToken);

    }

    public boolean tryLogout(String refreshToken, HttpServletResponse response) {
        try {
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

            redisService.deleteSingleData("refreshToken:" + username);

            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public Map<String, Object> checkStatus(HttpServletRequest request, HttpServletResponse servletResponse) {
        Map<String, Object> response = new HashMap<>();
        String username = getCurrentUsername(request, servletResponse);

        boolean isAuthenticated = username != null && isValidUsername(username);

        response.put("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            response.put("username", username);
        }

        return response;
    }

    public String getCurrentUsername(HttpServletRequest request, HttpServletResponse response) {
        String token = extractTokenFromHeader(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            return jwtTokenProvider.getUsernameFromToken(token);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("ANON_ID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String anonId = "ANON_" + UUID.randomUUID();

        ResponseCookie cookie = ResponseCookie.from("ANON_ID", anonId)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return anonId;
    }

    public Optional<Map<String, String>> refreshWithToken(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return Optional.empty();
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String storedRefreshToken = redisService.getRefreshToken("refreshToken:" + username);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            return Optional.empty();
        }

        // 기존 리프레시 토큰 삭제 (재사용 방지)
        redisService.deleteSingleData("refreshToken:" + username);

        // 새 액세스 토큰 & 새 리프레시 토큰 발급
        String newAccessToken = jwtTokenProvider.generateTokenByUsername(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshTokenByUsername(username);

        // 새 리프레시 토큰 저장
        redisService.saveRefreshToken("refreshToken:" + username, newRefreshToken);

        return Optional.of(Map.of("token", newAccessToken, "refreshToken", newRefreshToken));
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
