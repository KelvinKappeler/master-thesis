package ch.epfl.printwizard.shared.model.program.structures.expr;

import ch.epfl.printwizard.shared.model.program.ProgramPosition;

/**
 * Represents the code of an expression in the program.json file.
 */
// TODO: Maybe implement variants of expressions (literals, binary ops, method calls, etc.)
public final class ExprCode extends ExprNode {

    public ExprCode(String code, ProgramPosition startPosition, ProgramPosition endPosition) {
        super(code, startPosition, endPosition);
    }
}
