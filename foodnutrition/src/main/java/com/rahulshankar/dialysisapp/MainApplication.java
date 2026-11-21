package com.rahulshankar.dialysisapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FoodPreparationUI.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 700);
        stage.setTitle("Renal Food Nutrition Calculator");
        stage.setMinWidth(820);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}