package com.behsazan.corebanking.iam.s1b;

import java.util.*;

/** Mandatory runtime permission proof for the dedicated IAM Oracle reader. */
public final class IamS1bOracleReadPolicy {
    public static final Set<String> TABLES = Set.of(
            "SECURITY_DOMAINS", "PRINCIPALS", "USER_ACCOUNTS", "IDENTITY_LINKS");
    public record Grant(String owner, String table, String privilege) { }

    private IamS1bOracleReadPolicy() { }

    public static boolean leastPrivilege(Collection<String> sessionPrivileges,
                                          Collection<String> enabledRoles,
                                          Collection<Grant> directGrants) {
        if (sessionPrivileges == null || enabledRoles == null || directGrants == null
                || !enabledRoles.isEmpty() || !sessionPrivileges.contains("CREATE SESSION")) return false;
        for (String privilege : sessionPrivileges) {
            if (!"CREATE SESSION".equals(privilege)) return false;
        }
        Set<String> readable = new HashSet<>();
        for (Grant grant : directGrants) {
            if (grant == null || !"IAM".equals(grant.owner()) || !TABLES.contains(grant.table())
                    || !("SELECT".equals(grant.privilege()) || "READ".equals(grant.privilege()))) return false;
            readable.add(grant.table());
        }
        return readable.containsAll(TABLES);
    }
}
