package com.behsazan.corebanking.referencedata.management;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorRegistry;
import com.behsazan.corebanking.referencedata.geography.descriptor.GeographyDescriptorProvider;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.referencedata.management.application.ReferenceRepository;
import com.behsazan.corebanking.referencedata.management.application.ReferenceService;
import com.behsazan.corebanking.referencedata.management.domain.LookupOption;
import com.behsazan.corebanking.referencedata.management.domain.ReferenceSearchQuery;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceServiceTest {
    @Test
    void createAppliesDefaultsAndDoesNotSendLastModifiedAuditFields() {
        CapturingRepository repository = new CapturingRepository();
        ReferenceService service = new ReferenceService(new ReferenceDescriptorRegistry(List.of(new GeographyDescriptorProvider())), repository);

        service.create("provinces", Map.of(
                "provinceCode", "99",
                "provinceName", "نمونه"
        ), 42L);

        assertThat(repository.inserted).containsEntry("countryId", new java.math.BigDecimal("71"));
        assertThat(repository.inserted).containsEntry("isActive", true);
        assertThat(repository.inserted).doesNotContainKeys(
                "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate"
        );
        assertThat(repository.actorId).isEqualTo(42L);
    }

    private static final class CapturingRepository implements ReferenceRepository {
        private Map<String, Object> inserted = Map.of();
        private long actorId;

        @Override
        public PageResponse<Map<String, Object>> search(ReferenceTableDescriptor descriptor, ReferenceSearchQuery query) {
            return new PageResponse<>(List.of(), 0, 0, 20);
        }

        @Override
        public Optional<Map<String, Object>> findById(ReferenceTableDescriptor descriptor, long id) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (var field : descriptor.fields()) row.put(field.apiName(), null);
            row.put(descriptor.idApiName(), java.math.BigDecimal.valueOf(id));
            row.put(descriptor.nameApiName(), "نمونه");
            return Optional.of(row);
        }

        @Override
        public long insert(ReferenceTableDescriptor descriptor, Map<String, Object> values, long actorId) {
            this.inserted = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values));
            this.actorId = actorId;
            return 100L;
        }

        @Override public boolean update(ReferenceTableDescriptor d, long id, Map<String, Object> v, long actorId) { return true; }
        @Override public boolean delete(ReferenceTableDescriptor d, long id) { return true; }
        @Override public List<LookupOption> lookup(ReferenceTableDescriptor d, Long p, String t, int l) { return List.of(); }
        @Override public long count(ReferenceTableDescriptor d) { return 0; }
    }
}
