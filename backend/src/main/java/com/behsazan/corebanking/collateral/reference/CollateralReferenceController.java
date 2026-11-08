package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** R1A: read endpoints only. POST/PUT/PATCH/DELETE are not exposed and are denied by SecurityFilterChain. */
@RestController
@RequestMapping("/api/v1/collateral/reference")
public final class CollateralReferenceController {
    private final CollateralReferenceCatalog catalog;
    private final CollateralReferenceRepository repository;
    public CollateralReferenceController(CollateralReferenceCatalog catalog, CollateralReferenceRepository repository) {
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
              @RequestParam(defaultValue = "25") int size,
              @RequestParam(required = false) String status,
              @RequestParam(required = false) String sort,
              @RequestParam(required = false) String direction,
              @RequestParam(required = false) String filterField,
              @RequestParam(required = false) String filterValue) {
        return repository.list(table, text, page, size, status, sort, direction, filterField, filterValue);
    }

    @GetMapping("/{table}/{primaryKey}")
    Map<String, Object> record(@PathVariable String table, @PathVariable String primaryKey) {
        return repository.find(table, primaryKey);
    }
}
