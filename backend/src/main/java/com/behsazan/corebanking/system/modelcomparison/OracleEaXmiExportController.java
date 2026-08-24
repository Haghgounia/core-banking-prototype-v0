package com.behsazan.corebanking.system.modelcomparison;

import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.ComparisonConfiguration;
import com.behsazan.corebanking.system.modelcomparison.OracleEaXmiExportModels.ExportPreview;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/system/oracle-ea-xmi-export")
public class OracleEaXmiExportController {
    private final OracleEaXmiExportService service;

    OracleEaXmiExportController(OracleEaXmiExportService service) {
        this.service = service;
    }

    @GetMapping("/configuration")
    ComparisonConfiguration configuration() {
        return service.configuration();
    }

    @GetMapping("/preview")
    ExportPreview preview(
            @RequestParam(value = "schema", required = false) String schema,
            @RequestParam(value = "tablePattern", defaultValue = "%") String tablePattern,
            @RequestParam(value = "includeForeignKeys", defaultValue = "true") boolean includeForeignKeys,
            @RequestParam(value = "includeIndexes", defaultValue = "true") boolean includeIndexes,
            @RequestParam(value = "includeChecks", defaultValue = "true") boolean includeChecks,
            @RequestParam(value = "includeComments", defaultValue = "true") boolean includeComments,
            @RequestParam(value = "includeExternalReferences", defaultValue = "true") boolean includeExternalReferences
    ) {
        return service.preview(options(schema, tablePattern, includeForeignKeys, includeIndexes, includeChecks, includeComments, includeExternalReferences));
    }

    @GetMapping(value = "/export", produces = {"application/xml", "application/octet-stream"})
    ResponseEntity<byte[]> export(
            @RequestParam(value = "schema", required = false) String schema,
            @RequestParam(value = "tablePattern", defaultValue = "%") String tablePattern,
            @RequestParam(value = "includeForeignKeys", defaultValue = "true") boolean includeForeignKeys,
            @RequestParam(value = "includeIndexes", defaultValue = "true") boolean includeIndexes,
            @RequestParam(value = "includeChecks", defaultValue = "true") boolean includeChecks,
            @RequestParam(value = "includeComments", defaultValue = "true") boolean includeComments,
            @RequestParam(value = "includeExternalReferences", defaultValue = "true") boolean includeExternalReferences
    ) {
        OracleEaXmiExportService.ExportedXmi exported = service.export(options(
                schema, tablePattern, includeForeignKeys, includeIndexes, includeChecks, includeComments, includeExternalReferences
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(exported.fileName(), StandardCharsets.UTF_8)
                .build());
        headers.set("X-EA-XMI-Table-Count", Integer.toString(exported.tableCount()));
        headers.set("X-EA-XMI-FK-Count", Integer.toString(exported.foreignKeyCount()));
        return ResponseEntity.ok().headers(headers).body(exported.content());
    }

    private static OracleEaExportOptions options(
            String schema,
            String tablePattern,
            boolean includeForeignKeys,
            boolean includeIndexes,
            boolean includeChecks,
            boolean includeComments,
            boolean includeExternalReferences
    ) {
        return new OracleEaExportOptions(schema, tablePattern, includeForeignKeys, includeIndexes, includeChecks, includeComments, includeExternalReferences);
    }
}
