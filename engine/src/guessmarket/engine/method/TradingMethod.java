package guessmarket.engine.method;

import guessmarket.engine.model.EventOption;

import java.io.Serializable;
import java.util.List;

public abstract class TradingMethod implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final List<EventOption> options;

    protected TradingMethod(List<EventOption> options)
    {
        this.options = options;
    }

    protected List<EventOption> getOptions()
    {
        return options;
    }

    public int getOptionCount()
    {
        return options.size();
    }

    /** Short name of the method, as shown to the user and used by the filters. */
    public abstract String getTypeName();

    /** The amount the market maker has to pay out of his own account when he opens the event. */
    public abstract double getInitialInvestment();

    /** What a single share of the winning option is worth when the event is closed. */
    public abstract double getWinningShareValue();
}
