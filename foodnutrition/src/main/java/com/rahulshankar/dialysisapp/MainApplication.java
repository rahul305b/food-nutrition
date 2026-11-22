package com.rahulshankar.dialysisapp;

import com.rahulshankar.dialysisapp.controllers.FoodPreparationController;
import com.rahulshankar.dialysisapp.service.DemoFoodImporter;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Files;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FoodPreparationUI.fxml"));
        BorderPane root = loader.load();
        FoodPreparationController controller = loader.getController();
        VBox top=(VBox)root.getTop();
        Button importButton=new Button("Import Demo Food Dataset (CSV)");
        importButton.setOnAction(e->{
            FileChooser chooser=new FileChooser();
            chooser.setTitle("Select Demo Food Dataset CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files","*.csv"));
            File file=chooser.showOpenDialog(stage);
            if(file==null)return;
            final byte[] data;
            try { data=Files.readAllBytes(file.toPath()); }
            catch(Exception ex){new Alert(Alert.AlertType.ERROR,"Unable to read CSV: "+ex.getMessage()).showAndWait();return;}
            importButton.setDisable(true);
            importButton.setText("Importing...");
            Task<DemoFoodImporter.ImportResult> task=new Task<>(){
                @Override protected DemoFoodImporter.ImportResult call() throws Exception {
                    return new DemoFoodImporter().importCsv(new java.io.ByteArrayInputStream(data));
                }
            };
            task.setOnSucceeded(event->{
                importButton.setDisable(false);importButton.setText("Import Demo Food Dataset (CSV)");
                try{controller.refreshFoodCatalogueAfterImport();}catch(Exception ignored){}
                Alert alert=new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Demo import complete");alert.setHeaderText("Demo data import summary");
                alert.setContentText(task.getValue().toString()+"\n\nIMPORTANT: Synthetic test data. It is not used by the nutrition calculator.");
                alert.showAndWait();
            });
            task.setOnFailed(event->{
                importButton.setDisable(false);importButton.setText("Import Demo Food Dataset (CSV)");
                Alert alert=new Alert(Alert.AlertType.ERROR);alert.setTitle("Import failed");
                alert.setHeaderText("Dataset could not be imported");alert.setContentText(task.getException()==null?"Unknown error":task.getException().getMessage());alert.showAndWait();
            });
            Thread worker=new Thread(task,"demo-food-import");worker.setDaemon(true);worker.start();
        });
        top.getChildren().add(importButton);
        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("Renal Food Nutrition Calculator");
        stage.setMinWidth(820);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
