package com.test.shortenurl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ShortenUrlInterceptor shortenUrlInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(shortenUrlInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/**", "/auth/**", "/static/**", "/login/**");
    }
}
