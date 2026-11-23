package com.rahulshankar.dialysisapp.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Base class that points {@code user.home} at a fresh temporary directory so that tests never touch the
 * developer's real {@code ~/.foodnutrition} databases. Tests using it must not run in parallel.
 */
abstract class HomeIsolatedTest {
    @TempDir Path home;
    private String originalHome;

    @BeforeEach
    void useTemporaryHome() {
        originalHome = System.getProperty("user.home");
        System.setProperty("user.home", home.toString());
    }

    @AfterEach
    void restoreHome() {
        if (originalHome == null) System.clearProperty("user.home");
        else System.setProperty("user.home", originalHome);
    }

    Path dataDir() { return home.resolve(".foodnutrition"); }
    Path catalogueDb() { return dataDir().resolve("foodnutrition.db"); }
    Path demoDb() { return dataDir().resolve("foodnutrition-demo.db"); }

    /** Opens a plain JDBC connection (bypassing the application classes) for assertions. */
    static Connection open(Path db) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + db.toAbsolutePath());
    }

    static long count(Path db, String table) throws SQLException {
        try (Connection c = open(db); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            r.next();
            return r.getLong(1);
        }
    }

    static List<String> tableNames(Path db) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection c = open(db); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")) {
            while (r.next()) names.add(r.getString(1));
        }
        return names;
    }

    static int userVersion(Path db) throws SQLException {
        try (Connection c = open(db); Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA user_version")) {
            r.next();
            return r.getInt(1);
        }
    }

    static String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
}
