@echo off
setlocal
set "ROOT=%~dp0..\"
node "%ROOT%tools\verify-org-contacts-r1.mjs" || exit /b 1
node "%ROOT%tools\verify-org-contacts-release.mjs" || exit /b 1
node "%ROOT%tools\verify-org-menu-contact-review.mjs" || exit /b 1
node "%ROOT%tools\verify-org-unit-location-r1.mjs" || exit /b 1
node "%ROOT%tools\verify-org-unit-location-r2a.mjs" || exit /b 1
node "%ROOT%tools\verify-org-forms.mjs" || exit /b 1
echo ORG_CONTACTS_RELEASE_VERIFY_PASS
exit /b 0
