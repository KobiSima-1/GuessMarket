package guessmarket.engine.dto;

public class OptionStateDto
{
    private final String name;
    private final int shares;
    private final double value;

    public OptionStateDto(String name, int shares, double value)
    {
        this.name = name;
        this.shares = shares;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public int getShares()
    {
        return shares;
    }

    public double getValue()
    {
        return value;
    }
}
