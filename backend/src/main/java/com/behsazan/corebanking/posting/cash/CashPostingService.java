package com.behsazan.corebanking.posting.cash;

import com.behsazan.corebanking.posting.cash.CashPostingPolicy.GlLine;
import com.behsazan.corebanking.posting.cash.CashPostingPolicy.Instruction;
import com.behsazan.corebanking.posting.cash.CashPostingPolicy.Plan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * R1-D5 opt-in cash slice. One Oracle transaction atomically commits DPS2, PST,
 * LGR and ACC. This is NOT a Kafka consumer or a production GL integration.
 * All chart, posting rule, mapping, fiscal and position data must pre-exist.
 */
@Service
@ConditionalOnProperty(prefix="core-banking.posting.cash", name="enabled", havingValue="true")
public final class CashPostingService {
    private final NamedParameterJdbcTemplate db;
    @Value("${core-banking.posting.cash.book-code:}") private String bookCode;
    @Value("${core-banking.posting.cash.legal-entity:}") private String legalEntity;
    @Value("${core-banking.posting.cash.ledger-code:}") private String ledgerCode;
    @Value("${core-banking.posting.cash.chart-code:}") private String chartCode;
    @Value("${core-banking.posting.cash.rule-deposit-code:}") private String depositRule;
    @Value("${core-banking.posting.cash.rule-withdrawal-code:}") private String withdrawalRule;
    @Value("${core-banking.posting.cash.mapping-deposit-code:}") private String depositMapping;
    @Value("${core-banking.posting.cash.mapping-withdrawal-code:}") private String withdrawalMapping;

    public CashPostingService(NamedParameterJdbcTemplate db) { this.db = Objects.requireNonNull(db); }

    private record Resolved(long bookId, long periodId, long ledgerId, long ruleVersionId,
            long templateVersionId, long mappingVersionId,
            String sourcePosition, String destinationPosition,
            long sourcePositionId, long destinationPositionId,
            Map<String,Long> glAccounts) {}

