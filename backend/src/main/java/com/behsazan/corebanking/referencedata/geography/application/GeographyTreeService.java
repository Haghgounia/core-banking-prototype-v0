package com.behsazan.corebanking.referencedata.geography.application;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorRegistry;
import com.behsazan.corebanking.referencedata.geography.domain.GeographyTreeNode;
import com.behsazan.corebanking.referencedata.management.application.ReferenceRepository;
import com.behsazan.corebanking.referencedata.management.domain.LookupOption;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class GeographyTreeService {
    private final ReferenceDescriptorRegistry registry;
    private final ReferenceRepository repository;

    public GeographyTreeService(ReferenceDescriptorRegistry registry, ReferenceRepository repository) {
        this.registry = registry;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GeographyTreeNode> roots() {
        return nodes("provinces", null, true);
    }

    @Transactional(readOnly = true)
    public List<GeographyTreeNode> children(String resource, long id) {
        return switch (resource) {
            case "provinces" -> nodes("counties", id, true);
            case "counties" -> nodes("districts", id, true);
            case "districts" -> {
                List<GeographyTreeNode> result = new ArrayList<>();
                result.addAll(nodes("cities", id, false));
                result.addAll(nodes("rural-districts", id, true));
                result.sort(Comparator.comparing(GeographyTreeNode::label));
                yield List.copyOf(result);
            }
            case "rural-districts" -> nodes("villages", id, false);
            case "cities", "villages" -> List.of();
            default -> throw new ReferenceNotFoundException("سطح جغرافیایی ناشناخته است: " + resource);
        };
    }

    private List<GeographyTreeNode> nodes(String resource, Long parentId, boolean hasChildren) {
        return repository.lookup(registry.require(resource), parentId, null, 500).stream()
                .map(option -> node(resource, option, hasChildren))
                .toList();
    }

    private static GeographyTreeNode node(String resource, LookupOption option, boolean hasChildren) {
        return new GeographyTreeNode(resource, option.value(), option.code(), option.label(), hasChildren);
    }
}
