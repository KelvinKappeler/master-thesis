package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * An ArithmeticEvent represents an event involving arithmetic operations.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param operation the operation performed on the operands
 * @param left the left operand involved in the computation
 * @param right the right operand involved in the computation
 * @param result the result of the computation
 * @param resultObjectId the ID of the object representing the result, if applicable
 */
public record ArithmeticEvent(
    String eventId,
    String spanId,
    String frameId,
    TraceLoc location,
    String operation,
    Object left,
    Object right,
    Object result,
    String resultObjectId
) implements TraceEvent {

    public ArithmeticEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(operation, "operation is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!operation.isEmpty(), "operation is empty");
    }

}

