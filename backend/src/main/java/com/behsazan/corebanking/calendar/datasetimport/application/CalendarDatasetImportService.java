package com.behsazan.corebanking.calendar.datasetimport.application;

import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.DatasetImportResult;
import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.DatasetStatus;
import com.behsazan.corebanking.calendar.datasetimport.oracle.CalendarDatasetImportRepository;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CalendarDatasetImportService {
    private static final Logger log = LoggerFactory.getLogger(CalendarDatasetImportService.class);
    private static final long MAX_FILE_SIZE = 64L * 1024 * 1024;

    private final CalendarDatasetImportRepository repository;

    public CalendarDatasetImportService(CalendarDatasetImportRepository repository) {
        this.repository = repository;
    }

    /** Informational endpoint only. It is not used to permit/block import. */
    public DatasetStatus status() {
        return repository.status();
    }

    /**
     * Raw JDBC import mode requested for the calendar dataset.
     * No dataset/content validation, row-count verification, seed check, empty-table check,
     * hash comparison, calendar-system verification, or post-insert validation is performed.
     * The transaction is committed by Spring only after both INSERT streams finish successfully.
     */
    @Transactional
    public DatasetImportResult importDataset(MultipartFile calendarDayFile, MultipartFile calendarDateFile) {
        requireUpload(calendarDayFile, "calendarDayFile");
        requireUpload(calendarDateFile, "calendarDateFile");

        long started = System.currentTimeMillis();
        log.info("Calendar dataset raw JDBC import started: dayFile={} dayBytes={} dateFile={} dateBytes={}",
                safeName(calendarDayFile), calendarDayFile.getSize(), safeName(calendarDateFile), calendarDateFile.getSize());

        long dayRows = repository.loadCalendarDays(calendarDayFile);
        long dateRows = repository.loadCalendarDates(calendarDateFile);
        long elapsed = System.currentTimeMillis() - started;

        log.info("Calendar dataset raw JDBC insert completed; transaction will commit now: dayInserted={} dateInserted={} elapsedMs={}",
                dayRows, dateRows, elapsed);

        return new DatasetImportResult(
                repository.schemaName(), safeName(calendarDayFile), safeName(calendarDateFile),
                dayRows, dateRows, elapsed
        );
    }

    private static void requireUpload(MultipartFile file, String field) {
        if (file == null) {
            throw new ReferenceValidationException("فایل برای Import ارسال نشده است.", Map.of(field, "فایل الزامی است."));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ReferenceValidationException("حجم فایل بیش از حد مجاز فنی Upload است.", Map.of(field, "حداکثر حجم هر فایل 64MB است."));
        }
    }

    private static String safeName(MultipartFile file) {
        String name = file == null ? null : file.getOriginalFilename();
        if (name == null || name.isBlank()) return "calendar.csv";
        String normalized = name.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }
}
