package guessmarket.engine.model;

import java.io.Serializable;

public class EventOption implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final String name;
    private int sharesBought;

    public EventOption(String name)
    {
        this.name = name;
        sharesBought = 0;
    }

    public String getName()
    {
        return name;
    }

    public int getSharesBought()
    {
        return sharesBought;
    }

    public void addShares(int amount)
    {
        sharesBought += amount;
    }
}
