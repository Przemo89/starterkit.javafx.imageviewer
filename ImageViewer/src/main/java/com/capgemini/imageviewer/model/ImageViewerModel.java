package com.capgemini.imageviewer.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class ImageViewerModel {

	// REV: wykomentowany kod
//	private final StringProperty fileName = new SimpleStringProperty();
	private File currentOpenedImage;
	private final ListProperty<File> imagesInDirectory = new SimpleListProperty<>(FXCollections.observableList(new ArrayList<>()));

	public File getCurrentOpenedImage() {
		return currentOpenedImage;
	}

	public void setCurrentOpenedImage(File currentOpenedImage) {
		this.currentOpenedImage = currentOpenedImage;
	}
//	public StringProperty fileNameProperty() {
//		return fileName;
//	}
//	
//	public String getFileName() {
//		return fileName.get();
//	}
//	
//	public void setFileName(String value) {
//		fileName.set(value);
//	}
	
	public List<File> getImagesInDirectory() {
		return imagesInDirectory.get();
	}
	
	public void setImagesInDirectory(List<File> value) {
		imagesInDirectory.setAll(value);
	}
	
	public ListProperty<File> imagesInDirectoryProperty() {
		return imagesInDirectory;
	}
	
}
