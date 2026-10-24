package com.behsazan.corebanking.calendar2.personalv2;

import com.behsazan.corebanking.calendar2.personalv2.PersonalV2Models.*;
import com.behsazan.corebanking.calendar2.personalv2.PersonalV2Repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Transactional personal-calendar application service against the installed R2-B1 model. */
@Service
public class PersonalV2Service {
    private final PersonalV2Repository db;
    private final String demoActor;
    private final boolean demoEnabled;
    private final String issuer;
    private final ZoneId defaultZone;
    private static final int HORIZON_DAYS = 90;
    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    public PersonalV2Service(PersonalV2Repository db,
            @Value("${core-banking.calendar2.personal.r2b2.demo-enabled:false}") boolean demoEnabled,
            @Value("${core-banking.calendar2.personal.r2b2.demo-subject:prototype-admin}") String demoActor,
            @Value("${core-banking.calendar2.personal.r2b2.issuer:urn:core-banking:local}") String issuer,
            @Value("${core-banking.calendar2.personal.r2b2.zone:Asia/Tehran}") String zone) {
        this.db=db;this.demoEnabled=demoEnabled;
        this.demoActor=demoActor;this.issuer=issuer;this.defaultZone=ZoneId.of(zone);
        if(demoEnabled && (demoActor==null || !demoActor.matches("[A-Za-z0-9_.-]{1,128}")))
            throw new IllegalArgumentException("Unsafe demo subject");
        if(issuer==null || issuer.isBlank() || issuer.length()>255)
            throw new IllegalArgumentException("Unsafe identity issuer");
    }

    /** No user-controlled owner request params or headers. Production needs an authenticated principal. */
    public Owner owner(Principal principal) {
        if(principal!=null && principal.getName()!=null && !principal.getName().isBlank()) {
            if(principal.getName().length()>128) throw bad(HttpStatus.UNAUTHORIZED,"Invalid principal");
            return new Owner(issuer,principal.getName());
        }
        if(demoEnabled) return new Owner("urn:core-banking:prototype",demoActor);
        throw bad(HttpStatus.UNAUTHORIZED,"Authentication is required for personal calendar");
    }
    private static ResponseStatusException bad(HttpStatus status,String message) {
        return new ResponseStatusException(status,message);
    }
    private static long id(String raw) {
        if(raw==null || !raw.matches("[1-9][0-9]{0,11}")) throw bad(HttpStatus.BAD_REQUEST,"Invalid NUMBER(12) id");
        return Long.parseLong(raw);
    }
    private static BigDecimal dayId(String raw) {
        if(raw==null || !raw.matches("[1-9][0-9]{0,18}"))
            throw bad(HttpStatus.BAD_REQUEST,"Invalid CAL2 NUMBER(19) day id");
        return new BigDecimal(raw);
    }
    private static long precondition(String header) {
        if(header==null || header.isBlank()) throw bad(HttpStatus.PRECONDITION_REQUIRED,"If-Match is required");
        if(!header.matches("\\\"[1-9][0-9]{0,9}\\\""))
            throw bad(HttpStatus.PRECONDITION_FAILED,"Invalid If-Match ETag");
        return Long.parseLong(header.substring(1,header.length()-1));
    }
    private record Normalized(BigDecimal day,String type,String title,String description,Integer startMinute,Integer minute,
                              String reminderTime,int advance,String repeat,String zone,String frequency,
                              BigDecimal variant,int interval,Integer dayOfMonth,Integer monthOfYear,
                              String mask,String dayPolicy) {}

