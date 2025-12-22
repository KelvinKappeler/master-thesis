/**
 * Represents a node in an expression tree.
 */
export class ExprNode {
    constructor(code, startPosition, endPosition) {
        this.code = code;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    /**
     * Checks if the expression node contains the given line.
     * @param line {number} The line number to check.
     * @returns {boolean} True if the expression node contains the line, false otherwise.
     */
    containsLine(line) {
        return this.startPosition.line <= line && line <= this.endPosition.line;
    }

    /**
     * Returns the line content of the expression node.
     * @returns {string} The line content.
     */
    getLineContent() {
        return this.code;
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