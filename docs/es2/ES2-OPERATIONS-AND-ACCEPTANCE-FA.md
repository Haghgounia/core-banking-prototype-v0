# Core Banking Prototype — Event Streaming ES2-PR1

**تاریخ:** 2026-10-17  
**مبنای سورس:** `core-banking-prototype-v0-2026-10-17-1443.zip`  
**هدف:** رفع ابهام UnknownTopicOrPartitionException بدون دست‌کاری سورس، Docker، Oracle و عملیات مالی.

## 1. یافته‌های مستند از سورس

- نام Topic پیش‌فرض `corebanking.pst.events.v1` است؛ مقدار `core-banking.event-stream.topic` در زمان اجرا می‌تواند آن را تغییر دهد.
- Producer و Consumer در سورس نام پیش‌فرض یکسان دارند.
- Consumer فقط `PST.POSTING_INBOX_RECEIPTS` را برای رسید حسابرسی به‌روزرسانی می‌کند و GL Posting را اجرا نمی‌کند.
- ES1 unit tests و static checks آزمون اتصال واقعی Kafka/Oracle نیستند.
- Event streaming به‌صورت پیش‌فرض فعال نیست؛ تولید/مصرف رویداد منوط به پیکربندی صریح است.
- در این بسته هیچ کد Backend و Frontend یا دیتابیس تغییر نکرده است.

## 2. نصب بسته (Overlay)

از ریشه پروژه (برای نمونه `D:\Projects-Patch\core-banking-prototype-patch\R1D`) فایل‌های دو پوشه `tools/es2/` و `docs/es2/` این Patch را کپی کنید.
این بسته مستقل است و نیاز ندارد کل سورس را جایگزین کنید. همه فایل‌های آن در مسیرهای جدید اضافه می‌شوند.

## 3. گام A — Preflight فقط خواندنی

از CMD در مسیر ریشه پروژه:

```bat
tools\es2\Run-ES2-Kafka-Preflight.cmd
```

یا اگر Topic واقعی Backend متفاوت است:

```bat
tools\es2\Run-ES2-Kafka-Preflight.cmd -Topic YOUR_APPROVED_TOPIC
```

پارامترهای اختیاری: `-ContainerName kafka`، `-ContainerBootstrap localhost:9092`، `-HostAddress 127.0.0.1`، `-HostPort 9092`.

**خروجی مورد انتظار برای صحت زیرساخت:** `ES2_PREFLIGHT_STATUS=PASS` و نتیجه PASS برای Docker، TCP، Metadata، نام دقیق Topic، Leader و ISR. این خروجی به‌تنهایی نشانه قبولی ES2 یکپارچه نیست.

اگر نتیجه `FAIL_3` بود، ابتدا نام مؤثر Topic را با پیکربندی در حال اجرای Backend تطبیق دهید. اسامی Default کد لزوماً با Override زمان اجرا یکسان نیستند.

## 4. گام B — ایجاد Topic فقط پس از تأیید عملیاتی

**این گام Kafka metadata را می‌نویسد و بخشی از Preflight نیست.** برای محیط تست تک‌Broker با تأیید مسئول اجرا:

```bat
tools\es2\Run-ES2-Create-Topic-Explicit.cmd -ApproveCreate
```

معادل Docker CLI:

```bat
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic corebanking.pst.events.v1 --partitions 3 --replication-factor 1
```

سپس دوباره گام A را اجرا کنید. اگر Topic از قبل وجود دارد، نباید آن را بی‌دلیل بازسازی یا حذف کرد. این نمونه `replication-factor=1` فقط برای Kafka تک‌Broker آزمایشی است، نه High Availability بانکی.

## 5. گام C — کنترل Oracle بدون نوشتن

فایل `docs/es2/ES2-Oracle-Metadata-READONLY.sql` را با حساب مجاز فقط‌خواندنی اجرا کنید.

پیش‌شرط فعال‌سازی: تمام ستون‌ها `OK`، کلید یکتای مستقل `EVENT_IDENTIFIER` در Outbox و کلید یکتای سه‌ستونی (`ACCOUNTING_PURPOSE`, `SOURCE_DOMAIN`, `SOURCE_EVENT_IDENTIFIER`) در Inbox باید وجود داشته باشند. کد SchemaGuard فقط Unique Constraint نوع U و وضعیت ENABLED/VALIDATED را می‌پذیرد.

**توجه:** اگر مجوز تغییر Oracle وجود ندارد، این مرحله صرفاً گزارش است؛ هیچ DDL یا DML نباید اجرا شود.

