package com.behsazan.corebanking.collateral.reference;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class CltR1aCatalogTest {
    private final CltR1aCatalog catalog = new CltR1aCatalog(new JsonMapper());

    @Test void catalogsAll17PhysicalReferenceTablesWith53TechnicalSeedRows() {
        assertEquals(17, catalog.all().tables().size());
        assertEquals(204, catalog.all().tables().stream().mapToInt(t -> t.columns().size()).sum());
        assertEquals(53, catalog.all().tables().stream().mapToInt(t -> t.seedCount()).sum());
        assertEquals("CLT", catalog.all().schema());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> catalog.require("CLT.COLLATERAL;DROP TABLE"));
        assertTrue(catalog.all().tables().stream().allMatch(t ->
                CltR1aReadRepository.projection(t).matches("[A-Z0-9_,]+")));
    }
    @Test void privilegedOracleReadersDenied() {
        for (String name : java.util.List.of("SYSTEM", "SYS", "CLT", "DBA")) {
            CltR1aProperties p = new CltR1aProperties(true, "https://issuer.example",
                    "https://issuer.example/jwks", "collateral-read", 3600,
                    "jdbc:oracle:thin:@localhost:1521/PDB", name, "secret");
            assertThrows(IllegalStateException.class, p::requireSafeConfig);
        }
    }
    @Test void noConfiguredReaderIsTheOnlySafeDefault() {
        new CltR1aProperties(false, null, null, null, 3600, null, null, null).requireSafeConfig();
        assertThrows(IllegalStateException.class, () ->
                new CltR1aProperties(true, "http://issuer.invalid", null, null, 3600,
                        null, null, null).requireSafeConfig());
    }
}
