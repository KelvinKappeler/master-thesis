package ch.epfl.printwizard.model.structures;

import ch.epfl.printwizard.model.structures.expr.ExprNode;
import ch.epfl.printwizard.utils.Preconditions;

/**
 * Represents an expression statement in the AST (used in program.json).
 */
public final class ExprStmtNode extends StructureNode {
    
    private final ExprNode expr;

    public ExprStmtNode(String structureId, int startLine, int endLine, ExprNode expr) {
        super(structureId, StructureKind.EXPR_STMT, startLine, endLine);

        Preconditions.RequireNonNull(expr, "expr cannot be null");

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
