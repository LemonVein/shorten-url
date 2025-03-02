package com.test.shortenurl.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.test.shortenurl.config.JwtTokenProvider;
import com.test.shortenurl.domain.url.Url;
import com.test.shortenurl.domain.url.UrlRepository;
import com.test.shortenurl.domain.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    private final AuthService authService;

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    public String shortenUrl(String originalUrl, HttpServletRequest request) {
        String createdBy = authService.getCurrentUsername(request);
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

        String urlsUsernameKey = "user:urls:" + createdBy;

        redisService.deleteSingleData(urlsUsernameKey);

        return shortUrl;
    }

    public List<Url> getUrls(HttpServletRequest request) throws JsonProcessingException {
        String username = authService.getCurrentUsername(request);

        String urlsUsernameKey = "user:urls:" + username;

        List<Url> cachedShortUrls = redisService.getListData(urlsUsernameKey);
        if (cachedShortUrls != null) {
            return cachedShortUrls;
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

        Optional<Url> original = urlRepository.findByShortUrlAndDeletedFalse(shortUrl);

        if (original.isPresent()) {
            String originalUrl = original.get().getOriginalUrl();
            if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
                originalUrl = "https://" + originalUrl;
            }
            redisService.saveSingleData(key, originalUrl);
            return originalUrl;
        }
        else {
            return null;
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

    public boolean deleteShortUrl(String shortUrl, HttpServletRequest request) {
        String username = authService.getCurrentUsername(request);
        Optional<Url> original = urlRepository.findByShortUrlAndDeletedFalse(shortUrl);

        if (original.isPresent()) {
            Url url = original.get();
            url.setDeleted(true);
            urlRepository.save(url);

            String urlsUsernameKey = "user:urls:" + username;
            boolean result = redisService.deleteSingleData(urlsUsernameKey);

            return true;
        }
        else {
            return false;
        }
    }

}
