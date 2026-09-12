package guessmarket.ui;

import guessmarket.engine.dto.EventDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Locale;

/** Wraps one EventDto for display in the events TableView. */
public class EventRow {

    private final int id;
    private final String methodTypeRaw;
    private final String statusRaw;
    private final String commissionTypeRaw;

    private final StringProperty name;
    private final StringProperty status;
    private final StringProperty methodType;
    private final StringProperty commission;
    private final StringProperty accountBalance;

    public EventRow(EventDto dto) {
        id = dto.getId();
        methodTypeRaw = dto.getMethodType();
        statusRaw = dto.getStatus();
        commissionTypeRaw = dto.getCommissionTypeValue();

        name = new SimpleStringProperty(dto.getName());
        status = new SimpleStringProperty(statusRaw);
        methodType = new SimpleStringProperty(methodTypeRaw);
        commission = new SimpleStringProperty(dto.getCommissionPercent() + "% (" + commissionTypeRaw + ")");
        accountBalance = new SimpleStringProperty(String.format(Locale.US, "%.2f", dto.getAccountBalance()));
    }

    public int getId() {
        return id;
    }

    public String getMethodTypeRaw() {
        return methodTypeRaw;
    }

    public String getStatusRaw() {
        return statusRaw;
    }

    public String getCommissionTypeRaw() {
        return commissionTypeRaw;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty methodTypeProperty() {
        return methodType;
    }

    public StringProperty commissionProperty() {
        return commission;
    }

    public StringProperty accountBalanceProperty() {
        return accountBalance;
    }
}