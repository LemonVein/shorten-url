package com.test.shortenurl.url;

import com.test.shortenurl.config.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
