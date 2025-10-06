package ch.epfl.printwizard.shared.model.program.structures;

import ch.epfl.printwizard.shared.model.program.ProgramPosition;
import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents the base class of a structure in the program.json file. A structure can be a loop, a conditional, etc.
 */
public abstract class StructureNode
{
    private final String structureId;
    private final StructureKind kind;
    private final String code;
    private final ProgramPosition startPosition;
    private final ProgramPosition endPosition;

    protected StructureNode(String structureId, StructureKind kind, String code, ProgramPosition startPosition, ProgramPosition endPosition) {
        Preconditions.requireNonNull(structureId, "Structure ID cannot be null");
        Preconditions.requireNonNull(kind, "Structure kind cannot be null");
        Preconditions.require(!structureId.isEmpty(), "Structure ID cannot be empty");
        Preconditions.requireNonNull(startPosition, "Start position cannot be null");
        Preconditions.requireNonNull(endPosition, "End position cannot be null");

        this.structureId = structureId;
        this.kind = kind;
        this.code = code;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
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
     * Gets the code snippet representing the structure.
     * @return the code snippet
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the starting position of the structure in the source code.
     * @return the starting position
     */
    public ProgramPosition getStartPosition() {
        return startPosition;
    }

    /**
     * Gets the ending position of the structure in the source code.
     * @return the ending position
     */
    public ProgramPosition getEndPosition() {
        return endPosition;
    }
}
