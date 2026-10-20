package com.behsazan.corebanking.organization.grade;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GradeActorResolverTest {
    @Test void prototypeLabelNeedsNoRoles() {
        assertThat(new GradeActorResolver().actor()).isEqualTo("PROTOTYPE");
    }
}
