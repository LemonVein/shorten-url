package com.test.shortenurl.domain.url;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortUrlAndDeletedFalse(String shortUrl);
    Optional<Url> findByOriginalUrlAndCreatedByAndDeletedFalse(String originalUrl, String createdBy);
    Optional<Url> existsByShortUrl(String shortenUrl);

    List<Url> findByCreatedByStartingWithAndCreatedAtBefore(String createdBy, LocalDateTime createdAt);
    List<Url> findByCreatedAtBefore(LocalDateTime createdAt);
    List<Url> findByCreatedByAndDeletedFalse(String username);
}
