package com.rahulshankar.dialysisapp.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class FoodPreparationController {

    @FXML private TextField dishNameField;
    @FXML private ComboBox<String> ingredientComboBox;
    @FXML private TextField weightField;
    @FXML private TableView<Ingredient> ingredientTableView;
    @FXML private TableColumn<Ingredient, String> ingredientColumn;
    @FXML private TableColumn<Ingredient, Integer> weightColumn;
    @FXML private TableColumn<Ingredient, Integer> potassiumColumn;
    @FXML private Button addIngredientButton;
    @FXML private Button submitButton;
    @FXML private Button deleteIngredientButton;
    @FXML private Label totalPotassiumLabel;

    private final ObservableList<Ingredient> ingredients = FXCollections.observableArrayList();
    private final Map<String, Integer> potassiumValues = new HashMap<>();

    public void initialize() {
        // Default potassium values per 100g
        potassiumValues.put("Onion", 146);
        potassiumValues.put("Tomato", 237);
        potassiumValues.put("Potato", 429);
        
        ingredientComboBox.setItems(FXCollections.observableArrayList(potassiumValues.keySet()));
        
        ingredientColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        weightColumn.setCellValueFactory(new PropertyValueFactory<>("weight"));
        potassiumColumn.setCellValueFactory(new PropertyValueFactory<>("potassium"));
        
        ingredientTableView.setItems(ingredients);
    }

    @FXML
    private void handleAddIngredient() {
        String ingredient = ingredientComboBox.getValue();
        String weightText = weightField.getText();

        if (ingredient == null || weightText.isEmpty()) {
            showAlert("Error", "Please select an ingredient and enter weight.");
            return;
        }

        try {
            int weight = Integer.parseInt(weightText);
            int potassiumPer100g = potassiumValues.getOrDefault(ingredient, 0);
            int totalPotassium = (potassiumPer100g * weight) / 100;

            ingredients.add(new Ingredient(ingredient, weight, totalPotassium));
            ingredientComboBox.getItems().remove(ingredient);
            weightField.clear();
            updateTotalPotassium();
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number for weight.");
        }
    }

    @FXML
    private void handleDeleteIngredient() {
        Ingredient selected = ingredientTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ingredientTableView.getItems().remove(selected);
            ingredientComboBox.getItems().add(selected.getName());
            updateTotalPotassium();
        }
    }

    @FXML
    private void handleSubmit() {
        int totalPotassium = ingredients.stream().mapToInt(Ingredient::getPotassium).sum();
        showAlert("Total Potassium", "Total Potassium Content: " + totalPotassium + " mg");
    }

    private void updateTotalPotassium() {
        int totalPotassium = ingredients.stream().mapToInt(Ingredient::getPotassium).sum();
        totalPotassiumLabel.setText(String.valueOf(totalPotassium));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class Ingredient {
        private final String name;
        private final int weight;
        private final int potassium;

        public Ingredient(String name, int weight, int potassium) {
            this.name = name;
            this.weight = weight;
            this.potassium = potassium;
        }

        public String getName() { return name; }
        public int getWeight() { return weight; }
        public int getPotassium() { return potassium; }
    }
}
