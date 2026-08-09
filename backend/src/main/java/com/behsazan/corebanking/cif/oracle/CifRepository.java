package com.behsazan.corebanking.cif.oracle;

import com.behsazan.corebanking.cif.domain.CifModels.CifDashboardSummary;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointRecord;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointRequest;
import com.behsazan.corebanking.cif.domain.CifModels.CreatePartyRequest;
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
                       LIFE_STATUS_CODE, RECORD_VERSION
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
                        rs.getLong("RECORD_VERSION")
                )).optional();
    }

    public Optional<OrganizationProfile> findOrganization(long partyId) {
        String sql = """
                SELECT ORGANIZATION_ID, PARTY_ID, REGISTERED_NAME, TRADE_NAME, LEGAL_FORM_CODE,
                       REGISTRATION_NO, REGISTRATION_PLACE_CODE, INCORPORATION_DATE, DISSOLUTION_DATE,
                       ECONOMIC_SECTOR_CODE, ISIC_CODE, LISTED_COMPANY_FLAG, RECORD_VERSION
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
                       PA.IS_PRIMARY, PA.VALID_FROM, PA.VALID_TO, PA.RECORD_VERSION AS PA_RECORD_VERSION,
                       A.COUNTRY_CODE, A.PROVINCE_CODE, A.CITY_CODE, A.DISTRICT_CODE, A.POSTAL_CODE,
                       A.ADDRESS_LINE1, A.ADDRESS_LINE2, A.RECORD_VERSION AS ADDRESS_RECORD_VERSION
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
                        rs.getString("PROVINCE_CODE"), rs.getString("CITY_CODE"), rs.getString("DISTRICT_CODE"),
                        rs.getString("POSTAL_CODE"), rs.getString("ADDRESS_LINE1"), rs.getString("ADDRESS_LINE2"),
                        rs.getLong("PA_RECORD_VERSION"), rs.getLong("ADDRESS_RECORD_VERSION")
                )).list();
    }

    public List<ContactPointRecord> findContacts(long partyId) {
        String sql = """
                SELECT CONTACT_POINT_ID, PARTY_ID, CONTACT_TYPE_CODE, CONTACT_VALUE, NORMALIZED_VALUE,
                       PURPOSE_CODE, IS_PRIMARY, IS_VERIFIED, VERIFIED_AT, VALID_FROM, VALID_TO, RECORD_VERSION
                FROM %s WHERE PARTY_ID = :partyId
                ORDER BY CASE WHEN IS_PRIMARY = 'Y' THEN 0 ELSE 1 END, VALID_FROM DESC, CONTACT_POINT_ID DESC
                """.formatted(table("CONTACT_POINT"));
        return jdbc.sql(sql).param("partyId", partyId)
                .query((rs, rowNum) -> new ContactPointRecord(
                        rs.getLong("CONTACT_POINT_ID"), rs.getLong("PARTY_ID"), rs.getString("CONTACT_TYPE_CODE"),
                        rs.getString("CONTACT_VALUE"), rs.getString("NORMALIZED_VALUE"), rs.getString("PURPOSE_CODE"),
                        rs.getString("IS_PRIMARY"), rs.getString("IS_VERIFIED"), localDateTime(rs, "VERIFIED_AT"),
                        localDate(rs, "VALID_FROM"), localDate(rs, "VALID_TO"), rs.getLong("RECORD_VERSION")
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
                       CONTENT_HASH, STORAGE_REF, MIME_TYPE, RECORD_VERSION
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
                        rs.getLong("RECORD_VERSION")
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
                    PARTY_ID, PARTY_TYPE_CODE, LIFECYCLE_STATUS_CODE, VERIFICATION_STATUS_CODE,
                    DATA_QUALITY_STATUS_CODE, CREATION_SOURCE_CODE, VALID_FROM, IS_CURRENT, CREATED_BY
                ) VALUES (
                    :partyId, :partyType, :lifecycle, :verification, :dataQuality, :source,
                    TRUNC(SYSDATE), 'Y', :actor
                )
                """.formatted(table("PARTY"));
        jdbc.sql(sql)
                .param("partyId", id)
                .param("partyType", request.partyTypeCode())
                .param("lifecycle", request.lifecycleStatusCode())
                .param("verification", request.verificationStatusCode())
                .param("dataQuality", request.dataQualityStatusCode())
                .param("source", request.creationSourceCode())
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
                    CREATED_AT, CREATED_BY, RECORD_VERSION, LIFE_STATUS_CODE
                ) VALUES (
                    :partyId, :birthDate, :gender, :birthCountry, :birthPlaceId, :birthPlaceText,
                    :father, :mother, :marital, :deathDate, :legalCapacity, :language, :dataQuality,
                    :verification, :residence, :physicalAbility, SYSTIMESTAMP, :actor, 1, :lifeStatus
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
                    LIFE_STATUS_CODE=:lifeStatus, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
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
                .param("actor", actor);
    }

    public long insertOrganization(long partyId, OrganizationRequest request, String actor) {
        long id = nextVal("SEQ_ORGANIZATION");
        String sql = """
                INSERT INTO %s (
                    ORGANIZATION_ID, PARTY_ID, REGISTERED_NAME, TRADE_NAME, LEGAL_FORM_CODE,
                    REGISTRATION_NO, REGISTRATION_PLACE_CODE, INCORPORATION_DATE, DISSOLUTION_DATE,
                    ECONOMIC_SECTOR_CODE, ISIC_CODE, LISTED_COMPANY_FLAG,
                    CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :organizationId, :partyId, :registeredName, :tradeName, :legalForm,
                    :registrationNo, :registrationPlace, :incorporationDate, :dissolutionDate,
                    :economicSector, :isic, :listed, SYSTIMESTAMP, :actor, 1
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
                    ADDRESS_ID, COUNTRY_CODE, PROVINCE_CODE, CITY_CODE, DISTRICT_CODE, POSTAL_CODE,
                    ADDRESS_LINE1, ADDRESS_LINE2, CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :addressId, :country, :province, :city, :district, :postal,
                    :line1, :line2, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("ADDRESS"));
        jdbc.sql(addressSql).param("addressId", addressId).param("country", r.countryCode())
                .param("province", r.provinceCode()).param("city", r.cityCode()).param("district", r.districtCode())
                .param("postal", r.postalCode()).param("line1", r.addressLine1()).param("line2", r.addressLine2())
                .param("actor", actor).update();

        String relationSql = """
                INSERT INTO %s (
                    PARTY_ADDRESS_ID, PARTY_ID, ADDRESS_ID, ADDRESS_TYPE_CODE, IS_PRIMARY,
                    VALID_FROM, VALID_TO, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :partyAddressId, :partyId, :addressId, :addressType, :isPrimary,
                    :validFrom, :validTo, :actor, 1
                )
                """.formatted(table("PARTY_ADDRESS"));
        jdbc.sql(relationSql).param("partyAddressId", partyAddressId).param("partyId", partyId)
                .param("addressId", addressId).param("addressType", r.addressTypeCode()).param("isPrimary", r.isPrimary())
                .param("validFrom", sqlDate(r.validFrom())).param("validTo", sqlDate(r.validTo())).param("actor", actor).update();
        return partyAddressId;
    }

    public int updateAddress(long partyId, long partyAddressId, PartyAddressRequest r, String actor) {
        Long addressId = addressIdForPartyAddress(partyId, partyAddressId).orElse(null);
        if (addressId == null) {
            return 0;
        }
        int addressUpdated = jdbc.sql("""
                UPDATE %s SET COUNTRY_CODE=:country, PROVINCE_CODE=:province, CITY_CODE=:city,
                    DISTRICT_CODE=:district, POSTAL_CODE=:postal, ADDRESS_LINE1=:line1, ADDRESS_LINE2=:line2,
                    UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1
                WHERE ADDRESS_ID=:addressId AND RECORD_VERSION=:recordVersion
                """.formatted(table("ADDRESS")))
                .param("country", r.countryCode()).param("province", r.provinceCode()).param("city", r.cityCode())
                .param("district", r.districtCode()).param("postal", r.postalCode()).param("line1", r.addressLine1())
                .param("line2", r.addressLine2()).param("actor", actor).param("addressId", addressId)
                .param("recordVersion", r.addressRecordVersion()).update();
        if (addressUpdated == 0) {
            return 0;
        }
        return jdbc.sql("""
                UPDATE %s SET ADDRESS_TYPE_CODE=:addressType, IS_PRIMARY=:isPrimary,
                    VALID_FROM=:validFrom, VALID_TO=:validTo, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                    RECORD_VERSION=RECORD_VERSION+1
                WHERE PARTY_ADDRESS_ID=:partyAddressId AND PARTY_ID=:partyId AND RECORD_VERSION=:recordVersion
                """.formatted(table("PARTY_ADDRESS")))
                .param("addressType", r.addressTypeCode()).param("isPrimary", r.isPrimary())
                .param("validFrom", sqlDate(r.validFrom())).param("validTo", sqlDate(r.validTo())).param("actor", actor)
                .param("partyAddressId", partyAddressId).param("partyId", partyId)
                .param("recordVersion", r.partyAddressRecordVersion()).update();
    }

    public long insertContact(long partyId, ContactPointRequest r, String actor) {
        long id = nextVal("SEQ_CONTACT_POINT");
        String sql = """
                INSERT INTO %s (
                    CONTACT_POINT_ID, PARTY_ID, CONTACT_TYPE_CODE, CONTACT_VALUE, NORMALIZED_VALUE,
                    PURPOSE_CODE, IS_PRIMARY, IS_VERIFIED, VERIFIED_AT, VALID_FROM, VALID_TO,
                    CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :type, :value, :normalized, :purpose, :isPrimary, :isVerified,
                    :verifiedAt, :validFrom, :validTo, SYSTIMESTAMP, :actor, 1
                )
                """.formatted(table("CONTACT_POINT"));
        bindContact(jdbc.sql(sql).param("id", id).param("partyId", partyId), r, actor).update();
        return id;
    }

    public int updateContact(long partyId, long id, ContactPointRequest r, String actor) {
        String sql = """
                UPDATE %s SET CONTACT_TYPE_CODE=:type, CONTACT_VALUE=:value, NORMALIZED_VALUE=:normalized,
                    PURPOSE_CODE=:purpose, IS_PRIMARY=:isPrimary, IS_VERIFIED=:isVerified, VERIFIED_AT=:verifiedAt,
                    VALID_FROM=:validFrom, VALID_TO=:validTo, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
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
                .param("validTo", sqlDate(r.validTo())).param("actor", actor);
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
                    STORAGE_REF, MIME_TYPE, CREATED_AT, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :id, :partyId, :kycCaseId, :type, :number, :issuer, :issueDate, :expiryDate,
                    :verification, :verifiedAt, :hash, :storageRef, :mimeType, SYSTIMESTAMP, :actor, 1
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
                    STORAGE_REF=:storageRef, MIME_TYPE=:mimeType, UPDATED_AT=SYSTIMESTAMP,
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
                .param("hash", r.contentHash()).param("storageRef", r.storageRef()).param("mimeType", r.mimeType()).param("actor", actor);
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

    public void clearPrimaryAddresses(long partyId, Long exceptId, String actor) {
        String sql = "UPDATE " + table("PARTY_ADDRESS")
                + " SET IS_PRIMARY='N', UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1"
                + " WHERE PARTY_ID=:partyId AND IS_PRIMARY='Y'"
                + (exceptId == null ? "" : " AND PARTY_ADDRESS_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("actor", actor).param("partyId", partyId);
        if (exceptId != null) spec = spec.param("exceptId", exceptId);
        spec.update();
    }

    public void clearPrimaryContacts(long partyId, Long exceptId, String actor) {
        String sql = "UPDATE " + table("CONTACT_POINT")
                + " SET IS_PRIMARY='N', UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, RECORD_VERSION=RECORD_VERSION+1"
                + " WHERE PARTY_ID=:partyId AND IS_PRIMARY='Y'"
                + (exceptId == null ? "" : " AND CONTACT_POINT_ID<>:exceptId");
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("actor", actor).param("partyId", partyId);
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
        return deleteChild("CONTACT_POINT", "CONTACT_POINT_ID", partyId, id);
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
        int deleted = jdbc.sql("DELETE FROM " + table("PARTY_ADDRESS") + " WHERE PARTY_ADDRESS_ID=:id AND PARTY_ID=:partyId")
                .param("id", partyAddressId).param("partyId", partyId).update();
        if (deleted > 0) {
            jdbc.sql("DELETE FROM " + table("ADDRESS") + " WHERE ADDRESS_ID=:addressId")
                    .param("addressId", addressId).update();
        }
        return deleted;
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
