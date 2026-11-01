package com.behsazan.corebanking.organization.contact;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * R5-C4B: the former V2 migration-only API is retired (HTTP 410 Gone).
 * Operational contact readiness is available at /api/v1/organization/unit-contacts/readiness.
 * This class must not query the retired V2 archive or compare mutable OUCP rows
 * with an immutable historical migration snapshot.
 */
@RestController
@RequestMapping("/api/v1/organization/unit-contacts/migration")
public class OrganizationContactMigrationReadinessController {
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, String>> readiness() {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                "code", "ORG_V2_MIGRATION_ENDPOINT_RETIRED",
                "status", "RETIRED",
                "operationalReadinessEndpoint", "/api/v1/organization/unit-contacts/readiness"
        ));
    }
}
