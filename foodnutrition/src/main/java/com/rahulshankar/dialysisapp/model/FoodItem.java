package com.rahulshankar.dialysisapp.model;

public class FoodItem {
    private String foodName;
    private String category;
    private int phosphorus;
    private int potassium;
    private int sodium;

    public FoodItem(String foodName, String category, int phosphorus, int potassium, int sodium) {
        this.foodName = foodName;
        this.category = category;
        this.phosphorus = phosphorus;
        this.potassium = potassium;
        this.sodium = sodium;
    }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPhosphorus() { return phosphorus; }
    public void setPhosphorus(int phosphorus) { this.phosphorus = phosphorus; }

    public int getPotassium() { return potassium; }
    public void setPotassium(int potassium) { this.potassium = potassium; }

    public int getSodium() { return sodium; }
    public void setSodium(int sodium) { this.sodium = sodium; }
}
