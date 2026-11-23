package com.rahulshankar.dialysisapp.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static com.rahulshankar.dialysisapp.service.DemoCsv.csv;
import static com.rahulshankar.dialysisapp.service.DemoCsv.row;
import static com.rahulshankar.dialysisapp.service.DemoCsv.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoFoodDatabaseTest extends HomeIsolatedTest {

    private static List<String> columns(Connection c, String table) throws SQLException {
        List<String> cols = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (r.next()) cols.add(r.getString("name"));
        }
        return cols;
    }

    /** Creates a demo database in the shape produced by the first demo-importer revision (no K/P/Na columns on the metadata table). */
    private void createLegacyDemoDatabase() throws Exception {
        Files.createDirectories(dataDir());
        try (Connection c = open(demoDb()); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE demo_imported_foods (demo_id TEXT PRIMARY KEY, food_name TEXT NOT NULL, imported_at TEXT DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE demo_food_metadata (demo_id TEXT PRIMARY KEY, food_name TEXT NOT NULL, local_name TEXT, category TEXT, preparation TEXT, basis TEXT, "
                    + "energy_kcal REAL, protein_g REAL, carbohydrates_g REAL, fat_g REAL, fibre_g REAL, calcium_mg REAL, magnesium_mg REAL, iron_mg REAL, "
                    + "source_name TEXT, source_record_id TEXT, data_status TEXT, citation TEXT, license_note TEXT)");
            s.execute("CREATE TABLE food_nutrient_provenance (demo_id TEXT NOT NULL, food_name TEXT NOT NULL, nutrient TEXT NOT NULL, value REAL, unit TEXT NOT NULL, "
                    + "source_name TEXT, source_record_id TEXT, data_status TEXT, citation TEXT, license_note TEXT, preparation TEXT, local_name TEXT, basis TEXT, PRIMARY KEY(demo_id,nutrient))");
            s.execute("INSERT INTO demo_imported_foods(demo_id, food_name) VALUES('L1','Legacy Food'),('L2','Legacy Unknown')");
            s.execute("INSERT INTO demo_food_metadata(demo_id, food_name, category, data_status) VALUES('L1','Legacy Food','Demo','SYNTHETIC'),('L2','Legacy Unknown','Demo','SYNTHETIC')");
            s.execute("INSERT INTO food_nutrient_provenance(demo_id, food_name, nutrient, value, unit) VALUES "
                    + "('L1','Legacy Food','potassium_mg',150,'mg'),('L1','Legacy Food','phosphorus_mg',60,'mg'),('L1','Legacy Food','sodium_mg',7,'mg'),"
                    + "('L2','Legacy Unknown','potassium_mg',NULL,'mg'),('L2','Legacy Unknown','phosphorus_mg',12,'mg'),('L2','Legacy Unknown','sodium_mg',NULL,'mg')");
        }
        assertEquals(0, userVersion(demoDb()));
    }

    // ------------------------------------------------------------------ schema creation / versioning

    @Test void freshDatabaseHasTheFullSchemaAndIsMarkedAsCurrent() throws Exception {
        DemoFoodDatabase.connect().close();
        assertTrue(Files.exists(demoDb()));
        assertEquals(List.of("demo_food_metadata", "demo_imported_foods", "food_nutrient_provenance"), tableNames(demoDb()));
        try (Connection c = open(demoDb())) {
            List<String> cols = columns(c, "demo_food_metadata");
            assertTrue(cols.containsAll(List.of("potassium_mg", "phosphorus_mg", "sodium_mg", "calcium_mg", "magnesium_mg", "iron_mg")), cols.toString());
        }
        assertEquals(1, userVersion(demoDb()));
    }

    @Test void openingAnUpToDateDatabaseDoesNotWriteToIt() throws Exception {
        DemoFoodDatabase.connect().close();
        String before = sha256(demoDb());
        for (int i = 0; i < 3; i++) DemoFoodDatabase.connect().close();
        new DemoFoodProfileService().findProfiles();
        assertEquals(before, sha256(demoDb()), "read-only use (viewer, repeated connects) must not modify the database file");
    }

    @Test void viewerCanReadWhileAnImportTransactionHoldsTheWriteLock() throws Exception {
        DemoFoodDatabase.connect().close();           // database exists and is up to date
        try (Connection writer = open(demoDb()); Statement s = writer.createStatement()) {
            s.execute("BEGIN IMMEDIATE");             // what a long-running import holds
            long started = System.nanoTime();
            List<java.util.Map<String, String>> rows = new DemoFoodProfileService().findProfiles();
            long millis = (System.nanoTime() - started) / 1_000_000;
            assertTrue(rows.isEmpty());
            assertTrue(millis < 2_000, "opening the viewer blocked on the write lock for " + millis + " ms");
            s.execute("ROLLBACK");
        }
    }

    // ------------------------------------------------------------------ migration

    @Test void legacyDemoDatabaseIsUpgradedAndNutrientValuesAreBackfilled() throws Exception {
        createLegacyDemoDatabase();

        List<java.util.Map<String, String>> profiles = new DemoFoodProfileService().findProfiles();

        assertEquals(2, profiles.size());
        java.util.Map<String, String> legacy = profiles.stream().filter(m -> "Legacy Food".equals(m.get("food_name"))).findFirst().orElseThrow();
        assertEquals("150.0", legacy.get("potassium_mg"));
        assertEquals("60.0", legacy.get("phosphorus_mg"));
        assertEquals("7.0", legacy.get("sodium_mg"));
        java.util.Map<String, String> unknown = profiles.stream().filter(m -> "Legacy Unknown".equals(m.get("food_name"))).findFirst().orElseThrow();
        assertEquals("—", unknown.get("potassium_mg"), "unknown must stay unknown, not become 0");
        assertEquals("12.0", unknown.get("phosphorus_mg"));
        assertEquals("—", unknown.get("sodium_mg"));
        assertEquals(1, userVersion(demoDb()));
        assertEquals(2, count(demoDb(), "demo_imported_foods"), "migration must not drop or duplicate records");
        assertEquals(6, count(demoDb(), "food_nutrient_provenance"));
    }

    @Test void migrationIsIdempotentAndImportingWorksAfterwards() throws Exception {
        createLegacyDemoDatabase();
        DemoFoodDatabase.connect().close();
        DemoFoodDatabase.connect().close();
        assertEquals(1, userVersion(demoDb()));
        var result = new DemoFoodImporter().importCsv(stream(csv(row("demo_id", "N1"))));
        assertEquals(1, result.imported);
        assertEquals(3, new DemoFoodProfileService().findProfiles().size());
        assertEquals(3, count(demoDb(), "demo_imported_foods"));
    }

    @Test void migrationNeverTouchesTheCatalogueDatabase() throws Exception {
        createLegacyDemoDatabase();
        DemoFoodDatabase.connect().close();
        assertTrue(Files.notExists(catalogueDb()));
    }

    // ------------------------------------------------------------------ connection failures

    @Test void unwritableDataDirectoryIsReportedAsSqlException() throws Exception {
        Files.writeString(dataDir(), "this is a file where the directory should be");
        SQLException e = assertThrows(SQLException.class, DemoFoodDatabase::connect);
        assertTrue(e.getMessage().contains("Unable to create demo data directory"), e.getMessage());
    }

    @Test void importerSurfacesConnectionFailureAndLeavesNoDatabaseBehind() throws Exception {
        Files.writeString(dataDir(), "blocked");
        assertThrows(SQLException.class, () -> new DemoFoodImporter().importCsv(stream(csv(row()))));
        assertTrue(Files.isRegularFile(dataDir()), "the blocking file must be untouched");
    }

    @Test void viewerAndDeleteAllSurfaceConnectionFailure() throws Exception {
        Files.writeString(dataDir(), "blocked");
        assertThrows(SQLException.class, () -> new DemoFoodProfileService().findProfiles());
        assertThrows(SQLException.class, () -> new DemoFoodProfileService().deleteAllDemoProfiles());
    }

    @Test void corruptDatabaseFileIsReportedAndTheApplicationRecoversOnceItIsRemoved() throws Exception {
        Files.createDirectories(dataDir());
        Files.write(demoDb(), "this is definitely not an sqlite database file ".repeat(40).getBytes(StandardCharsets.UTF_8));
        assertThrows(SQLException.class, DemoFoodDatabase::connect);
        assertThrows(SQLException.class, () -> new DemoFoodImporter().importCsv(stream(csv(row()))));

        Files.delete(demoDb());                       // would fail on some platforms if a connection had been leaked
        assertEquals(1, new DemoFoodImporter().importCsv(stream(csv(row()))).imported);
        assertFalse(new DemoFoodProfileService().findProfiles().isEmpty());
    }

    @Test void connectionsAreIndependentAndCloseCleanly() throws Exception {
        Connection a = DemoFoodDatabase.connect();
        Connection b = DemoFoodDatabase.connect();
        assertNotEquals(a, b);
        a.close();
        b.close();
        assertTrue(a.isClosed() && b.isClosed());
    }
}
