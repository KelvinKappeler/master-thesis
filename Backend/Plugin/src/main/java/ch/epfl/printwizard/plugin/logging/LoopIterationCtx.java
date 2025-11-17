package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.List;

/**
 * Represents the context of a loop iteration.
 * @param loopEventId the identifier of the loop event
 * @param iterationEventId the identifier of the iteration event
 * @param iterationIndex the index of the iteration
 * @param conditionEventIds the identifiers of any related condition events
 * @param bodyEventIds the identifiers of events executed in the loop body
 * @param updateEventIds the identifiers of events executed in the loop update
 */
public record LoopIterationCtx(
    String loopEventId,
    String iterationEventId,
    int iterationIndex,
    List<String> conditionEventIds,
    List<String> bodyEventIds,
    List<String> updateEventIds
) {

    public LoopIterationCtx {
        Preconditions.requireNonNull(loopEventId, "loopEventId is null");
        Preconditions.requireNonNull(iterationEventId, "iterationEventId is null");
        Preconditions.requireNonNull(bodyEventIds, "bodyEventIds is null");
        Preconditions.requireNonNull(conditionEventIds, "conditionEventIds is null");
        Preconditions.requireNonNull(updateEventIds, "updateEventIds is null");
        Preconditions.require(!loopEventId.isEmpty(), "loopEventId is empty");
        Preconditions.require(!iterationEventId.isEmpty(), "iterationEventId is empty");
        Preconditions.require(iterationIndex >= 0, "iterationIndex is negative");
    }

}
