package com.checkin.components;

import com.checkin.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable card: "FACIAL SIGNAL" — allows users to drag & drop or browse for
 * a photo, displays file metadata & preview, and supports change & removal.
 */
public class PhotoUploadCard extends VBox {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    private final Label dropLabel = new Label("Drag & Drop Your Photo Here");
    private final Label orLabel = new Label("OR");
    private final Button browseButton = new Button("Browse Files");
    private final Label supportedLabel = new Label("Supported: JPG • JPEG • PNG");
    private final Label maxSizeLabel = new Label("Maximum: 10 MB");
    private final Label errorLabel = new Label();

    private final VBox uploadArea = new VBox(8);
    private final VBox previewArea = new VBox(8);
    private final ImageView imageView = new ImageView();
    private final Label filenameLabel = new Label();
    private final Label fileSizeLabel = new Label();
    private final Button changePhotoButton = new Button("Change Photo");
    private final Button removeButton = new Button("Remove");

    private File selectedFile;
    private Consumer<File> onPhotoChanged;

    public PhotoUploadCard() {
        super(12);
        setPadding(new Insets(20));
        Theme.applyCardStyle(this);
        setAlignment(Pos.TOP_CENTER);

        VBox headerBox = new VBox(3);
        headerBox.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("FACIAL SIGNAL");
        title.getStyleClass().add("card-title");

        Label subtitle = new Label("Let your expression speak.");
        subtitle.getStyleClass().add("card-subtitle");

        headerBox.getChildren().addAll(title, subtitle);

        buildUploadArea();
        buildPreviewArea();

        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);

        VBox.setVgrow(uploadArea, Priority.ALWAYS);
        VBox.setVgrow(previewArea, Priority.ALWAYS);

        getChildren().addAll(headerBox, uploadArea, previewArea, errorLabel);
        showUploadState();

        setupDragAndDrop();
    }

    private void buildUploadArea() {
        uploadArea.setAlignment(Pos.CENTER);
        uploadArea.setPadding(new Insets(20, 16, 20, 16));
        uploadArea.getStyleClass().add("drop-zone");

        dropLabel.getStyleClass().add("drop-label");
        orLabel.getStyleClass().add("or-label");

        browseButton.getStyleClass().add("secondary-button");
        browseButton.setOnAction(e -> openFileChooser());

        supportedLabel.getStyleClass().add("hint-label");
        maxSizeLabel.getStyleClass().add("hint-label");

        uploadArea.getChildren().addAll(dropLabel, orLabel, browseButton, supportedLabel, maxSizeLabel);
    }

    private void buildPreviewArea() {
        previewArea.setAlignment(Pos.CENTER);
        previewArea.setPadding(new Insets(10));

        imageView.setFitWidth(160);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Rectangle clip = new Rectangle(160, 140);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imageView.setClip(clip);

        filenameLabel.getStyleClass().add("meta-label");
        filenameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f3f9;");
        filenameLabel.setWrapText(true);

        fileSizeLabel.getStyleClass().add("meta-label");

        HBox actions = new HBox(10, changePhotoButton, removeButton);
        actions.setAlignment(Pos.CENTER);
        changePhotoButton.getStyleClass().add("secondary-button");
        removeButton.getStyleClass().add("danger-button");
        changePhotoButton.setOnAction(e -> openFileChooser());
        removeButton.setOnAction(e -> removePhoto());

        previewArea.getChildren().addAll(imageView, filenameLabel, fileSizeLabel, actions);
    }

    private void setupDragAndDrop() {
        uploadArea.setOnDragOver(event -> {
            if (event.getGestureSource() != uploadArea && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        uploadArea.setOnDragEntered(event -> uploadArea.getStyleClass().add("drop-zone-active"));
        uploadArea.setOnDragExited(event -> uploadArea.getStyleClass().remove("drop-zone-active"));

        uploadArea.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                success = tryAcceptFile(db.getFiles().get(0));
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void openFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a Facial Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images (JPG, JPEG, PNG)", "*.jpg", "*.jpeg", "*.png"));
        Window window = getScene() != null ? getScene().getWindow() : null;
        File file = chooser.showOpenDialog(window);
        if (file != null) {
            tryAcceptFile(file);
        }
    }

    private boolean tryAcceptFile(File file) {
        String name = file.getName().toLowerCase();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            showError("Unsupported file type. Please select a JPG, JPEG, or PNG image.");
            return false;
        }
        if (file.length() > MAX_BYTES) {
            showError("File exceeds the maximum 10 MB limit.");
            return false;
        }

        try {
            Image image = new Image(file.toURI().toString());
            if (image.isError()) {
                showError("Could not process this image. Please try another photo.");
                return false;
            }
            hideError();
            selectedFile = file;
            imageView.setImage(image);
            filenameLabel.setText(file.getName());
            fileSizeLabel.setText(String.format("Size: %.2f MB", file.length() / (1024.0 * 1024.0)));
            showPreviewState();
            if (onPhotoChanged != null) {
                onPhotoChanged.accept(selectedFile);
            }
            return true;
        } catch (Exception ex) {
            showError("Unable to load image file.");
            return false;
        }
    }

    private void removePhoto() {
        selectedFile = null;
        imageView.setImage(null);
        hideError();
        showUploadState();
        if (onPhotoChanged != null) {
            onPhotoChanged.accept(null);
        }
    }

    private void showUploadState() {
        uploadArea.setVisible(true);
        uploadArea.setManaged(true);
        previewArea.setVisible(false);
        previewArea.setManaged(false);
    }

    private void showPreviewState() {
        uploadArea.setVisible(false);
        uploadArea.setManaged(false);
        previewArea.setVisible(true);
        previewArea.setManaged(true);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public boolean hasPhoto() {
        return selectedFile != null;
    }

    public void setOnPhotoChanged(Consumer<File> callback) {
        this.onPhotoChanged = callback;
    }
}
