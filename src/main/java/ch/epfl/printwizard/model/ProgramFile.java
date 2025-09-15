package ch.epfl.printwizard.model;

import java.util.List;

/**
 * Represents the entire program file structure for program.json, including sources, classes, and methods.
 * @param sources File sources
 * @param classes Classes
 * @param methods Methods
 */
public record ProgramFile(
    List<Source> sources,
    List<ClassInfo> classes,
    List<MethodInfo> methods
) { }
