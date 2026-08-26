# CAL 0.3.42 / FIX53 - Java SQL Date import compile fix

## مشاهده
Build نسخه 0.3.41 در مرحله Maven compile با خطای `cannot find symbol: class Date` در `CalendarDatasetImportRepository.java` متوقف شد.

## علت
متد کمکی `toIso(Date value)` همچنان از `java.sql.Date` استفاده می‌کند، اما import این کلاس در تغییرات قبلی حذف شده بود.

## اصلاح
در `CalendarDatasetImportRepository.java` این import اضافه شد:

```java
import java.sql.Date;
```

هیچ تغییر رفتاری در Import انجام نشده است. Raw Transactional Import همچنان:
- هیچ Dataset/count validation اجرا نمی‌کند.
- ابتدا `CALENDAR_DAY` و سپس `CALENDAR_DATE` را Insert می‌کند.
- Commit در پایان موفق تراکنش توسط Spring انجام می‌شود.
- وضعیت Constraintهای Oracle را تغییر نمی‌دهد.

## کنترل‌های انجام‌شده
- sync system specification: PASS
- CIF persisted-grid verifier: PASS
- EA/Oracle comparison verifier: PASS
- Calendar reference verifier: PASS
- Calendar raw import verifier (19 checks): PASS

## Migration
ندارد.
