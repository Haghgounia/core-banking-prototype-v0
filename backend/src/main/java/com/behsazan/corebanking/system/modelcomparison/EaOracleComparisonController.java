package com.behsazan.corebanking.system.modelcomparison;

import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.ComparisonConfiguration;
import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.ComparisonReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/system/database-model-comparison")
public class EaOracleComparisonController {
    private final EaOracleComparisonService service;

    EaOracleComparisonController(EaOracleComparisonService service) {
        this.service = service;
    }

    @GetMapping("/configuration")
    ComparisonConfiguration configuration() {
        return service.configuration();
    }

    @PostMapping(value = "/compare", consumes = "multipart/form-data")
    ComparisonReport compare(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "schema", required = false) String schema,
            @RequestParam(value = "includeRowCounts", defaultValue = "true") boolean includeRowCounts
    ) {
        return service.compare(file, schema, includeRowCounts);
    }
}
