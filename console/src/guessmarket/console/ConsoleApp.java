package guessmarket.console;

import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.dto.EventDto;
import guessmarket.engine.dto.EventStateDto;
import guessmarket.engine.dto.PurchaseResultDto;
import guessmarket.engine.exception.InvalidFileException;
import guessmarket.engine.exception.InvalidRequestException;
import guessmarket.engine.exception.StateFileException;

import java.util.List;

/*
  The menu loop of the application. It asks the engine to do the work
  and hands the answers over to the printer
 */
public class ConsoleApp 
{
    private final GuessMarketEngine engine;
    private final InputReader inputReader;
    private final Printer printer;

    public ConsoleApp(GuessMarketEngine engine)
    {
        this.engine = engine;
        inputReader = new InputReader();
        printer = new Printer();
    }

    public void run() 
    {
        printer.printWelcome();

        MenuOption choice = null;
        while (choice != MenuOption.EXIT) 
        {
            printer.printMenu();
            int number = inputReader.readNumberInRange("Please choose an option (1 - " + MenuOption.values().length + "): ",
                    1, MenuOption.values().length);
            choice = MenuOption.values()[number - 1];
            try
            {
                handle(choice);
            }
            catch (InvalidRequestException e)
            {
                printer.printMessage("The action was not done - " + e.getMessage());
            }
        }
    }

    private void handle(MenuOption option)
    {
        switch (option)
            {
            case LOAD_FILE:
                loadFile();
                break;
            case SHOW_EVENTS:
                showEvents();
                break;
            case EVENT_STATE:
                showEventState();
                break;
            case PARTICIPATE:
                participate();
                break;
            case CLOSE_EVENT:
                closeEvent();
                break;
            case SAVE_STATE:
                saveState();
                break;
            case LOAD_STATE:
                loadState();
                break;
            case EXIT:
                printer.printMessage("Thank you and goodbye !");
                break;
        }
    }

    private void loadFile() 
    {
        String path = inputReader.readPath("Please enter the full path of the xml file: ");
        try {
            engine.loadEventsFile(path);
            printer.printMessage("The file is valid and was loaded. The system now holds "
                    + engine.getAllEvents().size() + " events.");
        } catch (InvalidFileException e)
            {
            printer.printMessage("The file was not loaded. " + e.getMessage());
            if (engine.isFileLoaded()) 
            {
                printer.printMessage("The events that were loaded before are still in the system");
            }
        }
    }

    private void showEvents()
    {
        if (!isFileLoaded()) 
        {
            return;
        }
        printer.printEvents(engine.getAllEvents());
    }

    private void showEventState()
    {
        if (!isFileLoaded())
        {
            return;
        }

        List<EventDto> events = engine.getAllEvents();
        printer.printEvents(events);
        int eventId = chooseEvent(events, "Please choose the event you want to see");
        printer.printEventState(engine.getEventState(eventId));
    }

    private void participate() 
    {
        if (!isFileLoaded())
        {
            return;
        }

        List<EventDto> events = engine.getActiveEvents();
        if (events.isEmpty())
        {
            printer.printMessage("There are no active events to participate in right now");
            return;
        }

        printer.printEvents(events);
        int eventId = chooseEvent(events, "Please choose the event you want to participate in");

        EventStateDto state = engine.getEventState(eventId);
        printer.printCurrentState(state.getOptions());

        int optionNumber = chooseOption(state, "Please choose the option you believe in");
        int quantity = inputReader.readPositiveNumber("How many shares of this option would you like to buy ? ");

        PurchaseResultDto result = engine.buyShares(eventId, optionNumber - 1, quantity);
        printer.printPurchaseResult(result);
        printer.printEventState(engine.getEventState(eventId));
    }

    private void closeEvent()
    {
        if (!isFileLoaded())
        {
            return;
        }

        List<EventDto> events = engine.getActiveEvents();
        if (events.isEmpty())
        {
            printer.printMessage("There are no active events to close right now");
            return;
        }

        printer.printEvents(events);
        int eventId = chooseEvent(events, "Please choose the event you want to close");

        EventStateDto state = engine.getEventState(eventId);
        printer.printEventState(state);

        int optionNumber = chooseOption(state, "Please choose the option that the event ended with");
        engine.closeEvent(eventId, optionNumber - 1);

        printer.printMessage("The event was closed");
        printer.printEventState(engine.getEventState(eventId));
    }

    private void saveState() 
    {
        if (!isFileLoaded())
        {
            return;
        }

        String path = inputReader.readPath("Please enter the full path and file name to save into, without an extension: ");
        try 
            {
            engine.saveState(path);
            printer.printMessage("The state of the system was saved");
        } catch (StateFileException e) 
            {
            printer.printMessage("The state was not saved - " + e.getMessage());
        }
    }

    private void loadState()
    {
        String path = inputReader.readPath("Please enter the full path and file name of the saved state, without an extension: ");
        try
            {
            engine.loadState(path);
            printer.printMessage("The saved state was loaded. The system now holds "
                    + engine.getAllEvents().size() + " events.");
        } catch (StateFileException e) 
            {
            printer.printMessage("The state was not loaded - " + e.getMessage());
            if (engine.isFileLoaded()) 
            {
                printer.printMessage("The events that were loaded before are still in the system");
            }
        }
    }

    private boolean isFileLoaded() 
    {
        if (!engine.isFileLoaded()) 
        {
            printer.printMessage("There is no events file in the system yet. Please load a file first (option 1)");
            return false;
        }
        return true;
    }

    private int chooseEvent(List<EventDto> events, String message) 
    {
        int choice = inputReader.readNumberInRange(message + " (1-" + events.size() + "): ", 1, events.size());
        return events.get(choice - 1).getId();
    }

    private int chooseOption(EventStateDto state, String message)
    {
        int size = state.getOptions().size();
        return inputReader.readNumberInRange(message + " (1-" + size + "): ", 1, size);
    }
}
