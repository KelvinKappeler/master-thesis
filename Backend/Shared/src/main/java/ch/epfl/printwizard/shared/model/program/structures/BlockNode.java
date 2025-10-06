package ch.epfl.printwizard.shared.model.program.structures;

import ch.epfl.printwizard.shared.model.program.ProgramPosition;
import ch.epfl.printwizard.shared.utils.Preconditions;

import java.util.List;

/**
 * Represents a block structure in the AST (used in program.json).
 */
public final class BlockNode extends StructureNode {
    
    private final List<StructureNode> structures;
    
    public BlockNode(String structureId, StructureKind kind, ProgramPosition start, ProgramPosition end, List<StructureNode> structures) {
        super(structureId, kind, null, start, end);

        Preconditions.requireNonNull(structures, "structures cannot be null");
        
        this.structures = structures;
    }
    
    /**
     * Gets the list of structures contained in the block.
     * @return the list of structures
     */
    public List<StructureNode> getStructures() {
        return structures;
    }
}
