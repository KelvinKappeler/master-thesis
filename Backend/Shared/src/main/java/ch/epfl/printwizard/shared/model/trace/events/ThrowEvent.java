package ch.epfl.printwizard.shared.model.trace.events;

import ch.epfl.printwizard.shared.model.trace.TraceLoc;
import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * A ThrowEvent represents an event where an exception is thrown during the execution of a program.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param throwableType the resultType of the thrown exception
 * @param message the message of the thrown exception
 */
public record ThrowEvent(
        String eventId,
        String spanId,
        String frameId,
        TraceLoc location,
        String throwableType,
        String message
) implements TraceEvent {

    public ThrowEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(throwableType, "callerMethodId is null");
        Preconditions.requireNonNull(message, "calleeMethodId is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!throwableType.isEmpty(), "callerMethodId is empty");
    }

}
