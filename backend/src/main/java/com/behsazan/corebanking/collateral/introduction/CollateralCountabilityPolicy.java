package com.behsazan.corebanking.collateral.introduction;

/** G-12: two independent Oracle reference flags; NULL is an unresolved bank decision. */
public final class CollateralCountabilityPolicy {
    private CollateralCountabilityPolicy() {}
    public record Decision(boolean configured, boolean countable, boolean hasMultiplicity) {}
    public static Decision evaluate(String countableFlag, String multiplicityFlag) {
        if (countableFlag != null && !countableFlag.equals("Y") && !countableFlag.equals("N"))
            throw new IllegalArgumentException("G12_COUNTABLE_FLAG_INVALID");
        if (multiplicityFlag != null && !multiplicityFlag.equals("Y") && !multiplicityFlag.equals("N"))
            throw new IllegalArgumentException("G12_MULTIPLICITY_FLAG_INVALID");
        return new Decision(countableFlag != null, "Y".equals(countableFlag),
                "Y".equals(multiplicityFlag));
    }
}
