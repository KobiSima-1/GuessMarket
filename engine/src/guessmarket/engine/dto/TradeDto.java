package guessmarket.engine.dto;

public class TradeDto
{
    private final String userName;
    private final String optionName;
    private final String side;
    private final int quantity;
    private final double amount;
    private final double pricePerShare;
    private final double commission;

    public TradeDto(String userName, String optionName, String side, int quantity, double amount, double pricePerShare, double commission)
    {
        this.userName = userName;
        this.optionName = optionName;
        this.side = side;
        this.quantity = quantity;
        this.amount = amount;
        this.pricePerShare = pricePerShare;
        this.commission = commission;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getOptionName()
    {
        return optionName;
    }

    public String getSide()
    {
        return side;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public double getAmount()
    {
        return amount;
    }

    public double getPricePerShare()
    {
        return pricePerShare;
    }

    public double getCommission()
    {
        return commission;
    }
}
