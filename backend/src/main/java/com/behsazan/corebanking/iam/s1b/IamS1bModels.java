package com.behsazan.corebanking.iam.s1b;

import java.util.List;

/** Read-only projections. No password, token, MFA secret or raw IdP credentials. */
public final class IamS1bModels {
    private IamS1bModels() { }

    public record Domain(long securityDomainId, String domainCode, String context, boolean active) { }

    public record Account(long accountId, long principalId, String principalUid,
                          String principalType, String principalStatus, String username,
                          String usernameNormalized, String accountStatus, String context,
                          long accountVersion, long principalVersion) { }

    public record AccountPage(String domainCode, String context, int offset, int limit,
                              boolean hasMore, List<Account> items) { }

    public record IdentityLink(long linkId, long principalId, String sourceSystem,
                               String externalEntityType, String externalEntityId,
                               String validFrom, String validTo, long recordVersion) { }

    public record Readiness(String schema, String snapshotContract, String domainCode,
                            String context, boolean readOnly, boolean writesEnabled,
                            boolean metadataVerified) { }
}
