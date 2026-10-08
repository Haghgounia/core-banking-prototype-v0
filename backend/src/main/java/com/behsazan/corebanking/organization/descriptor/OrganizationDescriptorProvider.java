package com.behsazan.corebanking.organization.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorProvider;
import com.behsazan.corebanking.referencedata.descriptor.domain.FieldType;
import com.behsazan.corebanking.referencedata.descriptor.domain.ParentDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.SelectOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.*;

@Component
@Order(70)
public class OrganizationDescriptorProvider implements ReferenceDescriptorProvider {
    private static final String CATEGORY = "ORGANIZATION";

    private static final List<SelectOption> ACTIVE_INACTIVE = List.of(
            new SelectOption("ACTIVE", "فعال"),
            new SelectOption("INACTIVE", "غیرفعال")
    );
    private static final List<SelectOption> UNIT_STATUS = List.of(
            new SelectOption("PLANNED", "برنامه‌ریزی‌شده"),
            new SelectOption("ACTIVE", "فعال"),
            new SelectOption("TEMPORARILY_CLOSED", "موقتاً تعطیل"),
            new SelectOption("CLOSED", "بسته"),
            new SelectOption("MERGED", "ادغام‌شده")
    );
    private static final List<SelectOption> CAPABILITY_STATUS = List.of(
            new SelectOption("ACTIVE", "فعال"),
            new SelectOption("SUSPENDED", "تعلیق"),
            new SelectOption("INACTIVE", "غیرفعال")
    );
    private static final List<SelectOption> POS_STATUS = List.of(
            new SelectOption("ACTIVE", "فعال"),
            new SelectOption("TEMPORARILY_CLOSED", "موقتاً تعطیل"),
            new SelectOption("INACTIVE", "غیرفعال")
    );
    private static final List<SelectOption> TERMINAL_STATUS = List.of(
            new SelectOption("PLANNED", "برنامه‌ریزی‌شده"),
            new SelectOption("ACTIVE", "فعال"),
            new SelectOption("OUT_OF_SERVICE", "خارج از سرویس"),
            new SelectOption("DECOMMISSIONED", "جمع‌آوری‌شده")
    );
    private static final List<SelectOption> SCHEDULE_TYPES = List.of(
            new SelectOption("REGULAR", "عادی"),
            new SelectOption("SEASONAL", "فصلی"),
            new SelectOption("SPECIAL", "ویژه")
    );
    private static final List<SelectOption> DAYS_OF_WEEK = List.of(
            new SelectOption("SATURDAY", "شنبه"),
            new SelectOption("SUNDAY", "یکشنبه"),
            new SelectOption("MONDAY", "دوشنبه"),
            new SelectOption("TUESDAY", "سه‌شنبه"),
            new SelectOption("WEDNESDAY", "چهارشنبه"),
            new SelectOption("THURSDAY", "پنجشنبه"),
            new SelectOption("FRIDAY", "جمعه")
    );
    private static final List<SelectOption> LOCATION_ROLES = List.of(
            new SelectOption("PRIMARY", "محل اصلی"),
            new SelectOption("REGISTERED", "نشانی ثبتی"),
            new SelectOption("OPERATIONAL", "محل عملیاتی")
    );
    private static final List<SelectOption> COVERAGE_TYPES = List.of(
            new SelectOption("ADMINISTRATIVE", "اداری"),
            new SelectOption("OPERATIONAL", "عملیاتی"),
            new SelectOption("SUPERVISORY", "نظارتی / سرپرستی"),
            new SelectOption("CASH_SERVICE", "پول‌رسانی")
    );
    private static final List<SelectOption> EXCEPTION_TYPES = List.of(
            new SelectOption("CLOSED", "تعطیل"),
            new SelectOption("SPECIAL_HOURS", "ساعات ویژه")
    );
    private static final List<SelectOption> SCHEDULE_ROLES = List.of(
            new SelectOption("CUSTOMER_SERVICE", "خدمت‌رسانی به مشتری"),
            new SelectOption("STAFF_WORKING", "ساعات کاری کارکنان"),
            new SelectOption("CASH_SERVICE", "خدمات نقدی")
    );
    private static final List<SelectOption> POS_ASSIGNMENT_ROLES = List.of(
            new SelectOption("OWNER", "مالک"),
            new SelectOption("MANAGER", "مدیر"),
            new SelectOption("SUPERVISOR", "ناظر"),
            new SelectOption("HOST_BRANCH", "شعبه میزبان")
    );
    private static final List<SelectOption> TERMINAL_ASSIGNMENT_ROLES = List.of(
            new SelectOption("SUPERVISOR", "ناظر"),
            new SelectOption("CASH_SERVICE_PROVIDER", "تأمین‌کننده پول‌رسانی")
    );
    private static final List<SelectOption> ADDRESS_TYPES = List.of(
            new SelectOption("PHYSICAL", "نشانی فیزیکی"),
            new SelectOption("POSTAL", "نشانی پستی"),
            new SelectOption("MAILING", "نشانی مکاتبات")
    );
    private static final List<SelectOption> COORDINATE_REFERENCE_SYSTEMS = List.of(
            new SelectOption("WGS84", "WGS 84 (EPSG:4326)")
    );

    @Value("${core-banking.schemas.organization:ORG}")
    private String schemaName = "ORG";

    @Override
    public List<ReferenceTableDescriptor> descriptors() {
        return List.of(
                contactRoles(),
                employeeAssignmentRoles(),
                employeeAssignments(),
                organizations(),
                organizationUnitTypes(),
                organizationUnits(),
                organizationUnitClassifications(),
                organizationUnitRelationshipTypes(),
                organizationUnitRelationships(),
                organizationUnitRelationshipRules(),
                organizationUnitLifecycleEventTypes(),
                organizationUnitLifecycleEvents(),
                locations(),
                postalAddresses(),
                organizationUnitLocations(),
                organizationUnitGeoCoverages(),
                operatingSchedules(),
                operatingScheduleIntervals(),
                organizationUnitOperatingSchedules(),
                organizationUnitOperatingExceptions(),
                organizationUnitOperatingExceptionIntervals(),
                serviceCapabilities(),
                organizationUnitServiceCapabilities(),
                foreignExchangeServiceProfiles(),
                pointOfServiceTypes(),
                pointsOfService(),
                organizationUnitPointOfServiceAssignments(),
                pointOfServiceContactPoints(),
                pointOfServiceOperatingSchedules(),
                pointOfServiceOperatingExceptions(),
                pointOfServiceOperatingExceptionIntervals(),
                selfServiceTerminalTypes(),
                selfServiceTerminals(),
                selfServiceTerminalAssignments(),
                organizationUnitContactPoints()
        );
    }

