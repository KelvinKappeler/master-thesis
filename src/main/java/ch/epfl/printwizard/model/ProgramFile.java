package ch.epfl.printwizard.model;

import ch.epfl.printwizard.utils.Preconditions;

import java.util.List;

/**
 * Represents the entire program file structure for program.json, including sources, classes, and methods.
 * @param sources File sources
 * @param classes Classes
 * @param interfaces Interfaces
 * @param records Records
 * @param enums Enums
 * @param methods Methods
 */
public record ProgramFile(
    List<Source> sources,
    List<ClassInfo> classes,
    List<InterfaceInfo> interfaces,
    List<RecordInfo> records,
    List<EnumInfo> enums,
    List<MethodInfo> methods
) {
    public ProgramFile {
        Preconditions.requireNonNull(sources, "sources cannot be null");
        Preconditions.requireNonNull(classes, "classes cannot be null");
        Preconditions.requireNonNull(interfaces, "interfaces cannot be null");
        Preconditions.requireNonNull(records, "records cannot be null");
        Preconditions.requireNonNull(enums, "enums cannot be null");
        Preconditions.requireNonNull(methods, "methods cannot be null");
    }
}
