package com.behsazan.corebanking.organization.contact;

import com.behsazan.corebanking.organization.contact.OrganizationContactModels.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organization/unit-contacts")
public class OrganizationContactController {
    private final OrganizationContactService service;

    public OrganizationContactController(OrganizationContactService service) {
        this.service = service;
    }
    @GetMapping("/readiness")
    public ContactReadiness readiness() { return service.readiness(); }

    @GetMapping("/roles")
    public List<ContactRole> roles() { return service.roles(); }

    @GetMapping
    public List<ContactRow> list(@RequestParam Long organizationUnitId) {
        return service.list(organizationUnitId);
    }

    @PostMapping
    public ContactRow create(@RequestBody ContactInput input) { return service.create(input); }

    @PutMapping("/{id}")
    public ContactRow update(@PathVariable long id, @RequestBody ContactInput input) {
        return service.update(id, input);
    }
}