    private ReferenceTableDescriptor contactRoles() {
        return descriptor("contact-roles", CATEGORY, "نقش‌های راه تماس", "contact_phone",
                schemaName, "CONTACT_ROLES", "SEQ_CONTACT_ROLES",
                "contactRoleId", "CONTACT_ROLE_ID", "roleCode", "nameFa", null,
                fields(
                        id("contactRoleId", "CONTACT_ROLE_ID", "شناسه"),
                        text("roleCode", "ROLE_CODE", "کد نقش", true, true, true, 50),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 150),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, false, true, 150),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500),
                        bool("isActive", "ACTIVE_FLAG", "وضعیت", true, true, true)
                ));
    }

    private ReferenceTableDescriptor employeeAssignmentRoles() {
        return descriptor("employee-assignment-roles", CATEGORY, "نقش‌های انتساب کارکنان", "badge",
                schemaName, "EMPLOYEE_ASSIGNMENT_ROLES", "SEQ_EMPLOYEE_ASSIGNMENT_ROLES",
                "employeeAssignmentRoleId", "EMPLOYEE_ASSIGNMENT_ROLE_ID", "roleCode", "nameFa", null,
                fields(
                        id("employeeAssignmentRoleId", "EMPLOYEE_ASSIGNMENT_ROLE_ID", "شناسه"),
                        text("roleCode", "ROLE_CODE", "کد نقش", true, true, true, 50),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 150),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, false, true, 150),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500),
                        bool("isActive", "ACTIVE_FLAG", "وضعیت", true, true, true)
                ));
    }

    private ReferenceTableDescriptor employeeAssignments() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("employee-assignments", CATEGORY, "انتساب کارکنان به واحدها", "assignment_ind",
                schemaName, "EMPLOYEE_ASSIGNMENTS", "SEQ_EMPLOYEE_ASSIGNMENTS",
                "employeeAssignmentId", "EMPLOYEE_ASSIGNMENT_ID", "employeeAssignmentId", "employeeAssignmentId", parent,
                fields(
                        id("employeeAssignmentId", "EMPLOYEE_ASSIGNMENT_ID", "شناسه"),
                        number("employeeId", "EMPLOYEE_ID", "شناسه کارمند", true, true, null),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        lookup("employeeAssignmentRoleId", "EMPLOYEE_ASSIGNMENT_ROLE_ID", "نقش سازمانی", "employee-assignment-roles", true, true),
                        bool("primaryFlag", "PRIMARY_FLAG", "انتساب اصلی", true, true, false),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اعتبار", false, true),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اعتبار", false, true)
                ));
    }

    private ReferenceTableDescriptor organizations() {
        return descriptor("organizations", CATEGORY, "سازمان‌ها", "corporate_fare",
                schemaName, "ORGANIZATIONS", "SEQ_ORGANIZATIONS",
                "organizationId", "ORGANIZATION_ID", "organizationCode", "nameFa", null,
                fields(
                        id("organizationId", "ORGANIZATION_ID", "شناسه"),
                        text("organizationCode", "ORGANIZATION_CODE", "کد سازمان", true, true, true, 30),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 200),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, true, true, 200),
                        stringSelect("statusCode", "STATUS_CODE", "وضعیت", true, true, "ACTIVE", ACTIVE_INACTIVE),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500)
                ));
    }

    private ReferenceTableDescriptor organizationUnitTypes() {
        return descriptor("organization-unit-types", CATEGORY, "انواع واحد سازمانی", "category",
                schemaName, "ORGANIZATION_UNIT_TYPES", "SEQ_ORGANIZATION_UNIT_TYPES",
                "organizationUnitTypeId", "ORGANIZATION_UNIT_TYPE_ID", "typeCode", "nameFa", null,
                fields(
                        id("organizationUnitTypeId", "ORGANIZATION_UNIT_TYPE_ID", "شناسه"),
                        text("typeCode", "TYPE_CODE", "کد نوع", true, true, true, 50),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 150),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, true, true, 150),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", false, false, null),
                        bool("isActive", "ACTIVE_FLAG", "وضعیت", true, true, true)
                ));
    }

    private ReferenceTableDescriptor organizationUnits() {
        return descriptor("organization-units", CATEGORY, "واحدهای سازمانی و بانکی", "account_balance",
                schemaName, "ORGANIZATION_UNITS", "SEQ_ORGANIZATION_UNITS",
                "organizationUnitId", "ORGANIZATION_UNIT_ID", "unitCode", "nameFa", null,
                fieldsWithAudits(
                        id("organizationUnitId", "ORGANIZATION_UNIT_ID", "شناسه"),
                        lookup("organizationId", "ORGANIZATION_ID", "سازمان", "organizations", true, true),
                        lookup("organizationUnitTypeId", "ORGANIZATION_UNIT_TYPE_ID", "نوع واحد", "organization-unit-types", true, true),
                        text("unitCode", "UNIT_CODE", "کد واحد", true, true, true, 30),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 250),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, true, true, 250),
                        text("shortNameFa", "SHORT_NAME_FA", "نام کوتاه فارسی", false, false, true, 100),
                        text("shortNameEn", "SHORT_NAME_EN", "نام کوتاه انگلیسی", false, false, true, 100),
                        text("bic", "BIC", "کد BIC", false, true, true, 11),
                        stringSelect("statusCode", "STATUS_CODE", "وضعیت واحد", true, true, "ACTIVE", UNIT_STATUS),
                        date("openingDate", "OPENING_DATE", "تاریخ افتتاح", false, false),
                        date("closingDate", "CLOSING_DATE", "تاریخ خاتمه / تعطیلی", false, false),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 1000),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 1000)
                ));
    }

    private ReferenceTableDescriptor organizationUnitClassifications() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("organization-unit-classifications", CATEGORY, "طبقه‌بندی واحدها", "label",
                schemaName, "ORGANIZATION_UNIT_CLASSIFICATIONS", "SEQ_ORGANIZATION_UNIT_CLASSIFICATIONS",
                "organizationUnitClassificationId", "ORGANIZATION_UNIT_CLASSIFICATION_ID",
                "classificationTypeCode", "classificationValueCode", parent,
                fields(
                        id("organizationUnitClassificationId", "ORGANIZATION_UNIT_CLASSIFICATION_ID", "شناسه"),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        text("classificationTypeCode", "CLASSIFICATION_TYPE_CODE", "نوع طبقه‌بندی", true, true, true, 50),
                        text("classificationValueCode", "CLASSIFICATION_VALUE_CODE", "مقدار طبقه‌بندی", true, true, true, 50),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500)
                ));
    }

    private ReferenceTableDescriptor organizationUnitRelationshipTypes() {
        return descriptor("organization-unit-relationship-types", CATEGORY, "انواع رابطه واحدها", "schema",
                schemaName, "ORGANIZATION_UNIT_RELATIONSHIP_TYPES", "SEQ_ORGANIZATION_UNIT_RELATIONSHIP_TYPES",
                "organizationUnitRelationshipTypeId", "ORGANIZATION_UNIT_RELATIONSHIP_TYPE_ID", "typeCode", "nameFa", null,
                fields(
                        id("organizationUnitRelationshipTypeId", "ORGANIZATION_UNIT_RELATIONSHIP_TYPE_ID", "شناسه"),
                        text("typeCode", "TYPE_CODE", "کد نوع رابطه", true, true, true, 60),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 200),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, true, true, 200),
                        bool("hierarchicalFlag", "HIERARCHICAL_FLAG", "رابطه سلسله‌مراتبی", true, true, false),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500),
                        bool("isActive", "ACTIVE_FLAG", "وضعیت", true, true, true)
                ));
    }

    private ReferenceTableDescriptor organizationUnitRelationships() {
        return descriptor("organization-unit-relationships", CATEGORY, "روابط بین واحدهای سازمانی", "device_hub",
                schemaName, "ORGANIZATION_UNIT_RELATIONSHIPS", "SEQ_ORGANIZATION_UNIT_RELATIONSHIPS",
                "organizationUnitRelationshipId", "ORGANIZATION_UNIT_RELATIONSHIP_ID",
                "organizationUnitRelationshipId", "organizationUnitRelationshipId", null,
                fields(
                        id("organizationUnitRelationshipId", "ORGANIZATION_UNIT_RELATIONSHIP_ID", "شناسه"),
                        lookup("sourceOrganizationUnitId", "SOURCE_ORGANIZATION_UNIT_ID", "واحد مبدأ", "organization-units", true, true),
                        lookup("targetOrganizationUnitId", "TARGET_ORGANIZATION_UNIT_ID", "واحد مقصد", "organization-units", true, true),
                        lookup("organizationUnitRelationshipTypeId", "ORGANIZATION_UNIT_RELATIONSHIP_TYPE_ID", "نوع رابطه", "organization-unit-relationship-types", true, true),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اعتبار", false, true),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اعتبار", false, true),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500)
                ));
    }

    private ReferenceTableDescriptor organizationUnitRelationshipRules() {
        return descriptor("organization-unit-relationship-rules", CATEGORY, "قواعد روابط انواع واحدها", "rule",
                schemaName, "ORGANIZATION_UNIT_RELATIONSHIP_RULES", "SEQ_ORGANIZATION_UNIT_RELATIONSHIP_RULES",
                "organizationUnitRelationshipRuleId", "ORGANIZATION_UNIT_RELATIONSHIP_RULE_ID",
                "organizationUnitRelationshipRuleId", "organizationUnitRelationshipRuleId", null,
                fields(
                        id("organizationUnitRelationshipRuleId", "ORGANIZATION_UNIT_RELATIONSHIP_RULE_ID", "شناسه"),
                        lookup("organizationUnitRelationshipTypeId", "ORGANIZATION_UNIT_RELATIONSHIP_TYPE_ID", "نوع رابطه", "organization-unit-relationship-types", true, true),
                        lookup("sourceOrganizationUnitTypeId", "SOURCE_ORGANIZATION_UNIT_TYPE_ID", "نوع واحد مبدأ", "organization-unit-types", true, true),
                        lookup("targetOrganizationUnitTypeId", "TARGET_ORGANIZATION_UNIT_TYPE_ID", "نوع واحد مقصد", "organization-unit-types", true, true),
                        text("operationTypeCode", "OPERATION_TYPE_CODE", "کد نوع عملیات", false, true, true, 30),
                        text("subsystemCode", "SUBSYSTEM_CODE", "کد زیرسامانه", false, true, true, 30)
                ));
    }

    private ReferenceTableDescriptor organizationUnitLifecycleEventTypes() {
        return descriptor("organization-unit-lifecycle-event-types", CATEGORY, "انواع رویداد چرخه عمر واحد", "published_with_changes",
                schemaName, "ORGANIZATION_UNIT_LIFECYCLE_EVENT_TYPES", "SEQ_ORGANIZATION_UNIT_LIFECYCLE_EVENT_TYPES",
                "organizationUnitLifecycleEventTypeId", "ORGANIZATION_UNIT_LIFECYCLE_EVENT_TYPE_ID", "typeCode", "nameFa", null,
                fields(
                        id("organizationUnitLifecycleEventTypeId", "ORGANIZATION_UNIT_LIFECYCLE_EVENT_TYPE_ID", "شناسه"),
                        text("typeCode", "TYPE_CODE", "کد رویداد", true, true, true, 40),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 150),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, true, true, 150),
                        bool("isActive", "ACTIVE_FLAG", "وضعیت", true, true, true)
                ));
    }

    private ReferenceTableDescriptor organizationUnitLifecycleEvents() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("organization-unit-lifecycle-events", CATEGORY, "رویدادهای چرخه عمر واحدها", "history",
                schemaName, "ORGANIZATION_UNIT_LIFECYCLE_EVENTS", "SEQ_ORGANIZATION_UNIT_LIFECYCLE_EVENTS",
                "organizationUnitLifecycleEventId", "ORGANIZATION_UNIT_LIFECYCLE_EVENT_ID",
                "organizationUnitLifecycleEventId", "organizationUnitLifecycleEventId", parent,
                fields(
                        id("organizationUnitLifecycleEventId", "ORGANIZATION_UNIT_LIFECYCLE_EVENT_ID", "شناسه"),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        lookup("organizationUnitLifecycleEventTypeId", "ORGANIZATION_UNIT_LIFECYCLE_EVENT_TYPE_ID", "نوع رویداد", "organization-unit-lifecycle-event-types", true, true),
                        date("eventDate", "EVENT_DATE", "تاریخ رویداد", true, true),
                        lookup("successorOrganizationUnitId", "SUCCESSOR_ORGANIZATION_UNIT_ID", "واحد جانشین", "organization-units", false, true),
                        text("reasonCode", "REASON_CODE", "کد علت", false, true, true, 50),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 1000),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 1000)
                ));
    }

    private ReferenceTableDescriptor locations() {
        return descriptor("locations", CATEGORY, "مکان‌های فیزیکی", "location_on",
                schemaName, "LOCATIONS", "SEQ_LOCATIONS",
                "locationId", "LOCATION_ID", "geoEntityId", "descriptionFa", null,
                fields(
                        id("locationId", "LOCATION_ID", "شناسه"),
                        lookup("geoEntityId", "GEO_ENTITY_ID", "شهر / موجودیت جغرافیایی GEO", "cities", true, true),
                        number("latitude", "LATITUDE", "عرض جغرافیایی", true, true, null),
                        number("longitude", "LONGITUDE", "طول جغرافیایی", true, true, null),
                        stringSelect("coordinateReferenceSystemCode", "COORDINATE_REFERENCE_SYSTEM_CODE", "سامانه مرجع مختصات", true, false, "WGS84", COORDINATE_REFERENCE_SYSTEMS),
                        text("descriptionFa", "DESCRIPTION_FA", "شرح مکان", true, true, true, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "شرح انگلیسی", false, false, true, 500)
                ));
    }

    private ReferenceTableDescriptor postalAddresses() {
        ParentDescriptor parent = parent("locations", "locationId", "LOCATION_ID", "مکان");
        return descriptor("postal-addresses", CATEGORY, "نشانی‌های پستی", "home_pin",
                schemaName, "POSTAL_ADDRESSES", "SEQ_POSTAL_ADDRESSES",
                "postalAddressId", "POSTAL_ADDRESS_ID", "postalCode", "addressFa", parent,
                fields(
                        id("postalAddressId", "POSTAL_ADDRESS_ID", "شناسه"),
                        lookup("locationId", "LOCATION_ID", "مکان", "locations", true, false),
                        stringSelect("addressTypeCode", "ADDRESS_TYPE_CODE", "نوع نشانی", true, true, "PHYSICAL", ADDRESS_TYPES),
                        text("mainStreetFa", "MAIN_STREET_FA", "خیابان اصلی - فارسی", false, false, true, 300),
                        text("mainStreetEn", "MAIN_STREET_EN", "خیابان اصلی - انگلیسی", false, false, true, 300),
                        text("secondaryStreetFa", "SECONDARY_STREET_FA", "خیابان فرعی - فارسی", false, false, true, 300),
                        text("secondaryStreetEn", "SECONDARY_STREET_EN", "خیابان فرعی - انگلیسی", false, false, true, 300),
                        text("squareCrossroadFa", "SQUARE_CROSSROAD_FA", "میدان / تقاطع - فارسی", false, false, true, 300),
                        text("squareCrossroadEn", "SQUARE_CROSSROAD_EN", "میدان / تقاطع - انگلیسی", false, false, true, 300),
                        text("buildingNo", "BUILDING_NO", "پلاک / شماره ساختمان", false, true, true, 30),
                        text("postalCode", "POSTAL_CODE", "کد پستی", false, true, true, 20),
                        text("addressFa", "ADDRESS_FA", "نشانی کامل فارسی", false, true, true, 1000),
                        text("addressEn", "ADDRESS_EN", "نشانی کامل انگلیسی", false, false, true, 1000),
                        text("additionalAddressFa", "ADDITIONAL_ADDRESS_FA", "توضیحات تکمیلی نشانی", false, false, false, 500),
                        text("additionalAddressEn", "ADDITIONAL_ADDRESS_EN", "توضیحات تکمیلی انگلیسی", false, false, false, 500)
                ));
    }

    private ReferenceTableDescriptor organizationUnitLocations() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("organization-unit-locations", CATEGORY, "محل استقرار واحدها", "my_location",
                schemaName, "ORGANIZATION_UNIT_LOCATIONS", "SEQ_ORGANIZATION_UNIT_LOCATIONS",
                "organizationUnitLocationId", "ORGANIZATION_UNIT_LOCATION_ID",
                "organizationUnitLocationId", "organizationUnitLocationId", parent,
                fields(
                        id("organizationUnitLocationId", "ORGANIZATION_UNIT_LOCATION_ID", "شناسه"),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        lookup("locationId", "LOCATION_ID", "مکان", "locations", true, true),
                        stringSelect("locationRoleCode", "LOCATION_ROLE_CODE", "نقش مکان", true, true, "PRIMARY", LOCATION_ROLES),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اعتبار", false, true),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اعتبار", false, true)
                ));
    }

    private ReferenceTableDescriptor organizationUnitGeoCoverages() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("organization-unit-geo-coverages", CATEGORY, "پوشش جغرافیایی واحدها", "map",
                schemaName, "ORGANIZATION_UNIT_GEO_COVERAGES", "SEQ_ORGANIZATION_UNIT_GEO_COVERAGES",
                "organizationUnitGeoCoverageId", "ORGANIZATION_UNIT_GEO_COVERAGE_ID",
                "organizationUnitGeoCoverageId", "organizationUnitGeoCoverageId", parent,
                fields(
                        id("organizationUnitGeoCoverageId", "ORGANIZATION_UNIT_GEO_COVERAGE_ID", "شناسه"),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        number("geoEntityId", "GEO_ENTITY_ID", "شناسه موجودیت جغرافیایی GEO", true, true, null),
                        stringSelect("coverageTypeCode", "COVERAGE_TYPE_CODE", "نوع پوشش", true, true, "SUPERVISORY", COVERAGE_TYPES),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اعتبار", false, true),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اعتبار", false, true)
                ));
    }

    private ReferenceTableDescriptor operatingSchedules() {
        return descriptor("operating-schedules", CATEGORY, "الگوهای ساعات کاری", "schedule",
                schemaName, "OPERATING_SCHEDULES", "SEQ_OPERATING_SCHEDULES",
                "operatingScheduleId", "OPERATING_SCHEDULE_ID", "scheduleCode", "nameFa", null,
                fields(
                        id("operatingScheduleId", "OPERATING_SCHEDULE_ID", "شناسه"),
                        text("scheduleCode", "SCHEDULE_CODE", "کد برنامه", true, true, true, 50),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 200),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, true, true, 200),
                        stringSelect("scheduleTypeCode", "SCHEDULE_TYPE_CODE", "نوع برنامه", true, true, "REGULAR", SCHEDULE_TYPES),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500),
                        bool("isActive", "ACTIVE_FLAG", "وضعیت", true, true, true)
                ));
    }

    private ReferenceTableDescriptor operatingScheduleIntervals() {
        ParentDescriptor parent = parent("operating-schedules", "operatingScheduleId", "OPERATING_SCHEDULE_ID", "برنامه ساعات کاری");
        return descriptor("operating-schedule-intervals", CATEGORY, "بازه‌های ساعات کاری", "access_time",
                schemaName, "OPERATING_SCHEDULE_INTERVALS", "SEQ_OPERATING_SCHEDULE_INTERVALS",
                "operatingScheduleIntervalId", "OPERATING_SCHEDULE_INTERVAL_ID",
                "operatingScheduleIntervalId", "operatingScheduleIntervalId", parent,
                fields(
                        id("operatingScheduleIntervalId", "OPERATING_SCHEDULE_INTERVAL_ID", "شناسه"),
                        lookup("operatingScheduleId", "OPERATING_SCHEDULE_ID", "برنامه ساعات کاری", "operating-schedules", true, false),
                        stringSelect("dayOfWeekCode", "DAY_OF_WEEK_CODE", "روز هفته", true, true, "SATURDAY", DAYS_OF_WEEK),
                        number("shiftNo", "SHIFT_NO", "شماره شیفت", true, true, 1L),
                        text("startTime", "START_TIME", "ساعت شروع", true, true, false, 5),
                        text("endTime", "END_TIME", "ساعت پایان", true, true, false, 5)
                ));
    }

    private ReferenceTableDescriptor organizationUnitOperatingSchedules() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("organization-unit-operating-schedules", CATEGORY, "انتساب ساعات کاری به واحدها", "calendar_month",
                schemaName, "ORGANIZATION_UNIT_OPERATING_SCHEDULES", "SEQ_ORGANIZATION_UNIT_OPERATING_SCHEDULES",
                "organizationUnitOperatingScheduleId", "ORGANIZATION_UNIT_OPERATING_SCHEDULE_ID",
                "organizationUnitOperatingScheduleId", "organizationUnitOperatingScheduleId", parent,
                fields(
                        id("organizationUnitOperatingScheduleId", "ORGANIZATION_UNIT_OPERATING_SCHEDULE_ID", "شناسه"),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        lookup("operatingScheduleId", "OPERATING_SCHEDULE_ID", "برنامه ساعات کاری", "operating-schedules", true, true),
                        stringSelect("scheduleRoleCode", "SCHEDULE_ROLE_CODE", "کاربرد برنامه", true, true, "CUSTOMER_SERVICE", SCHEDULE_ROLES),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اعتبار", false, true),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اعتبار", false, true)
                ));
    }

    private ReferenceTableDescriptor organizationUnitOperatingExceptions() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("organization-unit-operating-exceptions", CATEGORY, "استثناهای ساعات کاری واحدها", "event_busy",
                schemaName, "ORGANIZATION_UNIT_OPERATING_EXCEPTIONS", "SEQ_ORGANIZATION_UNIT_OPERATING_EXCEPTIONS",
                "organizationUnitOperatingExceptionId", "ORGANIZATION_UNIT_OPERATING_EXCEPTION_ID",
                "organizationUnitOperatingExceptionId", "organizationUnitOperatingExceptionId", parent,
                fields(
                        id("organizationUnitOperatingExceptionId", "ORGANIZATION_UNIT_OPERATING_EXCEPTION_ID", "شناسه"),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        date("exceptionDate", "EXCEPTION_DATE", "تاریخ استثنا", true, true),
                        stringSelect("exceptionTypeCode", "EXCEPTION_TYPE_CODE", "نوع استثنا", true, true, "CLOSED", EXCEPTION_TYPES),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, true, true, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, true, 500)
                ));
    }

    private ReferenceTableDescriptor organizationUnitOperatingExceptionIntervals() {
        ParentDescriptor parent = parent("organization-unit-operating-exceptions", "organizationUnitOperatingExceptionId", "ORGANIZATION_UNIT_OPERATING_EXCEPTION_ID", "استثنای ساعات کاری");
        return descriptor("organization-unit-operating-exception-intervals", CATEGORY, "بازه‌های استثنای ساعات کاری واحد", "more_time",
                schemaName, "ORGANIZATION_UNIT_OPERATING_EXCEPTION_INTERVALS", "SEQ_ORGANIZATION_UNIT_OPERATING_EXCEPTION_INTERVALS",
                "organizationUnitOperatingExceptionIntervalId", "ORGANIZATION_UNIT_OPERATING_EXCEPTION_INTERVAL_ID",
                "organizationUnitOperatingExceptionIntervalId", "organizationUnitOperatingExceptionIntervalId", parent,
                fields(
                        id("organizationUnitOperatingExceptionIntervalId", "ORGANIZATION_UNIT_OPERATING_EXCEPTION_INTERVAL_ID", "شناسه"),
                        lookup("organizationUnitOperatingExceptionId", "ORGANIZATION_UNIT_OPERATING_EXCEPTION_ID", "استثنای ساعات کاری", "organization-unit-operating-exceptions", true, false),
                        number("shiftNo", "SHIFT_NO", "شماره شیفت", true, true, 1L),
                        text("startTime", "START_TIME", "ساعت شروع", true, true, false, 5),
                        text("endTime", "END_TIME", "ساعت پایان", true, true, false, 5)
                ));
    }

    private ReferenceTableDescriptor serviceCapabilities() {
        return descriptor("service-capabilities", CATEGORY, "قابلیت‌های خدمت‌رسانی", "room_service",
                schemaName, "SERVICE_CAPABILITIES", "SEQ_SERVICE_CAPABILITIES",
                "serviceCapabilityId", "SERVICE_CAPABILITY_ID", "capabilityCode", "nameFa", null,
                fields(
                        id("serviceCapabilityId", "SERVICE_CAPABILITY_ID", "شناسه"),
                        text("capabilityCode", "CAPABILITY_CODE", "کد قابلیت", true, true, true, 50),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 150),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, true, true, 150),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500),
                        bool("isActive", "ACTIVE_FLAG", "وضعیت", true, true, true)
                ));
    }

    private ReferenceTableDescriptor organizationUnitServiceCapabilities() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("organization-unit-service-capabilities", CATEGORY, "قابلیت‌های خدماتی واحدها", "fact_check",
                schemaName, "ORGANIZATION_UNIT_SERVICE_CAPABILITIES", "SEQ_ORGANIZATION_UNIT_SERVICE_CAPABILITIES",
                "organizationUnitServiceCapabilityId", "ORGANIZATION_UNIT_SERVICE_CAPABILITY_ID",
                "organizationUnitServiceCapabilityId", "organizationUnitServiceCapabilityId", parent,
                fields(
                        id("organizationUnitServiceCapabilityId", "ORGANIZATION_UNIT_SERVICE_CAPABILITY_ID", "شناسه"),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        lookup("serviceCapabilityId", "SERVICE_CAPABILITY_ID", "قابلیت خدمت", "service-capabilities", true, true),
                        stringSelect("statusCode", "STATUS_CODE", "وضعیت قابلیت", true, true, "ACTIVE", CAPABILITY_STATUS),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اعتبار", false, true),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اعتبار", false, true)
                ));
    }

    private ReferenceTableDescriptor foreignExchangeServiceProfiles() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("foreign-exchange-service-profiles", CATEGORY, "پروفایل خدمات ارزی واحدها", "currency_exchange",
                schemaName, "FOREIGN_EXCHANGE_SERVICE_PROFILES", "SEQ_FOREIGN_EXCHANGE_SERVICE_PROFILES",
                "foreignExchangeServiceProfileId", "FOREIGN_EXCHANGE_SERVICE_PROFILE_ID",
                "foreignExchangeServiceProfileId", "foreignExchangeServiceProfileId", parent,
                fields(
                        id("foreignExchangeServiceProfileId", "FOREIGN_EXCHANGE_SERVICE_PROFILE_ID", "شناسه"),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        date("situationDate", "SITUATION_DATE", "تاریخ وضعیت ارزی", false, true),
                        date("workDate", "WORK_DATE", "تاریخ کاری ارزی", false, true),
                        bool("recordIncomeFlag", "RECORD_INCOME_FLAG", "ثبت درآمد ارزی", false, true, false),
                        stringSelect("statusCode", "STATUS_CODE", "وضعیت خدمت ارزی", false, true, "ACTIVE", CAPABILITY_STATUS),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500)
                ));
    }

    private ReferenceTableDescriptor pointOfServiceTypes() {
        return descriptor("point-of-service-types", CATEGORY, "انواع نقطه ارائه خدمت", "place",
                schemaName, "POINT_OF_SERVICE_TYPES", "SEQ_POINT_OF_SERVICE_TYPES",
                "pointOfServiceTypeId", "POINT_OF_SERVICE_TYPE_ID", "typeCode", "nameFa", null,
                fields(
                        id("pointOfServiceTypeId", "POINT_OF_SERVICE_TYPE_ID", "شناسه"),
                        text("typeCode", "TYPE_CODE", "کد نوع", true, true, true, 50),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 150),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, true, true, 150),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500),
                        bool("isActive", "ACTIVE_FLAG", "وضعیت", true, true, true)
                ));
    }

    private ReferenceTableDescriptor pointsOfService() {
        return descriptor("points-of-service", CATEGORY, "نقاط ارائه خدمت", "storefront",
                schemaName, "POINTS_OF_SERVICE", "SEQ_POINTS_OF_SERVICE",
                "pointOfServiceId", "POINT_OF_SERVICE_ID", "pointOfServiceCode", "nameFa", null,
                fields(
                        id("pointOfServiceId", "POINT_OF_SERVICE_ID", "شناسه"),
                        lookup("pointOfServiceTypeId", "POINT_OF_SERVICE_TYPE_ID", "نوع نقطه خدمت", "point-of-service-types", true, true),
                        lookup("locationId", "LOCATION_ID", "مکان", "locations", true, true),
                        text("pointOfServiceCode", "POINT_OF_SERVICE_CODE", "کد نقطه خدمت", false, true, true, 50),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 250),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, true, true, 250),
                        stringSelect("statusCode", "STATUS_CODE", "وضعیت", true, true, "ACTIVE", POS_STATUS),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500)
                ));
    }

    private ReferenceTableDescriptor organizationUnitPointOfServiceAssignments() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("organization-unit-point-of-service-assignments", CATEGORY, "انتساب نقاط خدمت به واحدها", "lan",
                schemaName, "ORGANIZATION_UNIT_POINT_OF_SERVICE_ASSIGNMENTS", "SEQ_ORGANIZATION_UNIT_POINT_OF_SERVICE_ASSIGNMENTS",
                "organizationUnitPointOfServiceAssignmentId", "ORGANIZATION_UNIT_POINT_OF_SERVICE_ASSIGNMENT_ID",
                "organizationUnitPointOfServiceAssignmentId", "organizationUnitPointOfServiceAssignmentId", parent,
                fields(
                        id("organizationUnitPointOfServiceAssignmentId", "ORGANIZATION_UNIT_POINT_OF_SERVICE_ASSIGNMENT_ID", "شناسه"),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        lookup("pointOfServiceId", "POINT_OF_SERVICE_ID", "نقطه ارائه خدمت", "points-of-service", true, true),
                        stringSelect("assignmentRoleCode", "ASSIGNMENT_ROLE_CODE", "نقش انتساب", true, true, "SUPERVISOR", POS_ASSIGNMENT_ROLES),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اعتبار", false, true),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اعتبار", false, true)
                ));
    }

    private ReferenceTableDescriptor pointOfServiceContactPoints() {
        ParentDescriptor parent = parent("points-of-service", "pointOfServiceId", "POINT_OF_SERVICE_ID", "نقطه ارائه خدمت");
        return descriptor("point-of-service-contact-points", CATEGORY, "راه‌های تماس نقاط خدمت", "contact_mail",
                schemaName, "POINT_OF_SERVICE_CONTACT_POINTS", "SEQ_POINT_OF_SERVICE_CONTACT_POINTS",
                "pointOfServiceContactPointId", "POINT_OF_SERVICE_CONTACT_POINT_ID",
                "pointOfServiceContactPointId", "pointOfServiceContactPointId", parent,
                fields(
                        id("pointOfServiceContactPointId", "POINT_OF_SERVICE_CONTACT_POINT_ID", "شناسه"),
                        lookup("pointOfServiceId", "POINT_OF_SERVICE_ID", "نقطه ارائه خدمت", "points-of-service", true, false),
                        number("contactPointId", "CONTACT_POINT_ID", "شناسه راه تماس مشترک", true, true, null),
                        lookup("contactRoleId", "CONTACT_ROLE_ID", "نقش راه تماس", "contact-roles", true, true),
                        bool("primaryFlag", "PRIMARY_FLAG", "راه تماس اصلی", true, true, false)
                ));
    }

    private ReferenceTableDescriptor pointOfServiceOperatingSchedules() {
        ParentDescriptor parent = parent("points-of-service", "pointOfServiceId", "POINT_OF_SERVICE_ID", "نقطه ارائه خدمت");
        return descriptor("point-of-service-operating-schedules", CATEGORY, "ساعات کاری نقاط خدمت", "calendar_today",
                schemaName, "POINT_OF_SERVICE_OPERATING_SCHEDULES", "SEQ_POINT_OF_SERVICE_OPERATING_SCHEDULES",
                "pointOfServiceOperatingScheduleId", "POINT_OF_SERVICE_OPERATING_SCHEDULE_ID",
                "pointOfServiceOperatingScheduleId", "pointOfServiceOperatingScheduleId", parent,
                fields(
                        id("pointOfServiceOperatingScheduleId", "POINT_OF_SERVICE_OPERATING_SCHEDULE_ID", "شناسه"),
                        lookup("pointOfServiceId", "POINT_OF_SERVICE_ID", "نقطه ارائه خدمت", "points-of-service", true, false),
                        lookup("operatingScheduleId", "OPERATING_SCHEDULE_ID", "برنامه ساعات کاری", "operating-schedules", true, true),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اعتبار", false, true),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اعتبار", false, true)
                ));
    }

    private ReferenceTableDescriptor pointOfServiceOperatingExceptions() {
        ParentDescriptor parent = parent("points-of-service", "pointOfServiceId", "POINT_OF_SERVICE_ID", "نقطه ارائه خدمت");
        return descriptor("point-of-service-operating-exceptions", CATEGORY, "استثناهای ساعات کاری نقاط خدمت", "event_busy",
                schemaName, "POINT_OF_SERVICE_OPERATING_EXCEPTIONS", "SEQ_POINT_OF_SERVICE_OPERATING_EXCEPTIONS",
                "pointOfServiceOperatingExceptionId", "POINT_OF_SERVICE_OPERATING_EXCEPTION_ID",
                "pointOfServiceOperatingExceptionId", "pointOfServiceOperatingExceptionId", parent,
                fields(
                        id("pointOfServiceOperatingExceptionId", "POINT_OF_SERVICE_OPERATING_EXCEPTION_ID", "شناسه"),
                        lookup("pointOfServiceId", "POINT_OF_SERVICE_ID", "نقطه ارائه خدمت", "points-of-service", true, false),
                        date("exceptionDate", "EXCEPTION_DATE", "تاریخ استثنا", true, true),
                        stringSelect("exceptionTypeCode", "EXCEPTION_TYPE_CODE", "نوع استثنا", true, true, "CLOSED", EXCEPTION_TYPES),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, true, true, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, true, 500)
                ));
    }

    private ReferenceTableDescriptor pointOfServiceOperatingExceptionIntervals() {
        ParentDescriptor parent = parent("point-of-service-operating-exceptions", "pointOfServiceOperatingExceptionId", "POINT_OF_SERVICE_OPERATING_EXCEPTION_ID", "استثنای ساعات کاری");
        return descriptor("point-of-service-operating-exception-intervals", CATEGORY, "بازه‌های استثنای ساعات کاری نقاط خدمت", "more_time",
                schemaName, "POINT_OF_SERVICE_OPERATING_EXCEPTION_INTERVALS", "SEQ_POINT_OF_SERVICE_OPERATING_EXCEPTION_INTERVALS",
                "pointOfServiceOperatingExceptionIntervalId", "POINT_OF_SERVICE_OPERATING_EXCEPTION_INTERVAL_ID",
                "pointOfServiceOperatingExceptionIntervalId", "pointOfServiceOperatingExceptionIntervalId", parent,
                fields(
                        id("pointOfServiceOperatingExceptionIntervalId", "POINT_OF_SERVICE_OPERATING_EXCEPTION_INTERVAL_ID", "شناسه"),
                        lookup("pointOfServiceOperatingExceptionId", "POINT_OF_SERVICE_OPERATING_EXCEPTION_ID", "استثنای ساعات کاری", "point-of-service-operating-exceptions", true, false),
                        number("shiftNo", "SHIFT_NO", "شماره شیفت", true, true, 1L),
                        text("startTime", "START_TIME", "ساعت شروع", true, true, false, 5),
                        text("endTime", "END_TIME", "ساعت پایان", true, true, false, 5)
                ));
    }

    private ReferenceTableDescriptor selfServiceTerminalTypes() {
        return descriptor("self-service-terminal-types", CATEGORY, "انواع پایانه خودخدمت", "atm",
                schemaName, "SELF_SERVICE_TERMINAL_TYPES", "SEQ_SELF_SERVICE_TERMINAL_TYPES",
                "selfServiceTerminalTypeId", "SELF_SERVICE_TERMINAL_TYPE_ID", "typeCode", "nameFa", null,
                fields(
                        id("selfServiceTerminalTypeId", "SELF_SERVICE_TERMINAL_TYPE_ID", "شناسه"),
                        text("typeCode", "TYPE_CODE", "کد نوع", true, true, true, 50),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 150),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, true, true, 150),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, false, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, false, 500),
                        bool("isActive", "ACTIVE_FLAG", "وضعیت", true, true, true)
                ));
    }

    private ReferenceTableDescriptor selfServiceTerminals() {
        ParentDescriptor parent = parent("points-of-service", "pointOfServiceId", "POINT_OF_SERVICE_ID", "نقطه ارائه خدمت");
        return descriptor("self-service-terminals", CATEGORY, "پایانه‌های خودخدمت", "atm",
                schemaName, "SELF_SERVICE_TERMINALS", "SEQ_SELF_SERVICE_TERMINALS",
                "selfServiceTerminalId", "SELF_SERVICE_TERMINAL_ID", "terminalCode", "terminalCode", parent,
                fields(
                        id("selfServiceTerminalId", "SELF_SERVICE_TERMINAL_ID", "شناسه"),
                        lookup("selfServiceTerminalTypeId", "SELF_SERVICE_TERMINAL_TYPE_ID", "نوع پایانه", "self-service-terminal-types", true, true),
                        lookup("pointOfServiceId", "POINT_OF_SERVICE_ID", "نقطه ارائه خدمت", "points-of-service", true, false),
                        text("terminalCode", "TERMINAL_CODE", "کد پایانه", true, true, true, 50),
                        text("serialNumber", "SERIAL_NUMBER", "شماره سریال", false, true, true, 100),
                        text("assetNumber", "ASSET_NUMBER", "شماره اموال", false, true, true, 100),
                        stringSelect("statusCode", "STATUS_CODE", "وضعیت پایانه", true, true, "ACTIVE", TERMINAL_STATUS),
                        date("installationDate", "INSTALLATION_DATE", "تاریخ نصب", false, false),
                        date("activationDate", "ACTIVATION_DATE", "تاریخ فعال‌سازی", false, false),
                        date("decommissionDate", "DECOMMISSION_DATE", "تاریخ جمع‌آوری", false, false),
                        text("descriptionFa", "DESCRIPTION_FA", "توضیحات فارسی", false, false, true, 500),
                        text("descriptionEn", "DESCRIPTION_EN", "توضیحات انگلیسی", false, false, true, 500)
                ));
    }

    private ReferenceTableDescriptor selfServiceTerminalAssignments() {
        ParentDescriptor parent = parent("self-service-terminals", "selfServiceTerminalId", "SELF_SERVICE_TERMINAL_ID", "پایانه خودخدمت");
        return descriptor("self-service-terminal-assignments", CATEGORY, "انتساب پایانه‌ها به واحدها", "account_tree",
                schemaName, "SELF_SERVICE_TERMINAL_ASSIGNMENTS", "SEQ_SELF_SERVICE_TERMINAL_ASSIGNMENTS",
                "selfServiceTerminalAssignmentId", "SELF_SERVICE_TERMINAL_ASSIGNMENT_ID",
                "selfServiceTerminalAssignmentId", "selfServiceTerminalAssignmentId", parent,
                fields(
                        id("selfServiceTerminalAssignmentId", "SELF_SERVICE_TERMINAL_ASSIGNMENT_ID", "شناسه"),
                        lookup("selfServiceTerminalId", "SELF_SERVICE_TERMINAL_ID", "پایانه خودخدمت", "self-service-terminals", true, false),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, true),
                        stringSelect("assignmentRoleCode", "ASSIGNMENT_ROLE_CODE", "نقش انتساب", true, true, "SUPERVISOR", TERMINAL_ASSIGNMENT_ROLES),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اعتبار", false, true),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اعتبار", false, true)
                ));
    }

    private ReferenceTableDescriptor organizationUnitContactPoints() {
        ParentDescriptor parent = parent("organization-units", "organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی");
        return descriptor("organization-unit-contact-points", CATEGORY, "راه‌های تماس واحدها", "call",
                schemaName, "ORGANIZATION_UNIT_CONTACT_POINTS", "SEQ_ORGANIZATION_UNIT_CONTACT_POINTS",
                "organizationUnitContactPointId", "ORGANIZATION_UNIT_CONTACT_POINT_ID",
                "organizationUnitContactPointId", "organizationUnitContactPointId", parent,
                fields(
                        id("organizationUnitContactPointId", "ORGANIZATION_UNIT_CONTACT_POINT_ID", "شناسه"),
                        lookup("organizationUnitId", "ORGANIZATION_UNIT_ID", "واحد سازمانی", "organization-units", true, false),
                        number("contactPointId", "CONTACT_POINT_ID", "شناسه راه تماس مشترک", true, true, null),
                        lookup("contactRoleId", "CONTACT_ROLE_ID", "نقش راه تماس", "contact-roles", true, true),
                        bool("primaryFlag", "PRIMARY_FLAG", "راه تماس اصلی", true, true, false),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اعتبار", false, true),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اعتبار", false, true)
                ));
    }

    private static ParentDescriptor parent(String resource, String apiField, String columnName, String label) {
        return new ParentDescriptor(resource, apiField, columnName, label);
    }

    private static List<ReferenceFieldDescriptor> fields(ReferenceFieldDescriptor... values) {
        return List.of(values);
    }

    private static List<ReferenceFieldDescriptor> fieldsWithAudits(ReferenceFieldDescriptor... values) {
        List<ReferenceFieldDescriptor> result = new ArrayList<>(List.of(values));
        result.add(audit("createdAt", "CREATED_AT", "زمان ایجاد", FieldType.TIMESTAMP));
        result.add(audit("createdBy", "CREATED_BY", "ایجادکننده", FieldType.TEXT));
        result.add(audit("updatedAt", "UPDATED_AT", "زمان آخرین ویرایش", FieldType.TIMESTAMP));
        result.add(audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", FieldType.TEXT));
        return List.copyOf(result);
    }
}
