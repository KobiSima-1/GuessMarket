package guessmarket.ui;

import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.dto.EventDto;
import guessmarket.engine.dto.EventStateDto;
import guessmarket.engine.dto.OptionBookDto;
import guessmarket.engine.dto.OptionStateDto;
import guessmarket.engine.dto.OrderBookStateDto;
import guessmarket.engine.dto.OrderDto;
import guessmarket.engine.dto.ParticipantDto;
import guessmarket.engine.dto.ParticipationDto;
import guessmarket.engine.dto.TradeDto;
import guessmarket.engine.dto.UserDto;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class MainController {

    @FXML private Label filePathLabel;
    @FXML private Button loadButton;
    @FXML private ProgressBar progressBar;
    @FXML private VBox eventsContainer;
    @FXML private VBox usersContainer;

    private GuessMarketEngine engine;

    private final ObservableList<EventRow> eventsData = FXCollections.observableArrayList();
    private FilteredList<EventRow> eventsFiltered;

    private final List<ToggleButton> methodToggles = new ArrayList<>();
    private final List<ToggleButton> statusToggles = new ArrayList<>();
    private final List<ToggleButton> commissionToggles = new ArrayList<>();

    private final ObservableList<UserRow> usersData = FXCollections.observableArrayList();

    private VBox eventDetailPane;
    private VBox userDetailPane;

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
    }

    @FXML
    private void initialize() {
        buildEventsTab();
        buildUsersTab();
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

        eventDetailPane.getChildren().clear();
        userDetailPane.getChildren().clear();

        List<EventDto> events = engine.getAllEvents();
        eventsData.setAll(events.stream().map(EventRow::new).collect(Collectors.toList()));

        List<UserDto> users = engine.getAllUsers();
        usersData.setAll(users.stream().map(UserRow::new).collect(Collectors.toList()));

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
        table.setPrefHeight(220);

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

        eventDetailPane = new VBox(10);
        eventDetailPane.setPadding(new Insets(10, 0, 0, 0));

        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldRow, newRow) -> showEventDetails(newRow));

        eventsContainer.getChildren().addAll(methodRow, statusRow, commissionRow, table, eventDetailPane);
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

    private void showEventDetails(EventRow row) {
        eventDetailPane.getChildren().clear();
        if (row == null) {
            return;
        }
        if ("LMSR".equals(row.getMethodTypeRaw())) {
            eventDetailPane.getChildren().add(buildLmsrDetail(row.getId()));
        } else {
            eventDetailPane.getChildren().add(buildOrderBookDetail(row.getId()));
        }
    }

    private VBox buildLmsrDetail(int eventId) {
        EventStateDto state = engine.getLmsrState(eventId);
        VBox box = new VBox(6);

        String title = state.getEventName() + " - LMSR";
        if (state.getWinningOptionName() != null) {
            title += "  (winner: " + state.getWinningOptionName() + ")";
        }
        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label summary = new Label(String.format(Locale.US,
                "Event account: %.2f    Commission collected: %.2f    b: %d",
                state.getAccountBalance(), state.getCollectedCommission(), state.getB()));

        TableView<OptionStateDto> optionsTable = buildOptionsTable();
        optionsTable.getItems().addAll(state.getOptions());

        Label tradesHeader = new Label("Trade history (most recent first):");
        TableView<TradeDto> tradesTable = buildTradesTable();
        tradesTable.getItems().addAll(state.getTrades());

        box.getChildren().addAll(header, summary, optionsTable, tradesHeader, tradesTable);
        return box;
    }

    private VBox buildOrderBookDetail(int eventId) {
        OrderBookStateDto state = engine.getOrderBookState(eventId);
        VBox box = new VBox(10);

        String title = state.getEventName() + " - Order Book";
        if (state.getWinningOptionName() != null) {
            title += "  (winner: " + state.getWinningOptionName() + ")";
        }
        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label summary = new Label(String.format(Locale.US,
                "Event account: %.2f    Base value (d): %d    Mint allowed: %s    Commission collected: %.2f",
                state.getAccountBalance(), state.getBaseValue(), state.isMintAllowed(), state.getCollectedCommission()));

        box.getChildren().addAll(header, summary);

        for (OptionBookDto optionBook : state.getBooks()) {
            box.getChildren().add(buildOptionBookBlock(optionBook));
        }

        Label participantsHeader = new Label("Participants:");
        TableView<ParticipantDto> participantsTable = new TableView<>();
        participantsTable.setPrefHeight(120);

        TableColumn<ParticipantDto, String> pUser = new TableColumn<>("User");
        pUser.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUserName()));
        TableColumn<ParticipantDto, String> pHoldings = new TableColumn<>("Holdings");
        pHoldings.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatHoldings(c.getValue(), state.getBooks())));
        TableColumn<ParticipantDto, String> pCommission = new TableColumn<>("Commission paid");
        pCommission.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                String.format(Locale.US, "%.2f", c.getValue().getCommissionPaid())));
        participantsTable.getColumns().addAll(pUser, pHoldings, pCommission);
        participantsTable.getItems().addAll(state.getParticipants());

        box.getChildren().addAll(participantsHeader, participantsTable);
        return box;
    }

    private VBox buildOptionBookBlock(OptionBookDto book) {
        VBox optionBox = new VBox(4);

        Label optionHeader = new Label(book.getOptionName() + "  (shares outstanding: " + book.getTotalShares() + ")");
        optionHeader.setStyle("-fx-font-weight: bold;");

        Label stats = new Label(String.format(Locale.US,
                "LAST: %s    BID: %s    ASK: %s    MID: %s    SPREAD: %s",
                fmtPrice(book.getLastPrice()), fmtPrice(book.getBestBid()), fmtPrice(book.getBestAsk()),
                fmtPrice(book.getMidPrice()), fmtPrice(book.getSpread())));

        TableView<OrderDto> bidsTable = buildOrdersTable();
        bidsTable.getItems().addAll(book.getBids());
        TableView<OrderDto> asksTable = buildOrdersTable();
        asksTable.getItems().addAll(book.getAsks());

        HBox tablesRow = new HBox(12,
                buildLabeledBox("Bids", bidsTable),
                buildLabeledBox("Asks", asksTable));

        optionBox.getChildren().addAll(optionHeader, stats, tablesRow);
        return optionBox;
    }

    private TableView<OrderDto> buildOrdersTable() {
        TableView<OrderDto> table = new TableView<>();
        table.setPrefHeight(110);
        table.setPrefWidth(280);

        TableColumn<OrderDto, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        TableColumn<OrderDto, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<OrderDto, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        table.getColumns().addAll(userCol, qtyCol, priceCol);
        return table;
    }

    private VBox buildLabeledBox(String label, TableView<?> table) {
        VBox box = new VBox(4);
        box.getChildren().addAll(new Label(label), table);
        return box;
    }

    private String formatHoldings(ParticipantDto participant, List<OptionBookDto> books) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < books.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            int shares = participant.getShares().get(i);
            double paid = participant.getNetPaid().get(i);
            sb.append(String.format(Locale.US, "%s: %d (paid %.2f)", books.get(i).getOptionName(), shares, paid));
        }
        return sb.toString();
    }

    private String fmtPrice(double value) {
        return value < 0 ? "-" : String.format(Locale.US, "%.2f", value);
    }

    private TableView<OptionStateDto> buildOptionsTable() {
        TableView<OptionStateDto> table = new TableView<>();
        table.setPrefHeight(90);
        TableColumn<OptionStateDto, String> optName = new TableColumn<>("Option");
        optName.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<OptionStateDto, Number> optShares = new TableColumn<>("Shares bought");
        optShares.setCellValueFactory(new PropertyValueFactory<>("shares"));
        TableColumn<OptionStateDto, Number> optValue = new TableColumn<>("Current price");
        optValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        table.getColumns().addAll(optName, optShares, optValue);
        return table;
    }

    private TableView<TradeDto> buildTradesTable() {
        TableView<TradeDto> table = new TableView<>();
        table.setPrefHeight(140);
        table.setPlaceholder(new Label("No trades yet"));

        TableColumn<TradeDto, String> tUser = new TableColumn<>("User");
        tUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        TableColumn<TradeDto, String> tOption = new TableColumn<>("Option");
        tOption.setCellValueFactory(new PropertyValueFactory<>("optionName"));
        TableColumn<TradeDto, Number> tQuantity = new TableColumn<>("Quantity");
        tQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<TradeDto, Number> tAmount = new TableColumn<>("Amount paid");
        tAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<TradeDto, Number> tCommission = new TableColumn<>("Commission");
        tCommission.setCellValueFactory(new PropertyValueFactory<>("commission"));
        table.getColumns().addAll(tUser, tOption, tQuantity, tAmount, tCommission);
        return table;
    }

    // ----- users tab -----

    private void buildUsersTab() {
        TableView<UserRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(180);

        TableColumn<UserRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        TableColumn<UserRow, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(c -> c.getValue().balanceProperty());
        TableColumn<UserRow, String> blockedCol = new TableColumn<>("Blocked");
        blockedCol.setCellValueFactory(c -> c.getValue().blockedProperty());
        table.getColumns().addAll(nameCol, balanceCol, blockedCol);
        table.setItems(usersData);

        userDetailPane = new VBox(10);
        userDetailPane.setPadding(new Insets(10, 0, 0, 0));

        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldRow, newRow) -> showUserDetails(newRow == null ? null : newRow.getName()));

        usersContainer.getChildren().addAll(table, userDetailPane);
        usersContainer.setSpacing(8);
        usersContainer.setPadding(new Insets(10));
    }

    private void showUserDetails(String userName) {
        userDetailPane.getChildren().clear();
        if (userName == null) {
            return;
        }

        UserDto user = engine.getUser(userName);
        Label header = new Label(String.format(Locale.US, "%s    Balance: %.2f    Blocked: %s",
                user.getName(), user.getBalance(), user.isBlocked() ? "Yes" : "No"));
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        String mmText = user.getMarketMakerEventIds().isEmpty()
                ? "None"
                : user.getMarketMakerEventIds().stream()
                .map(id -> engine.getEvent(id).getName() + " (#" + id + ")")
                .collect(Collectors.joining(", "));
        Label mmLabel = new Label("Market maker of: " + mmText);

        List<ParticipationDto> participations = engine.getParticipations(userName);
        Label partHeader = new Label("Participating in " + participations.size() + " event(s):");

        VBox partsBox = new VBox(12);
        for (ParticipationDto participation : participations) {
            partsBox.getChildren().add(buildParticipationBlock(participation));
        }

        userDetailPane.getChildren().addAll(header, mmLabel, partHeader, partsBox);
    }

    private VBox buildParticipationBlock(ParticipationDto participation) {
        VBox box = new VBox(6);

        Label titleLabel = new Label(participation.getEventName() + " - " + participation.getMethodType()
                + " (" + participation.getStatus() + ")");
        titleLabel.setStyle("-fx-font-weight: bold;");
        box.getChildren().add(titleLabel);

        if ("LMSR".equals(participation.getMethodType())) {
            TableView<TradeDto> tradesTable = buildTradesTable();
            tradesTable.setPrefHeight(110);
            tradesTable.getItems().addAll(participation.getTrades());
            box.getChildren().add(tradesTable);

            if (participation.isSettled()) {
                EventStateDto finalState = engine.getLmsrState(participation.getEventId());
                Label finalHeader = new Label("Final results for this event (all users):");
                TableView<OptionStateDto> finalOptions = buildOptionsTable();
                finalOptions.getItems().addAll(finalState.getOptions());
                box.getChildren().addAll(finalHeader, finalOptions);
            }
        } else {
            VBox holdings = new VBox(2);
            List<String> optionNames = participation.getOptionNames();
            for (int i = 0; i < optionNames.size(); i++) {
                int shares = participation.getShares().get(i);
                double paid = participation.getNetPaid().get(i);
                holdings.getChildren().add(new Label(String.format(Locale.US,
                        "%s: %d shares (paid %.2f)", optionNames.get(i), shares, paid)));
            }
            box.getChildren().add(holdings);
            box.getChildren().add(new Label(String.format(Locale.US,
                    "Commission paid: %.2f", participation.getCommissionPaid())));
        }

        if (participation.isSettled()) {
            box.getChildren().add(new Label(String.format(Locale.US,
                    "Event closed - winning option: %s    Your payout: %.2f    Profit/Loss: %.2f",
                    participation.getWinningOptionName(), participation.getPayout(), participation.getProfitOrLoss())));
        }

        return box;
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