Core Banking Prototype 0.11.0 - ORG Forms Patch - 2026-10-07

Apply:
1) Extract this ZIP over the project root (preserve folders / overwrite files).
2) Verify:
   node tools\verify-org-forms.mjs
3) Backend build on Windows:
   cd backend
   mvnw.cmd -DskipTests package
4) Frontend build:
   cd frontend
   npm ci
   npm run build

Main UI route:
   /organization

This patch adds the forms/descriptors for all 35 current ORG tables.
Shared Employee, Contact Point and GEO masters are not duplicated in ORG.
