package com.behsazan.corebanking.collateral.reference;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class CollateralReferenceCatalogTest {
    private final CollateralReferenceCatalog catalog = new CollateralReferenceCatalog(new JsonMapper());

    @Test void catalogsAll17PhysicalReferenceTablesWith53TechnicalSeedRows() {
        assertEquals(17, catalog.all().tables().size());
        assertEquals(204, catalog.all().tables().stream().mapToInt(t -> t.columns().size()).sum());
        assertEquals(53, catalog.all().tables().stream().mapToInt(t -> t.seedCount()).sum());
        assertEquals("CLT", catalog.all().schema());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> catalog.require("CLT.COLLATERAL;DROP TABLE"));
        assertTrue(catalog.all().tables().stream().allMatch(t ->
                CollateralReferenceRepository.projection(t).matches("[A-Z0-9_,]+")));
    }
    @Test void privilegedOracleReadersDenied() {
        for (String name : java.util.List.of("SYSTEM", "SYS", "CLT", "DBA")) {
            CollateralReferenceProperties p = new CollateralReferenceProperties(true, "https://issuer.example",
                    "https://issuer.example/jwks", "collateral-read", 3600,
                    "jdbc:oracle:thin:@localhost:1521/PDB", name, "secret", false, null, null, null);
            assertThrows(IllegalStateException.class, p::requireSafeConfig);
        }
    }
    @Test void noConfiguredReaderIsTheOnlySafeDefault() {
        new CollateralReferenceProperties(false, null, null, null, 3600, null, null, null, false, null, null, null).requireSafeConfig();
        assertThrows(IllegalStateException.class, () ->
                new CollateralReferenceProperties(true, "http://issuer.invalid", null, null, 3600,
                        null, null, null, false, null, null, null).requireSafeConfig());
    }
}
