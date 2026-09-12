package guessmarket.engine.dto;

import java.util.List;

public class EventDto
{
    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final String commissionTypeValue;
    private final String commissionDescription;
    private final String methodType;
    private final String status;
    private final double accountBalance;
    private final String marketMakerName;
    private final List<String> optionNames;

    public EventDto(int id, String name, String description, int commissionPercent, String commissionTypeValue, String commissionDescription, String methodType, String status, double accountBalance, String marketMakerName, List<String> optionNames)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionTypeValue = commissionTypeValue;
        this.commissionDescription = commissionDescription;
        this.methodType = methodType;
        this.status = status;
        this.accountBalance = accountBalance;
        this.marketMakerName = marketMakerName;
        this.optionNames = optionNames;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public int getCommissionPercent()
    {
        return commissionPercent;
    }

    public String getCommissionTypeValue()
    {
        return commissionTypeValue;
    }

    public String getCommissionDescription()
    {
        return commissionDescription;
    }

    public String getMethodType()
    {
        return methodType;
    }

    public String getStatus()
    {
        return status;
    }

    public double getAccountBalance()
    {
        return accountBalance;
    }

    public String getMarketMakerName()
    {
        return marketMakerName;
    }

    public List<String> getOptionNames()
    {
        return optionNames;
    }
}
