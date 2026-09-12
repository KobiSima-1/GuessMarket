package guessmarket.engine.model;

import guessmarket.engine.exception.InvalidRequestException;
import guessmarket.engine.method.LmsrMethod;
import guessmarket.engine.method.OrderBookMethod;
import guessmarket.engine.method.TradingMethod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A single event in the market. The event owns its own account (the "contract"),
 * its trading method and the history of the trades that happened inside it.
 */
public class Event implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final List<EventOption> options;
    private final TradingMethod method;
    private final Account account;
    private final List<Trade> trades;
    private final Set<String> participants;
    private String marketMakerName;
    private double collectedCommission;
    private EventStatus status;
    private EventOption winningOption;

    public Event(int id, String name, String description, int commissionPercent, CommissionType commissionType, List<EventOption> options, TradingMethod method)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.options = options;
        this.method = method;
        trades = new ArrayList<Trade>();
        participants = new LinkedHashSet<String>();
        // the event account starts empty. It is filled by the market maker when he opens the event.
        account = new Account(0);
        status = EventStatus.NOT_STARTED;
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

    public CommissionType getCommissionType()
    {
        return commissionType;
    }

    public List<EventOption> getOptions()
    {
        return options;
    }

    public int getOptionCount()
    {
        return options.size();
    }

    public TradingMethod getMethod()
    {
        return method;
    }

    public EventStatus getStatus()
    {
        return status;
    }

    public boolean isActive()
    {
        return status == EventStatus.ACTIVE;
    }

    public double getAccountBalance()
    {
        return account.getBalance();
    }

    public double getCollectedCommission()
    {
        return collectedCommission;
    }

    public List<Trade> getTrades()
    {
        return trades;
    }

    public Set<String> getParticipants()
    {
        return participants;
    }

    public String getMarketMakerName()
    {
        return marketMakerName;
    }

    public void setMarketMakerName(String marketMakerName)
    {
        this.marketMakerName = marketMakerName;
    }

    public EventOption getWinningOption()
    {
        return winningOption;
    }

    public boolean isOrderBook()
    {
        return method instanceof OrderBookMethod;
    }

    public OrderBookMethod getOrderBookMethod()
    {
        return (OrderBookMethod) method;
    }

    public LmsrMethod getLmsrMethod()
    {
        return (LmsrMethod) method;
    }

    /**
     * Opens the event. Only the market maker can do this, and only if he has
     * enough money for the initial investment.
     */
    public void open(User marketMaker)
    {
        if (status != EventStatus.NOT_STARTED)
        {
            throw new InvalidRequestException("Event '" + name + "' was already started");
        }
        if (!marketMaker.getName().equals(marketMakerName))
        {
            throw new InvalidRequestException("Only " + marketMakerName + ", the market maker of this event, can start it");
        }
        if (marketMaker.isBlocked())
        {
            throw new InvalidRequestException("User " + marketMaker.getName() + " is blocked and cannot act in the system");
        }

        double investment = method.getInitialInvestment();
        if (!marketMaker.canAfford(investment))
        {
            throw new InvalidRequestException(String.format(java.util.Locale.US,
                    "%s needs %.2f to start this event but his balance is only %.2f",
                    marketMaker.getName(), investment, marketMaker.getBalance()));
        }

        marketMaker.pay(investment);
        account.deposit(investment);

        if (isOrderBook())
        {
            // the market maker gets one pair of shares for every base value he paid
            OrderBookMethod orderBook = getOrderBookMethod();
            int pairs = orderBook.getInitialPairs();
            if (pairs > 0)
            {
                Participation participation = marketMaker.openParticipation(id, options.size());
                double pricePerShare = orderBook.getD() / 2.0;
                for (int i = 0; i < options.size(); i++)
                {
                    options.get(i).addShares(pairs);
                    participation.recordBuy(i, pairs, pairs * pricePerShare, 0);
                }
                participants.add(marketMaker.getName());
            }
        }

        status = EventStatus.ACTIVE;
    }

    /** A purchase of shares in an LMSR event. */
    public TradeResult buyLmsr(User user, int optionIndex, int quantity, UserLookup users)
    {
        checkTradingAllowed(user);
        checkOption(optionIndex);
        if (quantity <= 0)
        {
            throw new InvalidRequestException("The amount of shares has to be a positive number");
        }

        LmsrMethod lmsr = getLmsrMethod();
        double cost = lmsr.getBuyCost(optionIndex, quantity);
        double commission = 0;
        if (commissionType == CommissionType.ON_PURCHASE)
        {
            commission = cost * commissionPercent / 100.0;
        }

        TradeResult result = new TradeResult();
        options.get(optionIndex).addShares(quantity);
        account.deposit(cost);
        payFor(user, cost + commission, result);
        payCommission(commission, users, result);

        Participation participation = user.openParticipation(id, options.size());
        participation.recordBuy(optionIndex, quantity, cost, commission);
        participants.add(user.getName());

        Trade trade = new Trade(user.getName(), options.get(optionIndex).getName(), Side.BUY, quantity, cost, commission);
        participation.addTrade(trade);
        trades.add(trade);
        result.addTrade(trade);
        result.addFilled(quantity);
        return result;
    }

    /** Submits an order to the book of one option and processes the book right away. */
    public TradeResult submitOrder(User user, Side side, int optionIndex, int quantity, double price, UserLookup users)
    {
        checkTradingAllowed(user);
        checkOption(optionIndex);
        OrderBookMethod book = getOrderBookMethod();

        if (quantity <= 0)
        {
            throw new InvalidRequestException("The amount of shares has to be a positive number");
        }
        if (price <= 0)
        {
            throw new InvalidRequestException("The price has to be a positive number");
        }
        if (price > book.getMaxPrice() + 1e-9)
        {
            throw new InvalidRequestException(String.format(java.util.Locale.US,
                    "The price cannot be higher than %.2f (the base value of this event is %d)",
                    book.getMaxPrice(), book.getD()));
        }
        if (side == Side.SELL)
        {
            int available = availableSharesToSell(user, optionIndex, book);
            if (available < quantity)
            {
                throw new InvalidRequestException("You only have " + available + " shares of '" + options.get(optionIndex).getName() + "' available to sell");
            }
        }

        TradeResult result = new TradeResult();
        Order incoming = new Order(book.takeOrderId(), user.getName(), side, optionIndex, quantity, price);
        user.openParticipation(id, options.size());
        participants.add(user.getName());

        while (!incoming.isExhausted())
        {
            if (matchOnce(incoming, book, users, result))
            {
                continue;
            }
            if (book.isMintAllowed() && mintOnce(incoming, book, users, result))
            {
                continue;
            }
            break;
        }

        if (!incoming.isExhausted())
        {
            book.getBook(optionIndex).add(incoming);
            result.setRestingQuantity(incoming.getQuantity());
        }
        return result;
    }

    /**
     * Closes the event and decides the winning option. The event account is
     * emptied to the holders of the winning option.
     */
    public void close(User marketMaker, int optionIndex, UserLookup users)
    {
        if (status != EventStatus.ACTIVE)
        {
            throw new InvalidRequestException("Only an active event can be closed");
        }
        if (!marketMaker.getName().equals(marketMakerName))
        {
            throw new InvalidRequestException("Only " + marketMakerName + ", the market maker of this event, can close it");
        }
        checkOption(optionIndex);

        winningOption = options.get(optionIndex);
        double shareValue = method.getWinningShareValue();

        for (String participantName : participants)
        {
            User participant = users.find(participantName);
            Participation participation = participant.getParticipation(id);
            if (participation == null)
            {
                continue;
            }

            double payout = participation.getShares(optionIndex) * shareValue;
            double commission = 0;
            if (commissionType == CommissionType.ON_CLOSE && payout > 0)
            {
                commission = payout * commissionPercent / 100.0;
            }

            if (payout > 0)
            {
                account.withdraw(payout);
                participant.receive(payout - commission);
            }
            if (commission > 0)
            {
                participation.addCommission(commission);
                collectedCommission += commission;
                users.find(marketMakerName).receive(commission);
            }
            participation.settle(payout - commission);
        }

        // whatever is left in the event account goes back to the market maker
        double leftOver = account.getBalance();
        if (leftOver > 0)
        {
            account.withdraw(leftOver);
            users.find(marketMakerName).receive(leftOver);
        }

        if (isOrderBook())
        {
            getOrderBookMethod().closeBooks();
        }
        status = EventStatus.CLOSED;
    }

    private boolean matchOnce(Order incoming, OrderBookMethod book, UserLookup users, TradeResult result)
    {
        OrderBook optionBook = book.getBook(incoming.getOptionIndex());
        Order resting;
        if (incoming.getSide() == Side.BUY)
        {
            resting = optionBook.getBestAsk();
            if (resting == null || resting.getPrice() > incoming.getPrice() + 1e-9)
            {
                return false;
            }
        }
        else
        {
            resting = optionBook.getBestBid();
            if (resting == null || resting.getPrice() < incoming.getPrice() - 1e-9)
            {
                return false;
            }
        }

        int tradedQuantity = Math.min(incoming.getQuantity(), resting.getQuantity());
        double tradePrice = resting.getPrice();
        double amount = tradedQuantity * tradePrice;

        User buyer = users.find(incoming.getSide() == Side.BUY ? incoming.getUserName() : resting.getUserName());
        User seller = users.find(incoming.getSide() == Side.BUY ? resting.getUserName() : incoming.getUserName());

        double commission = 0;
        if (commissionType == CommissionType.ON_PURCHASE)
        {
            commission = amount * commissionPercent / 100.0;
        }

        payFor(buyer, amount + commission, result);
        seller.receive(amount);
        payCommission(commission, users, result);

        moveShares(buyer, seller, incoming.getOptionIndex(), tradedQuantity, amount, commission);
        recordFill(buyer, seller, incoming.getOptionIndex(), tradedQuantity, amount, commission, result);

        optionBook.setLastPrice(tradePrice);
        incoming.reduce(tradedQuantity);
        resting.reduce(tradedQuantity);
        optionBook.removeExhausted();
        result.addFilled(tradedQuantity);
        return true;
    }

    /**
     * Creates new shares when two buyers - one on each option - together offer at
     * least the base value. The order that was already waiting keeps its own price,
     * the new order pays what is left up to the base value.
     */
    private boolean mintOnce(Order incoming, OrderBookMethod book, UserLookup users, TradeResult result)
    {
        if (incoming.getSide() != Side.BUY || options.size() != 2)
        {
            return false;
        }

        int otherIndex = 1 - incoming.getOptionIndex();
        Order resting = book.getBook(otherIndex).getBestBid();
        if (resting == null || resting.getPrice() + incoming.getPrice() < book.getD() - 1e-9)
        {
            return false;
        }

        int quantity = Math.min(incoming.getQuantity(), resting.getQuantity());
        double restingPrice = resting.getPrice();
        double incomingPrice = book.getD() - restingPrice;

        User restingUser = users.find(resting.getUserName());
        User incomingUser = users.find(incoming.getUserName());

        mintSide(restingUser, otherIndex, quantity, restingPrice, users, result);
        mintSide(incomingUser, incoming.getOptionIndex(), quantity, incomingPrice, users, result);
        account.deposit(quantity * book.getD());

        resting.reduce(quantity);
        incoming.reduce(quantity);
        book.getBook(otherIndex).removeExhausted();
        result.addFilled(quantity);
        result.setMinted(true);
        return true;
    }

    private void mintSide(User user, int optionIndex, int quantity, double price, UserLookup users, TradeResult result)
    {
        double amount = quantity * price;
        double commission = 0;
        if (commissionType == CommissionType.ON_PURCHASE)
        {
            commission = amount * commissionPercent / 100.0;
        }

        payFor(user, amount + commission, result);
        payCommission(commission, users, result);
        options.get(optionIndex).addShares(quantity);

        Participation participation = user.openParticipation(id, options.size());
        participation.recordBuy(optionIndex, quantity, amount, commission);
        participants.add(user.getName());

        Trade trade = new Trade(user.getName(), options.get(optionIndex).getName(), Side.BUY, quantity, amount, commission);
        participation.addTrade(trade);
        trades.add(trade);
        result.addTrade(trade);
    }

    private void moveShares(User buyer, User seller, int optionIndex, int quantity, double amount, double commission)
    {
        Participation buyerParticipation = buyer.openParticipation(id, options.size());
        buyerParticipation.recordBuy(optionIndex, quantity, amount, commission);
        Participation sellerParticipation = seller.openParticipation(id, options.size());
        sellerParticipation.recordSell(optionIndex, quantity, amount);
        participants.add(buyer.getName());
        participants.add(seller.getName());
    }

    private void recordFill(User buyer, User seller, int optionIndex, int quantity, double amount, double commission, TradeResult result)
    {
        String optionName = options.get(optionIndex).getName();
        Trade buyTrade = new Trade(buyer.getName(), optionName, Side.BUY, quantity, amount, commission);
        Trade sellTrade = new Trade(seller.getName(), optionName, Side.SELL, quantity, amount, 0);

        buyer.getParticipation(id).addTrade(buyTrade);
        seller.getParticipation(id).addTrade(sellTrade);
        trades.add(buyTrade);
        trades.add(sellTrade);
        result.addTrade(buyTrade);
        result.addTrade(sellTrade);
    }

    private int availableSharesToSell(User user, int optionIndex, OrderBookMethod book)
    {
        Participation participation = user.getParticipation(id);
        int owned = participation == null ? 0 : participation.getShares(optionIndex);
        for (Order order : book.getBook(optionIndex).getAsks())
        {
            if (order.getUserName().equals(user.getName()))
            {
                owned -= order.getQuantity();
            }
        }
        return owned;
    }

    private void payFor(User user, double amount, TradeResult result)
    {
        if (user.pay(amount))
        {
            result.addBlockedUser(user.getName());
        }
    }

    /** Commissions always end up in the account of the market maker of the event. */
    private void payCommission(double commission, UserLookup users, TradeResult result)
    {
        if (commission <= 0)
        {
            return;
        }
        collectedCommission += commission;
        users.find(marketMakerName).receive(commission);
    }

    private void checkTradingAllowed(User user)
    {
        if (status != EventStatus.ACTIVE)
        {
            throw new InvalidRequestException("Event '" + name + "' is " + status.getDescription().toLowerCase() + " - no trading is possible in it");
        }
        if (user.isBlocked())
        {
            throw new InvalidRequestException("User " + user.getName() + " is blocked and cannot act in the system");
        }
    }

    private void checkOption(int optionIndex)
    {
        if (optionIndex < 0 || optionIndex >= options.size())
        {
            throw new InvalidRequestException("There is no option number " + (optionIndex + 1) + " in this event");
        }
    }
}
