package ch.epfl.printwizard.model.structures;

import ch.epfl.printwizard.utils.Preconditions;

/**
 * Represents the base class of a structure in the program.json file. A structure can be a loop, a conditional, etc.
 */
public abstract class Structure
{
    public String structureId;
    public StructureKind kind;
    public int startLine;
    public int endLine;

    protected Structure(String structureId, StructureKind kind, int start, int end) {
        Preconditions.RequireNonNull(structureId, "Structure ID cannot be null");
        Preconditions.RequireNonNull(kind, "Structure kind cannot be null");
        Preconditions.Require(!structureId.isEmpty(), "Structure ID cannot be empty");
        Preconditions.Require(start >= 0, "Start line cannot be negative");
        Preconditions.Require(end >= start, "End line cannot be less than start line");

        this.structureId = structureId;
        this.kind = kind;
        this.startLine = start;
        this.endLine = end;
    }
}
