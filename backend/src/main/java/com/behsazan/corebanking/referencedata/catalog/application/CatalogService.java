package com.behsazan.corebanking.referencedata.catalog.application;

import com.behsazan.corebanking.referencedata.catalog.domain.CatalogItem;
import com.behsazan.corebanking.referencedata.catalog.domain.CatalogResponse;
import com.behsazan.corebanking.referencedata.catalog.domain.CatalogStatus;
import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorRegistry;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CatalogService {
    private final ReferenceDescriptorRegistry registry;

    public CatalogService(ReferenceDescriptorRegistry registry) {
        this.registry = registry;
    }

    public CatalogResponse catalog() {
        List<CatalogItem> items = new ArrayList<>();
        registry.all().stream().map(this::active).forEach(items::add);

        planned(items, "religions", "GENERAL", "ادیان", "diversity_3");

        return new CatalogResponse(items);
    }

    private CatalogItem active(ReferenceTableDescriptor descriptor) {
        return new CatalogItem(
                descriptor.resource(), descriptor.category(), descriptor.title(), descriptor.icon(),
                CatalogStatus.ACTIVE, descriptor.parent() == null ? null : descriptor.parent().resource()
        );
    }

    private static void planned(List<CatalogItem> items, String resource, String category, String title, String icon) {
        items.add(new CatalogItem(resource, category, title, icon, CatalogStatus.PLANNED, null));
    }
}
