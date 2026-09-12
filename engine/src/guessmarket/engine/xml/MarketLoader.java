package guessmarket.engine.xml;

import guessmarket.engine.exception.InvalidFileException;
import guessmarket.engine.method.LmsrMethod;
import guessmarket.engine.method.OrderBookMethod;
import guessmarket.engine.method.TradingMethod;
import guessmarket.engine.model.CommissionType;
import guessmarket.engine.model.Event;
import guessmarket.engine.model.EventOption;
import guessmarket.engine.model.Market;
import guessmarket.engine.model.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads an exercise 2 xml file into a Market object and checks that its content
 * makes sense. Nothing is loaded into the system unless the whole file is valid.
 */
public class MarketLoader
{
    private static final String XML_SUFFIX = ".xml";
    private static final String ROOT_TAG = "Guess-Market";
    private static final String EVENTS_TAG = "GM-events";
    private static final String EVENT_TAG = "GM-event";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String ID_TAG = "id";
    private static final String ID_ATTRIBUTE = "id";
    private static final String DESCRIPTION_TAG = "description";
    private static final String COMMISSION_TAG = "commission";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String OPTIONS_TAG = "GM-options";
    private static final String OPTION_TAG = "GM-option";
    private static final String METHOD_TAG = "GM-method";
    private static final String LMSR_TAG = "GM-LMSR";
    private static final String B_TAG = "b";
    private static final String ORDER_BOOK_TAG = "GM-order-book";
    private static final String ALLOW_MINT_ATTRIBUTE = "allow-mint";
    private static final String INITIAL_ATTRIBUTE = "initial";
    private static final String D_ATTRIBUTE = "d";
    private static final String USERS_TAG = "GM-users";
    private static final String USER_TAG = "GM-user";
    private static final String INITIAL_CASH_TAG = "initial-cash";
    private static final String MARKET_MAKER_TAG = "GM-market-maker";
    private static final String MM_EVENT_TAG = "event";

    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int OPTIONS_PER_EVENT = 2;

    public Market load(String path) throws InvalidFileException
    {
        File file = checkFile(path);
        Element root = readRoot(file);
        Market market = new Market();
        readEvents(root, market);
        readUsers(root, market);
        checkMarketMakers(market);
        return market;
    }

    private File checkFile(String path) throws InvalidFileException
    {
        if (path == null || path.trim().isEmpty())
        {
            throw new InvalidFileException("No path was entered");
        }
        if (!path.toLowerCase().endsWith(XML_SUFFIX))
        {
            throw new InvalidFileException("The file '" + path + "' is not an xml file - the path must end with " + XML_SUFFIX);
        }

        File file = new File(path);
        if (!file.exists())
        {
            throw new InvalidFileException("There is no file at the path '" + path + "'");
        }
        if (!file.isFile())
        {
            throw new InvalidFileException("The path '" + path + "' points to a folder and not to a file");
        }
        return file;
    }

    private Element readRoot(File file) throws InvalidFileException
    {
        Document document;
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(file);
        }
        catch (ParserConfigurationException | SAXException | IOException e)
        {
            throw new InvalidFileException("The file could not be read as an xml document: " + e.getMessage());
        }

