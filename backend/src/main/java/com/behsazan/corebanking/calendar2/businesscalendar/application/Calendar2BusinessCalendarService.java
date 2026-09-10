package com.behsazan.corebanking.calendar2.businesscalendar.application;

import com.behsazan.corebanking.calendar2.businesscalendar.domain.Calendar2BusinessCalendarModels.RebuildResult;
import com.behsazan.corebanking.calendar2.businesscalendar.oracle.Calendar2BusinessCalendarRepository;
import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
public class Calendar2BusinessCalendarService {
    private final Calendar2BusinessCalendarRepository repository;

    public Calendar2BusinessCalendarService(Calendar2BusinessCalendarRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RebuildResult rebuild(long businessCalendarId, LocalDate requestedFrom, LocalDate requestedTo) {
        if (!repository.businessCalendarExists(businessCalendarId)) {
            throw validation("تقویم کاری انتخاب‌شده وجود ندارد یا غیرفعال است.", "businessCalendarId");
        }
        LocalDate[] range = repository.resolveRange(businessCalendarId, requestedFrom, requestedTo);
        LocalDate from = range[0];
        LocalDate to = range[1];
        if (from == null || to == null) {
            throw validation("برای این تقویم کاری، بازه فعال برنامه ساعات کاری یا استثنا یافت نشد. تاریخ شروع و پایان را مشخص کنید.", "_form");
        }
        if (to.isBefore(from)) throw validation("تاریخ پایان بازسازی نمی‌تواند قبل از تاریخ شروع باشد.", "toDate");
        if (from.plusYears(5).isBefore(to)) {
            throw validation("برای کنترل حجم عملیات، هر بار حداکثر پنج سال را بازسازی کنید.", "toDate");
        }
        long incompleteScheduleDays = repository.incompleteActiveScheduleDays(businessCalendarId, from, to);
        if (incompleteScheduleDays > 0) {
            throw validation("الگوی هفتگی Scheduleهای فعال کامل نیست؛ برای روزهای باز/نیمه‌وقت ساعت شروع و پایان کارکنان و مشتری را تکمیل کنید. تعداد ردیف ناقص: " + incompleteScheduleDays, "_form");
        }
        int affected = repository.rebuild(businessCalendarId, from, to);
        long[] counts = repository.resolutionCounts(businessCalendarId, from, to);
        return new RebuildResult(businessCalendarId, from, to, affected, counts[0], counts[1], counts[2], counts[3], counts[4]);
    }

    private static ReferenceValidationException validation(String message, String field) {
        return new ReferenceValidationException(message, Map.of(field, message));
    }
}
