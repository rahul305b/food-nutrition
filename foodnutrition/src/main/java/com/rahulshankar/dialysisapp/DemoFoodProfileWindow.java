package com.rahulshankar.dialysisapp;

import com.rahulshankar.dialysisapp.service.DemoFoodProfileService;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.Map;

public final class DemoFoodProfileWindow {
    private DemoFoodProfileWindow() {}
    public static void show(Stage owner) {
        try {
            var rows = new DemoFoodProfileService().findProfiles();
            TableView<Map<String,String>> table = new TableView<>();
            String[] keys = {"food_name","local_name","category","preparation","basis","energy_kcal","protein_g","carbohydrates_g","fat_g","fibre_g","potassium_mg","phosphorus_mg","sodium_mg","calcium_mg","magnesium_mg","iron_mg","data_status","source_name","source_record_id","citation","license_note"};
            String[] labels = {"Food","Local name","Category","Preparation","Basis","Energy","Protein","Carbs","Fat","Fibre","Potassium","Phosphorus","Sodium","Calcium","Magnesium","Iron","Status","Source","Record ID","Citation","Licence note"};
            for(int i=0;i<keys.length;i++) { final String key=keys[i]; TableColumn<Map<String,String>,String> col=new TableColumn<>(labels[i]); col.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().get(key))); table.getColumns().add(col); }
            table.setItems(FXCollections.observableArrayList(rows));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            Label warn=new Label("SYNTHETIC TEST DATA ONLY — NOT FOR DIETARY OR MEDICAL USE");
            warn.setStyle("-fx-text-fill: #b00020; -fx-font-weight: bold;");
            Label note=new Label("These records are stored separately from the calculator's food catalogue. They are not used in calculations.");
            Label empty=new Label(rows.isEmpty()?"No demo profiles have been imported yet.": "");
            Button clearDemo = new Button("Remove All Demo Profiles");
            clearDemo.setOnAction(event -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete all demo profiles from the separate demo database? This will not affect the calculator catalogue.",
                        ButtonType.YES, ButtonType.NO);
                confirm.initOwner(owner);
                if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                    try {
                        new com.rahulshankar.dialysisapp.service.DemoFoodProfileService().deleteAllDemoProfiles();
                        table.getItems().clear();
                        empty.setText("No demo profiles have been imported yet.");
                    } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, "Unable to remove demo profiles: " + e.getMessage()).showAndWait();
                    }
                }
            });
            VBox box=new VBox(10,warn,note,empty,clearDemo,table); box.setPadding(new javafx.geometry.Insets(12));
            Stage window=new Stage(); window.initOwner(owner); window.setTitle("Demo Food Profiles");
            window.setScene(new Scene(box,1400,700)); window.show();
        } catch(Exception ex) { new Alert(Alert.AlertType.ERROR,"Unable to load profile data: "+ex.getMessage()).showAndWait(); }
    }
}
