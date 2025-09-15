package ch.epfl.printwizard.model.structures;

import ch.epfl.printwizard.model.LocalVar;
import ch.epfl.printwizard.utils.Preconditions;

import java.util.List;

/**
 * Represents a local variable declaration structure node in the AST.
 */
public final class LocalVarDeclNode extends StructureNode {
    
    private final List<LocalVar> localVars;

    public LocalVarDeclNode(String structureId, int startLine, int endLine, List<LocalVar> localVars) {
        super(structureId, StructureKind.LOCAL_VAR_DECL, startLine, endLine);

        Preconditions.requireNonNull(localVars, "localVars cannot be null");
        Preconditions.require(!localVars.isEmpty(), "localVars cannot be empty");
        
        this.localVars = localVars;
    }

    /**
     * Gets the list of local variables declared.
     * @return the list of local variables
     */
    public List<LocalVar> getLocalVars() {
        return localVars;
    }
}
