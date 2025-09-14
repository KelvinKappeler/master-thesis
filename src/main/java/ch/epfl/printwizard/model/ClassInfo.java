package ch.epfl.printwizard.model;

/**
 * Represents a class in the program.json file.
 * @param classId ID of the class
 * @param name Name of the class
 * @param packageName Package name of the class
 * @param sourceId ID of the source file where the class is defined
 */
public record ClassInfo(
    String classId,
    String name,
    String packageName,
    String sourceId
) { }
