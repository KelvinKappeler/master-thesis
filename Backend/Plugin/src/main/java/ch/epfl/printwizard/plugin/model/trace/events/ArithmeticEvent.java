package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * An ArithmeticEvent represents an event involving arithmetic operations.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param location the location in the source code where the event occurred
 * @param operator the operator performed on the operands
 * @param left the left operand involved in the computation
 * @param leftEventId the event ID of the left operand
 * @param right the right operand involved in the computation
 * @param rightEventId the event ID of the right operand
 * @param value the result of the arithmetic operator
 */
public record ArithmeticEvent(
    String eventId,
    String spanId,
    TraceLoc location,
    String operator,
    EventValue left,
    String leftEventId,
    EventValue right,
    String rightEventId,
    EventValue value
) implements TraceEvent {

    public ArithmeticEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(operator, "operator is null");
        Preconditions.requireNonNull(value, "value is null");
        Preconditions.requireNonNull(left, "left is null");
        Preconditions.requireNonNull(right, "right is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!operator.isEmpty(), "operator is empty");
    }

}

