package com.behsazan.corebanking.organization.contact;

import com.behsazan.corebanking.organization.contact.OrganizationContactModels.*;
import java.util.List;

/** Exactly one contact storage implementation is active per application instance. */
public interface OrganizationContactOperations {
    ContactReadiness readiness();
    List<ContactRole> roles();
    List<ContactRow> list(Long organizationUnitId);
    ContactRow create(ContactInput input);
    ContactRow update(long recordId, ContactInput input);
}
