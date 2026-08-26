package com.behsazan.corebanking.calendar.datasetimport.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalendarDatasetCsvParserTest {

    @Test
    void parsesCalendarDayRow() {
        var row = CalendarDatasetCsvParser.parseDay("1,1826-01-01,-52595,2387993,7,7,2", 2);
        assertEquals(1L, row.dayId());
        assertEquals("1826-01-01", row.canonicalDate().toString());
        assertEquals(2387993L, row.julianDayNumber());
        assertEquals(7, row.isoWeekdayNo());
    }

    @Test
    void parsesCalendarDateRow() {
        var row = CalendarDatasetCsvParser.parseDate(
                "2,1,SOLAR_HIJRI_IR,1204,10,11,287,1204/10/11,N,PERSIAN_KHAYYAM_33", 2);
        assertEquals(2L, row.calendarDateId());
        assertEquals("SOLAR_HIJRI_IR", row.calendarSystemCode());
        assertEquals("1204/10/11", row.formattedDate());
    }

    @Test
    void supportsUtf8BomAndQuotedCsvHeader() {
        CalendarDatasetCsvParser.requireHeader(
                "\ufeffDAY_ID,CANONICAL_DATE,EPOCH_DAY,JULIAN_DAY_NUMBER,WEEKDAY_ID,ISO_WEEKDAY_NO,IR_WEEKDAY_NO",
                CalendarDatasetCsvParser.DAY_HEADER,
                "calendar_day.csv"
        );
        assertEquals(3, CalendarDatasetCsvParser.split("A,\"B,C\",D").size());
        assertEquals("B,C", CalendarDatasetCsvParser.split("A,\"B,C\",D").get(1));
    }

    @Test
    void rejectsUnknownCalendarSystem() {
        assertThrows(IllegalArgumentException.class, () -> CalendarDatasetCsvParser.parseDate(
                "1,1,UNKNOWN,2026,1,1,1,2026/01/01,N,ALGORITHM", 2));
    }
}
