package guessmarket.ui;

import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.dto.EventDto;
import guessmarket.engine.exception.InvalidFileException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainController {

    @FXML private Label filePathLabel;
    @FXML private Button loadButton;
    @FXML private ProgressBar progressBar;
    @FXML private VBox eventsContainer;

    private GuessMarketEngine engine;

    private final ObservableList<EventRow> eventsData = FXCollections.observableArrayList();
    private FilteredList<EventRow> eventsFiltered;

    private final List<ToggleButton> methodToggles = new ArrayList<>();
    private final List<ToggleButton> statusToggles = new ArrayList<>();
    private final List<ToggleButton> commissionToggles = new ArrayList<>();

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
    }

    @FXML
    private void initialize() {
        buildEventsTab();
    }

    // ----- file loading -----

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

        List<EventDto> events = engine.getAllEvents();
        eventsData.setAll(events.stream().map(EventRow::new).collect(Collectors.toList()));

        showInfo("File loaded", "The file is valid. The system now holds " + events.size() + " events.");
    }

    private void onLoadFailed(Throwable error) {
        progressBar.progressProperty().unbind();
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        loadButton.setDisable(false);

        String message = (error != null && error.getMessage() != null)
                ? error.getMessage()
                : "Unknown error while loading the file.";
        showError("The file was not loaded", message);
    }

    // ----- events tab -----

    private void buildEventsTab() {
        HBox methodRow = buildToggleRow("Method", new String[]{"LMSR", "Order Book"}, methodToggles);
        HBox statusRow = buildToggleRow("Status", new String[]{"Not started", "Active", "Closed"}, statusToggles);
        HBox commissionRow = buildToggleRow("Commission", new String[]{"on-close", "on-purchase"}, commissionToggles);

        TableView<EventRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<EventRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());

        TableColumn<EventRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> c.getValue().statusProperty());

        TableColumn<EventRow, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(c -> c.getValue().methodTypeProperty());

        TableColumn<EventRow, String> commissionCol = new TableColumn<>("Commission");
        commissionCol.setCellValueFactory(c -> c.getValue().commissionProperty());

        TableColumn<EventRow, String> balanceCol = new TableColumn<>("Account balance");
        balanceCol.setCellValueFactory(c -> c.getValue().accountBalanceProperty());

        table.getColumns().addAll(nameCol, statusCol, methodCol, commissionCol, balanceCol);

        eventsFiltered = new FilteredList<>(eventsData, row -> true);
        table.setItems(eventsFiltered);

        eventsContainer.getChildren().addAll(methodRow, statusRow, commissionRow, table);
        eventsContainer.setSpacing(8);
        eventsContainer.setPadding(new Insets(10));

        refreshEventsFilter();
    }

    private HBox buildToggleRow(String label, String[] values, List<ToggleButton> registry) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(new Label(label + ":"));
        for (String value : values) {
            ToggleButton button = new ToggleButton(value);
            button.setSelected(true);
            button.selectedProperty().addListener((obs, wasSelected, isSelected) -> refreshEventsFilter());
            registry.add(button);
            box.getChildren().add(button);
        }
        return box;
    }

    private void refreshEventsFilter() {
        if (eventsFiltered == null) {
            return;
        }
        Set<String> methods = selectedValues(methodToggles);
        Set<String> statuses = selectedValues(statusToggles);
        Set<String> commissions = selectedValues(commissionToggles);

        eventsFiltered.setPredicate(row ->
                methods.contains(row.getMethodTypeRaw())
                        && statuses.contains(row.getStatusRaw())
                        && commissions.contains(row.getCommissionTypeRaw()));
    }

    private Set<String> selectedValues(List<ToggleButton> toggles) {
        Set<String> result = new HashSet<>();
        for (ToggleButton toggle : toggles) {
            if (toggle.isSelected()) {
                result.add(toggle.getText());
            }
        }
        return result;
    }

    // ----- alerts -----

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