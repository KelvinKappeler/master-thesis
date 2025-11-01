package ch.epfl.printwizard.plugin.model.trace;

import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * Represents a location in the source code.
 * @param sourceId the identifier of the source (e.g., file name)
 * @param line the line number in the source (1-based)
 */
public record TraceLoc(
    String sourceId,
    int line
) {

    public TraceLoc {
        Preconditions.requireNonNull(sourceId, "sourceId is null");
        Preconditions.require(!sourceId.isEmpty(), "sourceId is empty");
        Preconditions.require(line > 0, "line is not positive");
    }

}
