package ch.epfl.printwizard.shared.model.program;

import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents a position in source code (line and column).
 */
public record ProgramPosition(int line, int column) {

    public ProgramPosition {
        Preconditions.require(line >= 0, "Line cannot be negative");
        Preconditions.require(column >= 0, "Column cannot be negative");
    }
}
