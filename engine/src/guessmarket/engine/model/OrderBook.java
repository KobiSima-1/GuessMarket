package guessmarket.engine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The book of one single option. Bids are kept from the highest price to the
 * lowest, asks from the lowest to the highest; inside the same price the older
 * order comes first.
 */
public class OrderBook implements Serializable
{
    private static final long serialVersionUID = 1L;

    public static final double NO_PRICE = -1;

    private final List<Order> bids;
    private final List<Order> asks;
    private double lastPrice;

    public OrderBook()
    {
        bids = new ArrayList<Order>();
        asks = new ArrayList<Order>();
        lastPrice = NO_PRICE;
    }

    public List<Order> getBids()
    {
        return bids;
    }

    public List<Order> getAsks()
    {
        return asks;
    }

    public double getLastPrice()
    {
        return lastPrice;
    }

    public void setLastPrice(double price)
    {
        lastPrice = price;
    }

    public Order getBestBid()
    {
        if (bids.isEmpty())
        {
            return null;
        }
        return bids.get(0);
    }

    public Order getBestAsk()
    {
        if (asks.isEmpty())
        {
            return null;
        }
        return asks.get(0);
    }

    public double getBestBidPrice()
    {
        Order order = getBestBid();
        if (order == null)
        {
            return NO_PRICE;
        }
        return order.getPrice();
    }

    public double getBestAskPrice()
    {
        Order order = getBestAsk();
        if (order == null)
        {
            return NO_PRICE;
        }
        return order.getPrice();
    }

    /** Average of the best bid and the best ask. NO_PRICE when one of them is missing. */
    public double getMidPrice()
    {
        double bid = getBestBidPrice();
        double ask = getBestAskPrice();
        if (bid == NO_PRICE || ask == NO_PRICE)
        {
            return NO_PRICE;
        }
        return (bid + ask) / 2;
    }

    /** Distance between the best ask and the best bid. NO_PRICE when one of them is missing. */
    public double getSpread()
    {
        double bid = getBestBidPrice();
        double ask = getBestAskPrice();
        if (bid == NO_PRICE || ask == NO_PRICE)
        {
            return NO_PRICE;
        }
        return ask - bid;
    }

    public void add(Order order)
    {
        if (order.getSide() == Side.BUY)
        {
            insert(bids, order, true);
        }
        else
        {
            insert(asks, order, false);
        }
    }

    public void removeExhausted()
    {
        removeExhaustedFrom(bids);
        removeExhaustedFrom(asks);
    }

    public void clear()
    {
        bids.clear();
        asks.clear();
    }

    private void insert(List<Order> book, Order order, boolean highestFirst)
    {
        int index = 0;
        while (index < book.size())
        {
            double price = book.get(index).getPrice();
            boolean comesAfter = highestFirst ? price < order.getPrice() : price > order.getPrice();
            if (comesAfter)
            {
                break;
            }
            index++;
        }
        book.add(index, order);
    }

    private void removeExhaustedFrom(List<Order> book)
    {
        int index = 0;
        while (index < book.size())
        {
            if (book.get(index).isExhausted())
            {
                book.remove(index);
            }
            else
            {
                index++;
            }
        }
    }
}
