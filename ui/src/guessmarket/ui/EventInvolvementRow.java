package guessmarket.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class EventInvolvementRow {

    private final int eventId;
    private final StringProperty name;
    private final StringProperty role;
    private final StringProperty methodType;
    private final StringProperty status;

    public EventInvolvementRow(int eventId, String name, String role, String methodType, String status) {
        this.eventId = eventId;
        this.name = new SimpleStringProperty(name);
        this.role = new SimpleStringProperty(role);
        this.methodType = new SimpleStringProperty(methodType);
        this.status = new SimpleStringProperty(status);
    }

    public int getEventId() {
        return eventId;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty roleProperty() {
        return role;
    }

    public StringProperty methodTypeProperty() {
        return methodType;
    }

    public StringProperty statusProperty() {
        return status;
    }
}