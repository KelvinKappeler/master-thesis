package ch.epfl.printwizard.shared.model.program.structures.expr;

import ch.epfl.printwizard.shared.model.program.ProgramPosition;
import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents the base class for all expression nodes in the AST (used in program.json).
 */
public abstract class ExprNode {

    private final String code;
    private final ProgramPosition startPosition;
    private final ProgramPosition endPosition;
    
    protected ExprNode(String code, ProgramPosition startPosition, ProgramPosition endPosition) {
        Preconditions.requireNonNull(code, "Code cannot be null");
        Preconditions.require(!code.isEmpty(), "Code cannot be empty");
        Preconditions.requireNonNull(startPosition, "Start position cannot be null");
        Preconditions.requireNonNull(endPosition, "End position cannot be null");
        
        this.code = code;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    /**
     * Gets the code snippet representing the expression.
     * @return the code snippet
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the starting position of the expression in the source code.
     * @return the starting position
     */
    public ProgramPosition getStartPosition() {
        return startPosition;
    }

    /**
     * Gets the ending position of the expression in the source code.
     * @return the ending position
     */
    public ProgramPosition getEndPosition() {
        return endPosition;
    }
}
