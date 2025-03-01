package com.test.shortenurl.domain.url;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortUrl(String shortUrl);
    Optional<Url> findByShortUrlAndDeletedFalse(String shortUrl);
    Optional<Url> findByOriginalUrlAndCreatedBy(String originalUrl, String createdBy);
    Optional<Url> existsByShortUrl(String shortenUrl);

    List<Url> findByCreatedBy(String username);
    List<Url> findByCreatedByAndDeletedFalse(String username);
}
