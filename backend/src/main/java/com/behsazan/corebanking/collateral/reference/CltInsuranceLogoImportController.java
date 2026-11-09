package com.behsazan.corebanking.collateral.reference;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** An explicit local-only import endpoint; /logo-import matches the existing POST allowlist. */
@RestController
@RequestMapping("/api/v1/collateral/reference/logo-import")
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
public class CltInsuranceLogoImportController {
    private final CltInsuranceLogoImportService service;
    public CltInsuranceLogoImportController(CltInsuranceLogoImportService service) { this.service = service; }

    @GetMapping("/status")
    public CltInsuranceLogoImportService.Status status() { return service.status(); }

    @PostMapping
    public CltInsuranceLogoImportService.ImportResult importLogos() { return service.importOnce(); }

    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> logo(@PathVariable String id) {
        if (!id.matches("[1-9][0-9]{0,18}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLT_LOGO_INVALID_ID");
        byte[] image = service.image(Long.parseLong(id));
        String contentType = CltInsuranceImagePolicy.checkedContentType(image);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(contentType)).body(image);
    }
}
