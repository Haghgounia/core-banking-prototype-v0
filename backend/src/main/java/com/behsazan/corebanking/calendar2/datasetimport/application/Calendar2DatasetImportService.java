package com.behsazan.corebanking.calendar2.datasetimport.application;

import com.behsazan.corebanking.calendar2.datasetimport.domain.Calendar2DatasetImportModels.DatasetImportResult;
import com.behsazan.corebanking.calendar2.datasetimport.oracle.Calendar2DatasetImportRepository;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class Calendar2DatasetImportService {
    private static final Logger log = LoggerFactory.getLogger(Calendar2DatasetImportService.class);
    private static final long MAX_ZIP_SIZE = 64L * 1024 * 1024;
    private static final long MAX_EXPANDED_SIZE = 128L * 1024 * 1024;
    private static final List<String> EXPECTED_FILES = List.of(
            "01_calendar_system.csv", "02_source_authority.csv", "03_dataset_version.csv", "04_calendar_variant.csv",
            "05_calendar_month.csv", "06_weekday.csv", "07_canonical_day.csv", "08_calendar_date.csv", "09_event_type.csv",
            "10_event.csv", "11_event_occurrence.csv", "12_business_calendar.csv", "13_business_calendar_day.csv",
            "14_validation_run.csv", "15_validation_result.csv"
    );

    private final Calendar2DatasetImportRepository repository;

    public Calendar2DatasetImportService(Calendar2DatasetImportRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DatasetImportResult importPackage(MultipartFile packageFile) {
        requireUpload(packageFile);
        long started = System.currentTimeMillis();
        Path temp = null;
        try {
            temp = Files.createTempDirectory("cal2-import-");
            Map<String, Path> files = extractExpectedCsv(packageFile, temp);
            LinkedHashMap<String, Long> rows = new LinkedHashMap<>();

            log.info("CAL2 package JDBC import started: file={} bytes={} schema={}", safeName(packageFile), packageFile.getSize(), repository.schemaName());

            rows.put("CALENDAR_SYSTEM", repository.loadCalendarSystem(files.get("01_calendar_system.csv")));
            rows.put("SOURCE_AUTHORITY", repository.loadSourceAuthority(files.get("02_source_authority.csv")));
            rows.put("DATASET_VERSION", repository.loadDatasetVersion(files.get("03_dataset_version.csv")));
            rows.put("CALENDAR_VARIANT", repository.loadCalendarVariant(files.get("04_calendar_variant.csv")));
            rows.put("CALENDAR_MONTH", repository.loadCalendarMonth(files.get("05_calendar_month.csv")));
            rows.put("WEEKDAY", repository.loadWeekday(files.get("06_weekday.csv")));
            rows.put("CANONICAL_DAY", repository.loadCanonicalDay(files.get("07_canonical_day.csv")));
            rows.put("CALENDAR_DATE", repository.loadCalendarDate(files.get("08_calendar_date.csv")));
            rows.put("EVENT_TYPE", repository.loadEventType(files.get("09_event_type.csv")));
            rows.put("EVENT", repository.loadEvent(files.get("10_event.csv")));
            rows.put("EVENT_OCCURRENCE", repository.loadEventOccurrence(files.get("11_event_occurrence.csv")));
            rows.put("BUSINESS_CALENDAR", repository.loadBusinessCalendar(files.get("12_business_calendar.csv")));
            rows.put("BUSINESS_CALENDAR_DAY", repository.loadBusinessCalendarDay(files.get("13_business_calendar_day.csv")));
            rows.put("VALIDATION_RUN", repository.loadValidationRun(files.get("14_validation_run.csv")));
            rows.put("VALIDATION_RESULT", repository.loadValidationResult(files.get("15_validation_result.csv")));

            long total = rows.values().stream().mapToLong(Long::longValue).sum();
            long elapsed = System.currentTimeMillis() - started;
            log.info("CAL2 package JDBC insert completed; transaction will commit: totalRows={} elapsedMs={}", total, elapsed);
            return new DatasetImportResult(repository.schemaName(), safeName(packageFile), rows, total, elapsed);
        } catch (IOException exception) {
            throw new UncheckedIOException("بازکردن بسته ZIP تقویم CAL2 ناموفق بود.", exception);
        } finally {
            if (temp != null) deleteTree(temp);
        }
    }

    private static Map<String, Path> extractExpectedCsv(MultipartFile file, Path temp) throws IOException {
        LinkedHashMap<String, Path> found = new LinkedHashMap<>();
        long expanded = 0;
        try (InputStream input = file.getInputStream(); ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String normalized = entry.getName().replace('\\', '/');
                String baseName = normalized.substring(normalized.lastIndexOf('/') + 1);
                if (!EXPECTED_FILES.contains(baseName)) continue;
                if (found.containsKey(baseName)) throw validation("فایل تکراری در بسته ZIP وجود دارد: " + baseName);
                Path target = temp.resolve(baseName).normalize();
                if (!target.startsWith(temp)) throw validation("مسیر نامعتبر در بسته ZIP شناسایی شد.");
                long copied = Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                expanded += copied;
                if (expanded > MAX_EXPANDED_SIZE) throw validation("حجم بازشده بسته ZIP بیش از حد مجاز 128MB است.");
                found.put(baseName, target);
            }
        }
        List<String> missing = EXPECTED_FILES.stream().filter(name -> !found.containsKey(name)).toList();
        if (!missing.isEmpty()) throw validation("بسته Import ناقص است. فایل‌های مورد انتظار یافت نشد: " + String.join(", ", missing));
        return found;
    }

    private static void requireUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ReferenceValidationException("بسته ZIP برای Import ارسال نشده است.", Map.of("packageFile", "فایل ZIP الزامی است."));
        if (file.getSize() > MAX_ZIP_SIZE) throw new ReferenceValidationException("حجم فایل ZIP بیش از حد مجاز است.", Map.of("packageFile", "حداکثر حجم 64MB است."));
        String name = safeName(file).toLowerCase();
        if (!name.endsWith(".zip")) throw new ReferenceValidationException("فرمت فایل Import باید ZIP باشد.", Map.of("packageFile", "فایل با پسوند .zip انتخاب کنید."));
    }

    private static ReferenceValidationException validation(String detail) {
        return new ReferenceValidationException("ساختار بسته CAL2 معتبر نیست.", Map.of("packageFile", detail));
    }

    private static String safeName(MultipartFile file) {
        String name = file == null ? null : file.getOriginalFilename();
        return name == null || name.isBlank() ? "calendar2-package.zip" : name.replace('\\', '/').replaceAll("^.*/", "");
    }

    private static void deleteTree(Path root) {
        try (var paths = Files.walk(root)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
