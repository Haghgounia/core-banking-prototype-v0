package com.behsazan.corebanking.referencedata.catalog.web;

import com.behsazan.corebanking.referencedata.catalog.application.CatalogService;
import com.behsazan.corebanking.referencedata.catalog.domain.CatalogResponse;
import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorRegistry;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final CatalogService catalogService;
    private final ReferenceDescriptorRegistry registry;

    public CatalogController(CatalogService catalogService, ReferenceDescriptorRegistry registry) {
        this.catalogService = catalogService;
        this.registry = registry;
    }

    @GetMapping
    CatalogResponse catalog() {
        return catalogService.catalog();
    }

    @GetMapping("/{resource}")
    ReferenceTableDescriptor descriptor(@PathVariable String resource) {
        return registry.require(resource);
    }
}
