EDU CIF R6 - targeted safe source cutover

Baseline: user output 2026-11-02, original EduLookupController.java (R3), descriptor EDU or already manually changed CIF.

Extract the ZIP to D:\Projects\core-banking-prototype-v0. Then run:
  tools\edu-cif-r6\INSTALL.cmd
  bin\stop.cmd
  build-production.cmd
  bin\start.cmd (ONLY when build succeeded)

Installer checks the expected controller SHA-256, accepts an already applied target, backs up changes, replaces the controller with CIF SQL equivalent of original EDU views, and changes ONLY the descriptor SCHEMA constant to CIF if needed.

No DDL/DML/COMMIT; no GEO table changes. EDU schema is NOT dropped.

Verify UI table label CIF.EDUCATION_INSTITUTIONS and GET /api/v1/education/lookups/{fields,levels,institutions,field-levels}?limit=3. Check grants and read-only parity before retiring EDU.

Full build/runtime tests are NOT performed here.
