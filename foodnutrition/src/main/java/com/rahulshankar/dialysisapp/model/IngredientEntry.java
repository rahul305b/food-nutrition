package com.rahulshankar.dialysisapp.model;

/** A food and the weight used in the current dish. */
public class IngredientEntry {
    private final FoodItem food;
    private final int weightGrams;

    public IngredientEntry(FoodItem food, int weightGrams) {
        this.food = food;
        this.weightGrams = weightGrams;
    }

    public String getName() { return food.getName(); }
    public int getWeightGrams() { return weightGrams; }

    public Double getPotassiumMg() {
        return calculate(food.getPotassiumMgPer100g());
    }

    public Double getPhosphorusMg() {
        return calculate(food.getPhosphorusMgPer100g());
    }

    public Double getSodiumMg() {
        return calculate(food.getSodiumMgPer100g());
    }

    private Double calculate(Double per100g) {
        return per100g == null ? null : per100g * weightGrams / 100.0;
    }
}