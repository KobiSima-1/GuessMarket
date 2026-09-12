package guessmarket.engine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Market implements Serializable, UserLookup
{
    private static final long serialVersionUID = 1L;

    private final List<Event> events;
    private final Map<String, User> users;

    public Market()
    {
        events = new ArrayList<Event>();
        users = new LinkedHashMap<String, User>();
    }

    public void addEvent(Event event)
    {
        events.add(event);
    }

    public List<Event> getEvents()
    {
        return events;
    }

    public Event findById(int id)
    {
        for (Event event : events)
        {
            if (event.getId() == id)
            {
                return event;
            }
        }
        return null;
    }

    public boolean containsId(int id)
    {
        return findById(id) != null;
    }

    public void addUser(User user)
    {
        users.put(user.getName(), user);
    }

    public List<User> getUsers()
    {
        return new ArrayList<User>(users.values());
    }

    public boolean containsUser(String name)
    {
        return users.containsKey(name);
    }

    @Override
    public User find(String name)
    {
        return users.get(name);
    }
}
