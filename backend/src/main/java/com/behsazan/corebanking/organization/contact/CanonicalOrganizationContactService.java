package com.behsazan.corebanking.organization.contact;

import com.behsazan.corebanking.organization.contact.OrganizationContactModels.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * R5-C4B: read-only compatibility alias for the retired "canonical" setting.
 * The former V2 pilot is never used by an application service again.
 * Requests use the SAME OUCP implementation that serves the default mode.
 * This historic configuration cannot enable writes under either write flag.
 * For approved OUCP writes use the default "legacy" bean with Oracle guards.
 */
@Deprecated(forRemoval = false)
@Service
@ConditionalOnProperty(prefix = "core-banking.organization.contacts", name = "storage-mode",
        havingValue = "canonical")
public class CanonicalOrganizationContactService implements OrganizationContactOperations {
    private final OrganizationContactService delegate;

    public CanonicalOrganizationContactService(NamedParameterJdbcTemplate jdbc) {
        this.delegate = new OrganizationContactService(jdbc, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ContactReadiness readiness() { return delegate.readiness(); }

    @Override
    @Transactional(readOnly = true)
    public List<ContactRole> roles() { return delegate.roles(); }

    @Override
    @Transactional(readOnly = true)
    public List<ContactRow> list(Long organizationUnitId) { return delegate.list(organizationUnitId); }

    @Override
    @Transactional
    public ContactRow create(ContactInput input) { return delegate.create(input); }

    @Override
    @Transactional
    public ContactRow update(long recordId, ContactInput input) { return delegate.update(recordId, input); }
}
