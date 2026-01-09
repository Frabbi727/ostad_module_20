package org.ostad.assignment_20.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ostad.assignment_20.dto.ShortenUrlRequest;
import org.ostad.assignment_20.dto.ShortenUrlResponse;
import org.ostad.assignment_20.dto.UrlRedirectResponse;
import org.ostad.assignment_20.entity.ShortUrl;
import org.ostad.assignment_20.exception.InvalidValidityException;
import org.ostad.assignment_20.exception.UrlExpiredException;
import org.ostad.assignment_20.exception.UrlNotFoundException;
import org.ostad.assignment_20.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final ShortUrlRepository shortUrlRepository;

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final int MAX_RETRY_ATTEMPTS = 5;

    @Transactional
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {
        validateValidity(request.getValidity());

        Optional<ShortUrl> existingUrl = shortUrlRepository.findByOriginalUrl(request.getOriginalUrl());
        if (existingUrl.isPresent()) {
            log.info("URL already exists: {}", request.getOriginalUrl());
            return buildResponse(existingUrl.get());
        }

        String shortCode = generateUniqueShortCode(request.getOriginalUrl());

        ShortUrl shortUrl = ShortUrl.builder()
                .originalUrl(request.getOriginalUrl())
                .code(shortCode)
                .expiresAt(request.getValidity())
                .build();

        shortUrl = shortUrlRepository.save(shortUrl);
        log.info("Created short URL: {} for {}", shortCode, request.getOriginalUrl());

        return buildResponse(shortUrl);
    }

    @Transactional(readOnly = true)
    public UrlRedirectResponse getOriginalUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found"));

        if (isExpired(shortUrl.getExpiresAt())) {
            throw new UrlExpiredException("URL expired");
        }

        return UrlRedirectResponse.builder()
                .originalUrl(shortUrl.getOriginalUrl())
                .expiresAt(shortUrl.getExpiresAt())
                .build();
    }

    private void validateValidity(LocalDateTime validity) {
        if (validity.isBefore(LocalDateTime.now())) {
            throw new InvalidValidityException("Validity date must be in the future");
        }
    }

    private boolean isExpired(LocalDateTime expiresAt) {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    private String generateUniqueShortCode(String originalUrl) {
        String baseCode = generateShortCode(originalUrl);
        String shortCode = baseCode;
        int attempt = 0;

        while (shortUrlRepository.existsByCode(shortCode) && attempt < MAX_RETRY_ATTEMPTS) {
            shortCode = generateShortCode(originalUrl + System.nanoTime() + attempt);
            attempt++;
        }

        if (shortUrlRepository.existsByCode(shortCode)) {
            throw new RuntimeException("Failed to generate unique short code after " + MAX_RETRY_ATTEMPTS + " attempts");
        }

        return shortCode;
    }

    private String generateShortCode(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes());

            long hashValue = 0;
            for (int i = 0; i < 8; i++) {
                hashValue = (hashValue << 8) | (hash[i] & 0xFF);
            }

            hashValue = Math.abs(hashValue);

            return encodeBase62(hashValue, SHORT_CODE_LENGTH);

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            return encodeBase62(url.hashCode(), SHORT_CODE_LENGTH);
        }
    }

    private String encodeBase62(long value, int length) {
        StringBuilder encoded = new StringBuilder();

        while (value > 0 && encoded.length() < length) {
            int remainder = (int) (value % 62);
            encoded.insert(0, BASE62_CHARS.charAt(remainder));
            value /= 62;
        }

        while (encoded.length() < length) {
            encoded.insert(0, '0');
        }

        return encoded.substring(0, length);
    }

    private ShortenUrlResponse buildResponse(ShortUrl shortUrl) {
        String shortUrlString = String.format("http://localhost:%s/r/%s", serverPort, shortUrl.getCode());

        return ShortenUrlResponse.builder()
                .shortUrl(shortUrlString)
                .originalUrl(shortUrl.getOriginalUrl())
                .expiresAt(shortUrl.getExpiresAt())
                .build();
    }
}