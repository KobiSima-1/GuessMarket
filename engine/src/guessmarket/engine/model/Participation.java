package guessmarket.engine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything a single user holds and did inside a single event.
 */
public class Participation implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final int eventId;
    private final int[] shares;
    private final double[] netPaid;
    private final List<Trade> trades;
    private double commissionPaid;
    private double payout;
    private boolean settled;

    public Participation(int eventId, int optionCount)
    {
        this.eventId = eventId;
        shares = new int[optionCount];
        netPaid = new double[optionCount];
        trades = new ArrayList<Trade>();
    }

    public int getEventId()
    {
        return eventId;
    }

    public int getShares(int optionIndex)
    {
        return shares[optionIndex];
    }

    public double getNetPaid(int optionIndex)
    {
        return netPaid[optionIndex];
    }

    public int getOptionCount()
    {
        return shares.length;
    }

    public List<Trade> getTrades()
    {
        return trades;
    }

    public double getCommissionPaid()
    {
        return commissionPaid;
    }

    public double getPayout()
    {
        return payout;
    }

    public boolean isSettled()
    {
        return settled;
    }

    public double getTotalNetPaid()
    {
        double total = 0;
        for (double paid : netPaid)
        {
            total += paid;
        }
        return total;
    }

    /** Profit or loss of the whole participation. Meaningful once the event is closed. */
    public double getProfitOrLoss()
    {
        return payout - getTotalNetPaid() - commissionPaid;
    }

    public void recordBuy(int optionIndex, int quantity, double amount, double commission)
    {
        shares[optionIndex] += quantity;
        netPaid[optionIndex] += amount;
        commissionPaid += commission;
    }

    public void recordSell(int optionIndex, int quantity, double amount)
    {
        shares[optionIndex] -= quantity;
        netPaid[optionIndex] -= amount;
    }

    public void addCommission(double commission)
    {
        commissionPaid += commission;
    }

    public void addTrade(Trade trade)
    {
        trades.add(trade);
    }

    public void settle(double payout)
    {
        this.payout = payout;
        settled = true;
    }
}
