package guessmarket.engine.dto;

import java.util.List;

public class ParticipationDto
{
    private final int eventId;
    private final String eventName;
    private final String methodType;
    private final String status;
    private final List<String> optionNames;
    private final List<Integer> shares;
    private final List<Double> netPaid;
    private final double commissionPaid;
    private final List<TradeDto> trades;
    private final boolean settled;
    private final double payout;
    private final double profitOrLoss;
    private final String winningOptionName;

    public ParticipationDto(int eventId, String eventName, String methodType, String status, List<String> optionNames, List<Integer> shares, List<Double> netPaid, double commissionPaid, List<TradeDto> trades, boolean settled, double payout, double profitOrLoss, String winningOptionName)
    {
        this.eventId = eventId;
        this.eventName = eventName;
        this.methodType = methodType;
        this.status = status;
        this.optionNames = optionNames;
        this.shares = shares;
        this.netPaid = netPaid;
        this.commissionPaid = commissionPaid;
        this.trades = trades;
        this.settled = settled;
        this.payout = payout;
        this.profitOrLoss = profitOrLoss;
        this.winningOptionName = winningOptionName;
    }

    public int getEventId()
    {
        return eventId;
    }

    public String getEventName()
    {
        return eventName;
    }

    public String getMethodType()
    {
        return methodType;
    }

    public String getStatus()
    {
        return status;
    }

    public List<String> getOptionNames()
    {
        return optionNames;
    }

    public List<Integer> getShares()
    {
        return shares;
    }

    public List<Double> getNetPaid()
    {
        return netPaid;
    }

    public double getCommissionPaid()
    {
        return commissionPaid;
    }

    public List<TradeDto> getTrades()
    {
        return trades;
    }

    public boolean isSettled()
    {
        return settled;
    }

    public double getPayout()
    {
        return payout;
    }

    public double getProfitOrLoss()
    {
        return profitOrLoss;
    }

    public String getWinningOptionName()
    {
        return winningOptionName;
    }
}
