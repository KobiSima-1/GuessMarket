package guessmarket.engine.dto;

import java.util.List;

public class UserDto
{
    private final String name;
    private final double balance;
    private final boolean blocked;
    private final List<Integer> marketMakerEventIds;
    private final List<Integer> participatingEventIds;

    public UserDto(String name, double balance, boolean blocked, List<Integer> marketMakerEventIds, List<Integer> participatingEventIds)
    {
        this.name = name;
        this.balance = balance;
        this.blocked = blocked;
        this.marketMakerEventIds = marketMakerEventIds;
        this.participatingEventIds = participatingEventIds;
    }

    public String getName()
    {
        return name;
    }

    public double getBalance()
    {
        return balance;
    }

    public boolean isBlocked()
    {
        return blocked;
    }

    public List<Integer> getMarketMakerEventIds()
    {
        return marketMakerEventIds;
    }

    public List<Integer> getParticipatingEventIds()
    {
        return participatingEventIds;
    }
}
