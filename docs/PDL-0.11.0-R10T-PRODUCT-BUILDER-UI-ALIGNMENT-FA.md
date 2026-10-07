# PDL 0.11.0 / R10T — همسان‌سازی واسط کاربری محصول‌ساز

## هدف
حذف الگوی دوستونی List/Editor و همسان‌سازی فرم‌های محصول‌ساز با الگوی CIF و چهار سپرده.

## قرارداد بصری
- فرم ثبت/ویرایش تمام‌عرض و بالای Grid است.
- چیدمان Desktop سه‌ستونی، Tablet دوستونی و Mobile تک‌ستونی است.
- کد فنی در عنوان Combo/Grid به کاربر بانک نمایش داده نمی‌شود.

## مراجع بخش ۴
- نوع/وضعیت مشتری: CIF Party Reference
- بخش مشتری: DPS Customer Segment
- کانال: DPS Channel Reference
- مدرک/استعلام: DPS Product Reference
- واحد سازمانی: DPS Org Reference

## کنترل اختلال Reference
اگر داده مرجع اجباری بارگذاری نشود، فرم به‌جای پذیرش مقدار دستی، ثبت را مسدود می‌کند.

## Qualification
- verify-pdl-ui-alignment-r10t.mjs
- verify-pdl-common-rules-r10s.mjs
- verify-pdl-product-version-r10r.mjs
- verify-pdl-product-builder.mjs
- verify-release-layout.mjs
