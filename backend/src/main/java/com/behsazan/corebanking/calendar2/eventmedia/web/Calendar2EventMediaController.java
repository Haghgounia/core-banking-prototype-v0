package com.behsazan.corebanking.calendar2.eventmedia.web;

import com.behsazan.corebanking.calendar2.eventmedia.application.Calendar2EventMediaService;
import com.behsazan.corebanking.calendar2.eventmedia.domain.Calendar2EventMediaModels.EventMediaContent;
import com.behsazan.corebanking.calendar2.eventmedia.domain.Calendar2EventMediaModels.EventMediaMetadata;
import com.behsazan.corebanking.calendar2.eventmedia.domain.Calendar2EventMediaModels.TodayEventMedia;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar2/event-media")
public class Calendar2EventMediaController {
    private final Calendar2EventMediaService service;

    public Calendar2EventMediaController(Calendar2EventMediaService service) { this.service = service; }

    @PostMapping(value = "/events/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    EventMediaMetadata upload(
            @PathVariable long eventId,
            @RequestParam("file") MultipartFile file,
            @RequestParam int displayYearNo,
            @RequestParam(defaultValue = "PERSIAN") String displayCalendarCode,
            @RequestParam(required = false) String captionFa,
            @RequestParam(required = false) String altTextFa,
            @RequestParam(required = false) String actor
    ) {
        return service.upload(eventId, file, displayYearNo, displayCalendarCode, captionFa, altTextFa, actor);
    }

    @GetMapping("/events/{eventId}")
    List<EventMediaMetadata> history(@PathVariable long eventId) { return service.history(eventId); }

    @GetMapping("/today")
    List<TodayEventMedia> today() { return service.today(); }

    @GetMapping("/{mediaId}/content")
    ResponseEntity<byte[]> content(@PathVariable long mediaId) {
        EventMediaContent content = service.content(mediaId);
        MediaType type;
        try { type = MediaType.parseMediaType(content.mimeType()); }
        catch (Exception ignored) { type = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(content.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(content.content());
    }
}
