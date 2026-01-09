package org.ostad.assignment_20.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ostad.assignment_20.dto.ShortenUrlRequest;
import org.ostad.assignment_20.dto.ShortenUrlResponse;
import org.ostad.assignment_20.dto.UrlRedirectResponse;
import org.ostad.assignment_20.service.UrlShortenerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenUrlResponse> shortenUrl(@Valid @RequestBody ShortenUrlRequest request) {
        log.info("Received request to shorten URL: {}", request.getOriginalUrl());
        ShortenUrlResponse response = urlShortenerService.shortenUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/r/{shortCode}")
    public ResponseEntity<UrlRedirectResponse> getOriginalUrl(@PathVariable String shortCode) {
        log.info("Received request to get original URL for code: {}", shortCode);
        UrlRedirectResponse response = urlShortenerService.getOriginalUrl(shortCode);
        return ResponseEntity.ok(response);
    }
}