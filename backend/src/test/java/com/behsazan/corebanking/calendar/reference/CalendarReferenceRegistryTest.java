package com.behsazan.corebanking.calendar.reference;

import com.behsazan.corebanking.calendar.reference.application.CalendarReferenceRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarReferenceRegistryTest {
    @Test
    void calendarCatalogMustCoverPhysicalCalTablesAndProtectGeneratedDataset() {
        CalendarReferenceRegistry registry = new CalendarReferenceRegistry("CAL");

        assertEquals(16, registry.catalog().tableCount());
        assertEquals("CAL", registry.catalog().schemaName());
        assertEquals(4, registry.catalog().groups().size());

        assertFalse(registry.require("calendar-days").allowCreate());
        assertFalse(registry.require("calendar-dates").allowUpdate());
        assertTrue(registry.require("business-calendars").allowCreate());
        assertTrue(registry.require("occasions").allowUpdate());
        assertEquals("CALENDAR_DAY", registry.require("calendar-days").tableName());
        assertEquals("HIJRI_DATE_OVERRIDE", registry.require("hijri-date-overrides").tableName());
    }
}
