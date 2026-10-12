package com.behsazan.corebanking.fee.admin.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Explicit UAT ONLY. Test-managed transaction rolls back all FEE row changes.
 * Oracle SEQUENCE.NEXTVAL advances are not reversible; run on isolated test DB only.
 * Disabled unless an operator explicitly enables it and pins DB_NAME + CON_NAME.
 */
@SpringJUnitConfig(FeeChargeStageR3B2OracleUatTest.UatConfig.class)
@EnabledIfEnvironmentVariable(named="FEE_R3B2_ORACLE_UAT", matches="YES_ROLLBACK_ONLY")
@Transactional
@Rollback
class FeeChargeStageR3B2OracleUatTest {
    /** Deliberately minimal FEE-only Spring context; no application startup or other schema services. */
    @Configuration(proxyBeanMethods=false)
    @EnableTransactionManagement
    static class UatConfig {
        private static String required(String name) {
            String value = System.getenv(name);
            if (value == null || value.isBlank())
                throw new IllegalStateException(name + " required for isolated Oracle UAT");
            return value;
        }
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource(required("SPRING_DATASOURCE_URL"),
                    required("SPRING_DATASOURCE_USERNAME"), required("SPRING_DATASOURCE_PASSWORD"));
        }
        @Bean PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
        @Bean JdbcClient feeJdbc(DataSource dataSource) { return JdbcClient.create(dataSource); }
        @Bean com.behsazan.corebanking.fee.admin.oracle.FeeAdminRepository feeAdminRepository(JdbcClient jdbc) {
            return new com.behsazan.corebanking.fee.admin.oracle.FeeAdminRepository(jdbc, "FEE");
        }
        @Bean FeeAdminService feeAdminService(com.behsazan.corebanking.fee.admin.oracle.FeeAdminRepository repository) {
            return new FeeAdminService(repository);
        }
    }

    @Autowired JdbcClient jdbc;
    @Autowired FeeAdminService service;
    private final String prefix = "R3B2_UAT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    private boolean fixturesStarted = false;
    private Long versionAId;
    private Long versionBId;

    private void verifyExplicitDatabaseIdentity() {
        String expectedDb = System.getenv("FEE_R3B2_EXPECTED_DB_NAME");
        String expectedContainer = System.getenv("FEE_R3B2_EXPECTED_CON_NAME");
        if (expectedDb == null || expectedDb.isBlank() || expectedContainer == null || expectedContainer.isBlank()) {
            throw new IllegalStateException("Provide FEE_R3B2_EXPECTED_DB_NAME and FEE_R3B2_EXPECTED_CON_NAME; no writes permitted.");
        }
        Map<String, Object> id = jdbc.sql("SELECT SYS_CONTEXT('USERENV','DB_NAME') DB, "
                + "SYS_CONTEXT('USERENV','CON_NAME') CON_NAME FROM DUAL").query((rs, i) ->
                Map.<String, Object>of("DB", rs.getString("DB"), "CON_NAME", rs.getString("CON_NAME"))).single();
        if (!expectedDb.equalsIgnoreCase(String.valueOf(id.get("DB")))
                || !expectedContainer.equalsIgnoreCase(String.valueOf(id.get("CON_NAME")))) {
            throw new IllegalStateException("Oracle fingerprint mismatch; refused UAT writes. Actual " + id);
        }
        Long feeOwner = jdbc.sql("SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='FEE' AND TABLE_NAME='FEE_CHARGE_STAGE'")
                .query(Long.class).single();
        if (feeOwner == null || feeOwner != 1) throw new IllegalStateException("FEE schema not available; no writes.");
    }

    private long next(String sequence) {
        // Only constant allowlisted names are used; not derived from external input.
        return jdbc.sql("SELECT FEE." + sequence + ".NEXTVAL FROM DUAL").query(Long.class).single();
    }
    private long countStage(int no, long version) {
        return jdbc.sql("SELECT COUNT(*) FROM FEE.FEE_CHARGE_STAGE WHERE FEE_DEFINITION_VERSION_ID=:v AND STAGE_NO=:n")
                .param("v", version).param("n", no).query(Long.class).single();
    }
    private Map<String, Object> stage(long version, long rule, int no, Long component) {
        Map<String, Object> values = new HashMap<>();
        values.put("FEE_DEFINITION_VERSION_ID", version);
        values.put("CALCULATION_RULE_ID", rule);
        values.put("CHARGE_COMPONENT_ID", component);
        values.put("STAGE_NO", no);
        values.put("STAGE_CODE", prefix + "_" + no);
        values.put("NAME_FA", "مرحله آزمایشی بازگشت تراکنش");
        values.put("TRIGGER_CODE", "UAT_EVENT");
        return values;
    }

    @Test void validWritesAndInvalidLinksAreIsolatedInRolledBackTransaction() {
        verifyExplicitDatabaseIdentity(); // MUST run before any FEE DML or sequence NEXTVAL.
        Long policy = jdbc.sql("SELECT POLICY_VERSION_ID FROM FEE.FEE_POLICY_VERSION FETCH FIRST 1 ROWS ONLY")
                .query(Long.class).optional().orElseThrow(() -> new IllegalStateException("FEE policy seed missing"));
        fixturesStarted = true;
        long feature = next("SEQ_FEE_FEATURE");
        jdbc.sql("INSERT INTO FEE.FEE_FEATURE (FEE_FEATURE_ID, FEATURE_CODE, NAME_FA, FEE_TYPE_CODE, CREATED_BY) "
                + "VALUES (:id,:code,'R3B2 UAT feature','SERVICE','r3b2-uat')")
                .param("id", feature).param("code", prefix).update();
        long definition = next("SEQ_FEE_DEFINITION");
        jdbc.sql("INSERT INTO FEE.FEE_DEFINITION (FEE_DEFINITION_ID,FEE_FEATURE_ID,FEE_CODE,NAME_FA,CREATED_BY) "
                + "VALUES (:id,:feature,:code,'R3B2 UAT definition','r3b2-uat')")
                .param("id", definition).param("feature", feature).param("code", prefix).update();
        long versionA = next("SEQ_FEE_DEFINITION_VERSION");
        long versionB = next("SEQ_FEE_DEFINITION_VERSION");
        versionAId = versionA;
        versionBId = versionB;
        for (long version : new long[]{versionA, versionB}) {
            jdbc.sql("INSERT INTO FEE.FEE_DEFINITION_VERSION "
                    + "(FEE_DEFINITION_VERSION_ID,FEE_DEFINITION_ID,POLICY_VERSION_ID,VERSION_NO,EFFECTIVE_FROM,CREATED_BY) "
                    + "VALUES (:id,:definition,:policy,:number,TRUNC(SYSDATE),'r3b2-uat')")
                    .param("id", version).param("definition", definition).param("policy", policy)
                    .param("number", version == versionA ? "R3B2-A" : "R3B2-B").update();
        }
        long ruleA = next("SEQ_FEE_CALCULATION_RULE");
        long ruleB = next("SEQ_FEE_CALCULATION_RULE");
        for (long rule : new long[]{ruleA, ruleB}) {
            jdbc.sql("INSERT INTO FEE.FEE_CALCULATION_RULE "
                    + "(CALCULATION_RULE_ID,FEE_DEFINITION_VERSION_ID,RULE_CODE,NAME_FA,CALCULATION_STRATEGY_CODE,BASIS_TYPE_CODE,FIXED_AMOUNT,CREATED_BY) "
                    + "VALUES (:id,:v,:code,'R3B2 UAT rule','FIXED','AMOUNT',100,'r3b2-uat')")
                    .param("id", rule).param("v", rule == ruleA ? versionA : versionB)
                    .param("code", prefix + (rule == ruleA ? "_A" : "_B")).update();
        }
        long componentB = next("SEQ_FEE_CHARGE_COMPONENT");
        jdbc.sql("INSERT INTO FEE.FEE_CHARGE_COMPONENT "
                + "(CHARGE_COMPONENT_ID,FEE_DEFINITION_VERSION_ID,COMPONENT_CODE,NAME_FA,COMPONENT_TYPE_CODE,SEQUENCE_NO,CREATED_BY) "
                + "VALUES (:id,:v,:code,'R3B2 UAT component','BASE',1,'r3b2-uat')")
                .param("id", componentB).param("v", versionB).param("code", prefix).update();

        assertThat(countStage(1, versionA)).isZero();
        Map<String, Object> created = service.create("FEE_CHARGE_STAGE", stage(versionA, ruleA, 1, null), "r3b2-uat");
        long stageId = ((Number) created.get("CHARGE_STAGE_ID")).longValue();
        assertThat(countStage(1, versionA)).isEqualTo(1);
        assertThat(created.get("CHARGE_COMPONENT_ID")).isNull();

        assertThatThrownBy(() -> service.create("FEE_CHARGE_STAGE", stage(versionA, ruleB, 2, null), "r3b2-uat"))
                .isInstanceOf(FeeAdminValidationException.class).hasMessageContaining("قاعده");
        assertThat(countStage(2, versionA)).isZero();
        assertThatThrownBy(() -> service.create("FEE_CHARGE_STAGE", stage(versionA, ruleA, 3, componentB), "r3b2-uat"))
                .isInstanceOf(FeeAdminValidationException.class).hasMessageContaining("مؤلفه");
        assertThat(countStage(3, versionA)).isZero();
        assertThatThrownBy(() -> service.update("FEE_CALCULATION_RULE", ruleA,
                Map.of("FEE_DEFINITION_VERSION_ID", versionB), "r3b2-uat"))
                .isInstanceOf(FeeAdminValidationException.class);
        assertThat(jdbc.sql("SELECT FEE_DEFINITION_VERSION_ID FROM FEE.FEE_CALCULATION_RULE WHERE CALCULATION_RULE_ID=:id")
                .param("id", ruleA).query(Long.class).single()).isEqualTo(versionA);
        assertThatThrownBy(() -> service.update("FEE_CHARGE_STAGE", stageId,
                Map.of("CALCULATION_RULE_ID", ruleB), "r3b2-uat"))
                .isInstanceOf(FeeAdminValidationException.class);
        assertThat(jdbc.sql("SELECT CALCULATION_RULE_ID FROM FEE.FEE_CHARGE_STAGE WHERE CHARGE_STAGE_ID=:id")
                .param("id", stageId).query(Long.class).single()).isEqualTo(ruleA);
        System.out.println("FEE_R3B2_ORACLE_TRANSACTION_ASSERTIONS_PASS (pending @Rollback / @AfterTransaction)");
    }

    /** Executes AFTER the test transaction rolled back; no test-owned records may persist. */
    @AfterTransaction void assertNoFixtureSurvivedRollback() {
        if (!fixturesStarted) return;
        Map<String, String> tables = Map.of(
                "FEE_FEATURE", "FEATURE_CODE", "FEE_DEFINITION", "FEE_CODE",
                "FEE_CALCULATION_RULE", "RULE_CODE", "FEE_CHARGE_COMPONENT", "COMPONENT_CODE",
                "FEE_CHARGE_STAGE", "STAGE_CODE");
        for (var entry : tables.entrySet()) {
            // Table/column names are hardcoded, internal, non-user-controllable.
            long remains = jdbc.sql("SELECT COUNT(*) FROM FEE." + entry.getKey()
                    + " WHERE " + entry.getValue() + " LIKE :prefix")
                    .param("prefix", prefix + "%").query(Long.class).single();
            assertThat(remains).as("No rollback residue in " + entry.getKey()).isZero();
        }
        if (versionAId != null && versionBId != null) {
            long remainingVersions = jdbc.sql("SELECT COUNT(*) FROM FEE.FEE_DEFINITION_VERSION "
                    + "WHERE FEE_DEFINITION_VERSION_ID IN (:a,:b)")
                    .param("a", versionAId).param("b", versionBId).query(Long.class).single();
            assertThat(remainingVersions).as("No test-owned fee versions survive rollback").isZero();
        }
        System.out.println("FEE_R3B2_ORACLE_ROLLBACK_VERIFIED | no test-owned records remain");
    }
}
