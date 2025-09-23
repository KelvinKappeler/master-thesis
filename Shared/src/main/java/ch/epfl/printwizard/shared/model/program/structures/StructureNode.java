package ch.epfl.printwizard.shared.model.program.structures;

import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents the base class of a structure in the program.json file. A structure can be a loop, a conditional, etc.
 */
public abstract class StructureNode
{
    private final String structureId;
    private final StructureKind kind;
    private final int startLine;
    private final int endLine;

    protected StructureNode(String structureId, StructureKind kind, int start, int end) {
        Preconditions.requireNonNull(structureId, "Structure ID cannot be null");
        Preconditions.requireNonNull(kind, "Structure kind cannot be null");
        Preconditions.require(!structureId.isEmpty(), "Structure ID cannot be empty");
        Preconditions.require(start >= 0, "Start line cannot be negative");
        Preconditions.require(end >= start, "End line cannot be less than start line");

        this.structureId = structureId;
        this.kind = kind;
        this.startLine = start;
        this.endLine = end;
    }

    /**
     * Gets the unique identifier of the structure.
     * @return the structure ID
     */
    public String getStructureId() {
        return structureId;
    }

    /**
     * Gets the kind of the structure.
     * @return the structure kind
     */
    public StructureKind getKind() {
        return kind;
    }

    /**
     * Gets the starting line number of the structure in the source code.
     * @return the starting line number
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Gets the ending line number of the structure in the source code.
     * @return the ending line number
     */
    public int getEndLine() {
        return endLine;
    }
}
