package guessmarket.engine.dto;

import java.util.List;

public class EventStateDto
{
    private final int eventId;
    private final String eventName;
    private final String status;
    private final double accountBalance;
    private final double collectedCommission;
    private final int b;
    private final List<OptionStateDto> options;
    private final List<TradeDto> trades;
    private final String winningOptionName;

    public EventStateDto(int eventId, String eventName, String status, double accountBalance, double collectedCommission, int b, List<OptionStateDto> options, List<TradeDto> trades, String winningOptionName)
    {
        this.eventId = eventId;
        this.eventName = eventName;
        this.status = status;
        this.accountBalance = accountBalance;
        this.collectedCommission = collectedCommission;
        this.b = b;
        this.options = options;
        this.trades = trades;
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

    public String getStatus()
    {
        return status;
    }

    public double getAccountBalance()
    {
        return accountBalance;
    }

    public double getCollectedCommission()
    {
        return collectedCommission;
    }

    public int getB()
    {
        return b;
    }

    public List<OptionStateDto> getOptions()
    {
        return options;
    }

    public List<TradeDto> getTrades()
    {
        return trades;
    }

    public String getWinningOptionName()
    {
        return winningOptionName;
    }
}
