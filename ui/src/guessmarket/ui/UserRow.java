package guessmarket.ui;

import guessmarket.engine.dto.UserDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Locale;

public class UserRow {

    private final String name;
    private final StringProperty nameProperty;
    private final StringProperty balanceProperty;
    private final StringProperty blockedProperty;

    public UserRow(UserDto dto) {
        name = dto.getName();
        nameProperty = new SimpleStringProperty(name);
        balanceProperty = new SimpleStringProperty(String.format(Locale.US, "%.2f", dto.getBalance()));
        blockedProperty = new SimpleStringProperty(dto.isBlocked() ? "Yes" : "No");
    }

    public String getName() {
        return name;
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public StringProperty balanceProperty() {
        return balanceProperty;
    }

    public StringProperty blockedProperty() {
        return blockedProperty;
    }
}