        Element root = document.getDocumentElement();
        if (root == null || !ROOT_TAG.equals(root.getNodeName()))
        {
            throw new InvalidFileException("The main element of the file should be '" + ROOT_TAG + "'");
        }
        return root;
    }

    private void readEvents(Element root, Market market) throws InvalidFileException
    {
        Element eventsElement = findChild(root, EVENTS_TAG);
        if (eventsElement == null)
        {
            throw new InvalidFileException("The element '" + EVENTS_TAG + "' is missing from the file");
        }

        List<Element> eventElements = findChildren(eventsElement, EVENT_TAG);
        if (eventElements.isEmpty())
        {
            throw new InvalidFileException("The file does not contain any event");
        }

        for (Element eventElement : eventElements)
        {
            Event event = buildEvent(eventElement);
            if (market.containsId(event.getId()))
            {
                throw new InvalidFileException("Event number " + event.getId() + " appears more than once in the file. Every event must have its own unique number");
            }
            market.addEvent(event);
        }
    }

    private Event buildEvent(Element eventElement) throws InvalidFileException
    {
        String name = readAttribute(eventElement, NAME_ATTRIBUTE, "an event");
        int id = readInt(readChildText(eventElement, ID_TAG, "event '" + name + "'"), "The id of the event '" + name + "'");
        String description = readChildText(eventElement, DESCRIPTION_TAG, "event '" + name + "'");

        Element commissionElement = findChild(eventElement, COMMISSION_TAG);
        if (commissionElement == null)
        {
            throw new InvalidFileException("The element '" + COMMISSION_TAG + "' is missing from the event '" + name + "'");
        }
        int commissionPercent = readInt(text(commissionElement), "The commission of the event '" + name + "'");
        if (commissionPercent < MIN_COMMISSION || commissionPercent > MAX_COMMISSION)
        {
            throw new InvalidFileException("The commission of the event '" + name + "' is " + commissionPercent + "% - it has to be between " + MIN_COMMISSION + "% and " + MAX_COMMISSION + "%");
        }

        String commissionTypeValue = readAttribute(commissionElement, TYPE_ATTRIBUTE, "the commission of the event '" + name + "'");
        CommissionType commissionType = CommissionType.fromFileValue(commissionTypeValue);
        if (commissionType == null)
        {
            throw new InvalidFileException("The commission type '" + commissionTypeValue + "' of the event '" + name + "' is not one of: on-close, on-purchase");
        }

        List<EventOption> options = buildOptions(eventElement, name);
        TradingMethod method = buildMethod(eventElement, name, options);
        return new Event(id, name, description, commissionPercent, commissionType, options, method);
    }

    private List<EventOption> buildOptions(Element eventElement, String eventName) throws InvalidFileException
    {
        Element optionsElement = findChild(eventElement, OPTIONS_TAG);
        if (optionsElement == null)
        {
            throw new InvalidFileException("The element '" + OPTIONS_TAG + "' is missing from the event '" + eventName + "'");
        }

        List<Element> optionElements = findChildren(optionsElement, OPTION_TAG);
        if (optionElements.size() != OPTIONS_PER_EVENT)
        {
            throw new InvalidFileException("The event '" + eventName + "' has " + optionElements.size() + " options - every event must have exactly " + OPTIONS_PER_EVENT);
        }

        List<EventOption> options = new ArrayList<EventOption>();
        for (Element optionElement : optionElements)
        {
            String optionName = text(optionElement);
            if (optionName.isEmpty())
            {
                throw new InvalidFileException("One of the options of the event '" + eventName + "' has no name");
            }
            for (EventOption existing : options)
            {
                if (existing.getName().equals(optionName))
                {
                    throw new InvalidFileException("The option '" + optionName + "' appears twice in the event '" + eventName + "'");
                }
            }
            options.add(new EventOption(optionName));
        }
        return options;
    }

    private TradingMethod buildMethod(Element eventElement, String eventName, List<EventOption> options) throws InvalidFileException
    {
        Element methodElement = findChild(eventElement, METHOD_TAG);
        if (methodElement == null)
        {
            throw new InvalidFileException("The element '" + METHOD_TAG + "' is missing from the event '" + eventName + "'");
        }

        Element lmsrElement = findChild(methodElement, LMSR_TAG);
        Element orderBookElement = findChild(methodElement, ORDER_BOOK_TAG);

        if (lmsrElement != null && orderBookElement != null)
        {
            throw new InvalidFileException("The event '" + eventName + "' declares both " + LMSR_TAG + " and " + ORDER_BOOK_TAG + " - it must declare exactly one trading method");
        }
        if (lmsrElement != null)
        {
            int b = readInt(readChildText(lmsrElement, B_TAG, "the LMSR method of the event '" + eventName + "'"), "The value of b in the event '" + eventName + "'");
            if (b <= 0)
            {
                throw new InvalidFileException("The value of b in the event '" + eventName + "' is " + b + " - it has to be a positive number");
            }
            return new LmsrMethod(options, b);
        }
        if (orderBookElement != null)
        {
            int d = readInt(readAttribute(orderBookElement, D_ATTRIBUTE, "the order book of the event '" + eventName + "'"), "The base value (d) of the event '" + eventName + "'");
            if (d <= 0)
            {
                throw new InvalidFileException("The base value (d) of the event '" + eventName + "' is " + d + " - it has to be a positive number");
            }
            int initial = readInt(readAttribute(orderBookElement, INITIAL_ATTRIBUTE, "the order book of the event '" + eventName + "'"), "The initial investment of the event '" + eventName + "'");
            if (initial < 0)
            {
                throw new InvalidFileException("The initial investment of the event '" + eventName + "' is " + initial + " - it cannot be negative");
            }
            String mintValue = readAttribute(orderBookElement, ALLOW_MINT_ATTRIBUTE, "the order book of the event '" + eventName + "'");
            if (!"true".equalsIgnoreCase(mintValue) && !"false".equalsIgnoreCase(mintValue))
            {
                throw new InvalidFileException("The value of " + ALLOW_MINT_ATTRIBUTE + " in the event '" + eventName + "' is '" + mintValue + "' - it has to be true or false");
            }
            return new OrderBookMethod(options, Boolean.parseBoolean(mintValue.toLowerCase()), initial, d);
        }
        throw new InvalidFileException("The event '" + eventName + "' does not declare a trading method (" + LMSR_TAG + " or " + ORDER_BOOK_TAG + ")");
    }

    private void readUsers(Element root, Market market) throws InvalidFileException
    {
        Element usersElement = findChild(root, USERS_TAG);
        if (usersElement == null)
        {
            throw new InvalidFileException("The element '" + USERS_TAG + "' is missing from the file");
        }

        List<Element> userElements = findChildren(usersElement, USER_TAG);
        if (userElements.isEmpty())
        {
            throw new InvalidFileException("The file does not contain any user");
        }

        for (Element userElement : userElements)
        {
            String name = readAttribute(userElement, NAME_ATTRIBUTE, "a user");
            if (market.containsUser(name))
            {
                throw new InvalidFileException("The user name '" + name + "' appears more than once in the file. Every user must have a unique name");
            }

            int initialCash = readInt(readChildText(userElement, INITIAL_CASH_TAG, "user '" + name + "'"), "The initial cash of the user '" + name + "'");
            if (initialCash <= 0)
            {
                throw new InvalidFileException("The initial cash of the user '" + name + "' is " + initialCash + " - every user has to start with more than 0");
            }

            User user = new User(name, initialCash);
            readMarketMakerEvents(userElement, user, market);
            market.addUser(user);
        }
    }

    private void readMarketMakerEvents(Element userElement, User user, Market market) throws InvalidFileException
    {
        Element marketMakerElement = findChild(userElement, MARKET_MAKER_TAG);
        if (marketMakerElement == null)
        {
            return;
        }

        for (Element eventElement : findChildren(marketMakerElement, MM_EVENT_TAG))
        {
            int eventId = readInt(readAttribute(eventElement, ID_ATTRIBUTE, "a market maker entry of the user '" + user.getName() + "'"), "The event id of the market maker entry of the user '" + user.getName() + "'");
            if (!market.containsId(eventId))
            {
                throw new InvalidFileException("The user '" + user.getName() + "' is defined as the market maker of event number " + eventId + ", but there is no such event in the file");
            }
            if (user.isMarketMakerOf(eventId))
            {
                throw new InvalidFileException("The user '" + user.getName() + "' is defined twice as the market maker of event number " + eventId);
            }
            user.addMarketMakerEvent(eventId);
        }
    }

    private void checkMarketMakers(Market market) throws InvalidFileException
    {
        Map<Integer, String> ownerByEvent = new HashMap<Integer, String>();
        for (User user : market.getUsers())
        {
            for (Integer eventId : user.getMarketMakerEvents())
            {
                String existing = ownerByEvent.get(eventId);
                if (existing != null)
                {
                    throw new InvalidFileException("Event number " + eventId + " has more than one market maker: '" + existing + "' and '" + user.getName() + "'. Every event must have exactly one");
                }
                ownerByEvent.put(eventId, user.getName());
            }
        }

        for (Event event : market.getEvents())
        {
            String ownerName = ownerByEvent.get(Integer.valueOf(event.getId()));
            if (ownerName == null)
            {
                throw new InvalidFileException("Event number " + event.getId() + " ('" + event.getName() + "') has no market maker. Every event must have exactly one");
            }
            event.setMarketMakerName(ownerName);
        }
    }

    private String readAttribute(Element element, String attribute, String owner) throws InvalidFileException
    {
        if (!element.hasAttribute(attribute))
        {
            throw new InvalidFileException("The attribute '" + attribute + "' is missing from " + owner);
        }
        String value = element.getAttribute(attribute).trim();
        if (value.isEmpty())
        {
            throw new InvalidFileException("The attribute '" + attribute + "' of " + owner + " is empty");
        }
        return value;
    }

    private String readChildText(Element parent, String tag, String owner) throws InvalidFileException
    {
        Element child = findChild(parent, tag);
        if (child == null)
        {
            throw new InvalidFileException("The element '" + tag + "' is missing from " + owner);
        }
        String value = text(child);
        if (value.isEmpty())
        {
            throw new InvalidFileException("The element '" + tag + "' of " + owner + " is empty");
        }
        return value;
    }

    private int readInt(String value, String owner) throws InvalidFileException
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            throw new InvalidFileException(owner + " is '" + value + "' - it has to be a whole number");
        }
    }

    private String text(Element element)
    {
        String content = element.getTextContent();
        if (content == null)
        {
            return "";
        }
        return content.trim();
    }

    private Element findChild(Element parent, String tag)
    {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tag.equals(node.getNodeName()))
            {
                return (Element) node;
            }
        }
        return null;
    }

    private List<Element> findChildren(Element parent, String tag)
    {
        List<Element> result = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tag.equals(node.getNodeName()))
            {
                result.add((Element) node);
            }
        }
        return result;
    }
}
