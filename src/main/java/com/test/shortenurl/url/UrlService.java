package com.test.shortenurl.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.test.shortenurl.config.UrlGenerator;
import com.test.shortenurl.domain.url.Url;
import com.test.shortenurl.domain.url.UrlRepository;
import com.test.shortenurl.domain.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        if (urlRepository.findByOriginalUrlAndCreatedBy(originalUrl, createdBy).isPresent()) {
            return urlRepository.findByOriginalUrlAndCreatedBy(originalUrl, createdBy).get().getShortUrl();
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
