package guessmarket.engine.model;

public enum Side
{
    BUY("Buy"),
    SELL("Sell");

    private final String description;

    Side(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public static Side fromDescription(String value)
    {
        for (Side side : values())
        {
            if (side.description.equalsIgnoreCase(value) || side.name().equalsIgnoreCase(value))
            {
                return side;
            }
        }
        return null;
    }
}
