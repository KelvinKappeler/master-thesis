package ch.epfl.printwizard.model.structures.expr;

/**
 * Represents the code of an expression in the program.json file.
 */
// TODO: Maybe implement variants of expressions (literals, binary ops, method calls, etc.)
public final class ExprCode extends ExprNode {

    public ExprCode(String code, int startLine, int endLine) {
        super(code, startLine, endLine);
    }
}
