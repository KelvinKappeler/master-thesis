package ch.epfl.printwizard.shared.model.trace;

import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents an argument with a name and a value.
 * @param name the name of the argument
 * @param value the value of the argument
 */
public record Arg(
    String name,
    Object value
) {

    public Arg {
        Preconditions.requireNonNull(name, "name is null");
        Preconditions.require(!name.isEmpty(), "name is empty");
    }

}
