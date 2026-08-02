package com.behsazan.corebanking.referencedata.employment.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorProvider;
import com.behsazan.corebanking.referencedata.descriptor.domain.ParentDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.*;

@Component
@Order(30)
public class EmploymentDescriptorProvider implements ReferenceDescriptorProvider {
    @Value("${core-banking.schemas.reference-data:GEO}")
    private String schemaName = "GEO";

    @Override
    public List<ReferenceTableDescriptor> descriptors() {
        return List.of(jobGroups(), jobs());
    }

    private ReferenceTableDescriptor jobGroups() {
        return descriptor(
                "job-groups", "EMPLOYMENT", "گروه‌های شغلی", "workspaces",
                schemaName, "JOB_GROUPS", "SEQ_JOB_GROUPS",
                "jobGroupId", "JOB_GROUP_ID", "jobGroupCode", "jobGroupName", null,
                withAudits(
                        id("jobGroupId", "JOB_GROUP_ID", "شناسه"),
                        text("jobGroupCode", "JOB_GROUP_CODE", "کد گروه شغلی", true, true, true, 20),
                        text("jobGroupName", "JOB_GROUP_NAME", "نام گروه شغلی", true, true, true, 200),
                        text("jobGroupEnglishName", "JOB_GROUP_ENGLISH_NAME", "نام انگلیسی", false, true, true, 200),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true)
                )
        );
    }

    private ReferenceTableDescriptor jobs() {
        ParentDescriptor parent = new ParentDescriptor("job-groups", "jobGroupId", "JOB_GROUP_ID", "گروه شغلی");
        return descriptor(
                "jobs", "EMPLOYMENT", "مشاغل", "work",
                schemaName, "JOBS", "SEQ_JOBS",
                "jobId", "JOB_ID", "jobCode", "jobName", parent,
                withAudits(
                        id("jobId", "JOB_ID", "شناسه"),
                        text("jobCode", "JOB_CODE", "کد شغل", true, true, true, 20),
                        text("jobName", "JOB_NAME", "نام شغل", true, true, true, 200),
                        text("jobEnglishName", "JOB_ENGLISH_NAME", "نام انگلیسی", false, true, true, 200),
                        lookup("jobGroupId", "JOB_GROUP_ID", "گروه شغلی", "job-groups", true, false),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true)
                )
        );
    }

    private static List<ReferenceFieldDescriptor> withAudits(ReferenceFieldDescriptor... mainFields) {
        List<ReferenceFieldDescriptor> result = new ArrayList<>(List.of(mainFields));
        result.addAll(employmentAudits());
        return List.copyOf(result);
    }
}
