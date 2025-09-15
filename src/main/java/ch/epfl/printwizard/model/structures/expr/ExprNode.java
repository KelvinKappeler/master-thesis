package ch.epfl.printwizard.model.structures.expr;

import ch.epfl.printwizard.utils.Preconditions;

/**
 * Represents the base class for all expression nodes in the AST (used in program.json).
 */
public abstract class ExprNode {

    private final String code;
    private final int startLine;
    private final int endLine;
    
    protected ExprNode(String code, int startLine, int endLine) {
        Preconditions.RequireNonNull(code, "Code cannot be null");
        Preconditions.Require(!code.isEmpty(), "Code cannot be empty");
        Preconditions.Require(startLine >= 0, "Start line cannot be negative");
        Preconditions.Require(endLine >= startLine, "End line cannot be less than start line");
        
        this.code = code;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    /**
     * Gets the code snippet representing the expression.
     * @return the code snippet
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the starting line number of the expression in the source code.
     * @return the starting line number
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Gets the ending line number of the expression in the source code.
     * @return the ending line number
     */
    public int getEndLine() {
        return endLine;
    }
}
