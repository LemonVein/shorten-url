package com.test.shortenurl.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.test.shortenurl.config.JwtAuthenticationFilter;
import com.test.shortenurl.config.JwtTokenProvider;
import com.test.shortenurl.domain.url.Url;
import com.test.shortenurl.user.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class UrlApiController {
    private final UrlService urlService;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @PostMapping("/shorten_url")
    public String makeShortenUrl(@RequestParam String originalUrl, HttpServletRequest request, HttpServletResponse response) {
        String shortenUrl = urlService.createShortenUrl(originalUrl, request, response);

        return shortenUrl;
    }

    @GetMapping("/my-urls")
    public ResponseEntity<?> getUserUrls(HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {
        String username = authService.getCurrentUsername(request, response);

        if (username == null || username.startsWith("ANON_")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        List<Url> urls = urlService.getUrls(request, response);

        return ResponseEntity.ok(urls);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(HttpServletRequest request, HttpServletResponse servletResponse) throws JsonProcessingException {

        String token = jwtTokenProvider.getTokenFromRequest(request);

        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "토큰이 만료되었거나 유효하지 않습니다."));
        }

        Map<String, Object> response = authService.checkStatus(request, servletResponse);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shortenCode}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String shortenCode, HttpServletRequest request, HttpServletResponse response) {
        boolean result = urlService.deleteShortUrl(shortenCode, request, response);
        if (result) {
            return ResponseEntity.noContent().build();
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }
}
