package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.Debtor;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceReaderPool;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read-only, branch-scoped, bounded sponsor comparison against existing physical CLT data. */
@Repository
public class CollateralAllocationRequestRepository {
    private final ObjectProvider<CollateralReferenceReaderPool> reader;
    public CollateralAllocationRequestRepository(ObjectProvider<CollateralReferenceReaderPool> reader) {
        this.reader = reader;
    }
    public Set<String> sponsorDebtorMatches(String collateralId, String branch, List<Debtor> debtors) {
        if (debtors.isEmpty() || debtors.size() > 20) throw new IllegalArgumentException("CLT_ALLOCATION_DEBTOR_LIMIT");
        String marks = String.join(",", Collections.nCopies(debtors.size(), "?"));
        String sql = """
            SELECT DISTINCT s.PARTY_ID FROM CLT.COLLATERAL_SPONSOR s
            JOIN CLT.COLLATERAL c ON c.COLLATERAL_ID = s.COLLATERAL_ID
            WHERE c.COLLATERAL_ID = ? AND c.BRANCH_CODE = ?
            AND s.PARTY_ID IN (%s)
            """.formatted(marks);
        List<Object> args = new ArrayList<>();
        args.add(new BigDecimal(collateralId));
        args.add(branch);
        for (Debtor d : debtors) args.add(new BigDecimal(d.partyId()));
        JdbcTemplate jdbc = reader.getObject().jdbc();
        return new HashSet<>(jdbc.query(sql,
                (rs, row) -> rs.getBigDecimal("PARTY_ID").toPlainString(), args.toArray()));
    }
}
