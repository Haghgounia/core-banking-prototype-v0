package com.behsazan.corebanking.cif.isic.web;

import com.behsazan.corebanking.cif.isic.application.IsicService;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityDetail;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityLookup;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityRequest;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityRow;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ReleaseLookup;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ReleaseRequest;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ReleaseRow;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cif/isic")
public class IsicController {
    private final IsicService service;
    public IsicController(IsicService service) { this.service = service; }

    @GetMapping("/releases")
    public PageResponse<ReleaseRow> releases(@RequestParam(required=false) String text, @RequestParam(required=false) Boolean active,
                                             @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
                                             @RequestParam(required=false) String sortBy, @RequestParam(defaultValue="asc") String direction) {
        return service.searchReleases(text, active, page, size, sortBy, direction);
    }
    @GetMapping("/releases/lookup") public List<ReleaseLookup> releaseLookup(@RequestParam(defaultValue="true") boolean includeInactive) { return service.releaseLookup(includeInactive); }
    @GetMapping("/releases/{id}") public ReleaseRow release(@PathVariable long id) { return service.findRelease(id); }
    @PostMapping("/releases") public ReleaseRow createRelease(@RequestBody ReleaseRequest request) { return service.createRelease(request); }
    @PutMapping("/releases/{id}") public ReleaseRow updateRelease(@PathVariable long id, @RequestBody ReleaseRequest request) { return service.updateRelease(id, request); }
    @DeleteMapping("/releases/{id}") public ResponseEntity<Void> deleteRelease(@PathVariable long id) { service.deleteRelease(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/activities")
    public PageResponse<ActivityRow> activities(@RequestParam(required=false) Long releaseId, @RequestParam(required=false) Long parentActivityId,
                                                @RequestParam(required=false) String levelCode, @RequestParam(required=false) String text,
                                                @RequestParam(required=false) Boolean active, @RequestParam(required=false) Boolean selectable,
                                                @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size,
                                                @RequestParam(required=false) String sortBy, @RequestParam(defaultValue="asc") String direction) {
        return service.searchActivities(releaseId, parentActivityId, levelCode, text, active, selectable, page, size, sortBy, direction);
    }
    @GetMapping("/activities/lookup") public List<ActivityLookup> activityLookup(@RequestParam long releaseId, @RequestParam(required=false) String text,
                                                                                  @RequestParam(defaultValue="false") boolean selectableOnly,
                                                                                  @RequestParam(defaultValue="1000") int limit) {
        return service.activityLookup(releaseId, text, selectableOnly, limit);
    }
    @GetMapping("/activities/{id}") public ActivityDetail activity(@PathVariable long id) { return service.findActivity(id); }
    @PostMapping("/activities") public ActivityDetail createActivity(@RequestBody ActivityRequest request) { return service.createActivity(request); }
    @PutMapping("/activities/{id}") public ActivityDetail updateActivity(@PathVariable long id, @RequestBody ActivityRequest request) { return service.updateActivity(id, request); }
    @DeleteMapping("/activities/{id}") public ResponseEntity<Void> deleteActivity(@PathVariable long id) { service.deleteActivity(id); return ResponseEntity.noContent().build(); }
}
