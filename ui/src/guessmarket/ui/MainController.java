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
import guessmarket.engine.dto.TradeResultDto;
import guessmarket.engine.dto.UserDto;
import javafx.animation.FadeTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MainController {

    @FXML private Label filePathLabel;
    @FXML private Button loadButton;
    @FXML private ProgressBar progressBar;
    @FXML private VBox eventsContainer;
    @FXML private VBox usersContainer;
    @FXML private ComboBox<String> skinCombo;
    @FXML private CheckBox animationsCheckBox;

    private GuessMarketEngine engine;

    private final ObservableList<EventRow> eventsData = FXCollections.observableArrayList();
    private FilteredList<EventRow> eventsFiltered;

    private final List<ToggleButton> methodToggles = new ArrayList<>();
    private final List<ToggleButton> statusToggles = new ArrayList<>();
    private final List<ToggleButton> commissionToggles = new ArrayList<>();

    private final ObservableList<UserRow> usersData = FXCollections.observableArrayList();

    private VBox eventDetailPane;
    private VBox userDetailPane;

    private Integer selectedEventId;
    private String selectedEventMethod;
    private String selectedUserName;

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
    }

    @FXML
    private void initialize() {
        buildEventsTab();
        buildUsersTab();

        skinCombo.setItems(FXCollections.observableArrayList("Default", "Dark", "Sunset"));
        skinCombo.getSelectionModel().selectFirst();
        skinCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> applySkin(newVal));
    }

    // ----- skins (bonus, starts on "Default") -----

    private void applySkin(String name) {
        if (filePathLabel.getScene() == null) {
            return;
        }
        ObservableList<String> stylesheets = filePathLabel.getScene().getStylesheets();
        stylesheets.clear();
        if ("Dark".equals(name)) {
            stylesheets.add(getClass().getResource("/guessmarket/ui/skin-dark.css").toExternalForm());
        } else if ("Sunset".equals(name)) {
            stylesheets.add(getClass().getResource("/guessmarket/ui/skin-sunset.css").toExternalForm());
        }
    }

    // ----- animations (bonus, starts disabled) -----

    private void playFade(Node node, Duration duration) {
        if (animationsCheckBox == null || !animationsCheckBox.isSelected()) {
            node.setOpacity(1);
            return;
        }
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
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

        selectedEventId = null;
        selectedEventMethod = null;
        selectedUserName = null;
        eventDetailPane.getChildren().clear();
        userDetailPane.getChildren().clear();

        List<EventDto> events = engine.getAllEvents();
        eventsData.setAll(events.stream().map(EventRow::new).collect(Collectors.toList()));

        List<UserDto> users = engine.getAllUsers();
        usersData.setAll(users.stream().map(UserRow::new).collect(Collectors.toList()));

        playFade(eventsContainer, Duration.millis(500));
        playFade(usersContainer, Duration.millis(500));

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

    /** Re-pulls everything from the engine after a trading action and redraws whatever is currently shown. */
    private void refreshAfterAction() {
        List<EventDto> events = engine.getAllEvents();
        eventsData.setAll(events.stream().map(EventRow::new).collect(Collectors.toList()));

        List<UserDto> users = engine.getAllUsers();
        usersData.setAll(users.stream().map(UserRow::new).collect(Collectors.toList()));

        if (selectedEventId != null) {
            eventDetailPane.getChildren().clear();
            renderEventDetail();
        }
        if (selectedUserName != null) {
            userDetailPane.getChildren().clear();
            renderUserDetail();
        }
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

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            eventDetailPane.getChildren().clear();
            if (newRow == null) {
                selectedEventId = null;
                selectedEventMethod = null;
                return;
            }
            selectedEventId = newRow.getId();
            selectedEventMethod = newRow.getMethodTypeRaw();
            renderEventDetail();
        });

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

    private void renderEventDetail() {
        if (selectedEventId == null) {
            return;
        }
        VBox detail;
        if ("LMSR".equals(selectedEventMethod)) {
            detail = buildLmsrDetail(selectedEventId);
        } else {
            detail = buildOrderBookDetail(selectedEventId);
        }
        eventDetailPane.getChildren().add(detail);
        playFade(eventDetailPane, Duration.millis(300));
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

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            userDetailPane.getChildren().clear();
            selectedUserName = newRow == null ? null : newRow.getName();
            renderUserDetail();
        });

        usersContainer.getChildren().addAll(table, userDetailPane);
        usersContainer.setSpacing(8);
        usersContainer.setPadding(new Insets(10));
    }

    private void renderUserDetail() {
        if (selectedUserName == null) {
            return;
        }
        UserDto user = engine.getUser(selectedUserName);

        Label header = new Label(String.format(Locale.US, "%s    Balance: %.2f    Blocked: %s",
                user.getName(), user.getBalance(), user.isBlocked() ? "Yes" : "No"));
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        String mmText = user.getMarketMakerEventIds().isEmpty()
                ? "None"
                : user.getMarketMakerEventIds().stream()
                .map(id -> engine.getEvent(id).getName() + " (#" + id + ")")
                .collect(Collectors.joining(", "));
        Label mmLabel = new Label("Market maker of: " + mmText);

        List<ParticipationDto> participations = engine.getParticipations(selectedUserName);
        Label partHeader = new Label("Participating in " + participations.size() + " event(s):");

        VBox partsBox = new VBox(12);
        for (ParticipationDto participation : participations) {
            partsBox.getChildren().add(buildParticipationBlock(participation));
        }

        VBox actions = buildActionsSection(selectedUserName, user);

        userDetailPane.getChildren().addAll(header, mmLabel, partHeader, partsBox, actions);
        playFade(userDetailPane, Duration.millis(300));
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

    // ----- trading actions -----

    private VBox buildActionsSection(String userName, UserDto user) {
        VBox actions = new VBox(8);
        Label header = new Label("Actions:");
        header.setStyle("-fx-font-weight: bold;");
        actions.getChildren().add(header);

        for (Integer eventId : user.getMarketMakerEventIds()) {
            EventDto event = engine.getEvent(eventId);
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().add(new Label(event.getName() + " (#" + eventId + ") - " + event.getStatus()));

            if ("Not started".equals(event.getStatus())) {
                Button openButton = new Button("Open event");
                openButton.setOnAction(e -> handleOpenEvent(userName, eventId));
                row.getChildren().add(openButton);
            } else if ("Active".equals(event.getStatus())) {
                Button closeButton = new Button("Close event");
                closeButton.setOnAction(e -> handleCloseEvent(userName, eventId));
                row.getChildren().add(closeButton);
            }
            actions.getChildren().add(row);
        }

        List<EventDto> activeEvents = engine.getAllEvents().stream()
                .filter(ev -> "Active".equals(ev.getStatus()))
                .collect(Collectors.toList());

        if (!activeEvents.isEmpty()) {
            Map<String, EventDto> lookup = new HashMap<>();
            List<String> labels = new ArrayList<>();
            for (EventDto ev : activeEvents) {
                String label = ev.getName() + " (#" + ev.getId() + ")";
                labels.add(label);
                lookup.put(label, ev);
            }

            Label tradeHeader = new Label("Trade in an active event:");
            ComboBox<String> eventCombo = new ComboBox<>(FXCollections.observableArrayList(labels));
            VBox tradeControls = new VBox(8);

            eventCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                tradeControls.getChildren().clear();
                if (newVal == null) {
                    return;
                }
                EventDto event = lookup.get(newVal);
                if ("LMSR".equals(event.getMethodType())) {
                    tradeControls.getChildren().add(buildLmsrTradeForm(userName, event));
                } else {
                    tradeControls.getChildren().add(buildOrderBookTradeForm(userName, event));
                }
            });

            actions.getChildren().addAll(tradeHeader, eventCombo, tradeControls);
        }

        actions.getChildren().add(new Separator());
        actions.getChildren().add(buildCreateEventForm(userName));

        return actions;
    }

    private VBox buildCreateEventForm(String creatorName) {
        VBox form = new VBox(8);
        Label header = new Label("Create a new event:");
        header.setStyle("-fx-font-weight: bold;");

        TextField nameField = new TextField();
        nameField.setPromptText("Event name");
        TextField descField = new TextField();
        descField.setPromptText("Description");

        Spinner<Integer> commissionSpinner = new Spinner<>(0, 90, 5);
        commissionSpinner.setEditable(true);
        ComboBox<String> commissionTypeCombo = new ComboBox<>(FXCollections.observableArrayList("on-close", "on-purchase"));
        commissionTypeCombo.getSelectionModel().selectFirst();

        TextField option1Field = new TextField();
        option1Field.setPromptText("Option 1");
        TextField option2Field = new TextField();
        option2Field.setPromptText("Option 2");

        ComboBox<String> methodCombo = new ComboBox<>(FXCollections.observableArrayList("LMSR", "Order Book"));
        methodCombo.getSelectionModel().selectFirst();

        VBox methodFields = new VBox(6);

        Spinner<Integer> bSpinner = new Spinner<>(1, 1_000_000, 100);
        bSpinner.setEditable(true);

        Spinner<Integer> dSpinner = new Spinner<>(1, 1_000_000, 1);
        dSpinner.setEditable(true);
        Spinner<Integer> initialSpinner = new Spinner<>(0, 1_000_000, 100);
        initialSpinner.setEditable(true);
        CheckBox allowMintCheck = new CheckBox("Allow mint");

        Runnable rebuildMethodFields = () -> {
            methodFields.getChildren().clear();
            if ("LMSR".equals(methodCombo.getValue())) {
                methodFields.getChildren().add(new HBox(8, new Label("b:"), bSpinner));
            } else {
                HBox row = new HBox(8, new Label("d:"), dSpinner, new Label("Initial:"), initialSpinner, allowMintCheck);
                row.setAlignment(Pos.CENTER_LEFT);
                methodFields.getChildren().add(row);
            }
        };
        rebuildMethodFields.run();
        methodCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> rebuildMethodFields.run());

        Button createButton = new Button("Create event");
        createButton.setOnAction(e -> {
            List<String> optionNames = List.of(option1Field.getText(), option2Field.getText());
            try {
                int newId;
                if ("LMSR".equals(methodCombo.getValue())) {
                    newId = engine.createLmsrEvent(creatorName, nameField.getText(), descField.getText(),
                            commissionSpinner.getValue(), commissionTypeCombo.getValue(), optionNames, bSpinner.getValue());
                } else {
                    newId = engine.createOrderBookEvent(creatorName, nameField.getText(), descField.getText(),
                            commissionSpinner.getValue(), commissionTypeCombo.getValue(), optionNames,
                            allowMintCheck.isSelected(), initialSpinner.getValue(), dSpinner.getValue());
                }
                refreshAfterAction();
                showInfo("Event created", "Event #" + newId + " was created. You are its market maker.");
                nameField.clear();
                descField.clear();
                option1Field.clear();
                option2Field.clear();
            } catch (RuntimeException ex) {
                showError("Could not create the event", ex.getMessage());
            }
        });

        HBox row1 = new HBox(8, new Label("Name:"), nameField, new Label("Description:"), descField);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(8, new Label("Commission %:"), commissionSpinner, new Label("Type:"), commissionTypeCombo);
        row2.setAlignment(Pos.CENTER_LEFT);
        HBox row3 = new HBox(8, new Label("Option 1:"), option1Field, new Label("Option 2:"), option2Field);
        row3.setAlignment(Pos.CENTER_LEFT);
        HBox row4 = new HBox(8, new Label("Method:"), methodCombo);
        row4.setAlignment(Pos.CENTER_LEFT);

        form.getChildren().addAll(header, row1, row2, row3, row4, methodFields, createButton);
        return form;
    }

    private HBox buildLmsrTradeForm(String userName, EventDto event) {
        HBox form = new HBox(8);
        form.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> optionCombo = new ComboBox<>(FXCollections.observableArrayList(event.getOptionNames()));
        optionCombo.getSelectionModel().selectFirst();

        Spinner<Integer> quantitySpinner = new Spinner<>(1, 1_000_000, 1);
        quantitySpinner.setEditable(true);

        Button buyButton = new Button("Buy");
        buyButton.setOnAction(e -> {
            int optionIndex = event.getOptionNames().indexOf(optionCombo.getValue());
            int quantity = quantitySpinner.getValue();
            try {
                TradeResultDto result = engine.buyShares(userName, event.getId(), optionIndex, quantity);
                refreshAfterAction();
                StringBuilder message = new StringBuilder(String.format(Locale.US,
                        "Paid %.2f in total.", result.getTotalAmount() + result.getTotalCommission()));
                if (!result.getBlockedUsers().isEmpty()) {
                    message.append("\n").append(String.join(", ", result.getBlockedUsers()))
                            .append(" went into a negative balance and is now blocked.");
                }
                showInfo("Purchase completed", message.toString());
            } catch (RuntimeException ex) {
                showError("Purchase failed", ex.getMessage());
            }
        });

        form.getChildren().addAll(new Label("Option:"), optionCombo, new Label("Quantity:"), quantitySpinner, buyButton);
        return form;
    }

    private HBox buildOrderBookTradeForm(String userName, EventDto event) {
        HBox form = new HBox(8);
        form.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> optionCombo = new ComboBox<>(FXCollections.observableArrayList(event.getOptionNames()));
        optionCombo.getSelectionModel().selectFirst();

        ComboBox<String> sideCombo = new ComboBox<>(FXCollections.observableArrayList("Buy", "Sell"));
        sideCombo.getSelectionModel().selectFirst();

        Spinner<Integer> quantitySpinner = new Spinner<>(1, 1_000_000, 1);
        quantitySpinner.setEditable(true);

        TextField priceField = new TextField();
        priceField.setPromptText("Price");
        priceField.setPrefWidth(70);

        Button submitButton = new Button("Submit order");
        submitButton.setOnAction(e -> {
            int optionIndex = event.getOptionNames().indexOf(optionCombo.getValue());
            int quantity = quantitySpinner.getValue();
            double price;
            try {
                price = Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException nfe) {
                showError("Invalid price", "Please enter a valid number for the price");
                return;
            }
            try {
                TradeResultDto result = engine.submitOrder(userName, event.getId(), optionIndex,
                        sideCombo.getValue(), quantity, price);
                refreshAfterAction();
                StringBuilder message = new StringBuilder("Filled: " + result.getFilledQuantity());
                if (result.getRestingQuantity() > 0) {
                    message.append(", resting in the book: ").append(result.getRestingQuantity());
                }
                if (result.isMinted()) {
                    message.append(" (mint occurred)");
                }
                if (!result.getBlockedUsers().isEmpty()) {
                    message.append("\n").append(String.join(", ", result.getBlockedUsers()))
                            .append(" went into a negative balance and is now blocked.");
                }
                showInfo("Order submitted", message.toString());
            } catch (RuntimeException ex) {
                showError("Order failed", ex.getMessage());
            }
        });

        form.getChildren().addAll(new Label("Option:"), optionCombo, new Label("Side:"), sideCombo,
                new Label("Qty:"), quantitySpinner, new Label("Price:"), priceField, submitButton);
        return form;
    }

    private void handleOpenEvent(String userName, int eventId) {
        try {
            engine.openEvent(userName, eventId);
            refreshAfterAction();
            showInfo("Event opened", "The event was opened successfully.");
        } catch (RuntimeException ex) {
            showError("Could not open the event", ex.getMessage());
        }
    }

    private void handleCloseEvent(String userName, int eventId) {
        EventDto event = engine.getEvent(eventId);
        ChoiceDialog<String> dialog = new ChoiceDialog<>(event.getOptionNames().get(0), event.getOptionNames());
        dialog.setTitle("Close event");
        dialog.setHeaderText("Choose the winning option for '" + event.getName() + "'");
        dialog.setContentText("Winning option:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(winner -> {
            int optionIndex = event.getOptionNames().indexOf(winner);
            try {
                engine.closeEvent(userName, eventId, optionIndex);
                refreshAfterAction();
                showInfo("Event closed", "The event was closed. Winning option: " + winner);
            } catch (RuntimeException ex) {
                showError("Could not close the event", ex.getMessage());
            }
        });
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