package ch.epfl.printwizard.model;

/**
 * Represents a source for the program.json file. A source is a file containing code.
 * @param sourceId ID of the source
 * @param path Path of the source
 * @param language Programming language of the source
 * @param lines Number of lines of code in the source
 */
public record Source(
    String sourceId,
    String path,
    String language,
    int lines
) { }
