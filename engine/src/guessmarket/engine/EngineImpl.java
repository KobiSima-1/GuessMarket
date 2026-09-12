package guessmarket.engine;

import guessmarket.engine.dto.EventDto;
import guessmarket.engine.dto.EventStateDto;
import guessmarket.engine.dto.OptionBookDto;
import guessmarket.engine.dto.OptionStateDto;
import guessmarket.engine.dto.OrderBookStateDto;
import guessmarket.engine.dto.OrderDto;
import guessmarket.engine.dto.ParticipantDto;
import guessmarket.engine.dto.ParticipationDto;
import guessmarket.engine.dto.TradeDto;
import guessmarket.engine.dto.TradeResultDto;
import guessmarket.engine.dto.UserDto;
import guessmarket.engine.exception.InvalidFileException;
import guessmarket.engine.exception.InvalidRequestException;
import guessmarket.engine.method.LmsrMethod;
import guessmarket.engine.method.OrderBookMethod;
import guessmarket.engine.model.Event;
import guessmarket.engine.model.EventOption;
import guessmarket.engine.model.Market;
import guessmarket.engine.model.Order;
import guessmarket.engine.model.OrderBook;
import guessmarket.engine.model.Participation;
import guessmarket.engine.model.Side;
import guessmarket.engine.model.Trade;
import guessmarket.engine.model.TradeResult;
import guessmarket.engine.model.User;
import guessmarket.engine.xml.MarketLoader;
import guessmarket.engine.model.CommissionType;

import java.util.ArrayList;
import java.util.List;

public class EngineImpl implements GuessMarketEngine
{
    private Market market;
    private String loadedFilePath;

    @Override
    public void loadEventsFile(String path) throws InvalidFileException
    {
        MarketLoader loader = new MarketLoader();
        Market loaded = loader.load(path);
        // only after a fully successful load the new file replaces the old one
        market = loaded;
        loadedFilePath = path;
    }

    @Override
    public boolean isFileLoaded()
    {
        return market != null;
    }

    @Override
    public String getLoadedFilePath()
    {
        return loadedFilePath;
    }

    @Override
    public List<EventDto> getAllEvents()
    {
        checkLoaded();
        List<EventDto> result = new ArrayList<EventDto>();
        for (Event event : market.getEvents())
        {
            result.add(toEventDto(event));
        }
        return result;
    }

    @Override
    public EventDto getEvent(int eventId)
    {
        return toEventDto(requireEvent(eventId));
    }

    @Override
    public EventStateDto getLmsrState(int eventId)
    {
        Event event = requireEvent(eventId);
        if (event.isOrderBook())
        {
            throw new InvalidRequestException("Event number " + eventId + " is not an LMSR event");
        }

        LmsrMethod lmsr = event.getLmsrMethod();
        List<OptionStateDto> options = new ArrayList<OptionStateDto>();
        for (int i = 0; i < event.getOptionCount(); i++)
        {
            EventOption option = event.getOptions().get(i);
            options.add(new OptionStateDto(option.getName(), option.getSharesBought(), lmsr.getOptionValue(i)));
        }

        return new EventStateDto(event.getId(), event.getName(), event.getStatus().getDescription(),
                event.getAccountBalance(), event.getCollectedCommission(), lmsr.getB(), options,
                toTradeDtos(event.getTrades()), winnerName(event));
    }

    @Override
    public OrderBookStateDto getOrderBookState(int eventId)
    {
        Event event = requireEvent(eventId);
        if (!event.isOrderBook())
        {
            throw new InvalidRequestException("Event number " + eventId + " is not an order book event");
        }

        OrderBookMethod method = event.getOrderBookMethod();
        List<OptionBookDto> books = new ArrayList<OptionBookDto>();
        for (int i = 0; i < event.getOptionCount(); i++)
        {
            OrderBook book = method.getBook(i);
            books.add(new OptionBookDto(i, event.getOptions().get(i).getName(),
                    event.getOptions().get(i).getSharesBought(),
                    toOrderDtos(book.getBids()), toOrderDtos(book.getAsks()),
                    book.getLastPrice(), book.getBestBidPrice(), book.getBestAskPrice(),
                    book.getMidPrice(), book.getSpread()));
        }

        List<ParticipantDto> participants = new ArrayList<ParticipantDto>();
        for (String name : event.getParticipants())
        {
            User user = market.find(name);
            Participation participation = user.getParticipation(eventId);
            if (participation == null)
            {
                continue;
            }
            participants.add(new ParticipantDto(name, sharesOf(participation), netPaidOf(participation), participation.getCommissionPaid()));
        }

        return new OrderBookStateDto(event.getId(), event.getName(), event.getStatus().getDescription(),
                method.getD(), method.isMintAllowed(), method.getInitialInvestment(),
                event.getAccountBalance(), event.getCollectedCommission(), books, participants, winnerName(event));
    }

