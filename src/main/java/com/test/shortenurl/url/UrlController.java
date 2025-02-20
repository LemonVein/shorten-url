package com.test.shortenurl.url;

import com.test.shortenurl.config.JwtTokenProvider;
import com.test.shortenurl.domain.url.Url;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class UrlController {

    private final UrlService urlService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

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

    @PostMapping("/auth/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestParam String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            String token = jwtTokenProvider.generateToken(authentication);

            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @PostMapping("/auth/register")
    @ResponseBody
    public ResponseEntity<?> register(@RequestParam String username, String password) {
        return urlService.registerUser(username, password);
    }

    @PostMapping("/shorten_url")
    @ResponseBody
    public String makeShortenUrl(@RequestParam String originalUrl, HttpServletRequest request){
        String shortenUrl = urlService.shortenUrl(originalUrl, request);

        return shortenUrl;
    }

    @GetMapping("/my-urls")
    @ResponseBody
    public List<Url> getUserUrls(HttpServletRequest request) {
        return urlService.getUrls(request);
    }

    @GetMapping("/status")
    @ResponseBody
    public Map<String, Object> getAuthStatus(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String username = urlService.getCurrentUsername(request);

        boolean isAuthenticated = username != null && urlService.isValidUsername(username);

        response.put("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            response.put("username", username);
        }

        return response;
    }
}
