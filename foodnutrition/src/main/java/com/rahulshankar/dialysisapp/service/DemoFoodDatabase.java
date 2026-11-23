package com.rahulshankar.dialysisapp.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Storage for synthetic/demo food records. Deliberately separate from the
 * production food catalogue database used by the nutrition calculator.
 */
public final class DemoFoodDatabase {
    private DemoFoodDatabase() { }

    public static Connection connect() throws SQLException {
        Path directory = Path.of(System.getProperty("user.home"), ".foodnutrition");
        try {
            Files.createDirectories(directory);
        } catch (Exception e) {
            throw new SQLException("Unable to create demo data directory: " + directory, e);
        }

        Path database = directory.resolve("foodnutrition-demo.db");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
        try {
            initialize(connection);
            return connection;
        } catch (SQLException e) {
            try { connection.close(); } catch (SQLException closeError) { e.addSuppressed(closeError); }
            throw e;
        }
    }

    /** Schema version stored in {@code PRAGMA user_version}. Bump it and extend {@link #migrate} for every schema change. */
    static final int SCHEMA_VERSION = 1;

    /**
     * Brings the database up to {@link #SCHEMA_VERSION}. An up-to-date database is left completely untouched, so opening
     * a connection (for example from the profile viewer) never takes a write lock while an import is running. The
     * migration itself runs in one transaction, so a failure cannot leave a half-upgraded schema behind.
     */
    private static void initialize(Connection connection) throws SQLException {
        if (userVersion(connection) >= SCHEMA_VERSION) return;
        connection.setAutoCommit(false);
        try {
            migrate(connection);
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            try { connection.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static int userVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS demo_imported_foods (" +
                    "demo_id TEXT PRIMARY KEY, food_name TEXT NOT NULL, " +
                    "imported_at TEXT DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS demo_food_metadata (" +
                    "demo_id TEXT PRIMARY KEY, food_name TEXT NOT NULL, local_name TEXT, " +
                    "category TEXT, preparation TEXT, basis TEXT, energy_kcal REAL, " +
                    "protein_g REAL, carbohydrates_g REAL, fat_g REAL, fibre_g REAL, " +
                    "potassium_mg REAL, phosphorus_mg REAL, sodium_mg REAL, " +
                    "calcium_mg REAL, magnesium_mg REAL, iron_mg REAL, " +
                    "source_name TEXT, source_record_id TEXT, data_status TEXT, " +
                    "citation TEXT, license_note TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS food_nutrient_provenance (" +
                    "demo_id TEXT NOT NULL, food_name TEXT NOT NULL, nutrient TEXT NOT NULL, " +
                    "value REAL, unit TEXT NOT NULL, source_name TEXT, source_record_id TEXT, " +
                    "data_status TEXT, citation TEXT, license_note TEXT, preparation TEXT, " +
                    "local_name TEXT, basis TEXT, PRIMARY KEY(demo_id,nutrient))");
        }

        // Upgrade databases created by the first demo-importer revision.
        ensureColumn(connection, "potassium_mg");
        ensureColumn(connection, "phosphorus_mg");
        ensureColumn(connection, "sodium_mg");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE demo_food_metadata SET potassium_mg = " +
                    "(SELECT value FROM food_nutrient_provenance p WHERE p.demo_id=demo_food_metadata.demo_id " +
                    "AND p.nutrient='potassium_mg') WHERE potassium_mg IS NULL");
            statement.executeUpdate("UPDATE demo_food_metadata SET phosphorus_mg = " +
                    "(SELECT value FROM food_nutrient_provenance p WHERE p.demo_id=demo_food_metadata.demo_id " +
                    "AND p.nutrient='phosphorus_mg') WHERE phosphorus_mg IS NULL");
            statement.executeUpdate("UPDATE demo_food_metadata SET sodium_mg = " +
                    "(SELECT value FROM food_nutrient_provenance p WHERE p.demo_id=demo_food_metadata.demo_id " +
                    "AND p.nutrient='sodium_mg') WHERE sodium_mg IS NULL");
            statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
        }
    }

    private static void ensureColumn(Connection connection, String column) throws SQLException {
        boolean found = false;
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(demo_food_metadata)")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE demo_food_metadata ADD COLUMN " + column + " REAL");
            }
        }
    }
}
