package com.behsazan.corebanking.productbuilder.web;

import com.behsazan.corebanking.productbuilder.application.RuleGovernanceLiteService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** Narrow, explicit API: policy tables are NOT exposed via generic PDL CRUD. */
@RestController
@RequestMapping("/api/v1/product-builder")
public class RuleGovernanceLiteController {
    private final RuleGovernanceLiteService service;
    public RuleGovernanceLiteController(RuleGovernanceLiteService service) { this.service = service; }

    @GetMapping("/rule-policies")
    public List<RuleGovernanceLiteService.Policy> list() { return service.policies(); }
    @GetMapping("/rule-policies/{id}/controls")
    public List<RuleGovernanceLiteService.Control> controls(@PathVariable long id) { return service.controls(id); }
    @PostMapping("/rule-policies")
    public RuleGovernanceLiteService.Policy create(@RequestBody RuleGovernanceLiteService.CreatePolicy body,
                     @RequestHeader(name="X-User-Name",defaultValue="prototype-ui") String actor) {
        return service.create(body,actor);
    }
    @PostMapping("/rule-policies/{id}/clone")
    public RuleGovernanceLiteService.Policy clone(@PathVariable long id,
                     @RequestHeader(name="X-User-Name",defaultValue="prototype-ui") String actor) {
        return service.clonePolicy(id,actor);
    }
    @PutMapping("/rule-policies/{id}/controls")
    public List<RuleGovernanceLiteService.Control> save(@PathVariable long id,
                     @RequestBody List<RuleGovernanceLiteService.ChangeControl> controls,
                     @RequestHeader(name="X-User-Name",defaultValue="prototype-ui") String actor) {
        return service.saveControls(id,controls,actor);
    }
    @PostMapping("/rule-policies/{id}/approve")
    public RuleGovernanceLiteService.Policy approve(@PathVariable long id,
                     @RequestBody RuleGovernanceLiteService.Approval body,
                     @RequestHeader(name="X-User-Name",defaultValue="prototype-ui") String actor) {
        return service.approve(id,body,actor);
    }
    @PutMapping("/versions/{versionId}/rule-policy")
    public Map<String,Object> bind(@PathVariable long versionId,@RequestBody RuleGovernanceLiteService.Binding body) {
        return service.bind(versionId,body);
    }
}
