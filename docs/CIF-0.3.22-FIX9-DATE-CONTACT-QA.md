# CIF 0.3.22 Fix 9 - Date / Contact / Address Source / Employment QA

- Party birth date is required and must be strictly before the system date.
- Birth date equal to the system date has a dedicated Persian validation message; it is not reported as a generic required-field error.
- Identifier issue date cannot be after the system date; for PERSON it cannot precede birth date.
- Identifier expiry date cannot precede issue date.
- Persian date control supports min/max Gregorian bounds while displaying Persian dates.
- Contact owner is selectable: CUSTOMER / REPRESENTATIVE / COMPANY with Persian labels from the operational form.
- Contact verification status/method choices display Persian labels only; database/API codes are unchanged.
- Verified-at is controlled by the Backend system clock for new verification/re-verification and cannot be back-dated through direct API calls. Existing historical timestamps are retained when the contact remains verified.
- Contact VALID_TO must be today or later and must not precede VALID_FROM; the UI minimum is max(today, VALID_FROM).
- Employment status is selectable from the six operational-form values; JOB_STATUS and EMPLOYMENT_STATUS_CODE are synchronized and validated server-side.
- Party address source is loaded from dedicated CIF.REF_ADDRESS_SOURCE with CUSTOMER_DECLARATION / POSTAL_SYSTEM / RESIDENCE_DOCUMENT and Persian labels اظهار مشتری / سامانه پست / مدرک سکونت.
- Generic CIF.REF_DATA_SOURCE is not used for the address-source ComboBox.
- Runtime CIF reference catalog: 98 tables/forms; total system reference forms: 168.
- Required migration: database/oracle/cif/migrations/0.3.22-fix9-contact-date-address-source.sql
