package com.behsazan.corebanking.referencedata.geography.web;

import com.behsazan.corebanking.referencedata.geography.application.GeographyTreeService;
import com.behsazan.corebanking.referencedata.geography.domain.GeographyTreeNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/geography/tree")
public class GeographyTreeController {
    private final GeographyTreeService service;

    public GeographyTreeController(GeographyTreeService service) {
        this.service = service;
    }

    @GetMapping("/roots")
    List<GeographyTreeNode> roots() {
        return service.roots();
    }

    @GetMapping("/{resource}/{id}/children")
    List<GeographyTreeNode> children(@PathVariable String resource, @PathVariable long id) {
        return service.children(resource, id);
    }
}
