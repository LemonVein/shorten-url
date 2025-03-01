package com.test.shortenurl.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.test.shortenurl.domain.url.Url;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class UrlApiController {
    private final UrlService urlService;

    @PostMapping("/shorten_url")
    public String makeShortenUrl(@RequestParam String originalUrl, HttpServletRequest request){
        String shortenUrl = urlService.shortenUrl(originalUrl, request);

        return shortenUrl;
    }

    @GetMapping("/my-urls")
    public List<Url> getUserUrls(HttpServletRequest request) throws JsonProcessingException {
        return urlService.getUrls(request);
    }

    @GetMapping("/status")
    public Map<String, Object> getAuthStatus(HttpServletRequest request) {
        Map<String, Object> response = urlService.checkStatus(request);

        return response;
    }

    @GetMapping("/{shortenCode}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String shortenCode, HttpServletRequest request) {
        boolean result = urlService.deleteShortUrl(shortenCode, request);
        if (result) {
            return ResponseEntity.noContent().build();
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }
}
