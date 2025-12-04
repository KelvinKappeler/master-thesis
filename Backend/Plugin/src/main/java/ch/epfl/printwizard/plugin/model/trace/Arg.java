package ch.epfl.printwizard.plugin.model.trace;

import ch.epfl.printwizard.plugin.model.trace.events.EventValue;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * Represents an argument with a name, a value, and a type.
 * @param name the name of the argument
 * @param value the value of the argument
 */
public record Arg(
    String name,
    EventValue value,
    String type
) {

    public Arg {
        Preconditions.requireNonNull(name, "name is null");
        Preconditions.requireNonNull(value, "value is null");
        Preconditions.requireNonNull(type, "type is null");
        Preconditions.require(!name.isEmpty(), "name is empty");
        Preconditions.require(!type.isEmpty(), "type is empty");
    }

    /**
     * Creates a new argument.
     * @param name the name of the argument
     * @param value the value of the argument
     * @param type the type of the argument
     * @return the new argument
     */
    public static <T> Arg of(String name, T value, String type) {
        EventValue eventValue = EventValue.of(value);
        
        return new Arg(name, eventValue, type);
    }

}
