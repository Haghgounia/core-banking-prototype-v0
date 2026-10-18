package com.behsazan.corebanking.posting.cash;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only, token-gated cash posting evidence; returns no customer personal data. */
@RestController
@RequestMapping("/api/v1/deal-posting/cash")
@ConditionalOnProperty(prefix="core-banking.posting.cash",name="enabled",havingValue="true")
public class CashPostingJournalReadController {
    private final NamedParameterJdbcTemplate db;
    private final byte[] requiredToken;
    public record JournalLine(long sequence, String glCode, String direction, BigDecimal amount, String currency) {}
    public record JournalEvidence(String sourceEvent, long accountingTransactionId, String postingState,
                                  String instructionState, List<JournalLine> entries,
                                  BigDecimal totalDebit, BigDecimal totalCredit, boolean balanced) {}
    public CashPostingJournalReadController(NamedParameterJdbcTemplate db,
            @Value("${core-banking.posting.cash.review-token:}") String token) {
        this.db=db;
        this.requiredToken=token.getBytes(StandardCharsets.UTF_8);
    }
    @GetMapping("/deposit-transactions/{transactionId}/journal")
    @Transactional(readOnly=true)
    public ResponseEntity<?> journal(@PathVariable long transactionId,
            @RequestHeader(name="X-Posting-Review-Token",required=false) String supplied) {
        if (requiredToken.length<16 || new String(requiredToken,StandardCharsets.UTF_8).startsWith("REPLACE_") || supplied==null ||
                !MessageDigest.isEqual(requiredToken,supplied.getBytes(StandardCharsets.UTF_8)))
            return ResponseEntity.status(403).build();
        if(transactionId<=0) return ResponseEntity.badRequest().build();
        String event="DPS2-TX-"+transactionId;
        var params=Map.of("event",event,"purpose",CashPostingPolicy.PURPOSE);
        var journals=db.queryForList("SELECT T.IDENTIFIER,T.POSTING_STATE FROM ACC.ACCOUNTING_TRANSACTIONS T " +
                        "JOIN ACC.ACCOUNTING_FACTS F ON F.IDENTIFIER=T.ACCOUNTING_FACT_IDENTIFIER " +
                        "WHERE F.SOURCE_FACT_IDENTIFIER=:event AND F.ACCOUNTING_PURPOSE=:purpose " +
                        "AND T.ACCOUNTING_PURPOSE=:purpose",params);
        if (journals.isEmpty()) return ResponseEntity.notFound().build();
        if (journals.size()!=1) return ResponseEntity.status(409).body(Map.of("error","AMBIGUOUS_ACCOUNTING_TRANSACTION"));
        long id=((Number)journals.getFirst().get("IDENTIFIER")).longValue();
        var instruction=db.queryForList("SELECT EXECUTION_STATE FROM PST.POSTING_INSTRUCTIONS " +
                "WHERE SOURCE_DOMAIN='DPS2' AND SOURCE_EVENT_IDENTIFIER=:event AND ACCOUNTING_PURPOSE=:purpose",params);
        if (instruction.size()!=1) return ResponseEntity.status(409).body(Map.of("error","POSTING_TRACE_INCONSISTENT"));
        var entries=db.query("SELECT E.SEQUENCE_NUMBER,G.ACCOUNT_CODE,E.POSTING_DIRECTION,E.AMOUNT,E.CURRENCY_CODE " +
                        "FROM ACC.ACCOUNTING_ENTRIES E JOIN ACC.GENERAL_LEDGER_ACCOUNTS G " +
                        "ON G.IDENTIFIER=E.GENERAL_LEDGER_ACCOUNT_IDENTIFIER " +
                        "WHERE E.ACCOUNTING_TRANSACTION_IDENTIFIER=:id ORDER BY E.SEQUENCE_NUMBER",
                Map.of("id",id),(rs,i)->new JournalLine(rs.getLong(1),rs.getString(2),rs.getString(3),
                        rs.getBigDecimal(4),rs.getString(5)));
        BigDecimal debit=entries.stream().filter(e->"DEBIT".equals(e.direction()))
                .map(JournalLine::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal credit=entries.stream().filter(e->"CREDIT".equals(e.direction()))
                .map(JournalLine::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
        boolean balanced=entries.size()==2 && debit.signum()>0 && debit.compareTo(credit)==0;
        if (!balanced) return ResponseEntity.status(409).body(Map.of("error","ACCOUNTING_JOURNAL_UNBALANCED"));
        return ResponseEntity.ok(new JournalEvidence(event,id,String.valueOf(journals.getFirst().get("POSTING_STATE")),
                String.valueOf(instruction.getFirst().get("EXECUTION_STATE")),entries,debit,credit,balanced));
    }
}