    @Override
    public List<UserDto> getAllUsers()
    {
        checkLoaded();
        List<UserDto> result = new ArrayList<UserDto>();
        for (User user : market.getUsers())
        {
            result.add(toUserDto(user));
        }
        return result;
    }

    @Override
    public UserDto getUser(String userName)
    {
        return toUserDto(requireUser(userName));
    }

    @Override
    public List<ParticipationDto> getParticipations(String userName)
    {
        User user = requireUser(userName);
        List<ParticipationDto> result = new ArrayList<ParticipationDto>();
        for (Participation participation : user.getParticipations())
        {
            result.add(toParticipationDto(participation));
        }
        return result;
    }

    @Override
    public ParticipationDto getParticipation(String userName, int eventId)
    {
        User user = requireUser(userName);
        Participation participation = user.getParticipation(eventId);
        if (participation == null)
        {
            return null;
        }
        return toParticipationDto(participation);
    }

    @Override
    public void openEvent(String userName, int eventId)
    {
        requireEvent(eventId).open(requireUser(userName));
    }

    @Override
    public TradeResultDto buyShares(String userName, int eventId, int optionIndex, int quantity)
    {
        Event event = requireEvent(eventId);
        if (event.isOrderBook())
        {
            throw new InvalidRequestException("Event number " + eventId + " is an order book event - use an order instead");
        }
        return toResultDto(event.buyLmsr(requireUser(userName), optionIndex, quantity, market), userName);
    }

    @Override
    public TradeResultDto submitOrder(String userName, int eventId, int optionIndex, String side, int quantity, double price)
    {
        Event event = requireEvent(eventId);
        if (!event.isOrderBook())
        {
            throw new InvalidRequestException("Event number " + eventId + " is an LMSR event - orders are not used in it");
        }
        Side parsedSide = Side.fromDescription(side);
        if (parsedSide == null)
        {
            throw new InvalidRequestException("'" + side + "' is not a valid order side");
        }
        return toResultDto(event.submitOrder(requireUser(userName), parsedSide, optionIndex, quantity, price, market), userName);
    }

    @Override
    public void closeEvent(String userName, int eventId, int optionIndex)
    {
        requireEvent(eventId).close(requireUser(userName), optionIndex, market);
    }


    @Override
    public int createLmsrEvent(String creatorUserName, String name, String description, int commissionPercent,
                               String commissionType, List<String> optionNames, int b)
    {
        checkLoaded();
        User creator = requireUser(creatorUserName);
        checkEventName(name);
        checkCommissionPercent(commissionPercent);
        CommissionType type = parseCommissionType(commissionType);
        List<EventOption> options = buildNewOptions(optionNames);
        if (b <= 0)
        {
            throw new InvalidRequestException("b must be a positive number");
        }

        int id = nextEventId();
        Event event = new Event(id, name.trim(), description == null ? "" : description.trim(),
                commissionPercent, type, options, new LmsrMethod(options, b));
        event.setMarketMakerName(creatorUserName);
        market.addEvent(event);
        creator.addMarketMakerEvent(id);
        return id;
    }

    @Override
    public int createOrderBookEvent(String creatorUserName, String name, String description, int commissionPercent,
                                    String commissionType, List<String> optionNames, boolean allowMint, int initial, int d)
    {
        checkLoaded();
        User creator = requireUser(creatorUserName);
        checkEventName(name);
        checkCommissionPercent(commissionPercent);
        CommissionType type = parseCommissionType(commissionType);
        List<EventOption> options = buildNewOptions(optionNames);
        if (d <= 0)
        {
            throw new InvalidRequestException("The base value (d) must be a positive number");
        }
        if (initial < 0)
        {
            throw new InvalidRequestException("The initial investment cannot be negative");
        }

        int id = nextEventId();
        Event event = new Event(id, name.trim(), description == null ? "" : description.trim(),
                commissionPercent, type, options, new OrderBookMethod(options, allowMint, initial, d));
        event.setMarketMakerName(creatorUserName);
        market.addEvent(event);
        creator.addMarketMakerEvent(id);
        return id;
    }

    private EventDto toEventDto(Event event)
    {
        List<String> optionNames = new ArrayList<String>();
        for (EventOption option : event.getOptions())
        {
            optionNames.add(option.getName());
        }
        return new EventDto(event.getId(), event.getName(), event.getDescription(), event.getCommissionPercent(),
                event.getCommissionType().getFileValue(), event.getCommissionType().getDescription(),
                event.getMethod().getTypeName(), event.getStatus().getDescription(), event.getAccountBalance(),
                event.getMarketMakerName(), optionNames);
    }

    private UserDto toUserDto(User user)
    {
        List<Integer> mmEvents = new ArrayList<Integer>(user.getMarketMakerEvents());
        List<Integer> participating = new ArrayList<Integer>();
        for (Participation participation : user.getParticipations())
        {
            participating.add(Integer.valueOf(participation.getEventId()));
        }
        return new UserDto(user.getName(), user.getBalance(), user.isBlocked(), mmEvents, participating);
    }

