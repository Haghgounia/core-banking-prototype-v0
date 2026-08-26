package com.behsazan.corebanking.calendar.datasetimport.web;

import com.behsazan.corebanking.calendar.datasetimport.application.CalendarDatasetImportService;
import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.DatasetImportResult;
import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.DatasetStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/calendar/dataset-import")
public class CalendarDatasetImportController {
    private final CalendarDatasetImportService service;

    public CalendarDatasetImportController(CalendarDatasetImportService service) {
        this.service = service;
    }

    @GetMapping("/status")
    DatasetStatus status() {
        return service.status();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    DatasetImportResult importDataset(@RequestPart("calendarDayFile") MultipartFile calendarDayFile,
                                      @RequestPart("calendarDateFile") MultipartFile calendarDateFile) {
        return service.importDataset(calendarDayFile, calendarDateFile);
    }
}