    /** Called only inside DepositTransactionService.post's existing writable transaction. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void commit(Instruction instruction) {
        if (!TransactionSynchronizationManager.isActualTransactionActive() ||
                TransactionSynchronizationManager.isCurrentTransactionReadOnly())
            throw new IllegalStateException("R1-D5 requires an existing writable DPS2 transaction");
        requireConfiguration();
        int scale = currencyScale(instruction.currency());
        Plan plan = CashPostingPolicy.make(instruction, scale);
        String sourceEvent = "DPS2-TX-" + instruction.depositTransactionId();
        List<Map<String,Object>> duplicates = rows("SELECT IDENTIFIER, PAYLOAD_HASH, EXECUTION_STATE FROM " +
                "PST.POSTING_INSTRUCTIONS WHERE SOURCE_DOMAIN='DPS2' AND SOURCE_EVENT_IDENTIFIER=:event " +
                "AND ACCOUNTING_PURPOSE=:purpose", p("event",sourceEvent,"purpose",CashPostingPolicy.PURPOSE));
        if (!duplicates.isEmpty()) {
            if (duplicates.size() == 1 && plan.payloadHash().equals(s(duplicates.getFirst(),"PAYLOAD_HASH")) &&
                    "COMMITTED".equals(s(duplicates.getFirst(),"EXECUTION_STATE"))) return;
            throw new IllegalStateException("Duplicate or conflicting cash posting instruction");
        }
        Resolved cfg = resolve(instruction, plan);
        long instructionId = seq("PST.SEQ_POSTING_INSTRUCTIONS");
        long planId = seq("PST.SEQ_POSTING_PLANS");
        long executionId = seq("PST.SEQ_POSTING_EXECUTIONS");
        long ledgerTxId = seq("LGR.SEQ_LEDGER_TRANSACTIONS");
        long factId = seq("ACC.SEQ_ACCOUNTING_FACTS");
        long accTxId = seq("ACC.SEQ_ACCOUNTING_TRANSACTIONS");
        String dim = "{\"sourceDomain\":\"DPS2\",\"productVersion\":\"" +
                safeDimension(instruction.productVersion()) + "\",\"bankUnit\":\"" +
                safeDimension(instruction.bankUnit()) + "\"}";
        String idem = "DPS2:" + instruction.depositTransactionId() + ":CASH_PRINCIPAL";

        update("INSERT INTO PST.POSTING_INBOX_RECEIPTS(SOURCE_DOMAIN,SOURCE_EVENT_IDENTIFIER,ACCOUNTING_PURPOSE,PAYLOAD_HASH,RECEIVED_AT) " +
                "VALUES('DPS2',:event,:purpose,:hash,SYSTIMESTAMP)", p("event",sourceEvent,"purpose",CashPostingPolicy.PURPOSE,"hash",plan.payloadHash()));
        update("INSERT INTO PST.POSTING_INSTRUCTIONS(IDENTIFIER,SOURCE_DOMAIN,SOURCE_EVENT_IDENTIFIER,ACCOUNTING_PURPOSE," +
                "SOURCE_BUSINESS_REFERENCE,IDEMPOTENCY_KEY,PAYLOAD_HASH,EXECUTION_STATE,RECEIVED_AT) " +
                "VALUES(:id,'DPS2',:event,:purpose,:ref,:idem,:hash,'RECEIVED',SYSTIMESTAMP)",
                p("id",instructionId,"event",sourceEvent,"purpose",CashPostingPolicy.PURPOSE,
                        "ref",plan.reference(),"idem",idem,"hash",plan.payloadHash()));
        update("INSERT INTO PST.POSTING_PLANS(IDENTIFIER,POSTING_INSTRUCTION_IDENTIFIER,POSTING_RULE_VERSION_IDENTIFIER," +
                "POSTING_TEMPLATE_VERSION_IDENTIFIER,ACCOUNTING_PURPOSE,REVISION_NUMBER,PLAN_HASH,PUBLICATION_STATE,CREATED_AT) " +
                "VALUES(:id,:ins,:rule,:template,:purpose,1,:hash,'APPROVED',SYSTIMESTAMP)",
                p("id",planId,"ins",instructionId,"rule",cfg.ruleVersionId(),"template",cfg.templateVersionId(),
                        "purpose",CashPostingPolicy.PURPOSE,"hash",plan.payloadHash()));
        update("INSERT INTO PST.POSTING_PLAN_MOVEMENTS(IDENTIFIER,POSTING_PLAN_IDENTIFIER,SEQUENCE_NUMBER," +
                "SOURCE_POSITION_ACCOUNT_REFERENCE,DESTINATION_POSITION_ACCOUNT_REFERENCE,AMOUNT,CURRENCY_CODE) " +
                "VALUES(:id,:plan,1,:source,:dest,:amount,:currency)",
                p("id",seq("PST.SEQ_POSTING_PLAN_MOVEMENTS"),"plan",planId,"source",cfg.sourcePosition(),
                        "dest",cfg.destinationPosition(),"amount",plan.amountUnits(),"currency",instruction.currency()));
        for (int i=0;i<2;i++) {
            String role = i==0?plan.sourcePositionRole():plan.destinationPositionRole();
            update("INSERT INTO PST.POSTING_ACCOUNT_RESOLUTIONS(IDENTIFIER,POSTING_PLAN_IDENTIFIER,ACCOUNT_ROLE," +
                    "POSITION_ACCOUNT_REFERENCE,ACCOUNTING_MAPPING_VERSION_REFERENCE,VALID_AT) " +
                    "VALUES(:id,:plan,:role,:position,:mapping,SYSTIMESTAMP)",
                    p("id",seq("PST.SEQ_POSTING_ACCOUNT_RESOLUTIONS"),"plan",planId,"role",role,
                            "position",i==0?cfg.sourcePosition():cfg.destinationPosition(),
                            "mapping",Long.toString(cfg.mappingVersionId())));
        }
        update("INSERT INTO PST.POSTING_PREFLIGHT_RESULTS(IDENTIFIER,POSTING_PLAN_IDENTIFIER,ACCOUNTING_MAPPING_VERSION_REFERENCE," +
                "ACCOUNTING_PERIOD_REFERENCE,VALIDATION_STATE,VALID_UNTIL) " +
                "VALUES(:id,:plan,:mapping,:period,'PASS',NULL)",
                p("id",seq("PST.SEQ_POSTING_PREFLIGHT_RESULTS"),"plan",planId,
                        "mapping",Long.toString(cfg.mappingVersionId()),"period",Long.toString(cfg.periodId())));

        update("INSERT INTO LGR.LEDGER_TRANSACTIONS(IDENTIFIER,LEDGER_IDENTIFIER,SOURCE_INSTRUCTION_REFERENCE," +
                "IDEMPOTENCY_KEY,POSTING_STATE,BOOKING_DATE,VALUE_DATE,COMMITTED_AT) " +
                "VALUES(:id,:ledger,:ref,:idem,'COMMITTED',:booking,:value,SYSTIMESTAMP)",
                p("id",ledgerTxId,"ledger",cfg.ledgerId(),"ref",plan.reference(),"idem",idem,
                        "booking",Date.valueOf(instruction.bookingDate()),"value",Date.valueOf(instruction.valueDate())));
        update("INSERT INTO LGR.LEDGER_MOVEMENTS(IDENTIFIER,LEDGER_TRANSACTION_IDENTIFIER,SEQUENCE_NUMBER," +
                "SOURCE_POSITION_ACCOUNT_REFERENCE,DESTINATION_POSITION_ACCOUNT_REFERENCE,AMOUNT,ASSET_CODE) " +
                "VALUES(:id,:tx,1,:source,:dest,:amount,:asset)",
                p("id",seq("LGR.SEQ_LEDGER_MOVEMENTS"),"tx",ledgerTxId,"source",cfg.sourcePosition(),
                        "dest",cfg.destinationPosition(),"amount",plan.amountUnits(),"asset",instruction.currency()));
        applyPositionBalances(cfg, instruction.currency(), plan.amountUnits(), "LGR-TX-"+ledgerTxId);
        update("INSERT INTO LGR.COMMIT_RECEIPTS(IDENTIFIER,PROVIDER_NAME,SOURCE_INSTRUCTION_REFERENCE,POSTING_PURPOSE," +
                "PROVIDER_TRANSACTION_REFERENCE,COMMIT_STATE,EVIDENCE_HASH,RECORDED_AT) " +
                "VALUES(:id,'ORACLE_POSITION_LEDGER',:ref,:purpose,:ledger,'COMMITTED',:hash,SYSTIMESTAMP)",
                p("id",seq("LGR.SEQ_COMMIT_RECEIPTS"),"ref",plan.reference(),"purpose",CashPostingPolicy.PURPOSE,
                        "ledger","LGR-TX-"+ledgerTxId,"hash",plan.payloadHash()));

        update("INSERT INTO ACC.ACCOUNTING_FACTS(IDENTIFIER,SOURCE_FACT_IDENTIFIER,ACCOUNTING_BOOK_IDENTIFIER," +
                "ACCOUNTING_PURPOSE,OCCURRENCE_NUMBER,PAYLOAD_HASH,RECEIPT_STATE,RECEIVED_AT) " +
                "VALUES(:id,:ref,:book,:purpose,1,:hash,'ACCEPTED',SYSTIMESTAMP)",
                p("id",factId,"ref",sourceEvent,"book",cfg.bookId(),"purpose",CashPostingPolicy.PURPOSE,"hash",plan.payloadHash()));
        update("INSERT INTO ACC.ACCOUNTING_PLANS(IDENTIFIER,ACCOUNTING_FACT_IDENTIFIER,ACCOUNTING_MAPPING_VERSION_IDENTIFIER," +
                "PLAN_HASH,PREPARATION_STATE,CREATED_AT) VALUES(:id,:fact,:mapping,:hash,'PREPARED',SYSTIMESTAMP)",
                p("id",seq("ACC.SEQ_ACCOUNTING_PLANS"),"fact",factId,"mapping",cfg.mappingVersionId(),"hash",plan.payloadHash()));
        update("INSERT INTO ACC.ACCOUNTING_TRANSACTIONS(IDENTIFIER,ACCOUNTING_BOOK_IDENTIFIER,FISCAL_PERIOD_IDENTIFIER," +
                "ACCOUNTING_FACT_IDENTIFIER,ACCOUNTING_PURPOSE,OCCURRENCE_NUMBER,POSTING_STATE,BOOKING_DATE,VALUE_DATE,POSTED_AT) " +
                "VALUES(:id,:book,:period,:fact,:purpose,1,'POSTED',:booking,:value,SYSTIMESTAMP)",
                p("id",accTxId,"book",cfg.bookId(),"period",cfg.periodId(),"fact",factId,
                        "purpose",CashPostingPolicy.PURPOSE,"booking",Date.valueOf(instruction.bookingDate()),
                        "value",Date.valueOf(instruction.valueDate())));
        lockGlAccounts(cfg.glAccounts());
        for (GlLine entry : plan.entries()) {
            long glId = cfg.glAccounts().get(entry.role());
            update("INSERT INTO ACC.ACCOUNTING_ENTRIES(IDENTIFIER,ACCOUNTING_TRANSACTION_IDENTIFIER," +
                    "GENERAL_LEDGER_ACCOUNT_IDENTIFIER,SEQUENCE_NUMBER,POSTING_DIRECTION,AMOUNT,CURRENCY_CODE," +
                    "BASE_CURRENCY_AMOUNT,DIMENSION_SNAPSHOT) " +
                    "VALUES(:id,:tx,:gl,:seq,:dc,:amount,:currency,:base,:dim)",
                    p("id",seq("ACC.SEQ_ACCOUNTING_ENTRIES"),"tx",accTxId,"gl",glId,"seq",entry.sequence(),
                            "dc",entry.direction(),"amount",entry.amountUnits(),"currency",instruction.currency(),
                            "base",entry.amountUnits(),"dim",dim));
            applyGlProjection(glId, cfg.periodId(), instruction.currency(), entry, "ACC-TX-"+accTxId);
        }
        update("INSERT INTO PST.POSTING_EXECUTIONS(IDENTIFIER,POSTING_PLAN_IDENTIFIER,EXECUTION_PURPOSE," +
                "PROVIDER_NAME,PROVIDER_TRANSACTION_REFERENCE,OUTCOME_STATE,LAST_CHECKED_AT) " +
                "VALUES(:id,:plan,:purpose,'ORACLE_POSITION_LEDGER',:ref,'COMMITTED',SYSTIMESTAMP)",
                p("id",executionId,"plan",planId,"purpose",CashPostingPolicy.PURPOSE,"ref","LGR-TX-"+ledgerTxId));
        update("INSERT INTO PST.POSTING_EXECUTION_EVENTS(IDENTIFIER,POSTING_EXECUTION_IDENTIFIER,EVENT_SEQUENCE," +
                "PREVIOUS_STATE,NEXT_STATE,EVENT_TIME,REASON) " +
                "VALUES(:id,:exec,1,NULL,'COMMITTED',SYSTIMESTAMP,NULL)",
                p("id",seq("PST.SEQ_POSTING_EXECUTION_EVENTS"),"exec",executionId));
        update("UPDATE PST.POSTING_INSTRUCTIONS SET EXECUTION_STATE='COMMITTED' WHERE IDENTIFIER=:id AND EXECUTION_STATE='RECEIVED'",
                p("id",instructionId));
    }

    private Resolved resolve(Instruction r, Plan p) {
        String ruleCode = "CASH_DEPOSIT".equals(r.transactionType()) ? depositRule : withdrawalRule;
        String mappingCode = "CASH_DEPOSIT".equals(r.transactionType()) ? depositMapping : withdrawalMapping;
        long book = n(one("SELECT IDENTIFIER FROM ACC.ACCOUNTING_BOOKS WHERE BOOK_CODE=:code AND " +
                "LEGAL_ENTITY_REFERENCE=:entity AND BOOK_STATE='ACTIVE' AND BASE_CURRENCY_CODE=:currency",
                p("code",bookCode,"entity",legalEntity,"currency",r.currency()),"accounting book"),"IDENTIFIER");
        long period = n(one("SELECT IDENTIFIER FROM ACC.FISCAL_PERIODS WHERE ACCOUNTING_BOOK_IDENTIFIER=:book " +
                "AND PERIOD_STATE='OPEN' AND PERIOD_START<=:booking AND PERIOD_END>=:booking",
                p("book",book,"booking",Date.valueOf(r.bookingDate())),"open fiscal period"),"IDENTIFIER");
        long ledger = n(one("SELECT IDENTIFIER FROM LGR.LEDGERS WHERE LEDGER_CODE=:code AND " +
                "LEGAL_ENTITY_REFERENCE=:entity AND LEDGER_STATE='ACTIVE'",
                p("code",ledgerCode,"entity",legalEntity),"position ledger"),"IDENTIFIER");
        long chart = n(one("SELECT IDENTIFIER FROM ACC.CHARTS_OF_ACCOUNTS WHERE ACCOUNTING_BOOK_IDENTIFIER=:book " +
                "AND CHART_CODE=:code AND EFFECTIVE_FROM<=SYSTIMESTAMP AND " +
                "(EFFECTIVE_UNTIL IS NULL OR EFFECTIVE_UNTIL>SYSTIMESTAMP)",
                p("book",book,"code",chartCode),"chart of accounts"),"IDENTIFIER");
        Map<String,Object> rule = one("SELECT V.IDENTIFIER,V.CONTENT_HASH,V.APPROVAL_REFERENCE " +
                "FROM PST.POSTING_RULES R JOIN PST.POSTING_RULE_VERSIONS V ON V.POSTING_RULE_IDENTIFIER=R.IDENTIFIER " +
                "WHERE R.RULE_CODE=:code AND V.PUBLICATION_STATE='PUBLISHED' AND " +
                "V.EFFECTIVE_FROM<=SYSTIMESTAMP AND (V.EFFECTIVE_UNTIL IS NULL OR V.EFFECTIVE_UNTIL>SYSTIMESTAMP)",
                p("code",ruleCode),"published cash rule");
        published(rule, true);
        long ruleId = n(rule,"IDENTIFIER");
        var conditions = rows("SELECT ATTRIBUTE_NAME,COMPARISON_OPERATOR,EXPECTED_VALUE FROM PST.POSTING_RULE_CONDITIONS " +
                "WHERE POSTING_RULE_VERSION_IDENTIFIER=:id ORDER BY CONDITION_SEQUENCE",
                p("id",ruleId));
        if (conditions.isEmpty()) throw new IllegalStateException("Posting rule has no conditions");
        Map<String,String> actual = Map.of("event_type",r.transactionType(),"source_domain","DPS2",
                "accounting_purpose",CashPostingPolicy.PURPOSE,"currency_code",r.currency(),
                "product_version_reference",r.productVersion(),"bank_unit_reference",r.bankUnit());
        boolean containsEvent = false;
        for (var condition : conditions) {
            String attribute = s(condition,"ATTRIBUTE_NAME");
            if ("event_type".equals(attribute)) containsEvent = true;
            if (!"EQUALS".equals(s(condition,"COMPARISON_OPERATOR")) || !actual.containsKey(attribute) ||
                    !actual.get(attribute).equals(s(condition,"EXPECTED_VALUE")))
                throw new IllegalStateException("Unsupported or nonmatching published posting rule condition: " + attribute);
        }
        if (!containsEvent) throw new IllegalStateException("Posting rule must scope event_type");
        Map<String,Object> assignment = one("SELECT POSTING_TEMPLATE_VERSION_IDENTIFIER,CONTENT_HASH,APPROVAL_REFERENCE " +
                "FROM PST.POSTING_RULE_TEMPLATE_ASSIGNMENTS WHERE POSTING_RULE_VERSION_IDENTIFIER=:rule " +
                "AND ACCOUNTING_PURPOSE=:purpose AND PUBLICATION_STATE='PUBLISHED' AND EFFECTIVE_FROM<=SYSTIMESTAMP " +
                "AND (EFFECTIVE_UNTIL IS NULL OR EFFECTIVE_UNTIL>SYSTIMESTAMP)",
                p("rule",ruleId,"purpose",CashPostingPolicy.PURPOSE),"published rule-template assignment");
        published(assignment, true);
        long templateId = n(assignment,"POSTING_TEMPLATE_VERSION_IDENTIFIER");
        Map<String,Object> template = one("SELECT IDENTIFIER,CONTENT_HASH FROM PST.POSTING_TEMPLATE_VERSIONS " +
                "WHERE IDENTIFIER=:id AND PUBLICATION_STATE='PUBLISHED' AND EFFECTIVE_FROM<=SYSTIMESTAMP " +
                "AND (EFFECTIVE_UNTIL IS NULL OR EFFECTIVE_UNTIL>SYSTIMESTAMP)",
                p("id",templateId),"published template");
        published(template, false);
        one("SELECT IDENTIFIER FROM PST.POSTING_APPROVALS WHERE SUBJECT_REFERENCE=:subject " +
                "AND APPROVAL_TYPE='PUBLISH' AND DECISION_STATE='APPROVED' " +
                "AND REQUESTED_BY IS NOT NULL AND APPROVED_BY IS NOT NULL AND REQUESTED_BY<>APPROVED_BY " +
                "AND DECIDED_AT<=SYSTIMESTAMP",p("subject","PST.POSTING_TEMPLATE_VERSIONS:"+templateId),
                "maker-checker approval for template");
        Map<String,Object> line = one("SELECT SOURCE_ACCOUNT_ROLE,DESTINATION_ACCOUNT_ROLE,AMOUNT_COMPONENT,ASSET_POLICY " +
                "FROM PST.POSTING_TEMPLATE_LINES WHERE POSTING_TEMPLATE_VERSION_IDENTIFIER=:id",
                p("id",templateId),"one cash-principal movement template");
        if (!p.sourcePositionRole().equals(s(line,"SOURCE_ACCOUNT_ROLE")) ||
                !p.destinationPositionRole().equals(s(line,"DESTINATION_ACCOUNT_ROLE")) ||
                !"CASH_PRINCIPAL".equals(s(line,"AMOUNT_COMPONENT")) ||
                !"SAME_AS_INSTRUCTION".equals(s(line,"ASSET_POLICY")))
            throw new IllegalStateException("Published template does not match deposit/withdrawal cash flow");
        Map<String,Object> mapping = one("SELECT IDENTIFIER,CONTENT_HASH,APPROVAL_REFERENCE FROM " +
                "ACC.ACCOUNTING_MAPPING_VERSIONS WHERE MAPPING_CODE=:code AND PUBLICATION_STATE='PUBLISHED' " +
                "AND EFFECTIVE_FROM<=SYSTIMESTAMP AND (EFFECTIVE_UNTIL IS NULL OR EFFECTIVE_UNTIL>SYSTIMESTAMP)",
                p("code",mappingCode),"published accounting mapping");
        published(mapping, true);
        long mappingId = n(mapping,"IDENTIFIER");
        var entries = rows("SELECT SEQUENCE_NUMBER,ACCOUNT_ROLE,POSTING_DIRECTION,ACCOUNTING_COMPONENT " +
                "FROM ACC.ACCOUNTING_MAPPING_ENTRIES WHERE ACCOUNTING_MAPPING_VERSION_IDENTIFIER=:id " +
                "AND SOURCE_EVENT_TYPE=:type ORDER BY SEQUENCE_NUMBER",
                p("id",mappingId,"type",r.transactionType()));
        if (entries.size()!=2) throw new IllegalStateException("Cash GL mapping must define exactly two entries");
        Map<String,Long> gls = new HashMap<>();
        for (int i=0;i<2;i++) {
            GlLine expected = p.entries().get(i);
            var entry = entries.get(i);
            if (n(entry,"SEQUENCE_NUMBER") != expected.sequence() ||
                    !expected.role().equals(s(entry,"ACCOUNT_ROLE")) ||
                    !expected.direction().equals(s(entry,"POSTING_DIRECTION")) ||
                    !"CASH_PRINCIPAL".equals(s(entry,"ACCOUNTING_COMPONENT")))
                throw new IllegalStateException("GL mapping deviates from approved balanced cash policy");
            var assignmentGl = one("SELECT GENERAL_LEDGER_ACCOUNT_IDENTIFIER,CONTENT_HASH,APPROVAL_REFERENCE " +
                    "FROM ACC.ACCOUNT_ROLE_ASSIGNMENTS WHERE ACCOUNTING_MAPPING_VERSION_IDENTIFIER=:mapping " +
                    "AND ACCOUNTING_BOOK_IDENTIFIER=:book AND ACCOUNT_ROLE=:role AND CURRENCY_CODE=:currency " +
                    "AND PRODUCT_VERSION_REFERENCE=:product AND BANK_UNIT_REFERENCE=:unit " +
                    "AND PUBLICATION_STATE='PUBLISHED' AND EFFECTIVE_FROM<=SYSTIMESTAMP " +
                    "AND (EFFECTIVE_UNTIL IS NULL OR EFFECTIVE_UNTIL>SYSTIMESTAMP)",
                    p("mapping",mappingId,"book",book,"role",expected.role(),"currency",r.currency(),
                            "product",r.productVersion(),"unit",r.bankUnit()),"effective GL role assignment");
            published(assignmentGl, true);
            long glId = n(assignmentGl,"GENERAL_LEDGER_ACCOUNT_IDENTIFIER");
            var gl = one("SELECT ACCOUNT_TYPE,NORMAL_BALANCE_DIRECTION FROM ACC.GENERAL_LEDGER_ACCOUNTS WHERE IDENTIFIER=:id " +
                    "AND CHART_OF_ACCOUNTS_IDENTIFIER=:chart AND ACCOUNT_STATE='ACTIVE'",
                    p("id",glId,"chart",chart),"active GL account");
            String expectedType = CashPostingPolicy.CASH_GL.equals(expected.role()) ? "ASSET" : "LIABILITY";
            String normal = CashPostingPolicy.CASH_GL.equals(expected.role()) ? "DEBIT" : "CREDIT";
            if (!expectedType.equals(s(gl,"ACCOUNT_TYPE")) || !normal.equals(s(gl,"NORMAL_BALANCE_DIRECTION")))
                throw new IllegalStateException("GL account classification/normal balance contradicts cash policy");
            gls.put(expected.role(), glId);
        }
        if (Objects.equals(gls.get(CashPostingPolicy.CASH_GL), gls.get(CashPostingPolicy.CUSTOMER_GL)))
            throw new IllegalStateException("Cash and deposit-liability GL accounts must differ");
        Map<String,Object> source = position(p.sourcePositionRole(),r,ledger);
        Map<String,Object> dest = position(p.destinationPositionRole(),r,ledger);
        String sourceCode = s(source,"POSITION_ACCOUNT_CODE");
        String destinationCode = s(dest,"POSITION_ACCOUNT_CODE");
        if (sourceCode.equals(destinationCode)) throw new IllegalStateException("Source and destination positions are identical");
        return new Resolved(book,period,ledger,ruleId,templateId,mappingId,sourceCode,destinationCode,
                n(source,"IDENTIFIER"),n(dest,"IDENTIFIER"),Map.copyOf(gls));
    }

    private Map<String,Object> position(String role, Instruction r, long ledger) {
        String domain = CashPostingPolicy.CASH_POSITION.equals(role) ? "SYSTEM" : "DPS2";
        String sourceReference = CashPostingPolicy.CASH_POSITION.equals(role) ? r.cashDeskCode() : r.accountNumber();
        Map<String,Object> binding = one("SELECT POSITION_ACCOUNT_REFERENCE FROM LGR.ACCOUNT_BINDINGS " +
                "WHERE SOURCE_DOMAIN=:domain AND SOURCE_ACCOUNT_REFERENCE=:source AND ACCOUNT_ROLE=:role " +
                "AND ASSET_CODE=:currency AND EFFECTIVE_FROM<=SYSTIMESTAMP " +
                "AND (EFFECTIVE_UNTIL IS NULL OR EFFECTIVE_UNTIL>SYSTIMESTAMP)",
                p("domain",domain,"source",sourceReference,"role",role,"currency",r.currency()),
                "effective ledger account binding");
        Map<String,Object> position = one("SELECT IDENTIFIER,POSITION_ACCOUNT_CODE,ALLOW_NEGATIVE_BALANCE " +
                "FROM LGR.POSITION_ACCOUNTS WHERE LEDGER_IDENTIFIER=:ledger AND POSITION_ACCOUNT_CODE=:code " +
                "AND ASSET_CODE=:currency AND ACCOUNT_STATE='ACTIVE'",
                p("ledger",ledger,"code",s(binding,"POSITION_ACCOUNT_REFERENCE"),"currency",r.currency()),
                "active ledger position account");
        if (CashPostingPolicy.CASH_POSITION.equals(role) && n(position,"ALLOW_NEGATIVE_BALANCE") != 1)
            throw new IllegalStateException("Cash clearing position must explicitly allow negative balance");
        if (CashPostingPolicy.CUSTOMER_POSITION.equals(role) && n(position,"ALLOW_NEGATIVE_BALANCE") != 0)
            throw new IllegalStateException("Customer deposit position must forbid negative balance");
        return position;
    }

    private int currencyScale(String currency) {
        if (currency == null || !currency.matches("[A-Z]{3}"))
            throw new IllegalArgumentException("Currency must be a 3-character ISO code");
        Map<String,Object> asset = one("SELECT DECIMAL_PRECISION FROM LGR.ASSETS " +
                "WHERE ASSET_CODE=:currency AND ASSET_STATE='ACTIVE'",p("currency",currency),"asset definition");
        int scale = Math.toIntExact(n(asset,"DECIMAL_PRECISION"));
        if (scale<0 || scale>8) throw new IllegalStateException("Unsupported asset precision");
        return scale;
    }

    private void applyPositionBalances(Resolved cfg, String currency, BigDecimal units, String transactionRef) {
        // Lock both position definitions in canonical order before looking up or creating balance rows.
        List<Long> ids = new ArrayList<>(List.of(cfg.sourcePositionId(),cfg.destinationPositionId()));
        ids.sort(Comparator.naturalOrder());
        for (long id : ids) one("SELECT IDENTIFIER FROM LGR.POSITION_ACCOUNTS WHERE IDENTIFIER=:id FOR UPDATE",
                p("id",id),"position lock");
        adjustPosition(cfg.sourcePosition(),currency,units.negate(),transactionRef);
        adjustPosition(cfg.destinationPosition(),currency,units,transactionRef);
    }
    private void adjustPosition(String position, String currency, BigDecimal delta, String reference) {
        var current = rows("SELECT IDENTIFIER,POSTED_BALANCE,RESERVED_AMOUNT,RECORD_VERSION " +
                "FROM LGR.POSITION_BALANCES WHERE POSITION_ACCOUNT_REFERENCE=:code AND ASSET_CODE=:currency FOR UPDATE",
                p("code",position,"currency",currency));
        if (current.size()>1) throw new IllegalStateException("Duplicate ledger balance");
        BigDecimal before = current.isEmpty()?BigDecimal.ZERO:decimal(current.getFirst(),"POSTED_BALANCE");
        BigDecimal after = before.add(delta);
        if (after.precision()>24) throw new IllegalStateException("Position balance exceeds NUMBER(24,0)");
        var pos = one("SELECT ALLOW_NEGATIVE_BALANCE FROM LGR.POSITION_ACCOUNTS WHERE POSITION_ACCOUNT_CODE=:code " +
                "AND ASSET_CODE=:currency AND ACCOUNT_STATE='ACTIVE'",
                p("code",position,"currency",currency),"unique active position for balance");
        // Customer positions cannot spend already reserved amount.
        if (delta.signum()<0 && n(pos,"ALLOW_NEGATIVE_BALANCE")!=1 && !current.isEmpty()) {
            BigDecimal reserved=decimal(current.getFirst(),"RESERVED_AMOUNT");
            if (before.subtract(reserved).compareTo(delta.negate())<0)
                throw new IllegalStateException("Insufficient unreserved ledger position balance");
        }
        if (after.signum()<0 && n(pos,"ALLOW_NEGATIVE_BALANCE")!=1)
            throw new IllegalStateException("Insufficient ledger position balance (no overdraft allowed)");
        if (current.isEmpty()) {
            update("INSERT INTO LGR.POSITION_BALANCES(IDENTIFIER,POSITION_ACCOUNT_REFERENCE,ASSET_CODE," +
                    "POSTED_BALANCE,RESERVED_AMOUNT,RECORD_VERSION,LAST_LEDGER_TRANSACTION_REFERENCE) " +
                    "VALUES(:id,:code,:currency,:balance,0,1,:ref)",
                    p("id",seq("LGR.SEQ_POSITION_BALANCES"),"code",position,"currency",currency,
                            "balance",after,"ref",reference));
        } else {
            var row = current.getFirst();
            if (update("UPDATE LGR.POSITION_BALANCES SET POSTED_BALANCE=:balance,RECORD_VERSION=NVL(RECORD_VERSION,0)+1," +
                    "LAST_LEDGER_TRANSACTION_REFERENCE=:ref WHERE IDENTIFIER=:id",
                    p("balance",after,"ref",reference,"id",n(row,"IDENTIFIER")))!=1)
                throw new IllegalStateException("Position balance concurrency conflict");
        }
    }
    private void lockGlAccounts(Map<String,Long> gls) {
        var ids = gls.values().stream().distinct().sorted().toList();
        for (Long id : ids) one("SELECT IDENTIFIER FROM ACC.GENERAL_LEDGER_ACCOUNTS " +
                "WHERE IDENTIFIER=:id AND ACCOUNT_STATE='ACTIVE' FOR UPDATE",p("id",id),"GL account lock");
    }
    private void applyGlProjection(long gl, long period, String currency, GlLine line, String reference) {
        String dimension = "DEFAULT";
        var current = rows("SELECT IDENTIFIER,DEBIT_TOTAL,CREDIT_TOTAL FROM ACC.ACCOUNT_BALANCE_PROJECTIONS " +
                "WHERE GENERAL_LEDGER_ACCOUNT_IDENTIFIER=:gl AND FISCAL_PERIOD_IDENTIFIER=:period " +
                "AND CURRENCY_CODE=:currency AND DIMENSION_KEY=:dimension FOR UPDATE",
                p("gl",gl,"period",period,"currency",currency,"dimension",dimension));
        if (current.size()>1) throw new IllegalStateException("Duplicate GL projection");
        BigDecimal debit = current.isEmpty()?BigDecimal.ZERO:decimal(current.getFirst(),"DEBIT_TOTAL");
        BigDecimal credit = current.isEmpty()?BigDecimal.ZERO:decimal(current.getFirst(),"CREDIT_TOTAL");
        if ("DEBIT".equals(line.direction())) debit=debit.add(line.amountUnits());
        else credit=credit.add(line.amountUnits());
        BigDecimal net=debit.subtract(credit);
        if (debit.precision()>24||credit.precision()>24||net.precision()>24)
            throw new IllegalStateException("GL projection exceeds NUMBER(24,0)");
        if (current.isEmpty()) {
            update("INSERT INTO ACC.ACCOUNT_BALANCE_PROJECTIONS(IDENTIFIER,GENERAL_LEDGER_ACCOUNT_IDENTIFIER," +
                    "FISCAL_PERIOD_IDENTIFIER,CURRENCY_CODE,DIMENSION_KEY,DEBIT_TOTAL,CREDIT_TOTAL,NET_BALANCE," +
                    "LAST_ACCOUNTING_TRANSACTION_REFERENCE) VALUES(:id,:gl,:period,:currency,:dimension,:debit,:credit,:net,:ref)",
                    p("id",seq("ACC.SEQ_ACCOUNT_BALANCE_PROJECTIONS"),"gl",gl,"period",period,"currency",currency,
                            "dimension",dimension,"debit",debit,"credit",credit,"net",net,"ref",reference));
        } else {
            if (update("UPDATE ACC.ACCOUNT_BALANCE_PROJECTIONS SET DEBIT_TOTAL=:debit,CREDIT_TOTAL=:credit," +
                    "NET_BALANCE=:net,LAST_ACCOUNTING_TRANSACTION_REFERENCE=:ref WHERE IDENTIFIER=:id",
                    p("debit",debit,"credit",credit,"net",net,"ref",reference,
                            "id",n(current.getFirst(),"IDENTIFIER")))!=1)
                throw new IllegalStateException("GL projection update failed");
        }
    }

    private void requireConfiguration() {
        for (String x : List.of(bookCode,legalEntity,ledgerCode,chartCode,depositRule,withdrawalRule,depositMapping,withdrawalMapping))
            if (x==null || !x.matches("[A-Za-z0-9][A-Za-z0-9_./:-]{0,59}") || x.startsWith("REPLACE_"))
                throw new IllegalStateException("R1-D5 enabled without complete posting book, rule and mapping configuration");
    }
    private static String safeDimension(String s) {
        if(s==null || !s.matches("[A-Za-z0-9_./:-]{1,160}"))
            throw new IllegalArgumentException("Unsafe GL dimension code");
        return s;
    }
    private void published(Map<String,Object> row, boolean approvalRequired) {
        String contentHash = s(row,"CONTENT_HASH");
        if (contentHash == null || !contentHash.matches("(?i)[a-f0-9]{64}"))
            throw new IllegalStateException("Published posting configuration lacks SHA-256-shaped hash");
        if (approvalRequired && (s(row,"APPROVAL_REFERENCE")==null || s(row,"APPROVAL_REFERENCE").isBlank()))
            throw new IllegalStateException("Published posting configuration lacks approval reference");
    }
    private long seq(String fixedSequence) {
        // Sequence identifiers originate ONLY from the fixed, source-owned call sites above.
        return db.queryForObject("SELECT " + fixedSequence + ".NEXTVAL FROM DUAL",
                new MapSqlParameterSource(), Long.class);
    }
    private Map<String,Object> one(String sql, MapSqlParameterSource params, String label) {
        List<Map<String,Object>> items=rows(sql,params);
        if(items.size()!=1) throw new IllegalStateException("Missing or ambiguous posting configuration: " + label);
        return items.getFirst();
    }
    private List<Map<String,Object>> rows(String sql, MapSqlParameterSource params) {
        return db.queryForList(sql,params);
    }
    private int update(String sql, MapSqlParameterSource params) {
        return db.update(sql,params);
    }
    private static MapSqlParameterSource p(Object... args) {
        if (args.length%2!=0) throw new IllegalArgumentException("Uneven SQL parameters");
        MapSqlParameterSource params=new MapSqlParameterSource();
        for(int i=0;i<args.length;i+=2) params.addValue((String) args[i],args[i+1]);
        return params;
    }
    private static long n(Map<String,Object> row,String key) {
        Object value=row.get(key);
        if (!(value instanceof Number number)) throw new IllegalStateException("Missing numeric posting field: "+key);
        return number.longValue();
    }
    private static String s(Map<String,Object> row,String key) {
        Object value=row.get(key);
        return value==null?null:value.toString();
    }
    private static BigDecimal decimal(Map<String,Object> row,String key) {
        Object value=row.get(key);
        return value==null?BigDecimal.ZERO:(BigDecimal)value;
    }
}
