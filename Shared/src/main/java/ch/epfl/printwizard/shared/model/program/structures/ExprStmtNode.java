package ch.epfl.printwizard.shared.model.program.structures;

import ch.epfl.printwizard.shared.model.program.ProgramPosition;
import ch.epfl.printwizard.shared.model.program.structures.expr.ExprNode;
import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents an expression statement in the AST (used in program.json).
 */
public final class ExprStmtNode extends StructureNode {
    
    private final ExprNode expr;

    public ExprStmtNode(String structureId, ProgramPosition start, ProgramPosition end, ExprNode expr) {
        super(structureId, StructureKind.EXPR_STMT, null, start, end);

        Preconditions.requireNonNull(expr, "expr cannot be null");

        this.expr = expr;
    }

    /**
     * Gets the expression of the expression statement.
     * @return the expression
     */
    public ExprNode getExpr() {
        return expr;
    }
}
