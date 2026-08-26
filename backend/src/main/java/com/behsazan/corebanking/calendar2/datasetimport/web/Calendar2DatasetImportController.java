package com.behsazan.corebanking.calendar2.datasetimport.web;

import com.behsazan.corebanking.calendar2.datasetimport.application.Calendar2DatasetImportService;
import com.behsazan.corebanking.calendar2.datasetimport.domain.Calendar2DatasetImportModels.DatasetImportResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/calendar2/dataset")
public class Calendar2DatasetImportController {
    private final Calendar2DatasetImportService service;

    public Calendar2DatasetImportController(Calendar2DatasetImportService service) { this.service = service; }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    DatasetImportResult importPackage(@RequestPart("packageFile") MultipartFile packageFile) {
        return service.importPackage(packageFile);
    }
}
