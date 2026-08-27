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


    @Test
    void treatsEaTimestampZeroAndOracleTimestampSixAsCompatible() {
        var ea = new EaTableDefinition(
                "AUDIT_SAMPLE", 1,
                List.of(new EaColumnDefinition("CREATED_AT", "TIMESTAMP", 0, 0, 0, false, null, null)),
                List.of()
        );
        var db = new OracleTableDefinition(
                "AUDIT_SAMPLE",
                List.of(new OracleColumnDefinition("CREATED_AT", "TIMESTAMP(6)", 11, 0, null, null, 6, false, 1)),
                List.of(), 0L, null
        );

        var result = EaOracleComparisonService.compareTable(ea, db);

        assertThat(result.status()).isEqualTo(EaOracleComparisonModels.TableStatus.MATCH);
        assertThat(result.columns().getFirst().status()).isEqualTo(EaOracleComparisonModels.ColumnStatus.MATCH);
        assertThat(result.columns().getFirst().eaDefinition()).isEqualTo("TIMESTAMP(0)");
        assertThat(result.columns().getFirst().databaseDefinition()).isEqualTo("TIMESTAMP(6)");
        assertThat(result.columns().getFirst().differences()).isEmpty();
    }

    @Test
    void stillReportsOtherTimestampPrecisionDifferences() {
        var ea = new EaTableDefinition(
                "AUDIT_SAMPLE", 1,
                List.of(new EaColumnDefinition("CREATED_AT", "TIMESTAMP", 3, 0, 0, false, null, null)),
                List.of()
        );
        var db = new OracleTableDefinition(
                "AUDIT_SAMPLE",
                List.of(new OracleColumnDefinition("CREATED_AT", "TIMESTAMP(6)", 11, 0, null, null, 6, false, 1)),
                List.of(), 0L, null
        );

        var result = EaOracleComparisonService.compareTable(ea, db);

        assertThat(result.status()).isEqualTo(EaOracleComparisonModels.TableStatus.DIFFERENT);
        assertThat(result.columns().getFirst().differences()).anyMatch(value -> value.contains("Fractional seconds"));
    }

    @Test
    void comparesForeignKeysAndCheckConstraintsAsPartOfTableStatus() {
        var ea = new EaTableDefinition(
                "CHILD", 1, null, null,
                List.of(
                        new EaColumnDefinition("CHILD_ID", "NUMBER", 0, 19, 0, false, null, null),
                        new EaColumnDefinition("PARENT_ID", "NUMBER", 0, 19, 0, false, null, null)
                ),
                List.of("CHILD_ID"),
                List.of(new EaForeignKeyDefinition("FK_CHILD_PARENT", List.of("PARENT_ID"), "PARENT", List.of("PARENT_ID"))),
                List.of(new EaCheckConstraintDefinition("CK_CHILD_ID", "CHILD_ID > 0"))
        );
        var dbFk = new OracleEaForeignKey(
                "FK_CHILD_PARENT", "APP", "CHILD", "APP", "PARENT", "PK_PARENT", "P",
                "NO ACTION", "ENABLED", "NOT DEFERRABLE", "IMMEDIATE",
                List.of(new OracleEaForeignKeyColumn("PARENT_ID", "PARENT_ID", 1))
        );
        var db = new OracleTableDefinition(
                "CHILD", null,
                List.of(
                        new OracleColumnDefinition("CHILD_ID", "NUMBER", 22, 0, null, 19, 0, false, 1),
                        new OracleColumnDefinition("PARENT_ID", "NUMBER", 22, 0, null, 19, 0, false, 2)
                ),
                List.of("CHILD_ID"),
                List.of(dbFk),
                List.of(new OracleEaCheckConstraint("CK_CHILD_ID", "\"CHILD_ID\" > 0", "ENABLED")),
                1L,
                null
        );

        var match = EaOracleComparisonService.compareTable(ea, db);
        assertThat(match.status()).isEqualTo(EaOracleComparisonModels.TableStatus.MATCH);
        assertThat(match.foreignKeysMatch()).isTrue();
        assertThat(match.checkConstraintsMatch()).isTrue();
        assertThat(match.foreignKeys()).allMatch(item -> item.status() == EaOracleComparisonModels.ConstraintStatus.MATCH);
        assertThat(match.checkConstraints()).allMatch(item -> item.status() == EaOracleComparisonModels.ConstraintStatus.MATCH);

        var changedDb = new OracleTableDefinition(
                "CHILD", null,
                db.columns(),
                db.primaryKeyColumns(),
                List.of(new OracleEaForeignKey(
                        "FK_CHILD_PARENT", "APP", "CHILD", "APP", "OTHER_PARENT", "PK_OTHER", "P",
                        "NO ACTION", "ENABLED", "NOT DEFERRABLE", "IMMEDIATE",
                        List.of(new OracleEaForeignKeyColumn("PARENT_ID", "PARENT_ID", 1))
                )),
                List.of(new OracleEaCheckConstraint("CK_CHILD_ID", "CHILD_ID >= 0", "ENABLED")),
                1L,
                null
        );
        var different = EaOracleComparisonService.compareTable(ea, changedDb);
        assertThat(different.status()).isEqualTo(EaOracleComparisonModels.TableStatus.DIFFERENT);
        assertThat(different.foreignKeysMatch()).isFalse();
        assertThat(different.checkConstraintsMatch()).isFalse();
    }



    @Test
    void doesNotTreatEaDocumentationAsOraclePersianTableTitle() {
        var ea = new EaTableDefinition(
                "SUB_OPERATION", 1, "ریز عملیات", "جدول مرجع برگرفته از مدل تسهیلات: SUB_OPERATION",
                List.of(new EaColumnDefinition("SUB_OPERATION_ID", "NUMBER", 0, 19, 0, false, null, null, "زیر عملیات شناسه")),
                List.of("SUB_OPERATION_ID")
        );
        var db = new OracleTableDefinition(
                "SUB_OPERATION", "ریز عملیات",
                List.of(new OracleColumnDefinition("SUB_OPERATION_ID", "NUMBER", 22, 0, null, 19, 0, false, 1, "زیر عملیات شناسه")),
                List.of("SUB_OPERATION_ID"), 1L, null
        );

        var result = EaOracleComparisonService.compareTable(ea, db);

        assertThat(result.persianMetadataMatch()).isTrue();
        assertThat(result.tableMetadataDifferences()).isEmpty();
        assertThat(result.status()).isEqualTo(EaOracleComparisonModels.TableStatus.MATCH);
    }



    @Test
    void treatsPrimaryKeyMissingInEaAsTableDifferenceWhenOracleHasOne() {
        var ea = new EaTableDefinition(
                "SAMPLE", 1,
                List.of(new EaColumnDefinition("ID", "NUMBER", 0, 19, 0, false, null, null)),
                List.of()
        );
        var db = new OracleTableDefinition(
                "SAMPLE",
                List.of(new OracleColumnDefinition("ID", "NUMBER", 22, 0, null, 19, 0, false, 1)),
                List.of("ID"), 0L, null
        );

        var result = EaOracleComparisonService.compareTable(ea, db);

        assertThat(result.primaryKeyStatus()).isEqualTo(EaOracleComparisonModels.PrimaryKeyStatus.NOT_DEFINED_IN_EA);
        assertThat(result.status()).isEqualTo(EaOracleComparisonModels.TableStatus.DIFFERENT);
    }


}
