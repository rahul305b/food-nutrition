package com.rahulshankar.dialysisapp.ui;

import com.rahulshankar.dialysisapp.service.AuthService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginUI extends Application {
    
    private AuthService authService = new AuthService();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Login Page");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label heading = new Label("Login");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (authService.authenticate(username, password)) {
                new FoodPreparationUI().start(new Stage()); // Open next screen
                stage.close(); // Close login window
            } else {
                errorLabel.setText("Invalid username or password!");
            }
        });

        root.getChildren().addAll(heading, usernameField, passwordField, loginButton, errorLabel);
        Scene scene = new Scene(root, 300, 250);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
