package guessmarket.engine.model;

public enum CommissionType
{
    ON_PURCHASE("on-purchase", "on every purchase"),
    ON_CLOSE("on-close", "when the event is closed");

    private final String fileValue;
    private final String description;

    CommissionType(String fileValue, String description)
    {
        this.fileValue = fileValue;
        this.description = description;
    }

    public String getFileValue()
    {
        return fileValue;
    }

    public String getDescription()
    {
        return description;
    }

    public static CommissionType fromFileValue(String value)
    {
        for (CommissionType type : values())
        {
            if (type.fileValue.equalsIgnoreCase(value))
            {
                return type;
            }
        }
        return null;
    }
}
