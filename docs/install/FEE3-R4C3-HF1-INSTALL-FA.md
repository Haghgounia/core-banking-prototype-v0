# FEE3-R4C3-HF1 — اصلاح دقت JSON در ابزار آزمون (فقط‌خواندنی)

**علت:** خروجی Java برای `yearFraction` از نوع `BigDecimal` است و به شکل عدد JSON ارسال می‌شود. تابع `json.loads` در ابزار پیشین، آن را به `float` باینری تبدیل می‌کرد؛ سپس مقایسه دقیق `Decimal` در چهار سناریوی مثبت با خطای کاذب مواجه می‌شد. این رفتار با نمونه JSON عددیِ ۳۴رقمی بازتولید شده است.

**تغییر دقیق:** فقط یک خط در `tools/fee3-backend/r4c3/runtime-readonly.py`:

```python
result=json.loads(raw.decode('utf8'),parse_float=Decimal)
```

هیچ فایل Java، هیچ SQL، هیچ API جدید، هیچ تغییر Oracle، هیچ ثبت مالی یا رفع محدودیت امنیتی وجود ندارد. اسکریپت نصب، هش فایل فعلی و سه فایل مبنا را بررسی می‌کند؛ در تعارض، بدون تغییر متوقف می‌شود. نصب مجدد امن و بدون تغییر است.

## دستورات Windows CMD

ZIP را بیرون از پروژه نگهداری یا پس از استخراج از ریشه پروژه خارج کنید، تا کنترل Build پروژه با فایل اضافی متوقف نشود.

```bat
cd /d D:\Projects\core-banking-prototype-v0
tar -xf FEE3-R4C3-HF1-PATCH.zip
move FEE3-R4C3-HF1-PATCH.zip ..\FEE3-R4C3-HF1-ARCHIVE.zip
python tools\fee3-backend\releases\r4c3-hf1\install.py --preview
python tools\fee3-backend\releases\r4c3-hf1\install.py --apply
python tools\fee3-backend\releases\r4c3-hf1\selftest.py
python tools\fee3-backend\r4c3\selftest.py
call tools\fee3-backend\r4c3\run-runtime-readonly.cmd
```

از آنجا که **فقط ابزار Python** تغییر می‌کند، به توقف Backend یا اجرای مجدد Build نیازی نیست. پیش از آزمون واقعی، Backend باید پاسخ `readiness` سالم با `draftWritesEnabled=false` بدهد؛ خود ابزار این شرط را نیز کنترل می‌کند.

نتیجه هدف: `FEE3_R4C3_RUNTIME_PASS=35 FAIL=0`. اگر باز هم شکست وجود داشت، خروجی و مخصوصاً فایل `docs\install\FEE3-R4C3-RUNTIME-RESULT.json` را ارسال کنید. از حذف شروط و تبدیل `FAIL` به `PASS` خودداری کنید.

## هشدار پذیرش

تا اجرای مجدد و موفق تست واقعی، R4-C3 **پذیرفته نشده** است. نرخ‌ها و برنامه اقساط هنوز از منابع معتبر تسهیلات گواهی نشده‌اند؛ تشخیص عدم مطالبه تکراری و مجوز وصول نیز خارج از محدوده این مرحله‌اند.

**نگهداری نسخه‌ها:** بسته اصلی R4-C3 و Manifest اصلی آن تغییر نمی‌کنند. پس از نصب HF1، اگر نصب‌کننده قدیمی R4-C3 را دوباره اجرا کنید، هش نسخه قدیمی ابزار را انتظار دارد و تعارض اعلام می‌کند؛ از اجرای دوباره آن خودداری کنید. کنترل و نصب مجدد را فقط از مسیر `releases\r4c3-hf1` انجام دهید.
