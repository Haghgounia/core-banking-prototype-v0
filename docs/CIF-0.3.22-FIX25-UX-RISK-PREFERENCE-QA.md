# CIF 0.3.22 Fix25 - UX / Risk / Inquiry / Preference / Duplicate Selection QA

## Scope
- Base: Fix24
- 8.2 Risk Assessment ordering and model-driven score guidance
- 8.4 External Inquiry provider reference selection
- 9.3 General Preference value clarification
- Additional identifier/classification duplicate-selection prevention
- Edit-box placeholder guidance
- Fix24 authority behavior regression

## Checks
- Risk model is selected before score entry; model version, minimum score and maximum score are displayed before score.
- Score validators remain model-driven using RiskModelProfile min/max.
- External inquiry provider uses CIF.REF_EXTERNAL_PROVIDER and backend validates provider/type/result codes.
- Current REF_PREFERENCE_TYPE values use grounded controls: LANGUAGE -> language list, CONTACT_TIME -> HH:mm, STATEMENT_DELIVERY -> REF_CHANNEL.
- Backend rejects incompatible preference type/value combinations.
- Additional identifier add-list excludes primary and already-added identifier types, while preserving the current type during edit.
- Classification type lookup excludes already-added types, while preserving the current type during edit.
- Authority document reference combo, formatted max amount, and IRR default from Fix24 remain present.
- Global placeholders use secondary/low-contrast theme color in light and dark modes.

## Database migration
No schema change is required for Fix25. REF_EXTERNAL_PROVIDER already exists in the CIF reference catalog.
