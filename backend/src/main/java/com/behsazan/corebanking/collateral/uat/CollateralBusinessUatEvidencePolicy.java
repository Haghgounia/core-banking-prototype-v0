package com.behsazan.corebanking.collateral.uat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Deterministic BUC/SUC evidence contract; a manual PASS is never an attestation by the bank. */
public final class CollateralBusinessUatEvidencePolicy {
    private CollateralBusinessUatEvidencePolicy() {}
    public static final String CASE_SET = "BUC01_16_V1";
    public static final String CONTRACT = "CLT-BUC-SUC-UAT-ORACLE-1";
    public static final int COUNT = 32;
    public record Scenario(String id, String status, String note) {}

    public static String scenario(String value) {
        if (value == null || !value.matches("BUC-(0[1-9]|1[0-6])-[PN]"))
            throw new IllegalArgumentException("CLT_UAT_SCENARIO_ID_INVALID");
        return value;
    }
    public static String status(String value) {
        if (value == null || !Set.of("NOT_RUN", "PASS", "FAIL", "BLOCKED").contains(value))
            throw new IllegalArgumentException("CLT_UAT_STATUS_INVALID");
        return value;
    }
    public static String note(String value) {
        if (value == null || value.codePointCount(0,value.length()) > 280)
            throw new IllegalArgumentException("CLT_UAT_NOTE_TOO_LONG");
        for (int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            if (Character.isISOControl(c) && c!='\n' && c!='\r' && c!='\t')
                throw new IllegalArgumentException("CLT_UAT_NOTE_CONTROL_CHARACTER");
        }
        return value.strip();
    }
    public static List<Scenario> normalize(List<Scenario> rows) {
        if (rows == null || rows.size()!=COUNT)
            throw new IllegalArgumentException("CLT_UAT_EXPECTS_32_SCENARIOS");
        Set<String> seen = new HashSet<>();
        List<Scenario> normalized=new ArrayList<>(COUNT);
        for (Scenario s:rows) {
            if (s == null) throw new IllegalArgumentException("CLT_UAT_NULL_SCENARIO");
            String id=scenario(s.id());
            if (!seen.add(id))throw new IllegalArgumentException("CLT_UAT_DUPLICATE_SCENARIO");
            normalized.add(new Scenario(id,status(s.status()),note(s.note())));
        }
        normalized.sort(java.util.Comparator.comparing(Scenario::id));
        return List.copyOf(normalized);
    }
    public static List<Scenario> fillMissing(List<Scenario> stored) {
        java.util.Map<String,Scenario> lookup=new java.util.HashMap<>();
        for (Scenario row:stored)lookup.put(scenario(row.id()),row);
        List<Scenario> complete=new ArrayList<>(COUNT);
        for(int i=1;i<=16;i++)for(String suffix:List.of("N","P")) {
            String id=String.format("BUC-%02d-%s",i,suffix);
            complete.add(lookup.getOrDefault(id,new Scenario(id,"NOT_RUN","")));
        }
        return List.copyOf(complete);
    }
    public static long revision(Long value) {
        if(value==null||value<0||value>999_999_999L)
            throw new IllegalArgumentException("CLT_UAT_EXPECTED_REVISION_INVALID");
        return value;
    }
}
