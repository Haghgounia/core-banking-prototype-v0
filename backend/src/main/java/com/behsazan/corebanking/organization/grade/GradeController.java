package com.behsazan.corebanking.organization.grade;

import com.behsazan.corebanking.organization.grade.GradeModels.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** Prototype-only grade maintenance. No approval workflow or special roles. */
@RestController
@RequestMapping("/api/v1/organization/grades")
public class GradeController {
    private final GradeService service;
    private final GradeActorResolver actors;
    public GradeController(GradeService service, GradeActorResolver actors) {
        this.service = service;
        this.actors = actors;
    }
    @GetMapping("/branch-type") public Map<String,Object> branchType() { return service.branchType(); }
    @GetMapping("/organizations") public List<Organization> organizations() { return service.organizations(); }
    @GetMapping("/branches/{id}") public Unit branchById(@PathVariable long id) { return service.branchById(id); }
    @GetMapping("/branches") public List<Unit> branches(@RequestParam(required = false) String text,
                @RequestParam(defaultValue = "75") int limit) { return service.branches(text, limit); }
    @GetMapping("/levels") public List<Level> levels(@RequestParam(required = false) Long organizationId,
                @RequestParam(defaultValue = "false") boolean activeOnly) {
        return service.levels(organizationId, activeOnly);
    }
    @PostMapping("/levels") public Level createLevel(@RequestBody LevelInput input) {
        return service.createLevel(input, actors.actor());
    }
    @PutMapping("/levels/{id}") public Level updateLevel(@PathVariable long id, @RequestBody LevelInput input) {
        return service.updateLevel(id, input, actors.actor());
    }
    @GetMapping("/periods") public List<Period> periods(@RequestParam long unitId) { return service.periods(unitId); }
    @GetMapping("/coverage") public Coverage coverage() { return service.coverage(); }
    @PostMapping("/periods") public Period assign(@RequestBody AssignmentInput input) {
        return service.assign(input, actors.actor());
    }
}
