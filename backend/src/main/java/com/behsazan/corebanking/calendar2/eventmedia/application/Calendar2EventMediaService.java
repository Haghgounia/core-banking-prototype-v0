package com.behsazan.corebanking.calendar2.eventmedia.application;

import com.behsazan.corebanking.calendar2.eventmedia.domain.Calendar2EventMediaModels.EventMediaContent;
import com.behsazan.corebanking.calendar2.eventmedia.domain.Calendar2EventMediaModels.EventMediaMetadata;
import com.behsazan.corebanking.calendar2.eventmedia.domain.Calendar2EventMediaModels.TodayEventMedia;
import com.behsazan.corebanking.calendar2.eventmedia.oracle.Calendar2EventMediaRepository;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class Calendar2EventMediaService {
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> CALENDAR_CODES = Set.of("PERSIAN", "GREGORIAN", "ISLAMIC");

    private final Calendar2EventMediaRepository repository;

    public Calendar2EventMediaService(Calendar2EventMediaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EventMediaMetadata upload(long eventId, MultipartFile file, int displayYearNo,
                                     String displayCalendarCode, String captionFa, String altTextFa,
                                     String actor) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (eventId <= 0) errors.put("eventId", "شناسه رویداد معتبر نیست.");
        if (file == null || file.isEmpty()) errors.put("file", "انتخاب تصویر الزامی است.");
        if (displayYearNo <= 0 || displayYearNo > 999999) errors.put("displayYearNo", "سال نمایش معتبر نیست.");
        String calendarCode = upper(displayCalendarCode);
        if (!CALENDAR_CODES.contains(calendarCode)) errors.put("displayCalendarCode", "تقویم سال تصویر معتبر نیست.");
        if (file != null && file.getSize() > MAX_IMAGE_BYTES) errors.put("file", "حجم تصویر باید حداکثر ۵ مگابایت باشد.");
        String mimeType = file == null ? null : lower(file.getContentType());
        if (file != null && !ALLOWED_TYPES.contains(mimeType)) errors.put("file", "فقط تصویر JPEG، PNG یا WebP مجاز است.");
        if (!errors.isEmpty()) throw new ReferenceValidationException("اطلاعات تصویر مناسبت معتبر نیست.", errors);

        String fileName = safeFileName(file.getOriginalFilename());
        String cleanCaption = trimToNull(captionFa);
        String cleanAlt = trimToNull(altTextFa);
        String cleanActor = trimToNull(actor);
        if (cleanCaption != null && cleanCaption.length() > 500) errors.put("captionFa", "عنوان تصویر حداکثر ۵۰۰ کاراکتر است.");
        if (cleanAlt != null && cleanAlt.length() > 500) errors.put("altTextFa", "متن جایگزین حداکثر ۵۰۰ کاراکتر است.");
        if (!errors.isEmpty()) throw new ReferenceValidationException("اطلاعات تصویر مناسبت معتبر نیست.", errors);

        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (IOException ex) { throw new ReferenceValidationException("خواندن فایل تصویر انجام نشد.", Map.of("file", "فایل تصویر قابل خواندن نیست.")); }

        repository.lockActiveEvent(eventId);
        repository.deactivateActive(eventId, cleanActor);
        long id = repository.nextId();
        repository.insert(id, eventId, displayYearNo, calendarCode, fileName, mimeType, bytes, cleanCaption, cleanAlt, cleanActor);
        return repository.findMetadata(id).orElseThrow(() -> new IllegalStateException("تصویر ثبت شد ولی Metadata آن قابل بازیابی نیست."));
    }

    public List<EventMediaMetadata> history(long eventId) {
        if (eventId <= 0) throw new ReferenceValidationException("شناسه رویداد معتبر نیست.", Map.of("eventId", "شناسه رویداد معتبر نیست."));
        return repository.history(eventId);
    }

    public List<TodayEventMedia> today() { return repository.today(); }

    public EventMediaContent content(long mediaId) {
        return repository.content(mediaId).orElseThrow(() -> new ReferenceNotFoundException("تصویر مناسبت یافت نشد."));
    }

    private static String safeFileName(String value) {
        String name = trimToNull(value);
        if (name == null) return "occasion-image";
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        return name.length() > 250 ? name.substring(name.length() - 250) : name;
    }

    private static String upper(String value) { String v = trimToNull(value); return v == null ? null : v.toUpperCase(Locale.ROOT); }
    private static String lower(String value) { String v = trimToNull(value); return v == null ? null : v.toLowerCase(Locale.ROOT); }
    private static String trimToNull(String value) { if (value == null) return null; String v = value.trim(); return v.isEmpty() ? null : v; }
}
