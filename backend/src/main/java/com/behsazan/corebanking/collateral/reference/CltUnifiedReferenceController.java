package com.behsazan.corebanking.collateral.reference;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Separate local CRUD contract; legacy maker/checker endpoints and their rules stay unchanged. */
@RestController
@RequestMapping("/api/v1/collateral/reference")
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
public class CltUnifiedReferenceController {
    private final CltUnifiedReferenceRepository repository;
    public CltUnifiedReferenceController(CltUnifiedReferenceRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/unified/status")
    Map<String, Object> status() {
        return Map.of("enabled", true, "writeMode", "ORACLE_APPLICATION_DATASOURCE",
                "localOnly", true, "referenceTables", 17, "financialMutationsEnabled", false);
    }

    @PostMapping("/{table}")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@PathVariable String table, @RequestBody Map<String, Object> values) {
        return repository.create(table, values);
    }

    @PutMapping("/{table}/{id}")
    Map<String, Object> update(@PathVariable String table, @PathVariable String id,
            @RequestParam long expectedVersion, @RequestBody Map<String, Object> values) {
        return repository.update(table, id, expectedVersion, values);
    }

    @DeleteMapping("/{table}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String table, @PathVariable String id,
            @RequestParam long expectedVersion) {
        repository.delete(table, id, expectedVersion);
    }
}
