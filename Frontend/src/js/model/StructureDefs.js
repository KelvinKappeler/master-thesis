/**
 * Class representing a position in code with line and column numbers.
 */
export class CodePosition {
    constructor(line, column) {
        this.line = line;
        this.column = column;
    }
}

/**
 * Represents a structural element in the code, such as blocks, loops, or conditionals.
 */
export class StructureNode {
    constructor(structureId, code, startPosition, endPosition) {
        this.id = structureId;
        this.code = code;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    /**
     * Visits the structure node for a specific line.
     * @param line {number} The line number to visit.
     * @returns {StructureNode} The structure node for the line.
     */
    visitForLine(line) {
        if (line >= this.startPosition.line && line <= this.endPosition.line) {
            return this;
        }

        return null;
    }

    /**
     * Checks if the structure node contains a specific line.
     * @param line {number} The line number to check.
     * @returns {boolean} True if the structure node contains the line, false otherwise.
     */
    containsLine(line) {
        return line >= this.startPosition.line && line <= this.endPosition.line;
    }

    /**
     * Returns the line content of the structure node.
     * @returns {string} The line content.
     */
    getLineContent() {
        return this.code;
    }
}

/**
 * Represents a block of code separated by braces (e.g., { ... }).
 */
export class BlockNode extends StructureNode {
    constructor(structureId, startPosition, endPosition, structures) {
        super(structureId, null, startPosition, endPosition);

        this.structures = structures;
    }

    visitForLine(line) {
        if (!this.containsLine(line)) return null;

        for (let structure of this.structures) {
            const result = structure.visitForLine(line);
            if (result) return result;
        }
    }
}

/**
 * Represents a conditional structure (if-else).
 */
export class ExprStmtNode extends StructureNode {
    constructor(structureId, expr, startPosition, endPosition) {
        super(structureId, null, startPosition, endPosition);

        this.expr = expr;
    }

    visitForLine(line) {
        if (!this.containsLine(line)) return null;

        return this;
    }

    getLineContent() {
        return this.expr.code;
    }
}

/**
 * Represents a return statement in the code.
 */
export class ReturnNode extends StructureNode {
    constructor(structureId, code, startPosition, endPosition, value) {
        super(structureId, code, startPosition, endPosition);

        this.value = value;
    }

    visitForLine(line) {
        if (!this.containsLine(line)) return null;

        return this;
    }
}

/**
 * Represents an if-else statement in the code.
 */
export class IfNode extends StructureNode {
    constructor(structureId, code, startPosition, endPosition, condition, thenBranch, elseBranch) {
        super(structureId, code, startPosition, endPosition);

        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    visitForLine(line) {
        if (!this.containsLine(line)) return null;

        const resultThen = this.thenBranch.visitForLine(line);
        if (resultThen) return resultThen;

        const resultElse = this.elseBranch?.visitForLine(line);
        if (resultElse) return resultElse;

        return this;
    }
}
