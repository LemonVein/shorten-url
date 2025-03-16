package com.test.shortenurl.domain.url;

import com.test.shortenurl.exception.ShortenUrlGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlGenerator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UrlRepository urlRepository;

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;


    public String generateUniqueShortUrl(Long id) {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String shortUrl = generateShortUrl(id);
            if (urlRepository.existsByShortUrl(shortUrl).isPresent()) {
                return shortUrl;
            }
        }
        throw new ShortenUrlGenerationException("Failed to generate a unique short URL after " + maxAttempts + " attempts");
    }

    private String generateShortUrl(Long id) {
        Long offset = getShardId(id);

        Long uniqueData = offset;

        return encodeBase62(uniqueData).substring(0, 7);
    }

    private String encodeBase62(long input) {
        StringBuilder sb = new StringBuilder();
        while (input > 0) {
            sb.append(BASE62.charAt((int) (input % BASE)));
            input /= BASE;
        }
        return sb.reverse().toString();
    }

    private String determineShardTable(Long id) {
        long shardIndex = (id % 5) + 1;
        return "id_shard_" + shardIndex;
    }

    private Long getShardId(Long id) {
        String tableName = determineShardTable(id);

        jdbcTemplate.update("INSERT INTO " + tableName + " (id) VALUES (NULL)");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
