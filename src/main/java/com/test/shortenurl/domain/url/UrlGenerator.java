package com.test.shortenurl.domain.url;

import com.test.shortenurl.common.ShortenUrlGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UrlGenerator {

    private final UrlRepository urlRepository;

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;


    public String generateUniqueShortUrl(String originalUrl, Long id) {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String shortUrl = generateShortUrl(originalUrl, id);
            if (urlRepository.existsByShortUrl(shortUrl).isPresent()) {
                return shortUrl;
            }
        }
        throw new ShortenUrlGenerationException("Failed to generate a unique short URL after " + maxAttempts + " attempts");
    }

    private String generateShortUrl(String originalUrl, Long id) {
        String uniqueData = originalUrl + id;

        return encodeBase62(uniqueData).substring(0, 7);
    }

    private String encodeBase62(String input) {
        BigInteger number = new BigInteger(1, input.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        while (number.compareTo(BigInteger.ZERO) > 0) {
            sb.append(BASE62.charAt(number.mod(BigInteger.valueOf(BASE)).intValue()));
            number = number.divide(BigInteger.valueOf(BASE));
        }
        return sb.reverse().toString();
    }
}
