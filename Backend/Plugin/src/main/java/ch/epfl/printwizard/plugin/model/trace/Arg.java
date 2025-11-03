package ch.epfl.printwizard.plugin.model.trace;

import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * Represents an argument with a name, a value, and a type.
 * @param name the name of the argument
 * @param value the value of the argument
 * @param type the type of the argument
 */
public record Arg(
    String name,
    Object value,
    String type
) {

    public Arg {
        Preconditions.requireNonNull(name, "name is null");
        Preconditions.requireNonNull(type, "type is null");
        Preconditions.require(!name.isEmpty(), "name is empty");
        Preconditions.require(!type.isEmpty(), "type is empty");
    }

}
