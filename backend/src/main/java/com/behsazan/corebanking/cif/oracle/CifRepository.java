package com.behsazan.corebanking.cif.oracle;

import com.behsazan.corebanking.cif.domain.CifModels.CifDashboardSummary;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointAddressRecord;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointAddressRequest;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointRecord;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointRequest;
import com.behsazan.corebanking.cif.domain.CifModels.CreatePartyRequest;
import com.behsazan.corebanking.cif.domain.CifModels.FinancialProfileRecord;
import com.behsazan.corebanking.cif.domain.CifModels.FinancialProfileRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAssetLiabilityRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAssetLiabilityRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyEmploymentRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyEmploymentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyIncomeSourceRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyIncomeSourceRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyLicenseRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyLicenseRequest;
import com.behsazan.corebanking.cif.domain.CifModels.KycCaseRecord;
import com.behsazan.corebanking.cif.domain.CifModels.KycCaseRequest;
import com.behsazan.corebanking.cif.domain.CifModels.OrganizationProfile;
import com.behsazan.corebanking.cif.domain.CifModels.OrganizationRequest;
import com.behsazan.corebanking.cif.domain.CifModels.Party360Response;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAddressRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAddressRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyCore;
import com.behsazan.corebanking.cif.domain.CifModels.PartyDocumentRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyDocumentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyIdentifierRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyIdentifierRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyNameRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyNameRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartySummary;
import com.behsazan.corebanking.cif.domain.CifModels.PersonProfile;
import com.behsazan.corebanking.cif.domain.CifModels.PersonRequest;
import com.behsazan.corebanking.cif.domain.CifModels.RiskAssessmentRecord;
import com.behsazan.corebanking.cif.domain.CifModels.RiskAssessmentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.ScreeningResultRecord;
import com.behsazan.corebanking.cif.domain.CifModels.ScreeningResultRequest;
import com.behsazan.corebanking.cif.domain.CifModels.UpdatePartyRequest;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public class CifRepository {
    private static final Pattern SQL_NAME = Pattern.compile("[A-Z][A-Z0-9_]*");

    private final JdbcClient jdbc;
    private final String schema;

    public CifRepository(JdbcClient jdbc, @Value("${core-banking.schemas.cif:CIF}") String schema) {
        this.jdbc = jdbc;
        String normalized = schema == null ? "CIF" : schema.trim().toUpperCase(Locale.ROOT);
        if (!SQL_NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid CIF schema name: " + schema);
        }
        this.schema = normalized;
    }

    public PageResponse<PartySummary> searchParties(
            String text,
            String partyType,
            String lifecycleStatus,
            int page,
            int size
    ) {
        String nameView = """
                LEFT JOIN (
                    SELECT PARTY_ID,
                           MAX(COALESCE(DISPLAY_NAME, FULL_NAME)) KEEP (
                               DENSE_RANK FIRST ORDER BY CASE WHEN IS_PRIMARY = 'Y' THEN 0 ELSE 1 END,
                               VALID_FROM DESC, PARTY_NAME_ID DESC
                           ) AS DISPLAY_NAME
                    FROM %s
                    GROUP BY PARTY_ID
                ) PN ON PN.PARTY_ID = P.PARTY_ID
                """.formatted(table("PARTY_NAME"));
        String identifierView = """
                LEFT JOIN (
                    SELECT PARTY_ID,
                           MAX(IDENTIFIER_VALUE) KEEP (
                               DENSE_RANK FIRST ORDER BY CASE WHEN IS_PRIMARY = 'Y' THEN 0 ELSE 1 END,
                               VALID_FROM DESC, PARTY_IDENTIFIER_ID DESC
                           ) AS IDENTIFIER_VALUE
                    FROM %s
                    GROUP BY PARTY_ID
                ) PI ON PI.PARTY_ID = P.PARTY_ID
                """.formatted(table("PARTY_IDENTIFIER"));

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        Map<String, Object> params = new LinkedHashMap<>();
        if (text != null && !text.isBlank()) {
            where.append(" AND (TO_CHAR(P.PARTY_ID) LIKE :text OR UPPER(PN.DISPLAY_NAME) LIKE :textUpper OR UPPER(PI.IDENTIFIER_VALUE) LIKE :textUpper) ");
            params.put("text", "%" + text.trim() + "%");
            params.put("textUpper", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        if (partyType != null && !partyType.isBlank()) {
            where.append(" AND P.PARTY_TYPE_CODE = :partyType ");
            params.put("partyType", partyType.trim().toUpperCase(Locale.ROOT));
        }
        if (lifecycleStatus != null && !lifecycleStatus.isBlank()) {
            where.append(" AND P.LIFECYCLE_STATUS_CODE = :lifecycleStatus ");
            params.put("lifecycleStatus", lifecycleStatus.trim().toUpperCase(Locale.ROOT));
        }

        String from = " FROM " + table("PARTY") + " P " + nameView + identifierView;
        long total = jdbc.sql("SELECT COUNT(*) " + from + where)
                .params(params)
                .query(Long.class)
                .single();

        Map<String, Object> pageParams = new LinkedHashMap<>(params);
        pageParams.put("offset", (long) page * size);
        pageParams.put("size", size);
        String sql = """
                SELECT P.PARTY_ID,
                       RAWTOHEX(P.PARTY_UID) AS PARTY_UID,
                       P.PARTY_TYPE_CODE,
                       P.LIFECYCLE_STATUS_CODE,
                       P.VERIFICATION_STATUS_CODE,
                       P.DATA_QUALITY_STATUS_CODE,
                       PN.DISPLAY_NAME,
                       PI.IDENTIFIER_VALUE AS PRIMARY_IDENTIFIER,
                       P.VALID_FROM,
                       P.VALID_TO,
                       P.RECORD_VERSION
                """ + from + where + " ORDER BY P.PARTY_ID DESC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY";

        List<PartySummary> rows = jdbc.sql(sql)
                .params(pageParams)
                .query((rs, rowNum) -> new PartySummary(
                        rs.getLong("PARTY_ID"),
                        rs.getString("PARTY_UID"),
                        rs.getString("PARTY_TYPE_CODE"),
                        rs.getString("LIFECYCLE_STATUS_CODE"),
                        rs.getString("VERIFICATION_STATUS_CODE"),
                        rs.getString("DATA_QUALITY_STATUS_CODE"),
                        rs.getString("DISPLAY_NAME"),
                        rs.getString("PRIMARY_IDENTIFIER"),
                        localDate(rs, "VALID_FROM"),
                        localDate(rs, "VALID_TO"),
                        rs.getLong("RECORD_VERSION")
                ))
                .list();
        return new PageResponse<>(rows, total, page, size);
    }

    public Optional<PartyCore> findParty(long partyId) {
        String sql = """
                SELECT PARTY_ID, RAWTOHEX(PARTY_UID) AS PARTY_UID, PARTY_TYPE_CODE,
                       LIFECYCLE_STATUS_CODE, STATUS_REASON_CODE, STATUS_CHANGED_AT,
                       VERIFICATION_STATUS_CODE, DATA_QUALITY_STATUS_CODE, CREATION_SOURCE_CODE,
                       MERGED_INTO_PARTY_ID, MERGED_AT, MERGED_BY, VALID_FROM, VALID_TO,
                       IS_CURRENT, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, RECORD_VERSION
                FROM %s
                WHERE PARTY_ID = :partyId
                """.formatted(table("PARTY"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> mapParty(rs)).optional();
    }

    public Optional<PersonProfile> findPerson(long partyId) {
        String sql = """
                SELECT PARTY_ID, BIRTH_DATE, GENDER_CODE, BIRTH_COUNTRY_CODE, BIRTH_PLACE_ID,
                       BIRTH_PLACE_TEXT, FATHER_GIVEN_NAME, MOTHER_GIVEN_NAME, MARITAL_STATUS_CODE,
                       DEATH_DATE, LEGAL_CAPACITY_CODE, PRIMARY_LANGUAGE_CODE, DATA_QUALITY_STATUS_CODE,
                       VERIFICATION_STATUS_CODE, RESIDENCE_STATUS_CODE, PHYSICAL_ABILITY,
                       LIFE_STATUS_CODE, NATIONALITY_COUNTRY_CODE, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                """.formatted(table("PERSON"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new PersonProfile(
                        rs.getLong("PARTY_ID"),
                        localDate(rs, "BIRTH_DATE"),
                        rs.getString("GENDER_CODE"),
                        trimChar(rs.getString("BIRTH_COUNTRY_CODE")),
                        nullableLong(rs, "BIRTH_PLACE_ID"),
                        rs.getString("BIRTH_PLACE_TEXT"),
                        rs.getString("FATHER_GIVEN_NAME"),
                        rs.getString("MOTHER_GIVEN_NAME"),
                        rs.getString("MARITAL_STATUS_CODE"),
                        localDate(rs, "DEATH_DATE"),
                        rs.getString("LEGAL_CAPACITY_CODE"),
                        rs.getString("PRIMARY_LANGUAGE_CODE"),
                        rs.getString("DATA_QUALITY_STATUS_CODE"),
                        rs.getString("VERIFICATION_STATUS_CODE"),
                        rs.getString("RESIDENCE_STATUS_CODE"),
                        rs.getString("PHYSICAL_ABILITY"),
                        rs.getString("LIFE_STATUS_CODE"),
                        rs.getString("NATIONALITY_COUNTRY_CODE"),
                        rs.getLong("RECORD_VERSION")
                )).optional();
    }

    public Optional<OrganizationProfile> findOrganization(long partyId) {
        String sql = """
                SELECT ORGANIZATION_ID, PARTY_ID, REGISTERED_NAME, TRADE_NAME, LEGAL_FORM_CODE,
                       REGISTRATION_NO, REGISTRATION_PLACE_CODE, INCORPORATION_DATE, DISSOLUTION_DATE,
                       ECONOMIC_SECTOR_CODE, ISIC_CODE, LISTED_COMPANY_FLAG, REGISTRATION_COUNTRY_CODE,
                       ACTIVITY_STATUS_CODE, MAIN_ACTIVITY_DESCRIPTION, EMPLOYEE_COUNT, ENTERPRISE_SIZE_CODE,
                       OWNERSHIP_TYPE_CODE, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                """.formatted(table("ORGANIZATION"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new OrganizationProfile(
                        rs.getLong("ORGANIZATION_ID"),
                        rs.getLong("PARTY_ID"),
                        rs.getString("REGISTERED_NAME"),
                        rs.getString("TRADE_NAME"),
                        rs.getString("LEGAL_FORM_CODE"),
                        rs.getString("REGISTRATION_NO"),
                        rs.getString("REGISTRATION_PLACE_CODE"),
                        localDate(rs, "INCORPORATION_DATE"),
                        localDate(rs, "DISSOLUTION_DATE"),
                        rs.getString("ECONOMIC_SECTOR_CODE"),
                        rs.getString("ISIC_CODE"),
                        rs.getString("LISTED_COMPANY_FLAG"),
                        rs.getString("REGISTRATION_COUNTRY_CODE"),
                        rs.getString("ACTIVITY_STATUS_CODE"),
                        rs.getString("MAIN_ACTIVITY_DESCRIPTION"),
                        nullableLong(rs, "EMPLOYEE_COUNT"),
                        rs.getString("ENTERPRISE_SIZE_CODE"),
                        rs.getString("OWNERSHIP_TYPE_CODE"),
                        rs.getLong("RECORD_VERSION")
                )).optional();
    }

    public List<PartyNameRecord> findNames(long partyId) {
        String sql = """
                SELECT PARTY_NAME_ID, PARTY_ID, NAME_TYPE_CODE, LANGUAGE_CODE, SCRIPT_CODE,
                       PREFIX_TEXT, GIVEN_NAME, MIDDLE_NAME, FAMILY_NAME, SUFFIX_TEXT,
                       FULL_NAME, DISPLAY_NAME, SORT_NAME, NORMALIZED_NAME, PHONETIC_KEY,
                       IS_PRIMARY, VALID_FROM, VALID_TO, VERIFICATION_STATUS_CODE,
                       SOURCE_CODE, SOURCE_REFERENCE, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                ORDER BY CASE WHEN IS_PRIMARY = 'Y' THEN 0 ELSE 1 END, VALID_FROM DESC, PARTY_NAME_ID DESC
                """.formatted(table("PARTY_NAME"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new PartyNameRecord(
                        rs.getLong("PARTY_NAME_ID"), rs.getLong("PARTY_ID"), rs.getString("NAME_TYPE_CODE"),
                        rs.getString("LANGUAGE_CODE"), rs.getString("SCRIPT_CODE"), rs.getString("PREFIX_TEXT"),
                        rs.getString("GIVEN_NAME"), rs.getString("MIDDLE_NAME"), rs.getString("FAMILY_NAME"),
                        rs.getString("SUFFIX_TEXT"), rs.getString("FULL_NAME"), rs.getString("DISPLAY_NAME"),
                        rs.getString("SORT_NAME"), rs.getString("NORMALIZED_NAME"), rs.getString("PHONETIC_KEY"),
                        rs.getString("IS_PRIMARY"), localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"),
                        rs.getString("VERIFICATION_STATUS_CODE"), rs.getString("SOURCE_CODE"),
                        rs.getString("SOURCE_REFERENCE"), rs.getLong("RECORD_VERSION")
                )).list();
    }

    public List<PartyIdentifierRecord> findIdentifiers(long partyId) {
        String sql = """
                SELECT PARTY_IDENTIFIER_ID, PARTY_ID, IDENTIFIER_TYPE_CODE, IDENTIFIER_VALUE,
                       NORMALIZED_IDENTIFIER_VALUE, ISSUING_COUNTRY_CODE, ISSUING_AUTHORITY_CODE,
                       ISSUER_CODE, ISSUE_DATE, EXPIRY_DATE, IS_PRIMARY, IS_ACTIVE,
                       VERIFICATION_STATUS_CODE, VERIFICATION_SOURCE_CODE, VERIFICATION_METHOD_CODE,
                       VERIFIED_AT, VALID_FROM, VALID_TO, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                ORDER BY CASE WHEN IS_PRIMARY = 'Y' THEN 0 ELSE 1 END, VALID_FROM DESC, PARTY_IDENTIFIER_ID DESC
                """.formatted(table("PARTY_IDENTIFIER"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new PartyIdentifierRecord(
                        rs.getLong("PARTY_IDENTIFIER_ID"), rs.getLong("PARTY_ID"),
                        rs.getString("IDENTIFIER_TYPE_CODE"), rs.getString("IDENTIFIER_VALUE"),
                        rs.getString("NORMALIZED_IDENTIFIER_VALUE"), trimChar(rs.getString("ISSUING_COUNTRY_CODE")),
                        rs.getString("ISSUING_AUTHORITY_CODE"), rs.getString("ISSUER_CODE"),
                        localDate(rs, "ISSUE_DATE"), localDate(rs, "EXPIRY_DATE"),
                        rs.getString("IS_PRIMARY"), rs.getString("IS_ACTIVE"),
                        rs.getString("VERIFICATION_STATUS_CODE"), rs.getString("VERIFICATION_SOURCE_CODE"),
                        rs.getString("VERIFICATION_METHOD_CODE"), localDateTime(rs, "VERIFIED_AT"),
                        localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"), rs.getLong("RECORD_VERSION")
                )).list();
    }

    public List<PartyAddressRecord> findAddresses(long partyId) {
        String sql = """
                SELECT PA.PARTY_ADDRESS_ID, PA.ADDRESS_ID, PA.PARTY_ID, PA.ADDRESS_TYPE_CODE,
                       PA.IS_PRIMARY, PA.VALID_FROM, PA.VALID_TO, PA.TENURE_TYPE_CODE,
                       PA.VERIFICATION_STATUS_CODE, PA.SOURCE_CODE, PA.RECORD_VERSION AS PA_RECORD_VERSION,
                       A.COUNTRY_CODE, A.PROVINCE_CODE, A.COUNTY_CODE, A.CITY_CODE, A.DISTRICT_CODE, A.POSTAL_CODE,
                       A.ADDRESS_LINE1, A.ADDRESS_LINE2, A.NEIGHBORHOOD_TEXT, A.MAIN_STREET_TEXT, A.SIDE_STREET_TEXT,
                       A.PLAQUE_NO, A.FLOOR_NO, A.UNIT_NO, A.ADDRESS_DETAIL, A.RECORD_VERSION AS ADDRESS_RECORD_VERSION
                FROM %s PA
                JOIN %s A ON A.ADDRESS_ID = PA.ADDRESS_ID
                WHERE PA.PARTY_ID = :partyId
                ORDER BY CASE WHEN PA.IS_PRIMARY = 'Y' THEN 0 ELSE 1 END, PA.VALID_FROM DESC, PA.PARTY_ADDRESS_ID DESC
                """.formatted(table("PARTY_ADDRESS"), table("ADDRESS"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new PartyAddressRecord(
                        rs.getLong("PARTY_ADDRESS_ID"), rs.getLong("ADDRESS_ID"), rs.getLong("PARTY_ID"),
                        rs.getString("ADDRESS_TYPE_CODE"), rs.getString("IS_PRIMARY"),
                        localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"), trimChar(rs.getString("COUNTRY_CODE")),
                        rs.getString("PROVINCE_CODE"), rs.getString("COUNTY_CODE"), rs.getString("CITY_CODE"),
                        rs.getString("DISTRICT_CODE"), rs.getString("POSTAL_CODE"), rs.getString("ADDRESS_LINE1"),
                        rs.getString("ADDRESS_LINE2"), rs.getString("NEIGHBORHOOD_TEXT"), rs.getString("MAIN_STREET_TEXT"),
                        rs.getString("SIDE_STREET_TEXT"), rs.getString("PLAQUE_NO"), rs.getString("FLOOR_NO"),
                        rs.getString("UNIT_NO"), rs.getString("ADDRESS_DETAIL"), rs.getString("TENURE_TYPE_CODE"),
                        rs.getString("VERIFICATION_STATUS_CODE"), rs.getString("SOURCE_CODE"),
                        rs.getLong("PA_RECORD_VERSION"), rs.getLong("ADDRESS_RECORD_VERSION")
                )).list();
    }

    public List<ContactPointRecord> findContacts(long partyId) {
        String sql = """
                SELECT CONTACT_POINT_ID, PARTY_ID, CONTACT_TYPE_CODE, CONTACT_VALUE, NORMALIZED_VALUE,
                       PURPOSE_CODE, IS_PRIMARY, IS_VERIFIED, VERIFIED_AT, VALID_FROM, VALID_TO,
                       COUNTRY_DIAL_CODE, AREA_CODE, EXTENSION_NO, OWNER_TYPE_CODE,
                       VERIFICATION_STATUS_CODE, VERIFICATION_METHOD_CODE, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                ORDER BY CASE WHEN IS_PRIMARY = 'Y' THEN 0 ELSE 1 END, VALID_FROM DESC, CONTACT_POINT_ID DESC
                """.formatted(table("CONTACT_POINT"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new ContactPointRecord(
                        rs.getLong("CONTACT_POINT_ID"), rs.getLong("PARTY_ID"), rs.getString("CONTACT_TYPE_CODE"),
                        rs.getString("CONTACT_VALUE"), rs.getString("NORMALIZED_VALUE"), rs.getString("PURPOSE_CODE"),
                        rs.getString("IS_PRIMARY"), rs.getString("IS_VERIFIED"), localDateTime(rs, "VERIFIED_AT"),
                        localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"), rs.getString("COUNTRY_DIAL_CODE"),
                        rs.getString("AREA_CODE"), rs.getString("EXTENSION_NO"), rs.getString("OWNER_TYPE_CODE"),
                        rs.getString("VERIFICATION_STATUS_CODE"), rs.getString("VERIFICATION_METHOD_CODE"),
                        rs.getLong("RECORD_VERSION")
                )).list();
    }

    public List<ContactPointAddressRecord> findContactAddressAssociations(long partyId) {
        String sql = """
                SELECT CPA.CONTACT_POINT_ADDRESS_ID, CPA.CONTACT_POINT_ID, CPA.PARTY_ADDRESS_ID,
                       CPA.ASSOCIATION_TYPE_CODE, CPA.IS_PRIMARY_FOR_ADDRESS, CPA.VALID_FROM, CPA.VALID_TO, CPA.RECORD_VERSION
                FROM %s CPA
                JOIN %s CP ON CP.CONTACT_POINT_ID = CPA.CONTACT_POINT_ID
                JOIN %s PA ON PA.PARTY_ADDRESS_ID = CPA.PARTY_ADDRESS_ID
                WHERE CP.PARTY_ID = :partyId AND PA.PARTY_ID = :partyId
                ORDER BY CPA.VALID_FROM DESC, CPA.CONTACT_POINT_ADDRESS_ID DESC
                """.formatted(table("CONTACT_POINT_ADDRESS"), table("CONTACT_POINT"), table("PARTY_ADDRESS"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new ContactPointAddressRecord(
                        rs.getLong("CONTACT_POINT_ADDRESS_ID"), rs.getLong("CONTACT_POINT_ID"),
                        rs.getLong("PARTY_ADDRESS_ID"), rs.getString("ASSOCIATION_TYPE_CODE"),
                        rs.getString("IS_PRIMARY_FOR_ADDRESS"), localDate(rs, "VALID_FROM"),
                        localDate(rs, "VALID_TO"), rs.getLong("RECORD_VERSION")
                )).list();
    }

    public List<FinancialProfileRecord> findFinancialProfiles(long partyId) {
        String sql = """
                SELECT FINANCIAL_PROFILE_ID, PARTY_ID, AS_OF_DATE, ANNUAL_INCOME, TOTAL_ASSETS, TOTAL_LIABILITIES,
                       CURRENCY_CODE, SOURCE_OF_FUNDS_CODE, SOURCE_OF_WEALTH_CODE, EXPECTED_MONTHLY_TURNOVER,
                       TAX_STATUS_CODE, VERIFICATION_STATUS_CODE, NET_MONTHLY_INCOME, OTHER_MONTHLY_INCOME,
                       EXPECTED_MONTHLY_TXN_COUNT, FUNDS_COUNTRIES_TEXT, FINANCIAL_RELATION_PURPOSE_CODE,
                       REAL_ESTATE_VALUE, INVESTMENT_VALUE, TOTAL_MONTHLY_INSTALLMENT, ESTIMATED_NET_WORTH,
                       FINANCIAL_CAPACITY_CODE, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY AS_OF_DATE DESC, FINANCIAL_PROFILE_ID DESC
                """.formatted(table("FINANCIAL_PROFILE"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new FinancialProfileRecord(
                rs.getLong("FINANCIAL_PROFILE_ID"), rs.getLong("PARTY_ID"), localDate(rs, "AS_OF_DATE"),
                rs.getBigDecimal("ANNUAL_INCOME"), rs.getBigDecimal("TOTAL_ASSETS"), rs.getBigDecimal("TOTAL_LIABILITIES"),
                trimChar(rs.getString("CURRENCY_CODE")), rs.getString("SOURCE_OF_FUNDS_CODE"),
                rs.getString("SOURCE_OF_WEALTH_CODE"), rs.getBigDecimal("EXPECTED_MONTHLY_TURNOVER"),
                rs.getString("TAX_STATUS_CODE"), rs.getString("VERIFICATION_STATUS_CODE"),
                rs.getBigDecimal("NET_MONTHLY_INCOME"), rs.getBigDecimal("OTHER_MONTHLY_INCOME"),
                nullableLong(rs, "EXPECTED_MONTHLY_TXN_COUNT"), rs.getString("FUNDS_COUNTRIES_TEXT"),
                rs.getString("FINANCIAL_RELATION_PURPOSE_CODE"), rs.getBigDecimal("REAL_ESTATE_VALUE"),
                rs.getBigDecimal("INVESTMENT_VALUE"), rs.getBigDecimal("TOTAL_MONTHLY_INSTALLMENT"),
                rs.getBigDecimal("ESTIMATED_NET_WORTH"), rs.getString("FINANCIAL_CAPACITY_CODE"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<PartyEmploymentRecord> findEmployments(long partyId) {
        String sql = """
                SELECT EMPLOYMENT_ID, PARTY_ID, EMPLOYER_PARTY_ID, EMPLOYER_NAME, OCCUPATION_CODE, JOB_TITLE,
                       ECONOMIC_SECTOR_CODE, ISIC_CODE, MONTHLY_INCOME, INCOME_CURRENCY_CODE, FAMILY_RANGE,
                       JOB_STATUS, EMPLOYEE_RANGE, VALID_FROM, VALID_TO, EMPLOYMENT_STATUS_CODE, OCCUPATION_GROUP_CODE,
                       EMPLOYER_IDENTIFIER, CONTRACT_TYPE_CODE, INSURANCE_NO, TAX_CODE, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY VALID_FROM DESC, EMPLOYMENT_ID DESC
                """.formatted(table("PARTY_EMPLOYMENT"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyEmploymentRecord(
                rs.getLong("EMPLOYMENT_ID"), rs.getLong("PARTY_ID"), nullableLong(rs, "EMPLOYER_PARTY_ID"),
                rs.getString("EMPLOYER_NAME"), rs.getString("OCCUPATION_CODE"), rs.getString("JOB_TITLE"),
                rs.getString("ECONOMIC_SECTOR_CODE"), rs.getString("ISIC_CODE"), rs.getBigDecimal("MONTHLY_INCOME"),
                trimChar(rs.getString("INCOME_CURRENCY_CODE")), rs.getString("FAMILY_RANGE"), rs.getString("JOB_STATUS"),
                rs.getString("EMPLOYEE_RANGE"), localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"),
                rs.getString("EMPLOYMENT_STATUS_CODE"), rs.getString("OCCUPATION_GROUP_CODE"),
                rs.getString("EMPLOYER_IDENTIFIER"), rs.getString("CONTRACT_TYPE_CODE"), rs.getString("INSURANCE_NO"),
                rs.getString("TAX_CODE"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<PartyIncomeSourceRecord> findIncomeSources(long partyId) {
        String sql = """
                SELECT INCOME_SOURCE_ID, PARTY_ID, SOURCE_TYPE_CODE, MONTHLY_AMOUNT, CURRENCY_CODE,
                       DOCUMENTED_FLAG, STATUS_CODE, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY INCOME_SOURCE_ID DESC
                """.formatted(table("PARTY_INCOME_SOURCE"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyIncomeSourceRecord(
                rs.getLong("INCOME_SOURCE_ID"), rs.getLong("PARTY_ID"), rs.getString("SOURCE_TYPE_CODE"),
                rs.getBigDecimal("MONTHLY_AMOUNT"), rs.getString("CURRENCY_CODE"), rs.getString("DOCUMENTED_FLAG"),
                rs.getString("STATUS_CODE"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<PartyAssetLiabilityRecord> findAssetLiabilities(long partyId) {
        String sql = """
                SELECT ASSET_LIABILITY_ID, PARTY_ID, ITEM_TYPE_CODE, DESCRIPTION_TEXT, AMOUNT, CURRENCY_CODE,
                       ASSESSMENT_DATE, STATUS_CODE, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY ASSESSMENT_DATE DESC, ASSET_LIABILITY_ID DESC
                """.formatted(table("PARTY_ASSET_LIABILITY"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyAssetLiabilityRecord(
                rs.getLong("ASSET_LIABILITY_ID"), rs.getLong("PARTY_ID"), rs.getString("ITEM_TYPE_CODE"),
                rs.getString("DESCRIPTION_TEXT"), rs.getBigDecimal("AMOUNT"), rs.getString("CURRENCY_CODE"),
                localDate(rs, "ASSESSMENT_DATE"), rs.getString("STATUS_CODE"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<PartyLicenseRecord> findLicenses(long partyId) {
        String sql = """
                SELECT LICENSE_ID, PARTY_ID, LICENSE_TYPE_CODE, LICENSE_NUMBER, ISSUER_PARTY_ID, ISSUER_NAME,
                       ISSUE_DATE, EXPIRY_DATE, STATUS_CODE, DOCUMENT_REF, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY ISSUE_DATE DESC NULLS LAST, LICENSE_ID DESC
                """.formatted(table("PARTY_LICENSE"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyLicenseRecord(
                rs.getLong("LICENSE_ID"), rs.getLong("PARTY_ID"), rs.getString("LICENSE_TYPE_CODE"),
                rs.getString("LICENSE_NUMBER"), nullableLong(rs, "ISSUER_PARTY_ID"), rs.getString("ISSUER_NAME"),
                localDate(rs, "ISSUE_DATE"), localDate(rs, "EXPIRY_DATE"), rs.getString("STATUS_CODE"),
                rs.getString("DOCUMENT_REF"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<KycCaseRecord> findKycCases(long partyId) {
        String sql = """
                SELECT KYC_CASE_ID, PARTY_ID, KYC_TYPE_CODE, DUE_DILIGENCE_LEVEL_CODE, STATUS_CODE,
                       OPENED_AT, COMPLETED_AT, REVIEWED_AT, NEXT_REVIEW_DATE, FINAL_RISK_LEVEL_CODE,
                       DECISION_CODE, DECISION_REASON, APPROVED_BY, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                ORDER BY OPENED_AT DESC, KYC_CASE_ID DESC
                """.formatted(table("KYC_CASE"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new KycCaseRecord(
                        rs.getLong("KYC_CASE_ID"), rs.getLong("PARTY_ID"), rs.getString("KYC_TYPE_CODE"),
                        rs.getString("DUE_DILIGENCE_LEVEL_CODE"), rs.getString("STATUS_CODE"),
                        localDateTime(rs, "OPENED_AT"), localDateTime(rs, "COMPLETED_AT"),
                        localDateTime(rs, "REVIEWED_AT"), localDate(rs, "NEXT_REVIEW_DATE"),
                        rs.getString("FINAL_RISK_LEVEL_CODE"), rs.getString("DECISION_CODE"),
                        rs.getString("DECISION_REASON"), rs.getString("APPROVED_BY"), rs.getLong("RECORD_VERSION")
                )).list();
    }

    public List<PartyDocumentRecord> findDocuments(long partyId) {
        String sql = """
                SELECT DOCUMENT_ID, PARTY_ID, KYC_CASE_ID, DOCUMENT_TYPE_CODE, DOCUMENT_NUMBER,
                       ISSUER_CODE, ISSUE_DATE, EXPIRY_DATE, VERIFICATION_STATUS_CODE, VERIFIED_AT,
                       CONTENT_HASH, STORAGE_REF, MIME_TYPE, ISSUING_AUTHORITY_TEXT, CONTROL_STATUS_CODE,
                       DESCRIPTION_TEXT, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                ORDER BY CREATED_AT DESC, DOCUMENT_ID DESC
                """.formatted(table("PARTY_DOCUMENT"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new PartyDocumentRecord(
                        rs.getLong("DOCUMENT_ID"), rs.getLong("PARTY_ID"), nullableLong(rs, "KYC_CASE_ID"),
                        rs.getString("DOCUMENT_TYPE_CODE"), rs.getString("DOCUMENT_NUMBER"), rs.getString("ISSUER_CODE"),
                        localDate(rs, "ISSUE_DATE"), localDate(rs, "EXPIRY_DATE"),
                        rs.getString("VERIFICATION_STATUS_CODE"), localDateTime(rs, "VERIFIED_AT"),
                        rs.getString("CONTENT_HASH"), rs.getString("STORAGE_REF"), rs.getString("MIME_TYPE"),
                        rs.getString("ISSUING_AUTHORITY_TEXT"), rs.getString("CONTROL_STATUS_CODE"),
                        rs.getString("DESCRIPTION_TEXT"), rs.getLong("RECORD_VERSION")
                )).list();
    }

    public List<RiskAssessmentRecord> findRiskAssessments(long partyId) {
        String sql = """
                SELECT RISK_ASSESSMENT_ID, PARTY_ID, KYC_CASE_ID, RISK_TYPE_CODE, SCORE_VALUE,
                       RATING_CODE, DECISION_CODE, MODEL_CODE, MODEL_VERSION, ASSESSMENT_DATE,
                       VALID_TO, EXPLANATION, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                ORDER BY ASSESSMENT_DATE DESC, RISK_ASSESSMENT_ID DESC
                """.formatted(table("PARTY_RISK_ASSESSMENT"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new RiskAssessmentRecord(
                        rs.getLong("RISK_ASSESSMENT_ID"), rs.getLong("PARTY_ID"), nullableLong(rs, "KYC_CASE_ID"),
                        rs.getString("RISK_TYPE_CODE"), rs.getBigDecimal("SCORE_VALUE"), rs.getString("RATING_CODE"),
                        rs.getString("DECISION_CODE"), rs.getString("MODEL_CODE"), rs.getString("MODEL_VERSION"),
                        localDateTime(rs, "ASSESSMENT_DATE"), localDateTime(rs, "VALID_TO"),
                        rs.getString("EXPLANATION"), rs.getLong("RECORD_VERSION")
                )).list();
    }

    public List<ScreeningResultRecord> findScreenings(long partyId) {
        String sql = """
                SELECT SCREENING_RESULT_ID, PARTY_ID, KYC_CASE_ID, SCREENING_TYPE_CODE, SOURCE_LIST_CODE,
                       PROVIDER_CODE, PROVIDER_REFERENCE_NO, MATCHED_NAME, MATCH_SCORE, INITIAL_DECISION_CODE,
                       FINAL_DECISION_CODE, FALSE_POSITIVE_FLAG, SCREENED_AT, REVIEWED_AT, REVIEWED_BY,
                       PAYLOAD_REF, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                ORDER BY SCREENED_AT DESC, SCREENING_RESULT_ID DESC
                """.formatted(table("SCREENING_RESULT"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new ScreeningResultRecord(
                        rs.getLong("SCREENING_RESULT_ID"), rs.getLong("PARTY_ID"), nullableLong(rs, "KYC_CASE_ID"),
                        rs.getString("SCREENING_TYPE_CODE"), rs.getString("SOURCE_LIST_CODE"), rs.getString("PROVIDER_CODE"),
                        rs.getString("PROVIDER_REFERENCE_NO"), rs.getString("MATCHED_NAME"), rs.getBigDecimal("MATCH_SCORE"),
                        rs.getString("INITIAL_DECISION_CODE"), rs.getString("FINAL_DECISION_CODE"),
                        rs.getString("FALSE_POSITIVE_FLAG"), localDateTime(rs, "SCREENED_AT"),
                        localDateTime(rs, "REVIEWED_AT"), rs.getString("REVIEWED_BY"), rs.getString("PAYLOAD_REF"),
                        rs.getLong("RECORD_VERSION")
                )).list();
    }

    public Party360Response loadParty360(long partyId) {
        PartyCore party = findParty(partyId).orElse(null);
        if (party == null) {
            return null;
        }
        return new Party360Response(
                party,
                findPerson(partyId).orElse(null),
                findOrganization(partyId).orElse(null),
                findNames(partyId),
                findIdentifiers(partyId),
                findAddresses(partyId),
                findContacts(partyId),
                findContactAddressAssociations(partyId),
                findFinancialProfiles(partyId),
                findEmployments(partyId),
                findIncomeSources(partyId),
                findAssetLiabilities(partyId),
                findLicenses(partyId),
                findKycCases(partyId),
                findDocuments(partyId),
                findRiskAssessments(partyId),
                findScreenings(partyId)
        );
    }

    public CifDashboardSummary dashboardSummary() {
        long parties = count("PARTY");
        long persons = count("PERSON");
        long organizations = count("ORGANIZATION");
        long openKyc = jdbc.sql("SELECT COUNT(*) FROM " + table("KYC_CASE") + " WHERE COMPLETED_AT IS NULL")
                .query(Long.class).single();
        return new CifDashboardSummary(parties, persons, organizations, openKyc);
    }

    public long insertParty(CreatePartyRequest request, String actor) {
        long id = nextVal("SEQ_PARTY");
        String sql = """
                INSERT INTO %s (
                    PARTY_ID, PARTY_TYPE_CODE, LIFECYCLE_STATUS_CODE, STATUS_REASON_CODE,
                    VERIFICATION_STATUS_CODE, DATA_QUALITY_STATUS_CODE, CREATION_SOURCE_CODE,
                    VALID_FROM, VALID_TO, IS_CURRENT, CREATED_BY
                ) VALUES (
                    :partyId, :partyType, :lifecycle, :statusReason, :verification, :dataQuality, :source,
                    :validFrom, :validTo, 'Y', :actor
                )
                """.formatted(table("PARTY"));
        jdbc.sql(sql)
                .param("partyId", id)
                .param("partyType", request.partyTypeCode())
                .param("lifecycle", request.lifecycleStatusCode())
                .param("statusReason", request.statusReasonCode())
                .param("verification", request.verificationStatusCode())
                .param("dataQuality", request.dataQualityStatusCode())
                .param("source", request.creationSourceCode())
                .param("validFrom", sqlDate(request.validFrom()))
                .param("validTo", sqlDate(request.validTo()))
                .param("actor", actor)
                .update();
        return id;
    }

    public int updateParty(long partyId, UpdatePartyRequest request, String actor) {
        String sql = """
                UPDATE %s SET
                    LIFECYCLE_STATUS_CODE = :lifecycle,
                    STATUS_REASON_CODE = :statusReason,
                    STATUS_CHANGED_AT = CASE WHEN LIFECYCLE_STATUS_CODE <> :lifecycle THEN SYSTIMESTAMP ELSE STATUS_CHANGED_AT END,
                    VERIFICATION_STATUS_CODE = :verification,
                    DATA_QUALITY_STATUS_CODE = :dataQuality,
                    CREATION_SOURCE_CODE = :source,
                    MERGED_INTO_PARTY_ID = :mergedInto,
                    MERGED_AT = CASE WHEN :lifecycle = 'MERGED' AND MERGED_AT IS NULL THEN SYSTIMESTAMP WHEN :lifecycle <> 'MERGED' THEN NULL ELSE MERGED_AT END,
                    MERGED_BY = CASE WHEN :lifecycle = 'MERGED' THEN :actor ELSE NULL END,
                    VALID_FROM = :validFrom,
                    VALID_TO = :validTo,
                    IS_CURRENT = :isCurrent,
                    UPDATED_AT = SYSTIMESTAMP,
                    UPDATED_BY = :actor,
                    RECORD_VERSION = RECORD_VERSION + 1
                WHERE PARTY_ID = :partyId AND RECORD_VERSION = :recordVersion
                """.formatted(table("PARTY"));
        return jdbc.sql(sql)
                .param("lifecycle", request.lifecycleStatusCode())
                .param("statusReason", request.statusReasonCode())
                .param("verification", request.verificationStatusCode())
                .param("dataQuality", request.dataQualityStatusCode())
                .param("source", request.creationSourceCode())
                .param("mergedInto", request.mergedIntoPartyId())
                .param("validFrom", sqlDate(request.validFrom()))
                .param("validTo", sqlDate(request.validTo()))
                .param("isCurrent", request.isCurrent())
                .param("actor", actor)
                .param("partyId", partyId)
                .param("recordVersion", request.recordVersion())
                .update();
    }

    public void insertPerson(long partyId, PersonRequest request, String actor) {
        String sql = """
                INSERT INTO %s (
                    PARTY_ID, BIRTH_DATE, GENDER_CODE, BIRTH_COUNTRY_CODE, BIRTH_PLACE_ID, BIRTH_PLACE_TEXT,
                    FATHER_GIVEN_NAME, MOTHER_GIVEN_NAME, MARITAL_STATUS_CODE, DEATH_DATE,
                    LEGAL_CAPACITY_CODE, PRIMARY_LANGUAGE_CODE, DATA_QUALITY_STATUS_CODE,
                    VERIFICATION_STATUS_CODE, RESIDENCE_STATUS_CODE, PHYSICAL_ABILITY,
                    CREATED_AT, CREATED_BY, RECORD_VERSION, LIFE_STATUS_CODE, NATIONALITY_COUNTRY_CODE
                ) VALUES (
                    :partyId, :birthDate, :gender, :birthCountry, :birthPlaceId, :birthPlaceText,
                    :father, :mother, :marital, :deathDate, :legalCapacity, :language, :dataQuality,
                    :verification, :residence, :physicalAbility, SYSTIMESTAMP, :actor, 1, :lifeStatus, :nationalityCountry
                )
                """.formatted(table("PERSON"));
        bindPerson(jdbc.sql(sql).param("partyId", partyId), request, actor).update();
    }

    public int updatePerson(long partyId, PersonRequest request, String actor) {
        String sql = """
                UPDATE %s SET
                    BIRTH_DATE=:birthDate, GENDER_CODE=:gender, BIRTH_COUNTRY_CODE=:birthCountry,
                    BIRTH_PLACE_ID=:birthPlaceId, BIRTH_PLACE_TEXT=:birthPlaceText,
                    FATHER_GIVEN_NAME=:father, MOTHER_GIVEN_NAME=:mother, MARITAL_STATUS_CODE=:marital,
                    DEATH_DATE=:deathDate, LEGAL_CAPACITY_CODE=:legalCapacity, PRIMARY_LANGUAGE_CODE=:language,
                    DATA_QUALITY_STATUS_CODE=:dataQuality, VERIFICATION_STATUS_CODE=:verification,
                    RESIDENCE_STATUS_CODE=:residence, PHYSICAL_ABILITY=:physicalAbility,
                    LIFE_STATUS_CODE=:lifeStatus, NATIONALITY_COUNTRY_CODE=:nationalityCountry,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                    RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PERSON"));
        return bindPerson(jdbc.sql(sql).param("partyId", partyId), request, actor)
                .param("recordVersion", request.recordVersion())
                .update();
    }

    private JdbcClient.StatementSpec bindPerson(JdbcClient.StatementSpec spec, PersonRequest request, String actor) {
        return spec
                .param("birthDate", sqlDate(request.birthDate()))
                .param("gender", request.genderCode())
                .param("birthCountry", request.birthCountryCode())
                .param("birthPlaceId", request.birthPlaceId())
                .param("birthPlaceText", request.birthPlaceText())
                .param("father", request.fatherGivenName())
                .param("mother", request.motherGivenName())
                .param("marital", request.maritalStatusCode())
                .param("deathDate", sqlDate(request.deathDate()))
                .param("legalCapacity", request.legalCapacityCode())
                .param("language", request.primaryLanguageCode())
                .param("dataQuality", request.dataQualityStatusCode())
                .param("verification", request.verificationStatusCode())
                .param("residence", request.residenceStatusCode())
                .param("physicalAbility", request.physicalAbility())
                .param("lifeStatus", request.lifeStatusCode())
                .param("nationalityCountry", request.nationalityCountryCode())
                .param("actor", actor);
    }

    public long insertOrganization(long partyId, OrganizationRequest request, String actor) {
        long id = nextVal("SEQ_ORGANIZATION");
        String sql = """
                INSERT INTO %s (
                    ORGANIZATION_ID, PARTY_ID, REGISTERED_NAME, TRADE_NAME, LEGAL_FORM_CODE,
                    REGISTRATION_NO, REGISTRATION_PLACE_CODE, INCORPORATION_DATE, DISSOLUTION_DATE,
                    ECONOMIC_SECTOR_CODE, ISIC_CODE, LISTED_COMPANY_FLAG, REGISTRATION_COUNTRY_CODE,
                    ACTIVITY_STATUS_CODE, MAIN_ACTIVITY_DESCRIPTION, EMPLOYEE_COUNT, ENTERPRISE_SIZE_CODE,
                    OWNERSHIP_TYPE_CODE, CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :organizationId, :partyId, :registeredName, :tradeName, :legalForm,
                    :registrationNo, :registrationPlace, :incorporationDate, :dissolutionDate,
                    :economicSector, :isic, :listed, :registrationCountry, :activityStatus,
                    :mainActivityDescription, :employeeCount, :enterpriseSize, :ownershipType,
                    SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("ORGANIZATION"));
        bindOrganization(jdbc.sql(sql).param("organizationId", id).param("partyId", partyId), request, actor).update();
        return id;
    }

    public int updateOrganization(long partyId, OrganizationRequest request, String actor) {
        String sql = """
                UPDATE %s SET
                    REGISTERED_NAME=:registeredName, TRADE_NAME=:tradeName, LEGAL_FORM_CODE=:legalForm,
                    REGISTRATION_NO=:registrationNo, REGISTRATION_PLACE_CODE=:registrationPlace,
                    INCORPORATION_DATE=:incorporationDate, DISSOLUTION_DATE=:dissolutionDate,
                    ECONOMIC_SECTOR_CODE=:economicSector, ISIC_CODE=:isic, LISTED_COMPANY_FLAG=:listed,
                    REGISTRATION_COUNTRY_CODE=:registrationCountry, ACTIVITY_STATUS_CODE=:activityStatus,
                    MAIN_ACTIVITY_DESCRIPTION=:mainActivityDescription, EMPLOYEE_COUNT=:employeeCount,
                    ENTERPRISE_SIZE_CODE=:enterpriseSize, OWNERSHIP_TYPE_CODE=:ownershipType,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("ORGANIZATION"));
        return bindOrganization(jdbc.sql(sql).param("partyId", partyId), request, actor)
                .param("recordVersion", request.recordVersion())
                .update();
    }

    private JdbcClient.StatementSpec bindOrganization(JdbcClient.StatementSpec spec, OrganizationRequest request, String actor) {
        return spec
                .param("registeredName", request.registeredName())
                .param("tradeName", request.tradeName())
                .param("legalForm", request.legalFormCode())
                .param("registrationNo", request.registrationNo())
                .param("registrationPlace", request.registrationPlaceCode())
                .param("incorporationDate", sqlDate(request.incorporationDate()))
                .param("dissolutionDate", sqlDate(request.dissolutionDate()))
                .param("economicSector", request.economicSectorCode())
                .param("isic", request.isicCode())
                .param("listed", request.listedCompanyFlag())
                .param("registrationCountry", request.registrationCountryCode())
                .param("activityStatus", request.activityStatusCode())
                .param("mainActivityDescription", request.mainActivityDescription())
                .param("employeeCount", request.employeeCount())
                .param("enterpriseSize", request.enterpriseSizeCode())
                .param("ownershipType", request.ownershipTypeCode())
                .param("actor", actor);
    }

    public long insertName(long partyId, PartyNameRequest request, String actor) {
        long id = nextVal("SEQ_PARTY_NAME");
        String sql = """
                INSERT INTO %s (
                    PARTY_NAME_ID, PARTY_ID, NAME_TYPE_CODE, LANGUAGE_CODE, SCRIPT_CODE, PREFIX_TEXT,
                    GIVEN_NAME, MIDDLE_NAME, FAMILY_NAME, SUFFIX_TEXT, FULL_NAME, DISPLAY_NAME,
                    SORT_NAME, NORMALIZED_NAME, PHONETIC_KEY, IS_PRIMARY, VALID_FROM, VALID_TO,
                    VERIFICATION_STATUS_CODE, SOURCE_CODE, SOURCE_REFERENCE, CREATED_BY
                ) VALUES (
                    :id, :partyId, :nameType, :language, :script, :prefix, :given, :middle, :family,
                    :suffix, :fullName, :displayName, :sortName, :normalizedName, :phonetic,
                    :isPrimary, :validFrom, :validTo, :verification, :source, :sourceRef, :actor
                )
                """.formatted(table("PARTY_NAME"));
        bindName(jdbc.sql(sql).param("id", id).param("partyId", partyId), request, actor).update();
        return id;
    }

    public int updateName(long partyId, long id, PartyNameRequest request, String actor) {
        String sql = """
                UPDATE %s SET
                    NAME_TYPE_CODE=:nameType, LANGUAGE_CODE=:language, SCRIPT_CODE=:script, PREFIX_TEXT=:prefix,
                    GIVEN_NAME=:given, MIDDLE_NAME=:middle, FAMILY_NAME=:family, SUFFIX_TEXT=:suffix,
                    FULL_NAME=:fullName, DISPLAY_NAME=:displayName, SORT_NAME=:sortName,
                    NORMALIZED_NAME=:normalizedName, PHONETIC_KEY=:phonetic, IS_PRIMARY=:isPrimary,
                    VALID_FROM=:validFrom, VALID_TO=:validTo, VERIFICATION_STATUS_CODE=:verification,
                    SOURCE_CODE=:source, SOURCE_REFERENCE=:sourceRef, UPDATED_AT=SYSTIMESTAMP,
                    UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_NAME_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_NAME"));
        return bindName(jdbc.sql(sql).param("id", id).param("partyId", partyId), request, actor)
                .param("recordVersion", request.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindName(JdbcClient.StatementSpec spec, PartyNameRequest r, String actor) {
        return spec.param("nameType", r.nameTypeCode()).param("language", r.languageCode()).param("script", r.scriptCode())
                .param("prefix", r.prefixText()).param("given", r.givenName()).param("middle", r.middleName())
                .param("family", r.familyName()).param("suffix", r.suffixText()).param("fullName", r.fullName())
                .param("displayName", r.displayName()).param("sortName", r.sortName()).param("normalizedName", r.normalizedName())
                .param("phonetic", r.phoneticKey()).param("isPrimary", r.isPrimary()).param("validFrom", sqlDate(r.validFrom()))
                .param("validTo", sqlDate(r.validTo())).param("verification", r.verificationStatusCode())
                .param("source", r.sourceCode()).param("sourceRef", r.sourceReference()).param("actor", actor);
    }

    public long insertIdentifier(long partyId, PartyIdentifierRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_IDENTIFIER");
        String sql = """
                INSERT INTO %s (
                    PARTY_IDENTIFIER_ID, PARTY_ID, IDENTIFIER_TYPE_CODE, IDENTIFIER_VALUE,
                    NORMALIZED_IDENTIFIER_VALUE, ISSUING_COUNTRY_CODE, ISSUING_AUTHORITY_CODE,
                    ISSUER_CODE, ISSUE_DATE, EXPIRY_DATE, IS_PRIMARY, IS_ACTIVE,
                    VERIFICATION_STATUS_CODE, VERIFICATION_SOURCE_CODE, VERIFICATION_METHOD_CODE,
                    VERIFIED_AT, VALID_FROM, VALID_TO, CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :type, :value, :normalized, :country, :authority, :issuer,
                    :issueDate, :expiryDate, :isPrimary, :isActive, :verification, :verificationSource,
                    :verificationMethod, :verifiedAt, :validFrom, :validTo, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("PARTY_IDENTIFIER"));
        bindIdentifier(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateIdentifier(long partyId, long id, PartyIdentifierRequest r, String actor) {
        String sql = """
                UPDATE %s SET
                    IDENTIFIER_TYPE_CODE=:type, IDENTIFIER_VALUE=:value, NORMALIZED_IDENTIFIER_VALUE=:normalized,
                    ISSUING_COUNTRY_CODE=:country, ISSUING_AUTHORITY_CODE=:authority, ISSUER_CODE=:issuer,
                    ISSUE_DATE=:issueDate, EXPIRY_DATE=:expiryDate, IS_PRIMARY=:isPrimary, IS_ACTIVE=:isActive,
                    VERIFICATION_STATUS_CODE=:verification, VERIFICATION_SOURCE_CODE=:verificationSource,
                    VERIFICATION_METHOD_CODE=:verificationMethod, VERIFIED_AT=:verifiedAt, VALID_FROM=:validFrom,
                    VALID_TO=:validTo, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_IDENTIFIER_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_IDENTIFIER"));
        return bindIdentifier(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindIdentifier(JdbcClient.StatementSpec spec, PartyIdentifierRequest r, String actor) {
        return spec.param("type", r.identifierTypeCode()).param("value", r.identifierValue())
                .param("normalized", r.normalizedIdentifierValue()).param("country", r.issuingCountryCode())
                .param("authority", r.issuingAuthorityCode()).param("issuer", r.issuerCode())
                .param("issueDate", sqlDate(r.issueDate())).param("expiryDate", sqlDate(r.expiryDate()))
                .param("isPrimary", r.isPrimary()).param("isActive", r.isActive())
                .param("verification", r.verificationStatusCode()).param("verificationSource", r.verificationSourceCode())
                .param("verificationMethod", r.verificationMethodCode()).param("verifiedAt", sqlTimestamp(r.verifiedAt()))
                .param("validFrom", sqlDate(r.validFrom())).param("validTo", sqlDate(r.validTo())).param("actor", actor);
    }

    public long insertAddress(long partyId, PartyAddressRequest r, String actor) {
        long addressId = nextVal("SEQ_ADDRESS");
        long partyAddressId = nextVal("SEQ_PARTY_ADDRESS");
        String addressSql = """
                INSERT INTO %s (
                    ADDRESS_ID, COUNTRY_CODE, PROVINCE_CODE, COUNTY_CODE, CITY_CODE, DISTRICT_CODE, POSTAL_CODE,
                    ADDRESS_LINE1, ADDRESS_LINE2, NEIGHBORHOOD_TEXT, MAIN_STREET_TEXT, SIDE_STREET_TEXT,
                    PLAQUE_NO, FLOOR_NO, UNIT_NO, ADDRESS_DETAIL, CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :addressId, :country, :province, :county, :city, :district, :postal,
                    :line1, :line2, :neighborhood, :mainStreet, :sideStreet, :plaque, :floorNo, :unitNo, :detail,
                    SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("ADDRESS"));
        bindAddress(jdbc.sql(addressSql).param("addressId", addressId), r, actor).update();

        String relationSql = """
                INSERT INTO %s (
                    PARTY_ADDRESS_ID, PARTY_ID, ADDRESS_ID, ADDRESS_TYPE_CODE, IS_PRIMARY,
                    VALID_FROM, VALID_TO, TENURE_TYPE_CODE, VERIFICATION_STATUS_CODE, SOURCE_CODE,
                    CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :partyAddressId, :partyId, :addressId, :addressType, :isPrimary,
                    :validFrom, :validTo, :tenure, :verification, :source, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("PARTY_ADDRESS"));
        bindPartyAddress(jdbc.sql(relationSql).param("partyAddressId", partyAddressId).param("partyId", partyId)
                .param("addressId", addressId), r, actor).update();
        return partyAddressId;
    }

    public int updateAddress(long partyId, long partyAddressId, PartyAddressRequest r, String actor) {
        Long addressId = addressIdForPartyAddress(partyId, partyAddressId).orElse(null);
        if (addressId == null) {
            return 0;
        }
        int addressUpdated = bindAddress(jdbc.sql("""
                UPDATE %s SET COUNTRY_CODE=:country, PROVINCE_CODE=:province, COUNTY_CODE=:county, CITY_CODE=:city,
                    DISTRICT_CODE=:district, POSTAL_CODE=:postal, ADDRESS_LINE1=:line1, ADDRESS_LINE2=:line2,
                    NEIGHBORHOOD_TEXT=:neighborhood, MAIN_STREET_TEXT=:mainStreet, SIDE_STREET_TEXT=:sideStreet,
                    PLAQUE_NO=:plaque, FLOOR_NO=:floorNo, UNIT_NO=:unitNo, ADDRESS_DETAIL=:detail,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE ADDRESS_ID=:addressId AND RECORD_VERSION=:recordVersion
                """.formatted(table("ADDRESS"))).param("addressId", addressId)
                .param("recordVersion", r.addressRecordVersion()), r, actor).update();
        if (addressUpdated == 0) {
            return 0;
        }
        return bindPartyAddress(jdbc.sql("""
                UPDATE %s SET ADDRESS_TYPE_CODE=:addressType, IS_PRIMARY=:isPrimary,
                    VALID_FROM=:validFrom, VALID_TO=:validTo, TENURE_TYPE_CODE=:tenure,
                    VERIFICATION_STATUS_CODE=:verification, SOURCE_CODE=:source,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_ADDRESS_ID=:partyAddressId AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_ADDRESS"))).param("partyAddressId", partyAddressId).param("partyId", partyId)
                .param("recordVersion", r.partyAddressRecordVersion()), r, actor).update();
    }

    private JdbcClient.StatementSpec bindAddress(JdbcClient.StatementSpec spec, PartyAddressRequest r, String actor) {
        return spec.param("country", r.countryCode()).param("province", r.provinceCode()).param("county", r.countyCode())
                .param("city", r.cityCode()).param("district", r.districtCode()).param("postal", r.postalCode())
                .param("line1", r.addressLine1()).param("line2", r.addressLine2()).param("neighborhood", r.neighborhoodText())
                .param("mainStreet", r.mainStreetText()).param("sideStreet", r.sideStreetText()).param("plaque", r.plaqueNo())
                .param("floorNo", r.floorNo()).param("unitNo", r.unitNo()).param("detail", r.addressDetail())
                .param("actor", actor);
    }

    private JdbcClient.StatementSpec bindPartyAddress(JdbcClient.StatementSpec spec, PartyAddressRequest r, String actor) {
        return spec.param("addressType", r.addressTypeCode()).param("isPrimary", r.isPrimary())
                .param("validFrom", sqlDate(r.validFrom())).param("validTo", sqlDate(r.validTo()))
                .param("tenure", r.tenureTypeCode()).param("verification", r.verificationStatusCode())
                .param("source", r.sourceCode()).param("actor", actor);
    }

    public long insertContact(long partyId, ContactPointRequest r, String actor) {
        long id = nextVal("SEQ_CONTACT_POINT");
        String sql = """
                INSERT INTO %s (
                    CONTACT_POINT_ID, PARTY_ID, CONTACT_TYPE_CODE, CONTACT_VALUE, NORMALIZED_VALUE,
                    PURPOSE_CODE, IS_PRIMARY, IS_VERIFIED, VERIFIED_AT, VALID_FROM, VALID_TO,
                    COUNTRY_DIAL_CODE, AREA_CODE, EXTENSION_NO, OWNER_TYPE_CODE, VERIFICATION_STATUS_CODE,
                    VERIFICATION_METHOD_CODE, CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :type, :value, :normalized, :purpose, :isPrimary, :isVerified,
                    :verifiedAt, :validFrom, :validTo, :countryDial, :areaCode, :extensionNo, :ownerType,
                    :verificationStatus, :verificationMethod, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("CONTACT_POINT"));
        bindContact(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateContact(long partyId, long id, ContactPointRequest r, String actor) {
        String sql = """
                UPDATE %s SET CONTACT_TYPE_CODE=:type, CONTACT_VALUE=:value, NORMALIZED_VALUE=:normalized,
                    PURPOSE_CODE=:purpose, IS_PRIMARY=:isPrimary, IS_VERIFIED=:isVerified, VERIFIED_AT=:verifiedAt,
                    VALID_FROM=:validFrom, VALID_TO=:validTo, COUNTRY_DIAL_CODE=:countryDial, AREA_CODE=:areaCode,
                    EXTENSION_NO=:extensionNo, OWNER_TYPE_CODE=:ownerType, VERIFICATION_STATUS_CODE=:verificationStatus,
                    VERIFICATION_METHOD_CODE=:verificationMethod, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                    RECORD_VERSION=RECORD_VERSION+1
                WHERE CONTACT_POINT_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("CONTACT_POINT"));
        return bindContact(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindContact(JdbcClient.StatementSpec spec, ContactPointRequest r, String actor) {
        return spec.param("type", r.contactTypeCode()).param("value", r.contactValue()).param("normalized", r.normalizedValue())
                .param("purpose", r.purposeCode()).param("isPrimary", r.isPrimary()).param("isVerified", r.isVerified())
                .param("verifiedAt", sqlTimestamp(r.verifiedAt())).param("validFrom", sqlDate(r.validFrom()))
                .param("validTo", sqlDate(r.validTo())).param("countryDial", r.countryDialCode()).param("areaCode", r.areaCode())
                .param("extensionNo", r.extensionNo()).param("ownerType", r.ownerTypeCode())
                .param("verificationStatus", r.verificationStatusCode()).param("verificationMethod", r.verificationMethodCode())
                .param("actor", actor);
    }

    public long insertContactAddressAssociation(long partyId, ContactPointAddressRequest r, String actor) {
        long id = nextVal("SEQ_CONTACT_POINT_ADDRESS");
        jdbc.sql("""
                INSERT INTO %s (CONTACT_POINT_ADDRESS_ID, CONTACT_POINT_ID, PARTY_ADDRESS_ID, ASSOCIATION_TYPE_CODE,
                    IS_PRIMARY_FOR_ADDRESS, VALID_FROM, VALID_TO, CREATED_AT, CREATED_BY, RECORD_VERSION)
                SELECT :id, CP.CONTACT_POINT_ID, PA.PARTY_ADDRESS_ID, :associationType, :isPrimary,
                    :validFrom, :validTo, SYSTIMESTAMP, :actor, 1
                FROM %s CP JOIN %s PA ON PA.PARTY_ID = CP.PARTY_ID
                WHERE CP.CONTACT_POINT_ID=:contactPointId AND PA.PARTY_ADDRESS_ID=:partyAddressId AND CP.PARTY_ID=:partyId
                """.formatted(table("CONTACT_POINT_ADDRESS"), table("CONTACT_POINT"), table("PARTY_ADDRESS")))
                .param("id", id).param("associationType", r.associationTypeCode()).param("isPrimary", r.isPrimaryForAddress())
                .param("validFrom", sqlDate(r.validFrom())).param("validTo", sqlDate(r.validTo())).param("actor", actor)
                .param("contactPointId", r.contactPointId()).param("partyAddressId", r.partyAddressId()).param("partyId", partyId).update();
        return id;
    }

    public int updateContactAddressAssociation(long partyId, long id, ContactPointAddressRequest r, String actor) {
        return jdbc.sql("""
                UPDATE %s CPA SET ASSOCIATION_TYPE_CODE=:associationType, IS_PRIMARY_FOR_ADDRESS=:isPrimary,
                    VALID_FROM=:validFrom, VALID_TO=:validTo, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                    RECORD_VERSION=RECORD_VERSION+1
                WHERE CPA.CONTACT_POINT_ADDRESS_ID=:id AND CPA.RECORD_VERSION=:recordVersion
                  AND EXISTS (SELECT 1 FROM %s CP WHERE CP.CONTACT_POINT_ID=CPA.CONTACT_POINT_ID AND CP.PARTY_ID=:partyId)
                  AND EXISTS (SELECT 1 FROM %s PA WHERE PA.PARTY_ADDRESS_ID=CPA.PARTY_ADDRESS_ID AND PA.PARTY_ID=:partyId)
                """.formatted(table("CONTACT_POINT_ADDRESS"), table("CONTACT_POINT"), table("PARTY_ADDRESS")))
                .param("associationType", r.associationTypeCode()).param("isPrimary", r.isPrimaryForAddress())
                .param("validFrom", sqlDate(r.validFrom())).param("validTo", sqlDate(r.validTo())).param("actor", actor)
                .param("id", id).param("recordVersion", r.recordVersion()).param("partyId", partyId).update();
    }

    public int deleteContactAddressAssociation(long partyId, long id) {
        return jdbc.sql("""
                DELETE FROM %s CPA WHERE CPA.CONTACT_POINT_ADDRESS_ID=:id
                  AND EXISTS (SELECT 1 FROM %s CP WHERE CP.CONTACT_POINT_ID=CPA.CONTACT_POINT_ID AND CP.PARTY_ID=:partyId)
                  AND EXISTS (SELECT 1 FROM %s PA WHERE PA.PARTY_ADDRESS_ID=CPA.PARTY_ADDRESS_ID AND PA.PARTY_ID=:partyId)
                """.formatted(table("CONTACT_POINT_ADDRESS"), table("CONTACT_POINT"), table("PARTY_ADDRESS")))
                .param("id", id).param("partyId", partyId).update();
    }

    public boolean contactBelongsToParty(long partyId, long contactPointId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table("CONTACT_POINT") + " WHERE CONTACT_POINT_ID=:id AND PARTY_ID=:partyId")
                .param("id", contactPointId).param("partyId", partyId).query(Long.class).single() > 0;
    }

    public boolean partyAddressBelongsToParty(long partyId, long partyAddressId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY_ADDRESS") + " WHERE PARTY_ADDRESS_ID=:id AND PARTY_ID=:partyId")
                .param("id", partyAddressId).param("partyId", partyId).query(Long.class).single() > 0;
    }

    public long insertFinancialProfile(long partyId, FinancialProfileRequest r, String actor) {
        long id = nextVal("SEQ_FINANCIAL_PROFILE");
        String sql = """
                INSERT INTO %s (
                    FINANCIAL_PROFILE_ID, PARTY_ID, AS_OF_DATE, ANNUAL_INCOME, TOTAL_ASSETS, TOTAL_LIABILITIES,
                    CURRENCY_CODE, SOURCE_OF_FUNDS_CODE, SOURCE_OF_WEALTH_CODE, EXPECTED_MONTHLY_TURNOVER,
                    TAX_STATUS_CODE, VERIFICATION_STATUS_CODE, CREATED_AT, CREATED_BY, RECORD_VERSION,
                    NET_MONTHLY_INCOME, OTHER_MONTHLY_INCOME, EXPECTED_MONTHLY_TXN_COUNT, FUNDS_COUNTRIES_TEXT,
                    FINANCIAL_RELATION_PURPOSE_CODE, REAL_ESTATE_VALUE, INVESTMENT_VALUE, TOTAL_MONTHLY_INSTALLMENT,
                    ESTIMATED_NET_WORTH, FINANCIAL_CAPACITY_CODE
                ) VALUES (
                    :id, :partyId, :asOfDate, :annualIncome, :totalAssets, :totalLiabilities, :currencyCode,
                    :sourceOfFundsCode, :sourceOfWealthCode, :turnover, :taxStatusCode, :verificationStatusCode,
                    SYSTIMESTAMP, :actor, 1, :netMonthlyIncome, :otherMonthlyIncome, :txnCount, :fundsCountries,
                    :relationPurpose, :realEstateValue, :investmentValue, :monthlyInstallment, :netWorth, :capacityCode
                )
                """.formatted(table("FINANCIAL_PROFILE"));
        bindFinancialProfile(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateFinancialProfile(long partyId, long id, FinancialProfileRequest r, String actor) {
        String sql = """
                UPDATE %s SET AS_OF_DATE=:asOfDate, ANNUAL_INCOME=:annualIncome, TOTAL_ASSETS=:totalAssets,
                    TOTAL_LIABILITIES=:totalLiabilities, CURRENCY_CODE=:currencyCode, SOURCE_OF_FUNDS_CODE=:sourceOfFundsCode,
                    SOURCE_OF_WEALTH_CODE=:sourceOfWealthCode, EXPECTED_MONTHLY_TURNOVER=:turnover, TAX_STATUS_CODE=:taxStatusCode,
                    VERIFICATION_STATUS_CODE=:verificationStatusCode, NET_MONTHLY_INCOME=:netMonthlyIncome,
                    OTHER_MONTHLY_INCOME=:otherMonthlyIncome, EXPECTED_MONTHLY_TXN_COUNT=:txnCount,
                    FUNDS_COUNTRIES_TEXT=:fundsCountries, FINANCIAL_RELATION_PURPOSE_CODE=:relationPurpose,
                    REAL_ESTATE_VALUE=:realEstateValue, INVESTMENT_VALUE=:investmentValue,
                    TOTAL_MONTHLY_INSTALLMENT=:monthlyInstallment, ESTIMATED_NET_WORTH=:netWorth,
                    FINANCIAL_CAPACITY_CODE=:capacityCode, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                    RECORD_VERSION=RECORD_VERSION+1
                WHERE FINANCIAL_PROFILE_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("FINANCIAL_PROFILE"));
        return bindFinancialProfile(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindFinancialProfile(JdbcClient.StatementSpec spec, FinancialProfileRequest r, String actor) {
        return spec.param("asOfDate", sqlDate(r.asOfDate())).param("annualIncome", r.annualIncome())
                .param("totalAssets", r.totalAssets()).param("totalLiabilities", r.totalLiabilities())
                .param("currencyCode", r.currencyCode()).param("sourceOfFundsCode", r.sourceOfFundsCode())
                .param("sourceOfWealthCode", r.sourceOfWealthCode()).param("turnover", r.expectedMonthlyTurnover())
                .param("taxStatusCode", r.taxStatusCode()).param("verificationStatusCode", r.verificationStatusCode())
                .param("netMonthlyIncome", r.netMonthlyIncome()).param("otherMonthlyIncome", r.otherMonthlyIncome())
                .param("txnCount", r.expectedMonthlyTxnCount()).param("fundsCountries", r.fundsCountriesText())
                .param("relationPurpose", r.financialRelationPurposeCode()).param("realEstateValue", r.realEstateValue())
                .param("investmentValue", r.investmentValue()).param("monthlyInstallment", r.totalMonthlyInstallment())
                .param("netWorth", r.estimatedNetWorth()).param("capacityCode", r.financialCapacityCode()).param("actor", actor);
    }

    public long insertEmployment(long partyId, PartyEmploymentRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_EMPLOYMENT");
        String sql = """
                INSERT INTO %s (
                    EMPLOYMENT_ID, PARTY_ID, EMPLOYER_PARTY_ID, EMPLOYER_NAME, OCCUPATION_CODE, JOB_TITLE,
                    ECONOMIC_SECTOR_CODE, ISIC_CODE, MONTHLY_INCOME, INCOME_CURRENCY_CODE, FAMILY_RANGE, JOB_STATUS,
                    EMPLOYEE_RANGE, VALID_FROM, VALID_TO, CREATED_AT, CREATED_BY, RECORD_VERSION,
                    EMPLOYMENT_STATUS_CODE, OCCUPATION_GROUP_CODE, EMPLOYER_IDENTIFIER, CONTRACT_TYPE_CODE, INSURANCE_NO, TAX_CODE
                ) VALUES (
                    :id, :partyId, :employerPartyId, :employerName, :occupationCode, :jobTitle, :economicSectorCode,
                    :isicCode, :monthlyIncome, :currencyCode, :familyRange, :jobStatus, :employeeRange, :validFrom, :validTo,
                    SYSTIMESTAMP, :actor, 1, :employmentStatusCode, :occupationGroupCode, :employerIdentifier,
                    :contractTypeCode, :insuranceNo, :taxCode
                )
                """.formatted(table("PARTY_EMPLOYMENT"));
        bindEmployment(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateEmployment(long partyId, long id, PartyEmploymentRequest r, String actor) {
        String sql = """
                UPDATE %s SET EMPLOYER_PARTY_ID=:employerPartyId, EMPLOYER_NAME=:employerName, OCCUPATION_CODE=:occupationCode,
                    JOB_TITLE=:jobTitle, ECONOMIC_SECTOR_CODE=:economicSectorCode, ISIC_CODE=:isicCode,
                    MONTHLY_INCOME=:monthlyIncome, INCOME_CURRENCY_CODE=:currencyCode, FAMILY_RANGE=:familyRange,
                    JOB_STATUS=:jobStatus, EMPLOYEE_RANGE=:employeeRange, VALID_FROM=:validFrom, VALID_TO=:validTo,
                    EMPLOYMENT_STATUS_CODE=:employmentStatusCode, OCCUPATION_GROUP_CODE=:occupationGroupCode,
                    EMPLOYER_IDENTIFIER=:employerIdentifier, CONTRACT_TYPE_CODE=:contractTypeCode, INSURANCE_NO=:insuranceNo,
                    TAX_CODE=:taxCode, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE EMPLOYMENT_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_EMPLOYMENT"));
        return bindEmployment(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindEmployment(JdbcClient.StatementSpec spec, PartyEmploymentRequest r, String actor) {
        return spec.param("employerPartyId", r.employerPartyId()).param("employerName", r.employerName())
                .param("occupationCode", r.occupationCode()).param("jobTitle", r.jobTitle())
                .param("economicSectorCode", r.economicSectorCode()).param("isicCode", r.isicCode())
                .param("monthlyIncome", r.monthlyIncome()).param("currencyCode", r.incomeCurrencyCode())
                .param("familyRange", r.familyRange()).param("jobStatus", r.jobStatus()).param("employeeRange", r.employeeRange())
                .param("validFrom", sqlDate(r.validFrom())).param("validTo", sqlDate(r.validTo()))
                .param("employmentStatusCode", r.employmentStatusCode()).param("occupationGroupCode", r.occupationGroupCode())
                .param("employerIdentifier", r.employerIdentifier()).param("contractTypeCode", r.contractTypeCode())
                .param("insuranceNo", r.insuranceNo()).param("taxCode", r.taxCode()).param("actor", actor);
    }

    public long insertIncomeSource(long partyId, PartyIncomeSourceRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_INCOME_SOURCE");
        String sql = """
                INSERT INTO %s (INCOME_SOURCE_ID, PARTY_ID, SOURCE_TYPE_CODE, MONTHLY_AMOUNT, CURRENCY_CODE,
                    DOCUMENTED_FLAG, STATUS_CODE, CREATED_AT, CREATED_BY, RECORD_VERSION)
                VALUES (:id, :partyId, :sourceTypeCode, :monthlyAmount, :currencyCode, :documentedFlag, :statusCode,
                    SYSTIMESTAMP, :actor, 1)
                """.formatted(table("PARTY_INCOME_SOURCE"));
        bindIncomeSource(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateIncomeSource(long partyId, long id, PartyIncomeSourceRequest r, String actor) {
        String sql = """
                UPDATE %s SET SOURCE_TYPE_CODE=:sourceTypeCode, MONTHLY_AMOUNT=:monthlyAmount, CURRENCY_CODE=:currencyCode,
                    DOCUMENTED_FLAG=:documentedFlag, STATUS_CODE=:statusCode, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                    RECORD_VERSION=RECORD_VERSION+1
                WHERE INCOME_SOURCE_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_INCOME_SOURCE"));
        return bindIncomeSource(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindIncomeSource(JdbcClient.StatementSpec spec, PartyIncomeSourceRequest r, String actor) {
        return spec.param("sourceTypeCode", r.sourceTypeCode()).param("monthlyAmount", r.monthlyAmount())
                .param("currencyCode", r.currencyCode()).param("documentedFlag", r.documentedFlag())
                .param("statusCode", r.statusCode()).param("actor", actor);
    }

    public long insertAssetLiability(long partyId, PartyAssetLiabilityRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_ASSET_LIABILITY");
        String sql = """
                INSERT INTO %s (ASSET_LIABILITY_ID, PARTY_ID, ITEM_TYPE_CODE, DESCRIPTION_TEXT, AMOUNT, CURRENCY_CODE,
                    ASSESSMENT_DATE, STATUS_CODE, CREATED_AT, CREATED_BY, RECORD_VERSION)
                VALUES (:id, :partyId, :itemTypeCode, :descriptionText, :amount, :currencyCode, :assessmentDate,
                    :statusCode, SYSTIMESTAMP, :actor, 1)
                """.formatted(table("PARTY_ASSET_LIABILITY"));
        bindAssetLiability(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateAssetLiability(long partyId, long id, PartyAssetLiabilityRequest r, String actor) {
        String sql = """
                UPDATE %s SET ITEM_TYPE_CODE=:itemTypeCode, DESCRIPTION_TEXT=:descriptionText, AMOUNT=:amount,
                    CURRENCY_CODE=:currencyCode, ASSESSMENT_DATE=:assessmentDate, STATUS_CODE=:statusCode,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE ASSET_LIABILITY_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_ASSET_LIABILITY"));
        return bindAssetLiability(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindAssetLiability(JdbcClient.StatementSpec spec, PartyAssetLiabilityRequest r, String actor) {
        return spec.param("itemTypeCode", r.itemTypeCode()).param("descriptionText", r.descriptionText())
                .param("amount", r.amount()).param("currencyCode", r.currencyCode()).param("assessmentDate", sqlDate(r.assessmentDate()))
                .param("statusCode", r.statusCode()).param("actor", actor);
    }

    public long insertLicense(long partyId, PartyLicenseRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_LICENSE");
        String sql = """
                INSERT INTO %s (LICENSE_ID, PARTY_ID, LICENSE_TYPE_CODE, LICENSE_NUMBER, ISSUER_PARTY_ID, ISSUER_NAME,
                    ISSUE_DATE, EXPIRY_DATE, STATUS_CODE, DOCUMENT_REF, CREATED_AT, CREATED_BY, RECORD_VERSION)
                VALUES (:id, :partyId, :licenseTypeCode, :licenseNumber, :issuerPartyId, :issuerName, :issueDate,
                    :expiryDate, :statusCode, :documentRef, SYSTIMESTAMP, :actor, 1)
                """.formatted(table("PARTY_LICENSE"));
        bindLicense(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateLicense(long partyId, long id, PartyLicenseRequest r, String actor) {
        String sql = """
                UPDATE %s SET LICENSE_TYPE_CODE=:licenseTypeCode, LICENSE_NUMBER=:licenseNumber, ISSUER_PARTY_ID=:issuerPartyId,
                    ISSUER_NAME=:issuerName, ISSUE_DATE=:issueDate, EXPIRY_DATE=:expiryDate, STATUS_CODE=:statusCode,
                    DOCUMENT_REF=:documentRef, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE LICENSE_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_LICENSE"));
        return bindLicense(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindLicense(JdbcClient.StatementSpec spec, PartyLicenseRequest r, String actor) {
        return spec.param("licenseTypeCode", r.licenseTypeCode()).param("licenseNumber", r.licenseNumber())
                .param("issuerPartyId", r.issuerPartyId()).param("issuerName", r.issuerName())
                .param("issueDate", sqlDate(r.issueDate())).param("expiryDate", sqlDate(r.expiryDate()))
                .param("statusCode", r.statusCode()).param("documentRef", r.documentRef()).param("actor", actor);
    }

    public long insertKycCase(long partyId, KycCaseRequest r, String actor) {
        long id = nextVal("SEQ_KYC_CASE");
        String sql = """
                INSERT INTO %s (
                    KYC_CASE_ID, PARTY_ID, KYC_TYPE_CODE, DUE_DILIGENCE_LEVEL_CODE, STATUS_CODE,
                    OPENED_AT, COMPLETED_AT, REVIEWED_AT, NEXT_REVIEW_DATE, FINAL_RISK_LEVEL_CODE,
                    DECISION_CODE, DECISION_REASON, APPROVED_BY, CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :kycType, :dd, :status, :openedAt, :completedAt, :reviewedAt,
                    :nextReview, :riskLevel, :decision, :reason, :approvedBy, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("KYC_CASE"));
        bindKyc(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateKycCase(long partyId, long id, KycCaseRequest r, String actor) {
        String sql = """
                UPDATE %s SET KYC_TYPE_CODE=:kycType, DUE_DILIGENCE_LEVEL_CODE=:dd, STATUS_CODE=:status,
                    OPENED_AT=:openedAt, COMPLETED_AT=:completedAt, REVIEWED_AT=:reviewedAt,
                    NEXT_REVIEW_DATE=:nextReview, FINAL_RISK_LEVEL_CODE=:riskLevel, DECISION_CODE=:decision,
                    DECISION_REASON=:reason, APPROVED_BY=:approvedBy, UPDATED_AT=SYSTIMESTAMP,
                    UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE KYC_CASE_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("KYC_CASE"));
        return bindKyc(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindKyc(JdbcClient.StatementSpec spec, KycCaseRequest r, String actor) {
        return spec.param("kycType", r.kycTypeCode()).param("dd", r.dueDiligenceLevelCode()).param("status", r.statusCode())
                .param("openedAt", sqlTimestamp(r.openedAt())).param("completedAt", sqlTimestamp(r.completedAt()))
                .param("reviewedAt", sqlTimestamp(r.reviewedAt())).param("nextReview", sqlDate(r.nextReviewDate()))
                .param("riskLevel", r.finalRiskLevelCode()).param("decision", r.decisionCode()).param("reason", r.decisionReason())
                .param("approvedBy", r.approvedBy()).param("actor", actor);
    }

    public long insertDocument(long partyId, PartyDocumentRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_DOCUMENT");
        String sql = """
                INSERT INTO %s (
                    DOCUMENT_ID, PARTY_ID, KYC_CASE_ID, DOCUMENT_TYPE_CODE, DOCUMENT_NUMBER, ISSUER_CODE,
                    ISSUE_DATE, EXPIRY_DATE, VERIFICATION_STATUS_CODE, VERIFIED_AT, CONTENT_HASH,
                    STORAGE_REF, MIME_TYPE, ISSUING_AUTHORITY_TEXT, CONTROL_STATUS_CODE, DESCRIPTION_TEXT,
                    CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :kycCaseId, :type, :number, :issuer, :issueDate, :expiryDate,
                    :verification, :verifiedAt, :hash, :storageRef, :mimeType, :issuingAuthorityText,
                    :controlStatus, :description, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("PARTY_DOCUMENT"));
        bindDocument(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateDocument(long partyId, long id, PartyDocumentRequest r, String actor) {
        String sql = """
                UPDATE %s SET KYC_CASE_ID=:kycCaseId, DOCUMENT_TYPE_CODE=:type, DOCUMENT_NUMBER=:number,
                    ISSUER_CODE=:issuer, ISSUE_DATE=:issueDate, EXPIRY_DATE=:expiryDate,
                    VERIFICATION_STATUS_CODE=:verification, VERIFIED_AT=:verifiedAt, CONTENT_HASH=:hash,
                    STORAGE_REF=:storageRef, MIME_TYPE=:mimeType, ISSUING_AUTHORITY_TEXT=:issuingAuthorityText,
                    CONTROL_STATUS_CODE=:controlStatus, DESCRIPTION_TEXT=:description, UPDATED_AT=SYSTIMESTAMP,
                    UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE DOCUMENT_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_DOCUMENT"));
        return bindDocument(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindDocument(JdbcClient.StatementSpec spec, PartyDocumentRequest r, String actor) {
        return spec.param("kycCaseId", r.kycCaseId()).param("type", r.documentTypeCode()).param("number", r.documentNumber())
                .param("issuer", r.issuerCode()).param("issueDate", sqlDate(r.issueDate())).param("expiryDate", sqlDate(r.expiryDate()))
                .param("verification", r.verificationStatusCode()).param("verifiedAt", sqlTimestamp(r.verifiedAt()))
                .param("hash", r.contentHash()).param("storageRef", r.storageRef()).param("mimeType", r.mimeType())
                .param("issuingAuthorityText", r.issuingAuthorityText()).param("controlStatus", r.controlStatusCode())
                .param("description", r.descriptionText()).param("actor", actor);
    }

    public long insertRisk(long partyId, RiskAssessmentRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_RISK_ASSESSMENT");
        String sql = """
                INSERT INTO %s (
                    RISK_ASSESSMENT_ID, PARTY_ID, KYC_CASE_ID, RISK_TYPE_CODE, SCORE_VALUE, RATING_CODE,
                    DECISION_CODE, MODEL_CODE, MODEL_VERSION, ASSESSMENT_DATE, VALID_TO, EXPLANATION,
                    CREATED_AT, CREATED_BY, RECORD_VERSION, CREATED_DATE, LAST_MODIFIED_BY, LAST_MODIFIED_DATE
                ) VALUES (
                    :id, :partyId, :kycCaseId, :riskType, :score, :rating, :decision, :model, :modelVersion,
                    :assessmentDate, :validTo, :explanation, SYSTIMESTAMP, :actor, 1, SYSTIMESTAMP, :actor, SYSTIMESTAMP
                )
                """.formatted(table("PARTY_RISK_ASSESSMENT"));
        bindRisk(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateRisk(long partyId, long id, RiskAssessmentRequest r, String actor) {
        String sql = """
                UPDATE %s SET KYC_CASE_ID=:kycCaseId, RISK_TYPE_CODE=:riskType, SCORE_VALUE=:score,
                    RATING_CODE=:rating, DECISION_CODE=:decision, MODEL_CODE=:model, MODEL_VERSION=:modelVersion,
                    ASSESSMENT_DATE=:assessmentDate, VALID_TO=:validTo, EXPLANATION=:explanation,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, LAST_MODIFIED_BY=:actor,
                    LAST_MODIFIED_DATE=SYSTIMESTAMP, RECORD_VERSION=RECORD_VERSION+1
                WHERE RISK_ASSESSMENT_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_RISK_ASSESSMENT"));
        return bindRisk(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindRisk(JdbcClient.StatementSpec spec, RiskAssessmentRequest r, String actor) {
        return spec.param("kycCaseId", r.kycCaseId()).param("riskType", r.riskTypeCode()).param("score", r.scoreValue())
                .param("rating", r.ratingCode()).param("decision", r.decisionCode()).param("model", r.modelCode())
                .param("modelVersion", r.modelVersion()).param("assessmentDate", sqlTimestamp(r.assessmentDate()))
                .param("validTo", sqlTimestamp(r.validTo())).param("explanation", r.explanation()).param("actor", actor);
    }

    public long insertScreening(long partyId, ScreeningResultRequest r) {
        long id = nextVal("SEQ_SCREENING_RESULT");
        String sql = """
                INSERT INTO %s (
                    SCREENING_RESULT_ID, PARTY_ID, KYC_CASE_ID, SCREENING_TYPE_CODE, SOURCE_LIST_CODE,
                    PROVIDER_CODE, PROVIDER_REFERENCE_NO, MATCHED_NAME, MATCH_SCORE, INITIAL_DECISION_CODE,
                    FINAL_DECISION_CODE, FALSE_POSITIVE_FLAG, SCREENED_AT, REVIEWED_AT, REVIEWED_BY,
                    PAYLOAD_REF, CREATED_AT, CREATED_DATE, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :kycCaseId, :type, :sourceList, :provider, :providerRef, :matchedName,
                    :matchScore, :initialDecision, :finalDecision, :falsePositive, :screenedAt,
                    :reviewedAt, :reviewedBy, :payloadRef, SYSTIMESTAMP, SYSTIMESTAMP, 1
                )
                """.formatted(table("SCREENING_RESULT"));
        bindScreening(jdbc.sql(sql).param("id", id).param("partyId", partyId), r).update();
        return id;
    }

    public int updateScreening(long partyId, long id, ScreeningResultRequest r, String actor) {
        String sql = """
                UPDATE %s SET KYC_CASE_ID=:kycCaseId, SCREENING_TYPE_CODE=:type, SOURCE_LIST_CODE=:sourceList,
                    PROVIDER_CODE=:provider, PROVIDER_REFERENCE_NO=:providerRef, MATCHED_NAME=:matchedName,
                    MATCH_SCORE=:matchScore, INITIAL_DECISION_CODE=:initialDecision, FINAL_DECISION_CODE=:finalDecision,
                    FALSE_POSITIVE_FLAG=:falsePositive, SCREENED_AT=:screenedAt, REVIEWED_AT=:reviewedAt,
                    REVIEWED_BY=:reviewedBy, PAYLOAD_REF=:payloadRef, UPDATED_AT=SYSTIMESTAMP,
                    UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE SCREENING_RESULT_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("SCREENING_RESULT"));
        return bindScreening(jdbc.sql(sql).param("id", id).param("partyId", partyId), r)
                .param("actor", actor).param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindScreening(JdbcClient.StatementSpec spec, ScreeningResultRequest r) {
        return spec.param("kycCaseId", r.kycCaseId()).param("type", r.screeningTypeCode()).param("sourceList", r.sourceListCode())
                .param("provider", r.providerCode()).param("providerRef", r.providerReferenceNo()).param("matchedName", r.matchedName())
                .param("matchScore", r.matchScore()).param("initialDecision", r.initialDecisionCode()).param("finalDecision", r.finalDecisionCode())
                .param("falsePositive", r.falsePositiveFlag()).param("screenedAt", sqlTimestamp(r.screenedAt()))
                .param("reviewedAt", sqlTimestamp(r.reviewedAt())).param("reviewedBy", r.reviewedBy()).param("payloadRef", r.payloadRef());
    }

    public void clearPrimaryNames(long partyId, Long exceptId, String actor) {
        String sql = "UPDATE " + table("PARTY_NAME")
                + " SET IS_PRIMARY='N', UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1"
                + " WHERE PARTY_ID=:partyId AND IS_PRIMARY='Y'"
                + (exceptId == null ? "" : " AND PARTY_NAME_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("actor", actor).param("partyId", partyId);
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        spec.update();
    }

    public void clearPrimaryIdentifiers(long partyId, Long exceptId, String actor) {
        String sql = "UPDATE " + table("PARTY_IDENTIFIER")
                + " SET IS_PRIMARY='N', UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1"
                + " WHERE PARTY_ID=:partyId AND IS_PRIMARY='Y'"
                + (exceptId == null ? "" : " AND PARTY_IDENTIFIER_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("actor", actor).param("partyId", partyId);
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        spec.update();
    }

    public void clearPrimaryAddresses(long partyId, String addressTypeCode, Long exceptId, String actor) {
        String sql = "UPDATE " + table("PARTY_ADDRESS")
                + " SET IS_PRIMARY='N', UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1"
                + " WHERE PARTY_ID=:partyId AND ADDRESS_TYPE_CODE=:addressType AND IS_PRIMARY='Y'"
                + (exceptId == null ? "" : " AND PARTY_ADDRESS_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("actor", actor).param("partyId", partyId)
                .param("addressType", addressTypeCode);
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        spec.update();
    }

    public void clearPrimaryContacts(long partyId, String contactTypeCode, Long exceptId, String actor) {
        String sql = "UPDATE " + table("CONTACT_POINT")
                + " SET IS_PRIMARY='N', UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1"
                + " WHERE PARTY_ID=:partyId AND CONTACT_TYPE_CODE=:contactType AND IS_PRIMARY='Y'"
                + (exceptId == null ? "" : " AND CONTACT_POINT_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("actor", actor).param("partyId", partyId)
                .param("contactType", contactTypeCode);
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        spec.update();
    }

    public int deleteName(long partyId, long id) {
        return deleteChild("PARTY_NAME", "PARTY_NAME_ID", partyId, id);
    }

    public int deleteIdentifier(long partyId, long id) {
        return deleteChild("PARTY_IDENTIFIER", "PARTY_IDENTIFIER_ID", partyId, id);
    }

    public int deleteContact(long partyId, long id) {
        if (!contactBelongsToParty(partyId, id)) {
            return 0;
        }
        jdbc.sql("DELETE FROM " + table("CONTACT_POINT_ADDRESS") + " WHERE CONTACT_POINT_ID=:id")
                .param("id", id).update();
        return deleteChild("CONTACT_POINT", "CONTACT_POINT_ID", partyId, id);
    }

    public int deleteFinancialProfile(long partyId, long id) {
        return deleteChild("FINANCIAL_PROFILE", "FINANCIAL_PROFILE_ID", partyId, id);
    }

    public int deleteEmployment(long partyId, long id) {
        return deleteChild("PARTY_EMPLOYMENT", "EMPLOYMENT_ID", partyId, id);
    }

    public int deleteIncomeSource(long partyId, long id) {
        return deleteChild("PARTY_INCOME_SOURCE", "INCOME_SOURCE_ID", partyId, id);
    }

    public int deleteAssetLiability(long partyId, long id) {
        return deleteChild("PARTY_ASSET_LIABILITY", "ASSET_LIABILITY_ID", partyId, id);
    }

    public int deleteLicense(long partyId, long id) {
        return deleteChild("PARTY_LICENSE", "LICENSE_ID", partyId, id);
    }

    public int deleteKycCase(long partyId, long id) {
        return deleteChild("KYC_CASE", "KYC_CASE_ID", partyId, id);
    }

    public int deleteDocument(long partyId, long id) {
        return deleteChild("PARTY_DOCUMENT", "DOCUMENT_ID", partyId, id);
    }

    public int deleteRisk(long partyId, long id) {
        return deleteChild("PARTY_RISK_ASSESSMENT", "RISK_ASSESSMENT_ID", partyId, id);
    }

    public int deleteScreening(long partyId, long id) {
        return deleteChild("SCREENING_RESULT", "SCREENING_RESULT_ID", partyId, id);
    }

    public int deleteAddress(long partyId, long partyAddressId) {
        Long addressId = addressIdForPartyAddress(partyId, partyAddressId).orElse(null);
        if (addressId == null) {
            return 0;
        }
        jdbc.sql("DELETE FROM " + table("CONTACT_POINT_ADDRESS") + " WHERE PARTY_ADDRESS_ID=:id")
                .param("id", partyAddressId).update();
        int deleted = jdbc.sql("DELETE FROM " + table("PARTY_ADDRESS") + " WHERE PARTY_ADDRESS_ID=:id AND PARTY_ID=:partyId")
                .param("id", partyAddressId).param("partyId", partyId).update();
        if (deleted > 0) {
            jdbc.sql("DELETE FROM " + table("ADDRESS") + " WHERE ADDRESS_ID=:addressId")
                    .param("addressId", addressId).update();
        }
        return deleted;
    }

    public boolean financialProfileExistsForDate(long partyId, java.time.LocalDate asOfDate, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("FINANCIAL_PROFILE")
                + " WHERE PARTY_ID=:partyId AND AS_OF_DATE=:asOfDate"
                + (exceptId == null ? "" : " AND FINANCIAL_PROFILE_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("partyId", partyId).param("asOfDate", sqlDate(asOfDate));
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public boolean licenseExists(String licenseTypeCode, String licenseNumber, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("PARTY_LICENSE")
                + " WHERE LICENSE_TYPE_CODE=:type AND LICENSE_NUMBER=:number"
                + (exceptId == null ? "" : " AND LICENSE_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("type", licenseTypeCode).param("number", licenseNumber);
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public boolean hasPrimaryIdentifier(long partyId, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("PARTY_IDENTIFIER")
                + " WHERE PARTY_ID=:partyId AND IS_PRIMARY='Y'"
                + (exceptId == null ? "" : " AND PARTY_IDENTIFIER_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("partyId", partyId);
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public boolean identifierIsPrimary(long partyId, long identifierId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY_IDENTIFIER")
                        + " WHERE PARTY_ID=:partyId AND PARTY_IDENTIFIER_ID=:id AND IS_PRIMARY='Y'")
                .param("partyId", partyId).param("id", identifierId).query(Long.class).single() > 0;
    }

    public boolean identifierExists(String identifierTypeCode, String identifierValue, String issuerCode, java.time.LocalDate validFrom, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("PARTY_IDENTIFIER")
                + " WHERE IDENTIFIER_TYPE_CODE=:type AND IDENTIFIER_VALUE=:value"
                + " AND ((ISSUER_CODE=:issuer) OR (ISSUER_CODE IS NULL AND :issuer IS NULL))"
                + " AND VALID_FROM=:validFrom"
                + (exceptId == null ? "" : " AND PARTY_IDENTIFIER_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("type", identifierTypeCode).param("value", identifierValue)
                .param("issuer", issuerCode).param("validFrom", sqlDate(validFrom));
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public boolean partyIsPerson(long partyId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY") + " WHERE PARTY_ID=:partyId AND PARTY_TYPE_CODE='PERSON'")
                .param("partyId", partyId).query(Long.class).single() > 0;
    }

    public boolean partyExists(Long relatedPartyId) {
        if (relatedPartyId == null) return true;
        return jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY") + " WHERE PARTY_ID=:id")
                .param("id", relatedPartyId).query(Long.class).single() > 0;
    }

    public boolean kycCaseBelongsToParty(long partyId, Long kycCaseId) {
        if (kycCaseId == null) {
            return true;
        }
        long count = jdbc.sql("SELECT COUNT(*) FROM " + table("KYC_CASE") + " WHERE KYC_CASE_ID=:id AND PARTY_ID=:partyId")
                .param("id", kycCaseId).param("partyId", partyId).query(Long.class).single();
        return count > 0;
    }

    private Optional<Long> addressIdForPartyAddress(long partyId, long partyAddressId) {
        return jdbc.sql("SELECT ADDRESS_ID FROM " + table("PARTY_ADDRESS") + " WHERE PARTY_ADDRESS_ID=:id AND PARTY_ID=:partyId")
                .param("id", partyAddressId).param("partyId", partyId).query(Long.class).optional();
    }

    private int deleteChild(String table, String idColumn, long partyId, long id) {
        return jdbc.sql("DELETE FROM " + table(table) + " WHERE " + idColumn + "=:id AND PARTY_ID=:partyId")
                .param("id", id).param("partyId", partyId).update();
    }

    private long nextVal(String sequence) {
        if (!SQL_NAME.matcher(sequence).matches()) {
            throw new IllegalArgumentException("Invalid sequence name: " + sequence);
        }
        return jdbc.sql("SELECT " + schema + "." + sequence + ".NEXTVAL FROM DUAL")
                .query(Long.class).single();
    }

    private long count(String table) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table(table)).query(Long.class).single();
    }

    private String table(String tableName) {
        if (!SQL_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Invalid CIF table name: " + tableName);
        }
        return schema + "." + tableName;
    }

    private static PartyCore mapParty(ResultSet rs) throws SQLException {
        return new PartyCore(
                rs.getLong("PARTY_ID"), rs.getString("PARTY_UID"), rs.getString("PARTY_TYPE_CODE"),
                rs.getString("LIFECYCLE_STATUS_CODE"), rs.getString("STATUS_REASON_CODE"),
                localDateTime(rs, "STATUS_CHANGED_AT"), rs.getString("VERIFICATION_STATUS_CODE"),
                rs.getString("DATA_QUALITY_STATUS_CODE"), rs.getString("CREATION_SOURCE_CODE"),
                nullableLong(rs, "MERGED_INTO_PARTY_ID"), localDateTime(rs, "MERGED_AT"), rs.getString("MERGED_BY"),
                localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"), rs.getString("IS_CURRENT"),
                localDateTime(rs, "CREATED_AT"), rs.getString("CREATED_BY"), localDateTime(rs, "UPDATED_AT"),
                rs.getString("UPDATED_BY"), rs.getLong("RECORD_VERSION")
        );
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? null : value.longValue();
    }

    private static String trimChar(String value) {
        return value == null ? null : value.trim();
    }

    private static Date sqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private static Timestamp sqlTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
