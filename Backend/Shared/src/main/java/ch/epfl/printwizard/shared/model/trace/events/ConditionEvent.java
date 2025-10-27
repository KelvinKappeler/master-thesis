package ch.epfl.printwizard.shared.model.trace.events;

import ch.epfl.printwizard.shared.model.trace.TraceLoc;
import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * A ConditionEvent represents an event where a condition is evaluated.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param left the left operand of the condition
 * @param right the right operand of the condition
 * @param result the result of the condition evaluation
 * @param childrenEventIds the identifiers of any child events related to this condition
 */
public record ConditionEvent(
        String eventId,
        String spanId,
        String frameId,
        TraceLoc location,
        Object left,
        Object right,
        boolean result,
        String[] childrenEventIds
) implements TraceEvent {

    public ConditionEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(childrenEventIds, "childrenEventIds is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
    }

}
