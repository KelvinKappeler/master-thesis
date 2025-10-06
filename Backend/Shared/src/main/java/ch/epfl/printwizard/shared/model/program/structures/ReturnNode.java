package ch.epfl.printwizard.shared.model.program.structures;

import ch.epfl.printwizard.shared.model.program.ProgramPosition;
import ch.epfl.printwizard.shared.model.program.structures.expr.ExprNode;

/**
 * Represents a return statement structure in the AST (used in program.json).
 */
public class ReturnNode extends StructureNode {

    private final ExprNode value;

    public ReturnNode(String structureId, String code, ProgramPosition start, ProgramPosition end, ExprNode value) {
        super(structureId, StructureKind.RETURN, code, start, end);

        this.value = value;
    }

    /**
     * Gets the value returned by the return statement.
     * @return the return value
     */
    public ExprNode getValue() {
        return value;
    }
}
