package ch.epfl.printwizard.model;

/**
 * Represents a local variable in the program.json file.
 */
public record LocalVar(
    String id, String name, String typeId
) { }
