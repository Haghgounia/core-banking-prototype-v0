package com.behsazan.corebanking.fee3.r3;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Read-only effective CAL2 bank-day map: no client-supplied holidays are trusted. */
@Repository
public class Fee3F7Cal2WorkdayRepository {
    private final JdbcClient jdbc;
    private final String schema;
    public Fee3F7Cal2WorkdayRepository(JdbcClient jdbc,
            @Value("${core-banking.schemas.calendar2:CAL2}") String schema) {
        if(schema==null||!Pattern.matches("[A-Za-z][A-Za-z0-9_]{0,29}",schema))
            throw new IllegalArgumentException("CAL2_SCHEMA_IDENTIFIER_INVALID");
        this.jdbc=jdbc;this.schema=schema.toUpperCase(Locale.ROOT);
    }
    public List<Fee3F7LateDamageContract.Workday> coveredDays(String code,LocalDate first,LocalDate exclusive){
        if(code==null||!code.matches("[A-Za-z0-9_./:-]{3,80}")||first==null||exclusive==null||
            !exclusive.isAfter(first)||ChronoUnit.DAYS.between(first,exclusive)>3660)
            throw new IllegalArgumentException("CAL2_BUSINESS_DAY_QUERY_INVALID");
        String sql="SELECT D.ISO_DATE_TEXT, B.IS_BUSINESS_DAY, B.RESOLUTION_SOURCE, " +
            "B.BUSINESS_CALENDAR_DAY_ID FROM "+schema+".BUSINESS_CALENDAR_DAY B " +
            "JOIN "+schema+".BUSINESS_CALENDAR BC ON BC.BUSINESS_CALENDAR_ID=B.BUSINESS_CALENDAR_ID " +
            "JOIN "+schema+".CANONICAL_DAY D ON D.DAY_ID=B.DAY_ID " +
            "WHERE BC.CALENDAR_CODE=:code AND BC.ACTIVE_FLAG='Y' " +
            "AND (BC.VALID_FROM IS NULL OR BC.VALID_FROM<=:fromDay) " +
            "AND (BC.VALID_TO IS NULL OR BC.VALID_TO>=:lastDay) " +
            "AND D.CANONICAL_DATE>=:fromDay AND D.CANONICAL_DATE<:toDay ORDER BY D.CANONICAL_DATE";
        List<Fee3F7LateDamageContract.Workday> records=jdbc.sql(sql)
            .param("code",code).param("fromDay",Date.valueOf(first))
            .param("lastDay",Date.valueOf(exclusive.minusDays(1)))
            .param("toDay",Date.valueOf(exclusive))
            .query((rs,row)->new Fee3F7LateDamageContract.Workday(
                LocalDate.parse(rs.getString(1)),"Y".equals(rs.getString(2)),
                "CAL2:"+code+":"+rs.getLong(4)+":"+rs.getString(3))).list();
        if(records.size()!=ChronoUnit.DAYS.between(first,exclusive))
            throw new IllegalArgumentException("CAL2_BUSINESS_CALENDAR_COVERAGE_MISSING");
        LocalDate expected=first;
        List<Fee3F7LateDamageContract.Workday> verified=new ArrayList<>();
        for(var row:records){
            if(!row.date().equals(expected))
                throw new IllegalArgumentException("CAL2_BUSINESS_DAY_GAP_OR_DUPLICATE");
            verified.add(row);expected=expected.plusDays(1);
        }
        return List.copyOf(verified);
    }
}
