package com.behsazan.corebanking.iam.s1b;

import com.behsazan.corebanking.iam.s1b.IamS1bModels.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(prefix = "core-banking.iam.s1b", name = "reads-enabled", havingValue = "true")
public class IamS1bReadService {
    private static final Logger log = LoggerFactory.getLogger(IamS1bReadService.class);
    private final IamS1bReadRepository repo;

    public IamS1bReadService(IamS1bReadRepository repo) { this.repo = repo; }

    /** Every read is authorized against both the signed token and the Oracle security domain. */
    public Domain authorizedDomain(JwtAuthenticationToken authentication, String requestedCode) {
        String code = IamS1bPolicy.domainCode(requestedCode);
        // No Oracle I/O before verifying the *signed* scope and domain. Prevent unauthorized enumeration.
        List<String> domains = IamS1bPolicy.safeStringClaim(authentication.getToken().getClaims().get("iam_domains"));
        List<String> contexts = IamS1bPolicy.safeStringClaim(authentication.getToken().getClaims().get("iam_contexts"));
        List<String> scopes = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("SCOPE_")).map(a -> a.substring(6)).toList();
        if (authentication.getToken().getSubject() == null || authentication.getToken().getSubject().isBlank()
                || !scopes.contains("iam.users.read") || !domains.contains(code)) {
            deny(authentication, code, "scope_or_domain_not_granted");
        }
        if (!repo.metadataReady()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "IAM_S1B_SCHEMA_NOT_READY");
        Domain domain = repo.domain(code).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "IAM_S1B_DOMAIN_NOT_FOUND"));
        if (!domain.active() || !IamS1bPolicy.canRead(authentication.getToken().getSubject(), scopes,
                domains, contexts, domain.domainCode(), domain.context())) {
            deny(authentication, code, "context_or_scope_denied");
        }
        return domain;
    }

    public Readiness readiness(JwtAuthenticationToken actor, String domainCode) {
        Domain domain = authorizedDomain(actor, domainCode);
        readAudit(actor, domain.domainCode(), "READINESS");
        return new Readiness("IAM", "R1.5.1-AS-IS", domain.domainCode(), domain.context(), true, false, true);
    }

    public AccountPage accounts(JwtAuthenticationToken actor, String domainCode, String search, int offset, int limit) {
        if (offset < 0 || offset > 10000 || limit < 1 || limit > 100)
            throw new IllegalArgumentException("IAM_S1B_INVALID_PAGINATION");
        String normalized = IamS1bPolicy.normalizedUsernameFragment(search);
        Domain domain = authorizedDomain(actor, domainCode);
        List<Account> all = repo.accounts(domain, normalized, offset, limit + 1);
        boolean more = all.size() > limit;
        readAudit(actor, domain.domainCode(), "ACCOUNT_LIST");
        return new AccountPage(domain.domainCode(), domain.context(), offset, limit, more,
                more ? List.copyOf(all.subList(0, limit)) : List.copyOf(all));
    }

    public Account account(JwtAuthenticationToken actor, String domainCode, long accountId) {
        if (accountId < 1) throw new IllegalArgumentException("IAM_S1B_INVALID_ACCOUNT_ID");
        Domain domain = authorizedDomain(actor, domainCode);
        Account result = repo.account(domain, accountId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "IAM_S1B_ACCOUNT_NOT_FOUND"));
        readAudit(actor, domain.domainCode(), "ACCOUNT_DETAIL");
        return result;
    }

    public List<IdentityLink> links(JwtAuthenticationToken actor, String domainCode, long accountId) {
        if (accountId < 1) throw new IllegalArgumentException("IAM_S1B_INVALID_ACCOUNT_ID");
        Domain domain = authorizedDomain(actor, domainCode);
        Account account = repo.account(domain, accountId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "IAM_S1B_ACCOUNT_NOT_FOUND"));
        // Identity links are principal-scoped in R1.5.1, NOT domain-scoped.
        // Block projection when that principal spans multiple accounts/domains until ADR-IAM-04.
        if (!repo.principalIsExclusiveToDomain(domain, account.principalId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "IAM_S1B_SHARED_PRINCIPAL_LINKS_BLOCKED");
        List<IdentityLink> links = repo.links(domain, account.principalId());
        if (links.size() > 100) throw new ResponseStatusException(HttpStatus.CONFLICT, "IAM_S1B_LINK_LIMIT_EXCEEDED");
        readAudit(actor, domain.domainCode(), "IDENTITY_LINK_LIST");
        return links;
    }

    private static void deny(JwtAuthenticationToken actor, String domainCode, String reason) {
        log.warn("IAM_S1B_ACCESS_DENIED actor={} domain={} reason={}", auditActor(actor), domainCode, reason);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "IAM_S1B_CROSS_CONTEXT_DENIED");
    }

    private static void readAudit(JwtAuthenticationToken actor, String domain, String action) {
        // Minimal operator log only, not durable audit. Do not emit username, external ID, JWT or raw claims.
        log.info("IAM_S1B_READ actor={} domain={} action={} correlation={}",
                auditActor(actor), domain, action, UUID.randomUUID());
    }

    private static String auditActor(JwtAuthenticationToken actor) {
        try {
            String identity = actor.getToken().getIssuer() + "|" + actor.getToken().getSubject();
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest).substring(0, 20);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("IAM_S1B_AUDIT_HASH_UNAVAILABLE", impossible);
        }
    }
}
