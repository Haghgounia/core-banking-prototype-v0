package com.behsazan.corebanking.organization.location.manage;

import com.behsazan.corebanking.organization.location.manage.OrganizationUnitLocationModels.*;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organization/unit-locations")
public class OrganizationUnitLocationController {
    private final OrganizationUnitLocationService service;
    public OrganizationUnitLocationController(OrganizationUnitLocationService service) { this.service=service; }

    @GetMapping("/readiness") public Readiness readiness() { return service.readiness(); }
    @GetMapping("/{id}") public LocationDetails get(@PathVariable long id) { return service.get(id); }
    @GetMapping("/{id}/history") public List<AuditEntry> history(@PathVariable long id) {return service.history(id);}
    @PostMapping public LocationDetails create(@RequestBody EditInput body) { return service.create(body); }
    @PutMapping("/{id}") public LocationDetails update(@PathVariable long id,@RequestBody EditInput body) {return service.update(id,body);}

    @RestControllerAdvice(assignableTypes=OrganizationUnitLocationController.class)
    public static class Advice {
        @ExceptionHandler(OrganizationUnitLocationException.class)
        public ProblemDetail problem(OrganizationUnitLocationException ex) {
            ProblemDetail pd=ProblemDetail.forStatusAndDetail(ex.status(),ex.getMessage());
            pd.setType(URI.create("urn:core-banking:organization:location"));
            pd.setTitle("خطا در ثبت یا ویرایش مکان و نشانی");
            pd.setProperty("errorCode",ex.code());
            return pd;
        }
    }
}
