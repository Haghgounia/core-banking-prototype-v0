package com.behsazan.corebanking.posting.readiness;

import java.util.List;

/**
 * R1-D1 static expectation catalog. Names come from the user-provided Oracle DDL
 * plus the verified 48-table metadata report and successful R1-B2 migration log.
 * This is not a substitute for live dictionary verification.
 */
public final class PostingReadinessCatalog {
    private PostingReadinessCatalog() {}

    public static final List<String> EXPECTED_TABLES = List.of(
            "PST.POSTING_ACCOUNT_RESOLUTIONS",
            "PST.POSTING_APPROVALS",
            "PST.POSTING_EXECUTIONS",
            "PST.POSTING_EXECUTION_EVENTS",
            "PST.POSTING_INBOX_RECEIPTS",
            "PST.POSTING_INSTRUCTIONS",
            "PST.POSTING_OUTBOX_EVENTS",
            "PST.POSTING_PLANS",
            "PST.POSTING_PLAN_MOVEMENTS",
            "PST.POSTING_PREFLIGHT_RESULTS",
            "PST.POSTING_RULES",
            "PST.POSTING_RULE_CONDITIONS",
            "PST.POSTING_RULE_TEMPLATE_ASSIGNMENTS",
            "PST.POSTING_RULE_VERSIONS",
            "PST.POSTING_TEMPLATES",
            "PST.POSTING_TEMPLATE_LINES",
            "PST.POSTING_TEMPLATE_VERSIONS",
            "LGR.ACCOUNT_BINDINGS",
            "LGR.ASSETS",
            "LGR.COMMIT_RECEIPTS",
            "LGR.LEDGERS",
            "LGR.LEDGER_MOVEMENTS",
            "LGR.LEDGER_OUTBOX_EVENTS",
            "LGR.LEDGER_TRANSACTIONS",
            "LGR.POSITION_ACCOUNTS",
            "LGR.POSITION_BALANCES",
            "LGR.RESERVATIONS",
            "ACC.ACCOUNTING_APPROVALS",
            "ACC.ACCOUNTING_BOOKS",
            "ACC.ACCOUNTING_ENTRIES",
            "ACC.ACCOUNTING_FACTS",
            "ACC.ACCOUNTING_MAPPING_ENTRIES",
            "ACC.ACCOUNTING_MAPPING_VERSIONS",
            "ACC.ACCOUNTING_OUTBOX_EVENTS",
            "ACC.ACCOUNTING_PLANS",
            "ACC.ACCOUNTING_TRANSACTIONS",
            "ACC.ACCOUNT_BALANCE_PROJECTIONS",
            "ACC.ACCOUNT_DIMENSIONS",
            "ACC.ACCOUNT_DIMENSION_ASSIGNMENTS",
            "ACC.ACCOUNT_ROLE_ASSIGNMENTS",
            "ACC.CHARTS_OF_ACCOUNTS",
            "ACC.FISCAL_PERIODS",
            "ACC.GENERAL_LEDGER_ACCOUNTS",
            "ACC.JOURNAL_BATCHES",
            "RCN.RECONCILIATION_EVIDENCE_ITEMS",
            "RCN.RECONCILIATION_EXCEPTIONS",
            "RCN.RECONCILIATION_RESULTS",
            "RCN.RECONCILIATION_RULE_VERSIONS",
            "RCN.RECONCILIATION_RUNS",
            "RCN.RESOLUTION_APPROVALS"
    );

    public static final List<String> NEW_SEQUENCES = List.of(
            "PST.SEQ_POSTING_RULE_TEMPLATE_ASSIGNMENTS",
            "ACC.SEQ_ACCOUNT_ROLE_ASSIGNMENTS"
    );

    public static final List<String> NEW_INDEXES = List.of(
            "PST.UNIQUE_POSTING_RULE_TEMPLATE_ASSIGNMENTS_START",
            "PST.IX_POSTING_RULE_TEMPLATE_ASSIGNMENTS_LOOKUP",
            "ACC.UNIQUE_ACCOUNT_ROLE_ASSIGNMENTS_SCOPE_START",
            "ACC.IX_ACCOUNT_ROLE_ASSIGNMENTS_LOOKUP"
    );

    public static final List<String> NEW_CONSTRAINTS = List.of(
            "PST.PK_POSTING_RULE_TEMPLATE_ASSIGNMENTS",
            "PST.FK_POSTING_RULE_TEMPLATE_ASSIGNMENTS_RULE",
            "PST.FK_POSTING_RULE_TEMPLATE_ASSIGNMENTS_TEMPLATE",
            "PST.CK_POSTING_RULE_TEMPLATE_ASSIGNMENTS_DATES",
            "ACC.PK_ACCOUNT_ROLE_ASSIGNMENTS",
            "ACC.FK_ACCOUNT_ROLE_ASSIGNMENTS_MAPPING",
            "ACC.FK_ACCOUNT_ROLE_ASSIGNMENTS_BOOK",
            "ACC.FK_ACCOUNT_ROLE_ASSIGNMENTS_LEDGER_ACCOUNT",
            "ACC.CK_ACCOUNT_ROLE_ASSIGNMENTS_DATES"
    );

    public static final List<String> CONFIGURATION_TABLES = List.of(
            "PST.POSTING_RULE_VERSIONS",
            "PST.POSTING_TEMPLATE_VERSIONS",
            "PST.POSTING_RULE_TEMPLATE_ASSIGNMENTS",
            "ACC.ACCOUNTING_BOOKS",
            "ACC.CHARTS_OF_ACCOUNTS",
            "ACC.GENERAL_LEDGER_ACCOUNTS",
            "ACC.ACCOUNTING_MAPPING_VERSIONS",
            "ACC.ACCOUNT_ROLE_ASSIGNMENTS",
            "ACC.FISCAL_PERIODS",
            "LGR.ASSETS",
            "LGR.LEDGERS",
            "LGR.POSITION_ACCOUNTS",
            "LGR.ACCOUNT_BINDINGS"
    );

    public static boolean isWhitelistedTable(String qualifiedName) {
        return EXPECTED_TABLES.contains(qualifiedName);
    }
}
