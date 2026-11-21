package com.rahulshankar.dialysisapp.model;

/** Nutrient composition per 100 grams. Null means unknown, not zero. */
public class FoodItem {
    private final String name;
    private final String category;
    private final Double potassiumMgPer100g;
    private final Double phosphorusMgPer100g;
    private final Double sodiumMgPer100g;

    public FoodItem(String name, String category, Double potassiumMgPer100g,
                    Double phosphorusMgPer100g, Double sodiumMgPer100g) {
        this.name = name;
        this.category = category;
        this.potassiumMgPer100g = potassiumMgPer100g;
        this.phosphorusMgPer100g = phosphorusMgPer100g;
        this.sodiumMgPer100g = sodiumMgPer100g;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public Double getPotassiumMgPer100g() { return potassiumMgPer100g; }
    public Double getPhosphorusMgPer100g() { return phosphorusMgPer100g; }
    public Double getSodiumMgPer100g() { return sodiumMgPer100g; }

    // Compatibility getters for earlier code that used FoodItem.
    public String getFoodName() { return name; }
    public int getPotassium() { return potassiumMgPer100g == null ? 0 : potassiumMgPer100g.intValue(); }
    public int getPhosphorus() { return phosphorusMgPer100g == null ? 0 : phosphorusMgPer100g.intValue(); }
    public int getSodium() { return sodiumMgPer100g == null ? 0 : sodiumMgPer100g.intValue(); }

    @Override
    public String toString() { return name; }
}