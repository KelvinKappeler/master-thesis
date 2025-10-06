package ch.epfl.printwizard.shared.model.program.structures;

import ch.epfl.printwizard.shared.model.program.ProgramPosition;
import ch.epfl.printwizard.shared.model.program.structures.expr.ExprNode;
import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents an if-else structure in the AST (used in program.json).
 */
public final class IfNode extends StructureNode {
    
    private final ExprNode condition;
    private final StructureNode thenBranch;
    private final StructureNode elseBranch; // can be null
    
    public IfNode(String structureId, String code, ProgramPosition start, ProgramPosition end, ExprNode condition, StructureNode thenBranch, StructureNode elseBranch) {
        super(structureId, StructureKind.IF, code, start, end);
        
        Preconditions.requireNonNull(condition, "condition cannot be null");
        Preconditions.requireNonNull(thenBranch, "thenBranch cannot be null");
        
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    /**
     * Gets the condition of the if statement.
     * @return the condition expression
     */
    public ExprNode getCondition() {
        return condition;
    }

    /**
     * Gets the 'then' branch of the if statement.
     * @return the 'then' branch structure
     */
    public StructureNode getThenBranch() {
        return thenBranch;
    }

    /**
     * Gets the 'else' branch of the if statement, or null if there is no else branch.
     * @return the 'else' branch structure, or null
     */
    public StructureNode getElseBranch() {
        return elseBranch;
    }
}
