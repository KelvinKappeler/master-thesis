package ch.epfl.printwizard.model;

import ch.epfl.printwizard.utils.Preconditions;

/**
 * Represents a parameter for a method or constructor in the program.json file.
 * @param index the index of the parameter in the parameter list
 * @param name the name of the parameter
 * @param typeId the type ID of the parameter
 */
public record ParameterInfo(int index, String name, String typeId) {

    public ParameterInfo {
        Preconditions.require(index >= 0, "Index must be non-negative");
        Preconditions.requireNonNull(name, "name cannot be null");
        Preconditions.requireNonNull(typeId, "typeId cannot be null");
        Preconditions.require(!typeId.isEmpty(), "typeId cannot be empty");
    }
}
