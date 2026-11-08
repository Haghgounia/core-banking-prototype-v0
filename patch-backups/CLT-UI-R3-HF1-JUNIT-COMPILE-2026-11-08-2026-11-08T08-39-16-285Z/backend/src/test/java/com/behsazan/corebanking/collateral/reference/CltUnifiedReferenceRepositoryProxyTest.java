package com.behsazan.corebanking.collateral.reference;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the actual Spring class-based proxy creation that failed in CLT Unified R1 startup. */
class CltUnifiedReferenceRepositoryProxyTest {
    @Test
    void repositoryIsCompatibleWithSpringCglibProxy() {
        ProxyFactory factory = new ProxyFactory();
        factory.setTargetClass(CltUnifiedReferenceRepository.class);
        factory.setProxyTargetClass(true);
        factory.addAdvice((MethodInterceptor) invocation -> invocation.proceed());
        Object proxy = assertDoesNotThrow(factory::getProxy);
        assertTrue(AopUtils.isCglibProxy(proxy), "CLT Repository must support class-based Spring AOP");
    }
}
