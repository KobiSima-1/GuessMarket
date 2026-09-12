package guessmarket.engine;

import guessmarket.engine.dto.EventDto;
import guessmarket.engine.dto.EventStateDto;
import guessmarket.engine.dto.OrderBookStateDto;
import guessmarket.engine.dto.ParticipationDto;
import guessmarket.engine.dto.TradeResultDto;
import guessmarket.engine.dto.UserDto;
import guessmarket.engine.exception.InvalidFileException;

import java.util.List;

/**
 * The whole api of the system. The ui talks only through this interface and
 * only with dto objects - it never sees the engine model itself.
 */
public interface GuessMarketEngine
{
    void loadEventsFile(String path) throws InvalidFileException;

    boolean isFileLoaded();

    String getLoadedFilePath();

    List<EventDto> getAllEvents();

    EventDto getEvent(int eventId);

    EventStateDto getLmsrState(int eventId);

    OrderBookStateDto getOrderBookState(int eventId);

    List<UserDto> getAllUsers();

    UserDto getUser(String userName);

    List<ParticipationDto> getParticipations(String userName);

    ParticipationDto getParticipation(String userName, int eventId);

    void openEvent(String userName, int eventId);

    TradeResultDto buyShares(String userName, int eventId, int optionIndex, int quantity);

    TradeResultDto submitOrder(String userName, int eventId, int optionIndex, String side, int quantity, double price);

    void closeEvent(String userName, int eventId, int optionIndex);
}
