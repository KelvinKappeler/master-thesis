package ch.epfl.printwizard.plugin.model.state;

import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * Represents the state of a field for a versioned object.
 * @param type The type of the field.
 * @param value The value of the field.
 * @param objectId The identifier of the object that contains the field.
 */
public record FieldState(
    String type,
    Object value,
    String objectId
) {

    public FieldState {
        Preconditions.requireNonNull(type, "type is null");
        Preconditions.require(!type.isEmpty(), "type is empty");
    }

}
