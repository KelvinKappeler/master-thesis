package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * A ComparisonEvent represents an event where a comparison operation is performed.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param operator the comparison operator used
 * @param left the left operand involved in the comparison
 * @param right the right operand involved in the comparison
 * @param result the result of the comparison
 */
public record ComparisonEvent(
        String eventId,
        String spanId,
        String frameId,
        TraceLoc location,
        String operator,
        Object left,
        Object right,
        boolean result
) implements TraceEvent {

    public ComparisonEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(operator, "operator is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!operator.isEmpty(), "operator is empty");
    }

}
