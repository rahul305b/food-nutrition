package com.rahulshankar.dialysisapp.controllers;

import com.rahulshankar.dialysisapp.model.FoodItem;
import com.rahulshankar.dialysisapp.model.IngredientEntry;
import com.rahulshankar.dialysisapp.service.FoodCatalogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class FoodPreparationController {
    @FXML private TextField dishNameField;
    @FXML private ComboBox<FoodItem> ingredientComboBox;
    @FXML private TextField weightField;
    @FXML private TableView<IngredientEntry> ingredientTableView;
    @FXML private TableColumn<IngredientEntry, String> ingredientColumn;
    @FXML private TableColumn<IngredientEntry, Integer> weightColumn;
    @FXML private TableColumn<IngredientEntry, String> potassiumColumn;
    @FXML private TableColumn<IngredientEntry, String> phosphorusColumn;
    @FXML private TableColumn<IngredientEntry, String> sodiumColumn;
    @FXML private Label totalPotassiumLabel;
    @FXML private Label totalPhosphorusLabel;
    @FXML private Label totalSodiumLabel;

    private final ObservableList<FoodItem> foodItems = FXCollections.observableArrayList();
    private final ObservableList<IngredientEntry> ingredients = FXCollections.observableArrayList();
    private FoodCatalogService catalog;

    @FXML
    public void initialize() {
        ingredientColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        weightColumn.setCellValueFactory(new PropertyValueFactory<>("weightGrams"));
        potassiumColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                formatValue(data.getValue().getPotassiumMg())));
        phosphorusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                formatValue(data.getValue().getPhosphorusMg())));
        sodiumColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                formatValue(data.getValue().getSodiumMg())));
        ingredientTableView.setItems(ingredients);

        ingredientComboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(FoodItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        ingredientComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(FoodItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        try {
            catalog = new FoodCatalogService();
            reloadFoods();
        } catch (Exception e) {
            showError("Food database could not be opened", e.getMessage());
        }
        updateTotals();
    }

    @FXML
    private void handleAddIngredient() {
        FoodItem food = ingredientComboBox.getValue();
        if (food == null) {
            showError("Missing ingredient", "Select a food item first.");
            return;
        }
        if (ingredients.stream().anyMatch(entry -> entry.getName().equalsIgnoreCase(food.getName()))) {
            showError("Duplicate ingredient", "This ingredient is already in the dish. Remove it before adding it again.");
            return;
        }

        final int weight;
        try {
            weight = Integer.parseInt(weightField.getText().trim());
            if (weight <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            showError("Invalid weight", "Enter a whole-number weight in grams greater than zero.");
            return;
        }

        ingredients.add(new IngredientEntry(food, weight));
        weightField.clear();
        updateAvailableFoods();
        updateTotals();
    }

    @FXML
    private void handleDeleteIngredient() {
        IngredientEntry selected = ingredientTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No selection", "Select an ingredient row to remove.");
            return;
        }
        ingredients.remove(selected);
        updateAvailableFoods();
        updateTotals();
    }

    @FXML
    private void handleSubmit() {
        if (dishNameField.getText().isBlank()) {
            showError("Dish name required", "Enter a name for the dish.");
            return;
        }
        if (ingredients.isEmpty()) {
            showError("No ingredients", "Add at least one ingredient before calculating.");
            return;
        }

        String summary = "Dish: " + dishNameField.getText().trim()
                + "\nIngredients: " + ingredients.size()
                + "\n\nPotassium: " + totalPotassiumLabel.getText() + " mg"
                + "\nPhosphorus: " + totalPhosphorusLabel.getText() + " mg"
                + "\nSodium: " + totalSodiumLabel.getText() + " mg"
                + "\n\nValues are estimates based on the food data entered in the catalogue.";
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nutrition summary");
        alert.setHeaderText("Calculated nutrition for " + dishNameField.getText().trim());
        alert.setContentText(summary);
        alert.showAndWait();
    }

    @FXML
    private void handleClearDish() {
        dishNameField.clear();
        ingredients.clear();
        weightField.clear();
        updateAvailableFoods();
        updateTotals();
    }

    @FXML
    private void handleManageFoods() {
        if (catalog == null) {
            showError("Database unavailable", "The local food database could not be opened.");
            return;
        }

        Stage stage = new Stage();
        stage.initOwner(ingredientComboBox.getScene().getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Manage Food Catalogue");

        TableView<FoodItem> table = new TableView<>();
        TableColumn<FoodItem, String> nameCol = new TableColumn<>("Food");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<FoodItem, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        table.getColumns().addAll(nameCol, categoryCol,
                foodValueColumn("Potassium / 100g", FoodItem::getPotassiumMgPer100g),
                foodValueColumn("Phosphorus / 100g", FoodItem::getPhosphorusMgPer100g),
                foodValueColumn("Sodium / 100g", FoodItem::getSodiumMgPer100g));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TextField nameField = new TextField();
        TextField categoryField = new TextField();
        TextField potassiumField = new TextField();
        TextField phosphorusField = new TextField();
        TextField sodiumField = new TextField();
        nameField.setPromptText("Food name");
        categoryField.setPromptText("Category");
        potassiumField.setPromptText("mg per 100g (optional)");
        phosphorusField.setPromptText("mg per 100g (optional)");
        sodiumField.setPromptText("mg per 100g (optional)");

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("Name"), nameField, new Label("Category"), categoryField);
        form.addRow(1, new Label("Potassium"), potassiumField, new Label("Phosphorus"), phosphorusField);
        form.addRow(2, new Label("Sodium"), sodiumField);
        form.setPadding(new Insets(8, 0, 8, 0));

        Button saveButton = new Button("Save / Update Food");
        Button clearButton = new Button("Clear Form");
        Button deleteButton = new Button("Delete Selected");
        Button importedProfilesButton = new Button("View Imported Profiles");
        HBox actions = new HBox(8, saveButton, clearButton, deleteButton, importedProfilesButton);
        importedProfilesButton.setOnAction(event -> com.rahulshankar.dialysisapp.DemoFoodProfileWindow.show(stage));

        Runnable clearForm = () -> {
            nameField.clear();
            categoryField.clear();
            potassiumField.clear();
            phosphorusField.clear();
            sodiumField.clear();
            table.getSelectionModel().clearSelection();
        };
        Runnable refresh = () -> {
            try {
                table.setItems(FXCollections.observableArrayList(catalog.findAll()));
                reloadFoods();
            } catch (Exception e) {
                showError("Could not refresh food catalogue", e.getMessage());
            }
        };

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, item) -> {
            if (item != null) {
                nameField.setText(item.getName());
                categoryField.setText(item.getCategory());
                potassiumField.setText(valueForForm(item.getPotassiumMgPer100g()));
                phosphorusField.setText(valueForForm(item.getPhosphorusMgPer100g()));
                sodiumField.setText(valueForForm(item.getSodiumMgPer100g()));
            }
        });

        saveButton.setOnAction(event -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Food name is required.");
                String category = categoryField.getText().isBlank() ? "Other" : categoryField.getText().trim();
                FoodItem item = new FoodItem(name, category,
                        parseOptionalValue(potassiumField, "Potassium"),
                        parseOptionalValue(phosphorusField, "Phosphorus"),
                        parseOptionalValue(sodiumField, "Sodium"));
                catalog.save(item);
                refresh.run();
                clearForm.run();
            } catch (IllegalArgumentException e) {
                showError("Check food details", e.getMessage());
            } catch (Exception e) {
                showError("Could not save food", e.getMessage());
            }
        });
        clearButton.setOnAction(event -> clearForm.run());
        deleteButton.setOnAction(event -> {
            FoodItem selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("No selection", "Select a food item to delete.");
                return;
            }
            try {
                catalog.delete(selected.getName());
                refresh.run();
                clearForm.run();
            } catch (Exception e) {
                showError("Could not delete food", e.getMessage());
            }
        });

        refresh.run();
        VBox root = new VBox(10,
                new Label("Enter nutrient amounts per 100 grams. Leave unknown values blank."),
                table, form, actions);
        root.setPadding(new Insets(14));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        stage.setScene(new Scene(root, 900, 580));
        stage.showAndWait();
    }

    private TableColumn<FoodItem, String> foodValueColumn(String title, Function<FoodItem, Double> getter) {
        TableColumn<FoodItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                formatValue(getter.apply(data.getValue()))));
        return column;
    }

    private Double parseOptionalValue(TextField field, String label) {
        String text = field.getText().trim();
        if (text.isEmpty()) return null;
        try {
            double value = Double.parseDouble(text);
            if (!Double.isFinite(value) || value < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a non-negative number in mg per 100g, or blank if unknown.");
        }
    }

    private void reloadFoods() throws Exception {
        foodItems.setAll(catalog.findAll());
        updateAvailableFoods();
    }

    private void updateAvailableFoods() {
        List<String> used = ingredients.stream().map(IngredientEntry::getName).toList();
        ingredientComboBox.setItems(FXCollections.observableArrayList(
                foodItems.stream().filter(food -> !used.contains(food.getName())).toList()));
        ingredientComboBox.getSelectionModel().clearSelection();
    }

    private void updateTotals() {
        totalPotassiumLabel.setText(totalFor(IngredientEntry::getPotassiumMg));
        totalPhosphorusLabel.setText(totalFor(IngredientEntry::getPhosphorusMg));
        totalSodiumLabel.setText(totalFor(IngredientEntry::getSodiumMg));
    }

    private String totalFor(Function<IngredientEntry, Double> getter) {
        if (ingredients.isEmpty()) return "0.00";
        double total = 0;
        for (IngredientEntry entry : ingredients) {
            Double value = getter.apply(entry);
            if (value == null) return "Incomplete data";
            total += value;
        }
        return formatNumber(total);
    }

    private static String formatValue(Double value) {
        return value == null ? "Not entered" : formatNumber(value);
    }

    private static String valueForForm(Double value) {
        return value == null ? "" : Double.toString(value);
    }

    private static String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "An unexpected error occurred." : message);
        alert.showAndWait();
    }
}