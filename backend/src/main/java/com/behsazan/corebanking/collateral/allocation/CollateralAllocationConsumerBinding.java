package com.behsazan.corebanking.collateral.allocation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Server-owned allowlist, disabled by empty defaults; never accept consumer identity from HTTP body/headers. */
@Component
public class CollateralAllocationConsumerBinding {
    private final Map<String, String> clients;
    public CollateralAllocationConsumerBinding(
            @Value("${core-banking.collateral.allocation.service-clients.loan:}") String loan,
            @Value("${core-banking.collateral.allocation.service-clients.guarantee:}") String guarantee,
            @Value("${core-banking.collateral.allocation.service-clients.letter-of-credit:}") String credit,
            @Value("${core-banking.collateral.allocation.service-clients.green-license:}") String green) {
        clients = Map.of("LOAN", loan.trim(), "GUARANTEE", guarantee.trim(),
                "LETTER_OF_CREDIT", credit.trim(), "GREEN_LICENSE", green.trim());
    }
    public String authenticatedClient(JwtAuthenticationToken jwt, String domain) {
        String expected = clients.getOrDefault(domain, "");
        if (expected.isBlank()) throw new SecurityException("CLT_ALLOCATION_CLIENT_BINDING_NOT_CONFIGURED");
        String clientId = jwt.getToken().getClaimAsString("client_id");
        String azp = jwt.getToken().getClaimAsString("azp");
        if (clientId == null || clientId.isBlank() || azp != null && !azp.equals(clientId))
            throw new SecurityException("CLT_ALLOCATION_CLIENT_CLAIMS_INVALID");
        String grant = jwt.getToken().getClaimAsString("gty");
        if (grant == null) grant = jwt.getToken().getClaimAsString("grant_type");
        if (!"client_credentials".equals(grant))
            throw new SecurityException("CLT_ALLOCATION_SERVICE_GRANT_REQUIRED");
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                clientId.getBytes(StandardCharsets.UTF_8)))
            throw new SecurityException("CLT_ALLOCATION_CONSUMER_BINDING_DENIED");
        return clientId;
    }
}
