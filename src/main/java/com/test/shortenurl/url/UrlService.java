package com.test.shortenurl.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.test.shortenurl.config.JwtTokenProvider;
import com.test.shortenurl.domain.url.Url;
import com.test.shortenurl.domain.url.UrlRepository;
import com.test.shortenurl.domain.user.User;
import com.test.shortenurl.domain.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Getter
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    public String shortenUrl(String originalUrl, HttpServletRequest request) {
        String createdBy = getCurrentUsername(request);
        String cacheKey = "shortUrl:" + originalUrl + ":" + createdBy;

        String cachedShortUrl = redisService.getSingleData(cacheKey);
        if (cachedShortUrl != null) {
            return cachedShortUrl;  // 캐시된 URL이 있으면 바로 반환
        }

        if (urlRepository.findByOriginalUrlAndCreatedBy(originalUrl, createdBy).isPresent()) {
            return urlRepository.findByOriginalUrlAndCreatedBy(originalUrl, createdBy).get().getShortUrl();
        }

        String shortUrl = generateUniqueShortUrl(originalUrl + createdBy);

        Url urlMapping = new Url();
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setCreatedAt(LocalDateTime.now());
        urlMapping.setDeleted(false);
        urlMapping.setCreatedBy(createdBy);
        urlRepository.save(urlMapping);

        redisService.saveSingleData(cacheKey, shortUrl);

        return shortUrl;
    }

    public String getCurrentUsername(HttpServletRequest request) {
        String token = extractTokenFromHeader(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            return jwtTokenProvider.getUsernameFromToken(token);
        }

        HttpSession session = request.getSession();
        String anonId = (String) session.getAttribute("ANON_ID");

        if (anonId == null) {
            anonId = "ANON_" + UUID.randomUUID(); // ✅ 익명 ID에 'ANON_' 프리픽스 추가
            session.setAttribute("ANON_ID", anonId);
        }

        return anonId;
    }

    public List<Url> getUrls(HttpServletRequest request) throws JsonProcessingException {
        String username = getCurrentUsername(request);

        String urlsUsernameKey = "user:urls:" + username;

        List<Url> cachedShortUrls = redisService.getListData(urlsUsernameKey);
        if (cachedShortUrls != null) {
            return cachedShortUrls;  // 캐시된 URL이 있으면 바로 반환
        }
        List<Url> shortUrls = urlRepository.findByCreatedByAndDeletedFalse(username);

        redisService.saveListData(urlsUsernameKey, shortUrls);

        return shortUrls;
    }

    public String getOriginalUrl(String shortUrl) {

        String key = shortUrl + ":" + shortUrl;

        if (redisService.getSingleData(key) != null) {
            return redisService.getSingleData(key);
        }

        Optional<Url> original = urlRepository.findByShortUrl(shortUrl);

        if (original.isPresent()) {
            String originalUrl = original.get().getOriginalUrl();
            if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
                originalUrl = "https://" + originalUrl;
            }
            redisService.saveSingleData(key, originalUrl);
            return originalUrl;
        }
        else {
            return "url does not exist";
        }
    }

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

    public ResponseEntity<?> tryLogin(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            String token = jwtTokenProvider.generateToken(authentication);

            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    private String generateShortUrl(String originalUrl) {
        String uniqueData = originalUrl + UUID.randomUUID();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(uniqueData.getBytes(StandardCharsets.UTF_8));

            BigInteger decimal = new BigInteger(1, hash);

            return encodeBase62(decimal).substring(0, 7);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 Algorithm not found", e);
        }
    }
    private String encodeBase62(BigInteger number) {
        StringBuilder sb = new StringBuilder();
        while (number.compareTo(BigInteger.ZERO) > 0) {
            sb.append(BASE62.charAt(number.mod(BigInteger.valueOf(BASE)).intValue()));
            number = number.divide(BigInteger.valueOf(BASE));
        }
        return sb.reverse().toString();
    }

    private String generateUniqueShortUrl(String originalUrl) {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String shortUrl = generateShortUrl(originalUrl);
            if (urlRepository.existsByShortUrl(shortUrl).isPresent()) {
                return shortUrl;
            }
        }
        throw new RuntimeException("Failed to generate a unique short URL after " + maxAttempts + " attempts");
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

    public boolean deleteShortUrl(String shortUrl, HttpServletRequest request) {
        String username = getCurrentUsername(request);
        Optional<Url> original = urlRepository.findByShortUrlAndDeletedFalse(shortUrl);

        if (original.isPresent()) {
            Url url = original.get();
            url.setDeleted(true);
            urlRepository.save(url);

            String urlsUsernameKey = "user:urls" + username;
            redisService.deleteSingleData(urlsUsernameKey);

            return true;
        }
        else {
            return false;
        }
    }

}
