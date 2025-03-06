package com.test.shortenurl.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.test.shortenurl.config.UrlGenerator;
import com.test.shortenurl.domain.url.Url;
import com.test.shortenurl.domain.url.UrlRepository;
import com.test.shortenurl.domain.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Getter
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final AuthService authService;
    private final UrlGenerator urlGenerator;

    public String createShortenUrl(String originalUrl, HttpServletRequest request) {
        String createdBy = authService.getCurrentUsername(request);
        String cacheKey = "shortUrl:" + originalUrl + ":" + createdBy;

        String cachedShortUrl = redisService.getSingleData(cacheKey);
        if (cachedShortUrl != null) {
            return cachedShortUrl;  // 캐시된 URL이 있으면 바로 반환
        }

        Optional<Url> urlOptional = urlRepository.findByOriginalUrlAndCreatedByAndDeletedFalse(originalUrl, createdBy);

        if (urlOptional.isPresent()) {
            return urlOptional.get().getShortUrl();
        }

        String shortUrl = urlGenerator.generateUniqueShortUrl(originalUrl + createdBy);

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

        String key = "shortUrl:" + shortUrl;

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

    public boolean deleteShortUrl(String shortUrl, HttpServletRequest request) {
        String username = authService.getCurrentUsername(request);
        Optional<Url> original = urlRepository.findByShortUrlAndDeletedFalse(shortUrl);

        if (original.isPresent()) {
            Url url = original.get();
            url.setDeleted(true);
            urlRepository.save(url);

            String urlsUsernameKey = "user:urls:" + username;
            String urlCacheKey = "shortUrl:" + url.getOriginalUrl() + ":" + username;

            redisService.deleteSingleData(urlsUsernameKey);
            redisService.deleteSingleData(urlCacheKey);

            return true;
        }
        else {
            return false;
        }
    }

    public boolean isOriginalUrlValid(String originalUrl) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(originalUrl, String.class);

            return responseEntity.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteOldAnonymousUrls() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<Url> oldAnonymousUrls = urlRepository.findByCreatedByStartingWithAndCreatedAtBefore("ANON_", sevenDaysAgo);

        if (!oldAnonymousUrls.isEmpty()) {
            oldAnonymousUrls.forEach(url -> redisService.deleteSingleData("shortUrl:" + url.getOriginalUrl() + ":" + url.getCreatedBy()));
            urlRepository.deleteAll(oldAnonymousUrls);
            System.out.println(oldAnonymousUrls.size() + "개의 익명 URL이 삭제되었습니다.");
        }
    }

}
