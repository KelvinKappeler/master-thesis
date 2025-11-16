package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.events.LoopKind;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.List;

/**
 * Represents the context of a loop.
 * @param loopEventId the identifier of the loop event
 * @param spanId the identifier of the span that contains the loop
 * @param frameId the identifier of the frame that contains the loop
 * @param location the location in the source code where the loop is located
 * @param kind the kind of loop that is executed
 * @param nextIterationIndex the index of the next iteration to be executed
 * @param initEventIds the identifiers of the loop initialization events
 * @param iterationEventIds the identifiers of the loop iteration events
 * @param pendingConditionEvents the identifiers of the pending condition events for the next iteration
 */
public record LoopCtx(
    String loopEventId,
    String spanId,
    String frameId,
    TraceLoc location,
    LoopKind kind,
    int nextIterationIndex,
    List<String> initEventIds,
    List<String> iterationEventIds,
    List<String> pendingConditionEvents
) {

    public LoopCtx {
        Preconditions.requireNonNull(loopEventId, "loopEventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(kind, "kind is null");
        Preconditions.requireNonNull(initEventIds, "initEventIds is null");
        Preconditions.requireNonNull(iterationEventIds, "iterationEventIds is null");
        Preconditions.requireNonNull(pendingConditionEvents, "pendingConditionEvents is null");
        Preconditions.require(!loopEventId.isEmpty(), "loopEventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(nextIterationIndex >= 0, "nextIterationIndex is negative");
    }
}
