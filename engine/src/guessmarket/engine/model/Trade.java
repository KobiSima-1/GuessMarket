package guessmarket.engine.model;

import java.io.Serializable;

/**
 * A single executed trade. Used by both trading methods:
 * in LMSR every purchase creates one trade, in the order book every fill
 * (or mint) creates one trade for each side.
 */
public class Trade implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final String userName;
    private final String optionName;
    private final Side side;
    private final int quantity;
    private final double amount;
    private final double commission;

    public Trade(String userName, String optionName, Side side, int quantity, double amount, double commission)
    {
        this.userName = userName;
        this.optionName = optionName;
        this.side = side;
        this.quantity = quantity;
        this.amount = amount;
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

    public Side getSide()
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

    public double getCommission()
    {
        return commission;
    }

    public double getPricePerShare()
    {
        if (quantity == 0)
        {
            return 0;
        }
        return amount / quantity;
    }

    public double getTotalPaid()
    {
        return amount + commission;
    }
}
