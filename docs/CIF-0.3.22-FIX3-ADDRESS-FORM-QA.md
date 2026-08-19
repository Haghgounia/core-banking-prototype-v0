# 0.3.22-prototype-fix3 — Party Address form alignment

- UI source: Party-Operation_Froms-1.html address page.
- Visible address fields: address type, country, province, county, city, district, neighborhood, main street, side street, plaque, floor, unit, postal code, address detail.
- Status/validity fields: primary flag, tenure type, valid from, valid to, verification status, source code.
- SOURCE_CODE lookup: CIF.REF_DATA_SOURCE.
- TENURE_TYPE_CODE lookup: CIF.REF_TENURE_TYPE (introduced in fix2).
- ADDRESS_LINE1 is derived and submitted to preserve physical-model compatibility.
- ADDRESS_LINE2 is not exposed and remains compatibility-only.
