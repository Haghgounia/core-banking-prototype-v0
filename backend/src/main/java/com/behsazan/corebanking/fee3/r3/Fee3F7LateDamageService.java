package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1JalaliDayRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F7 pure arithmetic + authoritative SELECT-only CAL2 calendar evidence. */
@Service
public final class Fee3F7LateDamageService {
    private final Fee3R4C1JalaliDayRepository calendar;
    private final Fee3F7Cal2WorkdayRepository workdays;
    public Fee3F7LateDamageService(Fee3R4C1JalaliDayRepository calendar,
            Fee3F7Cal2WorkdayRepository workdays){this.calendar=calendar;this.workdays=workdays;}
    @Transactional(readOnly=true)
    public Fee3F7DomainSimulationService.Result preview(Fee3F7DomainSimulationService.Request request){
        try{
            var ctx=Fee3F7LateDamageContract.validate(request);
            List<Fee3F7LateDamageContract.Workday> rows=List.of();
            if("EXCLUDE_ALL_HOLIDAYS".equals(ctx.holidayPolicy())){
                rows=workdays.coveredDays(ctx.calendarCode(),ctx.due(),ctx.endExclusive());
            }else if("ROLL_DUE_TO_WORKDAY".equals(ctx.holidayPolicy())){
                // The due date itself is sufficient if it is already a business day.
                // Only non-business due dates require the 45-day look-ahead.
                rows=workdays.coveredDays(ctx.calendarCode(),ctx.due(),ctx.due().plusDays(1));
                if(!rows.get(0).business())
                    rows=workdays.coveredDays(ctx.calendarCode(),ctx.due(),ctx.due().plusDays(46));
            }
            LocalDate start=ctx.due().plusDays(1);
            if("ROLL_DUE_TO_WORKDAY".equals(ctx.holidayPolicy())&& !rows.isEmpty()&&!rows.get(0).business()){
                boolean found=false;
                for(int j=1;j<rows.size()&&j<=45;j++){
                    if(rows.get(j).business()){start=rows.get(j).date().plusDays(1);found=true;break;}
                }
                if(!found)return Fee3F7LateDamageContract.blocked("CAL2_NO_BUSINESS_DAY_WITHIN_45_DAYS");
            }
            if(start.isAfter(ctx.endExclusive()))start=ctx.endExclusive();
            List<com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract.YearSlice> slices=
                start.isBefore(ctx.endExclusive())?calendar.yearSlices(start,ctx.endExclusive()):List.of();
            return Fee3F7LateDamageContract.calculate(request,ctx,slices,rows);
        }catch(DataAccessException ex){
            return Fee3F7LateDamageContract.blocked("CAL2_READONLY_CALENDAR_UNAVAILABLE");
        }catch(IllegalArgumentException|ArithmeticException ex){
            String code=ex.getMessage();
            return Fee3F7LateDamageContract.blocked(code!=null&&code.matches("[A-Z0-9_]{3,100}")?
                code:"LATE_CALCULATION_BLOCKED");
        }
    }
}
