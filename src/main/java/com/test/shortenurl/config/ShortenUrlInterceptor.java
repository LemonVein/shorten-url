package com.test.shortenurl.config;

import com.test.shortenurl.url.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ShortenUrlInterceptor implements HandlerInterceptor {

    private final UrlService urlService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI().substring(1); // "/" 제거

        // 특정 경로(API, 인증 관련 등)는 무시
        if (path.startsWith("api") || path.startsWith("auth") || path.startsWith("static") || path.startsWith("js") || path.startsWith("css")) {
            return true; // API, 인증 관련 요청은 건너뛴다.
        }

        // 기존 페이지들과 충돌 방지 (이외의 값이면 단축 URL인지 확인)
        if (!List.of("main", "register", "login").contains(path)) {
            String originalUrl = urlService.getOriginalUrl(path);
            if (originalUrl != null) {
                response.sendRedirect(originalUrl);
                return false;
            }
        }

        return true;
    }
}