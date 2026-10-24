package com.behsazan.corebanking.calendar2.personalv2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** In-app alerts are made available by the backend even when all browsers are closed. */
@Configuration(proxyBeanMethods=false)
@EnableScheduling
@ConditionalOnProperty(name="core-banking.calendar2.personal.r2b2.scheduler-enabled",havingValue="true")
public class PersonalV2Scheduler {
    private static final Logger LOG=LoggerFactory.getLogger(PersonalV2Scheduler.class);
    private final PersonalV2Service service;
    public PersonalV2Scheduler(PersonalV2Service service){this.service=service;}
    @Scheduled(fixedDelayString="${core-banking.calendar2.personal.r2b2.scheduler-interval-ms:60000}",initialDelay=10000)
    public void run() {
        try {int count=service.reconcile();if(count>0)LOG.info("CAL2 R2B2 created {} occurrence slots",count);}
        catch(Exception ex){LOG.error("CAL2 R2B2 reconciliation failed; alerts remain durable in Oracle",ex);}
    }
}
