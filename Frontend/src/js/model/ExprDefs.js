/**
 * Represents a node in an expression tree.
 */
export class ExprNode {
    constructor(code, startPosition, endPosition) {
        this.code = code;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
}

/**
 * Represents a code expression node.
 */
export class ExprCode extends ExprNode {
    constructor(code, startPosition, endPosition) {
        super(code, startPosition, endPosition);
    }
}