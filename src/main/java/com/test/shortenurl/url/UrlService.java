package com.test.shortenurl.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.test.shortenurl.domain.common.RedisService;
import com.test.shortenurl.domain.url.UrlGenerator;
import com.test.shortenurl.domain.url.Url;
import com.test.shortenurl.domain.url.UrlRepository;
import com.test.shortenurl.domain.user.User;
import com.test.shortenurl.domain.user.UserRepository;
import com.test.shortenurl.user.AuthService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Getter
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private static final String SINGLE_URL_KEY = "shortUrl:";
    private static final String MULTIPLE_URL_KEY = "user:urls:"; // 사용자 이름과 함께 사용
    private static final int MAX_RETRY_COUNT = 3;

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final AuthService authService;
    private final UrlGenerator urlGenerator;
    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/122.0.0.0 Safari/537.36")
                .build();
    }

    public String createShortenUrl(String originalUrl, HttpServletRequest request, HttpServletResponse response) {
        String createdBy = authService.getCurrentUsername(request, response);
        String cacheKey = "shortUrl:" + originalUrl + ":" + createdBy;

        String cachedShortUrl = redisService.getSingleData(cacheKey);
        if (cachedShortUrl != null) {
            return cachedShortUrl;
        }

        Optional<Url> urlOptional = urlRepository.findByOriginalUrlAndCreatedByAndDeletedFalse(originalUrl, createdBy);

        if (urlOptional.isPresent()) {
            return urlOptional.get().getShortUrl();
        }

        User user = userRepository.findByUserName(createdBy)
                    .orElse(null);

        Url newUrl = new Url();
        newUrl.setOriginalUrl(originalUrl);
        newUrl.setCreatedAt(LocalDateTime.now());
        newUrl.setDeleted(false);
        newUrl.setCreatedBy(createdBy);
        newUrl.setUser(user);

        newUrl = urlRepository.save(newUrl);

        String shortUrl = urlGenerator.generateUniqueShortUrl(newUrl.getId());

        newUrl.setShortUrl(shortUrl);

        urlRepository.save(newUrl);

        redisService.saveSingleData(cacheKey, shortUrl);

        String urlsUsernameKey = MULTIPLE_URL_KEY + createdBy;

        redisService.deleteSingleData(urlsUsernameKey);

        return shortUrl;
    }

    public List<Url> getUrls(HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {
        String username = authService.getCurrentUsername(request, response);

        String urlsUsernameKey = MULTIPLE_URL_KEY + username;

        List<Url> cachedShortUrls = redisService.getListData(urlsUsernameKey);
        if (cachedShortUrls != null) {
            return cachedShortUrls;
        }
        List<Url> shortUrls = urlRepository.findByCreatedByAndDeletedFalse(username);

        redisService.saveListData(urlsUsernameKey, shortUrls);

        return shortUrls;
    }

    public String getOriginalUrl(String shortUrl) {

        String key = SINGLE_URL_KEY + shortUrl;

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

    public boolean deleteShortUrl(String shortUrl, HttpServletRequest request, HttpServletResponse response) {
        String username = authService.getCurrentUsername(request, response);
        Optional<Url> original = urlRepository.findByShortUrlAndDeletedFalse(shortUrl);

        if (original.isPresent()) {
            Url url = original.get();
            url.setDeleted(true);
            urlRepository.save(url);

            String urlsUsernameKey = MULTIPLE_URL_KEY + username;
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
            // HEAD 요청 시도
            webClient
                    .head()
                    .uri(originalUrl)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return true;  // HEAD 요청이 성공하면 true 반환
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_ACCEPTABLE) {
                try {
                    webClient
                            .get()
                            .uri(originalUrl)
                            .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                            .header(HttpHeaders.ACCEPT, "*/*")
                            .retrieve()
                            .toBodilessEntity()
                            .block();

                    return true; // GET 요청이 성공하면 true 반환
                } catch (Exception ex) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteOldAnonymousUrls() { // 7일 지난 익명 사용자들의 url 삭제
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<Url> oldAnonymousUrls = urlRepository.findByCreatedByStartingWithAndCreatedAtBefore("ANON_", sevenDaysAgo);

        if (!oldAnonymousUrls.isEmpty()) {
            oldAnonymousUrls.forEach(url -> redisService.deleteSingleData("shortUrl:" + url.getOriginalUrl() + ":" + url.getCreatedBy()));
            urlRepository.deleteAll(oldAnonymousUrls);
            log.debug(oldAnonymousUrls.size() + "개의 익명 URL이 삭제되었습니다.");
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteOldUserUrls() { // 30일이 지난 갱신 안된 url들 삭제
        LocalDateTime daysAgo = LocalDateTime.now().minusDays(30);

        List<Url> oldUrls = urlRepository.findByCreatedAtBefore(daysAgo);

        if (!oldUrls.isEmpty()) {
            oldUrls.forEach(url -> redisService.deleteSingleData("shortUrl:" + url.getOriginalUrl() + ":" + url.getCreatedBy()));
            urlRepository.deleteAll(oldUrls);
            log.debug(oldUrls.size() + "개의 익명 URL이 삭제되었습니다.");
        }
    }

}
