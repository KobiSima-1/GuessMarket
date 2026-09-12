package guessmarket.ui;

import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.exception.InvalidFileException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainController {

    @FXML private Label filePathLabel;
    @FXML private Button loadButton;
    @FXML private ProgressBar progressBar;

    private GuessMarketEngine engine;

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
    }

    @FXML
    private void handleLoadFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an events file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));

        Stage stage = (Stage) loadButton.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        Task<Void> loadTask = buildLoadTask(file.getAbsolutePath());

        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.progressProperty().bind(loadTask.progressProperty());
        loadButton.setDisable(true);

        loadTask.setOnSucceeded(e -> onLoadFinished(file));
        loadTask.setOnFailed(e -> onLoadFailed(loadTask.getException()));

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private Task<Void> buildLoadTask(String path) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // the actual read is fast; a short artificial delay makes the
                // progress bar visible, as the assignment asks for.
                for (int i = 1; i <= 5; i++) {
                    updateProgress(i, 10);
                    Thread.sleep(150);
                }
                engine.loadEventsFile(path);
                for (int i = 6; i <= 10; i++) {
                    updateProgress(i, 10);
                    Thread.sleep(80);
                }
                return null;
            }
        };
    }

    private void onLoadFinished(File file) {
        progressBar.progressProperty().unbind();
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        loadButton.setDisable(false);
        filePathLabel.setText(file.getAbsolutePath());
        int count = engine.getAllEvents().size();
        showInfo("File loaded", "The file is valid. The system now holds " + count + " events.");
    }

    private void onLoadFailed(Throwable error) {
        progressBar.progressProperty().unbind();
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        loadButton.setDisable(false);

        String message = (error instanceof InvalidFileException || error != null)
                ? error.getMessage()
                : "Unknown error while loading the file.";
        showError("The file was not loaded", message);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}