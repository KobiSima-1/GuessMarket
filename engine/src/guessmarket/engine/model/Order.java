package guessmarket.engine.model;

import java.io.Serializable;

/**
 * A single order resting in (or being processed against) an order book.
 */
public class Order implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final long id;
    private final String userName;
    private final Side side;
    private final int optionIndex;
    private final double price;
    private final int originalQuantity;
    private int quantity;

    public Order(long id, String userName, Side side, int optionIndex, int quantity, double price)
    {
        this.id = id;
        this.userName = userName;
        this.side = side;
        this.optionIndex = optionIndex;
        this.price = price;
        this.originalQuantity = quantity;
        this.quantity = quantity;
    }

    public long getId()
    {
        return id;
    }

    public String getUserName()
    {
        return userName;
    }

    public Side getSide()
    {
        return side;
    }

    public int getOptionIndex()
    {
        return optionIndex;
    }

    public double getPrice()
    {
        return price;
    }

    public int getOriginalQuantity()
    {
        return originalQuantity;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public boolean isExhausted()
    {
        return quantity <= 0;
    }

    public void reduce(int amount)
    {
        quantity -= amount;
    }
}
