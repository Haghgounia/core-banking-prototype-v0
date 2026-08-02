package com.behsazan.corebanking.referencedata.management.application;

import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.referencedata.management.domain.LookupOption;
import com.behsazan.corebanking.referencedata.management.domain.ReferenceSearchQuery;
import com.behsazan.corebanking.shared.model.PageResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReferenceRepository {
    PageResponse<Map<String, Object>> search(ReferenceTableDescriptor descriptor, ReferenceSearchQuery query);

    Optional<Map<String, Object>> findById(ReferenceTableDescriptor descriptor, long id);

    long insert(ReferenceTableDescriptor descriptor, Map<String, Object> values, long actorId);

    boolean update(ReferenceTableDescriptor descriptor, long id, Map<String, Object> values, long actorId);

    boolean delete(ReferenceTableDescriptor descriptor, long id);

    List<LookupOption> lookup(ReferenceTableDescriptor descriptor, Long parentId, String text, int limit);

    long count(ReferenceTableDescriptor descriptor);
}
