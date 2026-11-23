package com.rahulshankar.dialysisapp.service;

import com.rahulshankar.dialysisapp.model.FoodItem;
import com.rahulshankar.dialysisapp.model.IngredientEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.rahulshankar.dialysisapp.service.DemoCsv.csv;
import static com.rahulshankar.dialysisapp.service.DemoCsv.row;
import static com.rahulshankar.dialysisapp.service.DemoCsv.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Demo data must stay in foodnutrition-demo.db; the calculator catalogue (foodnutrition.db) must be unaffected by it. */
class DemoFoodIsolationTest extends HomeIsolatedTest {

    /** Comparable snapshot of the catalogue contents. */
    private static List<String> snapshot(FoodCatalogService catalogue) throws Exception {
        List<String> out = new ArrayList<>();
        for (FoodItem f : catalogue.findAll()) {
            out.add(f.getName() + "|" + f.getCategory() + "|" + f.getPotassiumMgPer100g() + "|" + f.getPhosphorusMgPer100g() + "|" + f.getSodiumMgPer100g());
        }
        return out;
    }

    private static String importDemoThatCollidesWithRealFoods() {
        // "Onion" and "onion" collide (case-insensitively) with a seeded catalogue food; "Real Food" with a user-added one.
        return csv(
                row("demo_id", "D1", "food_name", "Onion", "potassium_mg", "9991", "phosphorus_mg", "9992", "sodium_mg", "9993"),
                row("demo_id", "D2", "food_name", "onion", "potassium_mg", "8881"),
                row("demo_id", "D3", "food_name", "Real Food", "potassium_mg", "7771"),
                row("demo_id", "D4", "food_name", "Demo Only Food", "potassium_mg", "6661"));
    }

    @Test void importWithoutACatalogueNeverCreatesTheCatalogueDatabase() throws Exception {
        new DemoFoodImporter().importCsv(stream(csv(row())));
        assertTrue(Files.exists(demoDb()));
        assertTrue(Files.notExists(catalogueDb()), "the demo import must not create or open foodnutrition.db");
    }

    @Test void importLeavesAnExistingCatalogueByteForByteUntouched() throws Exception {
        FoodCatalogService catalogue = new FoodCatalogService();
        catalogue.save(new FoodItem("Real Food", "Mine", 12.5, null, 3.0));
        String before = sha256(catalogueDb());

        var result = new DemoFoodImporter().importCsv(stream(importDemoThatCollidesWithRealFoods()));

        assertEquals(4, result.imported);
        assertEquals(before, sha256(catalogueDb()), "foodnutrition.db was modified by the demo import");
    }

    @Test void demoFoodsNeverAppearInTheCalculatorCatalogue() throws Exception {
        FoodCatalogService catalogue = new FoodCatalogService();
        catalogue.save(new FoodItem("Real Food", "Mine", 12.5, null, 3.0));
        List<String> before = snapshot(catalogue);

        new DemoFoodImporter().importCsv(stream(importDemoThatCollidesWithRealFoods()));

        List<String> after = snapshot(new FoodCatalogService());
        assertEquals(before, after);
        assertFalse(after.stream().anyMatch(s -> s.startsWith("Demo Only Food")));
        assertTrue(after.contains("Onion|Vegetable|146.0|null|null"), "seeded Onion must keep its original value: " + after);
        assertTrue(after.contains("Real Food|Mine|12.5|null|3.0"));
        assertTrue(after.stream().noneMatch(s -> s.contains("9991") || s.contains("8881") || s.contains("7771") || s.contains("6661")));
    }

    @Test void catalogueDatabaseNeverGetsDemoTables() throws Exception {
        new FoodCatalogService();
        new DemoFoodImporter().importCsv(stream(csv(row())));
        new DemoFoodProfileService().findProfiles();
        assertEquals(List.of("food_items"), tableNames(catalogueDb()));
    }

    @Test void demoValuesAreStoredOnlyInTheDemoDatabase() throws Exception {
        new FoodCatalogService();
        new DemoFoodImporter().importCsv(stream(importDemoThatCollidesWithRealFoods()));
        assertEquals(4, count(demoDb(), "demo_imported_foods"));
        assertEquals(4, count(demoDb(), "demo_food_metadata"));
        assertEquals(12, count(demoDb(), "food_nutrient_provenance"));
        assertEquals(3, count(catalogueDb(), "food_items"), "only the three seeded foods");
        // Same-named demo and catalogue foods stay independent records.
        try (Connection c = open(demoDb()); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM demo_food_metadata WHERE food_name IN ('Onion','onion')")) {
            r.next();
            assertEquals(2, r.getInt(1));
        }
    }