    private Normalized normalize(SaveRequest body) {
        if(body==null)throw bad(HttpStatus.BAD_REQUEST,"Request body is required");
        BigDecimal day=dayId(body.dayId());
        if(!db.dayExists(day))throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"CAL2 day does not exist");
        String type=body.entryType()==null?"":body.entryType().trim().toUpperCase(Locale.ROOT);
        if(!List.of("NOTE","TASK","APPOINTMENT").contains(type))
            throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Entry type must be NOTE, TASK or APPOINTMENT");
        String title=body.title()==null?"":body.title().trim();
        if(title.isEmpty()||title.length()>200)throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Title must have 1..200 characters");
        String description=body.description()==null?null:body.description().trim();
        if(description!=null && description.length()>100_000)
            throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Description too long");
        String zone=body.timeZoneId()==null?defaultZone.getId():body.timeZoneId();
        try { ZoneId.of(zone); } catch(DateTimeException x) { throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Invalid IANA time zone"); }
        String time=body.reminderTime()==null?null:body.reminderTime().trim();
        Integer minute=null;
        if(time!=null && !time.isEmpty()) {
            if(!time.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"))
                throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Reminder time must be HH:mm");
            minute=LocalTime.parse(time).toSecondOfDay()/60;
        } else {time=null;}
        String start=body.localStartTime()==null?null:body.localStartTime().trim();
        Integer startMinute=null;
        if(start!=null && !start.isEmpty()) {
            if(!start.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"))
                throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Event start time must be HH:mm");
            startMinute=LocalTime.parse(start).toSecondOfDay()/60;
        }
        int advance=body.advanceDays()==null?0:body.advanceDays();
        if(advance<0||advance>30)throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Advance days must be 0..30");
        if(time==null && advance!=0)throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Advance requires reminder time");
        String repeat=body.repeatPolicy()==null?"DAILY":body.repeatPolicy().trim().toUpperCase(Locale.ROOT);
        if(!List.of("DAILY","ONCE").contains(repeat))throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Invalid advance repeat policy");
        String frequency=body.recurrenceFrequency()==null?"NONE":body.recurrenceFrequency().trim().toUpperCase(Locale.ROOT);
        if(!List.of("NONE","DAILY","WEEKLY","MONTHLY","YEARLY").contains(frequency))
            throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Unsupported recurrence frequency");
        BigDecimal variant=null;
        int interval=body.recurrenceInterval()==null?1:body.recurrenceInterval();
        if(interval<1||interval>100)throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Recurrence interval out of bounds");
        Integer dom=body.dayOfMonth(),month=body.monthOfYear();
        String mask=body.byWeekdaysMask();
        String policy=body.dayResolutionPolicy()==null?"SKIP":body.dayResolutionPolicy().trim().toUpperCase(Locale.ROOT);
        if(!List.of("SKIP","LAST_DAY_IF_INVALID").contains(policy))
            throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Invalid day resolution policy");
        if(!"NONE".equals(frequency)) {
            variant=dayId(body.calendarVariantId());
            if(!db.calendarVariantExists(variant) || db.variantDate(variant,day).isEmpty())
                throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Calendar variant does not cover start day");
            VariantDay anchor=db.variantDate(variant,day).orElseThrow();
            if(dom==null)dom=anchor.day();
            if(month==null)month=anchor.month();
            if(dom<1||dom>31 ||month<1||month>12)
                throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Unsupported month/day for variant");
            if(mask==null && "WEEKLY".equals(frequency)) {
                char[] chars="0000000".toCharArray();chars[db.dayDate(day).getDayOfWeek().getValue()-1]='1';mask=new String(chars);
            }
            if(mask!=null && (!mask.matches("[01]{7}")||mask.equals("0000000")))
                throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Invalid weekly weekdays mask");
        }
        return new Normalized(day,type,title,description,startMinute,minute,time,advance,repeat,zone,frequency,
                variant,interval,dom,month,mask,policy);
    }

    @Transactional(readOnly=true)
    public List<Entry> entries(Owner owner,LocalDate from,LocalDate to) {
        if(from==null||to==null||to.isBefore(from)||to.isAfter(from.plusDays(62)))
            throw bad(HttpStatus.BAD_REQUEST,"Query range must be 1..63 days");
        return db.entries(owner,from,to);
    }
    @Transactional(readOnly=true)
    public Entry find(Owner owner,String raw) { return required(owner,id(raw)); }
    private Entry required(Owner owner,long id) {
        return db.entry(owner,id).orElseThrow(()->bad(HttpStatus.NOT_FOUND,"Entry not found"));
    }
    private static byte[] sha(SaveRequest b) {
        // Length-prefix each field: a note containing separators cannot collide with a different payload.
        StringBuilder bytes = new StringBuilder();
        Object[] fields={b.dayId(),b.entryType(),b.title(),b.description(),b.localStartTime(),b.reminderTime(),b.advanceDays(),
                b.repeatPolicy(),b.timeZoneId(),b.recurrenceFrequency(),b.calendarVariantId(),
                b.recurrenceInterval(),b.dayOfMonth(),b.monthOfYear(),b.byWeekdaysMask(),b.dayResolutionPolicy()};
        for(Object field:fields) {
            if(field==null){bytes.append("-1:");continue;}
            String str=field.toString();bytes.append(str.length()).append(':').append(str);
        }
        try {return MessageDigest.getInstance("SHA-256").digest(bytes.toString().getBytes(StandardCharsets.UTF_8));}
        catch(NoSuchAlgorithmException exception){throw new IllegalStateException("SHA-256 unavailable",exception);}
    }

    @Transactional
    public Entry create(Owner owner,String key,SaveRequest request) {
        if(key==null||!key.matches("[A-Za-z0-9_.:-]{8,128}"))
            throw bad(HttpStatus.BAD_REQUEST,"Idempotency-Key must be 8..128 opaque characters");
        Normalized input=normalize(request);
        byte[] hash=sha(request);
        long existing=db.idempotencyExisting(owner,key,hash);
        if(existing==-1L)throw bad(HttpStatus.CONFLICT,"Idempotency key used for a different request");
        if(existing>0)return required(owner,existing);
        try {db.claimIdempotency(owner,key,hash);}
        catch(DuplicateKeyException error) {throw bad(HttpStatus.CONFLICT,"Concurrent idempotency request; retry with same key");}
        long entry=db.next("USER_CALENDAR_ENTRIES");
        db.insertEntry(owner,entry,input.day(),input.type(),input.title(),input.description(),input.startMinute(),input.zone());
        Long recurrenceId=null;
        if(!"NONE".equals(input.frequency())) {
            recurrenceId=db.insertRecurrence(entry,input.variant(),input.frequency(),input.interval(),input.dayOfMonth(),
                    input.monthOfYear(),input.mask(),input.dayPolicy(),owner.subjectId());
        }
        db.insertOccurrence(entry,input.day(),"D:"+input.day().toPlainString(),input.startMinute(),
                dueUtc(db.dayDate(input.day()),input.startMinute(),input.zone()),recurrenceId);
        saveReminders(entry,input,owner);
        if(!"NONE".equals(input.frequency())) expandOne(db.activeRecurrences().stream()
               .filter(r->r.id()==entry).findFirst().orElseThrow(),LocalDate.now(defaultZone));
        generateAlerts(entry);
        db.finishIdempotency(owner,key,entry);
        return required(owner,entry);
    }

    private void saveReminders(long entry,Normalized input,Owner owner) {
        if(input.minute()==null)return;
        db.createReminder(entry,"AT_DUE","AT_DUE",0,null,null,null,null,
                input.startMinute()!=null?input.startMinute():input.minute(),owner.subjectId());
        if(input.advance()>0) {
            if("DAILY".equals(input.repeat()))
                db.createReminder(entry,"DAILY_WINDOW","BEFORE",null,null,input.advance(),1,1,input.minute(),owner.subjectId());
            else db.createReminder(entry,"SINGLE_OFFSET","BEFORE",input.advance(),"CALENDAR_DAY",
                    null,null,null,input.minute(),owner.subjectId());
        }
    }

    @Transactional
    public Entry update(Owner owner,String raw,String ifMatch,SaveRequest body) {
        long id=id(raw),version=precondition(ifMatch);
        Entry before=required(owner,id);
        if(before.versionNo()!=version)throw bad(HttpStatus.PRECONDITION_FAILED,"Stale entry ETag");
        if(db.hasRecurrence(id))throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Editing a series requires SPLIT_SERIES (future milestone)");
        if(db.hasAcknowledgedAlerts(id) || !"OPEN".equals(before.status()))
            throw bad(HttpStatus.CONFLICT,"Entry has completed or acknowledged history; cancel and create a successor");
        Normalized data=normalize(body);
        if(!"NONE".equals(data.frequency()))
            throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Cannot turn an existing entry into a series; create a new entry");
        if(db.updateEntry(owner,id,version,data.day(),data.type(),data.title(),data.description(),
                data.startMinute(),data.zone())!=1)throw bad(HttpStatus.PRECONDITION_FAILED,"Concurrent modification");
        db.cancelPendingAlertsForEntry(id);
        db.disableReminders(id);
        if(db.updateOneOccurrence(owner,Long.parseLong(before.occurrenceId()),id,data.day(),data.startMinute(),
                dueUtc(db.dayDate(data.day()),data.startMinute(),data.zone()))!=1)
            throw bad(HttpStatus.CONFLICT,"Occurrence is no longer editable");
        saveReminders(id,data,owner);
        generateAlerts(id);
        return required(owner,id);
    }

    @Transactional
    public void delete(Owner owner,String raw,String ifMatch) {
        long id=id(raw),version=precondition(ifMatch);
        if(db.cancelEntry(owner,id,version)!=1)
            throw bad(HttpStatus.PRECONDITION_FAILED,"Entry is missing or its version has changed");
        db.cancelPendingAlertsForEntry(id);
        db.disableReminders(id);
    }

    @Transactional
    public Entry complete(Owner owner,String raw,String ifMatch,boolean done) {
        long id=id(raw),version=precondition(ifMatch);
        Entry before=db.occurrence(owner,id).orElseThrow(()->bad(HttpStatus.NOT_FOUND,"Occurrence not found"));
        if(!"TASK".equals(before.entryType()))throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Only tasks can be completed");
        if(db.setOccurrenceCompleted(owner,id,version,done)!=1)
            throw bad(HttpStatus.PRECONDITION_FAILED,"Occurrence version changed");
        if(done) db.cancelAlertsForOccurrence(id);
        // Reopening never resurrects cancelled alerts silently: a fresh rule would need explicit scheduling.
        return db.occurrence(owner,id).orElseThrow(()->bad(HttpStatus.NOT_FOUND,"Occurrence not found"));
    }

    @Transactional(readOnly=true)
    public Inbox inbox(Owner owner) {
        return new Inbox("کاربر جاری",defaultZone.getId(),db.availableAlerts(owner,LocalDate.now(defaultZone)));
    }
    @Transactional
    public void acknowledge(Owner owner,String raw) {
        if(db.acknowledge(owner,id(raw))!=1)
            throw bad(HttpStatus.CONFLICT,"Alert not available or not owned");
    }
    @Transactional
    public void snooze(Owner owner,String raw,int minutes) {
        if(minutes<5||minutes>1440)throw bad(HttpStatus.UNPROCESSABLE_ENTITY,"Snooze must be 5..1440 minutes");
        if(db.snooze(owner,id(raw),minutes)!=1)
            throw bad(HttpStatus.CONFLICT,"Alert not available or not owned");
    }

    private static String dueUtc(LocalDate day,Integer minute,String zone) {
        return minute==null?null:utc(day,minute,zone);
    }
    private static String utc(LocalDate day,int minute,String zone) {
        return day.atStartOfDay().plusMinutes(minute).atZone(ZoneId.of(zone)).toInstant()
             .atOffset(ZoneOffset.UTC).format(UTC_FORMAT);
    }

    private void generateAlerts(long entryId) {
        List<Reminder> rules=db.reminders(entryId);
        if(rules.isEmpty())return;
        for(Occurrence occ:db.occurrencesForEntry(entryId)) {
            for(Reminder r:rules) {
                int minute=r.alertMinute()==null?(occ.startMinute()==null?540:occ.startMinute()):r.alertMinute();
                if("AT_DUE".equals(r.pattern()))
                    db.insertAlert(occ.id(),entryId,r,"DUE:"+occ.dayId().toPlainString(),utc(occ.date(),minute,occ.zone()));
                else if("SINGLE_OFFSET".equals(r.pattern()) && "CALENDAR_DAY".equals(r.unit())) {
                    int offset=r.offset();
                    db.insertAlert(occ.id(),entryId,r,"ADV:"+offset,utc(occ.date().minusDays(offset),minute,occ.zone()));
                } else if("DAILY_WINDOW".equals(r.pattern())) {
                    for(int d=r.startDays();d>=r.endDays();d-=r.intervalDays())
                        db.insertAlert(occ.id(),entryId,r,"ADV:"+d,utc(occ.date().minusDays(d),minute,occ.zone()));
                }
            }
        }
    }

    /** Called only from the opt-in server scheduler; never from a browser timer. */
    @Transactional
    public int reconcile() {
        LocalDate today=LocalDate.now(defaultZone);
        int materialized=0;
        for(RecurringEntry r:db.activeRecurrences())materialized+=expandOne(r,today);
        db.releaseEligibleAlerts();
        return materialized;
    }
    private int expandOne(RecurringEntry r,LocalDate today) {
        VariantDay anchor=db.variantDate(r.variant(),r.startDay()).orElse(null);
        if(anchor==null)return 0; // Covered reference data removed: no guessed calendar dates.
        LocalDate lower=anchor.date().isAfter(today)?anchor.date():today.minusDays(1);
        LocalDate upper=today.plusDays(HORIZON_DAYS);
        if(lower.isAfter(upper))return 0;
        List<VariantDay> candidates=db.variantDays(r.variant(),lower,upper.plusDays(35));
        Map<String,List<VariantDay>> byMonth=new LinkedHashMap<>();
        for(VariantDay c:candidates) byMonth.computeIfAbsent(c.year()+"/"+c.month(),k->new ArrayList<>()).add(c);
        int count=0;
        for(VariantDay candidate:candidates) {
            if(candidate.date().isAfter(upper))break;
            if(!matches(r,anchor,candidate,byMonth))continue;
            String slot="D:"+candidate.dayId().toPlainString();
            if(db.occurrenceBySlot(r.id(),slot).isEmpty()) {
                try {
                    db.insertOccurrence(r.id(),candidate.dayId(),slot,r.localMinute(),
                             dueUtc(candidate.date(),r.localMinute(),r.zone()),r.ruleId());
                    count++;
                }catch(DuplicateKeyException ignored) { /* slot uniqueness handles concurrent scheduler */ }
            }
        }
        generateAlerts(r.id());
        return count;
    }
    private static boolean matches(RecurringEntry r,VariantDay anchor,VariantDay day,
                                   Map<String,List<VariantDay>> months) {
        if(day.date().isBefore(anchor.date()))return false;
        return switch(r.frequency()) {
            case "DAILY" -> ChronoUnit.DAYS.between(anchor.date(),day.date())%r.interval()==0;
            case "WEEKLY" -> {
                long weeks=ChronoUnit.WEEKS.between(anchor.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                        day.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
                String mask=r.weekdayMask();
                yield weeks%r.interval()==0 && mask!=null && mask.charAt(day.date().getDayOfWeek().getValue()-1)=='1';
            }
            case "MONTHLY", "YEARLY" -> {
                // Supported calendar systems in the prototype have 12 numbered months.
                if(day.month()>12 || anchor.month()>12) yield false;
                long monthIndex=((long)day.year()-anchor.year())*12+day.month()-anchor.month();
                boolean interval="MONTHLY".equals(r.frequency())
                        ? monthIndex>=0 && monthIndex%r.interval()==0
                        : day.month()==r.targetMonth() && (day.year()-anchor.year())%r.interval()==0;
                if(!interval)yield false;
                List<VariantDay> all=months.get(day.year()+"/"+day.month());
                int last=all.stream().mapToInt(VariantDay::day).max().orElse(0);
                yield day.day()==r.targetDay() ||
                        ("LAST_DAY_IF_INVALID".equals(r.policy()) && r.targetDay()>last && day.day()==last);
            }
            default -> false;
        };
    }
}
