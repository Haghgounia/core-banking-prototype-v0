-- PDL R30-HF6: diagnostic only. No DML, DDL, or transaction.
-- Compare legacy and HF6 deposit versions; a missing/invalid legacy profile is NOT auto-fixed.
SELECT p.product_id,
       p.product_family_code,
       v.product_version_id,
       v.version_no,
       v.version_status_code,
       COUNT(dp.deposit_product_profile_id) AS profile_count,
       MAX(dp.deposit_group_code) AS stored_deposit_group_code,
       MAX(dp.deposit_type_code) AS stored_deposit_type_code,
       CASE p.product_family_code
          WHEN 'QARD_SAVINGS' THEN 'SAVINGS'
          WHEN 'SAVINGS' THEN 'SAVINGS'
          WHEN 'CURRENT_ACCOUNT' THEN 'CURRENT_NO_INTEREST'
          WHEN 'CURRENT' THEN 'CURRENT_NO_INTEREST'
          WHEN 'SHORT_TERM_DEPOSIT' THEN 'SHORT_TERM_DEPOSIT'
          WHEN 'LONG_TERM_DEPOSIT' THEN 'LONG_TERM_DEPOSIT'
          WHEN 'CERTIFICATE_OF_DEPOSIT' THEN 'CERTIFICATE_OF_DEPOSIT'
       END AS expected_deposit_type_code,
       CASE
          WHEN COUNT(dp.deposit_product_profile_id) = 0 THEN 'MISSING'
          WHEN COUNT(dp.deposit_product_profile_id) > 1 THEN 'DUPLICATE'
          WHEN MAX(dp.deposit_type_code) <> CASE p.product_family_code
             WHEN 'QARD_SAVINGS' THEN 'SAVINGS'
             WHEN 'SAVINGS' THEN 'SAVINGS'
             WHEN 'CURRENT_ACCOUNT' THEN 'CURRENT_NO_INTEREST'
             WHEN 'CURRENT' THEN 'CURRENT_NO_INTEREST'
             WHEN 'SHORT_TERM_DEPOSIT' THEN 'SHORT_TERM_DEPOSIT'
             WHEN 'LONG_TERM_DEPOSIT' THEN 'LONG_TERM_DEPOSIT'
             WHEN 'CERTIFICATE_OF_DEPOSIT' THEN 'CERTIFICATE_OF_DEPOSIT'
          END THEN 'TYPE_MISMATCH'
          ELSE 'OK'
       END AS profile_readiness
  FROM PDL.PRODUCT p
  JOIN PDL.PRODUCT_VERSION v ON v.product_id = p.product_id
  LEFT JOIN PDL.DEPOSIT_PRODUCT_PROFILE dp ON dp.product_version_id = v.product_version_id
 WHERE p.product_class_code = 'DEPOSIT'
   AND p.product_family_code IN
       ('QARD_SAVINGS', 'SAVINGS', 'CURRENT_ACCOUNT', 'CURRENT',
        'SHORT_TERM_DEPOSIT', 'LONG_TERM_DEPOSIT', 'CERTIFICATE_OF_DEPOSIT')
 GROUP BY p.product_id, p.product_family_code, v.product_version_id,
          v.version_no, v.version_status_code
 ORDER BY p.product_id, v.version_no;
