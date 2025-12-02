package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * A ConditionEvent represents an event where a condition is evaluated.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param kind the kind of condition that is evaluated
 * @param conditionEventIds the identifiers of any related condition events
 * @param thenEventIds the identifiers of events executed if the condition is true
 * @param elseEventIds the identifiers of events executed if the condition is false
 */
public record ConditionEvent(
    String eventId,
    String spanId,
    String frameId,
    TraceLoc location,
    ConditionKind kind,
    String[] conditionEventIds,
    String[] thenEventIds,
    String[] elseEventIds
) implements TraceEvent { 

    public ConditionEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(kind, "kind is null");
        Preconditions.requireNonNull(conditionEventIds, "conditionEventIds is null");
        Preconditions.requireNonNull(thenEventIds, "thenEventIds is null");
        Preconditions.requireNonNull(elseEventIds, "elseEventIds is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
    }

}
