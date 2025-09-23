package ch.epfl.printwizard.shared.model.program;

import ch.epfl.printwizard.shared.model.program.structures.StructureNode;
import ch.epfl.printwizard.shared.utils.Preconditions;

import java.util.List;

/**
 * Represents a method in the program.json file.
 * @param methodId ID of the method
 * @param classId ID of the class where the method is defined
 * @param name Name of the method
 * @param returnType Return type of the method
 * @param startLine Start line of the method
 * @param endLine End line of the method
 * @param parameters List of parameters of the method
 * @param structure Structure of the method (e.g., loops, conditionals)
 * @param localVars List of local variables in the method
 */
public record MethodInfo(
    String methodId,
    String classId,
    String name,
    String returnType,
    int startLine,
    int endLine,
    List<ParameterInfo> parameters,
    StructureNode structure,
    List<LocalVar> localVars
) {
    public MethodInfo {
        Preconditions.requireNonNull(methodId, "methodId is null");
        Preconditions.requireNonNull(classId, "classId is null");
        Preconditions.requireNonNull(name, "name is null");
        Preconditions.requireNonNull(returnType, "returnType is null");
        Preconditions.require(!methodId.isEmpty(), "methodId is empty");
        Preconditions.require(!classId.isEmpty(), "classId is empty");
        Preconditions.require(startLine >= 0, "startLine is negative");
        Preconditions.require(endLine >= startLine, "endLine is less than startLine");
        Preconditions.requireNonNull(parameters, "parameters is null");
        Preconditions.requireNonNull(localVars, "localVars is null");
    }
}
