# FEE2 Studio Theme Alignment — 0.11.0

## هدف
چیدمان صفحات جدید FEE2 Studio که با الهام از Simple_Fee_Engine_R5_2_FA_UI_Full طراحی شده‌اند حفظ می‌شود، اما ظاهر، رنگ‌ها، سطوح، Border، Hover، وضعیت‌ها و Dark/Light Theme فقط از Design Tokenهای عمومی Core Banking Prototype استفاده می‌کنند.

## قرارداد UI
- Layout و Information Architecture استودیو حفظ شده است: Header داخلی، Scope، Navigation، Dashboard، Catalog، Wizard، Editor Tabs، Simulation، Runtime و Compare.
- Palette مستقل Studio حذف شده است.
- هیچ رنگ hard-coded در SCSSهای Studio باقی نمانده است.
- سطوح از `--app-surface`, `--app-surface-muted`, `--app-surface-hover` استفاده می‌کنند.
- متن از `--app-text`, `--app-muted`, `--app-placeholder` استفاده می‌کند.
- رنگ اصلی از `--app-primary`, `--app-primary-strong`, `--app-primary-soft` استفاده می‌کند.
- Success/Danger/Warning از Design Tokenهای عمومی سیستم استفاده می‌کنند.
- Table/Gridها با Border/Divider/Surface عمومی سیستم هم‌راستا شده‌اند.
- Light/Dark Theme بدون Palette جداگانه Studio پشتیبانی می‌شود.

## عدم کپی Source
هیچ فایل JavaScript/CSS/Java از پروژه Simple_Fee_Engine_R5_2_FA_UI_Full وارد نشده است. فقط Layout و Workflow آن به‌عنوان مرجع UX استفاده شده است.

## کنترل
`tools/verify-fee2-studio.mjs` اکنون علاوه بر کنترل ساختار Studio، عدم استفاده از رنگ hard-coded و الزام استفاده از Theme Tokenهای عمومی را نیز بررسی می‌کند.
