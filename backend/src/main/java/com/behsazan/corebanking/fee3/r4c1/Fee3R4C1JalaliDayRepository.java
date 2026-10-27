package com.behsazan.corebanking.fee3.r4c1;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** SELECT-only mapping against CAL2 canonical days and the active default Persian variant.
 * Does not infer leap years from Gregorian rules or from caller-supplied inputs.
 */
@Repository
public class Fee3R4C1JalaliDayRepository {
    private final JdbcClient client;
    private final String owner;
    public Fee3R4C1JalaliDayRepository(JdbcClient client,
            @Value("${core-banking.schemas.calendar2:CAL2}") String schema){
        this.client=client;
        if(schema==null||!Pattern.matches("[A-Za-z][A-Za-z0-9_]{0,29}",schema))
            throw new IllegalArgumentException("UNSAFE_CALENDAR_SCHEMA");
        this.owner=schema.toUpperCase(java.util.Locale.ROOT);
    }
    private record Day(LocalDate date,int year,int ordinal,String leap,long variant,long dataset) {}
    public List<Fee3R4C1LoanProfitContract.YearSlice> yearSlices(LocalDate start,LocalDate end){
        long expected=ChronoUnit.DAYS.between(start,end);
        if(expected<1||expected>3653)throw new IllegalArgumentException("JALALI_PERIOD_OUTSIDE_LIMIT");
        String sql="SELECT D.ISO_DATE_TEXT, C.YEAR_NO, C.DAY_OF_YEAR, "+
            "C.IS_LEAP_YEAR, C.CALENDAR_VARIANT_ID, C.DATASET_VERSION_ID "+
            "FROM "+owner+".CALENDAR_DATE C "+
            "JOIN "+owner+".CANONICAL_DAY D ON D.DAY_ID=C.DAY_ID "+
            "JOIN "+owner+".CALENDAR_VARIANT V ON V.CALENDAR_VARIANT_ID=C.CALENDAR_VARIANT_ID "+
            "JOIN "+owner+".CALENDAR_SYSTEM S ON S.CALENDAR_SYSTEM_ID=V.CALENDAR_SYSTEM_ID "+
            "WHERE S.CALENDAR_CODE='PERSIAN' AND S.ACTIVE_FLAG='Y' "+
            "AND V.ACTIVE_FLAG='Y' AND V.IS_DEFAULT='Y' "+
            "AND D.CANONICAL_DATE>=:startDate AND D.CANONICAL_DATE<:endDate "+
            "ORDER BY D.CANONICAL_DATE, V.CALENDAR_VARIANT_ID";
        List<Day> dates=client.sql(sql)
            .param("startDate",Date.valueOf(start)).param("endDate",Date.valueOf(end))
            .query((rs,n)->new Day(parseISO(rs.getString(1)),rs.getInt(2),
                rs.getInt(3),rs.getString(4),rs.getLong(5),rs.getLong(6))).list();
        if(dates.size()!=expected)throw new IllegalArgumentException("CAL2_CANONICAL_DAY_COVERAGE_MISSING");
        List<Fee3R4C1LoanProfitContract.YearSlice> result=new ArrayList<>();
        LocalDate next=start,groupStart=start;
        int groupYear=0,groupDenom=0,groupCount=0,lastOrdinal=0;
        long variant=0,dataset=0;
        for(Day day:dates){
            if(!day.date().equals(next)||day.year()<1200||day.year()>1700||
               day.variant()<=0||
               (variant!=0&&variant!=day.variant())||day.dataset()<=0||
               !isLeap(day.leap())&&!isNonLeap(day.leap()))
                throw new IllegalArgumentException("CAL2_UNTRUSTED_DAY_MAPPING");
            int denom=isLeap(day.leap())?366:365;
            if(day.ordinal()<1||day.ordinal()>denom)
                throw new IllegalArgumentException("CAL2_INVALID_JALALI_ORDINAL");
            if(groupCount>0&&day.year()!=groupYear){
                if(day.year()!=groupYear+1||lastOrdinal!=groupDenom||day.ordinal()!=1)
                    throw new IllegalArgumentException("CAL2_JALALI_YEAR_BOUNDARY_INVALID");
                result.add(new Fee3R4C1LoanProfitContract.YearSlice(groupYear,groupStart,
                    day.date(),groupCount,groupDenom,"CAL2:PERSIAN:"+variant+":"+dataset));
                groupYear=0;groupCount=0;lastOrdinal=0;groupStart=day.date();
            }
            if(groupCount>0&&(denom!=groupDenom||day.ordinal()!=lastOrdinal+1))
                throw new IllegalArgumentException("CAL2_JALALI_DAY_SEQUENCE_INVALID");
            if(groupCount==0){groupYear=day.year();groupDenom=denom;dataset=day.dataset();}
            else if(dataset!=day.dataset())
                throw new IllegalArgumentException("CAL2_DATASET_CHANGED_DURING_YEAR");
            variant=day.variant();groupCount++;lastOrdinal=day.ordinal();next=next.plusDays(1);
        }
        if(!next.equals(end))throw new IllegalArgumentException("CAL2_PERIOD_NOT_COVERED");
        result.add(new Fee3R4C1LoanProfitContract.YearSlice(groupYear,groupStart,end,
            groupCount,groupDenom,"CAL2:PERSIAN:"+variant+":"+dataset));
        Fee3R4C1LoanProfitContract.yearFactor(start,end,result);
        return List.copyOf(result);
    }
    private static LocalDate parseISO(String value){
        try {return LocalDate.parse(value);}
        catch(RuntimeException e){throw new IllegalArgumentException("CAL2_CANONICAL_ISO_DATE_INVALID");}
    }
    private static boolean isLeap(String value){return "Y".equals(value)||"1".equals(value);}
    private static boolean isNonLeap(String value){return "N".equals(value)||"0".equals(value);}
}
