package guessmarket.engine.dto;

import java.util.List;

public class OrderBookStateDto
{
    private final int eventId;
    private final String eventName;
    private final String status;
    private final int baseValue;
    private final boolean mintAllowed;
    private final double initialInvestment;
    private final double accountBalance;
    private final double collectedCommission;
    private final List<OptionBookDto> books;
    private final List<ParticipantDto> participants;
    private final String winningOptionName;

    public OrderBookStateDto(int eventId, String eventName, String status, int baseValue, boolean mintAllowed, double initialInvestment, double accountBalance, double collectedCommission, List<OptionBookDto> books, List<ParticipantDto> participants, String winningOptionName)
    {
        this.eventId = eventId;
        this.eventName = eventName;
        this.status = status;
        this.baseValue = baseValue;
        this.mintAllowed = mintAllowed;
        this.initialInvestment = initialInvestment;
        this.accountBalance = accountBalance;
        this.collectedCommission = collectedCommission;
        this.books = books;
        this.participants = participants;
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

    public int getBaseValue()
    {
        return baseValue;
    }

    public boolean isMintAllowed()
    {
        return mintAllowed;
    }

    public double getInitialInvestment()
    {
        return initialInvestment;
    }

    public double getAccountBalance()
    {
        return accountBalance;
    }

    public double getCollectedCommission()
    {
        return collectedCommission;
    }

    public List<OptionBookDto> getBooks()
    {
        return books;
    }

    public List<ParticipantDto> getParticipants()
    {
        return participants;
    }

    public String getWinningOptionName()
    {
        return winningOptionName;
    }
}
