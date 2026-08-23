# CIF 0.3.25 FIX36 - Party onboarding runtime diagnostics

## هدف
حذف ابهام میان نسخه منبع، JAR اجراشده و مقدار کشور ثبت در INSERT اولیه سازمان.

## اصلاحات
- نسخه Maven backend به `0.3.25-SNAPSHOT` ارتقا یافت.
- نسخه قابل مشاهده برنامه به `0.3.25-prototype-fee-p1` ارتقا یافت.
- نسخه frontend package نیز با `0.3.25` هم‌راستا شد.
- قبل از ایجاد Party سازمانی، مقدار `registrationCountryCode` ورودی در log ثبت می‌شود.
- درست قبل از `insertOrganization`، مقدار `registrationCountryCode` و `legalFormCode` ثبت می‌شود.
- منطق FIX35 بدون تغییر باقی است: `REGISTRATION_COUNTRY_CODE` از درخواست onboarding به `CreatePartyRequest` منتقل و در INSERT اولیه `ORGANIZATION` استفاده می‌شود.

## انتظار تست
در startup باید عبارت `Starting CoreBankingApplication v0.3.25-SNAPSHOT` دیده شود.
در ثبت سازمان باید دو log زیر دیده شوند و مقدار کشور `IRN` باشد:
- `Party onboarding organization input: registrationCountryCode=IRN`
- `Organization bootstrap insert: ... registrationCountryCode=IRN`

اگر startup هنوز `0.3.23-SNAPSHOT` باشد، JAR قدیمی اجرا شده است.
