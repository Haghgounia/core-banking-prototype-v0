package com.behsazan.corebanking.referencedata.descriptor.application;

import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReferenceDescriptorRegistry {
    private final Map<String, ReferenceTableDescriptor> descriptors = new LinkedHashMap<>();

    public ReferenceDescriptorRegistry(List<ReferenceDescriptorProvider> providers) {
        providers.stream()
                .flatMap(provider -> provider.descriptors().stream())
                .forEach(this::register);
    }

    public ReferenceTableDescriptor require(String resource) {
        ReferenceTableDescriptor descriptor = descriptors.get(resource);
        if (descriptor == null) {
            throw new ReferenceNotFoundException("منبع اطلاعات پایه ناشناخته است: " + resource);
        }
        return descriptor;
    }

    public List<ReferenceTableDescriptor> all() {
        return List.copyOf(descriptors.values());
    }

    private void register(ReferenceTableDescriptor descriptor) {
        if (descriptors.putIfAbsent(descriptor.resource(), descriptor) != null) {
            throw new IllegalStateException("Duplicate resource descriptor: " + descriptor.resource());
        }
    }
}
