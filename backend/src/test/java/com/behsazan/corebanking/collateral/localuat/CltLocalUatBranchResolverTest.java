package com.behsazan.corebanking.collateral.localuat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Branch is discovered server-side: no request-supplied JWT, branch header, or startup prompt. */
class CltLocalUatBranchResolverTest {
    @Test void discoversBranchOnceAndCachesForSubsequentReadRequests() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(CltLocalUatBranchResolver.DISCOVERY_SQL, String.class))
                .thenReturn("65359");
        var resolver = new CltLocalUatBranchResolver(new CltLocalUatProperties(true, null), jdbc);
        assertEquals("65359", resolver.resolve());
        assertEquals("65359", resolver.resolve());
        verify(jdbc, times(1)).queryForObject(CltLocalUatBranchResolver.DISCOVERY_SQL, String.class);
    }

    @Test void noDataYieldsSafeNonexistentBranchNotCrossBranchVisibility() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(CltLocalUatBranchResolver.DISCOVERY_SQL, String.class))
                .thenReturn(null);
        assertEquals("CLT_NO_DATA", new CltLocalUatBranchResolver(
                new CltLocalUatProperties(true, null), jdbc).resolve());
    }

    @Test void unresolvedOracleReturnsServiceUnavailableNotUnscopedRead() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(CltLocalUatBranchResolver.DISCOVERY_SQL, String.class))
                .thenThrow(new DataAccessResourceFailureException("oracle unavailable"));
        var resolver = new CltLocalUatBranchResolver(new CltLocalUatProperties(true, null), jdbc);
        assertEquals(503, assertThrows(ResponseStatusException.class, resolver::resolve)
                .getStatusCode().value());
    }

    @Test void serverOnlyOverrideRetainsPriorBranchScopeAndDoesNotQueryOracleForDiscovery() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var resolver = new CltLocalUatBranchResolver(new CltLocalUatProperties(true, "100"), jdbc);
        assertEquals("100", resolver.resolve());
        verify(jdbc, never()).queryForObject(anyString(), eq(String.class));
    }

    @Test void disabledLocalModeCannotConstructLocalAuthorization() {
        var resolver = new CltLocalUatBranchResolver(new CltLocalUatProperties(false, null),
                mock(JdbcTemplate.class));
        assertThrows(IllegalStateException.class, resolver::resolve);
    }
}
