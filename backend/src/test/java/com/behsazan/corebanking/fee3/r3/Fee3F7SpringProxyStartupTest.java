package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1JalaliDayRepository;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Regression for F7 production-startup CGLIB failure (2026-10-31).
 *
 * The repository receives Spring persistence-exception translation advice and
 * the service receives read-only transaction advice. Both must permit class-
 * based proxy generation. No database or Spring Boot application startup is
 * required for this narrow regression test.
 */
class Fee3F7SpringProxyStartupTest {
    private static <T> void verifyClassProxy(T target, Class<T> expectedType) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice((MethodInterceptor) invocation -> invocation.proceed());
        Object proxy = factory.getProxy();
        assertInstanceOf(expectedType, proxy);
        assertTrue(AopUtils.isCglibProxy(proxy),
            "F7 advisor must be able to create a class-based Spring proxy");
    }

    @Test
    void cal2RepositorySupportsSpringExceptionTranslationProxy() {
        var repository = new Fee3F7Cal2WorkdayRepository(mock(JdbcClient.class), "CAL2");
        verifyClassProxy(repository, Fee3F7Cal2WorkdayRepository.class);
    }

    @Test
    void lateDamageServiceSupportsSpringTransactionProxy() {
        var service = new Fee3F7LateDamageService(
            mock(Fee3R4C1JalaliDayRepository.class),
            new Fee3F7Cal2WorkdayRepository(mock(JdbcClient.class), "CAL2"));
        verifyClassProxy(service, Fee3F7LateDamageService.class);
    }
}
