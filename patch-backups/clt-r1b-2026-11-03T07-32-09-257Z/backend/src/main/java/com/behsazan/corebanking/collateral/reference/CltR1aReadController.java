package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CltR1aModel.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** R1A: read endpoints only. POST/PUT/PATCH/DELETE are not exposed and are denied by SecurityFilterChain. */
@RestController
@RequestMapping("/api/v1/collateral/reference")
public final class CltR1aReadController {
    private final CltR1aCatalog catalog;
    private final CltR1aReadRepository repository;
    public CltR1aReadController(CltR1aCatalog catalog, CltR1aReadRepository repository) {
        this.catalog = catalog;
        this.repository = repository;
    }

    @GetMapping("/catalog")
    Catalog catalog() { return catalog.all(); }

    @GetMapping("/readiness")
    Readiness readiness() { return repository.readiness(); }

    @GetMapping("/{table}/descriptor")
    Table descriptor(@PathVariable String table) { return catalog.require(table); }

    @GetMapping("/{table}")
    Rows list(@PathVariable String table,
              @RequestParam(required = false) String text,
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "25") int size) {
        return repository.list(table, text, page, size);
    }

    @GetMapping("/{table}/{primaryKey}")
    Map<String, Object> record(@PathVariable String table, @PathVariable String primaryKey) {
        return repository.find(table, primaryKey);
    }
}
