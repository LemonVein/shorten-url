package com.test.shortenurl.url;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


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

    @GetMapping("/{shortenCode}")
    public String redirectOriginalUrl(@PathVariable String shortenCode, Model model) {
        String originalUrl = urlService.getOriginalUrl(shortenCode);

        return "redirect:" + originalUrl;
    }
}
