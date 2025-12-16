package ch.epfl.printwizard.shared.model.program.structures;

import ch.epfl.printwizard.shared.model.program.ProgramPosition;
import ch.epfl.printwizard.shared.model.program.structures.expr.ExprNode;
import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents a while-loop structure in the AST (used in program.json).
 */
public final class WhileNode extends StructureNode {
    
    private final ExprNode condition;
    private final StructureNode body;

    public WhileNode(String structureId, String code, ProgramPosition start, ProgramPosition end, ExprNode condition, StructureNode body) {
        super(structureId, StructureKind.WHILE, code, start, end);
        
        Preconditions.requireNonNull(condition, "condition cannot be null");
        Preconditions.requireNonNull(body, "body cannot be null");
        
        this.condition = condition;
        this.body = body;
    }

    /**
     * Gets the condition expression of the for-loop.
     * @return the condition expression
     */
    public ExprNode getCondition() {
        return condition;
    }

    /**
     * Gets the body of the for-loop.
     * @return the body structure
     */
    public StructureNode getBody() {
        return body;
    }
}