    private ParticipationDto toParticipationDto(Participation participation)
    {
        Event event = market.findById(participation.getEventId());
        List<String> optionNames = new ArrayList<String>();
        for (EventOption option : event.getOptions())
        {
            optionNames.add(option.getName());
        }
        return new ParticipationDto(event.getId(), event.getName(), event.getMethod().getTypeName(),
                event.getStatus().getDescription(), optionNames, sharesOf(participation), netPaidOf(participation),
                participation.getCommissionPaid(), toTradeDtos(participation.getTrades()), participation.isSettled(),
                participation.getPayout(), participation.getProfitOrLoss(), winnerName(event));
    }

    /**
     * A single fill creates one trade for the buyer and one for the seller, so the
     * totals are summed only over the trades of the user who made the request.
     */
    private TradeResultDto toResultDto(TradeResult result, String actingUserName)
    {
        double totalAmount = 0;
        double totalCommission = 0;
        for (Trade trade : result.getTrades())
        {
            if (trade.getUserName().equals(actingUserName))
            {
                totalAmount += trade.getAmount();
                totalCommission += trade.getCommission();
            }
        }
        return new TradeResultDto(result.getFilledQuantity(), result.getRestingQuantity(), result.isMinted(),
                totalAmount, totalCommission, toTradeDtos(result.getTrades()),
                new ArrayList<String>(result.getBlockedUsers()));
    }

    private List<TradeDto> toTradeDtos(List<Trade> trades)
    {
        List<TradeDto> result = new ArrayList<TradeDto>();
        // newest first
        for (int i = trades.size() - 1; i >= 0; i--)
        {
            Trade trade = trades.get(i);
            result.add(new TradeDto(trade.getUserName(), trade.getOptionName(), trade.getSide().getDescription(),
                    trade.getQuantity(), trade.getAmount(), trade.getPricePerShare(), trade.getCommission()));
        }
        return result;
    }

    private List<OrderDto> toOrderDtos(List<Order> orders)
    {
        List<OrderDto> result = new ArrayList<OrderDto>();
        for (Order order : orders)
        {
            result.add(new OrderDto(order.getId(), order.getUserName(), order.getSide().getDescription(),
                    order.getQuantity(), order.getPrice()));
        }
        return result;
    }

    private List<Integer> sharesOf(Participation participation)
    {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < participation.getOptionCount(); i++)
        {
            result.add(Integer.valueOf(participation.getShares(i)));
        }
        return result;
    }

    private List<Double> netPaidOf(Participation participation)
    {
        List<Double> result = new ArrayList<Double>();
        for (int i = 0; i < participation.getOptionCount(); i++)
        {
            result.add(Double.valueOf(participation.getNetPaid(i)));
        }
        return result;
    }

    private String winnerName(Event event)
    {
        if (event.getWinningOption() == null)
        {
            return null;
        }
        return event.getWinningOption().getName();
    }

    private void checkLoaded()
    {
        if (market == null)
        {
            throw new InvalidRequestException("No file is loaded in the system");
        }
    }

    private Event requireEvent(int eventId)
    {
        checkLoaded();
        Event event = market.findById(eventId);
        if (event == null)
        {
            throw new InvalidRequestException("There is no event number " + eventId + " in the system");
        }
        return event;
    }

    private User requireUser(String userName)
    {
        checkLoaded();
        User user = market.find(userName);
        if (user == null)
        {
            throw new InvalidRequestException("There is no user named '" + userName + "' in the system");
        }
        return user;
    }


    private int nextEventId()
    {
        int max = 0;
        for (Event event : market.getEvents())
        {
            max = Math.max(max, event.getId());
        }
        return max + 1;
    }

    private List<EventOption> buildNewOptions(List<String> optionNames)
    {
        if (optionNames == null || optionNames.size() != 2)
        {
            throw new InvalidRequestException("An event must have exactly 2 options");
        }
        List<EventOption> options = new ArrayList<>();
        for (String rawName : optionNames)
        {
            String name = rawName == null ? "" : rawName.trim();
            if (name.isEmpty())
            {
                throw new InvalidRequestException("Option names cannot be empty");
            }
            for (EventOption existing : options)
            {
                if (existing.getName().equalsIgnoreCase(name))
                {
                    throw new InvalidRequestException("The option '" + name + "' appears twice");
                }
            }
            options.add(new EventOption(name));
        }
        return options;
    }

    private CommissionType parseCommissionType(String value)
    {
        CommissionType type = CommissionType.fromFileValue(value);
        if (type == null)
        {
            throw new InvalidRequestException("Commission type must be 'on-close' or 'on-purchase'");
        }
        return type;
    }

    private void checkCommissionPercent(int percent)
    {
        if (percent < 0 || percent > 90)
        {
            throw new InvalidRequestException("Commission must be between 0 and 90");
        }
    }

    private void checkEventName(String name)
    {
        if (name == null || name.trim().isEmpty())
        {
            throw new InvalidRequestException("The event needs a name");
        }
    }
}
