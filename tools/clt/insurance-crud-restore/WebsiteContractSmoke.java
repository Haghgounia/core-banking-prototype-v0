package com.behsazan.corebanking.collateral.reference;

import java.util.List;
import java.util.Map;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Column;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Table;

/** No Oracle connection. Validate website extension does not enlarge the 17-table catalog. */
public class WebsiteContractSmoke {
    private static int checks = 0;
    static void expect(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        checks++;
    }
    static void rejected(Runnable fn) {
        try { fn.run(); throw new AssertionError("EXPECTED_REJECTION"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    public static void main(String[] args) {
        Table insurer = new Table("INSURANCE_COMPANY", "insurer", "", "insurance", "INSURANCE_COMPANY_ID",
                "INSURANCE_COMPANY_CODE", "INSURANCE_COMPANY_NAME", List.of(
                new Column("INSURANCE_COMPANY_ID", "", "NUMBER(19,0)", true, true),
                new Column("INSURANCE_COMPANY_CODE", "", "VARCHAR2(50 CHAR)", true, false),
                new Column("INSURANCE_COMPANY_NAME", "", "VARCHAR2(100 CHAR)", true, false),
                new Column("STATUS", "", "VARCHAR2(10 CHAR)", true, false),
                new Column("CREATED_AT", "", "TIMESTAMP(6)", true, false),
                new Column("CREATED_BY", "", "VARCHAR2(100 CHAR)", true, false),
                new Column("UPDATED_AT", "", "TIMESTAMP(6)", false, false),
                new Column("UPDATED_BY", "", "VARCHAR2(100 CHAR)", false, false),
                new Column("RECORD_VERSION", "", "NUMBER(10,0)", true, false)), 0);
        var accepted = CltUnifiedReferenceValidation.normalized(insurer,
                Map.of("WEBSITE_URL", "https://www.iraninsurance.ir"), false);
        expect(accepted.size() == 1 && "https://www.iraninsurance.ir".equals(accepted.get("WEBSITE_URL")), "INSURER_SITE_EDIT");
        expect(CltUnifiedReferenceValidation.normalized(insurer, Map.of("STATUS", "ACTIVE"), false).size() == 1,
                "REGULAR_CRUD_UNCHANGED");
        expect(CltUnifiedReferenceValidation.normalized(insurer, java.util.Collections.singletonMap("WEBSITE_URL", null), false)
                .containsKey("WEBSITE_URL"), "SITE_NULL_CLEAR_ALLOWED");
        expect(CltUnifiedReferenceValidation.normalized(insurer, Map.of("WEBSITE_URL", " https://melat.ir "), false)
                .get("WEBSITE_URL").equals("https://melat.ir"), "TRIM_SITE");
        for (String site : List.of("javascript:alert(1)", "https://localhost", "http://alice:pw@example.com",
                "https://example.com/white space", "https://", "https://" + "a".repeat(501) + ".com"))
            rejected(() -> CltUnifiedReferenceValidation.normalized(insurer, Map.of("WEBSITE_URL", site), false));
        Table other = new Table("COLLATERAL_TYPE", "", "", "", "COLLATERAL_TYPE_ID", "", "", insurer.columns(), 0);
        rejected(() -> CltUnifiedReferenceValidation.normalized(other, Map.of("WEBSITE_URL", "https://melat.ir"), false));
        rejected(() -> CltUnifiedReferenceValidation.normalized(insurer, Map.of("LOGO", "javascript:void(0)"), false));
        System.out.println("CLT_INSURANCE_WEBSITE_JAVA_SMOKE_PASS=" + checks);
    }
}
