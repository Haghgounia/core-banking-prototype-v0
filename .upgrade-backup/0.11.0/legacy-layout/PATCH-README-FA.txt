FEE3 prototype parity - scoped UI patch R1
Source baseline: core-banking-prototype-v0-2026-11-01-1440(1).zip
Reference: FCE_v13_8_84_Program_REDOWNLOAD(5).html
Changes: Only fee3-workspace.component.html and fee3-workspace.component.scss.
Moved the technical Oracle readonly preview after the main quick-calculation panel, behind an optional details disclosure. This keeps primary analyst test flow together. No TypeScript, backend, Oracle, or financial logic changed.
INSTALL: Copy the contained frontend/ directory into your project's root, then build with your existing production build procedure. Back up modified files first.
QA: Static structural check passed. Angular build/runtime verification NOT performed in this environment; no node_modules installed.
SCOPE LIMIT: This is a targeted layout correction, not a claim of full FCE prototype parity or completion of FEE3.
