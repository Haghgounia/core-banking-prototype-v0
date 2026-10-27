package com.behsazan.corebanking.fee3.r4c3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Exact single-day SELECT for Jalali anniversary and start-of-year events. */
@Repository
public class Fee3R4C3JalaliEventRepository {
    private final JdbcClient db;
    private final String owner;
    public Fee3R4C3JalaliEventRepository(JdbcClient db,
        @Value("${core-banking.schemas.calendar2:CAL2}") String schema){
        this.db=db;
        if(schema==null||!Pattern.matches("[A-Za-z][A-Za-z0-9_]{0,29}",schema))
            throw new IllegalArgumentException("UNSAFE_CAL2_SCHEMA");
        this.owner=schema.toUpperCase(Locale.ROOT);
    }
    public Fee3R4C3QardContract.EventDay eventDay(LocalDate isoDate){
        String sql="SELECT D.ISO_DATE_TEXT,C.YEAR_NO,C.MONTH_NO,C.DAY_NO,C.DAY_OF_YEAR,"+
            "C.IS_LEAP_YEAR,C.CALENDAR_VARIANT_ID,C.DATASET_VERSION_ID "+
            "FROM "+owner+".CALENDAR_DATE C JOIN "+owner+".CANONICAL_DAY D ON D.DAY_ID=C.DAY_ID "+
            "JOIN "+owner+".CALENDAR_VARIANT V ON V.CALENDAR_VARIANT_ID=C.CALENDAR_VARIANT_ID "+
            "JOIN "+owner+".CALENDAR_SYSTEM S ON S.CALENDAR_SYSTEM_ID=V.CALENDAR_SYSTEM_ID "+
            "WHERE S.CALENDAR_CODE='PERSIAN' AND S.ACTIVE_FLAG='Y' "+
            "AND V.ACTIVE_FLAG='Y' AND V.IS_DEFAULT='Y' AND D.CANONICAL_DATE=:date";
        List<Fee3R4C3QardContract.EventDay> rows=db.sql(sql)
            .param("date",Date.valueOf(isoDate)).query((rs,n)->{
                String leap=rs.getString(6);
                if(!"Y".equals(leap)&&!"N".equals(leap))
                    throw new IllegalArgumentException("CAL2_INVALID_LEAP_MARKER");
                LocalDate result;
                try{result=LocalDate.parse(rs.getString(1));}
                catch(RuntimeException ex){throw new IllegalArgumentException("CAL2_INVALID_ISO_DATE");}
                long variant=rs.getLong(7),dataset=rs.getLong(8);
                return new Fee3R4C3QardContract.EventDay(result,rs.getInt(2),rs.getInt(3),
                    rs.getInt(4),rs.getInt(5),"Y".equals(leap)?366:365,
                    "CAL2:PERSIAN:"+variant+":"+dataset);
            }).list();
        if(rows.size()!=1||!isoDate.equals(rows.get(0).isoDate()))
            throw new IllegalArgumentException("CAL2_QARD_EVENT_DAY_MISSING_OR_DUPLICATED");
        return rows.get(0);
    }
}
