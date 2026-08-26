package com.behsazan.corebanking.calendar.datasetimport.application;

import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.DatasetImportResult;
import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.DatasetStatus;
import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.DatasetVerification;
import com.behsazan.corebanking.calendar.datasetimport.oracle.CalendarDatasetImportRepository;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CalendarDatasetImportService {
    private static final Logger log = LoggerFactory.getLogger(CalendarDatasetImportService.class);
    private static final long MAX_FILE_SIZE = 64L * 1024 * 1024;
    private static final String V100_DAY_SHA256 = "cedd41829fdaea732594bffe9ceb5414a95cc0c55597112563239681a279e3e7";
    private static final String V100_DATE_SHA256 = "9b7b20c7a2a0f9704b296941c43cfd13c595b9e66f8189ff32f568facf3d26ff";

    private final CalendarDatasetImportRepository repository;

    public CalendarDatasetImportService(CalendarDatasetImportRepository repository) {
        this.repository = repository;
    }

    public DatasetStatus status() {
        return repository.status();
    }

    @Transactional
    public DatasetImportResult importDataset(MultipartFile calendarDayFile, MultipartFile calendarDateFile) {
        validateFile(calendarDayFile, "calendarDayFile", "calendar_day.csv");
        validateFile(calendarDateFile, "calendarDateFile", "calendar_date.csv");

        long started = System.currentTimeMillis();
        String dayHash = sha256(calendarDayFile, "calendarDayFile");
        String dateHash = sha256(calendarDateFile, "calendarDateFile");

        repository.lockDatasetTables();
        DatasetStatus before = repository.status();
        if (!before.seedReady()) {
            throw validation("داده‌های پایه تقویم قبل از Import کامل نیستند.", "seed",
                    "ابتدا Seedهای CALENDAR_SYSTEM، CALENDAR_ALGORITHM، WEEKDAY و CALENDAR_MONTH را تکمیل کنید: "
                            + String.join("، ", before.missingSeedItems()));
        }
        if (!before.datasetEmpty()) {
            throw validation("برای جلوگیری از Import تکراری، Dataset مقصد باید خالی باشد.", "dataset",
                    "CAL.CALENDAR_DAY=" + before.calendarDayRows() + " و CAL.CALENDAR_DATE=" + before.calendarDateRows()
                            + ". این عملیات عمداً روی جدول دارای داده Append نمی‌کند.");
        }

        log.info("Calendar dataset JDBC import started: dayFile={} dayBytes={} dateFile={} dateBytes={}",
                safeName(calendarDayFile), calendarDayFile.getSize(), safeName(calendarDateFile), calendarDateFile.getSize());

        long dayRows;
        long dateRows;
        try {
            dayRows = repository.loadCalendarDays(calendarDayFile);
            if (dayRows == 0) throw validation("فایل calendar_day.csv فاقد رکورد داده است.", "calendarDayFile", "حداقل یک رکورد داده لازم است.");
            dateRows = repository.loadCalendarDates(calendarDateFile);
        } catch (IllegalArgumentException exception) {
            throw validation("ساختار یا محتوای CSV معتبر نیست.", "csv", exception.getMessage());
        } catch (UncheckedIOException exception) {
            throw validation("خواندن فایل CSV ناموفق بود.", "csv", exception.getMessage());
        }

        if (dateRows == 0) {
            throw validation("فایل calendar_date.csv فاقد رکورد داده است.", "calendarDateFile", "حداقل یک رکورد داده لازم است.");
        }
        if (dateRows != dayRows * 3L) {
            throw validation("تعداد نمایش‌های سه‌تقویمی با تعداد روزها سازگار نیست.", "calendarDateFile",
                    "برای هر DAY_ID دقیقاً سه رکورد لازم است. calendar_day=" + dayRows + "، calendar_date=" + dateRows);
        }

        long dbDayRows = repository.countDays();
        long dbDateRows = repository.countDates();
        if (dbDayRows != dayRows || dbDateRows != dateRows) {
            throw validation("تعداد رکوردهای ثبت‌شده با فایل‌ها سازگار نیست.", "database",
                    "خوانده‌شده: " + dayRows + "/" + dateRows + "؛ ثبت‌شده: " + dbDayRows + "/" + dbDateRows);
        }

        DatasetVerification verification = repository.verify();
        validateVerification(dayRows, verification);
        String[] range = repository.canonicalRange();
        long elapsed = System.currentTimeMillis() - started;
        boolean knownDataset = V100_DAY_SHA256.equalsIgnoreCase(dayHash) && V100_DATE_SHA256.equalsIgnoreCase(dateHash);

        log.info("Calendar dataset JDBC import completed: dayRows={} dateRows={} range={}..{} elapsedMs={} officialV100={}",
                dayRows, dateRows, range[0], range[1], elapsed, knownDataset);

        return new DatasetImportResult(
                before.schemaName(), safeName(calendarDayFile), safeName(calendarDateFile), dayRows, dateRows,
                range[0], range[1], dayHash, dateHash, knownDataset, elapsed, verification
        );
    }

    private static void validateVerification(long dayRows, DatasetVerification verification) {
        LinkedHashMap<String, String> errors = new LinkedHashMap<>();
        if (!verification.valid()) {
            if (verification.badRepresentationDayCount() != 0) errors.put("representations", "تعداد روزهای بدون دقیقاً سه نمایش: " + verification.badRepresentationDayCount());
            if (verification.canonicalDateGapCount() != 0) errors.put("dateGap", "تعداد گسست در توالی تاریخ Canonical: " + verification.canonicalDateGapCount());
            if (verification.dayIdGapCount() != 0) errors.put("dayIdGap", "DAY_IDها پیوسته نیستند.");
            if (verification.badJulianDayCount() != 0) errors.put("julian", "تعداد مغایرت JDN: " + verification.badJulianDayCount());
            if (verification.badIsoWeekdayCount() != 0) errors.put("weekday", "تعداد مغایرت روز هفته ISO: " + verification.badIsoWeekdayCount());
            if (verification.unknownCalendarSystemCount() != 0) errors.put("calendarSystem", "کد تقویم خارج از سه سیستم مجاز وجود دارد.");
        }
        for (String system : CalendarDatasetCsvParser.CALENDAR_SYSTEMS) {
            long count = verification.calendarSystemCounts().getOrDefault(system, 0L);
            if (count != dayRows) errors.put("system." + system, system + " باید دقیقاً " + dayRows + " رکورد داشته باشد؛ مقدار فعلی " + count + " است.");
        }
        if (!errors.isEmpty()) {
            throw new ReferenceValidationException("اعتبارسنجی نهایی Dataset تقویم ناموفق بود؛ تراکنش Rollback می‌شود.", errors);
        }
    }

    private static void validateFile(MultipartFile file, String field, String expectedName) {
        if (file == null || file.isEmpty()) throw validation("هر دو فایل CSV الزامی هستند.", field, expectedName + " انتخاب نشده است.");
        String name = safeName(file).toLowerCase();
        if (!name.endsWith(".csv")) throw validation("نوع فایل معتبر نیست.", field, "فقط فایل CSV قابل Import است.");
        if (file.getSize() > MAX_FILE_SIZE) throw validation("حجم فایل بیش از حد مجاز است.", field, "حداکثر حجم هر فایل 64MB است.");
    }

    private static String sha256(MultipartFile file, String field) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream raw = file.getInputStream(); DigestInputStream input = new DigestInputStream(raw, digest)) {
                input.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (IOException exception) {
            throw validation("خواندن فایل برای محاسبه SHA-256 ناموفق بود.", field, exception.getMessage());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String safeName(MultipartFile file) {
        String name = file == null ? null : file.getOriginalFilename();
        if (name == null || name.isBlank()) return "calendar.csv";
        String normalized = name.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private static ReferenceValidationException validation(String message, String field, String detail) {
        return new ReferenceValidationException(message, Map.of(field, detail == null ? message : detail));
    }
}
