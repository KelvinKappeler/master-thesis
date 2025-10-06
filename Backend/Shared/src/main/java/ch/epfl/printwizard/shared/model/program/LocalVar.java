package ch.epfl.printwizard.shared.model.program;

import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents a local variable in a method.
 * @param index the index of the local variable
 * @param name the name of the local variable
 * @param typeId the type ID of the local variable
 */
public record LocalVar(int index, String name, String typeId) {
    
    public LocalVar {
        Preconditions.require(index >= 0, "slot must be >= 0");
        Preconditions.requireNonNull(name, "name cannot be null");
        Preconditions.requireNonNull(typeId, "typeId cannot be null");
        Preconditions.require(!typeId.isEmpty(), "typeId cannot be empty");
    }
}
