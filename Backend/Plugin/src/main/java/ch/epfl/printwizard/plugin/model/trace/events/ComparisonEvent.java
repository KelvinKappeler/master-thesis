package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * A ComparisonEvent represents an event where a comparison operator is performed.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param location the location in the source code where the event occurred
 * @param operator the comparison operator used
 * @param left the left operand involved in the comparison
 * @param leftEventId the event ID of the left operand
 * @param right the right operand involved in the comparison
 * @param rightEventId the event ID of the right operand
 * @param result the result of the comparison
 */
public record ComparisonEvent(
    String eventId,
    String spanId,
    TraceLoc location,
    String operator,
    EventValue left,
    String leftEventId,
    EventValue right,
    String rightEventId,
    EventValue result
) implements TraceEvent {

    public ComparisonEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(operator, "operator is null");
        Preconditions.requireNonNull(left, "left is null");
        Preconditions.requireNonNull(right, "right is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!operator.isEmpty(), "operator is empty");
    }

}
