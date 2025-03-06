package com.test.shortenurl.domain.url;

import com.test.shortenurl.common.ShortenUrlGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UrlGenerator {

    private final UrlRepository urlRepository;

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;


    public String generateUniqueShortUrl(String originalUrl) {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String shortUrl = generateShortUrl(originalUrl);
            if (urlRepository.existsByShortUrl(shortUrl).isPresent()) {
                return shortUrl;
            }
        }
        throw new ShortenUrlGenerationException("Failed to generate a unique short URL after " + maxAttempts + " attempts");
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
}
