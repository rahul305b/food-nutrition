package com.rahulshankar.dialysisapp;

import com.rahulshankar.dialysisapp.service.DemoFoodImporter;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileInputStream;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FoodPreparationUI.fxml"));
        BorderPane root = loader.load();
        VBox top=(VBox)root.getTop();
        Button importButton=new Button("Import Demo Food Dataset (CSV)");
        importButton.setOnAction(e->{
            FileChooser chooser=new FileChooser();
            chooser.setTitle("Select Demo Food Dataset CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files","*.csv"));
            File file=chooser.showOpenDialog(stage);
            if(file==null)return;
            try(FileInputStream in=new FileInputStream(file)){
                DemoFoodImporter.ImportResult result=new DemoFoodImporter().importCsv(in);
                Alert alert=new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Demo import complete");
                alert.setHeaderText("Demo data import summary");
                alert.setContentText(result.toString()+"\n\nIMPORTANT: Imported values are synthetic test data, not real nutritional values.");
                alert.showAndWait();
            }catch(Exception ex){
                Alert alert=new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Import failed");alert.setHeaderText("Dataset could not be imported");
                alert.setContentText(ex.getMessage());alert.showAndWait();
            }
        });
        Button profilesButton = new Button("View Imported Demo Profiles");\n        profilesButton.setOnAction(e -> DemoFoodProfileWindow.show(stage));\n        top.getChildren().add(new javafx.scene.layout.HBox(8, importButton, profilesButton));
        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("Renal Food Nutrition Calculator");
        stage.setMinWidth(820);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
