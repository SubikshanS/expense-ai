package com.expensetracker;

import com.expensetracker.view.MainDashboard;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main Entry Point for the JavaFX AI-Powered Expense Tracker Application.
 * Configured strictly in pure Java code without any external CSS files.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Instantiate the pure Java Main Dashboard View
        MainDashboard dashboard = new MainDashboard(primaryStage);

        // Create Application Scene (1120x720 dimensions)
        Scene scene = new Scene(dashboard, 1120, 720);

        // Configure Primary Stage Window
        primaryStage.setTitle("Expense Tracker");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
