package ch.epfl.printwizard.shared.model.program.structures;

import ch.epfl.printwizard.shared.model.program.structures.expr.ExprNode;
import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents a for-loop structure in the AST (used in program.json).
 */
public final class ForNode extends StructureNode {
    
    private final ExprNode initialization;
    private final ExprNode condition;
    private final ExprNode update;
    private final StructureNode body;
    
    public ForNode(String structureId, int start, int end,
            ExprNode initialization, ExprNode condition, ExprNode update, StructureNode body) {
        super(structureId, StructureKind.FOR, start, end);

        Preconditions.requireNonNull(initialization, "initialization cannot be null");
        Preconditions.requireNonNull(condition, "condition cannot be null");
        Preconditions.requireNonNull(update, "update cannot be null");
        Preconditions.requireNonNull(body, "body cannot be null");
        
        this.initialization = initialization;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    /**
     * Gets the initialization expression of the for-loop.
     * @return the initialization expression
     */
    public ExprNode getInitialization() {
        return initialization;
    }

    /**
     * Gets the condition expression of the for-loop.
     * @return the condition expression
     */
    public ExprNode getCondition() {
        return condition;
    }

    /**
     * Gets the update expression of the for-loop.
     * @return the update expression
     */
    public ExprNode getUpdate() {
        return update;
    }

    /**
     * Gets the body of the for-loop.
     * @return the body structure
     */
    public StructureNode getBody() {
        return body;
    }
}
