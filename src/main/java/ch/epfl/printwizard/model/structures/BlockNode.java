package ch.epfl.printwizard.model.structures;

import ch.epfl.printwizard.utils.Preconditions;

import java.util.List;

/**
 * Represents a block structure in the AST (used in program.json).
 */
public final class BlockNode extends StructureNode {
    
    private List<StructureNode> structures;
    
    public BlockNode(String structureId, StructureKind kind, int start, int end, List<StructureNode> structures) {
        super(structureId, kind, start, end);

        Preconditions.requireNonNull(structures, "structures cannot be null");
    }
    
    /**
     * Gets the list of structures contained in the block.
     * @return the list of structures
     */
    public List<StructureNode> getStructures() {
        return structures;
    }
}
