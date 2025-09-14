package ch.epfl.printwizard.model;

/**
 * Represents a method in the program.json file.
 * @param methodId ID of the method
 * @param classId ID of the class where the method is defined
 * @param name Name of the method
 * @param returnType Return type of the method
 * @param startLine Start line of the method
 * @param endLine End line of the method
 *                // Structures List of structures used in the method
 *                // Locals List of local variables in the method
 */
public record MethodInfo(
    String methodId,
    String classId,
    String name,
    String returnType,
    String startLine,
    String endLine
    // Structures
    // Locals
) { }
