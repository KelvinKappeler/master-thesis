package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.List;

/**
 * Represents the context of a block.
 * @param parentEventId the identifier of the parent event
 * @param eventIds the identifiers of the events in the block
 */
public record BlockCtx(String parentEventId, List<String> eventIds) {

    public BlockCtx {
        Preconditions.requireNonNull(parentEventId, "parentEventId is null");
        Preconditions.requireNonNull(eventIds, "eventIds is null");
        Preconditions.require(!parentEventId.isEmpty(), "parentEventId is empty");
    }

}
