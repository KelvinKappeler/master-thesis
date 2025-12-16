package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * A LoopIterationEvent represents an event where a loop iteration is executed.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param location the location in the source code where the event occurred
 * @param iterationIndex the index of the iteration
 * @param value the value produced by the condition expression
 * @param conditionEventIds the identifiers of any related condition events
 * @param bodyEventIds the identifiers of events executed in the loop body
 * @param updateEventIds the identifiers of events executed in the loop update
 */
public record LoopIterationEvent(
    String eventId,
    String spanId,
    TraceLoc location,
    int iterationIndex,
    EventValue value,
    String[] conditionEventIds,
    String[] bodyEventIds,
    String[] updateEventIds
) implements TraceEvent {
    
    public LoopIterationEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(value, "value is null");
        Preconditions.requireNonNull(conditionEventIds, "conditionEventIds is null");
        Preconditions.requireNonNull(bodyEventIds, "bodyEventIds is null");
        Preconditions.requireNonNull(updateEventIds, "updateEventIds is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(iterationIndex >= 0, "iterationIndex is negative");
    }
    
}
