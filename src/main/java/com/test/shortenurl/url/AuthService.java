package com.test.shortenurl.url;

import com.test.shortenurl.config.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, RedisService redisService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisService = redisService;
    }

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

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Optional<String> getAnonymousIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("ANON_ID".equals(cookie.getName())) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }

    public boolean isValidUsername(String username) {
        return username != null && !username.startsWith("ANON_");
    }
}
