package com.rahulshankar.dialysisapp.service;

import com.rahulshankar.dialysisapp.model.FoodItem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Local SQLite catalogue stored in the current user's home directory. */
public class FoodCatalogService {
    private final String jdbcUrl;

    public FoodCatalogService() throws SQLException {
        Path directory = Path.of(System.getProperty("user.home"), ".foodnutrition");
        try {
            Files.createDirectories(directory);
        } catch (Exception e) {
            throw new SQLException("Unable to create application data directory: " + directory, e);
        }
        Path database = directory.resolve("foodnutrition.db");
        jdbcUrl = "jdbc:sqlite:" + database.toAbsolutePath();
        initialize();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initialize() throws SQLException {
        String create = "CREATE TABLE IF NOT EXISTS food_items (" +
                "name TEXT PRIMARY KEY COLLATE NOCASE, " +
                "category TEXT NOT NULL DEFAULT 'Other', " +
                "potassium_mg REAL NULL, phosphorus_mg REAL NULL, sodium_mg REAL NULL)";
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute(create);
            // Preserve original project values. Unknown phosphorus and sodium stay NULL.
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT OR IGNORE INTO food_items(name, category, potassium_mg, phosphorus_mg, sodium_mg) VALUES(?, ?, ?, NULL, NULL)")) {
                seed(insert, "Onion", "Vegetable", 146);
                seed(insert, "Tomato", "Vegetable", 237);
                seed(insert, "Potato", "Vegetable", 429);
            }
        }
    }

    private void seed(PreparedStatement statement, String name, String category, double potassium)
            throws SQLException {
        statement.setString(1, name);
        statement.setString(2, category);
        statement.setDouble(3, potassium);
        statement.executeUpdate();
    }

    public List<FoodItem> findAll() throws SQLException {
        List<FoodItem> foods = new ArrayList<>();
        String sql = "SELECT name, category, potassium_mg, phosphorus_mg, sodium_mg " +
                "FROM food_items ORDER BY name COLLATE NOCASE";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                foods.add(new FoodItem(result.getString("name"), result.getString("category"),
                        nullableDouble(result, "potassium_mg"),
                        nullableDouble(result, "phosphorus_mg"),
                        nullableDouble(result, "sodium_mg")));
            }
        }
        return foods;
    }

    public void save(FoodItem food) throws SQLException {
        String sql = "INSERT INTO food_items(name, category, potassium_mg, phosphorus_mg, sodium_mg) " +
                "VALUES(?, ?, ?, ?, ?) ON CONFLICT(name) DO UPDATE SET " +
                "category=excluded.category, potassium_mg=excluded.potassium_mg, " +
                "phosphorus_mg=excluded.phosphorus_mg, sodium_mg=excluded.sodium_mg";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, food.getName());
            statement.setString(2, food.getCategory());
            setNullableDouble(statement, 3, food.getPotassiumMgPer100g());
            setNullableDouble(statement, 4, food.getPhosphorusMgPer100g());
            setNullableDouble(statement, 5, food.getSodiumMgPer100g());
            statement.executeUpdate();
        }
    }

    public void delete(String name) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM food_items WHERE name=?")) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    private static Double nullableDouble(ResultSet result, String column) throws SQLException {
        double value = result.getDouble(column);
        return result.wasNull() ? null : value;
    }

    private static void setNullableDouble(PreparedStatement statement, int index, Double value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.REAL);
        else statement.setDouble(index, value);
    }
}