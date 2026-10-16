package com.behsazan.corebanking.posting.configuration;

import com.behsazan.corebanking.posting.configuration.PostingConfigurationModels.ErrorReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * R1-D2 diagnostic endpoint. HTTP POST denotes a complex preview request body,
 * NOT a state change. The service contains only SELECT queries.
 * Reuses the already installed R1-D1 opt-in flag/token; no second secret.
 */
@RestController
@RequestMapping("/api/v1/deal-posting/configuration")
@ConditionalOnProperty(prefix = "core-banking.deal-posting.readiness", name = "enabled", havingValue = "true")
public class PostingConfigurationPreviewController {
    private final PostingConfigurationPreviewService service;
    private final byte[] requiredToken;

    public PostingConfigurationPreviewController(PostingConfigurationPreviewService service,
            @Value("${core-banking.deal-posting.readiness.token:}") String token) {
        this.service = service;
        requiredToken = token.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestHeader(value = "X-Posting-Readiness-Token", required = false) String suppliedToken) {
        if (!authorized(suppliedToken)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(service.status());
    }

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestHeader(value = "X-Posting-Readiness-Token", required = false) String suppliedToken,
                                      @RequestBody PostingConfigurationModels.PreviewRequest request) {
        if (!authorized(suppliedToken)) return ResponseEntity.status(403).build();
        if (request == null) return ResponseEntity.badRequest().body(
                new ErrorReport("R1-D2", "BAD_REQUEST", "Preview body is required"));
        try {
            return ResponseEntity.ok(service.preview(request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new ErrorReport("R1-D2", "BAD_REQUEST",
                    "Invalid diagnostic scope, timestamp, or unsupported first-slice event"));
        }
    }

    private boolean authorized(String suppliedToken) {
        return requiredToken.length > 0 && suppliedToken != null &&
                MessageDigest.isEqual(requiredToken, suppliedToken.getBytes(StandardCharsets.UTF_8));
    }
}
