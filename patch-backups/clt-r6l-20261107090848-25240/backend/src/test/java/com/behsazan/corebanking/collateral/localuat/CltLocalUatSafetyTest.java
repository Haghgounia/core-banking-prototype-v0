package com.behsazan.corebanking.collateral.localuat;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;

class CltLocalUatSafetyTest {
    private static CollateralReferenceProperties reference() {
        return new CollateralReferenceProperties(true, null, null, null, 3600,
                null, null, null, false, null, null, null);
    }
    private static MockEnvironment environment() {
        return new MockEnvironment().withProperty("server.address", "127.0.0.1");
    }
    private static MockHttpServletRequest request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/collaterals/readiness");
        request.setRemoteAddr("127.0.0.1");
        request.setLocalAddr("127.0.0.1");
        request.setServerName("localhost");
        return request;
    }
    @AfterEach void cleanContext() { SecurityContextHolder.clearContext(); }

    @Test void requiresLoopbackAndDisabledWritesWithOptionalBranch() {
        var properties = new CltLocalUatProperties(true, "100");
        assertDoesNotThrow(() -> properties.requireSafeMode(environment(), reference(),
                new CollateralRegistryProperties(true)));
        assertThrows(IllegalStateException.class, () -> properties.requireSafeMode(
                new MockEnvironment().withProperty("server.address", "0.0.0.0"), reference(),
                new CollateralRegistryProperties(true)));
        assertDoesNotThrow(() -> new CltLocalUatProperties(true, null).requireSafeMode(
                environment(), reference(), new CollateralRegistryProperties(true)));
        assertThrows(IllegalStateException.class, () -> new CltLocalUatProperties(true, "?BAD").requireSafeMode(
                environment(), reference(), new CollateralRegistryProperties(true)));
        assertThrows(IllegalStateException.class, () -> properties.requireSafeMode(
                environment().withProperty("core-banking.collateral.introduction.draft-writes-enabled", "true"),
                reference(), new CollateralRegistryProperties(true)));
        assertThrows(IllegalStateException.class, () -> properties.requireSafeMode(
                environment(), reference(), new CollateralRegistryProperties(false)));
    }

    @Test void loopbackGetHasOnlyReadAuthoritiesAndServerFixedBranch() throws Exception {
        var filter = new CltLocalUatAuthenticationFilter(new CltLocalUatProperties(true, "100"),
                new CltLocalUatBranchResolver(new CltLocalUatProperties(true, "100"),
                        org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class)));
        var request = request("GET");
        request.addHeader("X-Branch-Code", "FORGED");
        var response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> {
            invoked.set(true);
            assertInstanceOf(JwtAuthenticationToken.class, SecurityContextHolder.getContext().getAuthentication());
            JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            assertEquals("100", auth.getToken().getClaimAsString("branch_code"));
            assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("SCOPE_clt.collateral.read")));
            assertFalse(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("introduce")));
            assertFalse(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("allocation.request")));
        };
        filter.doFilter(request, response, chain);
        assertTrue(invoked.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(response.getHeader("Cache-Control").contains("no-store"));
    }

    @Test void remoteAndProxyRequestsAreRejectedBeforeAnyController() throws Exception {
        var filter = new CltLocalUatAuthenticationFilter(new CltLocalUatProperties(true, "100"),
                new CltLocalUatBranchResolver(new CltLocalUatProperties(true, "100"),
                        org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class)));
        for (int index = 0; index < 3; index++) {
            var request = request("GET");
            if (index == 0) request.setRemoteAddr("192.168.1.5");
            if (index == 1) request.addHeader("X-Forwarded-For", "192.168.1.5");
            if (index == 2) request.setServerName("example.org");
            var response = new MockHttpServletResponse();
            AtomicBoolean invoked = new AtomicBoolean(false);
            filter.doFilter(request, response, (req, res) -> invoked.set(true));
            assertFalse(invoked.get());
            assertEquals(403, response.getStatus());
        }
    }
    @Test void oracleBranchDiscoveryFailureReturnsHttp503BeforeController() throws Exception {
        var local = new CltLocalUatProperties(true, null);
        var jdbc = org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        org.mockito.Mockito.when(jdbc.queryForObject(
                CltLocalUatBranchResolver.DISCOVERY_SQL, String.class))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"));
        var filter = new CltLocalUatAuthenticationFilter(local,
                new CltLocalUatBranchResolver(local, jdbc));
        var response = new MockHttpServletResponse();
        filter.doFilter(request("GET"), response, (req, res) ->
                fail("controller must not run without a resolved server branch"));
        assertEquals(503, response.getStatus());
    }

    @Test void postNeverReceivesTestPrincipal() throws Exception {
        var filter = new CltLocalUatAuthenticationFilter(new CltLocalUatProperties(true, "100"),
                new CltLocalUatBranchResolver(new CltLocalUatProperties(true, "100"),
                        org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class)));
        var response = new MockHttpServletResponse();
        filter.doFilter(request("POST"), response, (req, res) ->
            assertNull(SecurityContextHolder.getContext().getAuthentication()));
    }
}
