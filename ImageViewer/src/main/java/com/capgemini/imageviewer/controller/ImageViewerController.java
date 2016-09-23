package com.capgemini.imageviewer.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import com.capgemini.imageviewer.model.ImageViewerModel;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public class ImageViewerController {

	private static final Logger LOG = Logger.getLogger(ImageViewerController.class);

	@FXML
	private AnchorPane mainAnchorPane;

	@FXML
	private Button loadButton;

	@FXML
	private ImageView imageViewer;

	@FXML
	private ScrollPane scrollPane;

	@FXML
	private HBox myHBox;
	
	@FXML
	private ListView<File> fileList;
	
	@FXML
	private CheckBox slideShowCheckBox;
	
	@FXML
	private Slider zoomSlider;
	
	@FXML
	private Button previousImageButton;
	
	@FXML
	private Button nextImageButton;
	
	private final long INTERVAL_TIME = 5000L;
	
	private ImageViewerModel model = new ImageViewerModel();
	
	private boolean isSlideShowEnabled = false;
	
	@FXML
	private void initialize() {
		scrollPane.setContent(imageViewer);
		scrollPane.setStyle("-fx-background-color:transparent;");
		myHBox.setAlignment(Pos.CENTER);
		initializeListView();
		initializeCheckBox();
		initializeSlider();
		nextImageButton.setDisable(true);
		previousImageButton.setDisable(true);
	}

	private void initializeSlider() {
		zoomSlider.setMin(0.0);
		zoomSlider.setMax(100.0);
		zoomSlider.setValue(0.0);
		zoomSlider.setShowTickLabels(true);
		zoomSlider.setShowTickMarks(true);
		zoomSlider.setMajorTickUnit(25.0);
		zoomSlider.setMinorTickCount(5);
		zoomSlider.setDisable(true);
		zoomSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				double value = zoomSlider.getValue() / 10;
				imageViewer.setFitHeight(imageViewer.getImage().getHeight() * (1.0 + value));
				imageViewer.setFitWidth(imageViewer.getImage().getWidth() * (1.0 + value));
			}
		});
	}

	private void initializeCheckBox() {
		slideShowCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (slideShowCheckBox.isSelected()) {
					isSlideShowEnabled = true;
					startSlideShow();
					return;
				}
				isSlideShowEnabled = false;
			}
		});
		disableSlideShow();
	}
	
	private void initializeListView() {
		fileList.setCellFactory(cell -> new ListCell<File>() {
			@Override
			protected void updateItem(File item, boolean empty) {
				super.updateItem(item, empty);
				if (item != null) {
					setText(item.getName());
					return;
				}
				setText(null);
			}
		});
		fileList.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				handleListViewEvent();
			}
		});
		fileList.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				handleListViewEvent();
			}
		});
		fileList.setItems(model.imagesInDirectoryProperty());
	}
	
	private void handleListViewEvent() {
		if (fileList.getSelectionModel().getSelectedItem().exists() == false) {
			displayErrorBox("Selected file does not exist anymore!");
			fileList.getSelectionModel().select(model.getCurrentOpenedImage());
			return;
		} else {
			openImage(fileList.getSelectionModel().getSelectedItem());
		}
	}

	/**By disabling CheckBox, there will be no possibility to start slide show. Only after successful listing of all
	 * image in current directory, this checkbox will be enabled.
	 * @param event
	 */
	@FXML
	private void loadButtonAction(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("All Images", "*.jpg", "*.bmp", "*.gif", "*.png"),
				new FileChooser.ExtensionFilter("JPG", "*.jpg"), new FileChooser.ExtensionFilter("GIF", "*.gif"),
				new FileChooser.ExtensionFilter("BMP", "*.bmp"), new FileChooser.ExtensionFilter("PNG", "*.png"));
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Pictures/image pack/"));
		File file = fileChooser.showOpenDialog(mainAnchorPane.getScene().getWindow());
		if (file == null) {
			return;
		}
		disableSlideShow();
		processSelectedImage(file);
	}
	
	private void handleNextAndPreviousButton() {
		if (fileList.getItems().size() <= 1) {
			nextImageButton.setDisable(true);
			previousImageButton.setDisable(true);
			return;
		}
		nextImageButton.setDisable(false);
		previousImageButton.setDisable(false);
	}

	private void disableSlideShow() {
		slideShowCheckBox.setSelected(false);
		slideShowCheckBox.setDisable(true);
	}

	private void processSelectedImage(File file) {
		openImage(file);
		processListingImagesInDirectory();
	}

	private void processListingImagesInDirectory() {
		disableSlideShow();
		Task<Void> backgroundFilesListing = listImagesInDirectory();
		new Thread(backgroundFilesListing).start();
	}
	
	private void refreshAllImagesInDirectory() {
		processListingImagesInDirectory();
	}
	
	private void startSlideShow() {
		Thread slideShow = new Thread(processSlideShow());
		slideShow.setDaemon(true);
		if (model.getImagesInDirectory().size() > 1 && isSlideShowEnabled == true) {
			slideShow.start();
		}
	}

	private Task<Void> listImagesInDirectory() {
		File parentDirectory = model.getCurrentOpenedImage().getParentFile();
		Task<Void> backgroundImagesListing = new Task<Void>() {
			@Override
			protected Void call() {
				if (parentDirectory != null && parentDirectory.isDirectory()) {
					File[] fileTable = parentDirectory.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".bmp") || name.endsWith(".gif");
						}
					});
					if (fileTable != null && fileTable.length > 0) {
						List<File> fileList = new ArrayList<>();
						for (File file : fileTable) {
							fileList.add(file);
						}
						model.setImagesInDirectory(fileList);
						handleNextAndPreviousButton();
					}
				}
				return null;
			}
			@Override
			protected void succeeded() {
				LOG.debug("Successfully crated images list in current directory");
				fileList.getSelectionModel().select(model.getCurrentOpenedImage());
				if (model.getImagesInDirectory().size() > 1) {
					slideShowCheckBox.setDisable(false);
				}
			}

			@Override
			protected void failed() {
				LOG.error("Failed creating images list in current directory", this.getException());;
			}
		};
		return backgroundImagesListing;
	}

	private Task<Void> processSlideShow() {
		Task<Void> slideShow = new Task<Void>() {
			@Override
			protected Void call() throws InterruptedException {
				Thread.sleep(INTERVAL_TIME);
				while (isSlideShowEnabled == true) {
					if (model.getImagesInDirectory().size() <= 1) {
						return null;
					}
					openImage(model.getImagesInDirectory().get(getNextIndexInSlideShow()));
					Thread.sleep(INTERVAL_TIME);
				}
				return null;
			}
			@Override
			protected void failed() {
				LOG.debug("Failed processing slide show.", this.getException());
				slideShowCheckBox.setSelected(false);
			}
		};
		return slideShow;
	}
	
	private int getCurrentIndexInSlideShow() {
		if (fileList.getSelectionModel().isEmpty() == false) {
			return fileList.getSelectionModel().getSelectedIndex();
		}
		return 0;
	}
	
	private int getNextIndexInSlideShow() {
		for (int i = getCurrentIndexInSlideShow(), counter = 0; i <= model.getImagesInDirectory().size() - 1; i++, counter++) {
			// if counter wouldn't be check, then this loop could go infinitely in case when after listing all images in directory
			// all images beside already opened would be deleted
			if (counter == model.getImagesInDirectory().size()) {
				refreshAllImagesInDirectory();
				return getCurrentIndexInSlideShow();
			}
			if (i >= model.getImagesInDirectory().size() - 1) {
				i = -1;
			}
			if (model.getImagesInDirectory().get(i+1).exists() && model.getImagesInDirectory().get(i+1) != model.getCurrentOpenedImage()) {
				return i+1;
			}
		}
		return 0;
	}

	private void openImage(File file) {
		if (file == null) {
			return;
		}
		try {
			BufferedImage bufferedImage = ImageIO.read(file);
			Image image = SwingFXUtils.toFXImage(bufferedImage, null);
			imageViewer.setImage(image);
			model.setCurrentOpenedImage(file);
			fileList.getSelectionModel().select(model.getCurrentOpenedImage());
			handleZoomSlider();
			LOG.debug("Successfully opened image");
		} catch (IOException exception) {
			LOG.error("An error during opening image occured", exception);
		}
	}
	
	private void handleZoomSlider() {
		if (zoomSlider.isDisabled() == true) {
			zoomSlider.setDisable(false);
		}
		zoomSlider.setValue(0.0);
	}
	
	private void displayErrorBox(String errorMessage) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error!");
		alert.setHeaderText("An error occured.");
		alert.setContentText(errorMessage);
		alert.showAndWait();
	}
	
	@FXML
	private void nextImageButtonAction(ActionEvent event) {
		openImage(model.getImagesInDirectory().get(getNextIndexInSlideShow()));
	}
	
	@FXML
	private void previousImageButtonAction(ActionEvent event) {
		for (int i = getCurrentIndexInSlideShow(), counter = 0; i >= 0; i--, counter++) {
			// if counter wouldn't be check, then this loop could go infinitely in case when after listing all images in directory
			// all images beside already opened would be deleted
			if (counter == model.getImagesInDirectory().size()) {
				refreshAllImagesInDirectory();
				return;
			}
			if (i <= 0) {
				i = model.getImagesInDirectory().size();
			}
			if (model.getImagesInDirectory().get(i-1).exists()) {
				openImage(model.getImagesInDirectory().get(i-1));
				return;
			}
		}
	}
}
