package com.behsazan.corebanking.referencedata.descriptor.application;

import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import java.util.List;

public interface ReferenceDescriptorProvider {
    List<ReferenceTableDescriptor> descriptors();
}
