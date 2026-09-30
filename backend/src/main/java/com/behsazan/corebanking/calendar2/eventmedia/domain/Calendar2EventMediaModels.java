package com.behsazan.corebanking.calendar2.eventmedia.domain;

import java.time.LocalDateTime;

public final class Calendar2EventMediaModels {
    private Calendar2EventMediaModels() {}

    public record EventMediaMetadata(
            long eventMediaId,
            long eventId,
            String eventCode,
            String eventName,
            int displayYearNo,
            String displayCalendarCode,
            String fileName,
            String mimeType,
            long fileSizeBytes,
            String captionFa,
            String altTextFa,
            boolean active,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime deactivatedAt,
            String deactivatedBy
    ) {}

    public record TodayEventMedia(
            long eventMediaId,
            long eventId,
            String eventCode,
            String eventName,
            String eventTypeName,
            int displayYearNo,
            String displayCalendarCode,
            String fileName,
            String mimeType,
            long fileSizeBytes,
            String captionFa,
            String altTextFa
    ) {}

    public record EventMediaContent(byte[] content, String mimeType, String fileName) {}
}
