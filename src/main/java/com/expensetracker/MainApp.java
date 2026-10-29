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

        // Create Application Scene (1160x740 dimensions)
        Scene scene = new Scene(dashboard, 1160, 740);

        try {
            var css = getClass().getResource("/styles/app.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {
        }

        // Configure Primary Stage Window
        primaryStage.setTitle("Expense Tracker - AI Financial Assistant");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