    @Test void deleteAllRemovesDemoDataButNotTheCatalogue() throws Exception {
        FoodCatalogService catalogue = new FoodCatalogService();
        catalogue.save(new FoodItem("Real Food", "Mine", 12.5, null, 3.0));
        List<String> before = snapshot(catalogue);
        String catalogueHash = sha256(catalogueDb());
        new DemoFoodImporter().importCsv(stream(importDemoThatCollidesWithRealFoods()));

        int removed = new DemoFoodProfileService().deleteAllDemoProfiles();

        assertEquals(4, removed);
        assertEquals(0, count(demoDb(), "demo_imported_foods"));
        assertEquals(0, count(demoDb(), "demo_food_metadata"));
        assertEquals(0, count(demoDb(), "food_nutrient_provenance"));
        assertTrue(new DemoFoodProfileService().findProfiles().isEmpty());
        assertEquals(before, snapshot(new FoodCatalogService()));
        assertEquals(catalogueHash, sha256(catalogueDb()));
    }

    @Test void deleteAllOnAnEmptyDemoDatabaseReturnsZero() throws Exception {
        assertEquals(0, new DemoFoodProfileService().deleteAllDemoProfiles());
    }

    @Test void demoDataCanBeReimportedAfterDeleteAll() throws Exception {
        new DemoFoodImporter().importCsv(stream(csv(row())));
        new DemoFoodProfileService().deleteAllDemoProfiles();
        var again = new DemoFoodImporter().importCsv(stream(csv(row())));
        assertEquals(1, again.imported);
        assertEquals(0, again.skipped);
    }

    // ------------------------------------------------------------------ regression: existing features

    @Test void catalogueSeedsThreeFoodsOnceAndPreservesUnknownValues() throws Exception {
        new FoodCatalogService();
        FoodCatalogService catalogue = new FoodCatalogService();   // second start-up must not duplicate or overwrite seeds
        List<FoodItem> foods = catalogue.findAll();
        assertEquals(List.of("Onion", "Potato", "Tomato"), foods.stream().map(FoodItem::getName).toList());
        for (FoodItem f : foods) {
            assertNull(f.getPhosphorusMgPer100g(), f.getName());
            assertNull(f.getSodiumMgPer100g(), f.getName());
        }
    }

    @Test void catalogueSaveUpdateDeleteStillWork() throws Exception {
        FoodCatalogService catalogue = new FoodCatalogService();
        catalogue.save(new FoodItem("Carrot", "Vegetable", 320.0, 35.0, null));
        catalogue.save(new FoodItem("carrot", "Veg", 321.0, null, 69.0));     // case-insensitive key: update, not insert
        List<FoodItem> carrots = catalogue.findAll().stream().filter(f -> f.getName().equalsIgnoreCase("carrot")).toList();
        assertEquals(1, carrots.size());
        assertEquals("Veg", carrots.get(0).getCategory());
        assertEquals(321.0, carrots.get(0).getPotassiumMgPer100g());
        assertNull(carrots.get(0).getPhosphorusMgPer100g(), "update must be able to set a value back to unknown");
        assertEquals(69.0, carrots.get(0).getSodiumMgPer100g());
        catalogue.delete("Carrot");
        assertTrue(catalogue.findAll().stream().noneMatch(f -> f.getName().equalsIgnoreCase("carrot")));
        assertEquals(3, catalogue.findAll().size());
    }

    @Test void ingredientMathAndUnknownHandlingAreUnchanged() {
        FoodItem tomato = new FoodItem("Tomato", "Vegetable", 237.0, null, 5.0);
        IngredientEntry e = new IngredientEntry(tomato, 150);
        assertEquals(355.5, Objects.requireNonNull(e.getPotassiumMg()), 1e-9);
        assertNull(e.getPhosphorusMg(), "unknown per-100g value must stay unknown (null), never 0");
        assertEquals(7.5, Objects.requireNonNull(e.getSodiumMg()), 1e-9);
    }
}
