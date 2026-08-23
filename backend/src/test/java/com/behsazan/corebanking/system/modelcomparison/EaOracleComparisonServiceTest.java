package com.behsazan.corebanking.system.modelcomparison;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EaOracleComparisonServiceTest {
    @Test
    void treatsEquivalentOracleShapeAsMatch() {
        var ea = new EaTableDefinition(
                "PARTY",
                1,
                List.of(
                        new EaColumnDefinition("PARTY_ID", "NUMBER", 0, 19, 0, false, null, null),
                        new EaColumnDefinition("PARTY_UID", "VARCHAR2", 36, 0, 0, false, "CHAR", null)
                ),
                List.of("PARTY_ID")
        );
        var db = new OracleTableDefinition(
                "PARTY",
                List.of(
                        new OracleColumnDefinition("PARTY_ID", "NUMBER", 22, 0, null, 19, 0, false, 1),
                        new OracleColumnDefinition("PARTY_UID", "VARCHAR2", 144, 36, "C", null, null, false, 2)
                ),
                List.of("PARTY_ID"),
                25L,
                null
        );

        var result = EaOracleComparisonService.compareTable(ea, db);

        assertThat(result.status()).isEqualTo(EaOracleComparisonModels.TableStatus.MATCH);
        assertThat(result.matchingColumnCount()).isEqualTo(2);
        assertThat(result.rowCount()).isEqualTo(25L);
        assertThat(result.primaryKeyStatus()).isEqualTo(EaOracleComparisonModels.PrimaryKeyStatus.MATCH);
    }

    @Test
    void reportsMissingExtraAndChangedColumnsSeparately() {
        var ea = new EaTableDefinition(
                "PARTY",
                1,
                List.of(
                        new EaColumnDefinition("PARTY_ID", "NUMBER", 0, 19, 0, false, null, null),
                        new EaColumnDefinition("PARTY_UID", "VARCHAR2", 36, 0, 0, false, "CHAR", null),
                        new EaColumnDefinition("PARTY_TYPE_CODE", "VARCHAR2", 30, 0, 0, false, "CHAR", null)
                ),
                List.of("PARTY_ID")
        );
        var db = new OracleTableDefinition(
                "PARTY",
                List.of(
                        new OracleColumnDefinition("PARTY_ID", "NUMBER", 22, 0, null, 19, 0, false, 1),
                        new OracleColumnDefinition("PARTY_UID", "VARCHAR2", 80, 20, "C", null, null, false, 2),
                        new OracleColumnDefinition("DB_ONLY", "VARCHAR2", 40, 10, "C", null, null, true, 3)
                ),
                List.of("PARTY_ID"),
                3L,
                null
        );

        var result = EaOracleComparisonService.compareTable(ea, db);

        assertThat(result.status()).isEqualTo(EaOracleComparisonModels.TableStatus.DIFFERENT);
        assertThat(result.differentColumnCount()).isEqualTo(1);
        assertThat(result.missingColumnCount()).isEqualTo(1);
        assertThat(result.extraColumnCount()).isEqualTo(1);
    }
    @Test
    void normalizesOracleCharSuffixWhenEaDoesNotSpecifyLengthSemantics() {
        var ea = new EaTableDefinition(
                "SAMPLE", 1,
                List.of(new EaColumnDefinition("CODE", "VARCHAR2", 30, 0, 0, false, null, null)),
                List.of()
        );
        var db = new OracleTableDefinition(
                "SAMPLE",
                List.of(new OracleColumnDefinition("CODE", "VARCHAR2", 120, 30, "C", null, null, false, 1)),
                List.of(), 0L, null
        );

        var result = EaOracleComparisonService.compareTable(ea, db);

        assertThat(result.status()).isEqualTo(EaOracleComparisonModels.TableStatus.MATCH);
        assertThat(result.columns().getFirst().eaDefinition()).isEqualTo("VARCHAR2(30)");
        assertThat(result.columns().getFirst().databaseDefinition()).isEqualTo("VARCHAR2(30)");
        assertThat(result.columns().getFirst().differences()).isEmpty();
    }

    @Test
    void keepsExplicitLengthSemanticsMismatchVisible() {
        var ea = new EaTableDefinition(
                "SAMPLE", 1,
                List.of(new EaColumnDefinition("CODE", "VARCHAR2", 30, 0, 0, false, "BYTE", null)),
                List.of()
        );
        var db = new OracleTableDefinition(
                "SAMPLE",
                List.of(new OracleColumnDefinition("CODE", "VARCHAR2", 120, 30, "C", null, null, false, 1)),
                List.of(), 0L, null
        );

        var result = EaOracleComparisonService.compareTable(ea, db);

        assertThat(result.status()).isEqualTo(EaOracleComparisonModels.TableStatus.DIFFERENT);
        assertThat(result.columns().getFirst().eaDefinition()).isEqualTo("VARCHAR2(30 BYTE)");
        assertThat(result.columns().getFirst().databaseDefinition()).isEqualTo("VARCHAR2(30 CHAR)");
        assertThat(result.columns().getFirst().differences()).anyMatch(value -> value.contains("Length semantics"));
    }

    @Test
    void includesPersianTableAndColumnCommentsInMatchDecision() {
        var ea = new EaTableDefinition(
                "SAMPLE", 1, "نمونه", "جدول نمونه برای آزمون",
                List.of(new EaColumnDefinition("CODE", "VARCHAR2", 30, 0, 0, false, "CHAR", null, "کد نمونه")),
                List.of()
        );
        var matchingDb = new OracleTableDefinition(
                "SAMPLE", "جدول نمونه برای آزمون",
                List.of(new OracleColumnDefinition("CODE", "VARCHAR2", 120, 30, "C", null, null, false, 1, "کد نمونه")),
                List.of(), 1L, null
        );

        var match = EaOracleComparisonService.compareTable(ea, matchingDb);
        assertThat(match.status()).isEqualTo(EaOracleComparisonModels.TableStatus.MATCH);
        assertThat(match.persianMetadataMatch()).isTrue();
        assertThat(match.columns().getFirst().status()).isEqualTo(EaOracleComparisonModels.ColumnStatus.MATCH);

        var differentDb = new OracleTableDefinition(
                "SAMPLE", "شرح متفاوت جدول",
                List.of(new OracleColumnDefinition("CODE", "VARCHAR2", 120, 30, "C", null, null, false, 1, "شرح متفاوت ستون")),
                List.of(), 1L, null
        );
        var different = EaOracleComparisonService.compareTable(ea, differentDb);
        assertThat(different.status()).isEqualTo(EaOracleComparisonModels.TableStatus.DIFFERENT);
        assertThat(different.persianMetadataMatch()).isFalse();
        assertThat(different.tableMetadataDifferences()).isNotEmpty();
        assertThat(different.columns().getFirst().differences()).anyMatch(value -> value.contains("COMMENT فارسی ستون"));
    }

    @Test
    void normalizesCommonPersianArabicVariantsAndSpacingInComments() {
        var ea = new EaTableDefinition(
                "SAMPLE", 1, "طبقه‌بندی پارتی", "اطلاعات طبقه‌بندی پارتی",
                List.of(new EaColumnDefinition("CODE", "VARCHAR2", 30, 0, 0, false, null, null, "كد پارتي")),
                List.of()
        );
        var db = new OracleTableDefinition(
                "SAMPLE", "اطلاعات طبقه بندی پارتی",
                List.of(new OracleColumnDefinition("CODE", "VARCHAR2", 120, 30, "C", null, null, false, 1, "کد پارتی")),
                List.of(), 1L, null
        );

        var result = EaOracleComparisonService.compareTable(ea, db);
        assertThat(result.status()).isEqualTo(EaOracleComparisonModels.TableStatus.MATCH);
    }

}
