package com.behsazan.corebanking.posting.readiness;

import com.behsazan.corebanking.posting.readiness.PostingReadinessModels.ReadinessReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Disabled unless explicitly opted in. A secret header is required even then.
 * Admin/Test-only metadata endpoint; NEVER exposes posting/commit methods.
 */
@RestController
@RequestMapping("/api/v1/deal-posting")
@ConditionalOnProperty(prefix = "core-banking.deal-posting.readiness", name = "enabled", havingValue = "true")
public class PostingReadinessController {
    private final PostingReadinessService service;
    private final byte[] requiredToken;

    public PostingReadinessController(
            PostingReadinessService service,
            @Value("${core-banking.deal-posting.readiness.token:}") String token) {
        this.service = service;
        this.requiredToken = token.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/readiness")
    public ResponseEntity<ReadinessReport> inspect(
            @RequestHeader(value = "X-Posting-Readiness-Token", required = false) String suppliedToken) {
        if (requiredToken.length == 0 || suppliedToken == null
                || !MessageDigest.isEqual(requiredToken, suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(service.inspect());
    }
}
