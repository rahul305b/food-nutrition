package com.rahulshankar.dialysisapp.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.event.ActionEvent;
import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLogin(ActionEvent event) {
        System.out.println("Login Attempt: " + usernameField.getText() + " / " + passwordField.getText());

        // Dummy login: Proceed to next screen
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FoodPreparationUI.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        System.out.println("Redirecting to Registration Page...");
        // TODO: Implement registration page transition
    }
}
