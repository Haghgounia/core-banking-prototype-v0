# CIF 0.3.22 Fix27 - Contact/Address Grid & Party Role Context QA

## Baseline
- Source: `0.3.22-prototype-fix26`
- Target: `0.3.22-prototype-fix27`
- Database schema migration: **Not required**

## 1. False red state after saving Contact Point-to-Address association
Root cause: the reactive `FormGroup` was reset, but the native `FormGroupDirective` remained in submitted state after `(ngSubmit)`. Angular Material therefore continued to render required empty controls in error state.

Fix:
- bind `#associationFormDirective="ngForm"`;
- pass it to `saveAssociation(...)`;
- after successful persistence call `resetForm(...)`, `markAsPristine()` and `markAsUntouched()`.

Expected result: after successful save, contact/address selectors return to their initial empty state without red error highlighting.

## 2. Saved-record grids
The Address & Contact operational page now uses explicit tabular grids rather than stream-style record cards.

### Address grid columns
- Address type
- Address
- Postal code
- Primary
- Verification status
- Valid from
- Valid to
- Actions

### Contact grid columns
- Contact type
- Contact value
- Purpose
- Owner
- Primary
- Verification
- Valid from
- Valid to
- Actions

### Contact-to-address association grid columns
- Contact point
- Address
- Association type
- Status / primary for address
- Valid from
- Valid to
- Actions

The tables are horizontally scrollable on narrow screens.

## 3. Duplicate association guard
Added repository/service validation matching Oracle unique key `UQ_CPA_CONTACT_ADDRESS`:
`CONTACT_POINT_ID + PARTY_ADDRESS_ID + ASSOCIATION_TYPE_CODE + VALID_FROM`.

The service rejects a duplicate before database constraint failure and returns a Persian field-validation message. Update mode excludes the current record.

## 4. Party Role context UX
UI help now defines context as the scope in which a Party role is valid.
- General/bank-wide role: context normally blank.
- ACCOUNT: role limited to an account.
- PRODUCT: role limited to a product.
- BRANCH: role limited to a branch.
- CONTRACT: role limited to a contract.
- CASE: role limited to a case (current prototype resolves KYC cases).

`CONTEXT_TYPE_CODE` and `CONTEXT_ID` remain an optional pair enforced by the existing schema/application validation.

## Regression notes
- No Person/Organization identity schema or form logic changed.
- No KYC/Risk/Financial/Document behavior changed.
- Fix24 authority-document reference behavior is preserved.

## Build verification
- Maven compile could not run in the execution environment because Maven Wrapper download from Maven Central was unavailable.
- Frontend dependency install could not complete within the environment timeout, so full Angular compilation was not available here.
- Source-level checks were performed for changed template bindings, FormControl names, Java method signatures, duplicate-validation path, and package cleanliness.
