package com.rahulshankar.dialysisapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.rahulshankar.dialysisapp.service.DemoCsv.csv;
import static com.rahulshankar.dialysisapp.service.DemoCsv.row;
import static com.rahulshankar.dialysisapp.service.DemoCsv.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoFoodImporterTest extends HomeIsolatedTest {

    private DemoFoodImporter.ImportResult importText(String text) throws Exception {
        return new DemoFoodImporter().importCsv(stream(text));
    }

    private void assertDemoTablesEmpty() throws Exception {
        assertEquals(0, count(demoDb(), "demo_imported_foods"));
        assertEquals(0, count(demoDb(), "demo_food_metadata"));
        assertEquals(0, count(demoDb(), "food_nutrient_provenance"));
    }

    // ------------------------------------------------------------------ mapping

    @Test void everyColumnIsMappedToTheCorrectField() throws Exception {
        assertEquals(1, importText(csv(row())).imported);
        try (Connection c = open(demoDb()); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT * FROM demo_food_metadata WHERE demo_id='D1'")) {
            assertTrue(r.next());
            assertEquals("Test Food", r.getString("food_name"));
            assertEquals(DemoCsv.TAMIL, r.getString("local_name"));
            assertEquals("Demo", r.getString("category"));
            assertEquals("Boiled", r.getString("preparation"));
            assertEquals("per 100g", r.getString("basis"));
            assertEquals(10.0, r.getDouble("energy_kcal"));
            assertEquals(1.5, r.getDouble("protein_g"));
            assertEquals(2.5, r.getDouble("carbohydrates_g"));
            assertEquals(3.5, r.getDouble("fat_g"));
            assertEquals(4.5, r.getDouble("fibre_g"));
            assertEquals(111.0, r.getDouble("potassium_mg"));
            assertEquals(222.0, r.getDouble("phosphorus_mg"));
            assertEquals(333.0, r.getDouble("sodium_mg"));
            assertEquals(44.0, r.getDouble("calcium_mg"));
            assertEquals(55.0, r.getDouble("magnesium_mg"));
            assertEquals(6.6, r.getDouble("iron_mg"));
            assertEquals("Test Source", r.getString("source_name"));
            assertEquals("SRC-1", r.getString("source_record_id"));
            assertEquals("SYNTHETIC", r.getString("data_status"));
            assertEquals("Test citation", r.getString("citation"));
            assertEquals("Test licence", r.getString("license_note"));
        }
        try (Connection c = open(demoDb()); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT nutrient, value, unit, source_name, data_status FROM food_nutrient_provenance WHERE demo_id='D1' ORDER BY nutrient")) {
            assertTrue(r.next()); assertEquals("phosphorus_mg", r.getString(1)); assertEquals(222.0, r.getDouble(2)); assertEquals("mg", r.getString(3));
            assertTrue(r.next()); assertEquals("potassium_mg", r.getString(1)); assertEquals(111.0, r.getDouble(2));
            assertTrue(r.next()); assertEquals("sodium_mg", r.getString(1)); assertEquals(333.0, r.getDouble(2));
            assertEquals("Test Source", r.getString(4));
            assertEquals("SYNTHETIC", r.getString(5));
            assertFalse(r.next());
        }
    }

    @Test void viewerReturnsAllNutrientColumnsAfterImport() throws Exception {
        importText(csv(row()));
        List<Map<String, String>> profiles = new DemoFoodProfileService().findProfiles();
        assertEquals(1, profiles.size());
        Map<String, String> p = profiles.get(0);
        assertEquals("111.0", p.get("potassium_mg"));
        assertEquals("222.0", p.get("phosphorus_mg"));
        assertEquals("333.0", p.get("sodium_mg"));
        assertEquals("44.0", p.get("calcium_mg"));
        assertEquals("55.0", p.get("magnesium_mg"));
        assertEquals("6.6", p.get("iron_mg"));
        assertEquals("Test Food", p.get("food_name"));
        assertEquals("SYNTHETIC", p.get("data_status"));
    }

    @Test void columnOrderInTheFileMayVary() throws Exception {
        List<String> reversed = new ArrayList<>(DemoCsv.HEADERS);
        Collections.reverse(reversed);
        Map<String, String> values = DemoCsv.defaults();
        List<String> fields = new ArrayList<>();
        for (String h : reversed) fields.add(DemoCsv.quote(values.get(h)));
        String text = String.join(",", reversed) + "\n" + String.join(",", fields) + "\n";
        assertEquals(1, importText(text).imported);
        Map<String, String> p = new DemoFoodProfileService().findProfiles().get(0);
        assertEquals("111.0", p.get("potassium_mg"));
        assertEquals("Test Food", p.get("food_name"));
    }

    @Test void extraUnknownColumnsAreIgnored() throws Exception {
        String text = DemoCsv.header() + ",notes\n" + row() + ",ignored text\n";
        assertEquals(1, importText(text).imported);
    }

    @Test void trailingEmptyHeaderColumnIsTolerated() throws Exception {
        // Some spreadsheet exports append a trailing comma to every line.
        String text = DemoCsv.header() + ",\n" + row() + ",\n";
        assertEquals(1, importText(text).imported);
    }

    // ------------------------------------------------------------------ duplicates / repeated imports

    @Test void repeatedImportSkipsExistingIds() throws Exception {
        var first = importText(csv(row()));
        var second = importText(csv(row()));
        var third = importText(csv(row()));
        assertEquals(1, first.imported);
        assertEquals(0, second.imported); assertEquals(1, second.skipped);
        assertEquals(0, third.imported); assertEquals(1, third.skipped);
        assertEquals(1, new DemoFoodProfileService().findProfiles().size());
        assertEquals(3, count(demoDb(), "food_nutrient_provenance"));
    }

    @Test void repeatedImportOnlyAddsNewRows() throws Exception {
        importText(csv(row()));
        var result = importText(csv(row(), row("demo_id", "D2", "food_name", "Second Food")));
        assertEquals(1, result.imported);
        assertEquals(1, result.skipped);
        assertEquals(2, new DemoFoodProfileService().findProfiles().size());
    }

    @Test void duplicateIdInsideOneFileKeepsTheFirstRecord() throws Exception {
        var result = importText(csv(row("food_name", "First"), row("food_name", "Second")));
        assertEquals(1, result.imported);
        assertEquals(1, result.skipped);
        List<Map<String, String>> profiles = new DemoFoodProfileService().findProfiles();
        assertEquals(1, profiles.size());
        assertEquals("First", profiles.get(0).get("food_name"));
    }

    @Test void duplicateFoodNamesWithDifferentIdsAreBothKept() throws Exception {
        // demo_id is the identity of a demo record; names (e.g. different preparations) may repeat.
        var result = importText(csv(row("demo_id", "D1", "potassium_mg", "100"), row("demo_id", "D2", "potassium_mg", "200")));
        assertEquals(2, result.imported);
        List<Map<String, String>> profiles = new DemoFoodProfileService().findProfiles();
        assertEquals(2, profiles.size());
        assertEquals("100.0", profiles.get(0).get("potassium_mg"));
        assertEquals("200.0", profiles.get(1).get("potassium_mg"));
    }

    @Test void blankIdOrNameIsRejectedAndNothingIsImported() throws Exception {
        IOException noId = assertThrows(IOException.class, () -> importText(csv(row(), row("demo_id", " "))));
        assertTrue(noId.getMessage().contains("row 3"), noId.getMessage());
        IOException noName = assertThrows(IOException.class, () -> importText(csv(row("food_name", ""))));
        assertTrue(noName.getMessage().contains("row 2"), noName.getMessage());
        assertDemoTablesEmpty();
    }

    // ------------------------------------------------------------------ headers

    static Stream<String> requiredHeaders() { return DemoCsv.HEADERS.stream(); }

    @ParameterizedTest @MethodSource("requiredHeaders")
    void missingRequiredColumnIsRejectedByName(String missing) throws Exception {
        int idx = DemoCsv.HEADERS.indexOf(missing);
        List<String> headers = new ArrayList<>(DemoCsv.HEADERS);
        headers.remove(idx);
        List<String> fields = new ArrayList<>();
        for (String h : headers) fields.add(DemoCsv.quote(DemoCsv.defaults().get(h)));
        String text = String.join(",", headers) + "\n" + String.join(",", fields) + "\n";
        IOException e = assertThrows(IOException.class, () -> importText(text));
        assertEquals("Missing required CSV column: " + missing, e.getMessage());
        assertTrue(Files.notExists(demoDb()), "Validation failures must happen before any database is opened.");
    }

    @Test void duplicateRequiredColumnIsRejected() {
        String text = DemoCsv.header() + ",potassium_mg\n" + row() + ",999\n";
        IOException e = assertThrows(IOException.class, () -> importText(text));
        assertTrue(e.getMessage().contains("potassium_mg"), e.getMessage());
        assertTrue(e.getMessage().toLowerCase().contains("duplicate"), e.getMessage());
    }

    // ------------------------------------------------------------------ nutrient values

    @ParameterizedTest @ValueSource(strings = {"abc", "12mg", "1,234", "<0.1", "0x1p3", "12.5f", "12.5d", "1e999", "--5", "1.2.3", "Tr"})
    void invalidNutrientNumbersAreRejectedWithRowColumnAndValue(String bad) throws Exception {
        String text = csv(row(), row("demo_id", "D2", "phosphorus_mg", bad));
        IOException e = assertThrows(IOException.class, () -> importText(text));
        assertTrue(e.getMessage().contains("row 3"), e.getMessage());
        assertTrue(e.getMessage().contains("phosphorus_mg"), e.getMessage());
        assertTrue(e.getMessage().contains(bad), e.getMessage());
        assertDemoTablesEmpty();   // whole file rolled back, including the valid first record
    }

    @ParameterizedTest @ValueSource(strings = {"-1", "-0.5", "-0", "NaN", "nan", "Infinity", "-Infinity", "+Infinity"})
    void negativeNaNAndInfiniteValuesAreRejected(String bad) throws Exception {
        IOException e = assertThrows(IOException.class, () -> importText(csv(row("sodium_mg", bad))));
        assertTrue(e.getMessage().contains("sodium_mg"), e.getMessage());
        assertDemoTablesEmpty();
    }

    @Test void invalidValueInANonMineralColumnIsAlsoRejectedAndRolledBack() throws Exception {
        assertThrows(IOException.class, () -> importText(csv(row(), row("demo_id", "D2", "energy_kcal", "lots"))));
        assertDemoTablesEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"", "   ", "N/A", "n/a", "NA", "na", "null", "NULL"})
    void blankOrNotAvailableNutrientsAreStoredAsUnknownNotZero(String unknown) throws Exception {
        importText(csv(row("potassium_mg", unknown)));
        try (Connection c = open(demoDb()); Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT potassium_mg, phosphorus_mg FROM demo_food_metadata")) {
                assertTrue(r.next());
                r.getDouble(1);
                assertTrue(r.wasNull(), "potassium must be NULL (unknown), not 0");
                assertEquals(222.0, r.getDouble(2));
            }
            try (ResultSet r = s.executeQuery("SELECT value FROM food_nutrient_provenance WHERE nutrient='potassium_mg'")) {
                assertTrue(r.next());
                r.getDouble(1);
                assertTrue(r.wasNull());
            }
        }
        assertEquals("—", new DemoFoodProfileService().findProfiles().get(0).get("potassium_mg"));
    }

    @Test void zeroIsAValueNotUnknown() throws Exception {
        importText(csv(row("potassium_mg", "0", "sodium_mg", "0.0")));
        Map<String, String> p = new DemoFoodProfileService().findProfiles().get(0);
        assertEquals("0.0", p.get("potassium_mg"));
        assertEquals("0.0", p.get("sodium_mg"));
    }

    @Test void scientificAndLeadingPlusNotationIsAcceptedForPlainNumbers() throws Exception {
        importText(csv(row("potassium_mg", "1.5e2", "phosphorus_mg", "+20", "sodium_mg", ".5")));
        Map<String, String> p = new DemoFoodProfileService().findProfiles().get(0);
        assertEquals("150.0", p.get("potassium_mg"));
        assertEquals("20.0", p.get("phosphorus_mg"));
        assertEquals("0.5", p.get("sodium_mg"));
    }

    @Test void blankTextFieldsAreStoredAsNullEverywhere() throws Exception {
        importText(csv(row("data_source", "", "citation", " ", "license_note", "")));
        try (Connection c = open(demoDb()); Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT source_name, citation, license_note FROM demo_food_metadata")) {
                assertTrue(r.next());
                assertNull(r.getString(1)); assertNull(r.getString(2)); assertNull(r.getString(3));
            }
            try (ResultSet r = s.executeQuery("SELECT source_name, citation, license_note FROM food_nutrient_provenance")) {
                int rows = 0;
                while (r.next()) {
                    rows++;
                    assertNull(r.getString(1), "provenance must use NULL, not an empty string"); assertNull(r.getString(2)); assertNull(r.getString(3));
                }
                assertEquals(3, rows);
            }
        }
    }

    // ------------------------------------------------------------------ CSV syntax

    @Test void quotedFieldsWithCommasAndEscapedQuotes() throws Exception {
        importText(csv(row("food_name", "Rice, white", "citation", "He said \"hi\", twice")));
        Map<String, String> p = new DemoFoodProfileService().findProfiles().get(0);
        assertEquals("Rice, white", p.get("food_name"));
        assertEquals("He said \"hi\", twice", p.get("citation"));
    }

    @Test void multilineQuotedFieldIsOneRecord() throws Exception {
        var result = importText(csv(row("citation", "line 1\nline 2"), row("demo_id", "D2")));
        assertEquals(2, result.imported);
        List<Map<String, String>> profiles = new DemoFoodProfileService().findProfiles();
        assertEquals(2, profiles.size());
        Map<String, String> d1 = profiles.stream().filter(m -> "line 1\nline 2".equals(m.get("citation"))).findFirst().orElse(null);
        assertNotNull(d1, "multiline citation was split or altered");
    }

    @Test void windowsLineEndingsAreAccepted() throws Exception {
        String text = (DemoCsv.header() + "\n" + row() + "\n" + row("demo_id", "D2") + "\n").replace("\n", "\r\n");
        assertEquals(2, importText(text).imported);
    }

    @Test void fileWithoutTrailingNewlineIsAccepted() throws Exception {
        assertEquals(1, importText(DemoCsv.header() + "\n" + row()).imported);
    }

    @Test void blankLinesBetweenRecordsAreIgnored() throws Exception {
        String text = DemoCsv.header() + "\n\n" + row() + "\n\n\n" + row("demo_id", "D2") + "\n\n";
        assertEquals(2, importText(text).imported);
    }

    @Test void utf8ByteOrderMarkIsAccepted() throws Exception {
        byte[] body = csv(row()).getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF; withBom[1] = (byte) 0xBB; withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);
        assertEquals(1, new DemoFoodImporter().importCsv(new ByteArrayInputStream(withBom)).imported);
        assertEquals(DemoCsv.TAMIL, new DemoFoodProfileService().findProfiles().get(0).get("local_name"));
    }

    @Test void invalidUtf8IsRejectedInsteadOfSilentlyCorrupted() throws Exception {
        byte[] head = (DemoCsv.header() + "\n" + row("citation", "caf")).getBytes(StandardCharsets.UTF_8);
        byte[] tail = "\n".getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[head.length + 1 + tail.length];
        System.arraycopy(head, 0, bytes, 0, head.length);
        bytes[head.length] = (byte) 0xE9;                 // 'e-acute' in Windows-1252 / Latin-1, invalid as UTF-8
        System.arraycopy(tail, 0, bytes, head.length + 1, tail.length);
        IOException e = assertThrows(IOException.class, () -> new DemoFoodImporter().importCsv(new ByteArrayInputStream(bytes)));
        assertTrue(e.getMessage().contains("UTF-8"), e.getMessage());
        assertTrue(Files.notExists(demoDb()));
    }

    @Test void emptyInputIsRejected() {
        IOException e = assertThrows(IOException.class, () -> new DemoFoodImporter().importCsv(new ByteArrayInputStream(new byte[0])));
        assertEquals("CSV file is empty.", e.getMessage());
        IOException bomOnly = assertThrows(IOException.class,
                () -> new DemoFoodImporter().importCsv(new ByteArrayInputStream(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF})));
        assertEquals("CSV file is empty.", bomOnly.getMessage());
    }

    @Test void whitespaceOnlyInputIsRejected() {
        assertThrows(IOException.class, () -> importText("   \n  \n"));
    }

    @Test void nullStreamIsRejected() {
        assertThrows(FileNotFoundException.class, () -> new DemoFoodImporter().importCsv(null));
    }

    @Test void headerOnlyFileImportsNothing() throws Exception {
        var result = importText(DemoCsv.header() + "\n");
        assertEquals(0, result.imported);
        assertEquals(0, result.skipped);
        assertTrue(new DemoFoodProfileService().findProfiles().isEmpty());
    }

    @Test void semicolonDelimitedFileIsRejectedWithAClearColumnError() {
        String text = DemoCsv.header().replace(',', ';') + "\n" + row().replace(',', ';') + "\n";
        IOException e = assertThrows(IOException.class, () -> importText(text));
        assertTrue(e.getMessage().startsWith("Missing required CSV column"), e.getMessage());
    }

    @Test void tooFewFieldsReportsTheRecordNumber() throws Exception {
        String shortRow = row("demo_id", "D2");
        shortRow = shortRow.substring(0, shortRow.lastIndexOf(','));
        String text = csv(row(), shortRow);
        IOException e = assertThrows(IOException.class, () -> importText(text));
        assertTrue(e.getMessage().contains("row 3"), e.getMessage());
        assertTrue(e.getMessage().contains("21 fields"), e.getMessage());
        assertDemoTablesEmpty();
    }

    @Test void tooManyFieldsIsRejected() {
        String text = csv(row() + ",extra");
        IOException e = assertThrows(IOException.class, () -> importText(text));
        assertTrue(e.getMessage().contains("23 fields"), e.getMessage());
    }

    @Test void unclosedQuoteIsRejectedBeforeTheDatabaseIsOpened() {
        // The opening quote is written by hand because row() only quotes when needed.
        String broken = DemoCsv.header() + "\nD1,Test Food,x,Demo,Boiled,per 100g,1,1,1,1,1,1,1,1,1,1,1,src,id,SYNTHETIC,\"never closed,lic\n";
        IOException e = assertThrows(IOException.class, () -> importText(broken));
        assertTrue(e.getMessage().toLowerCase().contains("quote"), e.getMessage());
        assertTrue(Files.notExists(demoDb()));
    }

    // ------------------------------------------------------------------ rollback

    @Test void failureOnALaterRowRollsBackEarlierRows() throws Exception {
        String text = csv(row("demo_id", "D1"), row("demo_id", "D2"), row("demo_id", "D3", "potassium_mg", "not-a-number"));
        IOException e = assertThrows(IOException.class, () -> importText(text));
        assertTrue(e.getMessage().contains("row 4") && e.getMessage().contains("potassium_mg"), e.getMessage());
        assertDemoTablesEmpty();
        assertTrue(new DemoFoodProfileService().findProfiles().isEmpty());
    }

    @Test void importCanBeRetriedAfterAFailedAttempt() throws Exception {
        assertThrows(IOException.class, () -> importText(csv(row("demo_id", "D1"), row("demo_id", "D2", "sodium_mg", "x"))));
        var result = importText(csv(row("demo_id", "D1"), row("demo_id", "D2")));
        assertEquals(2, result.imported);
        assertEquals(0, result.skipped);
    }
}
