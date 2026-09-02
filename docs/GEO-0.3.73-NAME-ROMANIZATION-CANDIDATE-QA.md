# GEO 0.3.73 - Name Romanization Candidate Resolver QA

## هدف
فرم «تبدیل نام فارسی به انگلیسی» باید بین «نام حل نشده» و «معادل پیشنهادی موجود ولی Auto-fill غیرمجاز» تفاوت بگذارد.

## قرارداد Backend
- Endpoint: `POST /api/v1/name-romanization/resolve`
- Resolver: `GEO.PKG_NAME_ROMANIZATION.RESOLVE_NAME`
- نتیجه شامل نام نرمال‌شده، معادل انگلیسی پیشنهادی، روش Resolve، Confidence، وضعیت Auto-fill و نیاز به Review است.
- اگر رکورد Exact در `GEO.NAME_ROMANIZATION_DICTIONARY` وجود داشته باشد، `GOVERNANCE_STATUS_CODE` نیز برگردانده می‌شود.
- `GEO.FN_ROMANIZE_NAME` همچنان برای Auto-fill امن بدون تغییر باقی می‌ماند و فقط رکوردهای مجاز را برمی‌گرداند.

## رفتار مورد انتظار UI
- `محمد` -> `Mohammad`، Auto-fill بله.
- `سید محمد` -> `Seyed Mohammad`، مطابق Resolver و Rule فعال.
- `آقا مصطفی` یا `آقامصطفی` -> `Agha Mostafa`، معادل پیشنهادی نمایش داده شود؛ Auto-fill خیر؛ Review بله.
- نامی که Resolver برای آن English ندارد -> پیام «معادل انگلیسی پیدا نشد».

## Governance
نمایش Candidate به معنی تأیید آن برای اطلاعات مشتری نیست. فقط `AUTO_FILL_ALLOWED=1` مجاز به تکمیل خودکار است و املای لاتین مدرک هویتی معتبر بر Dictionary عمومی اولویت دارد.
