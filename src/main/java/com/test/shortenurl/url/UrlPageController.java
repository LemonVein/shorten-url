package com.test.shortenurl.url;

import com.test.shortenurl.config.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class UrlPageController {

    private final UrlService urlService;

    @GetMapping("/main")
    public String createShortPage(HttpServletRequest request, Model model) { return "mainPage"; }

    @GetMapping("/register")
    public String createRegisterPage() { return "registerPage"; }

    @GetMapping("/login")
    public String createLoginPage() { return "login"; }

    @GetMapping("/connect/{shortenCode}")
    public String redirectOriginalUrl(@PathVariable String shortenCode, Model model) {
        String originalUrl = urlService.getOriginalUrl(shortenCode);

        return "redirect:" + originalUrl;
    }
}
