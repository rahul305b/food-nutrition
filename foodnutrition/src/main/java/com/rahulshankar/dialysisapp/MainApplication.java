package com.rahulshankar.dialysisapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {
	@Override
	public void start(Stage stage) throws Exception {
	    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
	    Scene scene = new Scene(fxmlLoader.load());
	    stage.setTitle("Dialysis Food Tracker");
	    stage.setScene(scene);
	    stage.show();
	}
}