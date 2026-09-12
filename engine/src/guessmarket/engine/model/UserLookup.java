package guessmarket.engine.model;

import java.io.Serializable;

/** Lets an event reach the users that take part in it, without holding them itself. */
public interface UserLookup extends Serializable
{
    User find(String name);
}
