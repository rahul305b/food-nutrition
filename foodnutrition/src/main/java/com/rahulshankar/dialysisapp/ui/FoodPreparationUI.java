package com.rahulshankar.dialysisapp.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class FoodPreparationUI extends Application {

    private VBox ingredientsBox = new VBox(10); // Box to hold selected ingredients
    private Map<String, Integer> selectedIngredients = new HashMap<>();
    
    private List<String> availableVegetables = new ArrayList<>(Arrays.asList("Carrot", "Spinach", "Potato"));
    private Map<String, Integer> potassiumValues = Map.of("Carrot", 320, "Spinach", 558, "Potato", 429);

    @Override
    public void start(Stage stage) {
        stage.setTitle("Food Preparation");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label heading = new Label("Food Preparation");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField dishNameField = new TextField();
        dishNameField.setPromptText("Enter the name of the dish");

        Label ingredientsLabel = new Label("Ingredients:");
        Button addIngredientButton = new Button("Add Ingredient");
        
        addIngredientButton.setOnAction(e -> addIngredient());

        Button calculateButton = new Button("Calculate Nutrition");
        calculateButton.setOnAction(e -> calculateNutrition());

        root.getChildren().addAll(heading, dishNameField, ingredientsLabel, ingredientsBox, addIngredientButton, calculateButton);

        Scene scene = new Scene(root, 400, 500);
        stage.setScene(scene);
        stage.show();
    }

    private void addIngredient() {
        if (availableVegetables.isEmpty()) {
            return;
        }

        HBox ingredientRow = new HBox(10);
        ingredientRow.setPadding(new Insets(5, 0, 5, 0));

        ComboBox<String> vegetableDropdown = new ComboBox<>();
        vegetableDropdown.getItems().addAll(availableVegetables);
        vegetableDropdown.getSelectionModel().selectFirst();

        TextField gramsField = new TextField();
        gramsField.setPromptText("Grams");
        gramsField.setPrefWidth(60);

        Button removeButton = new Button("❌");
        removeButton.setOnAction(e -> removeIngredient(ingredientRow, vegetableDropdown.getValue()));

        ingredientRow.getChildren().addAll(vegetableDropdown, gramsField, removeButton);
        ingredientsBox.getChildren().add(ingredientRow);

        vegetableDropdown.setOnAction(e -> selectedIngredients.put(vegetableDropdown.getValue(), 0));
        availableVegetables.remove(vegetableDropdown.getValue());
    }

    private void removeIngredient(HBox ingredientRow, String vegetable) {
        ingredientsBox.getChildren().remove(ingredientRow);
        availableVegetables.add(vegetable);
        selectedIngredients.remove(vegetable);
    }

    private void calculateNutrition() {
        int totalPotassium = 0;

        for (var node : ingredientsBox.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                ComboBox<String> vegetableDropdown = (ComboBox<String>) row.getChildren().get(0);
                TextField gramsField = (TextField) row.getChildren().get(1);

                String vegetable = vegetableDropdown.getValue();
                int grams = Integer.parseInt(gramsField.getText());
                int potassium = (potassiumValues.get(vegetable) * grams) / 100;

                totalPotassium += potassium;
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nutrition Info");
        alert.setHeaderText("Potassium Calculation");
        alert.setContentText("Total Potassium: " + totalPotassium + " mg");
        alert.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
