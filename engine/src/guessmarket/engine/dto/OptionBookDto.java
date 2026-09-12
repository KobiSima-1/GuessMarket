package guessmarket.engine.dto;

import java.util.List;

public class OptionBookDto
{
    private final int optionIndex;
    private final String optionName;
    private final int totalShares;
    private final List<OrderDto> bids;
    private final List<OrderDto> asks;
    private final double lastPrice;
    private final double bestBid;
    private final double bestAsk;
    private final double midPrice;
    private final double spread;

    public OptionBookDto(int optionIndex, String optionName, int totalShares, List<OrderDto> bids, List<OrderDto> asks, double lastPrice, double bestBid, double bestAsk, double midPrice, double spread)
    {
        this.optionIndex = optionIndex;
        this.optionName = optionName;
        this.totalShares = totalShares;
        this.bids = bids;
        this.asks = asks;
        this.lastPrice = lastPrice;
        this.bestBid = bestBid;
        this.bestAsk = bestAsk;
        this.midPrice = midPrice;
        this.spread = spread;
    }

    public int getOptionIndex()
    {
        return optionIndex;
    }

    public String getOptionName()
    {
        return optionName;
    }

    public int getTotalShares()
    {
        return totalShares;
    }

    public List<OrderDto> getBids()
    {
        return bids;
    }

    public List<OrderDto> getAsks()
    {
        return asks;
    }

    public double getLastPrice()
    {
        return lastPrice;
    }

    public double getBestBid()
    {
        return bestBid;
    }

    public double getBestAsk()
    {
        return bestAsk;
    }

    public double getMidPrice()
    {
        return midPrice;
    }

    public double getSpread()
    {
        return spread;
    }
}
