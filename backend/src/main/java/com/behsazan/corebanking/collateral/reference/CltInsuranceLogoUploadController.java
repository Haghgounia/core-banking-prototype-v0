package com.behsazan.corebanking.collateral.reference;

import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Same-origin, loopback-only CLT API; covered by the existing single-level POST allowlist. */
@RestController
@RequestMapping("/api/v1/collateral/reference/logo-upload")
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
public class CltInsuranceLogoUploadController {
    private final CltInsuranceLogoUploadService service;

    public CltInsuranceLogoUploadController(CltInsuranceLogoUploadService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CltInsuranceLogoUploadService.Receipt upload(
            @RequestParam("companyId") String companyId,
            @RequestParam("expectedVersion") long expectedVersion,
            @RequestParam("confirmed") boolean confirmed,
            @RequestParam("file") MultipartFile file) {
        if (!confirmed)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLT_LOGO_OWNERSHIP_NOT_CONFIRMED");
        if (file.isEmpty() || file.getSize() > CltInsuranceImagePolicy.MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CLT_LOGO_INVALID_SIZE");
        try { return service.upload(companyId, expectedVersion, file.getBytes()); }
        catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CLT_LOGO_CANNOT_READ_FILE");
        }
    }
}
