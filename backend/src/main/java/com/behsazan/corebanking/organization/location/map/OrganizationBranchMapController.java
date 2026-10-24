package com.behsazan.corebanking.organization.location.map;

import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Branch placement map endpoints: SELECT only, no writes or inferred branch contacts. */
@RestController
@RequestMapping("/api/v1/organization/branch-map")
public class OrganizationBranchMapController {
    private final OrganizationBranchMapService service;
    public OrganizationBranchMapController(OrganizationBranchMapService service){this.service=service;}

    @GetMapping("/geo-options")
    public List<OrganizationBranchMapService.GeoOption> options(
            @RequestParam String level,@RequestParam(required=false) Long provinceId,
            @RequestParam(required=false) Long countyId) {
        return service.options(level,provinceId,countyId);
    }
    @GetMapping("/points")
    public OrganizationBranchMapService.MapResult points(
            @RequestParam(required=false) Long provinceId,@RequestParam(required=false) Long countyId,
            @RequestParam(required=false) Long cityId,@RequestParam(required=false) Long ruralDistrictId,
            @RequestParam(required=false) String typeCode,@RequestParam(required=false) String text) {
        return service.points(provinceId,countyId,cityId,ruralDistrictId,typeCode,text);
    }
}
