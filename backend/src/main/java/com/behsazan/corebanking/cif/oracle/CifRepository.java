package com.behsazan.corebanking.cif.oracle;

import com.behsazan.corebanking.cif.domain.CifModels.*;
import com.behsazan.corebanking.cif.domain.CifModels.CifDashboardSummary;
import com.behsazan.corebanking.cif.domain.CifModels.ExternalInquiryRecord;
import com.behsazan.corebanking.cif.domain.CifModels.ExternalInquiryRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyConsentRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyConsentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.CommunicationPreferenceRecord;
import com.behsazan.corebanking.cif.domain.CifModels.CommunicationPreferenceRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyGeneralPreferenceRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyGeneralPreferenceRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyStatusHistoryRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyMergeHistoryRecord;
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
import com.behsazan.corebanking.cif.domain.CifModels.PartyClassificationRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyClassificationRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyRelationshipRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyRelationshipRequest;
import com.behsazan.corebanking.cif.domain.CifModels.BeneficialOwnershipRecord;
import com.behsazan.corebanking.cif.domain.CifModels.BeneficialOwnershipRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAuthorityRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAuthorityRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyRoleRecord;
import com.behsazan.corebanking.cif.domain.CifModels.PartyRoleRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyCustomerRecord;
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
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.LookupOption;
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
import java.util.Set;
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

    public List<PartyClassificationRecord> findClassifications(long partyId) {
        String sql = """
                SELECT PARTY_CLASSIFICATION_ID, PARTY_ID, CLASSIFICATION_TYPE_CODE, CLASSIFICATION_VALUE_CODE,
                       ASSIGNMENT_REASON_CODE, VALID_FROM, VALID_TO, DESCRIPTION_TEXT, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                ORDER BY VALID_FROM DESC, PARTY_CLASSIFICATION_ID DESC
                """.formatted(table("PARTY_CLASSIFICATION"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new PartyClassificationRecord(
                        rs.getLong("PARTY_CLASSIFICATION_ID"), rs.getLong("PARTY_ID"),
                        rs.getString("CLASSIFICATION_TYPE_CODE"), rs.getString("CLASSIFICATION_VALUE_CODE"),
                        rs.getString("ASSIGNMENT_REASON_CODE"), localDate(rs, "VALID_FROM"),
                        localDate(rs, "VALID_TO"), rs.getString("DESCRIPTION_TEXT"), rs.getLong("RECORD_VERSION")
                )).list();
    }


    public List<PartyRelationshipRecord> findRelationships(long partyId) {
        String sql = """
                SELECT PARTY_RELATIONSHIP_ID, PARTY_ID, RELATED_PARTY_ID, RELATIONSHIP_TYPE_CODE,
                       OWNERSHIP_PERCENT, POSITION_TITLE, SIGNING_RIGHT_CODE, AUTHORITY_LIMIT_AMOUNT,
                       START_DATE, END_DATE, EVIDENCE_DOCUMENT_ID, VERIFICATION_STATUS_CODE, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY START_DATE DESC, PARTY_RELATIONSHIP_ID DESC
                """.formatted(table("PARTY_RELATIONSHIP"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyRelationshipRecord(
                rs.getLong("PARTY_RELATIONSHIP_ID"), rs.getLong("PARTY_ID"), rs.getLong("RELATED_PARTY_ID"),
                rs.getString("RELATIONSHIP_TYPE_CODE"), rs.getBigDecimal("OWNERSHIP_PERCENT"),
                rs.getString("POSITION_TITLE"), rs.getString("SIGNING_RIGHT_CODE"),
                rs.getBigDecimal("AUTHORITY_LIMIT_AMOUNT"), localDate(rs, "START_DATE"), localDate(rs, "END_DATE"),
                nullableLong(rs, "EVIDENCE_DOCUMENT_ID"), rs.getString("VERIFICATION_STATUS_CODE"),
                rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<BeneficialOwnershipRecord> findBeneficialOwnerships(long partyId) {
        String sql = """
                SELECT OWNERSHIP_ID, LEGAL_PARTY_ID, BENEFICIAL_OWNER_PARTY_ID, DIRECT_OWNERSHIP_PERCENT,
                       INDIRECT_OWNERSHIP_PERCENT, CONTROL_PERCENT, CONTROL_BASIS_CODE, IS_ULTIMATE_OWNER,
                       OWNERSHIP_PATH, VALID_FROM, VALID_TO, EVIDENCE_REF, RECORD_VERSION
                FROM %s WHERE LEGAL_PARTY_ID=:partyId
                ORDER BY VALID_FROM DESC, OWNERSHIP_ID DESC
                """.formatted(table("BENEFICIAL_OWNERSHIP"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new BeneficialOwnershipRecord(
                rs.getLong("OWNERSHIP_ID"), rs.getLong("LEGAL_PARTY_ID"), rs.getLong("BENEFICIAL_OWNER_PARTY_ID"),
                rs.getBigDecimal("DIRECT_OWNERSHIP_PERCENT"), rs.getBigDecimal("INDIRECT_OWNERSHIP_PERCENT"),
                rs.getBigDecimal("CONTROL_PERCENT"), rs.getString("CONTROL_BASIS_CODE"), rs.getString("IS_ULTIMATE_OWNER"),
                rs.getString("OWNERSHIP_PATH"), localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"),
                rs.getString("EVIDENCE_REF"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<PartyAuthorityRecord> findAuthorities(long partyId) {
        String sql = """
                SELECT AUTHORITY_ID, PRINCIPAL_PARTY_ID, AUTHORIZED_PARTY_ID, AUTHORITY_TYPE_CODE, SCOPE_CODE,
                       MAX_AMOUNT, CURRENCY_CODE, VALID_FROM, VALID_TO, DOCUMENT_REF, PARTY_ID, RECORD_VERSION
                FROM %s WHERE PRINCIPAL_PARTY_ID=:partyId
                ORDER BY VALID_FROM DESC, AUTHORITY_ID DESC
                """.formatted(table("PARTY_AUTHORITY"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyAuthorityRecord(
                rs.getLong("AUTHORITY_ID"), rs.getLong("PRINCIPAL_PARTY_ID"), rs.getLong("AUTHORIZED_PARTY_ID"),
                rs.getString("AUTHORITY_TYPE_CODE"), rs.getString("SCOPE_CODE"), rs.getBigDecimal("MAX_AMOUNT"),
                rs.getString("CURRENCY_CODE"), localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"),
                rs.getString("DOCUMENT_REF"), rs.getLong("PARTY_ID"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<PartyRoleRecord> findRoles(long partyId) {
        String sql = """
                SELECT PARTY_ROLE_ID, PARTY_ID, ROLE_TYPE_CODE, CONTEXT_TYPE_CODE, CONTEXT_ID,
                       VALID_FROM, VALID_TO, STATUS_CODE, RECORD_VERSION, PRINCIPAL_PARTY_ID,
                       RELATIONSHIP_TYPE_CODE, AUTHORITY_BASIS_CODE, AUTHORITY_DOCUMENT_NO,
                       AUTHORITY_ISSUER, AUTHORITY_SCOPE_TEXT, ASSIGNMENT_REASON_TEXT, DESCRIPTION_TEXT
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY VALID_FROM DESC, PARTY_ROLE_ID DESC
                """.formatted(table("PARTY_ROLE"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyRoleRecord(
                rs.getLong("PARTY_ROLE_ID"), rs.getLong("PARTY_ID"), rs.getString("ROLE_TYPE_CODE"),
                rs.getString("CONTEXT_TYPE_CODE"), rs.getString("CONTEXT_ID"), localDate(rs, "VALID_FROM"),
                localDate(rs, "VALID_TO"), rs.getString("STATUS_CODE"), rs.getLong("RECORD_VERSION"),
                nullableLong(rs, "PRINCIPAL_PARTY_ID"), rs.getString("RELATIONSHIP_TYPE_CODE"),
                rs.getString("AUTHORITY_BASIS_CODE"), rs.getString("AUTHORITY_DOCUMENT_NO"),
                rs.getString("AUTHORITY_ISSUER"), rs.getString("AUTHORITY_SCOPE_TEXT"),
                rs.getString("ASSIGNMENT_REASON_TEXT"), rs.getString("DESCRIPTION_TEXT")
        )).list();
    }

    public Optional<PartyRoleRecord> findRole(long partyId, long roleId) {
        return findRoles(partyId).stream().filter(r -> r.partyRoleId() == roleId).findFirst();
    }

    public List<PartyCustomerRecord> findCustomers(long partyId) {
        String sql = """
                SELECT PARTY_CUSTOMER_ID, PARTY_ID, PARTY_ROLE_ID, CUSTOMER_NO, CUSTOMER_STATUS_CODE,
                       VALID_FROM, VALID_TO, IS_CURRENT, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY CASE WHEN IS_CURRENT='Y' THEN 0 ELSE 1 END, VALID_FROM DESC, PARTY_CUSTOMER_ID DESC
                """.formatted(table("PARTY_CUSTOMER"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyCustomerRecord(
                rs.getLong("PARTY_CUSTOMER_ID"), rs.getLong("PARTY_ID"), rs.getLong("PARTY_ROLE_ID"),
                rs.getString("CUSTOMER_NO"), rs.getString("CUSTOMER_STATUS_CODE"), localDate(rs, "VALID_FROM"),
                localDate(rs, "VALID_TO"), trimChar(rs.getString("IS_CURRENT")), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<KycCaseRecord> findKycCases(long partyId) {
        String sql = """
                SELECT KYC_CASE_ID, PARTY_ID, KYC_TYPE_CODE, DUE_DILIGENCE_LEVEL_CODE, STATUS_CODE,
                       OPENED_AT, COMPLETED_AT, REVIEWED_AT, NEXT_REVIEW_DATE, FINAL_RISK_LEVEL_CODE,
                       DECISION_CODE, DECISION_REASON, APPROVED_BY, RECORD_VERSION, RELATION_PURPOSE_CODE,
                       EXPECTED_ACTIVITY_LEVEL_CODE, GEOGRAPHIC_SCOPE_CODE, ACTIVITY_COUNTRIES_TEXT,
                       REQUESTED_PRODUCTS_TEXT, PREFERRED_SERVICE_CHANNEL_CODE, PEP_STATUS_CODE,
                       HIGH_RISK_COUNTRY_FLAG, EDD_REQUIRED_FLAG
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
                        rs.getString("DECISION_REASON"), rs.getString("APPROVED_BY"), rs.getLong("RECORD_VERSION"),
                        rs.getString("RELATION_PURPOSE_CODE"), rs.getString("EXPECTED_ACTIVITY_LEVEL_CODE"),
                        rs.getString("GEOGRAPHIC_SCOPE_CODE"), rs.getString("ACTIVITY_COUNTRIES_TEXT"),
                        rs.getString("REQUESTED_PRODUCTS_TEXT"), rs.getString("PREFERRED_SERVICE_CHANNEL_CODE"),
                        rs.getString("PEP_STATUS_CODE"), trimChar(rs.getString("HIGH_RISK_COUNTRY_FLAG")),
                        trimChar(rs.getString("EDD_REQUIRED_FLAG"))
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

    public List<ExternalInquiryRecord> findExternalInquiries(long partyId) {
        String sql = """
                SELECT INQUIRY_RESULT_ID, PARTY_ID, INQUIRY_TYPE_CODE, PROVIDER_CODE, REFERENCE_NO,
                       INQUIRY_RESULT_CODE, REQUESTED_AT, RESPONDED_AT, EXPIRY_AT, PAYLOAD_REF, PAYLOAD_HASH,
                       RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY REQUESTED_AT DESC, INQUIRY_RESULT_ID DESC
                """.formatted(table("EXTERNAL_INQUIRY_RESULT"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new ExternalInquiryRecord(
                rs.getLong("INQUIRY_RESULT_ID"), rs.getLong("PARTY_ID"), rs.getString("INQUIRY_TYPE_CODE"),
                rs.getString("PROVIDER_CODE"), rs.getString("REFERENCE_NO"), rs.getString("INQUIRY_RESULT_CODE"),
                localDateTime(rs, "REQUESTED_AT"), localDateTime(rs, "RESPONDED_AT"), localDateTime(rs, "EXPIRY_AT"),
                rs.getString("PAYLOAD_REF"), rs.getString("PAYLOAD_HASH"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<PartyConsentRecord> findConsents(long partyId) {
        String sql = """
                SELECT CONSENT_ID, PARTY_ID, CONSENT_TYPE_CODE, PURPOSE_CODE,
                       CASE WHEN CONSENT_STATUS_CODE='GRANTED' AND VALID_TO < TRUNC(SYSDATE) THEN 'EXPIRED' ELSE CONSENT_STATUS_CODE END AS CONSENT_STATUS_CODE,
                       GRANTED_AT, REVOKED_AT, EVIDENCE_REF, SOURCE_CODE, CUSTOMER_DECISION_CODE,
                       CAPTURE_CHANNEL_CODE, DECLARED_AT, VALID_TO, CONSENT_TEXT_VERSION_CODE,
                       SCOPE_TEXT, SCOPE_LIMITATION_TEXT, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY GRANTED_AT DESC, CONSENT_ID DESC
                """.formatted(table("PARTY_CONSENT"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyConsentRecord(
                rs.getLong("CONSENT_ID"), rs.getLong("PARTY_ID"), rs.getString("CONSENT_TYPE_CODE"),
                rs.getString("PURPOSE_CODE"), rs.getString("CONSENT_STATUS_CODE"), localDateTime(rs, "GRANTED_AT"),
                localDateTime(rs, "REVOKED_AT"), rs.getString("EVIDENCE_REF"), rs.getString("SOURCE_CODE"),
                rs.getString("CUSTOMER_DECISION_CODE"), rs.getString("CAPTURE_CHANNEL_CODE"),
                localDateTime(rs, "DECLARED_AT"), localDate(rs, "VALID_TO"), rs.getString("CONSENT_TEXT_VERSION_CODE"),
                rs.getString("SCOPE_TEXT"), rs.getString("SCOPE_LIMITATION_TEXT"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public Optional<PartyConsentRecord> findConsent(long partyId, long consentId) {
        return findConsents(partyId).stream().filter(x -> x.consentId() == consentId).findFirst();
    }

    public List<CommunicationPreferenceRecord> findCommunicationPreferences(long partyId) {
        String sql = """
                SELECT PREFERENCE_ID, PARTY_ID, CHANNEL_CODE, PURPOSE_CODE, ALLOWED_FLAG, PREFERRED_TIME_FROM,
                       PREFERRED_TIME_TO, LANGUAGE_CODE, ALLOWED_DAYS_CODE, TIME_ZONE_CODE,
                       MARKETING_OPT_OUT_FLAG, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId ORDER BY PURPOSE_CODE, CHANNEL_CODE, PREFERENCE_ID
                """.formatted(table("COMMUNICATION_PREFERENCE"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new CommunicationPreferenceRecord(
                rs.getLong("PREFERENCE_ID"), rs.getLong("PARTY_ID"), rs.getString("CHANNEL_CODE"),
                rs.getString("PURPOSE_CODE"), trimChar(rs.getString("ALLOWED_FLAG")), rs.getString("PREFERRED_TIME_FROM"),
                rs.getString("PREFERRED_TIME_TO"), rs.getString("LANGUAGE_CODE"), rs.getString("ALLOWED_DAYS_CODE"),
                rs.getString("TIME_ZONE_CODE"), trimChar(rs.getString("MARKETING_OPT_OUT_FLAG")), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<PartyGeneralPreferenceRecord> findGeneralPreferences(long partyId) {
        String sql = """
                SELECT PREFERENCE_ID, PARTY_ID, PREFERENCE_TYPE_CODE, PREFERENCE_VALUE, VALID_FROM, VALID_TO,
                       SOURCE_CODE, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId ORDER BY VALID_FROM DESC, PREFERENCE_ID DESC
                """.formatted(table("PARTY_GENERAL_PREFERENCE"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyGeneralPreferenceRecord(
                rs.getLong("PREFERENCE_ID"), rs.getLong("PARTY_ID"), rs.getString("PREFERENCE_TYPE_CODE"),
                rs.getString("PREFERENCE_VALUE"), localDateTime(rs, "VALID_FROM"), localDateTime(rs, "VALID_TO"),
                rs.getString("SOURCE_CODE"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<PartyStatusHistoryRecord> findStatusHistory(long partyId) {
        String sql = """
                SELECT PARTY_STATUS_HISTORY_ID, PARTY_ID, LIFECYCLE_STATUS_CODE, STATUS_REASON_CODE,
                       VALID_FROM, VALID_TO, DESCRIPTION_TEXT, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, RECORD_VERSION
                FROM %s WHERE PARTY_ID=:partyId
                ORDER BY VALID_FROM DESC, PARTY_STATUS_HISTORY_ID DESC
                """.formatted(table("PARTY_STATUS_HISTORY"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyStatusHistoryRecord(
                rs.getLong("PARTY_STATUS_HISTORY_ID"), rs.getLong("PARTY_ID"), rs.getString("LIFECYCLE_STATUS_CODE"),
                rs.getString("STATUS_REASON_CODE"), localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"),
                rs.getString("DESCRIPTION_TEXT"), localDateTime(rs, "CREATED_AT"), rs.getString("CREATED_BY"),
                localDateTime(rs, "UPDATED_AT"), rs.getString("UPDATED_BY"), rs.getLong("RECORD_VERSION")
        )).list();
    }

    public List<PartyMergeHistoryRecord> findMergeHistory(long partyId) {
        String sql = """
                SELECT PARTY_MERGE_ID, SOURCE_PARTY_ID, TARGET_PARTY_ID, MERGE_REASON_CODE, CONFLICT_RESOLUTION_CODE,
                       MERGED_AT, MERGED_BY, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, RECORD_VERSION, CREATED_DATE
                FROM %s WHERE SOURCE_PARTY_ID=:partyId OR TARGET_PARTY_ID=:partyId
                ORDER BY MERGED_AT DESC, PARTY_MERGE_ID DESC
                """.formatted(table("PARTY_MERGE_HISTORY"));
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyMergeHistoryRecord(
                rs.getLong("PARTY_MERGE_ID"), rs.getLong("SOURCE_PARTY_ID"), rs.getLong("TARGET_PARTY_ID"),
                rs.getString("MERGE_REASON_CODE"), rs.getString("CONFLICT_RESOLUTION_CODE"),
                localDateTime(rs, "MERGED_AT"), rs.getString("MERGED_BY"), localDateTime(rs, "CREATED_AT"),
                rs.getString("CREATED_BY"), localDateTime(rs, "UPDATED_AT"), rs.getString("UPDATED_BY"),
                rs.getLong("RECORD_VERSION"), localDateTime(rs, "CREATED_DATE")
        )).list();
    }

    private static final int SOURCE_360_LIMIT = 100;

    public Party360SummaryRecord findParty360Summary(PartyCore party) {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM %s H WHERE H.PARTY_ID=:partyId AND H.STATUS_CODE='ACTIVE'
                     AND (H.END_DATE IS NULL OR H.END_DATE >= TRUNC(SYSDATE))) AS ACTIVE_PRODUCT_COUNT,
                  (SELECT COUNT(*) FROM %s C WHERE C.PARTY_ID=:partyId AND C.STATUS_CODE IN ('OPEN','IN_PROGRESS')) AS OPEN_COMPLAINT_COUNT,
                  (SELECT MAX(I.OCCURRED_AT) FROM %s I WHERE I.PARTY_ID=:partyId) AS LAST_INTERACTION_AT,
                  (SELECT SCORE_VALUE FROM (
                     SELECT V.SCORE_VALUE FROM %s V WHERE V.PARTY_ID=:partyId AND V.SCORE_TYPE_CODE='CUSTOMER_VALUE'
                     ORDER BY V.AS_OF_DATE DESC, V.VALUE_SCORE_ID DESC
                   ) WHERE ROWNUM=1) AS CURRENT_VALUE_SCORE,
                  (SELECT SEGMENT_CODE FROM (
                     SELECT S.SEGMENT_CODE FROM %s S WHERE S.PARTY_ID=:partyId
                       AND (S.VALID_TO IS NULL OR S.VALID_TO >= SYSTIMESTAMP)
                     ORDER BY S.ASSIGNED_AT DESC, S.SEGMENT_MEMBERSHIP_ID DESC
                   ) WHERE ROWNUM=1) AS CURRENT_SEGMENT_CODE,
                  (SELECT COUNT(*) FROM %s R WHERE R.PARTY_ID=:partyId AND R.STATUS_CODE IN ('NEW','PRESENTED')
                     AND (R.EXPIRY_AT IS NULL OR R.EXPIRY_AT >= SYSTIMESTAMP)) AS ACTIVE_RECOMMENDATION_COUNT,
                  SYSTIMESTAMP AS AS_OF_TIMESTAMP
                FROM DUAL
                """.formatted(table("PARTY_PRODUCT_HOLDING"), table("PARTY_COMPLAINT"), table("PARTY_INTERACTION"),
                table("PARTY_VALUE_SCORE"), table("PARTY_SEGMENT_MEMBERSHIP"), table("PARTY_RECOMMENDATION"));
        return jdbc.sql(sql).param("partyId", party.partyId()).query((rs, rowNum) -> new Party360SummaryRecord(
                party.partyId(), party.partyUid(), party.lifecycleStatusCode(), rs.getLong("ACTIVE_PRODUCT_COUNT"),
                rs.getLong("OPEN_COMPLAINT_COUNT"), localDateTime(rs, "LAST_INTERACTION_AT"),
                rs.getBigDecimal("CURRENT_VALUE_SCORE"), rs.getString("CURRENT_SEGMENT_CODE"),
                rs.getLong("ACTIVE_RECOMMENDATION_COUNT"), localDateTime(rs, "AS_OF_TIMESTAMP")
        )).single();
    }

    public List<OrganizationOfficer360Record> findOrganizationOfficers360(long partyId) {
        String sql = """
                SELECT ORGANIZATION_OFFICER_ID, ORGANIZATION_PARTY_ID, OFFICER_PARTY_ID, OFFICER_ROLE_CODE, VALID_FROM, VALID_TO
                FROM %s WHERE ORGANIZATION_PARTY_ID=:partyId
                ORDER BY VALID_FROM DESC, ORGANIZATION_OFFICER_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("ORGANIZATION_OFFICER"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new OrganizationOfficer360Record(
                rs.getLong("ORGANIZATION_OFFICER_ID"), rs.getLong("ORGANIZATION_PARTY_ID"), rs.getLong("OFFICER_PARTY_ID"),
                rs.getString("OFFICER_ROLE_CODE"), localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO")
        )).list();
    }

    public List<PartyAlertCase360Record> findAlerts360(long partyId) {
        String sql = """
                SELECT ALERT_CASE_ID, PARTY_ID, ALERT_TYPE_CODE, SEVERITY_CODE, STATUS_CODE, SOURCE_SYSTEM_CODE,
                       SOURCE_REFERENCE_NO, OPENED_AT, CLOSED_AT, ASSIGNED_TO, RESOLUTION_CODE, RESOLUTION_NOTE
                FROM %s WHERE PARTY_ID=:partyId ORDER BY OPENED_AT DESC, ALERT_CASE_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_ALERT_CASE"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyAlertCase360Record(
                rs.getLong("ALERT_CASE_ID"), rs.getLong("PARTY_ID"), rs.getString("ALERT_TYPE_CODE"), rs.getString("SEVERITY_CODE"),
                rs.getString("STATUS_CODE"), rs.getString("SOURCE_SYSTEM_CODE"), rs.getString("SOURCE_REFERENCE_NO"),
                localDateTime(rs, "OPENED_AT"), localDateTime(rs, "CLOSED_AT"), rs.getString("ASSIGNED_TO"),
                rs.getString("RESOLUTION_CODE"), rs.getString("RESOLUTION_NOTE")
        )).list();
    }

    public List<PartyComplaint360Record> findComplaints360(long partyId) {
        String sql = """
                SELECT COMPLAINT_ID, PARTY_ID, COMPLAINT_TYPE_CODE, PRODUCT_TYPE_CODE, SEVERITY_CODE, STATUS_CODE,
                       OPENED_AT, DUE_AT, RESOLVED_AT, RESOLUTION_CODE, ASSIGNED_UNIT_CODE, COMPLAINT_DESCRIPTION
                FROM %s WHERE PARTY_ID=:partyId ORDER BY OPENED_AT DESC, COMPLAINT_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_COMPLAINT"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyComplaint360Record(
                rs.getLong("COMPLAINT_ID"), rs.getLong("PARTY_ID"), rs.getString("COMPLAINT_TYPE_CODE"),
                rs.getString("PRODUCT_TYPE_CODE"), rs.getString("SEVERITY_CODE"), rs.getString("STATUS_CODE"),
                localDateTime(rs, "OPENED_AT"), localDateTime(rs, "DUE_AT"), localDateTime(rs, "RESOLVED_AT"),
                rs.getString("RESOLUTION_CODE"), rs.getString("ASSIGNED_UNIT_CODE"), rs.getString("COMPLAINT_DESCRIPTION")
        )).list();
    }

    public List<PartyComplaintStatus360Record> findComplaintStatus360(long partyId) {
        String sql = """
                SELECT H.COMPLAINT_STATUS_HISTORY_ID, H.COMPLAINT_ID, H.FROM_STATUS_CODE, H.TO_STATUS_CODE,
                       H.CHANGED_AT, H.CHANGED_BY, H.REASON_CODE, H.COMMENT_TEXT
                FROM %s H JOIN %s C ON C.COMPLAINT_ID=H.COMPLAINT_ID
                WHERE C.PARTY_ID=:partyId
                ORDER BY H.CHANGED_AT DESC, H.COMPLAINT_STATUS_HISTORY_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_COMPLAINT_STATUS_HISTORY"), table("PARTY_COMPLAINT"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyComplaintStatus360Record(
                rs.getLong("COMPLAINT_STATUS_HISTORY_ID"), rs.getLong("COMPLAINT_ID"), rs.getString("FROM_STATUS_CODE"),
                rs.getString("TO_STATUS_CODE"), localDateTime(rs, "CHANGED_AT"), rs.getString("CHANGED_BY"),
                rs.getString("REASON_CODE"), rs.getString("COMMENT_TEXT")
        )).list();
    }

    public List<PartyGroupMembership360Record> findGroupMemberships360(long partyId) {
        String sql = """
                SELECT M.GROUP_MEMBER_ID, M.GROUP_ID, G.GROUP_TYPE_CODE, G.GROUP_NAME, G.STATUS_CODE AS GROUP_STATUS_CODE,
                       M.MEMBER_ROLE_CODE, M.OWNERSHIP_PERCENT, M.VALID_FROM, M.VALID_TO
                FROM %s M JOIN %s G ON G.GROUP_ID=M.GROUP_ID
                WHERE M.PARTY_ID=:partyId ORDER BY M.VALID_FROM DESC, M.GROUP_MEMBER_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_GROUP_MEMBER"), table("PARTY_GROUP"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyGroupMembership360Record(
                rs.getLong("GROUP_MEMBER_ID"), rs.getLong("GROUP_ID"), rs.getString("GROUP_TYPE_CODE"), rs.getString("GROUP_NAME"),
                rs.getString("GROUP_STATUS_CODE"), rs.getString("MEMBER_ROLE_CODE"), rs.getBigDecimal("OWNERSHIP_PERCENT"),
                localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO")
        )).list();
    }

    public List<PartyInteraction360Record> findInteractions360(long partyId) {
        String sql = """
                SELECT INTERACTION_ID, PARTY_ID, CHANNEL_CODE, INTERACTION_TYPE_CODE, SUBJECT, OCCURRED_AT, OUTCOME_CODE,
                       EMPLOYEE_ID, BRANCH_CODE, REFERENCE_TYPE_CODE, REFERENCE_ID, INTERACTION_DETAILS
                FROM %s WHERE PARTY_ID=:partyId ORDER BY OCCURRED_AT DESC, INTERACTION_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_INTERACTION"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyInteraction360Record(
                rs.getLong("INTERACTION_ID"), rs.getLong("PARTY_ID"), rs.getString("CHANNEL_CODE"), rs.getString("INTERACTION_TYPE_CODE"),
                rs.getString("SUBJECT"), localDateTime(rs, "OCCURRED_AT"), rs.getString("OUTCOME_CODE"), rs.getString("EMPLOYEE_ID"),
                rs.getString("BRANCH_CODE"), rs.getString("REFERENCE_TYPE_CODE"), rs.getString("REFERENCE_ID"), rs.getString("INTERACTION_DETAILS")
        )).list();
    }

    public List<PartyJourneyEvent360Record> findJourneyEvents360(long partyId) {
        String sql = """
                SELECT JOURNEY_EVENT_ID, PARTY_ID, JOURNEY_CODE, STAGE_CODE, EVENT_CODE, OCCURRED_AT, CHANNEL_CODE,
                       REFERENCE_TYPE_CODE, REFERENCE_ID
                FROM %s WHERE PARTY_ID=:partyId ORDER BY OCCURRED_AT DESC, JOURNEY_EVENT_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_JOURNEY_EVENT"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyJourneyEvent360Record(
                rs.getLong("JOURNEY_EVENT_ID"), rs.getLong("PARTY_ID"), rs.getString("JOURNEY_CODE"), rs.getString("STAGE_CODE"),
                rs.getString("EVENT_CODE"), localDateTime(rs, "OCCURRED_AT"), rs.getString("CHANNEL_CODE"),
                rs.getString("REFERENCE_TYPE_CODE"), rs.getString("REFERENCE_ID")
        )).list();
    }

    public List<PartyMetricSnapshot360Record> findMetricSnapshots360(long partyId) {
        String sql = """
                SELECT METRIC_SNAPSHOT_ID, PARTY_ID, METRIC_CODE, METRIC_VALUE, METRIC_UNIT_CODE, AS_OF_DATE,
                       PERIOD_FROM, PERIOD_TO, SOURCE_SNAPSHOT_ID
                FROM %s WHERE PARTY_ID=:partyId ORDER BY AS_OF_DATE DESC, METRIC_SNAPSHOT_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_METRIC_SNAPSHOT"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyMetricSnapshot360Record(
                rs.getLong("METRIC_SNAPSHOT_ID"), rs.getLong("PARTY_ID"), rs.getString("METRIC_CODE"), rs.getBigDecimal("METRIC_VALUE"),
                rs.getString("METRIC_UNIT_CODE"), localDate(rs, "AS_OF_DATE"), localDate(rs, "PERIOD_FROM"),
                localDate(rs, "PERIOD_TO"), rs.getString("SOURCE_SNAPSHOT_ID")
        )).list();
    }

    public List<PartyOperationLimit360Record> findOperationLimits360(long partyId) {
        String sql = """
                SELECT LIMIT_ID, PARTY_ID, LIMIT_TYPE_CODE, CONTEXT_TYPE_CODE, CONTEXT_ID, CURRENCY_CODE, LIMIT_AMOUNT,
                       PERIOD_CODE, VALID_FROM, VALID_TO, APPROVAL_REF
                FROM %s WHERE PARTY_ID=:partyId ORDER BY VALID_FROM DESC, LIMIT_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_OPERATION_LIMIT"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyOperationLimit360Record(
                rs.getLong("LIMIT_ID"), rs.getLong("PARTY_ID"), rs.getString("LIMIT_TYPE_CODE"), rs.getString("CONTEXT_TYPE_CODE"),
                rs.getString("CONTEXT_ID"), trimChar(rs.getString("CURRENCY_CODE")), rs.getBigDecimal("LIMIT_AMOUNT"),
                rs.getString("PERIOD_CODE"), localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"), rs.getString("APPROVAL_REF")
        )).list();
    }

    public List<PartyProductHolding360Record> findProductHoldings360(long partyId) {
        String sql = """
                SELECT PARTY_PRODUCT_HOLDING_ID, PARTY_ID, PRODUCT_TYPE_CODE, PRODUCT_INSTANCE_ID, RELATIONSHIP_ROLE_CODE,
                       STATUS_CODE, START_DATE, END_DATE, IS_PRIMARY
                FROM %s WHERE PARTY_ID=:partyId ORDER BY START_DATE DESC, PARTY_PRODUCT_HOLDING_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_PRODUCT_HOLDING"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyProductHolding360Record(
                rs.getLong("PARTY_PRODUCT_HOLDING_ID"), rs.getLong("PARTY_ID"), rs.getString("PRODUCT_TYPE_CODE"),
                rs.getString("PRODUCT_INSTANCE_ID"), rs.getString("RELATIONSHIP_ROLE_CODE"), rs.getString("STATUS_CODE"),
                localDate(rs, "START_DATE"), localDate(rs, "END_DATE"), trimChar(rs.getString("IS_PRIMARY"))
        )).list();
    }

    public List<PartyProductRestriction360Record> findProductRestrictions360(long partyId) {
        String sql = """
                SELECT RESTRICTION_ID, PARTY_ID, PRODUCT_TYPE_CODE, RESTRICTION_TYPE_CODE, REASON_CODE, SEVERITY_CODE,
                       VALID_FROM, VALID_TO, STATUS_CODE, APPROVAL_REF
                FROM %s WHERE PARTY_ID=:partyId ORDER BY VALID_FROM DESC, RESTRICTION_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_PRODUCT_RESTRICTION"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyProductRestriction360Record(
                rs.getLong("RESTRICTION_ID"), rs.getLong("PARTY_ID"), rs.getString("PRODUCT_TYPE_CODE"),
                rs.getString("RESTRICTION_TYPE_CODE"), rs.getString("REASON_CODE"), rs.getString("SEVERITY_CODE"),
                localDateTime(rs, "VALID_FROM"), localDateTime(rs, "VALID_TO"), rs.getString("STATUS_CODE"), rs.getString("APPROVAL_REF")
        )).list();
    }

    public List<PartyRecommendation360Record> findRecommendations360(long partyId) {
        String sql = """
                SELECT RECOMMENDATION_ID, PARTY_ID, RECOMMENDATION_TYPE_CODE, OFFER_CODE, PRIORITY_VALUE, SCORE_VALUE,
                       GENERATED_AT, EXPIRY_AT, STATUS_CODE, MODEL_CODE
                FROM %s WHERE PARTY_ID=:partyId ORDER BY GENERATED_AT DESC, RECOMMENDATION_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_RECOMMENDATION"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyRecommendation360Record(
                rs.getLong("RECOMMENDATION_ID"), rs.getLong("PARTY_ID"), rs.getString("RECOMMENDATION_TYPE_CODE"),
                rs.getString("OFFER_CODE"), nullableLong(rs, "PRIORITY_VALUE"), rs.getBigDecimal("SCORE_VALUE"),
                localDateTime(rs, "GENERATED_AT"), localDateTime(rs, "EXPIRY_AT"), rs.getString("STATUS_CODE"), rs.getString("MODEL_CODE")
        )).list();
    }

    public List<PartyRegistrationRequest360Record> findRegistrationRequests360(long partyId) {
        String sql = """
                SELECT REGISTRATION_REQUEST_ID, TEMPORARY_KEY, PARTY_TYPE_CODE, CREATION_SOURCE_CODE, VALID_FROM,
                       REQUEST_STATUS_CODE, IDENTITY_KIND_CODE, REQUESTED_AT, EXPIRES_AT, COMPLETED_PARTY_ID
                FROM %s WHERE COMPLETED_PARTY_ID=:partyId ORDER BY REQUESTED_AT DESC, REGISTRATION_REQUEST_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_REGISTRATION_REQUEST"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyRegistrationRequest360Record(
                rs.getLong("REGISTRATION_REQUEST_ID"), rs.getString("TEMPORARY_KEY"), rs.getString("PARTY_TYPE_CODE"),
                rs.getString("CREATION_SOURCE_CODE"), localDate(rs, "VALID_FROM"), rs.getString("REQUEST_STATUS_CODE"),
                rs.getString("IDENTITY_KIND_CODE"), localDateTime(rs, "REQUESTED_AT"), localDateTime(rs, "EXPIRES_AT"),
                nullableLong(rs, "COMPLETED_PARTY_ID")
        )).list();
    }

    public List<PartySegmentMembership360Record> findSegmentMemberships360(long partyId) {
        String sql = """
                SELECT SEGMENT_MEMBERSHIP_ID, PARTY_ID, SEGMENT_CODE, MODEL_CODE, ASSIGNED_AT, VALID_TO, CONFIDENCE_LEVEL
                FROM %s WHERE PARTY_ID=:partyId ORDER BY ASSIGNED_AT DESC, SEGMENT_MEMBERSHIP_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_SEGMENT_MEMBERSHIP"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartySegmentMembership360Record(
                rs.getLong("SEGMENT_MEMBERSHIP_ID"), rs.getLong("PARTY_ID"), rs.getString("SEGMENT_CODE"), rs.getString("MODEL_CODE"),
                localDateTime(rs, "ASSIGNED_AT"), localDateTime(rs, "VALID_TO"), rs.getBigDecimal("CONFIDENCE_LEVEL")
        )).list();
    }

    public List<PartyValueScore360Record> findValueScores360(long partyId) {
        String sql = """
                SELECT VALUE_SCORE_ID, PARTY_ID, SCORE_TYPE_CODE, SCORE_VALUE, SCORE_BAND_CODE, MODEL_CODE, MODEL_VERSION,
                       AS_OF_DATE, DATA_PERIOD_FROM, DATA_PERIOD_TO, CONFIDENCE_LEVEL, EXPLANATION
                FROM %s WHERE PARTY_ID=:partyId ORDER BY AS_OF_DATE DESC, VALUE_SCORE_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("PARTY_VALUE_SCORE"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new PartyValueScore360Record(
                rs.getLong("VALUE_SCORE_ID"), rs.getLong("PARTY_ID"), rs.getString("SCORE_TYPE_CODE"), rs.getBigDecimal("SCORE_VALUE"),
                rs.getString("SCORE_BAND_CODE"), rs.getString("MODEL_CODE"), rs.getString("MODEL_VERSION"), localDate(rs, "AS_OF_DATE"),
                localDate(rs, "DATA_PERIOD_FROM"), localDate(rs, "DATA_PERIOD_TO"), rs.getBigDecimal("CONFIDENCE_LEVEL"), rs.getString("EXPLANATION")
        )).list();
    }

    public List<SignatureSpecimen360Record> findSignatureSpecimens360(long partyId) {
        String sql = """
                SELECT SIGNATURE_ID, PARTY_ID, SIGNATORY_ID, SPECIMEN_TYPE_CODE,
                       CASE WHEN SIGNATURE_IMAGE IS NULL THEN 0 ELSE 1 END AS HAS_SIGNATURE_IMAGE,
                       EFFECTIVE_FROM, EFFECTIVE_TO, STATUS_CODE, SIGNING_RULE_CODE, VERIFICATION_STATUS_CODE,
                       CAPTURE_CHANNEL_CODE, DOCUMENT_ID, BRANCH_ID, CAPTURED_BY, REVOKED_AT, REVOCATION_REASON
                FROM %s WHERE PARTY_ID=:partyId ORDER BY EFFECTIVE_FROM DESC, SIGNATURE_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("SIGNATURE_SPECIMEN"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new SignatureSpecimen360Record(
                rs.getLong("SIGNATURE_ID"), rs.getLong("PARTY_ID"), rs.getString("SIGNATORY_ID"), rs.getString("SPECIMEN_TYPE_CODE"),
                rs.getInt("HAS_SIGNATURE_IMAGE") == 1, localDate(rs, "EFFECTIVE_FROM"), localDate(rs, "EFFECTIVE_TO"),
                rs.getString("STATUS_CODE"), rs.getString("SIGNING_RULE_CODE"), rs.getString("VERIFICATION_STATUS_CODE"),
                rs.getString("CAPTURE_CHANNEL_CODE"), nullableLong(rs, "DOCUMENT_ID"), nullableLong(rs, "BRANCH_ID"),
                rs.getString("CAPTURED_BY"), localDate(rs, "REVOKED_AT"), rs.getString("REVOCATION_REASON")
        )).list();
    }

    public List<AuditEvent360Record> findAuditEvents360(long partyId) {
        String sql = """
                SELECT AUDIT_EVENT_ID, ENTITY_TYPE_CODE, ENTITY_ID, ACTION_CODE, ACTOR_ID, ACTOR_ROLE_CODE, OCCURRED_AT,
                       CHANNEL_CODE, REQUEST_ID, REASON_CODE, APPROVAL_REF, CLIENT_IP, HASH_ALGORITHM_CODE
                FROM %s WHERE PARTY_ID=:partyId ORDER BY OCCURRED_AT DESC, AUDIT_EVENT_ID DESC FETCH FIRST %d ROWS ONLY
                """.formatted(table("AUDIT_EVENT"), SOURCE_360_LIMIT);
        return jdbc.sql(sql).param("partyId", partyId).query((rs, rowNum) -> new AuditEvent360Record(
                rs.getLong("AUDIT_EVENT_ID"), rs.getString("ENTITY_TYPE_CODE"), rs.getString("ENTITY_ID"), rs.getString("ACTION_CODE"),
                rs.getString("ACTOR_ID"), rs.getString("ACTOR_ROLE_CODE"), localDateTime(rs, "OCCURRED_AT"), rs.getString("CHANNEL_CODE"),
                rs.getString("REQUEST_ID"), rs.getString("REASON_CODE"), rs.getString("APPROVAL_REF"), rs.getString("CLIENT_IP"),
                rs.getString("HASH_ALGORITHM_CODE")
        )).list();
    }

    public Party360SourceData findSource360(long partyId) {
        return new Party360SourceData(
                findOrganizationOfficers360(partyId), findAlerts360(partyId), findComplaints360(partyId),
                findComplaintStatus360(partyId), findGroupMemberships360(partyId), findInteractions360(partyId),
                findJourneyEvents360(partyId), findMetricSnapshots360(partyId), findOperationLimits360(partyId),
                findProductHoldings360(partyId), findProductRestrictions360(partyId), findRecommendations360(partyId),
                findRegistrationRequests360(partyId), findSegmentMemberships360(partyId), findValueScores360(partyId),
                findSignatureSpecimens360(partyId), findAuditEvents360(partyId)
        );
    }

    public Party360Response loadParty360(long partyId) {
        PartyCore party = findParty(partyId).orElse(null);
        if (party == null) {
            return null;
        }
        return new Party360Response(
                party,
                findParty360Summary(party),
                findSource360(partyId),
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
                findClassifications(partyId),
                findRelationships(partyId),
                findBeneficialOwnerships(partyId),
                findAuthorities(partyId),
                findRoles(partyId),
                findCustomers(partyId),
                findKycCases(partyId),
                findDocuments(partyId),
                findRiskAssessments(partyId),
                findScreenings(partyId),
                findExternalInquiries(partyId),
                findConsents(partyId),
                findCommunicationPreferences(partyId),
                findGeneralPreferences(partyId),
                findStatusHistory(partyId),
                findMergeHistory(partyId)
        );
    }

    public boolean activeStatusReasonExists(String reasonCode) {
        return activeReferenceCodeExists("REF_PARTY_STATUS_REASON", "REASON_CODE", reasonCode);
    }

    public void insertStatusHistory(long partyId, String lifecycleStatusCode, String statusReasonCode,
                                    LocalDate validFrom, LocalDate validTo, String descriptionText, String actor) {
        long id = nextVal("SEQ_PARTY_STATUS_HISTORY");
        String sql = """
                INSERT INTO %s (
                    PARTY_STATUS_HISTORY_ID, PARTY_ID, LIFECYCLE_STATUS_CODE, STATUS_REASON_CODE,
                    VALID_FROM, VALID_TO, DESCRIPTION_TEXT, CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :lifecycle, :reason, :validFrom, :validTo, :description, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("PARTY_STATUS_HISTORY"));
        jdbc.sql(sql).param("id", id).param("partyId", partyId).param("lifecycle", lifecycleStatusCode)
                .param("reason", statusReasonCode).param("validFrom", sqlDate(validFrom)).param("validTo", sqlDate(validTo))
                .param("description", descriptionText).param("actor", actor).update();
    }

    public int closeOpenStatusHistory(long partyId, LocalDate validTo, String actor) {
        String sql = """
                UPDATE %s SET VALID_TO=:validTo, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_ID=:partyId AND VALID_TO IS NULL
                """.formatted(table("PARTY_STATUS_HISTORY"));
        return jdbc.sql(sql).param("validTo", sqlDate(validTo)).param("actor", actor).param("partyId", partyId).update();
    }

    public int updatePartyStatus(long partyId, String lifecycleStatusCode, String statusReasonCode, long recordVersion, String actor) {
        String sql = """
                UPDATE %s SET LIFECYCLE_STATUS_CODE=:lifecycle, STATUS_REASON_CODE=:reason, STATUS_CHANGED_AT=SYSTIMESTAMP,
                              UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY"));
        return jdbc.sql(sql).param("lifecycle", lifecycleStatusCode).param("reason", statusReasonCode)
                .param("actor", actor).param("partyId", partyId).param("recordVersion", recordVersion).update();
    }

    public int markPartyMerged(long sourcePartyId, long targetPartyId, String statusReasonCode, long recordVersion, String actor) {
        String sql = """
                UPDATE %s SET LIFECYCLE_STATUS_CODE='MERGED', STATUS_REASON_CODE=:reason, STATUS_CHANGED_AT=SYSTIMESTAMP,
                              MERGED_INTO_PARTY_ID=:targetPartyId, MERGED_AT=SYSTIMESTAMP, MERGED_BY=:actor,
                              UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_ID=:sourcePartyId AND RECORD_VERSION=:recordVersion AND LIFECYCLE_STATUS_CODE<>'MERGED'
                """.formatted(table("PARTY"));
        return jdbc.sql(sql).param("reason", statusReasonCode).param("targetPartyId", targetPartyId).param("actor", actor)
                .param("sourcePartyId", sourcePartyId).param("recordVersion", recordVersion).update();
    }

    public void insertMergeHistory(long sourcePartyId, long targetPartyId, String mergeReasonCode,
                                   String conflictResolutionCode, String actor) {
        long id = nextVal("SEQ_PARTY_MERGE_HISTORY");
        String sql = """
                INSERT INTO %s (
                    PARTY_MERGE_ID, SOURCE_PARTY_ID, TARGET_PARTY_ID, MERGE_REASON_CODE, CONFLICT_RESOLUTION_CODE,
                    MERGED_AT, MERGED_BY, CREATED_AT, CREATED_BY, RECORD_VERSION, CREATED_DATE
                ) VALUES (
                    :id, :sourcePartyId, :targetPartyId, :mergeReason, :conflictResolution,
                    SYSTIMESTAMP, :actor, SYSTIMESTAMP, :actor, 1, SYSTIMESTAMP
                )
                """.formatted(table("PARTY_MERGE_HISTORY"));
        jdbc.sql(sql).param("id", id).param("sourcePartyId", sourcePartyId).param("targetPartyId", targetPartyId)
                .param("mergeReason", mergeReasonCode).param("conflictResolution", conflictResolutionCode)
                .param("actor", actor).update();
    }

    /**
     * Moves currently-valid Party names to the canonical merge target. Exact semantic duplicates are left
     * on the merged source because the target already contains the same name. If the target already has
     * a primary name, a transferred source primary is demoted to keep the one-primary application rule.
     */
    public int transferValidNames(long sourcePartyId, long targetPartyId, String actor) {
        long targetHasPrimary = jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY_NAME")
                        + " WHERE PARTY_ID=:targetPartyId AND IS_PRIMARY='Y'")
                .param("targetPartyId", targetPartyId).query(Long.class).single();
        String names = table("PARTY_NAME");
        String sql = """
                UPDATE %s src
                   SET PARTY_ID=:targetPartyId,
                       IS_PRIMARY=CASE WHEN :demotePrimary=1 AND IS_PRIMARY='Y' THEN 'N' ELSE IS_PRIMARY END,
                       UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                 WHERE PARTY_ID=:sourcePartyId
                   AND VALID_FROM <= TRUNC(SYSDATE)
                   AND (VALID_TO IS NULL OR VALID_TO >= TRUNC(SYSDATE))
                   AND NOT EXISTS (
                       SELECT 1 FROM %s target
                        WHERE target.PARTY_ID=:targetPartyId
                          AND target.NAME_TYPE_CODE=src.NAME_TYPE_CODE
                          AND NVL(target.LANGUAGE_CODE,'#')=NVL(src.LANGUAGE_CODE,'#')
                          AND NVL(target.SCRIPT_CODE,'#')=NVL(src.SCRIPT_CODE,'#')
                          AND NVL(target.NORMALIZED_NAME,target.FULL_NAME)=NVL(src.NORMALIZED_NAME,src.FULL_NAME)
                          AND target.VALID_FROM=src.VALID_FROM
                   )
                """.formatted(names, names);
        return jdbc.sql(sql).param("targetPartyId", targetPartyId).param("sourcePartyId", sourcePartyId)
                .param("demotePrimary", targetHasPrimary > 0 ? 1 : 0).param("actor", actor).update();
    }

    /**
     * Moves active/current identifiers to the canonical merge target. The global identifier unique key is
     * unchanged because PARTY_ID is not part of UQ_IDENTIFIER. A source primary is demoted when the target
     * already owns a primary identifier.
     */
    public int transferValidIdentifiers(long sourcePartyId, long targetPartyId, String actor) {
        long targetHasPrimary = jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY_IDENTIFIER")
                        + " WHERE PARTY_ID=:targetPartyId AND IS_PRIMARY='Y'")
                .param("targetPartyId", targetPartyId).query(Long.class).single();
        String sql = """
                UPDATE %s
                   SET PARTY_ID=:targetPartyId,
                       IS_PRIMARY=CASE WHEN :demotePrimary=1 AND IS_PRIMARY='Y' THEN 'N' ELSE IS_PRIMARY END,
                       UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                 WHERE PARTY_ID=:sourcePartyId
                   AND IS_ACTIVE='Y'
                   AND VALID_FROM <= TRUNC(SYSDATE)
                   AND (VALID_TO IS NULL OR VALID_TO >= TRUNC(SYSDATE))
                """.formatted(table("PARTY_IDENTIFIER"));
        return jdbc.sql(sql).param("targetPartyId", targetPartyId).param("sourcePartyId", sourcePartyId)
                .param("demotePrimary", targetHasPrimary > 0 ? 1 : 0).param("actor", actor).update();
    }

    /**
     * Moves currently-valid classifications to the canonical merge target. Rows already represented by the
     * target with the same unique business period are intentionally retained on the source to avoid violating
     * UQ_CLASS_PERIOD; the target already contains the equivalent classification.
     */
    public int transferValidClassifications(long sourcePartyId, long targetPartyId, String actor) {
        String classifications = table("PARTY_CLASSIFICATION");
        String sql = """
                UPDATE %s src
                   SET PARTY_ID=:targetPartyId, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                       RECORD_VERSION=RECORD_VERSION+1
                 WHERE PARTY_ID=:sourcePartyId
                   AND VALID_FROM <= TRUNC(SYSDATE)
                   AND (VALID_TO IS NULL OR VALID_TO >= TRUNC(SYSDATE))
                   AND NOT EXISTS (
                       SELECT 1 FROM %s target
                        WHERE target.PARTY_ID=:targetPartyId
                          AND target.CLASSIFICATION_TYPE_CODE=src.CLASSIFICATION_TYPE_CODE
                          AND target.CLASSIFICATION_VALUE_CODE=src.CLASSIFICATION_VALUE_CODE
                          AND target.VALID_FROM=src.VALID_FROM
                   )
                """.formatted(classifications, classifications);
        return jdbc.sql(sql).param("targetPartyId", targetPartyId).param("sourcePartyId", sourcePartyId)
                .param("actor", actor).update();
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


    public long insertRelationship(long partyId, PartyRelationshipRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_RELATIONSHIP");
        String sql = """
                INSERT INTO %s (
                    PARTY_RELATIONSHIP_ID, PARTY_ID, RELATED_PARTY_ID, RELATIONSHIP_TYPE_CODE, OWNERSHIP_PERCENT,
                    POSITION_TITLE, SIGNING_RIGHT_CODE, AUTHORITY_LIMIT_AMOUNT, START_DATE, END_DATE,
                    EVIDENCE_DOCUMENT_ID, VERIFICATION_STATUS_CODE, CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :relatedPartyId, :relationshipTypeCode, :ownershipPercent,
                    :positionTitle, :signingRightCode, :authorityLimitAmount, :startDate, :endDate,
                    :evidenceDocumentId, :verificationStatusCode, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("PARTY_RELATIONSHIP"));
        bindRelationship(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateRelationship(long partyId, long id, PartyRelationshipRequest r, String actor) {
        String sql = """
                UPDATE %s SET RELATED_PARTY_ID=:relatedPartyId, RELATIONSHIP_TYPE_CODE=:relationshipTypeCode,
                    OWNERSHIP_PERCENT=:ownershipPercent, POSITION_TITLE=:positionTitle, SIGNING_RIGHT_CODE=:signingRightCode,
                    AUTHORITY_LIMIT_AMOUNT=:authorityLimitAmount, START_DATE=:startDate, END_DATE=:endDate,
                    EVIDENCE_DOCUMENT_ID=:evidenceDocumentId, VERIFICATION_STATUS_CODE=:verificationStatusCode,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_RELATIONSHIP_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_RELATIONSHIP"));
        return bindRelationship(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindRelationship(JdbcClient.StatementSpec spec, PartyRelationshipRequest r, String actor) {
        return spec.param("relatedPartyId", r.relatedPartyId()).param("relationshipTypeCode", r.relationshipTypeCode())
                .param("ownershipPercent", r.ownershipPercent()).param("positionTitle", r.positionTitle())
                .param("signingRightCode", r.signingRightCode()).param("authorityLimitAmount", r.authorityLimitAmount())
                .param("startDate", sqlDate(r.startDate())).param("endDate", sqlDate(r.endDate()))
                .param("evidenceDocumentId", r.evidenceDocumentId()).param("verificationStatusCode", r.verificationStatusCode())
                .param("actor", actor);
    }

    public long insertBeneficialOwnership(long partyId, BeneficialOwnershipRequest r, String actor) {
        long id = nextVal("SEQ_BENEFICIAL_OWNERSHIP");
        String sql = """
                INSERT INTO %s (
                    OWNERSHIP_ID, LEGAL_PARTY_ID, BENEFICIAL_OWNER_PARTY_ID, DIRECT_OWNERSHIP_PERCENT,
                    INDIRECT_OWNERSHIP_PERCENT, CONTROL_PERCENT, CONTROL_BASIS_CODE, IS_ULTIMATE_OWNER,
                    OWNERSHIP_PATH, VALID_FROM, VALID_TO, EVIDENCE_REF, CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :ownerPartyId, :directPercent, :indirectPercent, :controlPercent, :controlBasisCode,
                    :ultimateOwner, :ownershipPath, :validFrom, :validTo, :evidenceRef, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("BENEFICIAL_OWNERSHIP"));
        bindBeneficialOwnership(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateBeneficialOwnership(long partyId, long id, BeneficialOwnershipRequest r, String actor) {
        String sql = """
                UPDATE %s SET BENEFICIAL_OWNER_PARTY_ID=:ownerPartyId, DIRECT_OWNERSHIP_PERCENT=:directPercent,
                    INDIRECT_OWNERSHIP_PERCENT=:indirectPercent, CONTROL_PERCENT=:controlPercent,
                    CONTROL_BASIS_CODE=:controlBasisCode, IS_ULTIMATE_OWNER=:ultimateOwner,
                    OWNERSHIP_PATH=:ownershipPath, VALID_FROM=:validFrom, VALID_TO=:validTo, EVIDENCE_REF=:evidenceRef,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE OWNERSHIP_ID=:id AND LEGAL_PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("BENEFICIAL_OWNERSHIP"));
        return bindBeneficialOwnership(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindBeneficialOwnership(JdbcClient.StatementSpec spec, BeneficialOwnershipRequest r, String actor) {
        return spec.param("ownerPartyId", r.beneficialOwnerPartyId()).param("directPercent", r.directOwnershipPercent())
                .param("indirectPercent", r.indirectOwnershipPercent()).param("controlPercent", r.controlPercent())
                .param("controlBasisCode", r.controlBasisCode()).param("ultimateOwner", r.isUltimateOwner())
                .param("ownershipPath", r.ownershipPath()).param("validFrom", sqlDate(r.validFrom()))
                .param("validTo", sqlDate(r.validTo())).param("evidenceRef", r.evidenceRef()).param("actor", actor);
    }

    public long insertAuthority(long partyId, PartyAuthorityRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_AUTHORITY");
        String sql = """
                INSERT INTO %s (
                    AUTHORITY_ID, PRINCIPAL_PARTY_ID, AUTHORIZED_PARTY_ID, AUTHORITY_TYPE_CODE, SCOPE_CODE,
                    MAX_AMOUNT, CURRENCY_CODE, VALID_FROM, VALID_TO, DOCUMENT_REF, PARTY_ID,
                    CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :authorizedPartyId, :authorityTypeCode, :scopeCode, :maxAmount, :currencyCode,
                    :validFrom, :validTo, :documentRef, :authorizedPartyId, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("PARTY_AUTHORITY"));
        bindAuthority(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateAuthority(long partyId, long id, PartyAuthorityRequest r, String actor) {
        String sql = """
                UPDATE %s SET PRINCIPAL_PARTY_ID=:partyId, AUTHORIZED_PARTY_ID=:authorizedPartyId,
                    AUTHORITY_TYPE_CODE=:authorityTypeCode, SCOPE_CODE=:scopeCode, MAX_AMOUNT=:maxAmount,
                    CURRENCY_CODE=:currencyCode, VALID_FROM=:validFrom, VALID_TO=:validTo, DOCUMENT_REF=:documentRef,
                    PARTY_ID=:authorizedPartyId, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE AUTHORITY_ID=:id AND PRINCIPAL_PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_AUTHORITY"));
        return bindAuthority(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindAuthority(JdbcClient.StatementSpec spec, PartyAuthorityRequest r, String actor) {
        return spec.param("authorizedPartyId", r.authorizedPartyId()).param("authorityTypeCode", r.authorityTypeCode())
                .param("scopeCode", r.scopeCode()).param("maxAmount", r.maxAmount()).param("currencyCode", r.currencyCode())
                .param("validFrom", sqlDate(r.validFrom())).param("validTo", sqlDate(r.validTo()))
                .param("documentRef", r.documentRef()).param("actor", actor);
    }

    public long insertRole(long partyId, PartyRoleRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_ROLE");
        String sql = """
                INSERT INTO %s (
                    PARTY_ROLE_ID, PARTY_ID, ROLE_TYPE_CODE, CONTEXT_TYPE_CODE, CONTEXT_ID, VALID_FROM, VALID_TO,
                    STATUS_CODE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, RECORD_VERSION,
                    PRINCIPAL_PARTY_ID, RELATIONSHIP_TYPE_CODE, AUTHORITY_BASIS_CODE, AUTHORITY_DOCUMENT_NO,
                    AUTHORITY_ISSUER, AUTHORITY_SCOPE_TEXT, ASSIGNMENT_REASON_TEXT, DESCRIPTION_TEXT
                ) VALUES (
                    :id, :partyId, :roleType, :contextType, :contextId, :validFrom, :validTo, :statusCode,
                    SYSTIMESTAMP, :actor, NULL, NULL, 1, :principalPartyId, :relationshipType, :authorityBasis,
                    :authorityDocumentNo, :authorityIssuer, :authorityScope, :assignmentReason, :description
                )
                """.formatted(table("PARTY_ROLE"));
        bindRole(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateRole(long partyId, long roleId, PartyRoleRequest r, String actor) {
        String sql = """
                UPDATE %s SET ROLE_TYPE_CODE=:roleType, CONTEXT_TYPE_CODE=:contextType, CONTEXT_ID=:contextId,
                    VALID_FROM=:validFrom, VALID_TO=:validTo, STATUS_CODE=:statusCode, PRINCIPAL_PARTY_ID=:principalPartyId,
                    RELATIONSHIP_TYPE_CODE=:relationshipType, AUTHORITY_BASIS_CODE=:authorityBasis,
                    AUTHORITY_DOCUMENT_NO=:authorityDocumentNo, AUTHORITY_ISSUER=:authorityIssuer,
                    AUTHORITY_SCOPE_TEXT=:authorityScope, ASSIGNMENT_REASON_TEXT=:assignmentReason,
                    DESCRIPTION_TEXT=:description, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                    RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_ROLE_ID=:roleId AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_ROLE"));
        return bindRole(jdbc.sql(sql).param("roleId", roleId).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindRole(JdbcClient.StatementSpec spec, PartyRoleRequest r, String actor) {
        return spec.param("roleType", r.roleTypeCode()).param("contextType", r.contextTypeCode())
                .param("contextId", r.contextId()).param("validFrom", sqlDate(r.validFrom())).param("validTo", sqlDate(r.validTo()))
                .param("statusCode", r.statusCode()).param("principalPartyId", r.principalPartyId())
                .param("relationshipType", r.relationshipTypeCode()).param("authorityBasis", r.authorityBasisCode())
                .param("authorityDocumentNo", r.authorityDocumentNo()).param("authorityIssuer", r.authorityIssuer())
                .param("authorityScope", r.authorityScopeText()).param("assignmentReason", r.assignmentReasonText())
                .param("description", r.descriptionText()).param("actor", actor);
    }

    public int deleteRole(long partyId, long roleId) {
        return deleteChild("PARTY_ROLE", "PARTY_ROLE_ID", partyId, roleId);
    }

    public boolean roleReferenceExists(String roleType, String contextType, String statusCode) {
        long role = jdbc.sql("SELECT COUNT(*) FROM " + table("REF_ROLE_TYPE") + " WHERE ROLE_TYPE_CODE=:code AND IS_ACTIVE=1")
                .param("code", roleType).query(Long.class).single();
        long status = jdbc.sql("SELECT COUNT(*) FROM " + table("REF_WORKFLOW_STATUS") + " WHERE STATUS_CODE=:code AND IS_ACTIVE=1")
                .param("code", statusCode).query(Long.class).single();
        if (role == 0 || status == 0) return false;
        if (contextType == null) return true;
        long context = jdbc.sql("SELECT COUNT(*) FROM " + table("REF_CONTEXT_TYPE") + " WHERE CONTEXT_TYPE_CODE=:code AND IS_ACTIVE=1")
                .param("code", contextType).query(Long.class).single();
        return context > 0;
    }

    public boolean roleDuplicateExists(long partyId, String roleType, String contextType, String contextId, LocalDate validFrom, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("PARTY_ROLE") +
                " WHERE PARTY_ID=:partyId AND ROLE_TYPE_CODE=:roleType AND NVL(CONTEXT_TYPE_CODE,'#')=NVL(:contextType,'#')" +
                " AND NVL(CONTEXT_ID,'#')=NVL(:contextId,'#') AND VALID_FROM=:validFrom" +
                (exceptId == null ? "" : " AND PARTY_ROLE_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("partyId", partyId).param("roleType", roleType)
                .param("contextType", contextType).param("contextId", contextId).param("validFrom", sqlDate(validFrom));
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public boolean currentCustomerExists(long partyId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY_CUSTOMER") + " WHERE PARTY_ID=:partyId AND IS_CURRENT='Y'")
                .param("partyId", partyId).query(Long.class).single() > 0;
    }

    public boolean currentCustomerExistsForOtherRole(long partyId, long roleId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY_CUSTOMER") +
                        " WHERE PARTY_ID=:partyId AND IS_CURRENT='Y' AND PARTY_ROLE_ID<>:roleId")
                .param("partyId", partyId).param("roleId", roleId).query(Long.class).single() > 0;
    }

    public boolean customerForRoleExists(long partyId, long roleId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY_CUSTOMER") + " WHERE PARTY_ID=:partyId AND PARTY_ROLE_ID=:roleId")
                .param("partyId", partyId).param("roleId", roleId).query(Long.class).single() > 0;
    }

    public long insertCustomerForRole(long partyId, long roleId, PartyRoleRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_CUSTOMER");
        String customerNo = jdbc.sql("SELECT TO_CHAR(" + schema + ".SEQ_CUSTOMER_NO.NEXTVAL, 'FM00000000') FROM DUAL")
                .query(String.class).single();
        String current = customerCurrentFlag(r.statusCode(), r.validFrom(), r.validTo());
        String sql = """
                INSERT INTO %s (PARTY_CUSTOMER_ID, PARTY_ID, PARTY_ROLE_ID, CUSTOMER_NO, CUSTOMER_STATUS_CODE,
                    VALID_FROM, VALID_TO, IS_CURRENT, CREATED_AT, CREATED_BY, RECORD_VERSION)
                VALUES (:id, :partyId, :roleId, :customerNo, :statusCode, :validFrom, :validTo, :isCurrent,
                    SYSTIMESTAMP, :actor, 1)
                """.formatted(table("PARTY_CUSTOMER"));
        jdbc.sql(sql).param("id", id).param("partyId", partyId).param("roleId", roleId).param("customerNo", customerNo)
                .param("statusCode", r.statusCode()).param("validFrom", sqlDate(r.validFrom())).param("validTo", sqlDate(r.validTo()))
                .param("isCurrent", current).param("actor", actor).update();
        return id;
    }

    public int updateCustomerForRole(long partyId, long roleId, PartyRoleRequest r, String actor) {
        String sql = """
                UPDATE %s SET CUSTOMER_STATUS_CODE=:statusCode, VALID_FROM=:validFrom, VALID_TO=:validTo,
                    IS_CURRENT=:isCurrent, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_ID=:partyId AND PARTY_ROLE_ID=:roleId
                """.formatted(table("PARTY_CUSTOMER"));
        return jdbc.sql(sql).param("statusCode", r.statusCode()).param("validFrom", sqlDate(r.validFrom()))
                .param("validTo", sqlDate(r.validTo())).param("isCurrent", customerCurrentFlag(r.statusCode(), r.validFrom(), r.validTo()))
                .param("actor", actor).param("partyId", partyId).param("roleId", roleId).update();
    }

    private static String customerCurrentFlag(String statusCode, LocalDate validFrom, LocalDate validTo) {
        LocalDate today = LocalDate.now();
        if (Set.of("CLOSED", "REVOKED", "EXPIRED", "INACTIVE").contains(statusCode)) return "N";
        if (validFrom != null && validFrom.isAfter(today)) return "N";
        if (validTo != null && validTo.isBefore(today)) return "N";
        return "Y";
    }

    public long insertClassification(long partyId, PartyClassificationRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_CLASSIFICATION");
        String sql = """
                INSERT INTO %s (
                    PARTY_CLASSIFICATION_ID, PARTY_ID, CLASSIFICATION_TYPE_CODE, CLASSIFICATION_VALUE_CODE,
                    ASSIGNMENT_REASON_CODE, VALID_FROM, VALID_TO, CREATED_AT, CREATED_BY, RECORD_VERSION, DESCRIPTION_TEXT
                ) VALUES (
                    :id, :partyId, :type, :value, :reason, :validFrom, :validTo, SYSTIMESTAMP, :actor, 1, :description
                )
                """.formatted(table("PARTY_CLASSIFICATION"));
        bindClassification(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateClassification(long partyId, long id, PartyClassificationRequest r, String actor) {
        String sql = """
                UPDATE %s SET CLASSIFICATION_TYPE_CODE=:type, CLASSIFICATION_VALUE_CODE=:value,
                    ASSIGNMENT_REASON_CODE=:reason, VALID_FROM=:validFrom, VALID_TO=:validTo,
                    DESCRIPTION_TEXT=:description, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                    RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_CLASSIFICATION_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_CLASSIFICATION"));
        return bindClassification(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindClassification(JdbcClient.StatementSpec spec, PartyClassificationRequest r, String actor) {
        return spec.param("type", r.classificationTypeCode()).param("value", r.classificationValueCode())
                .param("reason", r.assignmentReasonCode()).param("validFrom", sqlDate(r.validFrom()))
                .param("validTo", sqlDate(r.validTo())).param("description", r.descriptionText()).param("actor", actor);
    }

    public List<LookupOption> classificationValueLookup(String typeCode, String text, int limit) {
        StringBuilder sql = new StringBuilder("SELECT CLASSIFICATION_VALUE_CODE AS CODE_VALUE, NAME_FA AS LABEL_VALUE FROM ")
                .append(table("REF_CLASSIFICATION_VALUE"))
                .append(" WHERE CLASSIFICATION_TYPE_CODE=:typeCode AND IS_ACTIVE=1");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("typeCode", typeCode);
        if (text != null && !text.isBlank()) {
            sql.append(" AND (UPPER(CLASSIFICATION_VALUE_CODE) LIKE :text OR UPPER(NAME_FA) LIKE :text)");
            params.put("text", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        sql.append(" ORDER BY SORT_ORDER, NAME_FA FETCH FIRST :limit ROWS ONLY");
        params.put("limit", Math.min(Math.max(limit, 1), 200));
        return jdbc.sql(sql.toString()).params(params).query((rs, rowNum) -> {
            String code = rs.getString("CODE_VALUE");
            return new LookupOption(code, code, rs.getString("LABEL_VALUE"));
        }).list();
    }

    public boolean classificationReferenceExists(String typeCode, String valueCode, String reasonCode) {
        Long typeCount = jdbc.sql("SELECT COUNT(*) FROM " + table("REF_CLASSIFICATION_TYPE") + " WHERE CLASSIFICATION_TYPE_CODE=:code AND IS_ACTIVE=1")
                .param("code", typeCode).query(Long.class).single();
        if (typeCount == 0) return false;
        Long valueCount = jdbc.sql("SELECT COUNT(*) FROM " + table("REF_CLASSIFICATION_VALUE") + " WHERE CLASSIFICATION_TYPE_CODE=:type AND CLASSIFICATION_VALUE_CODE=:value AND IS_ACTIVE=1")
                .param("type", typeCode).param("value", valueCode).query(Long.class).single();
        if (valueCount == 0) return false;
        Long reasonCount = jdbc.sql("SELECT COUNT(*) FROM " + table("REF_ASSIGNMENT_REASON") + " WHERE ASSIGNMENT_REASON_CODE=:code AND IS_ACTIVE=1")
                .param("code", reasonCode).query(Long.class).single();
        return reasonCount > 0;
    }

    public boolean classificationDuplicateExists(long partyId, String typeCode, String valueCode, LocalDate validFrom, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("PARTY_CLASSIFICATION")
                + " WHERE PARTY_ID=:partyId AND CLASSIFICATION_TYPE_CODE=:type AND CLASSIFICATION_VALUE_CODE=:value"
                + " AND VALID_FROM=:validFrom" + (exceptId == null ? "" : " AND PARTY_CLASSIFICATION_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("partyId", partyId).param("type", typeCode)
                .param("value", valueCode).param("validFrom", sqlDate(validFrom));
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public long insertKycCase(long partyId, KycCaseRequest r, String actor) {
        long id = nextVal("SEQ_KYC_CASE");
        String sql = """
                INSERT INTO %s (
                    KYC_CASE_ID, PARTY_ID, KYC_TYPE_CODE, DUE_DILIGENCE_LEVEL_CODE, STATUS_CODE,
                    OPENED_AT, COMPLETED_AT, REVIEWED_AT, NEXT_REVIEW_DATE, FINAL_RISK_LEVEL_CODE,
                    DECISION_CODE, DECISION_REASON, APPROVED_BY, CREATED_AT, CREATED_BY, RECORD_VERSION,
                    RELATION_PURPOSE_CODE, EXPECTED_ACTIVITY_LEVEL_CODE, GEOGRAPHIC_SCOPE_CODE,
                    ACTIVITY_COUNTRIES_TEXT, REQUESTED_PRODUCTS_TEXT, PREFERRED_SERVICE_CHANNEL_CODE,
                    PEP_STATUS_CODE, HIGH_RISK_COUNTRY_FLAG, EDD_REQUIRED_FLAG
                ) VALUES (
                    :id, :partyId, :kycType, :dd, :status, :openedAt, :completedAt, :reviewedAt,
                    :nextReview, :riskLevel, :decision, :reason, :approvedBy, SYSTIMESTAMP, :actor, 1,
                    :relationPurpose, :expectedActivity, :geographicScope, :activityCountries, :requestedProducts,
                    :preferredChannel, :pepStatus, :highRiskCountry, :eddRequired
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
                    DECISION_REASON=:reason, APPROVED_BY=:approvedBy, RELATION_PURPOSE_CODE=:relationPurpose,
                    EXPECTED_ACTIVITY_LEVEL_CODE=:expectedActivity, GEOGRAPHIC_SCOPE_CODE=:geographicScope,
                    ACTIVITY_COUNTRIES_TEXT=:activityCountries, REQUESTED_PRODUCTS_TEXT=:requestedProducts,
                    PREFERRED_SERVICE_CHANNEL_CODE=:preferredChannel, PEP_STATUS_CODE=:pepStatus,
                    HIGH_RISK_COUNTRY_FLAG=:highRiskCountry, EDD_REQUIRED_FLAG=:eddRequired, UPDATED_AT=SYSTIMESTAMP,
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
                .param("approvedBy", r.approvedBy()).param("relationPurpose", r.relationPurposeCode())
                .param("expectedActivity", r.expectedActivityLevelCode()).param("geographicScope", r.geographicScopeCode())
                .param("activityCountries", r.activityCountriesText()).param("requestedProducts", r.requestedProductsText())
                .param("preferredChannel", r.preferredServiceChannelCode()).param("pepStatus", r.pepStatusCode())
                .param("highRiskCountry", r.highRiskCountryFlag()).param("eddRequired", r.eddRequiredFlag())
                .param("actor", actor);
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

    public int deleteClassification(long partyId, long id) {
        return deleteChild("PARTY_CLASSIFICATION", "PARTY_CLASSIFICATION_ID", partyId, id);
    }

    public int deleteRelationship(long partyId, long id) {
        return deleteChild("PARTY_RELATIONSHIP", "PARTY_RELATIONSHIP_ID", partyId, id);
    }

    public int deleteBeneficialOwnership(long partyId, long id) {
        return jdbc.sql("DELETE FROM " + table("BENEFICIAL_OWNERSHIP") + " WHERE OWNERSHIP_ID=:id AND LEGAL_PARTY_ID=:partyId")
                .param("id", id).param("partyId", partyId).update();
    }

    public int deleteAuthority(long partyId, long id) {
        return jdbc.sql("DELETE FROM " + table("PARTY_AUTHORITY") + " WHERE AUTHORITY_ID=:id AND PRINCIPAL_PARTY_ID=:partyId")
                .param("id", id).param("partyId", partyId).update();
    }

    public long insertExternalInquiry(long partyId, ExternalInquiryRequest r, String actor) {
        long id = nextVal("SEQ_EXTERNAL_INQUIRY_RESULT");
        String sql = """
                INSERT INTO %s (INQUIRY_RESULT_ID, PARTY_ID, INQUIRY_TYPE_CODE, PROVIDER_CODE, REFERENCE_NO,
                    INQUIRY_RESULT_CODE, REQUESTED_AT, RESPONDED_AT, EXPIRY_AT, PAYLOAD_REF, PAYLOAD_HASH,
                    CREATED_AT, CREATED_BY, RECORD_VERSION)
                VALUES (:id, :partyId, :type, :provider, :referenceNo, :result, :requestedAt, :respondedAt,
                    :expiryAt, :payloadRef, :payloadHash, SYSTIMESTAMP, :actor, 1)
                """.formatted(table("EXTERNAL_INQUIRY_RESULT"));
        bindExternalInquiry(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateExternalInquiry(long partyId, long id, ExternalInquiryRequest r, String actor) {
        String sql = """
                UPDATE %s SET INQUIRY_TYPE_CODE=:type, PROVIDER_CODE=:provider, REFERENCE_NO=:referenceNo,
                    INQUIRY_RESULT_CODE=:result, REQUESTED_AT=:requestedAt, RESPONDED_AT=:respondedAt,
                    EXPIRY_AT=:expiryAt, PAYLOAD_REF=:payloadRef, PAYLOAD_HASH=:payloadHash,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE INQUIRY_RESULT_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("EXTERNAL_INQUIRY_RESULT"));
        return bindExternalInquiry(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindExternalInquiry(JdbcClient.StatementSpec spec, ExternalInquiryRequest r, String actor) {
        return spec.param("type", r.inquiryTypeCode()).param("provider", r.providerCode())
                .param("referenceNo", r.referenceNo()).param("result", r.inquiryResultCode())
                .param("requestedAt", sqlTimestamp(r.requestedAt())).param("respondedAt", sqlTimestamp(r.respondedAt()))
                .param("expiryAt", sqlTimestamp(r.expiryAt())).param("payloadRef", r.payloadRef())
                .param("payloadHash", r.payloadHash()).param("actor", actor);
    }

    public int deleteExternalInquiry(long partyId, long id) {
        return deleteChild("EXTERNAL_INQUIRY_RESULT", "INQUIRY_RESULT_ID", partyId, id);
    }

    public long insertConsent(long partyId, PartyConsentRequest r, String statusCode, String actor) {
        long id = nextVal("SEQ_PARTY_CONSENT");
        String sql = """
                INSERT INTO %s (CONSENT_ID, PARTY_ID, CONSENT_TYPE_CODE, PURPOSE_CODE, CONSENT_STATUS_CODE,
                    GRANTED_AT, REVOKED_AT, EVIDENCE_REF, CREATED_AT, CREATED_BY, SOURCE_CODE, RECORD_VERSION,
                    CUSTOMER_DECISION_CODE, CAPTURE_CHANNEL_CODE, DECLARED_AT, VALID_TO, CONSENT_TEXT_VERSION_CODE,
                    SCOPE_TEXT, SCOPE_LIMITATION_TEXT)
                VALUES (:id, :partyId, :type, :purpose, :status, :grantedAt, NULL, :evidence, SYSTIMESTAMP, :actor,
                    :source, 1, :decision, :captureChannel, :declaredAt, :validTo, :textVersion, :scopeText, :limitation)
                """.formatted(table("PARTY_CONSENT"));
        bindConsent(jdbc.sql(sql).param("id", id).param("partyId", partyId).param("status", statusCode), r, actor).update();
        return id;
    }

    public int updateConsent(long partyId, long id, PartyConsentRequest r, String statusCode, String actor) {
        String sql = """
                UPDATE %s SET CONSENT_TYPE_CODE=:type, PURPOSE_CODE=:purpose, CONSENT_STATUS_CODE=:status,
                    GRANTED_AT=:grantedAt, EVIDENCE_REF=:evidence, SOURCE_CODE=:source, CUSTOMER_DECISION_CODE=:decision,
                    CAPTURE_CHANNEL_CODE=:captureChannel, DECLARED_AT=:declaredAt, VALID_TO=:validTo,
                    CONSENT_TEXT_VERSION_CODE=:textVersion, SCOPE_TEXT=:scopeText, SCOPE_LIMITATION_TEXT=:limitation,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE CONSENT_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                  AND CONSENT_STATUS_CODE <> 'REVOKED'
                """.formatted(table("PARTY_CONSENT"));
        return bindConsent(jdbc.sql(sql).param("id", id).param("partyId", partyId).param("status", statusCode), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindConsent(JdbcClient.StatementSpec spec, PartyConsentRequest r, String actor) {
        return spec.param("type", r.consentTypeCode()).param("purpose", r.purposeCode())
                .param("grantedAt", sqlTimestamp(r.declaredAt())).param("evidence", r.evidenceRef())
                .param("source", r.sourceCode()).param("decision", r.customerDecisionCode())
                .param("captureChannel", r.captureChannelCode()).param("declaredAt", sqlTimestamp(r.declaredAt()))
                .param("validTo", sqlDate(r.validTo())).param("textVersion", r.consentTextVersionCode())
                .param("scopeText", r.scopeText()).param("limitation", r.scopeLimitationText()).param("actor", actor);
    }

    public int revokeConsent(long partyId, long id, long recordVersion, String actor) {
        return jdbc.sql("UPDATE " + table("PARTY_CONSENT") +
                        " SET CONSENT_STATUS_CODE='REVOKED', REVOKED_AT=SYSTIMESTAMP, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1" +
                        " WHERE CONSENT_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion AND CONSENT_STATUS_CODE='GRANTED'")
                .param("actor", actor).param("id", id).param("partyId", partyId).param("recordVersion", recordVersion).update();
    }

    public boolean consentDuplicateExists(long partyId, String typeCode, String purposeCode, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("PARTY_CONSENT") +
                " WHERE PARTY_ID=:partyId AND CONSENT_TYPE_CODE=:type AND PURPOSE_CODE=:purpose AND CONSENT_STATUS_CODE='GRANTED'" +
                " AND (VALID_TO IS NULL OR VALID_TO >= TRUNC(SYSDATE))" +
                (exceptId == null ? "" : " AND CONSENT_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("partyId", partyId).param("type", typeCode).param("purpose", purposeCode);
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public long insertCommunicationPreference(long partyId, CommunicationPreferenceRequest r, String actor) {
        long id = nextVal("SEQ_COMMUNICATION_PREFERENCE");
        String sql = """
                INSERT INTO %s (PREFERENCE_ID, PARTY_ID, CHANNEL_CODE, PURPOSE_CODE, ALLOWED_FLAG, PREFERRED_TIME_FROM,
                    PREFERRED_TIME_TO, LANGUAGE_CODE, CREATED_AT, CREATED_BY, RECORD_VERSION, ALLOWED_DAYS_CODE,
                    TIME_ZONE_CODE, MARKETING_OPT_OUT_FLAG)
                VALUES (:id, :partyId, :channel, :purpose, :allowed, :timeFrom, :timeTo, :language, SYSTIMESTAMP, :actor, 1,
                    :allowedDays, :timeZone, :marketingOptOut)
                """.formatted(table("COMMUNICATION_PREFERENCE"));
        bindCommunicationPreference(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateCommunicationPreference(long partyId, long id, CommunicationPreferenceRequest r, String actor) {
        String sql = """
                UPDATE %s SET CHANNEL_CODE=:channel, PURPOSE_CODE=:purpose, ALLOWED_FLAG=:allowed,
                    PREFERRED_TIME_FROM=:timeFrom, PREFERRED_TIME_TO=:timeTo, LANGUAGE_CODE=:language,
                    ALLOWED_DAYS_CODE=:allowedDays, TIME_ZONE_CODE=:timeZone, MARKETING_OPT_OUT_FLAG=:marketingOptOut,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PREFERENCE_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("COMMUNICATION_PREFERENCE"));
        return bindCommunicationPreference(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindCommunicationPreference(JdbcClient.StatementSpec spec, CommunicationPreferenceRequest r, String actor) {
        return spec.param("channel", r.channelCode()).param("purpose", r.purposeCode()).param("allowed", r.allowedFlag())
                .param("timeFrom", r.preferredTimeFrom()).param("timeTo", r.preferredTimeTo()).param("language", r.languageCode())
                .param("allowedDays", r.allowedDaysCode()).param("timeZone", r.timeZoneCode())
                .param("marketingOptOut", r.marketingOptOutFlag()).param("actor", actor);
    }

    public boolean communicationPreferenceDuplicateExists(long partyId, String channelCode, String purposeCode, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("COMMUNICATION_PREFERENCE") +
                " WHERE PARTY_ID=:partyId AND CHANNEL_CODE=:channel AND PURPOSE_CODE=:purpose" +
                (exceptId == null ? "" : " AND PREFERENCE_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("partyId", partyId).param("channel", channelCode).param("purpose", purposeCode);
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public int deleteCommunicationPreference(long partyId, long id) {
        return deleteChild("COMMUNICATION_PREFERENCE", "PREFERENCE_ID", partyId, id);
    }

    public long insertGeneralPreference(long partyId, PartyGeneralPreferenceRequest r, String actor) {
        long id = nextVal("SEQ_PARTY_GENERAL_PREFERENCE");
        String sql = """
                INSERT INTO %s (PREFERENCE_ID, PARTY_ID, PREFERENCE_TYPE_CODE, PREFERENCE_VALUE, VALID_FROM, VALID_TO,
                    SOURCE_CODE, CREATED_AT, CREATED_BY, RECORD_VERSION)
                VALUES (:id, :partyId, :type, :value, :validFrom, :validTo, :source, SYSTIMESTAMP, :actor, 1)
                """.formatted(table("PARTY_GENERAL_PREFERENCE"));
        bindGeneralPreference(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateGeneralPreference(long partyId, long id, PartyGeneralPreferenceRequest r, String actor) {
        String sql = """
                UPDATE %s SET PREFERENCE_TYPE_CODE=:type, PREFERENCE_VALUE=:value, VALID_FROM=:validFrom, VALID_TO=:validTo,
                    SOURCE_CODE=:source, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE PREFERENCE_ID=:id AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_GENERAL_PREFERENCE"));
        return bindGeneralPreference(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor)
                .param("recordVersion", r.recordVersion()).update();
    }

    private JdbcClient.StatementSpec bindGeneralPreference(JdbcClient.StatementSpec spec, PartyGeneralPreferenceRequest r, String actor) {
        return spec.param("type", r.preferenceTypeCode()).param("value", r.preferenceValue())
                .param("validFrom", sqlTimestamp(r.validFrom())).param("validTo", sqlTimestamp(r.validTo()))
                .param("source", r.sourceCode()).param("actor", actor);
    }

    public boolean generalPreferenceOverlapExists(long partyId, String typeCode, LocalDateTime validFrom, LocalDateTime validTo, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("PARTY_GENERAL_PREFERENCE") +
                " WHERE PARTY_ID=:partyId AND PREFERENCE_TYPE_CODE=:type" +
                " AND (VALID_TO IS NULL OR VALID_TO >= :validFrom)" +
                (validTo == null ? "" : " AND VALID_FROM <= :validTo") +
                (exceptId == null ? "" : " AND PREFERENCE_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("partyId", partyId).param("type", typeCode)
                .param("validFrom", sqlTimestamp(validFrom));
        if (validTo != null) spec = spec.param("validTo", sqlTimestamp(validTo));
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public int deleteGeneralPreference(long partyId, long id) {
        return deleteChild("PARTY_GENERAL_PREFERENCE", "PREFERENCE_ID", partyId, id);
    }

    public boolean activeReferenceCodeExists(String tableName, String codeColumn, String code) {
        if (!SQL_NAME.matcher(tableName).matches() || !SQL_NAME.matcher(codeColumn).matches()) {
            throw new IllegalArgumentException("Invalid reference metadata name");
        }
        return jdbc.sql("SELECT COUNT(*) FROM " + table(tableName)
                        + " WHERE " + codeColumn + "=:code AND IS_ACTIVE=1"
                        + " AND (VALID_FROM IS NULL OR VALID_FROM <= TRUNC(SYSDATE))"
                        + " AND (VALID_TO IS NULL OR VALID_TO >= TRUNC(SYSDATE))")
                .param("code", code).query(Long.class).single() > 0;
    }

    public boolean kycCaseHasDependents(long partyId, long id) {
        String sql = """
                SELECT (
                    (SELECT COUNT(*) FROM %s WHERE PARTY_ID=:partyId AND KYC_CASE_ID=:id) +
                    (SELECT COUNT(*) FROM %s WHERE PARTY_ID=:partyId AND KYC_CASE_ID=:id) +
                    (SELECT COUNT(*) FROM %s WHERE PARTY_ID=:partyId AND KYC_CASE_ID=:id)
                )
                FROM DUAL
                """.formatted(table("PARTY_RISK_ASSESSMENT"), table("SCREENING_RESULT"), table("PARTY_DOCUMENT"));
        return jdbc.sql(sql).param("partyId", partyId).param("id", id).query(Long.class).single() > 0;
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


    public boolean partyIsOrganization(long partyId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY") + " WHERE PARTY_ID=:partyId AND PARTY_TYPE_CODE='ORGANIZATION'")
                .param("partyId", partyId).query(Long.class).single() > 0;
    }

    public boolean documentBelongsToParty(long partyId, Long documentId) {
        if (documentId == null) return true;
        return jdbc.sql("SELECT COUNT(*) FROM " + table("PARTY_DOCUMENT") + " WHERE DOCUMENT_ID=:id AND PARTY_ID=:partyId")
                .param("id", documentId).param("partyId", partyId).query(Long.class).single() > 0;
    }

    public boolean relationshipDuplicateExists(long partyId, long relatedPartyId, String typeCode, LocalDate startDate, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("PARTY_RELATIONSHIP")
                + " WHERE PARTY_ID=:partyId AND RELATED_PARTY_ID=:relatedPartyId AND RELATIONSHIP_TYPE_CODE=:typeCode AND START_DATE=:startDate"
                + (exceptId == null ? "" : " AND PARTY_RELATIONSHIP_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("partyId", partyId).param("relatedPartyId", relatedPartyId)
                .param("typeCode", typeCode).param("startDate", sqlDate(startDate));
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public boolean beneficialOwnershipDuplicateExists(long partyId, long ownerPartyId, LocalDate validFrom, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("BENEFICIAL_OWNERSHIP")
                + " WHERE LEGAL_PARTY_ID=:partyId AND BENEFICIAL_OWNER_PARTY_ID=:ownerPartyId AND VALID_FROM=:validFrom"
                + (exceptId == null ? "" : " AND OWNERSHIP_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("partyId", partyId).param("ownerPartyId", ownerPartyId)
                .param("validFrom", sqlDate(validFrom));
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
    }

    public boolean authorityDuplicateExists(long partyId, long authorizedPartyId, String typeCode, String scopeCode, LocalDate validFrom, Long exceptId) {
        String sql = "SELECT COUNT(*) FROM " + table("PARTY_AUTHORITY")
                + " WHERE PRINCIPAL_PARTY_ID=:partyId AND AUTHORIZED_PARTY_ID=:authorizedPartyId AND AUTHORITY_TYPE_CODE=:typeCode"
                + " AND SCOPE_CODE=:scopeCode AND VALID_FROM=:validFrom"
                + (exceptId == null ? "" : " AND AUTHORITY_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("partyId", partyId).param("authorizedPartyId", authorizedPartyId)
                .param("typeCode", typeCode).param("scopeCode", scopeCode).param("validFrom", sqlDate(validFrom));
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        return spec.query(Long.class).single() > 0;
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
