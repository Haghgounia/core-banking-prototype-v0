# 0.3.22-prototype-fix2 — Party Address alignment

- `PARTY_ADDRESS.SOURCE_CODE` UI source: `CIF.REF_DATA_SOURCE`.
- `PARTY_ADDRESS.TENURE_TYPE_CODE`: database-backed `CIF.REF_TENURE_TYPE` added per explicit operational requirement. The original XMI/CIF-tables4 did not contain this reference table; the labels are taken from the operational HTML.
- `PARTY_ADDRESS.VERIFICATION_STATUS_CODE`: `CIF.REF_VERIFICATION_STATUS`.
- `PARTY_ADDRESS.VALID_FROM/VALID_TO`: validity interval of the Party-address relation; they are not verification timestamps.
- `ADDRESS.ADDRESS_LINE2`: retained physically/API-compatible but hidden from the operational form because the reference HTML exposes a single supplementary description field (`ADDRESS_DETAIL`).
- All party-reference lookup lists now exclude future/not-yet-valid and expired rows using `VALID_FROM/VALID_TO`.
