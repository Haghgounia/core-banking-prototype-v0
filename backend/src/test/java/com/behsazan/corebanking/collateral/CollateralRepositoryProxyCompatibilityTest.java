package com.behsazan.corebanking.collateral;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionRepository;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceRepository;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryRepository;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards against Spring exception-translation CGLIB failures at startup. */
class CollateralRepositoryProxyCompatibilityTest {
    @Test
    void repositoryBeansAreSubclassableForSpringAop() {
        for (Class<?> beanType : List.of(
                CollateralIntroductionRepository.class,
                CollateralRegistryRepository.class,
                CollateralReferenceRepository.class)) {
            assertTrue(beanType.isAnnotationPresent(Repository.class),
                    () -> beanType.getName() + " must remain a Spring repository");
            assertFalse(Modifier.isFinal(beanType.getModifiers()),
                    () -> beanType.getName() + " is final and cannot be CGLIB-proxied");
        }
    }
}
