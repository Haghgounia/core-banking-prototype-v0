package com.behsazan.corebanking.cif.reference.oracle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartyReferenceKeyCodecTest {
    @Test
    void roundTripsSingleAndCompositeKeys() {
        String single = PartyReferenceKeyCodec.encode(List.of("MALE"));
        assertThat(PartyReferenceKeyCodec.decode(single, 1)).containsExactly("MALE");

        String composite = PartyReferenceKeyCodec.encode(List.of("CUSTOMER_TIER", "RETAIL"));
        assertThat(PartyReferenceKeyCodec.decode(composite, 2)).containsExactly("CUSTOMER_TIER", "RETAIL");
    }
}
