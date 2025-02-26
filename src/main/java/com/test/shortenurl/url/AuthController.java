package com.test.shortenurl.url;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final UrlService urlService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, String password) {
        return urlService.tryLogin(username, password);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username, String password) {
        return urlService.registerUser(username, password);
    }
}
