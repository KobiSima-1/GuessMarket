package guessmarket.engine.dto;

public class OrderDto
{
    private final long id;
    private final String userName;
    private final String side;
    private final int quantity;
    private final double price;

    public OrderDto(long id, String userName, String side, int quantity, double price)
    {
        this.id = id;
        this.userName = userName;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
    }

    public long getId()
    {
        return id;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getSide()
    {
        return side;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public double getPrice()
    {
        return price;
    }
}
