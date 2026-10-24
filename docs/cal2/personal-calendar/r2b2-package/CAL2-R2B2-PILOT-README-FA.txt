CAL2 R2-B2 PILOT — Patch overlay / FA

Source baseline: core-banking-prototype-v0-2026-10-24-0748.zip
Status: Candidate, not production-qualified; no Oracle migrations or DDL.

1. Back up the latest project root.
2. Extract this patch ZIP into that root, preserving folders and replacing same-named files.
3. Run with SYSTEM in SQL Developer (F5):
   database/oracle/cal2/verification/CAL2-R2B2-CONTRACT-VERIFY-READONLY.sql
4. Run node tools/verify-cal2-personal-r2b2.mjs
5. Build on Windows in a configured Java 21, Node 22, Maven/Angular environment:
   build-production.cmd
6. To test ONLY in a controlled single-user demo, set in the Backend CMD process:
   set CAL2_PERSONAL_R2B2_DEMO_ENABLED=true
   bin\start.cmd

Full Persian guidance:
 docs/cal2/personal-calendar/CAL2-R2B2-PILOT-UAT-2026-10-24-FA.md

Old /api/v1/calendar2/personal is disabled by default. New route is /api/v2/calendar2/personal.
No user identity is accepted from query or request JSON. Default demo is OFF.


NEW: one-click Windows runner (project root):
  RUN-CAL2-R2B2-PILOT.cmd
Guide: CAL2-R2B2-START-HERE-FA.md
The script verifies source, builds, then starts local single-user UAT.
This SOURCE ZIP includes no built executable JAR.
