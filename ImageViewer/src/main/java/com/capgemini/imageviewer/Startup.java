package com.capgemini.imageviewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Startup extends Application {

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		primaryStage.setTitle("Image Viewer");
		
		Parent root = FXMLLoader.load(getClass().getResource("/com/capgemini/imageviewer/view/image-viewer-main.fxml"));
		
		Scene scene = new Scene(root);
		
		scene.getStylesheets().add(getClass().getResource("/com/capgemini/imageviewer/css/application.css").toExternalForm());
		
		primaryStage.setScene(scene);
		
		primaryStage.show();
	}
	
	
}
