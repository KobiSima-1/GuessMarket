package guessmarket.engine.dto;

import java.util.List;

public class TradeResultDto
{
    private final int filledQuantity;
    private final int restingQuantity;
    private final boolean minted;
    private final double totalAmount;
    private final double totalCommission;
    private final List<TradeDto> trades;
    private final List<String> blockedUsers;

    public TradeResultDto(int filledQuantity, int restingQuantity, boolean minted, double totalAmount, double totalCommission, List<TradeDto> trades, List<String> blockedUsers)
    {
        this.filledQuantity = filledQuantity;
        this.restingQuantity = restingQuantity;
        this.minted = minted;
        this.totalAmount = totalAmount;
        this.totalCommission = totalCommission;
        this.trades = trades;
        this.blockedUsers = blockedUsers;
    }

    public int getFilledQuantity()
    {
        return filledQuantity;
    }

    public int getRestingQuantity()
    {
        return restingQuantity;
    }

    public boolean isMinted()
    {
        return minted;
    }

    public double getTotalAmount()
    {
        return totalAmount;
    }

    public double getTotalCommission()
    {
        return totalCommission;
    }

    public List<TradeDto> getTrades()
    {
        return trades;
    }

    public List<String> getBlockedUsers()
    {
        return blockedUsers;
    }
}
