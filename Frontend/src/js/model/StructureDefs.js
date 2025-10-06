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
}

/**
 * Represents a block of code separated by braces (e.g., { ... }).
 */
export class BlockNode extends StructureNode {
    constructor(structureId, startPosition, endPosition, structures) {
        super(structureId, null, startPosition, endPosition);

        this.structures = structures;
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
}

/**
 * Represents a return statement in the code.
 */
export class ReturnNode extends StructureNode {
    constructor(structureId, code, startPosition, endPosition, value) {
        super(structureId, code, startPosition, endPosition);

        this.value = value;
    }
}