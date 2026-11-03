package com.behsazan.corebanking.iam.s1b;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IamS1bPolicyTest {
    @Test void signedClaimsMustHaveExactDomainContextAndScope() {
        assertTrue(IamS1bPolicy.canRead("actor", List.of("iam.users.read"),
                List.of("BRANCH_A"), List.of("WORKFORCE"), "BRANCH_A", "WORKFORCE"));
        assertFalse(IamS1bPolicy.canRead("actor", List.of("iam.users.read"),
                List.of("BRANCH_A"), List.of("WORKFORCE"), "BRANCH_B", "WORKFORCE"));
        assertFalse(IamS1bPolicy.canRead("actor", List.of("iam.users.read"),
                List.of("BRANCH_A"), List.of("WORKFORCE"), "BRANCH_A", "CUSTOMER"));
        assertFalse(IamS1bPolicy.canRead("actor", List.of("iam.users.write"),
                List.of("BRANCH_A"), List.of("WORKFORCE"), "BRANCH_A", "WORKFORCE"));
        assertFalse(IamS1bPolicy.canRead(null, List.of("iam.users.read"),
                List.of("BRANCH_A"), List.of("WORKFORCE"), "BRANCH_A", "WORKFORCE"));
        assertFalse(IamS1bPolicy.canRead("actor", List.of("iam.users.read"),
                List.of("*"), List.of("*"), "BRANCH_A", "WORKFORCE"));
    }

    @Test void claimsFailClosedAndSqlLikeIsEscaped() {
        assertEquals(List.of(), IamS1bPolicy.safeStringClaim("BRANCH_A"));
        assertEquals(List.of(), IamS1bPolicy.safeStringClaim(List.of("BRANCH_A", 99)));
        assertEquals("%a!%!_!!%", IamS1bPolicy.escapedLike("a%_!"));
        assertEquals("user", IamS1bPolicy.normalizedUsernameFragment("  USER  "));
        assertThrows(IllegalArgumentException.class, () -> IamS1bPolicy.domainCode("bad-domain"));
        assertThrows(IllegalArgumentException.class, () -> IamS1bPolicy.domainCode("*") );
        assertThrows(IllegalArgumentException.class, () -> IamS1bPolicy.normalizedUsernameFragment("a\u0001b"));
    }

    @Test void configurationRejectsSystemAccountAndNonHttpsIssuer() {
        IamS1bProperties disabled = new IamS1bProperties(false, null, null, null, 0L, null, null, null);
        assertDoesNotThrow(disabled::requireSafeReadConfiguration);
        IamS1bProperties privileged = new IamS1bProperties(true,"https://idp.bank.test", "https://idp.bank.test/jwks",
                "bank-iam-api",1800L,"jdbc:oracle:thin:@//db:1521/dev", "SYSTEM", "fake");
        assertThrows(IllegalStateException.class, privileged::requireSafeReadConfiguration);
        IamS1bProperties insecure = new IamS1bProperties(true,"http://idp.bank.test", "https://idp.bank.test/jwks",
                "bank-iam-api",1800L,"jdbc:oracle:thin:@//db:1521/dev", "IAM_RO", "fake");
        assertThrows(IllegalStateException.class, insecure::requireSafeReadConfiguration);
    }
}