## 6. مرز کنترل و سناریوهای پذیرش واقعی

| ID | آزمون | نوع عملیات | معیار پذیرش | وضعیت این بسته |
|---|---|---|---|---|
| ES2-01 | Docker/TCP/Metadata/Topic/ISR | خواندنی | همه PASS | قابل اجرا توسط کاربر |
| ES2-02 | انطباق Runtime Topic با سورس | خواندنی | Topic دقیقاً یکسان | نیازمند پیکربندی واقعی |
| ES2-03 | بررسی SchemaGuard و Unique Constraints | Oracle SELECT | همه کنترل‌ها OK | قابل اجرا توسط کاربر |
| ES2-04 | Producer به Broker و broker ACK | Kafka publish + PST writes | Outbox تنها پس از ACK به PUBLISHED برسد | اجرا نشده |
| ES2-05 | Audit-only Consumer | Kafka read + PST Inbox write | رسید ثبت و سپس Offset ACK شود؛ GL بدون تغییر | اجرا نشده |
| ES2-06 | Retry و Backoff | Fault injection کنترل‌شده + PST writes | شکست به RETRY/QUARANTINED و بدون lost event | اجرا نشده |
| ES2-07 | Replay و Duplicate Event | Kafka publish + PST Inbox write | Event یکسان دوباره اثر مضاعف نگذارد | اجرا نشده |
| ES2-08 | Conflict و Poison Event | محیط آزمایش ایزوله | خطا Fail-closed و پیام ناسازگار ACK نشود | اجرا نشده |
| ES2-09 | Per-aggregate ordering | PST writes | برای aggregate یکسان، رویداد بعدی جلو نزند | اجرا نشده |
| ES2-10 | Financial isolation | بررسی شواهد و Oracle SELECT | هیچ GL posting/balance mutation | اجرا نشده |

**کنترل مهم:** هرگز برای آزمون Smoke از `kafka-console-producer` با متن آزمایشی روی Topic اصلی `corebanking.pst.events.v1` استفاده نکنید؛ Consumer دارای اعتبارسنجی Envelope و retry بی‌پایان است و پیام نامعتبر می‌تواند مصرف را در یک Partition متوقف کند. برای آزمون پیام مصنوعی از Topic جداگانه و Consumer آزمایشی استفاده شود، نه Topic عملیاتی PST.

**قواعد اجرای مرحله عملیاتی ES2-04 تا ES2-10:** تنها پس از مجوز مکتوب برای درج آزمایشی در جداول `PST`، تهیه snapshot/backup مناسب و اطمینان از محیط ایزوله. هرگز از داده بانکی واقعی یا Trigger مالی استفاده نکنید. این Patch هیچ راه‌انداز یا داده‌گذار عملیاتی ندارد و به‌تنهایی این آزمون‌ها را اجرا نمی‌کند.

## 7. نحوه گزارش شواهد

به همراه خروجی Preflight، این موارد را ثبت کنید:

1. نام دقیق Topic و خروجی `--describe` (Partitions/Leader/Replicas/Isr).
2. نام `spring.kafka.bootstrap-servers` و `core-banking.event-stream.topic` در Runtime، بدون اشتراک رمزهای عبور.
3. خروجی SQL فقط خواندنی (وضعیت ستون‌ها و Unique Constraint).
4. در اجرای بعدی و فقط با مجوز، ردیف‌های وضعیت Outbox/Inbox و زمان ACK/Offset را به‌صورت غیرحساس ثبت کنید.

## 8. نکات معماری و محدودیت‌ها

- Kafka تک‌Broker با Replication Factor یک برای اثبات مفهوم است، نه معماری تاب‌آور Production.
- Producer idempotence، Outbox و Inbox receipts به تنهایی Exactly-Once End-to-End را اثبات نمی‌کنند؛ تضمین واقعی باید با Retry/Replay و Failover کنترل شود.
- در `application.yml`های سورس، اطلاعات اتصال Oracle به‌صورت ثابت مشاهده شده است. برای استقرار فراتر از محیط تست باید Secret Externalization و Least Privilege انجام شود.
- تا پایان ES2، Event-driven GL Posting خارج از محدوده و غیرفعال است.

## 9. آزمون مستقل سورس

از ریشه پروژه با Node.js:

```bat
node tools\verify-event-streaming-es1.mjs
node tools\es2\verify-es2-source-contract.mjs
```

این آزمون‌های استاتیک نه به Kafka متصل می‌شوند و نه به Oracle. ES2 source verification فقط قراردادها و پیش‌شرط‌ها را بررسی می‌کند.
